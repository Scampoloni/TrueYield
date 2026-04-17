package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.model.dto.AuditCommentCreateDTO;
import ch.zhaw.trueyield.model.dto.AuditCommentResponseDTO;
import ch.zhaw.trueyield.security.UserService;
import ch.zhaw.trueyield.service.AuditCommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service/auditcomment")
public class AuditCommentController {

    @Autowired
    private AuditCommentService auditCommentService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('fund-manager','auditor')")
    public ResponseEntity<List<AuditCommentResponseDTO>> getComments(@RequestParam String auditReportId) {
        List<AuditComment> comments = auditCommentService.getCommentsByReportId(auditReportId);
        List<AuditCommentResponseDTO> response = comments.stream()
                .map(AuditCommentResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditCommentResponseDTO> createComment(@Valid @RequestBody AuditCommentCreateDTO dto) {
        try {
            String auditorId = userService.getCurrentUserId();
            AuditComment created = auditCommentService.createComment(dto, auditorId);
            return new ResponseEntity<>(AuditCommentResponseDTO.fromEntity(created), HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
