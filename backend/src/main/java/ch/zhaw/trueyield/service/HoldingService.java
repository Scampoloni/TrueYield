package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoldingService {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private AccessControlService accessControlService;

    public List<Holding> getHoldingsByPortfolioId(String portfolioId) {
        accessControlService.requirePortfolioAccess(portfolioId);
        return holdingRepository.findByPortfolioId(portfolioId);
    }

    public void deleteHolding(String id) {
        Holding holding = accessControlService.requireHoldingAccess(id);
        holdingRepository.deleteById(holding.getId());
    }

    public Holding createHolding(HoldingCreateDTO dto) {
        accessControlService.requireFundManagerPortfolioAccess(dto.getPortfolioId());
        Holding holding = new Holding(dto.getPortfolioId(), dto.getSymbol());
        holding.setIsin(dto.getIsin());
        holding.setName(dto.getName());
        holding.setWeightPercent(dto.getWeightPercent());
        return holdingRepository.save(holding);
    }
}
