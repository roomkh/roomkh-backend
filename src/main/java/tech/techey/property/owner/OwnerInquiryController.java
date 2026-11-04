package tech.techey.property.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;
import tech.techey.property.common.security.*;

/** Lead inbox: seeker inquiries on the signed-in owner's listings. */
@RestController
@RequestMapping("/api/v1/owner/inquiries")
public class OwnerInquiryController {
    private final JdbcTemplate db;
    public OwnerInquiryController(JdbcTemplate db) { this.db = db; }

    private static final String SELECT = """
        SELECT i.id, i.seeker_name AS "seekerName", i.seeker_phone AS "seekerPhone", i.message, i.status,
               i.replied_at AS "repliedAt", i.created_at AS "createdAt",
               l.id AS "listingId", l.title AS "listingTitle"
        FROM inquiries i JOIN listings l ON l.id=i.listing_id
        """;

    @GetMapping
    public ApiResponse<?> list(@CurrentUser UserPrincipal user, @RequestParam(defaultValue = "ALL") String status) {
        if (!Set.of("ALL", "NEW", "REPLIED", "CLOSED").contains(status))
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "status");
        return ApiResponse.success(status.equals("ALL")
            ? db.queryForList(SELECT + " WHERE l.owner_id=? ORDER BY i.created_at DESC LIMIT 200", user.getId())
            : db.queryForList(SELECT + " WHERE l.owner_id=? AND i.status=? ORDER BY i.created_at DESC LIMIT 200",
                user.getId(), status));
    }

    public record StatusChange(@NotNull @Pattern(regexp = "NEW|REPLIED|CLOSED") String status) {}

    @PatchMapping("/{id}")
    public ApiResponse<?> update(@CurrentUser UserPrincipal user, @PathVariable UUID id,
                                 @Valid @RequestBody StatusChange change) {
        int updated = db.update("""
            UPDATE inquiries i SET status=?,
                replied_at = CASE WHEN ?='REPLIED' THEN coalesce(i.replied_at, now()) ELSE i.replied_at END
            FROM listings l WHERE l.id=i.listing_id AND i.id=? AND l.owner_id=?
            """, change.status(), change.status(), id, user.getId());
        if (updated == 0) throw ApiException.of(ErrorCode.NOT_FOUND);
        return ApiResponse.success(db.queryForList(SELECT + " WHERE i.id=? AND l.owner_id=?", id, user.getId()).getFirst());
    }
}
