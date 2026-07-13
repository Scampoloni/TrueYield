package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.enums.EsgEvidenceSignal;

/** Non-regulatory aggregation of stored evidence signals for one portfolio. */
public record EsgEvidenceSignalDTO(
        String portfolioId,
        String portfolioName,
        EsgEvidenceSignal signal,
        double averageEvidenceSentiment,
        int evidenceCount
) {}
