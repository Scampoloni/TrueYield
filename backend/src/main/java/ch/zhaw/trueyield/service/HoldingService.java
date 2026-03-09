package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HoldingService {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private PortfolioService portfolioService;

    public Holding createHolding(HoldingCreateDTO dto) {
        if (!portfolioService.portfolioExists(dto.getPortfolioId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Portfolio not found: " + dto.getPortfolioId());
        }
        Holding holding = new Holding(dto.getPortfolioId(), dto.getSymbol());
        holding.setIsin(dto.getIsin());
        holding.setName(dto.getName());
        holding.setWeightPercent(dto.getWeightPercent());
        return holdingRepository.save(holding);
    }
}
