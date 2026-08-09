package com.dearlavion.storeengine.media;

import com.dearlavion.storeengine.common.MediaClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Broker for dearlavion-spring-media-service's upload endpoints. media-service itself has no auth
 * (no guard in the service, and both API gateways fail to enforce anything in front of
 * /api/media/**) — this controller (gated by SecurityConfig's /admin/** -> hasRole("ADMIN") rule)
 * is the only sanctioned path into it from this app.
 *
 * The two upload targets use different patterns: for the default S3/MinIO path, the browser calls
 * requestUploadUrl to get a presigned PUT URL, then PUTs the file bytes directly to that URL
 * (skips this service entirely for the file bytes). For Google Drive, the browser instead uploads
 * the file straight to uploadToDrive (multipart) — Drive's own resumable-upload PUT response lacks
 * CORS headers, so a browser-direct PUT to it (like the S3 path uses) doesn't work; media-service
 * proxies the bytes to Drive server-side instead. */
@RestController
@RequestMapping("/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaClient mediaClient;

    @PostMapping("/upload-url")
    public UploadUrlResponse requestUploadUrl(@Valid @RequestBody UploadUrlRequest request) {
        return mediaClient.getUploadUrl(request);
    }

    @PostMapping(value = "/drive/upload", consumes = "multipart/form-data")
    public Map<String, String> uploadToDrive(@RequestParam("file") MultipartFile file) throws IOException {
        return mediaClient.uploadToDrive(file.getBytes(), file.getOriginalFilename(), file.getContentType());
    }
}
