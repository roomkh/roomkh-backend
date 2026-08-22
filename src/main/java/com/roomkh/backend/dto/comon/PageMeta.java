package com.roomkh.backend.dto.comon;

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

    public static PageMeta from(Page<?> page) {
        return PageMeta.builder()
                .currentPage(page.getNumber() + 1)
                .perPage(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}