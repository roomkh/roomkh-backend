package com.roomkh.backend.dto.comon;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMeta {
    private int currentPage;
    private int perPage;
    private long total;
    private int totalPages;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object statusCounts;

    public static PageMeta from(Page<?> page) {
        return PageMeta.builder()
                .currentPage(page.getNumber() + 1)
                .perPage(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public static PageMeta from(Page<?> page, Object statusCounts) {
        return PageMeta.builder()
                .currentPage(page.getNumber() + 1)
                .perPage(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .statusCounts(statusCounts)
                .build();
    }
}