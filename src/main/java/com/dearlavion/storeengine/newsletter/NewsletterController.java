package com.dearlavion.storeengine.newsletter;

import com.dearlavion.storeengine.newsletter.request.SubscribeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public, no auth — an optional opt-in prompt shown from the storefront (currently My Kit's PDF
 * download action), not a customer-account feature. */
@RestController
@RequestMapping("/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService service;

    @PostMapping("/subscribe")
    public Map<String, Boolean> subscribe(@Valid @RequestBody SubscribeRequest dto) {
        return service.subscribe(dto.email());
    }
}
