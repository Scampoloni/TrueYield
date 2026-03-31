package ch.zhaw.trueyield.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuditCommentCreateDTO {

    @NotBlank
    private String auditReportId;

    @NotBlank
    private String comment;
}
