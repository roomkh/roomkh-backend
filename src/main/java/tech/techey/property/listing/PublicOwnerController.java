package tech.techey.property.listing;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;

@RestController
@RequestMapping("/api/v1/owners")
public class PublicOwnerController {
    private final JdbcTemplate db;
    private final PublicListingQueries listings;
    public PublicOwnerController(JdbcTemplate db, PublicListingQueries listings) {
        this.db = db; this.listings = listings;
    }

    @GetMapping("/{id}/public")
    public ApiResponse<?> profile(@PathVariable UUID id) {
        var rows = db.queryForList("""
            SELECT uuid AS id, full_name AS name, created_at AS "joinedAt", avatar_url AS "avatarUrl",
                   contact_phone AS phone, contact_telegram AS telegram, contact_facebook AS facebook
            FROM users WHERE uuid=? AND role IN ('OWNER','AGENT','ADMIN')
            AND status='ACTIVE' AND deleted_at IS NULL
            """, id);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var profile = rows.getFirst();
        var properties = listings.findAll(id);
        profile.put("listingCount", properties.size());
        profile.put("listings", properties);
        return ApiResponse.success(profile);
    }
}
