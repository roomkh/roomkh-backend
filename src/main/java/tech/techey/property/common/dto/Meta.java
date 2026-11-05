package tech.techey.property.common.dto;

/**
 * Pagination metadata attached to {@link ApiResponse#paged}.
 */
public record Meta(int page, int perPage, long total, int totalPages) {
}
