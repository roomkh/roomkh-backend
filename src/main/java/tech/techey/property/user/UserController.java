package tech.techey.property.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.security.CurrentUser;
import tech.techey.property.common.security.UserPrincipal;
import tech.techey.property.user.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@CurrentUser UserPrincipal principal) {
        return ApiResponse.success(userService.getProfile(principal.getId()));
    }
}
