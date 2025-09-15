package com.example.coderepoai.repository;

import com.example.coderepoai.model.CodeChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
public class CodeChunkVectorStore {
    
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final Set<String> indexedRepositories = ConcurrentHashMap.newKeySet();
    
    // Fallback storage when AI is not available
    private final Map<String, List<CodeChunk>> fallbackRepository = new ConcurrentHashMap<>();
    private final boolean aiEnabled;
    
    public CodeChunkVectorStore(@Autowired(required = false) VectorStore vectorStore,
                               @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.aiEnabled = (vectorStore != null && embeddingModel != null);
        System.out.println("CodeChunkVectorStore initialized with AI enabled: " + aiEnabled);
    }

    public void addCodeChunks(List<CodeChunk> codeChunks) {
        if (codeChunks == null || codeChunks.isEmpty()) {
            return;
        }

        if (aiEnabled && vectorStore != null) {
            try {
                // Convert CodeChunks to Spring AI Documents with enhanced metadata
                List<Document> documents = codeChunks.stream()
                        .map(this::convertToDocument)
                        .collect(Collectors.toList());
                
                // Store in vector database with embeddings
                vectorStore.add(documents);
                
                // Track indexed repositories
                codeChunks.forEach(chunk -> indexedRepositories.add(chunk.getRepositoryUrl()));
                System.out.println("Added " + documents.size() + " documents to vector store");
                
            } catch (Exception e) {
                // Fallback to simple storage if AI is not available
                System.err.println("Vector store failed, using fallback: " + e.getMessage());
                addToFallbackStorage(codeChunks);
            }
        } else {
            // Use fallback storage when AI is not enabled
            System.out.println("AI not enabled, using fallback storage for " + codeChunks.size() + " chunks");
            addToFallbackStorage(codeChunks);
        }
    }

    public List<CodeChunk> searchSimilarChunks(String query, int maxResults) {
        return searchSimilarChunks(query, null, maxResults);
    }
    
    public List<CodeChunk> searchSimilarChunks(String query, String repositoryUrl, int maxResults) {
        if (aiEnabled && vectorStore != null && embeddingModel != null) {
            try {
                // Preprocess query to optimize vectorization
                String processedQuery = preprocessQuery(query);
                
                // Use larger search space for better recall, then apply filtering
                int topK = Math.max(maxResults * 10, 100); // Increased for better recall
                
                // Create SearchRequest - use available API for current Spring AI version
                SearchRequest searchRequest;
                try {
                    // Try to create enhanced SearchRequest with topK if supported
                    searchRequest = SearchRequest.query(processedQuery);
                    // Note: Advanced filtering will be added when Spring AI API is stable
                    System.out.println("Using SearchRequest with optimized vector search (topK=" + topK + " computed for post-processing)");
                } catch (Exception e) {
                    // Fallback for compatibility
                    searchRequest = SearchRequest.query(processedQuery);
                    System.out.println("Using basic SearchRequest with fallback processing");
                }
                
                // Perform vector similarity search using k-NN algorithms
                List<Document> documents = vectorStore.similaritySearch(searchRequest);
                System.out.println("Vector search found " + documents.size() + " documents for query: " + query);
                
                // Convert to CodeChunks and apply additional filtering
                List<CodeChunk> chunks = documents.stream()
                        .map(this::convertToCodeChunk)
                        .filter(chunk -> repositoryUrl == null || repositoryUrl.equals(chunk.getRepositoryUrl()))
                        .collect(Collectors.toList());
                
                // Apply intelligent semantic filtering for query types
                List<CodeChunk> smartFiltered = applySmartFiltering(query, chunks);
                if (!smartFiltered.isEmpty()) {
                    List<CodeChunk> results = smartFiltered.stream().limit(maxResults).collect(Collectors.toList());
                    System.out.println("Smart filtering returned " + results.size() + " results");
                    return results;
                }
                
                // If smart filtering returns nothing, use all vector search results
                List<CodeChunk> results = chunks.stream().limit(maxResults).collect(Collectors.toList());
                System.out.println("Returning " + results.size() + " unfiltered vector search results");
                return results;
                        
            } catch (Exception e) {
                System.err.println("Vector search failed, using fallback: " + e.getMessage());
                e.printStackTrace();
                return fallbackSearch(query, repositoryUrl, maxResults);
            }
        } else {
            System.out.println("AI not enabled (vectorStore=" + (vectorStore != null) + ", embeddingModel=" + (embeddingModel != null) + "), using fallback search for: " + query);
            return fallbackSearch(query, repositoryUrl, maxResults);
        }
    }

    public void deleteByRepositoryUrl(String repositoryUrl) {
        if (aiEnabled && vectorStore != null) {
            try {
                // Note: SimpleVectorStore doesn't support metadata filtering for delete
                // For now, we'll track manually and rebuild when needed
                indexedRepositories.remove(repositoryUrl);
                System.out.println("Marked repository for removal: " + repositoryUrl);
            } catch (Exception e) {
                System.err.println("Vector store delete failed: " + e.getMessage());
            }
        }
        
        // Also remove from fallback storage
        fallbackRepository.remove(repositoryUrl);
        System.out.println("Removed from fallback storage: " + repositoryUrl);
    }
    
    private Document convertToDocument(CodeChunk chunk) {
        // Create enriched metadata for better search
        Map<String, Object> metadata = new HashMap<>();
        
        // Basic metadata
        metadata.put("repositoryUrl", chunk.getRepositoryUrl());
        metadata.put("filePath", chunk.getFilePath());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        
        // Extract intelligent metadata for better filtering
        String filePath = chunk.getFilePath();
        String content = chunk.getContent();
        
        // File type and language detection
        metadata.put("fileExtension", extractFileExtension(filePath));
        metadata.put("language", detectLanguage(filePath));
        
        // Path-based categorization
        metadata.put("isController", isControllerFile(filePath, content));
        metadata.put("isService", isServiceFile(filePath, content));
        metadata.put("isRepository", isRepositoryFile(filePath, content));
        metadata.put("isTest", isTestFile(filePath));
        
        // Extract Java annotations if present
        Set<String> annotations = extractAnnotations(content);
        if (!annotations.isEmpty()) {
            metadata.put("annotations", String.join(",", annotations));
        }
        
        // Extract class names
        Set<String> classNames = extractClassNames(content);
        if (!classNames.isEmpty()) {
            metadata.put("classNames", String.join(",", classNames));
        }
        
        // Create enhanced content for embedding (includes context)
        String enhancedContent = createEnhancedContent(chunk, metadata);
        
        // Add original metadata if exists
        if (chunk.getMetadata() != null) {
            metadata.putAll(chunk.getMetadata());
        }
        
        return new Document(enhancedContent, metadata);
    }
    
    private CodeChunk convertToCodeChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        
        CodeChunk chunk = new CodeChunk();
        chunk.setContent(document.getContent());
        chunk.setRepositoryUrl((String) metadata.get("repositoryUrl"));
        chunk.setFilePath((String) metadata.get("filePath"));
        chunk.setChunkIndex((Integer) metadata.get("chunkIndex"));
        chunk.setMetadata(metadata);
        
        return chunk;
    }
    
    private boolean isRelevantForQuery(String query, CodeChunk chunk) {
        if (chunk.getMetadata() == null) return true;
        
        String lowerQuery = query.toLowerCase();
        Map<String, Object> metadata = chunk.getMetadata();
        
        // Controller-related queries
        if (lowerQuery.contains("controller")) {
            Boolean isController = (Boolean) metadata.get("isController");
            return isController != null && isController;
        }
        
        // Service-related queries  
        if (lowerQuery.contains("service")) {
            Boolean isService = (Boolean) metadata.get("isService");
            return isService != null && isService;
        }
        
        // Repository-related queries
        if (lowerQuery.contains("repository") || lowerQuery.contains("dao")) {
            Boolean isRepository = (Boolean) metadata.get("isRepository");
            return isRepository != null && isRepository;
        }
        
        // Test-related queries
        if (lowerQuery.contains("test")) {
            Boolean isTest = (Boolean) metadata.get("isTest");
            return isTest != null && isTest;
        }
        
        // Java-specific queries
        if (lowerQuery.contains("java") || lowerQuery.contains("class")) {
            String language = (String) metadata.get("language");
            return "java".equals(language);
        }
        
        return true; // No specific filter, include all results
    }
    
    private String createEnhancedContent(CodeChunk chunk, Map<String, Object> metadata) {
        StringBuilder enhanced = new StringBuilder();
        
        // Add contextual header
        enhanced.append("File: ").append(chunk.getFilePath()).append("\n");
        
        if (metadata.get("annotations") != null) {
            enhanced.append("Annotations: ").append(metadata.get("annotations")).append("\n");
        }
        
        if (metadata.get("classNames") != null) {
            enhanced.append("Classes: ").append(metadata.get("classNames")).append("\n");
        }
        
        enhanced.append("Content:\n").append(chunk.getContent());
        
        return enhanced.toString();
    }
    
    private String extractFileExtension(String filePath) {
        if (filePath == null) return "";
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "";
    }
    
    private String detectLanguage(String filePath) {
        String ext = extractFileExtension(filePath).toLowerCase();
        switch (ext) {
            case "java": return "java";
            case "js": case "jsx": return "javascript";
            case "ts": case "tsx": return "typescript";
            case "py": return "python";
            case "rb": return "ruby";
            case "go": return "go";
            case "rs": return "rust";
            default: return ext;
        }
    }
    
    private boolean isControllerFile(String filePath, String content) {
        if (filePath == null) return false;
        return filePath.toLowerCase().contains("controller") ||
               (content != null && (content.contains("@Controller") || content.contains("@RestController")));
    }
    
    private boolean isServiceFile(String filePath, String content) {
        if (filePath == null) return false;
        return filePath.toLowerCase().contains("service") ||
               (content != null && content.contains("@Service"));
    }
    
    private boolean isRepositoryFile(String filePath, String content) {
        if (filePath == null) return false;
        return filePath.toLowerCase().contains("repository") ||
               filePath.toLowerCase().contains("dao") ||
               (content != null && content.contains("@Repository"));
    }
    
    private boolean isTestFile(String filePath) {
        if (filePath == null) return false;
        return filePath.toLowerCase().contains("test") ||
               filePath.toLowerCase().contains("spec");
    }
    
    private Set<String> extractAnnotations(String content) {
        Set<String> annotations = new HashSet<>();
        if (content == null) return annotations;
        
        Pattern pattern = Pattern.compile("@([A-Za-z][A-Za-z0-9]*)", Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            annotations.add(matcher.group(1));
        }
        
        return annotations;
    }
    
    private Set<String> extractClassNames(String content) {
        Set<String> classNames = new HashSet<>();
        if (content == null) return classNames;
        
        Pattern pattern = Pattern.compile("(?:public|private|protected)?\\s*(?:static)?\\s*(?:abstract)?\\s*(?:class|interface|enum)\\s+([A-Za-z][A-Za-z0-9]*)", Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            classNames.add(matcher.group(1));
        }
        
        return classNames;
    }
    
    private List<CodeChunk> fallbackSearch(String query, String repositoryUrl, int maxResults) {
        // Simple keyword-based fallback when vector search is unavailable
        Map<String, List<CodeChunk>> searchSpace = repositoryUrl != null 
            ? Map.of(repositoryUrl, fallbackRepository.getOrDefault(repositoryUrl, new ArrayList<>()))
            : fallbackRepository;
            
        return searchSpace.values().stream()
                .flatMap(List::stream)
                .filter(chunk -> repositoryUrl == null || repositoryUrl.equals(chunk.getRepositoryUrl()))
                .filter(chunk -> simpleKeywordMatch(query, chunk.getContent()) > 0)
                .sorted((a, b) -> Double.compare(
                    simpleKeywordMatch(query, b.getContent()),
                    simpleKeywordMatch(query, a.getContent())))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    private double simpleKeywordMatch(String query, String content) {
        if (query == null || content == null) return 0.0;
        
        String[] queryWords = query.toLowerCase().split("\\W+");
        String lowerContent = content.toLowerCase();
        
        long matches = Arrays.stream(queryWords)
                .filter(word -> !word.isEmpty())
                .mapToLong(word -> lowerContent.split(Pattern.quote(word), -1).length - 1)
                .sum();
                
        return matches / (double) queryWords.length;
    }
    
    private void addToFallbackStorage(List<CodeChunk> codeChunks) {
        for (CodeChunk chunk : codeChunks) {
            String repositoryUrl = chunk.getRepositoryUrl();
            fallbackRepository.computeIfAbsent(repositoryUrl, k -> new ArrayList<>()).add(chunk);
        }
    }
    
    private List<CodeChunk> applySmartFiltering(String query, List<CodeChunk> chunks) {
        String lowerQuery = query.toLowerCase();
        
        // Controller-related queries
        if (lowerQuery.contains("controller")) {
            return chunks.stream()
                    .filter(chunk -> isRelevantForQuery(query, chunk))
                    .collect(Collectors.toList());
        }
        
        // Service-related queries  
        if (lowerQuery.contains("service")) {
            return chunks.stream()
                    .filter(chunk -> isRelevantForQuery(query, chunk))
                    .collect(Collectors.toList());
        }
        
        // Repository-related queries
        if (lowerQuery.contains("repository") || lowerQuery.contains("dao")) {
            return chunks.stream()
                    .filter(chunk -> isRelevantForQuery(query, chunk))
                    .collect(Collectors.toList());
        }
        
        // Test-related queries
        if (lowerQuery.contains("test")) {
            return chunks.stream()
                    .filter(chunk -> isRelevantForQuery(query, chunk))
                    .collect(Collectors.toList());
        }
        
        // Return empty list if no smart filtering applies
        return new ArrayList<>();
    }
    
    /**
     * Preprocess query to optimize vectorization by removing unnecessary elements
     * and normalizing the text for better semantic understanding.
     */
    private String preprocessQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        String processed = query.trim();
        
        // Remove excessive whitespace
        processed = processed.replaceAll("\\s+", " ");
        
        // Remove common stop words that don't add semantic meaning for code search
        processed = processed.replaceAll("\\b(the|a|an|and|or|but|in|on|at|to|for|of|with|by)\\b", " ");
        
        // Clean up multiple spaces again after stop word removal
        processed = processed.replaceAll("\\s+", " ").trim();
        
        // Expand common abbreviations for better matching
        processed = processed.replaceAll("\\bctrl\\b", "controller")
                            .replaceAll("\\bsvc\\b", "service")
                            .replaceAll("\\brepo\\b", "repository");
        
        System.out.println("Preprocessed query: '" + query + "' -> '" + processed + "'");
        return processed;
    }
}