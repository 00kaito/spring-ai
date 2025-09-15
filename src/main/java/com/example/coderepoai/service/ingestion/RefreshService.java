package com.example.coderepoai.service.ingestion;

import com.example.coderepoai.model.CodeChunk;
import com.example.coderepoai.repository.CodeChunkVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class RefreshService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshService.class);
    
    private final RepositoryFetcher repositoryFetcher;
    private final FileParser fileParser;
    private final CodeChunker codeChunker;
    private final CodeChunkVectorStore codeChunkVectorStore;

    public RefreshService(RepositoryFetcher repositoryFetcher, 
                         FileParser fileParser,
                         CodeChunker codeChunker,
                         CodeChunkVectorStore codeChunkVectorStore) {
        this.repositoryFetcher = repositoryFetcher;
        this.fileParser = fileParser;
        this.codeChunker = codeChunker;
        this.codeChunkVectorStore = codeChunkVectorStore;
    }

    public void refreshRepository(String repositoryUrl) {
        logger.info("Starting repository refresh for: {}", repositoryUrl);
        
        try {
            // Step 1: Clear existing data for this repository
            logger.debug("Clearing existing data for repository: {}", repositoryUrl);
            codeChunkVectorStore.deleteByRepositoryUrl(repositoryUrl);
            
            // Step 2: Fetch repository files
            logger.debug("Fetching repository files...");
            Map<String, String> rawFiles = repositoryFetcher.fetchRepositoryFiles(repositoryUrl);
            logger.info("Fetched {} files from repository", rawFiles.size());
            
            if (rawFiles.isEmpty()) {
                logger.warn("No files found in repository: {}", repositoryUrl);
                return;
            }
            
            // Step 3: Parse files
            logger.debug("Parsing files...");
            Map<String, String> parsedFiles = fileParser.parseFiles(rawFiles);
            logger.info("Parsed {} files successfully", parsedFiles.size());
            
            // Step 4: Chunk files into smaller pieces
            logger.debug("Chunking files...");
            List<CodeChunk> codeChunks = codeChunker.chunkFiles(parsedFiles, repositoryUrl);
            logger.info("Created {} code chunks", codeChunks.size());
            
            // Step 5: Store chunks in vector database
            logger.debug("Storing chunks in vector database...");
            codeChunkVectorStore.addCodeChunks(codeChunks);
            
            logger.info("Repository refresh completed successfully for: {}", repositoryUrl);
            
        } catch (Exception e) {
            logger.error("Failed to refresh repository: {}", repositoryUrl, e);
            throw new RuntimeException("Repository refresh failed", e);
        }
    }

    @Async
    public CompletableFuture<Void> refreshRepositoryAsync(String repositoryUrl) {
        logger.info("Starting async repository refresh for: {}", repositoryUrl);
        
        try {
            refreshRepository(repositoryUrl);
            logger.info("Async repository refresh completed successfully for: {}", repositoryUrl);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Async repository refresh failed for: {}", repositoryUrl, e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}