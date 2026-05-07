package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.dto.AuditReportResponseDTO;
import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.model.dto.PortfolioResponseDTO;
import ch.zhaw.trueyield.model.dto.SfdrPortfolioScoreDTO;
import ch.zhaw.trueyield.service.ComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('compliance-officer')")
    public ResponseEntity<ComplianceOverviewDTO> getOverview() {
        return ResponseEntity.ok(complianceService.getOverview());
    }

    @GetMapping("/sfdr")
    @PreAuthorize("hasRole('compliance-officer') or hasRole('fund-manager')")
    public ResponseEntity<List<SfdrPortfolioScoreDTO>> getSfdrScores() {
        return ResponseEntity.ok(complianceService.getSfdrScores());
    }

    @GetMapping("/portfolios")
    @PreAuthorize("hasRole('compliance-officer')")
    public ResponseEntity<List<PortfolioResponseDTO>> getAllPortfolios() {
        List<PortfolioResponseDTO> result = complianceService.getAllPortfolios().stream()
                .map(PortfolioResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('compliance-officer')")
    public ResponseEntity<List<AuditReportResponseDTO>> getAllReports() {
        List<AuditReportResponseDTO> result = complianceService.getAllReports().stream()
                .map(AuditReportResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
