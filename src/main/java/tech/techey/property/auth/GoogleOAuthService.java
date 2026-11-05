package tech.techey.property.auth;

import java.time.Duration;
import java.util.Map;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import tech.techey.property.auth.dto.GoogleLoginRequest;
import tech.techey.property.common.exception.ApiException;
import tech.techey.property.common.exception.ErrorCode;

/** Only exchanges single-use codes; never accepts a browser-supplied identity or ID token. */
@Service
public class GoogleOAuthService {
    private final String clientId;
    private final String clientSecret;
    private final String ownerRedirect;
    private final String adminRedirect;
    private final NimbusJwtDecoder decoder;
    private final RestClient client;

    public GoogleOAuthService(
            @Value("${app.google.client-id:}") String clientId,
            @Value("${app.google.client-secret:}") String clientSecret,
            @Value("${app.google.owner-redirect-uri}") String ownerRedirect,
            @Value("${app.google.admin-redirect-uri}") String adminRedirect) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.ownerRedirect = ownerRedirect;
        this.adminRedirect = adminRedirect;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().requestFactory(factory).build();
        this.decoder = createDecoder(clientId, new RestTemplate(factory));
    }

    static NimbusJwtDecoder createDecoder(String clientId, RestOperations http) {
        var decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(http).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefault(), token -> {
                    String issuer = token.getClaimAsString("iss");
                    String azp = token.getClaimAsString("azp");
                    boolean valid = ("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))
                            && token.getAudience().contains(clientId)
                            && (azp == null || clientId.equals(azp))
                            && token.getExpiresAt() != null;
                    return valid ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                }));
        return decoder;
    }

    public GoogleIdentity exchange(GoogleLoginRequest request) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw ApiException.of(ErrorCode.GOOGLE_NOT_CONFIGURED);
        }
        var form = new LinkedMultiValueMap<String, String>() {
            @Override public String toString() { return "[OAuth credentials redacted]"; }
        };
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", request.code());
        form.add("code_verifier", request.codeVerifier());
        form.add("grant_type", "authorization_code");
        // Exact configured destinations, never a request-controlled redirect URI.
        form.add("redirect_uri", "ADMIN".equals(request.portal()) ? adminRedirect : ownerRedirect);
        try {
            Map<?, ?> tokens = client.post().uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(Map.class);
            if (tokens == null || !(tokens.get("id_token") instanceof String idToken)) {
                throw new IllegalArgumentException("Missing ID token");
            }
            return identity(decoder.decode(idToken), request.nonce());
        } catch (RuntimeException ex) {
            // Never log codes, client secrets or Google's token response.
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    static GoogleIdentity identity(Jwt token, String nonce) {
        String subject = token.getSubject();
        String email = token.getClaimAsString("email");
        if (!nonce.equals(token.getClaimAsString("nonce"))
                || !Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"))
                || subject == null || subject.isBlank() || subject.length() > 190
                || email == null || email.length() > 190 || !email.contains("@")) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        email = email.toLowerCase(Locale.ROOT);
        String name = token.getClaimAsString("name");
        if (name == null || name.isBlank()) name = email.substring(0, email.indexOf('@'));
        name = name.substring(0, Math.min(name.length(), 120));
        String domain = token.getClaimAsString("hd");
        boolean authoritative = email.endsWith("@gmail.com") || (domain != null && !domain.isBlank());
        return new GoogleIdentity(subject, email, name, token.getClaimAsString("picture"), authoritative);
    }
}
