package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.model.dto.HoldingResponseDTO;
import ch.zhaw.trueyield.security.AccessControlService;
import ch.zhaw.trueyield.service.HoldingService;
import ch.zhaw.trueyield.service.NewsIngestionService;
import ch.zhaw.trueyield.service.provider.NewsProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/holding")
public class HoldingController {

    @Autowired
    private HoldingService holdingService;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private NewsIngestionService newsIngestionService;

    @Autowired
    private List<NewsProvider> newsProviders;

    @GetMapping
    @PreAuthorize("hasAnyRole('fund-manager','auditor','compliance-officer')")
    public ResponseEntity<List<HoldingResponseDTO>> getHoldingsByPortfolioId(@RequestParam String portfolioId) {
        List<HoldingResponseDTO> response = holdingService.getHoldingsByPortfolioId(portfolioId).stream()
                .map(HoldingResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<Void> deleteHolding(@PathVariable String id) {
        holdingService.deleteHolding(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<HoldingResponseDTO> createHolding(@Valid @RequestBody HoldingCreateDTO dto) {
        Holding created = holdingService.createHolding(dto);
        return new ResponseEntity<>(HoldingResponseDTO.fromEntity(created), HttpStatus.CREATED);
    }

    @GetMapping("/news-provider-status")
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<Map<String, Boolean>> getNewsProviderStatus() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        for (NewsProvider provider : newsProviders) {
            status.put(provider.getProviderName(), provider.isConfigured());
        }
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{id}/ingest-news")
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<Void> ingestNews(@PathVariable String id) {
        Holding holding = accessControlService.requireHoldingAccess(id);
        String companyName = holding.getName() != null && !holding.getName().isBlank()
                ? holding.getName() : holding.getSymbol();
        newsIngestionService.ingestNewsForHolding(holding.getId(), companyName);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
