package ch.zhaw.trueyield.controller;

import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.dto.AuditReportCreateDTO;
import ch.zhaw.trueyield.model.dto.AuditReportResponseDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.service.AuditReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<AuditReportResponseDTO> createAuditReport(@Valid @RequestBody AuditReportCreateDTO dto) {
        try {
            AuditReport created = auditReportService.createAuditReport(dto);
            return new ResponseEntity<>(AuditReportResponseDTO.fromEntity(created), HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuditReportResponseDTO> getAuditReportById(@PathVariable String id) {
        try {
            AuditReport report = auditReportService.getAuditReportById(id);
            return new ResponseEntity<>(AuditReportResponseDTO.fromEntity(report), HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @PutMapping("/reject")
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditReportResponseDTO> rejectAuditReport(@Valid @RequestBody StateChangeDTO dto) {
        try {
            AuditReport updated = auditReportService.rejectAuditReport(dto);
            return new ResponseEntity<>(AuditReportResponseDTO.fromEntity(updated), HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @PutMapping("/assign")
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditReportResponseDTO> assignAuditReport(@Valid @RequestBody StateChangeDTO dto) {
        try {
            AuditReport updated = auditReportService.assignAuditReport(dto);
            return new ResponseEntity<>(AuditReportResponseDTO.fromEntity(updated), HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @PutMapping("/complete")
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditReportResponseDTO> completeAuditReport(@Valid @RequestBody StateChangeDTO dto) {
        try {
            AuditReport updated = auditReportService.completeAuditReport(dto);
            return new ResponseEntity<>(AuditReportResponseDTO.fromEntity(updated), HttpStatus.OK);
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
