package ch.zhaw.trueyield.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class HoldingCreateDTO {

    @NotBlank
    private String portfolioId;

    @NotBlank
    private String symbol;

    private String isin;

    private String name;

    private Double weightPercent;
}
