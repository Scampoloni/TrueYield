package ch.zhaw.trueyield.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class StateChangeDTO {

    @NotBlank
    private String auditReportId;

    private String auditorId;

    private String rationale;
}
