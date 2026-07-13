package ch.zhaw.trueyield.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.model.dto.PortfolioUpdateDTO;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.repository.PortfolioRepository;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AuditReportRepository auditReportRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    // CREATE
    public Portfolio createPortfolio(PortfolioCreateDTO dto, String fundManagerId) {
        Portfolio portfolio = new Portfolio(dto.getName(), fundManagerId);
        portfolio.setDescription(dto.getDescription());
        return portfolioRepository.save(portfolio);
    }

    // READ ALL (nur eigene!)
    public List<Portfolio> getAllPortfoliosByFundManager(String fundManagerId) {
        return portfolioRepository.findByFundManagerId(fundManagerId);
    }

    // READ ALL (für Auditoren)
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    // READ ONE (mit Ownership Check!)
    public Portfolio getPortfolioById(String id, String requestingUserId) {
        Optional<Portfolio> optionalPortfolio = portfolioRepository.findById(id);

        if (optionalPortfolio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found");
        }

        Portfolio portfolio = optionalPortfolio.get();

        // OWNERSHIP CHECK
        if (!portfolio.getFundManagerId().equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return portfolio;
    }

    // READ ONE (für Auditoren — kein Ownership Check)
    public Portfolio getPortfolioByIdForAuditor(String id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    }

    // UPDATE (mit Ownership Check!)
    public Portfolio updatePortfolio(String id, PortfolioUpdateDTO dto, String requestingUserId) {
        Portfolio portfolio = getPortfolioById(id, requestingUserId);
        portfolio.setName(dto.getName());
        portfolio.setDescription(dto.getDescription());
        return portfolioRepository.save(portfolio);
    }

    // DELETE (mit Ownership Check!)
    public void deletePortfolio(String id, String requestingUserId) {
        Portfolio portfolio = getPortfolioById(id, requestingUserId);
        if (!auditReportRepository.findByPortfolioId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Portfolios with audit reports are retained to preserve review evidence");
        }
        List<Holding> holdings = holdingRepository.findByPortfolioId(id);
        for (Holding holding : holdings) {
            evidenceRepository.deleteAll(evidenceRepository.findByHoldingId(holding.getId()));
        }
        holdingRepository.deleteAll(holdings);
        portfolioRepository.deleteById(id);
    }

    // FK EXISTENCE CHECK
    public boolean portfolioExists(String id) {
        return portfolioRepository.existsById(id);
    }

    // AUDIT STATUS MAP (delegiert von Controller, vermeidet Layer-Verletzung)
    public Map<String, String> getLatestAuditStatusByPortfolioIds(List<String> portfolioIds) {
        return auditReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> portfolioIds.contains(r.getPortfolioId()))
                .collect(Collectors.toMap(
                        AuditReport::getPortfolioId,
                        r -> r.getAuditStatus().name(),
                        (existing, replacement) -> existing
                ));
    }
}
