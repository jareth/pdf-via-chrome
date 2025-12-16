package com.github.headlesschromepdf.testapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple health check controller for verifying the application is running.
 */
@RestController
public class HealthCheckController {

    private final Instant startTime = Instant.now();

    /**
     * Basic health check endpoint.
     *
     * @return health status with uptime information
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "headless-chrome-pdf-test-app");
        response.put("startTime", startTime);
        response.put("uptime", java.time.Duration.between(startTime, Instant.now()).toSeconds() + " seconds");
        response.put("message", "PDF Test Application is running successfully");

        return ResponseEntity.ok(response);
    }
}
