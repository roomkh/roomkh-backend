package tech.techey.property.auth;

public record GoogleIdentity(String subject, String email, String name, String picture,
                             boolean authoritativeEmail) {}
