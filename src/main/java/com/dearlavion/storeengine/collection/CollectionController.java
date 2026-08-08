package com.dearlavion.storeengine.collection;

import com.dearlavion.storeengine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kits")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService service;

    /** The caller's saved kits (newest first) — backs /profile/collection. */
    @GetMapping
    public List<SavedKit> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listForUser(user.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedKit save(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SaveKitRequest dto) {
        return service.save(user.userId(), dto.name(), dto.kit());
    }

    /** Update this kit in place — its name (rename, instead of creating a duplicate via POST)
     * and/or its item list (manual add/remove on /my-kit/:savedId). */
    @PatchMapping("/{id}")
    public SavedKit update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                            @Valid @RequestBody UpdateSavedKitRequest dto) {
        return service.update(user.userId(), id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        service.delete(user.userId(), id);
    }
}
