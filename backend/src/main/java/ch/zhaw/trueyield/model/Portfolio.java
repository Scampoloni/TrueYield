package ch.zhaw.trueyield.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "portfolio")
public class Portfolio {

    @Id
    private String id;

    private String name;

    private String description;

    private String fundManagerId;

    public Portfolio(String name, String fundManagerId) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Error: Portfolio name must not be blank.");
        }
        if (name.length() > 100) {
            throw new RuntimeException("Error: Portfolio name must not exceed 100 characters.");
        }
        this.name = name;
        this.fundManagerId = fundManagerId;
    }
}
