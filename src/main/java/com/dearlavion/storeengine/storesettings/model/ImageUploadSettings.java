package com.dearlavion.storeengine.storesettings.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Admin product/kit photo upload defaults, stored as its own document in the `store_settings`
 * collection keyed by `_id: "image_upload_settings"` — a separate entry from the shipping settings
 * (see {@link StoreSettings}) and exchange rates, each store-wide setting is its own document. */
@Getter
@Setter
@Document(collection = "store_settings")
public class ImageUploadSettings {

    public static final String SINGLETON_ID = "image_upload_settings";

    @Id
    private String id = SINGLETON_ID;

    /** Default upload destination admin forms preselect for new product/kit photos — "s3" (Media
     * Storage, MinIO) or "drive" (Google Drive). Admins can still override per-upload; see
     * ImageUploadFieldComponent on the frontend. */
    private String defaultMediaProvider = "s3";

    /** Max accepted file size (KB) for admin product/kit photo uploads, enforced client-side by
     * ImageUploadFieldComponent before it calls media-service. */
    private int maxImageSizeKb = 30;

    private Instant updatedAt;
}
