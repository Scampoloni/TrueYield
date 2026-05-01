package ch.zhaw.trueyield.model;

import java.time.LocalDate;

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
@Document(collection = "evidence")
public class Evidence {

    @Id
    private String id;

    @NonNull
    @Indexed
    private String holdingId;

    private String sourceUrl;

    private String contentSnippet;

    private Double aiSentimentScore;

    private String sourceName;

    private LocalDate publishedAt;
}
