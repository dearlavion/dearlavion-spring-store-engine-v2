package com.dearlavion.storeengine.media;

/** Mirrors dearlavion-spring-media-service's UploadUrlResponse — passthrough shape. */
public record UploadUrlResponse(String uploadUrl, String mediaKey, String expiresAt, String publicUrl) {
}
