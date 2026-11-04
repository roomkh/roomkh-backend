package tech.techey.property.listing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;

/** Anonymous engagement from the public site; feeds the owner portal's analytics and inbox. */
@RestController
@RequestMapping("/api/v1/listings/{id}")
public class PublicEngagementController {
    private final JdbcTemplate db;
    public PublicEngagementController(JdbcTemplate db) { this.db = db; }

    public record Event(@NotNull @Pattern(regexp = "VIEW|CONTACT") String kind) {}

    public record Inquiry(@NotNull UUID id,
                          @NotBlank @Size(max = 120) String name,
                          @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$") String phone,
                          @NotBlank @Size(min = 5, max = 2000) String message) {}

    @PostMapping("/events")
    public ResponseEntity<?> event(@PathVariable UUID id, @Valid @RequestBody Event event,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitor) {
        requireActive(id);
        String visitorId = visitor == null || visitor.isBlank() ? null : visitor.strip();
        if (visitorId != null && visitorId.length() > 64) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "visitor");
        // A refresh or repeated tap from the same visitor within 30 minutes counts once.
        db.update("""
            INSERT INTO listing_events(listing_id, kind, visitor_id)
            SELECT ?, ?, ? WHERE ?::varchar IS NULL OR NOT EXISTS (
                SELECT 1 FROM listing_events WHERE listing_id=? AND kind=? AND visitor_id=?
                AND created_at > now() - interval '30 minutes')
            """, id, event.kind(), visitorId, visitorId, id, event.kind(), visitorId);
        return ResponseEntity.accepted().body(ApiResponse.success(null));
    }

    @PostMapping("/inquiries")
    public ResponseEntity<?> inquiry(@PathVariable UUID id, @Valid @RequestBody Inquiry inquiry) {
        requireActive(id);
        // The client-generated ID makes a retried submission idempotent.
        int inserted = db.update("""
            INSERT INTO inquiries(id, listing_id, seeker_name, seeker_phone, message)
            VALUES (?,?,?,?,?) ON CONFLICT (id) DO NOTHING
            """, inquiry.id(), id, inquiry.name().strip(), inquiry.phone().strip(), inquiry.message().strip());
        return ResponseEntity.status(inserted == 1 ? 201 : 200).body(ApiResponse.success(Map.of("id", inquiry.id())));
    }

    private void requireActive(UUID id) {
        Integer found = db.queryForObject("""
            SELECT count(*) FROM listings l JOIN users u ON u.id=l.owner_id
            WHERE l.id=? AND l.status='ACTIVE' AND u.status='ACTIVE' AND u.deleted_at IS NULL
            """, Integer.class, id);
        if (found == null || found == 0) throw ApiException.of(ErrorCode.NOT_FOUND);
    }
}
