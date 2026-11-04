package tech.techey.property.listing;

import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;

/** Public projection: never expose private account or moderation data. */
@RestController
@RequestMapping("/api/v1/listings")
public class PublicListingController {
    private final JdbcTemplate db;
    private final PublicListingQueries queries;
    public PublicListingController(JdbcTemplate db, PublicListingQueries queries) { this.db = db; this.queries = queries; }
    @GetMapping
    public ApiResponse<?> list() { return ApiResponse.success(queries.findAll(null)); }

    @GetMapping("/suggestions")
    public ApiResponse<?> suggestions(@RequestParam(defaultValue = "") String q) {
        String term = q.strip();
        if (term.length() < 2) return ApiResponse.success(List.of());
        if (term.length() > 100) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "q");
        return ApiResponse.success(db.queryForList("""
            WITH available AS (
                SELECT l.title, l.address, p.name_en, p.name_km
                FROM listings l JOIN users u ON u.id=l.owner_id JOIN provinces p ON p.id=l.province_id
                WHERE l.status='ACTIVE' AND u.status='ACTIVE' AND u.deleted_at IS NULL
            ), candidates AS (
                SELECT left(title,100) AS value, 'listing' AS kind FROM available
                UNION ALL SELECT left(address,100), 'location' FROM available
                UNION ALL SELECT name_en, 'location' FROM available
                UNION ALL SELECT name_km, 'location' FROM available
            )
            SELECT value, kind FROM candidates
            WHERE strpos(lower(value), lower(?)) > 0
            GROUP BY value, kind
            ORDER BY (strpos(lower(value), lower(?)) = 1) DESC, count(*) DESC, kind, value
            LIMIT 8
            """, term, term));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable UUID id) {
        var rows = db.queryForList(PublicListingQueries.SELECT + " AND l.id=?", id);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var listing = rows.getFirst();
        listing.put("photos", db.queryForList("SELECT id FROM listing_photos WHERE listing_id=? ORDER BY position", id));
        return ApiResponse.success(listing);
    }

    @GetMapping("/{listingId}/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable UUID listingId, @PathVariable UUID photoId) {
        var rows = db.queryForList("""
            SELECT p.content, p.content_type FROM listing_photos p JOIN listings l ON l.id=p.listing_id
            JOIN users u ON u.id=l.owner_id WHERE p.id=? AND l.id=? AND l.status='ACTIVE'
            AND u.status='ACTIVE' AND u.deleted_at IS NULL
            """, photoId, listingId);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var row = rows.getFirst();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) row.get("content_type")))
            .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore()).body((byte[]) row.get("content"));
    }
}
