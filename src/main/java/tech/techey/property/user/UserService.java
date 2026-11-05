package tech.techey.property.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.techey.property.common.exception.ApiException;
import tech.techey.property.common.exception.ErrorCode;
import tech.techey.property.user.dto.UserResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUuid(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getPhoneVerifiedAt() != null,
                user.getAvatarUrl(),
                user.getPreferredLanguage().name(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
