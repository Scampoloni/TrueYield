package ch.zhaw.trueyield.model;

import java.time.LocalDateTime;

import ch.zhaw.trueyield.model.enums.AuditStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document(collection = "auditReport")
public class AuditReport {

    @Id
    private String id;

    @NonNull
    @Indexed
    private String portfolioId;

    private String portfolioName;

    @NonNull
    private AuditStatus auditStatus;

    private String aiRiskSummary;

    private Integer aiRiskScore;

    private String aiRiskRationale;

    private String auditorId;

    private LocalDateTime createdAt;
}
