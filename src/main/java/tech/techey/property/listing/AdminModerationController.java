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
               l.property_type_id AS "propertyTypeId", l.province_id AS "provinceId",
               (SELECT ph.id FROM listing_photos ph WHERE ph.listing_id=l.id ORDER BY ph.position LIMIT 1) AS "coverPhotoId",
               u.full_name AS "ownerName", u.email AS "ownerEmail",
               r.full_name AS "reviewerName",
               p.name_km AS "provinceKm", p.name_en AS "provinceEn",
               t.name_km AS "typeKm", t.name_en AS "typeEn"
        FROM listings l JOIN users u ON u.id=l.owner_id
        JOIN provinces p ON p.id=l.province_id JOIN property_types t ON t.id=l.property_type_id
        LEFT JOIN users r ON r.id=l.reviewed_by
        """;

    private static final List<String> STATUSES = List.of("DRAFT", "PENDING_REVIEW", "ACTIVE", "REJECTED");

    /**
     * Every owner listing, filterable by status (or ALL) and a free-text search over title, owner and phone.
     * Defaults keep the original moderation-queue behaviour: pending items, oldest first.
     */
    @GetMapping
    public ApiResponse<?> queue(@RequestParam(defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "oldest") String sort) {
        if (!(status.equals("ALL") || STATUSES.contains(status)) || page < 0 || page > 100000
                || q.length() > 100 || !Set.of("oldest", "newest").contains(sort))
            throw ApiException.of(ErrorCode.VALIDATION_ERROR);
        var where = new StringBuilder(" WHERE TRUE");
        var args = new ArrayList<Object>();
        String search = q.trim();
        if (!search.isEmpty()) {
            String like = "%" + search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append(" AND (l.title ILIKE ? OR u.full_name ILIKE ? OR u.email ILIKE ? OR l.contact_phone ILIKE ?)");
            args.addAll(List.of(like, like, like, like));
        }
        var counts = new HashMap<String, Long>();
        STATUSES.forEach(s -> counts.put(s, 0L));
        for (var row : db.queryForList("SELECT l.status, count(*) AS n FROM listings l JOIN users u ON u.id=l.owner_id"
                + where + " GROUP BY l.status", args.toArray()))
            counts.put((String) row.get("status"), ((Number) row.get("n")).longValue());
        counts.put("ALL", counts.values().stream().mapToLong(Long::longValue).sum());
        if (!status.equals("ALL")) {
            where.append(" AND l.status=?");
            args.add(status);
        }
        args.add(page * 20);
        String order = sort.equals("newest") ? "DESC" : "ASC";
        var items = db.queryForList(SELECT + where + " ORDER BY l.created_at " + order + ", l.id LIMIT 20 OFFSET ?",
            args.toArray());
        return ApiResponse.success(Map.of("items", items, "counts", counts, "total", counts.get(status),
            "page", page, "pageSize", 20));
    }

    @GetMapping("/options")
    public ApiResponse<?> options() {
        return ApiResponse.success(Map.of(
            "provinces", db.queryForList("SELECT id, name_km AS \"nameKm\", name_en AS \"nameEn\" FROM provinces ORDER BY code"),
            "propertyTypes", db.queryForList("SELECT id, name_km AS \"nameKm\", name_en AS \"nameEn\" FROM property_types ORDER BY id")));
    }

    /** Admins can correct any owner listing's content. Status is changed only through decisions. */
    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateListingRequest request) {
        if (db.queryForObject("SELECT count(*) FROM property_types WHERE id=?", Integer.class, request.propertyTypeId()) != 1)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "propertyTypeId");
        if (db.queryForObject("SELECT count(*) FROM provinces WHERE id=?", Integer.class, request.provinceId()) != 1)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "provinceId");
        int updated = db.update("""
            UPDATE listings SET title=?, purpose=?, property_type_id=?, province_id=?, address=?, bedrooms=?,
                bathrooms=?, area=?, price=?, currency=?, description=?, contact_phone=?, amenities=?
            WHERE id=?
            """, request.title().trim(), request.purpose(), request.propertyTypeId(), request.provinceId(),
            request.address().trim(), request.bedrooms(), request.bathrooms(), request.area(), request.price(),
            request.currency(), request.description().trim(), request.contactPhone().trim(), request.amenities(), id);
        if (updated == 0) throw ApiException.of(ErrorCode.NOT_FOUND);
        return ApiResponse.success(detail(id));
    }

    /** Permanently removes a listing; photos, reviews, views and inquiries cascade with it. */
    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<?> delete(@PathVariable UUID id) {
        if (db.update("DELETE FROM listings WHERE id=?", id) == 0) throw ApiException.of(ErrorCode.NOT_FOUND);
        return ApiResponse.success(Map.of("id", id));
    }

    private Map<String, Object> detail(UUID id) {
        var rows = db.queryForList(SELECT + " WHERE l.id=?", id);
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
        // Listings publish without approval, so admins can also take down (reject) a live listing.
        boolean takedown = current.equals("ACTIVE") && target.equals("REJECTED");
        if (!current.equals("PENDING_REVIEW") && !takedown) throw ApiException.of(ErrorCode.CONFLICT, "status");
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
            WHERE p.id=? AND l.id=?
            """, photoId, listingId);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var row = rows.getFirst();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) row.get("content_type")))
            .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore()).body((byte[]) row.get("content"));
    }
}
