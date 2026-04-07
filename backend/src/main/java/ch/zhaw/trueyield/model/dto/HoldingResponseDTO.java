package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.Holding;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HoldingResponseDTO {

    private String id;
    private String portfolioId;
    private String symbol;
    private String isin;
    private String name;
    private Double weightPercent;

    public static HoldingResponseDTO fromEntity(Holding holding) {
        return new HoldingResponseDTO(
                holding.getId(),
                holding.getPortfolioId(),
                holding.getSymbol(),
                holding.getIsin(),
                holding.getName(),
                holding.getWeightPercent()
        );
    }
}
