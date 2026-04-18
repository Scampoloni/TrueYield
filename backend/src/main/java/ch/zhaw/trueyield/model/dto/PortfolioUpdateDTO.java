package ch.zhaw.trueyield.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PortfolioUpdateDTO {
    @NotBlank
    @Size(max = 100)
    private String name;
    private String description;
}
