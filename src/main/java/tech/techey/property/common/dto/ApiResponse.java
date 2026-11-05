package tech.techey.property.common.dto;

import java.util.List;

/**
 * Standard response envelope for every REST endpoint under {@code /api/v1/}.
 *
 * @param success whether the request succeeded
 * @param data    the payload (null on error)
 * @param meta    pagination metadata (only present for {@link #paged})
 * @param errors  list of errors (only present on failure)
 */
public record ApiResponse<T>(boolean success, T data, Meta meta, List<ApiError> errors) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> paged(T data, Meta meta) {
        return new ApiResponse<>(true, data, meta, null);
    }

    public static <T> ApiResponse<T> error(List<ApiError> errors) {
        return new ApiResponse<>(false, null, null, errors);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, null, List.of(error));
    }
}
