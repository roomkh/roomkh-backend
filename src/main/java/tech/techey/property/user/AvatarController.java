package tech.techey.property.user;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.techey.property.common.dto.ApiResponse;
import tech.techey.property.common.exception.*;
import tech.techey.property.common.security.*;

@RestController
@RequestMapping("/api/v1")
public class AvatarController {
    private final JdbcTemplate db;
    public AvatarController(JdbcTemplate db) { this.db = db; }

    @PutMapping("/users/me/avatar")
    @PreAuthorize("hasAnyRole('OWNER','AGENT','ADMIN')")
    @Transactional
    public ApiResponse<?> upload(@CurrentUser UserPrincipal principal, @RequestPart("photo") MultipartFile photo) {
        if (photo.isEmpty() || photo.getSize() > 5 * 1024 * 1024) throw invalid();
        byte[] content;
        try (var input = ImageIO.createImageInputStream(photo.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                reader.setInput(input);
                var format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (!(format.equals("png") || format.equals("jpeg")) || w < 1 || h < 1 || (long) w * h > 16_000_000) throw invalid();
                var source = reader.read(0);
                int side = Math.min(w, h);
                int size = Math.min(side, 512);
                var output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                var graphics = output.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(source, 0, 0, size, size, (w-side)/2, (h-side)/2, (w+side)/2, (h+side)/2, null);
                } finally { graphics.dispose(); }
                var bytes = new ByteArrayOutputStream();
                ImageIO.write(output, "png", bytes);
                content = bytes.toByteArray();
            } finally { reader.dispose(); }
        } catch (java.io.IOException | IllegalArgumentException ex) { throw invalid(); }
        var ids = db.queryForList("SELECT uuid FROM users WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL", principal.getId());
        if (ids.isEmpty()) throw ApiException.of(ErrorCode.FORBIDDEN);
        String url = "/api/owner-avatars/" + ids.getFirst().get("uuid") + "?v=" + UUID.randomUUID();
        db.update("INSERT INTO user_avatars(user_id,content) VALUES (?,?) ON CONFLICT(user_id) DO UPDATE SET content=EXCLUDED.content", principal.getId(), content);
        db.update("UPDATE users SET avatar_url=? WHERE id=?", url, principal.getId());
        return ApiResponse.success(java.util.Map.of("avatarUrl", url));
    }

    @DeleteMapping("/users/me/avatar")
    @PreAuthorize("hasAnyRole('OWNER','AGENT','ADMIN')")
    @Transactional
    public ApiResponse<?> remove(@CurrentUser UserPrincipal principal) {
        db.update("DELETE FROM user_avatars WHERE user_id=?", principal.getId());
        db.update("UPDATE users SET avatar_url=NULL WHERE id=?", principal.getId());
        return ApiResponse.success(null);
    }

    @GetMapping("/owners/{id}/avatar")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        var rows = db.queryForList("""
            SELECT a.content FROM user_avatars a JOIN users u ON u.id=a.user_id
            WHERE u.uuid=? AND u.status='ACTIVE' AND u.deleted_at IS NULL
            AND u.role IN ('OWNER','AGENT','ADMIN')
            """, id);
        if (rows.isEmpty()) throw ApiException.of(ErrorCode.NOT_FOUND);
        return ResponseEntity.ok().header("Content-Type", "image/png")
            .header("Cache-Control", "no-store").header("X-Content-Type-Options", "nosniff")
            .body((byte[]) rows.getFirst().get("content"));
    }
    private ApiException invalid() { return ApiException.of(ErrorCode.VALIDATION_ERROR, "photo"); }
}
