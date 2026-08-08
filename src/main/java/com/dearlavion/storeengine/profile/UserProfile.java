package com.dearlavion.storeengine.profile;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One profile per user — backs the frontend's /profile/settings (displayName + emoji avatar). */
@Getter
@Setter
@Document(collection = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String displayName = "Traveler";

    private String avatar = "🧳";

    /** Preferred display/settlement currency (ISO 4217). Defaults to USD. */
    private String currency = "USD";

    private Instant updatedAt;
}
