package tech.techey.property.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GoogleTokenValidationTest {
    private RSAKey key;
    private NimbusJwtDecoder decoder;

    @BeforeEach void setup() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("google-key").generate();
        var http = new RestTemplate();
        var server = MockRestServiceServer.createServer(http);
        server.expect(requestTo("https://www.googleapis.com/oauth2/v3/certs"))
                .andRespond(withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
        decoder = GoogleOAuthService.createDecoder("roomkh-client", http);
    }

    @Test void signedGoogleTokenIsAccepted() throws Exception {
        assertThat(decoder.decode(sign(claims(), key)).getSubject()).isEqualTo("123");
    }

    @Test void wrongAudienceIsRejected() throws Exception {
        String token = sign(claims().audience("other-app"), key);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test void wrongIssuerIsRejected() throws Exception {
        String token = sign(claims().issuer("https://attacker.example"), key);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test void expiredTokenIsRejected() throws Exception {
        String token = sign(claims().expirationTime(Date.from(Instant.now().minusSeconds(120))), key);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test void missingExpiryIsRejected() throws Exception {
        String token = sign(claims().expirationTime(null), key);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test void wrongAuthorizedPartyIsRejected() throws Exception {
        String token = sign(claims().claim("azp", "other-client"), key);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test void forgedSignatureIsRejected() throws Exception {
        var forgedKey = new RSAKeyGenerator(2048).keyID("google-key").generate();
        String token = sign(claims(), forgedKey);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer("https://accounts.google.com").audience("roomkh-client")
                .subject("123").issueTime(Date.from(Instant.now().minusSeconds(300)))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)));
    }

    private String sign(JWTClaimsSet.Builder claims, RSAKey signingKey) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("google-key").build(), claims.build());
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
