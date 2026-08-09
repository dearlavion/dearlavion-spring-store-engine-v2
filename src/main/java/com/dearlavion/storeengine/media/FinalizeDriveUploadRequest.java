package com.dearlavion.storeengine.media;

import jakarta.validation.constraints.NotBlank;

public record FinalizeDriveUploadRequest(@NotBlank String fileId) {
}
