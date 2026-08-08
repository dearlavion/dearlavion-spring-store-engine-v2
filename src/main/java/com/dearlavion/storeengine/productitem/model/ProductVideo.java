package com.dearlavion.storeengine.productitem.model;

import lombok.Getter;
import lombok.Setter;

/** An influencer/testimonial video for this specific item — a pasted link, same as `image`; no
 * upload/hosting involved. Embedded sub-document, no own _id (matches the NestJS {_id:false} schema). */
@Getter
@Setter
public class ProductVideo {
    private String title;
    private String url;
    private String author;
}
