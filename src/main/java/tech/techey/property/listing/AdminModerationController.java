package tech.techey.property.listing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;
import tech.techey.property.common.security.*;

@RestController
@RequestMapping("/api/v1/admin/moderation")
public class AdminModerationController {
    private final JdbcTemplate db;
    public AdminModerationController(JdbcTemplate db) { this.db = db; }
    private static final String SELECT = """
        SELECT l.id, l.title, l.purpose, l.address, l.bedrooms, l.bathrooms,
               l.area, l.price, l.currency, l.description, l.amenities, l.status,
               l.contact_phone AS "contactPhone", l.created_at AS "createdAt",
               l.rejection_reason AS "rejectionReason", l.reviewed_at AS "reviewedAt",
               u.full_name AS "ownerName", u.email AS "ownerEmail",
               r.full_name AS "reviewerName",
               p.name_km AS "provinceKm", p.name_en AS "provinceEn",
               t.name_km AS "typeKm", t.name_en AS "typeEn"
        FROM listings l JOIN users u ON u.id=l.owner_id
        JOIN provinces p ON p.id=l.province_id JOIN property_types t ON t.id=l.property_type_id
        LEFT JOIN users r ON r.id=l.reviewed_by
        """;

    @GetMapping
    public ApiResponse<?> queue(@RequestParam(defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(defaultValue = "0") int page) {
        if (!Set.of("PENDING_REVIEW", "ACTIVE", "REJECTED").contains(status) || page < 0 || page > 100000)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR);
        var counts = new HashMap<String, Long>();
        for (String state : List.of("PENDING_REVIEW", "ACTIVE", "REJECTED"))
            counts.put(state, db.queryForObject("SELECT count(*) FROM listings WHERE status=?", Long.class, state));
        return ApiResponse.success(Map.of("items", db.queryForList(SELECT +
            " WHERE l.status=? ORDER BY l.created_at ASC, l.id LIMIT 20 OFFSET ?", status, page * 20),
            "counts", counts, "total", counts.get(status), "page", page, "pageSize", 20));
    }

    private Map<String, Object> detail(UUID id) {
        var rows = db.queryForList(SELECT + " WHERE l.id=? AND l.status <> 'DRAFT'", id);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var result = rows.getFirst();
        result.put("photos", db.queryForList("SELECT id FROM listing_photos WHERE listing_id=? ORDER BY position", id));
        return result;
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) { return ApiResponse.success(detail(id)); }

    public record Decision(@NotNull @Pattern(regexp = "APPROVE|REJECT") String decision,
                           @Size(max = 1000) String reason) {}

    @PostMapping("/{id}/decision")
    @Transactional
    public ApiResponse<?> decide(@CurrentUser UserPrincipal user, @PathVariable UUID id,
                                @Valid @RequestBody Decision decision) {
        String reason = decision.reason() == null ? "" : decision.reason().trim();
        if (decision.decision().equals("REJECT") && reason.length() < 10)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "reason");
        var rows = db.queryForList("SELECT status FROM listings WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        String target = decision.decision().equals("APPROVE") ? "ACTIVE" : "REJECTED";
        String current = (String) rows.getFirst().get("status");
        // Retrying a completed decision must not append a second audit record.
        if (current.equals(target)) return ApiResponse.success(detail(id));
        if (!current.equals("PENDING_REVIEW")) throw ApiException.of(ErrorCode.CONFLICT, "status");
        db.update("UPDATE listings SET status=?, rejection_reason=?, reviewed_at=now(), reviewed_by=? WHERE id=?",
            target, target.equals("REJECTED") ? reason : null, user.getId(), id);
        db.update("INSERT INTO listing_reviews(listing_id,reviewer_id,decision,reason) VALUES (?,?,?,?)",
            id, user.getId(), decision.decision(), target.equals("REJECTED") ? reason : null);
        return ApiResponse.success(detail(id));
    }

    @GetMapping("/{listingId}/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable UUID listingId, @PathVariable UUID photoId) {
        var rows = db.queryForList("""
            SELECT p.content, p.content_type FROM listing_photos p JOIN listings l ON l.id=p.listing_id
            WHERE p.id=? AND l.id=? AND l.status <> 'DRAFT'
            """, photoId, listingId);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var row = rows.getFirst();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) row.get("content_type")))
            .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore()).body((byte[]) row.get("content"));
    }
}
