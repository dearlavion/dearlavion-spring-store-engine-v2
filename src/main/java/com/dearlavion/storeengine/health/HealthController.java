package com.dearlavion.storeengine.health;

import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Ported from the NestJS HealthController (checks the Mongo connection's ready state). */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final MongoClient mongoClient;

    @GetMapping("/health")
    public Map<String, String> health() {
        boolean up = isUp();
        return Map.of("status", up ? "UP" : "DOWN");
    }

    private boolean isUp() {
        try {
            mongoClient.listDatabaseNames().first();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
