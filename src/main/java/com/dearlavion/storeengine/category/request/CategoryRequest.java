package com.dearlavion.storeengine.category.request;

import jakarta.validation.constraints.NotBlank;

/** NestJS's AdminCategoryController takes a raw {name} body with no DTO validation — this adds
 * @NotBlank for consistency with every other module's validated-DTO convention in this codebase;
 * not a behavior change for well-formed callers, just a safety net for empty/missing names. */
public record CategoryRequest(@NotBlank String name) {
}
