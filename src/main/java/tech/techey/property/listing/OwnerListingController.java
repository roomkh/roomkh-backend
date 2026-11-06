package tech.techey.property.listing;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;
import tech.techey.property.common.security.*;

@RestController
@RequestMapping("/api/v1/owner/listings")
public class OwnerListingController {
    private final JdbcTemplate db;
    public OwnerListingController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/options")
    public ApiResponse<?> options() {
        return ApiResponse.success(Map.of(
            "provinces", db.queryForList("SELECT id, name_km AS \"nameKm\", name_en AS \"nameEn\" FROM provinces ORDER BY code"),
            "propertyTypes", db.queryForList("SELECT id, name_km AS \"nameKm\", name_en AS \"nameEn\" FROM property_types ORDER BY id")));
    }

    private static final String SELECT = """
        SELECT l.id, l.title, l.purpose, l.address, l.bedrooms, l.bathrooms,
               l.area, l.price, l.currency, l.description, l.amenities, l.status,
               l.contact_phone AS "contactPhone", l.created_at AS "createdAt",
               l.rejection_reason AS "rejectionReason", l.reviewed_at AS "reviewedAt",
               p.name_km AS "provinceKm", p.name_en AS "provinceEn",
               t.name_km AS "typeKm", t.name_en AS "typeEn"
        FROM listings l JOIN provinces p ON p.id=l.province_id
        JOIN property_types t ON t.id=l.property_type_id
        """;

    @GetMapping
    public ApiResponse<?> list(@CurrentUser UserPrincipal user) {
        return ApiResponse.success(db.queryForList(SELECT + " WHERE l.owner_id=? ORDER BY l.created_at DESC", user.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@CurrentUser UserPrincipal user, @PathVariable UUID id) {
        return ApiResponse.success(owned(id, user.getId()));
    }

    private Map<String, Object> owned(UUID id, long ownerId) {
        var rows = db.queryForList(SELECT + " WHERE l.id=? AND l.owner_id=?", id, ownerId);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var result = rows.getFirst();
        result.put("photos", db.queryForList("SELECT id FROM listing_photos WHERE listing_id=? ORDER BY position", id));
        return result;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = IOException.class)
    public ResponseEntity<?> create(@CurrentUser UserPrincipal user,
            @Valid @RequestPart("listing") CreateListingRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) throws IOException {
        var files = photos == null ? List.<MultipartFile>of() : photos;
        // Owner posts go live immediately — no admin approval step. PENDING_REVIEW is still accepted
        // from older clients and treated as "publish".
        String status = request.status().equals("DRAFT") ? "DRAFT" : "ACTIVE";
        if (files.size() > 20 || (status.equals("ACTIVE") && files.size() < 3))
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos");
        if (db.queryForObject("SELECT count(*) FROM property_types WHERE id=?", Integer.class, request.propertyTypeId()) != 1)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "propertyTypeId");
        if (db.queryForObject("SELECT count(*) FROM provinces WHERE id=?", Integer.class, request.provinceId()) != 1)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "provinceId");
        var types = validatePhotos(files);
        // A stable request ID prevents duplicate listings if a client retries a submission.
        int inserted = db.update("""
            INSERT INTO listings(id, owner_id, title, purpose, property_type_id, province_id, address,
                bedrooms, bathrooms, area, price, currency, description, contact_phone, amenities, status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING
            """, request.id(), user.getId(), request.title().trim(), request.purpose(), request.propertyTypeId(),
            request.provinceId(), request.address().trim(), request.bedrooms(), request.bathrooms(), request.area(),
            request.price(), request.currency(), request.description().trim(), request.contactPhone().trim(),
            request.amenities(), status);
        if (inserted == 1) {
            for (int i = 0; i < files.size(); i++)
                db.update("INSERT INTO listing_photos(id,listing_id,position,content_type,content) VALUES (?,?,?,?,?)",
                    UUID.randomUUID(), request.id(), i, types.get(i), files.get(i).getBytes());
        }
        return ResponseEntity.status(inserted == 1 ? 201 : 200).body(ApiResponse.success(owned(request.id(), user.getId())));
    }

    private List<String> validatePhotos(List<MultipartFile> files) throws IOException {
        // Validate actual image bytes, not the filename or browser-supplied MIME type.
        var types = new ArrayList<String>();
        for (var file : files) {
            if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
                throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos");
            try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    var format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!Set.of("jpeg", "png").contains(format) || (long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000)
                        throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos");
                    if (reader.read(0) == null) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos");
                    types.add("image/" + format);
                } finally { reader.dispose(); }
            } catch (IOException ex) { throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos"); }
        }
        return types;
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = IOException.class)
    public ApiResponse<?> submit(@CurrentUser UserPrincipal user, @PathVariable UUID id,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) throws IOException {
        var rows = db.queryForList("SELECT status FROM listings WHERE id=? AND owner_id=? FOR UPDATE", id, user.getId());
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        // Retried publish of an already-live listing is a no-op.
        if ("ACTIVE".equals(rows.getFirst().get("status"))) return ApiResponse.success(owned(id, user.getId()));
        // PENDING_REVIEW only exists on listings created before auto-publish; let them go live too.
        if (!Set.of("DRAFT", "PENDING_REVIEW").contains(rows.getFirst().get("status"))) throw ApiException.of(ErrorCode.CONFLICT, "status");
        var files = photos == null ? List.<MultipartFile>of() : photos;
        int existing = db.queryForObject("SELECT count(*) FROM listing_photos WHERE listing_id=?", Integer.class, id);
        if (existing + files.size() < 3 || existing + files.size() > 20)
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "photos");
        var types = validatePhotos(files);
        for (int i = 0; i < files.size(); i++)
            db.update("INSERT INTO listing_photos(id,listing_id,position,content_type,content) VALUES (?,?,?,?,?)",
                UUID.randomUUID(), id, existing + i, types.get(i), files.get(i).getBytes());
        db.update("UPDATE listings SET status='ACTIVE' WHERE id=? AND owner_id=?", id, user.getId());
        return ApiResponse.success(owned(id, user.getId()));
    }

    @GetMapping("/{listingId}/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@CurrentUser UserPrincipal user, @PathVariable UUID listingId, @PathVariable UUID photoId) {
        var rows = db.queryForList("""
            SELECT p.content, p.content_type FROM listing_photos p JOIN listings l ON l.id=p.listing_id
            WHERE p.id=? AND l.id=? AND l.owner_id=?
            """, photoId, listingId, user.getId());
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        var row = rows.getFirst();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) row.get("content_type")))
            .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore()).body((byte[]) row.get("content"));
    }
}
