package com.financehub.controller;

import com.financehub.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final WebhookService webhookService;
    
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> sendWebhook(@RequestBody Map<String, Object> request) {
        String webhookUrl = (String) request.get("url");
        Map<String, Object> payload = (Map<String, Object>) request.get("payload");
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("Webhook URL required");
        }
        
        try {
            String response = webhookService.sendWebhook(webhookUrl, payload);
            return ResponseEntity.ok(Map.of(
                "message", "Webhook sent successfully",
                "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> validateWebhook(@RequestBody Map<String, String> request) {
        String webhookUrl = request.get("url");
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("Webhook URL required");
        }
        
        try {
            String result = webhookService.validateWebhookEndpoint(webhookUrl);
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> fetchConfig(@RequestParam("configUrl") String configUrl) {
        if (configUrl == null || configUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("Config URL required");
        }
        
        try {
            String config = webhookService.fetchWebhookConfig(configUrl);
            return ResponseEntity.ok(Map.of("config", config));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/proxy")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> proxyRequest(@RequestBody Map<String, Object> request) {
        String targetUrl = (String) request.get("url");
        String method = (String) request.get("method");
        Map<String, String> headers = (Map<String, String>) request.get("headers");
        String body = (String) request.get("body");
        
        if (targetUrl == null || targetUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("Target URL required");
        }

        // Validate URL format and prevent SSRF by restricting to allowed protocols and disallowing local/internal IPs
        try {
            URI uri = new URI(targetUrl);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return ResponseEntity.badRequest().body("Invalid URL scheme");
            }

            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid URL host");
            }

            // Basic SSRF protection: disallow localhost and private IP ranges
            if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1") ||
                host.endsWith(".local") || host.matches("^(10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.).*")) {
                return ResponseEntity.badRequest().body("Access to local or private network addresses is not allowed");
            }

        } catch (URISyntaxException e) {
            return ResponseEntity.badRequest().body("Malformed target URL");
        }
        
        if (method == null || method.isEmpty()) {
            method = "GET";
        }
        
        try {
            String response = webhookService.proxyRequest(targetUrl, method, headers, body);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            // Avoid exposing internal exception messages to client
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/notify")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> request) {
        String notificationUrl = (String) request.get("notificationUrl");
        String message = (String) request.get("message");
        String userId = (String) request.get("userId");
        
        if (notificationUrl == null || notificationUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("Notification URL required");
        }
        
        Map<String, Object> payload = Map.of(
            "message", message != null ? message : "Default notification",
            "userId", userId != null ? userId : "anonymous",
            "timestamp", java.time.Instant.now().toString()
        );
        
        try {
            String response = webhookService.sendWebhook(notificationUrl, payload);
            return ResponseEntity.ok(Map.of(
                "message", "Notification sent",
                "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}