package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public Portfolio createPortfolio(PortfolioCreateDTO dto) {
        // fundManagerId is hardcoded until Auth0 integration (Issue #27)
        Portfolio portfolio = new Portfolio(dto.getName(), "anonymous");
        portfolio.setDescription(dto.getDescription());
        return portfolioRepository.save(portfolio);
    }
}
