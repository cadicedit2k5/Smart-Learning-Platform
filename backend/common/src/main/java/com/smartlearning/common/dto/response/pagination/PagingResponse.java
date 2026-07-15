package com.smartlearning.common.dto.response.pagination;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@FieldDefaults(
        level = AccessLevel.PRIVATE,
        makeFinal = true
)
public class PagingResponse<T> {

    @Builder.Default
    List<T> content = List.of();
    PageableData pageable;

    public static <T> PagingResponse<T> from(Page<T> page) {

        return PagingResponse.<T>builder()
                .content(List.copyOf(page.getContent()))
                .pageable(PageableData.from(page))
                .build();
    }
}
