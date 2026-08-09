package com.dearlavion.storeengine.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Requests a presigned MinIO/S3 upload URL. Google Drive uploads go through a separate multipart
// broker endpoint (AdminMediaController#uploadToDrive) instead — see MediaClient's javadoc for why.
public record UploadUrlRequest(
        @NotBlank String fileName,

        // Matches dearlavion-spring-media-service's default FileValidator allowlist
        // (image/jpeg,png,webp,gif) — kept in sync manually, not fetched at runtime.
        // media-service re-validates independently; this is just an early, cheap rejection plus
        // defense-in-depth since media-service has no auth of its own.
        @NotBlank
        @Pattern(regexp = "image/jpeg|image/png|image/webp|image/gif")
        String contentType,

        @Positive long fileSize,

        // Folder is a raw path-prefix hint media-service uses to build the object key with no
        // validation on its side (only a trailing slash gets stripped) — this allowlist is what
        // actually prevents a caller from injecting something like "../../".
        @Pattern(regexp = "products|popular-kits")
        String folder
) {
}
