package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EvidenceService {

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired(required = false)
    private AiAnalysisService aiAnalysisService;

    public List<Evidence> getEvidenceByHoldingId(String holdingId) {
        accessControlService.requireHoldingAccess(holdingId);
        return evidenceRepository.findByHoldingId(holdingId);
    }

    public long countByHoldingId(String holdingId) {
        return evidenceRepository.countByHoldingId(holdingId);
    }

    public boolean existsByHoldingIdAndSourceUrl(String holdingId, String sourceUrl) {
        return evidenceRepository.existsByHoldingIdAndSourceUrl(holdingId, sourceUrl);
    }

    public Evidence getEvidenceById(String id) {
        return accessControlService.requireEvidenceAccess(id);
    }

    public Evidence createEvidence(EvidenceCreateDTO dto) {
        accessControlService.requireHoldingAccess(dto.getHoldingId());
        double sentimentScore = 0.0;
        try {
            if (aiAnalysisService != null) {
                sentimentScore = aiAnalysisService.analyzeSentiment(dto.getContentSnippet());
            }
        } catch (Exception ignored) {
            sentimentScore = 0.0;
        }

        Evidence evidence = new Evidence(dto.getHoldingId());
        evidence.setSourceUrl(dto.getSourceUrl());
        evidence.setContentSnippet(dto.getContentSnippet());
        evidence.setAiSentimentScore(sentimentScore);
        evidence.setPublishedAt(LocalDate.now());
        return evidenceRepository.save(evidence);
    }

    public void deleteEvidence(String id) {
        Evidence evidence = accessControlService.requireEvidenceAccess(id);
        evidenceRepository.deleteById(evidence.getId());
    }
}
