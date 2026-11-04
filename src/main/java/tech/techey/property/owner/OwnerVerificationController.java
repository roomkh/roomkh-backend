package tech.techey.property.owner;

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

/** Owner identity verification (PRD §6.1 step 4). Documents are private; only metadata is returned. */
@RestController
@RequestMapping("/api/v1/owner/verification")
public class OwnerVerificationController {
    private final JdbcTemplate db;
    public OwnerVerificationController(JdbcTemplate db) { this.db = db; }

    private static final int MAX_PENDING = 5;

    static String latestStatus(JdbcTemplate db, long ownerId, String kind) {
        var rows = db.queryForList("""
            SELECT status FROM owner_verification_documents WHERE owner_id=? AND kind=?
            ORDER BY (status='APPROVED') DESC, created_at DESC LIMIT 1
            """, ownerId, kind);
        return rows.isEmpty() ? null : (String) rows.getFirst().get("status");
    }

    @GetMapping
    public ApiResponse<?> status(@CurrentUser UserPrincipal user) {
        var account = db.queryForMap("""
            SELECT email, phone, phone_verified_at IS NOT NULL AS "phoneVerified" FROM users WHERE id=?
            """, user.getId());
        account.put("idStatus", Objects.requireNonNullElse(latestStatus(db, user.getId(), "ID"), "NONE"));
        account.put("propertyStatus", Objects.requireNonNullElse(latestStatus(db, user.getId(), "PROPERTY"), "NONE"));
        account.put("documents", db.queryForList("""
            SELECT id, kind, content_type AS "contentType", status, rejection_reason AS "rejectionReason",
                   reviewed_at AS "reviewedAt", created_at AS "createdAt"
            FROM owner_verification_documents WHERE owner_id=? ORDER BY created_at DESC
            """, user.getId()));
        return ApiResponse.success(account);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> upload(@CurrentUser UserPrincipal user, @RequestParam("kind") String kind,
                                    @RequestPart("file") MultipartFile file) throws IOException {
        if (!Set.of("ID", "PROPERTY").contains(kind)) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "kind");
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw ApiException.of(ErrorCode.VALIDATION_ERROR, "file");
        Integer pending = db.queryForObject(
            "SELECT count(*) FROM owner_verification_documents WHERE owner_id=? AND status='PENDING'", Integer.class, user.getId());
        if (pending != null && pending >= MAX_PENDING) throw ApiException.of(ErrorCode.CONFLICT, "file");
        byte[] content = file.getBytes();
        var id = UUID.randomUUID();
        db.update("INSERT INTO owner_verification_documents(id,owner_id,kind,content_type,content) VALUES (?,?,?,?,?)",
            id, user.getId(), kind, contentType(content), content);
        return ResponseEntity.status(201).body(ApiResponse.success(Map.of("id", id)));
    }

    /** Sniffs the real format from the bytes; the browser-supplied MIME type is never trusted. */
    private static String contentType(byte[] content) {
        if (content.length > 5 && new String(content, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            return "application/pdf";
        try (var input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    var format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (Set.of("jpeg", "png").contains(format) && (long) reader.getWidth(0) * reader.getHeight(0) <= 40_000_000)
                        return "image/" + format;
                } finally { reader.dispose(); }
            }
        } catch (IOException ignored) { }
        throw ApiException.of(ErrorCode.VALIDATION_ERROR, "file");
    }
}
