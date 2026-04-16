package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class EvidenceService {

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired(required = false)
    private AiAnalysisService aiAnalysisService;

    public List<Evidence> getEvidenceByHoldingId(String holdingId) {
        return evidenceRepository.findByHoldingId(holdingId);
    }

    public long countByHoldingId(String holdingId) {
        return evidenceRepository.countByHoldingId(holdingId);
    }

    public boolean existsByHoldingIdAndSourceUrl(String holdingId, String sourceUrl) {
        return evidenceRepository.existsByHoldingIdAndSourceUrl(holdingId, sourceUrl);
    }

    public Evidence getEvidenceById(String id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Evidence not found: " + id));
    }

    public Evidence createEvidence(EvidenceCreateDTO dto) {
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
        if (!evidenceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Evidence not found: " + id);
        }
        evidenceRepository.deleteById(id);
    }
}
