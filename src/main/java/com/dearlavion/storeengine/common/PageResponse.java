package com.dearlavion.storeengine.common;

import java.util.List;

/** Matches the NestJS services' {content, page, size, total} pagination shape exactly. */
public record PageResponse<T>(List<T> content, int page, int size, long total) {
}
