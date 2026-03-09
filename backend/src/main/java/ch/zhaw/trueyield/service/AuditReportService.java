package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditReportService {

    @Autowired
    private AuditReportRepository auditReportRepository;

    public AuditReport assignAuditReport(StateChangeDTO dto) {
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport must be in PENDING_REVIEW state");
        }
        report.setAuditStatus(AuditStatus.UNDER_REVIEW);
        report.setAuditorId(dto.getAuditorId());
        return auditReportRepository.save(report);
    }
}
