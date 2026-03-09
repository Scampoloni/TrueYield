package ch.zhaw.trueyield.model.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuditReportAggregationDTO {

    private String id;

    private String count;

    private List<String> itemIds;
}
