package ch.zhaw.trueyield.service;

import java.util.List;
import java.util.Optional;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.model.dto.PortfolioUpdateDTO;
import ch.zhaw.trueyield.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

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

    // READ ONE (ohne Ownership Check - für Auditoren)
    public Portfolio getPortfolioByIdForAuditor(String id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
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
        portfolioRepository.deleteById(id);
    }

    // FK EXISTENCE CHECK
    public boolean portfolioExists(String id) {
        return portfolioRepository.existsById(id);
    }
}
