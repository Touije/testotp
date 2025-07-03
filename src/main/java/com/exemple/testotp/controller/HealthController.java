package com.exemple.testotp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Slf4j
@Tag(name = "Health", description = "API de santé de l'application")
public class HealthController {

    @GetMapping("/status")
    @Operation(summary = "Vérifier le statut de l'application", description = "Retourne le statut de santé de l'application")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "Keycloak OTP Registration");
        status.put("version", "1.0.0");

        log.debug("Health check requested");
        return ResponseEntity.ok(status);
    }
}
