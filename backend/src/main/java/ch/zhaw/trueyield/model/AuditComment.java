package ch.zhaw.trueyield.model;

import java.time.LocalDateTime;

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
@Document(collection = "auditComment")
public class AuditComment {

    @Id
    private String id;

    @NonNull
    @Indexed
    private String auditReportId;

    @NonNull
    private String comment;

    @NonNull
    private String auditorId;

    private LocalDateTime createdAt;
}
