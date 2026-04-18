package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.service.ComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
