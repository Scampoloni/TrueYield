package ch.zhaw.trueyield.chat;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.service.EvidenceService;
import ch.zhaw.trueyield.service.HoldingService;
import ch.zhaw.trueyield.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class EsgChatTools {

    private static final Logger log = LoggerFactory.getLogger(EsgChatTools.class);

    private final PortfolioService portfolioService;
    private final HoldingService holdingService;
    private final EvidenceService evidenceService;

    public EsgChatTools(PortfolioService portfolioService, HoldingService holdingService, EvidenceService evidenceService) {
        this.portfolioService = portfolioService;
        this.holdingService = holdingService;
        this.evidenceService = evidenceService;
    }

    @Tool(description = "List all portfolios in the system.")
    public String getAllPortfolios() {
        logToolCall("getAllPortfolios");
        List<Portfolio> portfolios = portfolioService.getAllPortfolios();
        if (portfolios == null || portfolios.isEmpty()) {
            return "No results found";
        }

        StringBuilder sb = new StringBuilder("Portfolios:\n");
        portfolios.stream()
                .sorted(Comparator.comparing(p -> normalize(p.getName())))
                .forEach(p -> sb.append("- ")
                        .append(safe(p.getName()))
                        .append(" (id: ")
                        .append(safe(p.getId()))
                        .append(")\n"));
        return sb.toString().trim();
    }

    @Tool(description = "List holdings for a portfolio by portfolio name.")
    public String getHoldingsByPortfolioName(
            @ToolParam(description = "Portfolio name") String portfolioName) {
        logToolCall("getHoldingsByPortfolioName");
        String normalizedPortfolioName = normalize(portfolioName);
        if (normalizedPortfolioName.isEmpty()) {
            return "No results found";
        }

        Portfolio portfolio = findPortfolioByName(normalizedPortfolioName);
        if (portfolio == null) {
            return "Portfolio '" + safe(trimToEmpty(portfolioName)) + "' not found";
        }

        List<Holding> holdings = holdingService.getHoldingsByPortfolioId(portfolio.getId());
        if (holdings == null || holdings.isEmpty()) {
            return "No results found";
        }

        StringBuilder sb = new StringBuilder("Holdings for '")
                .append(safe(portfolio.getName()))
                .append("':\n");
        holdings.stream()
                .sorted(Comparator.comparing(h -> normalize(preferNameOrSymbol(h))))
                .forEach(h -> sb.append("- ")
                        .append(safe(preferNameOrSymbol(h)))
                        .append(" (symbol: ")
                        .append(safe(h.getSymbol()))
                        .append(", id: ")
                        .append(safe(h.getId()))
                        .append(")\n"));
        return sb.toString().trim();
    }

    @Tool(description = "List evidence for a holding by holding name.")
    public String getEvidenceByHoldingName(
            @ToolParam(description = "Holding name") String holdingName) {
        logToolCall("getEvidenceByHoldingName");
        String normalizedHoldingName = normalize(holdingName);
        if (normalizedHoldingName.isEmpty()) {
            return "No results found";
        }

        Holding holding = findHoldingByName(normalizedHoldingName);
        if (holding == null) {
            return "No results found";
        }

        List<Evidence> evidences = evidenceService.getEvidenceByHoldingId(holding.getId());
        if (evidences == null || evidences.isEmpty()) {
            return "No results found";
        }

        StringBuilder sb = new StringBuilder("Evidence for '")
                .append(safe(preferNameOrSymbol(holding)))
                .append("':\n");

        evidences.stream()
                .sorted(Comparator.comparing(e -> safe(e.getId())))
                .forEach(e -> sb.append("- id: ")
                        .append(safe(e.getId()))
                        .append(", sentiment: ")
                        .append(formatSentiment(e.getAiSentimentScore()))
                        .append(", source: ")
                        .append(safe(e.getSourceUrl()))
                        .append("\n"));

        return sb.toString().trim();
    }

    @Tool(description = "Create a portfolio. Fund-manager role required.")
    public String createPortfolio(
            @ToolParam(description = "Portfolio name") String name,
            @ToolParam(description = "Portfolio description") String description) {
        logToolCall("createPortfolio");
        requireFundManagerForPortfolioCreation();

        String normalizedName = trimToEmpty(name);
        if (normalizedName.isBlank()) {
            return "No results found";
        }

        PortfolioCreateDTO dto = new PortfolioCreateDTO();
        setField(dto, "name", normalizedName);
        setField(dto, "description", trimToNull(description));

        Portfolio created = portfolioService.createPortfolio(dto, currentUsername());
        return "Created portfolio '" + safe(created.getName()) + "' (id: " + safe(created.getId()) + ")";
    }

    @Tool(description = "Create a holding in a portfolio by portfolio name. Fund-manager role required.")
    public String createHolding(
            @ToolParam(description = "Portfolio name") String portfolioName,
            @ToolParam(description = "Holding name") String holdingName,
            @ToolParam(description = "Ticker symbol") String ticker) {
        logToolCall("createHolding");
        requireFundManagerForHoldingCreation();

        String normalizedPortfolioName = normalize(portfolioName);
        String normalizedHoldingName = trimToEmpty(holdingName);
        String normalizedTicker = trimToEmpty(ticker).toUpperCase(Locale.ROOT);

        if (normalizedPortfolioName.isEmpty() || normalizedHoldingName.isEmpty() || normalizedTicker.isEmpty()) {
            return "No results found";
        }

        Portfolio portfolio = findPortfolioByName(normalizedPortfolioName);
        if (portfolio == null) {
            return "Portfolio '" + safe(trimToEmpty(portfolioName)) + "' not found";
        }

        HoldingCreateDTO dto = new HoldingCreateDTO();
        setField(dto, "portfolioId", portfolio.getId());
        setField(dto, "name", normalizedHoldingName);
        setField(dto, "symbol", normalizedTicker);

        Holding created = holdingService.createHolding(dto);
        return "Created holding '" + safe(created.getName()) + "' (symbol: " + safe(created.getSymbol())
                + ", id: " + safe(created.getId()) + ") in portfolio '" + safe(portfolio.getName()) + "'";
    }

    private Portfolio findPortfolioByName(String normalizedName) {
        return portfolioService.getAllPortfolios().stream()
                .filter(Objects::nonNull)
                .filter(p -> normalize(p.getName()).equals(normalizedName))
                .findFirst()
                .orElse(null);
    }

    private Holding findHoldingByName(String normalizedHoldingName) {
        List<Portfolio> portfolios = portfolioService.getAllPortfolios();
        for (Portfolio portfolio : portfolios) {
            List<Holding> holdings = holdingService.getHoldingsByPortfolioId(portfolio.getId());
            for (Holding holding : holdings) {
                if (normalize(preferNameOrSymbol(holding)).equals(normalizedHoldingName)) {
                    return holding;
                }
            }
        }
        return null;
    }

    private void requireFundManagerForPortfolioCreation() {
        if (!hasRole("ROLE_fund-manager")) {
            throw new AccessDeniedException("Only fund-managers can create portfolios");
        }
    }

    private void requireFundManagerForHoldingCreation() {
        if (!hasRole("ROLE_fund-manager")) {
            throw new AccessDeniedException("Only fund-managers can create holdings");
        }
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private void logToolCall(String toolName) {
        log.info("ChatTool [{}] called by user: {}", toolName, currentUsername());
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private String preferNameOrSymbol(Holding holding) {
        String name = trimToEmpty(holding.getName());
        return name.isBlank() ? trimToEmpty(holding.getSymbol()) : name;
    }

    private String normalize(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? "n/a" : trimmed;
    }

    private String formatSentiment(Double score) {
        if (score == null) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.2f", score);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Could not map chat tool input to DTO field: " + fieldName, e);
        }
    }
}
