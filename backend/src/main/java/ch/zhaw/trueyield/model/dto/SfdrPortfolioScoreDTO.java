package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.enums.SfdrClassification;

public record SfdrPortfolioScoreDTO(
        String portfolioId,
        String portfolioName,
        SfdrClassification classification,
        double averageSentiment,
        int evidenceCount
) {}
