package tech.techey.property.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tech.techey.property.user.Role;
import tech.techey.property.user.User;
import tech.techey.property.user.UserRepository;
import tech.techey.property.user.UserStatus;

/**
 * Seeds a handful of fixed dev accounts (one per privileged role) for local development so
 * there's always a real, backend-authenticated user to log in as. There is no self-service way
 * to become ADMIN or OWNER via the public API ({@code AuthService.register} always assigns
 * {@code Role.USER}), so this fills the gap without opening a privilege-escalation path in real
 * environments — it only runs on the {@code local} profile and is idempotent (skips whatever
 * already exists).
 *
 * <p>Credentials are intentionally fixed and documented in {@code backend/README.md} — this
 * is dev-only bootstrap data, not a secret.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String DEV_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedIfAbsent("RoomKH Super Admin", "admin@roomkh.com", Role.ADMIN);
        seedIfAbsent("RoomKH Dev Owner", "owner@roomkh.com", Role.OWNER);
    }

    private void seedIfAbsent(String fullName, String email, Role role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(DEV_PASSWORD))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        log.info("Seeded local dev {} account: {} (see backend/README.md for the password)",
                role, email);
    }
}
