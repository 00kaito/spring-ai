package com.example.coderepoai.service.chat;

import com.example.coderepoai.model.CodeChunk;
import com.example.coderepoai.model.PromptRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final RetrievalService retrievalService;
    private final ChatClient chatClient;

    public ChatService(RetrievalService retrievalService, @Autowired(required = false) ChatClient chatClient) {
        this.retrievalService = retrievalService;
        this.chatClient = chatClient;
    }

    public String chat(PromptRequest promptRequest) {
        logger.info("Processing chat request for query: {}", promptRequest.getQuery());
        
        try {
            // Retrieve relevant code chunks
            List<CodeChunk> relevantChunks;
            if (promptRequest.getRepositoryUrl() != null && !promptRequest.getRepositoryUrl().trim().isEmpty()) {
                // Search within specific repository
                relevantChunks = retrievalService.retrieveRelevantChunksForRepository(
                    promptRequest.getQuery(), 
                    promptRequest.getRepositoryUrl(), 
                    promptRequest.getMaxResults()
                );
            } else {
                // Search across all repositories
                relevantChunks = retrievalService.retrieveRelevantChunks(
                    promptRequest.getQuery(), 
                    promptRequest.getMaxResults()
                );
            }
            
            // Build context from retrieved chunks
            String context = retrievalService.buildContextFromChunks(relevantChunks);
            
            if (relevantChunks.isEmpty()) {
                return "I couldn't find any relevant code for your question: \"" + promptRequest.getQuery() + "\". " +
                       "Please make sure the repository has been processed first using the /api/refresh endpoint.";
            }
            
            // Use Spring AI ChatClient if available
            if (chatClient != null) {
                try {
                    String aiPrompt = String.format("""
                        You are an AI assistant helping analyze code from GitHub repositories.
                        
                        User question: %s
                        
                        Here is the relevant code context I found:
                        %s
                        
                        Please provide a helpful answer based on the code context above. 
                        Be specific about what the code does, how it works, and answer the user's question.
                        If you see any patterns, best practices, or potential improvements, mention them.
                        """, promptRequest.getQuery(), context);
                        
                    String aiResponse = chatClient.prompt(aiPrompt).call().content();
                    
                    logger.info("Generated AI-powered chat response successfully");
                    return aiResponse;
                    
                } catch (Exception e) {
                    logger.warn("Failed to get AI response, falling back to simple response", e);
                    // Fall back to simple response
                }
            }
            
            // Fallback simple response when no AI is available
            StringBuilder response = new StringBuilder();
            response.append("Based on your question \"").append(promptRequest.getQuery()).append("\", ");
            response.append("I found ").append(relevantChunks.size()).append(" relevant code snippets:\n\n");
            
            for (int i = 0; i < Math.min(relevantChunks.size(), 3); i++) {
                CodeChunk chunk = relevantChunks.get(i);
                response.append("**File: ").append(chunk.getFilePath()).append("**\n");
                response.append("```\n").append(chunk.getContent().substring(0, Math.min(500, chunk.getContent().length())));
                if (chunk.getContent().length() > 500) response.append("...");
                response.append("\n```\n\n");
            }
            
            response.append("This gives you a good overview of the relevant code. ");
            response.append("Note: For better AI-powered responses, configure an OpenAI API key in your environment.");
            
            logger.info("Generated simple chat response successfully");
            return response.toString();
            
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return "I'm sorry, I encountered an error while processing your request. Please try again or rephrase your question.";
        }
    }

    public String chatWithSimpleQuery(String query) {
        PromptRequest request = new PromptRequest(query, null);
        return chat(request);
    }

    public String chatWithRepository(String query, String repositoryUrl) {
        PromptRequest request = new PromptRequest(query, repositoryUrl);
        return chat(request);
    }
}