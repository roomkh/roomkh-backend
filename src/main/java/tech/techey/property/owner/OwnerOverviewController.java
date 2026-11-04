package tech.techey.property.owner;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;
import tech.techey.property.common.security.*;

/** Account-level numbers for the owner dashboard, analytics and billing screens (PRD §6.5). */
@RestController
@RequestMapping("/api/v1/owner/overview")
public class OwnerOverviewController {
    private final JdbcTemplate db;
    public OwnerOverviewController(JdbcTemplate db) { this.db = db; }

    /** PRD §6.1 verification tiers; limits are informational until plan enforcement ships. */
    static final int BASIC_ACTIVE_LIMIT = 2, VERIFIED_ACTIVE_LIMIT = 10;
    private static final String TODAY = "(now() AT TIME ZONE 'Asia/Phnom_Penh')::date";

    @GetMapping
    public ApiResponse<?> overview(@CurrentUser UserPrincipal user, @RequestParam(defaultValue = "7") int days) {
        if (days != 7 && days != 30) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "days");
        long ownerId = user.getId();
        var profiles = db.queryForList("""
            SELECT full_name AS "fullName", email, phone, phone_verified_at IS NOT NULL AS "phoneVerified",
                   avatar_url AS "avatarUrl", preferred_language AS "preferredLanguage", role, created_at AS "createdAt"
            FROM users WHERE id=? AND deleted_at IS NULL
            """, ownerId);
        if (profiles.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);

        var listings = new LinkedHashMap<String, Object>(Map.of("DRAFT", 0L, "PENDING_REVIEW", 0L, "ACTIVE", 0L, "REJECTED", 0L));
        db.queryForList("SELECT status, count(*) AS n FROM listings WHERE owner_id=? GROUP BY status", ownerId)
            .forEach(row -> listings.put((String) row.get("status"), row.get("n")));

        var engagement = db.queryForMap("""
            SELECT count(*) FILTER (WHERE e.kind='VIEW' AND e.created_at > now() - interval '30 days') AS "views",
                   count(*) FILTER (WHERE e.kind='VIEW' AND e.created_at <= now() - interval '30 days'
                                    AND e.created_at > now() - interval '60 days') AS "viewsPrevious",
                   count(*) FILTER (WHERE e.kind='CONTACT' AND e.created_at > now() - interval '30 days') AS "contacts",
                   count(*) FILTER (WHERE e.kind='CONTACT' AND e.created_at <= now() - interval '30 days'
                                    AND e.created_at > now() - interval '60 days') AS "contactsPrevious"
            FROM listing_events e JOIN listings l ON l.id=e.listing_id WHERE l.owner_id=?
            """, ownerId);
        engagement.putAll(db.queryForMap("""
            SELECT count(*) FILTER (WHERE i.created_at > now() - interval '30 days') AS "inquiries",
                   count(*) FILTER (WHERE i.created_at <= now() - interval '30 days'
                                    AND i.created_at > now() - interval '60 days') AS "inquiriesPrevious",
                   count(*) FILTER (WHERE i.status='NEW') AS "openInquiries"
            FROM inquiries i JOIN listings l ON l.id=i.listing_id WHERE l.owner_id=?
            """, ownerId));

        // One row per local (Phnom Penh) calendar day, zero-filled so the chart never has gaps.
        var daily = db.queryForList("""
            WITH days AS (
                SELECT generate_series(%1$s - (?::int - 1), %1$s, interval '1 day')::date AS day
            ), events AS (
                SELECT (e.created_at AT TIME ZONE 'Asia/Phnom_Penh')::date AS day,
                       count(*) FILTER (WHERE e.kind='VIEW') AS views,
                       count(*) FILTER (WHERE e.kind='CONTACT') AS contacts
                FROM listing_events e JOIN listings l ON l.id=e.listing_id
                WHERE l.owner_id=? AND e.created_at > now() - make_interval(days => ?::int + 1) GROUP BY 1
            ), leads AS (
                SELECT (i.created_at AT TIME ZONE 'Asia/Phnom_Penh')::date AS day, count(*) AS inquiries
                FROM inquiries i JOIN listings l ON l.id=i.listing_id
                WHERE l.owner_id=? AND i.created_at > now() - make_interval(days => ?::int + 1) GROUP BY 1
            )
            SELECT to_char(d.day, 'YYYY-MM-DD') AS date, coalesce(e.views, 0) AS views,
                   coalesce(e.contacts, 0) AS contacts, coalesce(q.inquiries, 0) AS inquiries
            FROM days d LEFT JOIN events e USING (day) LEFT JOIN leads q USING (day) ORDER BY d.day
            """.formatted(TODAY), days, ownerId, days, ownerId, days);

        var perListing = db.queryForList("""
            SELECT l.id, l.title, l.status, l.purpose, l.price, l.currency, l.area, l.created_at AS "createdAt",
                   p.name_en AS "provinceEn", p.name_km AS "provinceKm",
                   (SELECT id FROM listing_photos f WHERE f.listing_id=l.id ORDER BY position LIMIT 1) AS "coverPhotoId",
                   (SELECT count(*) FROM listing_photos f WHERE f.listing_id=l.id) AS "photoCount",
                   (SELECT count(*) FROM listing_events e WHERE e.listing_id=l.id AND e.kind='VIEW') AS "views",
                   (SELECT count(*) FROM listing_events e WHERE e.listing_id=l.id AND e.kind='VIEW'
                    AND e.created_at > now() - interval '30 days') AS "views30",
                   (SELECT count(*) FROM listing_events e WHERE e.listing_id=l.id AND e.kind='CONTACT') AS "contacts",
                   (SELECT count(*) FROM inquiries i WHERE i.listing_id=l.id) AS "inquiries"
            FROM listings l JOIN provinces p ON p.id=l.province_id
            WHERE l.owner_id=? ORDER BY "views30" DESC, l.created_at DESC
            """, ownerId);

        var activity = db.queryForList("""
            SELECT * FROM (
                SELECT 'INQUIRY' AS type, l.id AS "listingId", l.title AS "listingTitle",
                       i.seeker_name AS detail, i.created_at AS at
                FROM inquiries i JOIN listings l ON l.id=i.listing_id WHERE l.owner_id=?
                UNION ALL
                SELECT r.decision, l.id, l.title, r.reason, r.created_at
                FROM listing_reviews r JOIN listings l ON l.id=r.listing_id WHERE l.owner_id=?
                UNION ALL
                SELECT 'CREATED', l.id, l.title, l.status, l.created_at FROM listings l WHERE l.owner_id=?
            ) a ORDER BY at DESC LIMIT 8
            """, ownerId, ownerId, ownerId);

        String idStatus = OwnerVerificationController.latestStatus(db, ownerId, "ID");
        boolean verified = "APPROVED".equals(idStatus);
        var result = new LinkedHashMap<String, Object>();
        result.put("profile", profiles.getFirst());
        result.put("listings", listings);
        result.put("engagement", engagement);
        result.put("daily", daily);
        result.put("perListing", perListing);
        result.put("activity", activity);
        result.put("verification", Map.of("idStatus", idStatus == null ? "NONE" : idStatus,
            "tier", verified ? "VERIFIED" : "BASIC",
            "activeLimit", verified ? VERIFIED_ACTIVE_LIMIT : BASIC_ACTIVE_LIMIT));
        return ApiResponse.success(result);
    }
}
