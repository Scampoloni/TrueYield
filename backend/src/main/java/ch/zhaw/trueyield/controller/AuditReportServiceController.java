package ch.zhaw.trueyield.controller;

import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.service.AuditReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/service/auditreport")
public class AuditReportServiceController {

    @Autowired
    private AuditReportService auditReportService;

    @PutMapping("/assign")
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditReport> assignAuditReport(@Valid @RequestBody StateChangeDTO dto) {
        try {
            AuditReport updated = auditReportService.assignAuditReport(dto);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @PutMapping("/complete")
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditReport> completeAuditReport(@Valid @RequestBody StateChangeDTO dto) {
        try {
            AuditReport updated = auditReportService.completeAuditReport(dto);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditReportAggregationDTO>> getDashboard(
            @RequestParam String portfolioId) {
        try {
            List<AuditReportAggregationDTO> result =
                    auditReportService.getAuditReportDashboard(portfolioId);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
