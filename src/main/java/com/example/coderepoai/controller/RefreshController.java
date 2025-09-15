package com.example.coderepoai.controller;

import com.example.coderepoai.service.ingestion.RefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refresh")
@CrossOrigin(origins = "*")
public class RefreshController {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshController.class);
    
    private final RefreshService refreshService;

    public RefreshController(RefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> refreshRepository(@RequestBody Map<String, String> request) {
        String repositoryUrl = request.get("repository_url");
        
        if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Repository URL is required",
                "status", "error"
            ));
        }
        
        logger.info("Received refresh request for repository: {}", repositoryUrl);
        
        try {
            refreshService.refreshRepository(repositoryUrl);
            
            return ResponseEntity.ok(Map.of(
                "message", "Repository refreshed successfully",
                "repository_url", repositoryUrl,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error refreshing repository: {}", repositoryUrl, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to refresh repository: " + e.getMessage(),
                "repository_url", repositoryUrl,
                "status", "error"
            ));
        }
    }

    @PostMapping("/async")
    public ResponseEntity<Map<String, String>> refreshRepositoryAsync(@RequestBody Map<String, String> request) {
        String repositoryUrl = request.get("repository_url");
        
        if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Repository URL is required",
                "status", "error"
            ));
        }
        
        logger.info("Received async refresh request for repository: {}", repositoryUrl);
        
        try {
            refreshService.refreshRepositoryAsync(repositoryUrl);
            
            return ResponseEntity.ok(Map.of(
                "message", "Repository refresh started",
                "repository_url", repositoryUrl,
                "status", "started"
            ));
            
        } catch (Exception e) {
            logger.error("Error starting async repository refresh: {}", repositoryUrl, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to start repository refresh: " + e.getMessage(),
                "repository_url", repositoryUrl,
                "status", "error"
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        // In a production system, this would return the status of ongoing refresh operations
        return ResponseEntity.ok(Map.of(
            "status", "ready",
            "service", "refresh"
        ));
    }
}