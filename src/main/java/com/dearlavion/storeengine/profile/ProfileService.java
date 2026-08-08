package com.dearlavion.storeengine.profile;

import com.dearlavion.storeengine.profile.model.UserProfile;
import com.dearlavion.storeengine.profile.request.UpdateProfileRequest;
import com.dearlavion.storeengine.storesettings.model.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository repository;

    /** The caller's profile, creating a default one on first read (so the UI always has something). */
    public UserProfile get(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setUpdatedAt(Instant.now());
            return repository.save(profile);
        });
    }

    /** Upsert the caller's editable fields; ignores blank values so partial saves are safe. */
    public UserProfile update(String userId, UpdateProfileRequest patch) {
        UserProfile profile = get(userId);
        if (patch.displayName() != null && !patch.displayName().trim().isEmpty()) {
            profile.setDisplayName(patch.displayName().trim());
        }
        if (patch.avatar() != null && !patch.avatar().isEmpty()) {
            profile.setAvatar(patch.avatar());
        }
        if (patch.currency() != null && !patch.currency().isEmpty()) {
            if (!Currency.SUPPORTED_CURRENCIES.contains(patch.currency())) {
                throw new IllegalArgumentException("Unsupported currency: " + patch.currency());
            }
            profile.setCurrency(patch.currency());
        }
        profile.setUpdatedAt(Instant.now());
        return repository.save(profile);
    }
}
