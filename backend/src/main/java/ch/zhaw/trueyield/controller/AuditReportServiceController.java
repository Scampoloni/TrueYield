package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.service.AuditReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/service/auditreport")
public class AuditReportServiceController {

    @Autowired
    private AuditReportService auditReportService;

    @PutMapping("/assign")
    public ResponseEntity<AuditReport> assignAuditReport(@Valid @RequestBody StateChangeDTO dto) {
        try {
            AuditReport updated = auditReportService.assignAuditReport(dto);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
