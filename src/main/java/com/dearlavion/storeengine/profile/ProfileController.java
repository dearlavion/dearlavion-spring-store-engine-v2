package com.dearlavion.storeengine.profile;

import com.dearlavion.storeengine.profile.model.UserProfile;
import com.dearlavion.storeengine.profile.request.UpdateProfileRequest;
import com.dearlavion.storeengine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    @GetMapping
    public UserProfile get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(user.userId());
    }

    @PutMapping
    public UserProfile update(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpdateProfileRequest dto) {
        return service.update(user.userId(), dto);
    }
}
