package tech.techey.property.listing;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PublicListingQueries {
    private final JdbcTemplate db;
    public PublicListingQueries(JdbcTemplate db) { this.db = db; }
    static final String SELECT = """
        SELECT l.id, l.title, l.purpose, l.address, l.bedrooms, l.bathrooms,
               l.area, l.price, l.currency, l.description, l.amenities,
               l.contact_phone AS "contactPhone",
               -- Listings publish without admin review, so reviewed_at is usually null; fall back to creation time.
               COALESCE(l.reviewed_at, l.created_at) AS "postedAt",
               p.name_km AS "provinceKm", p.name_en AS "provinceEn",
               t.key AS "propertyType", t.name_km AS "typeKm", t.name_en AS "typeEn",
               u.full_name AS "ownerName", u.uuid AS "ownerId",
               u.contact_telegram AS "ownerTelegram", u.contact_facebook AS "ownerFacebook"
        FROM listings l JOIN provinces p ON p.id=l.province_id
        JOIN property_types t ON t.id=l.property_type_id JOIN users u ON u.id=l.owner_id
        WHERE l.status='ACTIVE' AND u.status='ACTIVE' AND u.deleted_at IS NULL
        """;

    public List<Map<String, Object>> findAll(UUID ownerId) {
        Object[] args = ownerId == null ? new Object[0] : new Object[]{ownerId};
        String ownerFilter = ownerId == null ? "" : " AND u.uuid=?";
        var listings = db.queryForList(SELECT + ownerFilter + " ORDER BY COALESCE(l.reviewed_at, l.created_at) DESC, l.id", args);
        // One photo metadata query for the whole catalogue; image bytes are fetched separately.
        var photos = db.queryForList("""
            SELECT p.id, p.listing_id FROM listing_photos p JOIN listings l ON l.id=p.listing_id
            JOIN users u ON u.id=l.owner_id
            WHERE l.status='ACTIVE' AND u.status='ACTIVE' AND u.deleted_at IS NULL
            """ + ownerFilter + " ORDER BY p.position", args);
        var byListing = new HashMap<Object, List<Map<String, Object>>>();
        for (var photo : photos) byListing.computeIfAbsent(photo.get("listing_id"), key -> new ArrayList<>())
            .add(Map.of("id", photo.get("id")));
        for (var listing : listings) listing.put("photos", byListing.getOrDefault(listing.get("id"), List.of()));
        return listings;
    }
}
