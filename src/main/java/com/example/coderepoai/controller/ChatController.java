package com.example.coderepoai.controller;

import com.example.coderepoai.model.PromptRequest;
import com.example.coderepoai.service.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody PromptRequest promptRequest) {
        logger.info("Received chat request: {}", promptRequest.getQuery());
        
        try {
            String response = chatService.chat(promptRequest);
            
            return ResponseEntity.ok(Map.of(
                "response", response,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to process chat request: " + e.getMessage(),
                "status", "error"
            ));
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<Map<String, String>> simpleChat(@RequestParam String query) {
        logger.info("Received simple chat request: {}", query);
        
        try {
            String response = chatService.chatWithSimpleQuery(query);
            
            return ResponseEntity.ok(Map.of(
                "response", response,
                "query", query,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing simple chat request", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to process chat request: " + e.getMessage(),
                "status", "error"
            ));
        }
    }

    @PostMapping("/repository")
    public ResponseEntity<Map<String, String>> chatWithRepository(
            @RequestParam String query,
            @RequestParam String repositoryUrl) {
        
        logger.info("Received repository chat request for repo: {} with query: {}", repositoryUrl, query);
        
        try {
            String response = chatService.chatWithRepository(query, repositoryUrl);
            
            return ResponseEntity.ok(Map.of(
                "response", response,
                "query", query,
                "repository", repositoryUrl,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing repository chat request", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to process chat request: " + e.getMessage(),
                "status", "error"
            ));
        }
    }
}