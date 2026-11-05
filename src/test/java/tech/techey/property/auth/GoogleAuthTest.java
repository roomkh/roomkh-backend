package tech.techey.property.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import tech.techey.property.common.exception.ApiException;
import tech.techey.property.common.exception.ErrorCode;
import tech.techey.property.common.security.JwtService;
import tech.techey.property.user.*;

class GoogleAuthTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService auth = new AuthService(users, refresh, mock(RefreshTokenFamilyRevoker.class),
            mock(PasswordEncoder.class), jwt, mock(UserService.class));
    private final GoogleIdentity identity = new GoogleIdentity("google-123", "owner@gmail.com", "Owner", null, true);

    @BeforeEach void setup() {
        when(users.findByProviderAndProviderId(any(), anyString())).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(jwt.generateAccessToken(any())).thenReturn("access");
        when(jwt.generateOpaqueRefreshToken()).thenReturn("refresh");
        when(jwt.hashToken(any())).thenReturn("hash");
    }

    @Test void newOwnerGetsRealSessionAndGoogleSubject() {
        assertThat(auth.googleLogin(identity, "OWNER").accessToken()).isEqualTo("access");
        verify(users).save(argThat(user -> user.getRole() == Role.OWNER
                && user.getProvider() == AuthProvider.GOOGLE && user.getProviderId().equals("google-123")
                && user.getPasswordHash() == null));
        verify(refresh).save(any());
    }

    @Test void adminPortalNeverCreatesAnAccount() {
        assertError(() -> auth.googleLogin(identity, "ADMIN"), ErrorCode.FORBIDDEN);
        verify(users, never()).save(any());
        verifyNoInteractions(refresh);
    }

    @Test void existingAgentKeepsRoleAndPasswordWhenLinked() {
        User user = User.builder().email(identity.email()).role(Role.AGENT).passwordHash("existing").build();
        when(users.findByEmailIgnoreCase(identity.email())).thenReturn(Optional.of(user));
        auth.googleLogin(identity, "OWNER");
        assertThat(user.getRole()).isEqualTo(Role.AGENT);
        assertThat(user.getPasswordHash()).isEqualTo("existing");
        assertThat(user.getProviderId()).isEqualTo(identity.subject());
    }

    @Test void existingAdminCanSignInButOwnerCannotBecomeAdmin() {
        User user = User.builder().email(identity.email()).role(Role.OWNER).build();
        when(users.findByEmailIgnoreCase(identity.email())).thenReturn(Optional.of(user));
        assertError(() -> auth.googleLogin(identity, "ADMIN"), ErrorCode.FORBIDDEN);
        assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        user.setRole(Role.ADMIN);
        auth.googleLogin(identity, "ADMIN");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test void thirdPartyEmailCannotTakeOverExistingAccount() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(User.builder().build()));
        var thirdParty = new GoogleIdentity("123", "person@example.org", "Person", null, false);
        assertError(() -> auth.googleLogin(thirdParty, "OWNER"), ErrorCode.GOOGLE_ACCOUNT_CONFLICT);
        verifyNoInteractions(refresh);
    }

    @Test void differentSubjectCannotReplaceAnExistingGoogleIdentity() {
        User user = User.builder().provider(AuthProvider.GOOGLE).providerId("other-subject").build();
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        assertError(() -> auth.googleLogin(identity, "OWNER"), ErrorCode.GOOGLE_ACCOUNT_CONFLICT);
    }

    @Test void knownSubjectIsStableWhenEmailChanges() {
        User user = User.builder().provider(AuthProvider.GOOGLE).providerId(identity.subject())
                .email("old@gmail.com").role(Role.OWNER).build();
        when(users.findByProviderAndProviderId(AuthProvider.GOOGLE, identity.subject())).thenReturn(Optional.of(user));
        auth.googleLogin(identity, "OWNER");
        verify(users, never()).findByEmailIgnoreCase(anyString());
        assertThat(user.getEmail()).isEqualTo("old@gmail.com");
    }

    @Test void suspendedDeletedAndWrongRoleAccountsAreDenied() {
        User user = User.builder().role(Role.OWNER).status(UserStatus.SUSPENDED).build();
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        assertError(() -> auth.googleLogin(identity, "OWNER"), ErrorCode.FORBIDDEN);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeletedAt(Instant.now());
        assertError(() -> auth.googleLogin(identity, "OWNER"), ErrorCode.FORBIDDEN);
        user.setDeletedAt(null);
        user.setRole(Role.USER);
        assertError(() -> auth.googleLogin(identity, "OWNER"), ErrorCode.FORBIDDEN);
        verifyNoInteractions(refresh);
    }

    @Test void nonceAndVerifiedEmailAreRequired() {
        assertError(() -> GoogleOAuthService.identity(token("wrong", true), "nonce"), ErrorCode.INVALID_CREDENTIALS);
        assertError(() -> GoogleOAuthService.identity(token("nonce", false), "nonce"), ErrorCode.INVALID_CREDENTIALS);
        assertThat(GoogleOAuthService.identity(token("nonce", true), "nonce").subject()).isEqualTo("123");
    }

    private Jwt token(String nonce, boolean verified) {
        return Jwt.withTokenValue("test").header("alg", "RS256").subject("123")
                .claim("email", "test@gmail.com").claim("email_verified", verified).claim("nonce", nonce).build();
    }

    private void assertError(ThrowingRunnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }
    @FunctionalInterface interface ThrowingRunnable { void run() throws Exception; }
}
