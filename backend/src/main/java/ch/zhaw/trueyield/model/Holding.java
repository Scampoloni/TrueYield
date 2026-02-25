package ch.zhaw.trueyield.model;

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
@Document(collection = "holding")
public class Holding {

    @Id
    private String id;

    @NonNull
    @Indexed
    private String portfolioId;

    @NonNull
    private String symbol;

    private String isin;

    private String name;

    private Double weightPercent;
}
