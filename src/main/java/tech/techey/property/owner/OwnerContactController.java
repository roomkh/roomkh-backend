package tech.techey.property.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;
import tech.techey.property.common.security.*;

/**
 * Contact channels shown on the owner's public listings and profile.
 * Values are normalized on save so the public site can only ever render a
 * plain phone number, a t.me link, or a facebook.com link.
 */
@RestController
@RequestMapping("/api/v1/owner/contacts")
public class OwnerContactController {
    private final JdbcTemplate db;
    public OwnerContactController(JdbcTemplate db) { this.db = db; }

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{6,32}$");
    private static final Pattern TELEGRAM_USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{4,31}$");
    private static final Pattern FACEBOOK_NAME = Pattern.compile("^[A-Za-z0-9.\\-]{2,80}$");
    private static final Set<String> FACEBOOK_HOSTS = Set.of(
        "facebook.com", "www.facebook.com", "m.facebook.com", "web.facebook.com", "fb.com", "www.fb.com", "fb.me");

    public record Contacts(@Size(max = 64) String phone, @Size(max = 128) String telegram, @Size(max = 255) String facebook) {}

    @GetMapping
    public ApiResponse<?> get(@CurrentUser UserPrincipal user) {
        return ApiResponse.success(db.queryForMap("""
            SELECT contact_phone AS phone, contact_telegram AS telegram, contact_facebook AS facebook
            FROM users WHERE id=?
            """, user.getId()));
    }

    @PutMapping
    public ApiResponse<?> update(@CurrentUser UserPrincipal user, @Valid @RequestBody Contacts contacts) {
        String phone = phone(contacts.phone()), telegram = telegram(contacts.telegram()), facebook = facebook(contacts.facebook());
        db.update("UPDATE users SET contact_phone=?, contact_telegram=?, contact_facebook=?, updated_at=now() WHERE id=?",
            phone, telegram, facebook, user.getId());
        var result = new LinkedHashMap<String, Object>();
        result.put("phone", phone);
        result.put("telegram", telegram);
        result.put("facebook", facebook);
        return ApiResponse.success(result);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    static String phone(String raw) {
        String value = blankToNull(raw);
        if (value == null) return null;
        if (!PHONE.matcher(value).matches()) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "phone");
        return value;
    }

    /** A username ("@roomkh", "t.me/roomkh") or a phone number, stored as a t.me path segment. */
    static String telegram(String raw) {
        String value = blankToNull(raw);
        if (value == null) return null;
        value = value.replaceFirst("(?i)^(https?://)?(www\\.)?(t\\.me|telegram\\.me)/", "").replaceFirst("^@", "");
        if (TELEGRAM_USERNAME.matcher(value).matches()) return value;
        if (PHONE.matcher(value).matches()) {
            String digits = value.replaceAll("\\D", "");
            // Local Cambodian numbers (012 …) need the +855 country code for a t.me link.
            if (!value.startsWith("+") && digits.startsWith("0")) digits = "855" + digits.substring(1);
            if (digits.length() >= 8 && digits.length() <= 15) return "+" + digits;
        }
        throw ApiException.of(ErrorCode.VALIDATION_ERROR, "telegram");
    }

    /** A facebook.com profile/page URL, or a bare page name, stored as an https facebook.com URL. */
    static String facebook(String raw) {
        String value = blankToNull(raw);
        if (value == null) return null;
        if (FACEBOOK_NAME.matcher(value).matches() && !value.contains("facebook.") && !value.startsWith("fb."))
            return "https://www.facebook.com/" + value;
        try {
            var uri = URI.create(value.matches("(?i)^https?://.*") ? value : "https://" + value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (FACEBOOK_HOSTS.contains(host) && path.length() > 1 && uri.getRawFragment() == null) {
                String url = "https://" + host + path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                if (url.length() <= 255) return url;
            }
        } catch (IllegalArgumentException ignored) { }
        throw ApiException.of(ErrorCode.VALIDATION_ERROR, "facebook");
    }
}
