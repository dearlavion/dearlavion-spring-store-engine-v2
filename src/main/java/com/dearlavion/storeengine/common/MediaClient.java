package com.dearlavion.storeengine.common;

import com.dearlavion.storeengine.media.UploadUrlRequest;
import com.dearlavion.storeengine.media.UploadUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Server-to-server client for dearlavion-spring-media-service's upload endpoints.
 * dearlavion-spring-media-service has NO auth of its own (no guard in the service, and both API
 * gateways fail to enforce anything in front of /api/media/**) — AdminMediaController (gated by
 * SecurityConfig's /admin/** -> hasRole("ADMIN") rule) is the only sanctioned path into it from
 * this app. Never expose media-service's URL directly to the frontend.
 *
 * The Google Drive upload call is additionally gated by media-service's own X-Internal-Api-Key
 * check — unlike the S3 path, an ungated Drive endpoint would let anyone upload into a real
 * personal Google account and make files public, so media-service requires this shared secret for
 * it specifically. It's also a proxied multipart upload rather than a presigned/session URL the
 * browser PUTs to directly — Drive's resumable-upload PUT response lacks CORS headers, so the
 * browser can't PUT to it the way it can a presigned S3 URL. */
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

    public Map<String, String> uploadToDrive(byte[] fileBytes, String fileName, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        HttpHeaders filePartHeaders = new HttpHeaders();
        filePartHeaders.setContentType(MediaType.parseMediaType(contentType));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, filePartHeaders));

        @SuppressWarnings("unchecked")
        Map<String, String> response = restTemplate.postForObject(
                mediaServiceUrl + "/api/v1/media/drive/upload", new HttpEntity<>(body, headers), Map.class);
        return response;
    }
}
