package tech.techey.property.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tech.techey.property.common.dto.ApiError;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.ErrorCode;

/**
 * Replaces Spring Security's default 403 HTML/empty body with the standard
 * {@link ApiResponse} JSON error envelope.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(List.of(
                new ApiError(ErrorCode.FORBIDDEN.name(), null, "You do not have permission to access this resource")));

        objectMapper.writeValue(response.getWriter(), body);
    }
}
