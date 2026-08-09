package com.dearlavion.storeengine.media;

import com.dearlavion.storeengine.common.MediaClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Broker for dearlavion-spring-media-service's upload endpoints. media-service itself has no auth (no
 * guard in the service, and both API gateways fail to enforce anything in front of
 * /api/media/**) — this controller (gated by SecurityConfig's /admin/** -> hasRole("ADMIN") rule)
 * is the only sanctioned path into it from this app. For the default S3/MinIO path, the browser
 * calls requestUploadUrl to get a presigned PUT URL, then PUTs the file bytes directly to that URL
 * (skips this service entirely for the file bytes). For Google Drive (provider="drive"), the
 * browser similarly PUTs to the returned resumable-session URI, then calls finalizeDriveUpload
 * with the resulting Drive file id to make it public and get the final URL. */
@RestController
@RequestMapping("/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaClient mediaClient;

    @PostMapping("/upload-url")
    public UploadUrlResponse requestUploadUrl(@Valid @RequestBody UploadUrlRequest request) {
        return mediaClient.getUploadUrl(request);
    }

    @PostMapping("/drive/finalize")
    public Map<String, String> finalizeDriveUpload(@Valid @RequestBody FinalizeDriveUploadRequest request) {
        return mediaClient.finalizeDriveUpload(request.fileId());
    }
}
