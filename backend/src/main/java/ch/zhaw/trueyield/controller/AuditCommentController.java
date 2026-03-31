package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.model.dto.AuditCommentCreateDTO;
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

@RestController
@RequestMapping("/api/service/auditcomment")
public class AuditCommentController {

    @Autowired
    private AuditCommentService auditCommentService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditComment>> getComments(@RequestParam String auditReportId) {
        List<AuditComment> comments = auditCommentService.getCommentsByReportId(auditReportId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('auditor')")
    public ResponseEntity<AuditComment> createComment(@Valid @RequestBody AuditCommentCreateDTO dto) {
        try {
            String auditorId = userService.getCurrentUserId();
            AuditComment created = auditCommentService.createComment(dto, auditorId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
