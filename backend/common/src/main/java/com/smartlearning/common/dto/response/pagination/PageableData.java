package com.smartlearning.common.dto.response.pagination;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

@Getter
@Builder
@FieldDefaults(
        level = AccessLevel.PRIVATE,
        makeFinal = true
)
public class PageableData {
    int page;
    int size;
    long totalElements;
    int totalPages;

    public static PageableData from(Page<?> page) {
        return PageableData.builder()
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
