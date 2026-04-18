package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.model.dto.EvidenceResponseDTO;
import ch.zhaw.trueyield.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    @Autowired
    private EvidenceService evidenceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('fund-manager','auditor','compliance-officer')")
    public ResponseEntity<List<EvidenceResponseDTO>> getByHoldingId(@RequestParam String holdingId) {
        List<EvidenceResponseDTO> evidence = evidenceService.getEvidenceByHoldingId(holdingId)
                .stream()
                .map(EvidenceResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(evidence);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('fund-manager','auditor','compliance-officer')")
    public ResponseEntity<EvidenceResponseDTO> getById(@PathVariable String id) {
        Evidence evidence = evidenceService.getEvidenceById(id);
        return ResponseEntity.ok(EvidenceResponseDTO.fromEntity(evidence));
    }

    @PostMapping
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<EvidenceResponseDTO> createEvidence(@Valid @RequestBody EvidenceCreateDTO dto) {
        Evidence created = evidenceService.createEvidence(dto);
        return new ResponseEntity<>(EvidenceResponseDTO.fromEntity(created), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<Void> deleteEvidence(@PathVariable String id) {
        evidenceService.deleteEvidence(id);
        return ResponseEntity.noContent().build();
    }
}
