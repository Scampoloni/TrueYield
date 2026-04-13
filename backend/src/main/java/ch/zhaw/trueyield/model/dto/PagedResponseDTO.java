package ch.zhaw.trueyield.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedResponseDTO<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int pageNumber;
    private int pageSize;
}
