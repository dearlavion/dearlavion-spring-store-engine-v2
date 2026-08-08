package com.dearlavion.storeengine.collection;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BuiltKit {
    private List<KitItem> items = new ArrayList<>();
    private String summary = "";
    private String title;
}
