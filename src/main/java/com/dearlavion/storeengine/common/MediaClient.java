package com.dearlavion.storeengine.common;

import com.dearlavion.storeengine.media.UploadUrlRequest;
import com.dearlavion.storeengine.media.UploadUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Server-to-server client for dearlavion-spring-media-service's presigned-upload-URL endpoint.
 * dearlavion-spring-media-service has NO auth of its own (no guard in the service, and both API
 * gateways fail to enforce anything in front of /api/media/**) — AdminMediaController (gated by
 * SecurityConfig's /admin/** -> hasRole("ADMIN") rule) is the only sanctioned path into it from
 * this app. Never expose media-service's URL directly to the frontend.
 *
 * The Google Drive-touching calls (provider="drive" on getUploadUrl, and finalizeDriveUpload) are
 * additionally gated by media-service's own X-Internal-Api-Key check — unlike the S3 path, an
 * ungated Drive endpoint would let anyone make an arbitrary file in a real personal Google account
 * public, so media-service requires this shared secret for those specifically. */
@Component
public class MediaClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.media-service-url}")
    private String mediaServiceUrl;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    public UploadUrlResponse getUploadUrl(UploadUrlRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return restTemplate.postForObject(
                mediaServiceUrl + "/api/v1/media/upload-url", new HttpEntity<>(request, headers), UploadUrlResponse.class);
    }

    public Map<String, String> finalizeDriveUpload(String fileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        @SuppressWarnings("unchecked")
        Map<String, String> response = restTemplate.postForObject(
                mediaServiceUrl + "/api/v1/media/drive/finalize",
                new HttpEntity<>(Map.of("fileId", fileId), headers),
                Map.class);
        return response;
    }
}
