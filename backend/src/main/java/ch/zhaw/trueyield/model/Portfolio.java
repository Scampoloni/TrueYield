package ch.zhaw.trueyield.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document(collection = "portfolio")
public class Portfolio {

    @Id
    private String id;

    @NonNull
    private String name;

    private String description;

    @NonNull
    private String fundManagerId;
}
