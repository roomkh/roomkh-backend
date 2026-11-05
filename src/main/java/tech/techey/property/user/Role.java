package tech.techey.property.user;

/** Platform roles per PRD §3. Drives {@code ROLE_*} authorities in Spring Security. */
public enum Role {
    USER,
    OWNER,
    AGENT,
    MODERATOR,
    ADMIN
}
