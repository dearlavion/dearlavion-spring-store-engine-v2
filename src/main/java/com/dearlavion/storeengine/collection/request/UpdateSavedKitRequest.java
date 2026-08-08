package com.dearlavion.storeengine.collection.request;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateSavedKitRequest(String name, @Valid List<KitItemRequest> items) {
}
