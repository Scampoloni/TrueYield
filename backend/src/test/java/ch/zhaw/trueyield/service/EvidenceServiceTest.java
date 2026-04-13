package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private EvidenceService evidenceService;

    @Test
    void getEvidenceByHoldingId_returnsList() {
        Evidence e = new Evidence("holding-1");
        when(evidenceRepository.findByHoldingId("holding-1")).thenReturn(List.of(e));

        List<Evidence> result = evidenceService.getEvidenceByHoldingId("holding-1");

        assertEquals(1, result.size());
        verify(evidenceRepository).findByHoldingId("holding-1");
    }

    @Test
    void getEvidenceById_returnsEvidence_whenFound() {
        Evidence evidence = new Evidence("holding-1");
        evidence.setId("e-1");
        when(evidenceRepository.findById("e-1")).thenReturn(Optional.of(evidence));

        Evidence result = evidenceService.getEvidenceById("e-1");

        assertEquals("e-1", result.getId());
        verify(evidenceRepository).findById("e-1");
    }

    @Test
    void getEvidenceById_notFound_throwsNotFound() {
        when(evidenceRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evidenceService.getEvidenceById("missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createEvidence_callsAiAndSaves() {
        EvidenceCreateDTO dto = new EvidenceCreateDTO();
        dto.setHoldingId("holding-1");
        dto.setSourceUrl("https://example.com");
        dto.setContentSnippet("Great ESG news");
        when(aiAnalysisService.analyzeSentiment(anyString())).thenReturn(0.75);
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evidence result = evidenceService.createEvidence(dto);

        assertNotNull(result);
        assertEquals(0.75, result.getAiSentimentScore());
        verify(evidenceRepository).save(any());
    }

    @Test
    void createEvidence_fallsBackToZero_whenAiFails() {
        EvidenceCreateDTO dto = new EvidenceCreateDTO();
        dto.setHoldingId("holding-1");
        dto.setContentSnippet("Some news");
        when(aiAnalysisService.analyzeSentiment(anyString())).thenThrow(new RuntimeException("AI error"));
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evidence result = evidenceService.createEvidence(dto);

        assertEquals(0.0, result.getAiSentimentScore());
    }

    @Test
    void deleteEvidence_notFound_throwsNotFound() {
        when(evidenceRepository.existsById("nonexistent")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evidenceService.deleteEvidence("nonexistent"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(evidenceRepository, never()).deleteById(anyString());
    }

    @Test
    void deleteEvidence_existing_callsDelete() {
        when(evidenceRepository.existsById("e-1")).thenReturn(true);

        assertDoesNotThrow(() -> evidenceService.deleteEvidence("e-1"));

        verify(evidenceRepository).deleteById("e-1");
    }
}
