package com.dearlavion.storeengine.productitem.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ProductVideoRequest(@NotBlank String title, @URL String url, String author) {
}
