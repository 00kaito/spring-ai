package com.example.coderepoai.repository;

import com.example.coderepoai.model.CodeChunk;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class CodeChunkVectorStore {
    
    // Simple in-memory storage for code chunks
    private final Map<String, List<CodeChunk>> repository = new ConcurrentHashMap<>();
    private final JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();

    public void addCodeChunks(List<CodeChunk> codeChunks) {
        for (CodeChunk chunk : codeChunks) {
            String repositoryUrl = chunk.getRepositoryUrl();
            repository.computeIfAbsent(repositoryUrl, k -> new ArrayList<>()).add(chunk);
        }
    }

    public List<CodeChunk> searchSimilarChunks(String query, int maxResults) {
        List<CodeChunk> allChunks = repository.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        
        return allChunks.stream()
                .map(chunk -> new ScoredChunk(chunk, calculateSimilarity(query, chunk.getContent())))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(maxResults)
                .map(scoredChunk -> scoredChunk.chunk)
                .collect(Collectors.toList());
    }

    public void deleteByRepositoryUrl(String repositoryUrl) {
        repository.remove(repositoryUrl);
    }
    
    private double calculateSimilarity(String query, String content) {
        if (query == null || content == null) return 0.0;
        
        // Simple keyword matching similarity
        String[] queryWords = query.toLowerCase().split("\\W+");
        String[] contentWords = content.toLowerCase().split("\\W+");
        
        Set<String> querySet = Set.of(queryWords);
        Set<String> contentSet = Set.of(contentWords);
        
        return jaccardSimilarity.apply(String.join(" ", querySet), String.join(" ", contentSet));
    }
    
    private static class ScoredChunk {
        final CodeChunk chunk;
        final double score;
        
        ScoredChunk(CodeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}