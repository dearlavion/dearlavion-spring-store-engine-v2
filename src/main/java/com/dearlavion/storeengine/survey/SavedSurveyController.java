package com.dearlavion.storeengine.survey;

import com.dearlavion.storeengine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/surveys")
@RequiredArgsConstructor
public class SavedSurveyController {

    private final SurveyService service;

    /** Save the caller's survey result. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedSurvey save(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SurveyAnswersRequest answers) {
        return service.save(user.userId(), answers);
    }

    /** The caller's saved surveys (newest first). */
    @GetMapping
    public List<SavedSurvey> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listForUser(user.userId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        service.delete(user.userId(), id);
    }
}
