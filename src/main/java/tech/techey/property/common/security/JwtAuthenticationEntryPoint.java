package tech.techey.property.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tech.techey.property.common.dto.ApiError;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.ErrorCode;

/**
 * Replaces Spring Security's default 401 HTML/empty body with the standard
 * {@link ApiResponse} JSON error envelope, so API clients always get a consistent shape.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(List.of(
                new ApiError(ErrorCode.UNAUTHORIZED.name(), null, "Authentication is required to access this resource")));

        objectMapper.writeValue(response.getWriter(), body);
    }
}
