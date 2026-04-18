package ch.zhaw.trueyield.model.dto;

import java.util.Map;

public record ComplianceOverviewDTO(
        long totalPortfolios,
        long totalHoldings,
        long totalAuditReports,
        Map<String, Long> reportsByStatus
) {}
