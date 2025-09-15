package com.example.coderepoai.service.chat;

import com.example.coderepoai.model.CodeChunk;
import com.example.coderepoai.repository.CodeChunkVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {
    
    private static final Logger logger = LoggerFactory.getLogger(RetrievalService.class);
    
    private final CodeChunkVectorStore codeChunkVectorStore;

    public RetrievalService(CodeChunkVectorStore codeChunkVectorStore) {
        this.codeChunkVectorStore = codeChunkVectorStore;
    }

    public List<CodeChunk> retrieveRelevantChunks(String query, int maxResults) {
        logger.debug("Retrieving relevant chunks for query: {}", query);
        
        List<CodeChunk> relevantChunks = codeChunkVectorStore.searchSimilarChunks(query, maxResults);
        
        logger.info("Found {} relevant chunks for query", relevantChunks.size());
        
        return relevantChunks;
    }

    public String buildContextFromChunks(List<CodeChunk> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant code found for your query.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Here are the relevant code snippets:\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);
            context.append("--- Code Snippet ").append(i + 1).append(" ---\n");
            context.append("File: ").append(chunk.getFilePath()).append("\n");
            context.append("Repository: ").append(chunk.getRepositoryUrl()).append("\n");
            context.append("Content:\n").append(chunk.getContent()).append("\n\n");
        }
        
        return context.toString();
    }

    public List<CodeChunk> retrieveRelevantChunksForRepository(String query, String repositoryUrl, int maxResults) {
        List<CodeChunk> allRelevantChunks = retrieveRelevantChunks(query, maxResults * 2); // Get more to filter
        
        // Filter chunks by repository URL
        List<CodeChunk> filteredChunks = allRelevantChunks.stream()
                .filter(chunk -> repositoryUrl.equals(chunk.getRepositoryUrl()))
                .limit(maxResults)
                .collect(Collectors.toList());
        
        logger.info("Found {} relevant chunks for repository {} and query", filteredChunks.size(), repositoryUrl);
        
        return filteredChunks;
    }
}