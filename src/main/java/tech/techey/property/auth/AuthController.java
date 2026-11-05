package tech.techey.property.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.techey.property.auth.dto.LoginRequest;
import tech.techey.property.auth.dto.OtpRequest;
import tech.techey.property.auth.dto.RefreshRequest;
import tech.techey.property.auth.dto.RegisterRequest;
import tech.techey.property.auth.dto.SendOtpRequest;
import tech.techey.property.auth.dto.TokenResponse;
import tech.techey.property.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/google")
    public ApiResponse<TokenResponse> google(@Valid @RequestBody tech.techey.property.auth.dto.GoogleLoginRequest request) {
        return ApiResponse.success(authService.googleLogin(googleOAuthService.exchange(request), request.portal()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success(null);
    }

    /**
     * Sends a phone OTP. {@link OtpService} is a logging stub this milestone —
     * real SMS delivery is out of scope (see class Javadoc).
     */
    @PostMapping("/otp/send")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request.phone());
        return ApiResponse.success(null);
    }

    @PostMapping("/otp/verify")
    public ApiResponse<Boolean> verifyOtp(@Valid @RequestBody OtpRequest request) {
        boolean verified = otpService.verifyOtp(request.phone(), request.code());
        return ApiResponse.success(verified);
    }
}
