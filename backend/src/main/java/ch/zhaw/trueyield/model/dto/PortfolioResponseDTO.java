package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PortfolioResponseDTO {

    private String id;
    private String name;
    private String description;
    private String fundManagerId;

    public static PortfolioResponseDTO fromEntity(Portfolio portfolio) {
        return new PortfolioResponseDTO(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getDescription(),
                portfolio.getFundManagerId()
        );
    }
}
