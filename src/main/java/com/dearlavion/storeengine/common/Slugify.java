package com.dearlavion.storeengine.common;

/** Ported from schema.util.ts's slugify() — stable, URL-friendly lookups for products/categories. */
public final class Slugify {

    private Slugify() {
    }

    public static String slugify(String value) {
        return value
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
