package tech.techey.property.common.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tech.techey.property.user.User;

/**
 * Authenticated principal placed in the {@link org.springframework.security.core.context.SecurityContext}
 * by {@link JwtAuthFilter}. Carries just enough claims to authorize requests without
 * hitting the database on every call; controllers inject it via {@code @CurrentUser}.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String role;
    private final Long ownerId;
    private final String tier;

    public UserPrincipal(Long id, String email, String role, Long ownerId, String tier) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.ownerId = ownerId;
        this.tier = tier;
    }

    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getRole().name(), null, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
