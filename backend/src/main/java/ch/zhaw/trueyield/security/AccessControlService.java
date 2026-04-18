package ch.zhaw.trueyield.security;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccessControlService {

    @Autowired
    private UserService userService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private AuditReportRepository auditReportRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    public void requirePortfolioAccess(String portfolioId) {
        if (userService.userHasRole("compliance-officer")) {
            return;
        }
        if (userService.userHasRole("auditor")) {
            String auditorId = userService.getCurrentUserId();
            if (!auditorCanAccessPortfolio(portfolioId, auditorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            return;
        }
        portfolioService.getPortfolioById(portfolioId, userService.getCurrentUserId());
    }

    public void requireFundManagerPortfolioAccess(String portfolioId) {
        if (userService.userHasRole("auditor") || userService.userHasRole("compliance-officer")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        portfolioService.getPortfolioById(portfolioId, userService.getCurrentUserId());
    }

    public Holding requireHoldingAccess(String holdingId) {
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));
        requirePortfolioAccess(holding.getPortfolioId());
        return holding;
    }

    public Evidence requireEvidenceAccess(String evidenceId) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));
        requireHoldingAccess(evidence.getHoldingId());
        return evidence;
    }

    public void requireAuditReportAccess(AuditReport report) {
        if (userService.userHasRole("compliance-officer")) {
            return;
        }
        if (userService.userHasRole("auditor")) {
            String auditorId = userService.getCurrentUserId();
            if (report.getAuditStatus() == AuditStatus.PENDING_REVIEW) {
                return;
            }
            if (report.getAuditorId() == null || !report.getAuditorId().equals(auditorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            return;
        }
        portfolioService.getPortfolioById(report.getPortfolioId(), userService.getCurrentUserId());
    }

    public String requireAuditorId() {
        if (!userService.userHasRole("auditor")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return userService.getCurrentUserId();
    }

    private boolean auditorCanAccessPortfolio(String portfolioId, String auditorId) {
        List<AuditReport> reports = auditReportRepository.findByPortfolioId(portfolioId);
        return reports.stream().anyMatch(report ->
                report.getAuditStatus() == AuditStatus.PENDING_REVIEW ||
                auditorId.equals(report.getAuditorId()));
    }
}
