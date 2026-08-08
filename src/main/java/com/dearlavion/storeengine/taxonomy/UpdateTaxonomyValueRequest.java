package com.dearlavion.storeengine.taxonomy;

public record UpdateTaxonomyValueRequest(String value, Integer order, String emoji, String subtext) {
}
