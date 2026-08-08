package com.dearlavion.storeengine.taxonomy.request;

public record UpdateTaxonomyValueRequest(String value, Integer order, String emoji, String subtext) {
}
