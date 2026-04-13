package ch.zhaw.trueyield.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EvidenceCreateDTO {

    @NotBlank
    private String holdingId;

    private String sourceUrl;

    @NotBlank
    private String contentSnippet;
}
