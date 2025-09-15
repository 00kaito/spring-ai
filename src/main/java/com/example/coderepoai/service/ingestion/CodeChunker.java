package com.example.coderepoai.service.ingestion;

import com.example.coderepoai.model.CodeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeChunker {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeChunker.class);
    
    private static final int DEFAULT_CHUNK_SIZE = 1000; // characters
    private static final int OVERLAP_SIZE = 100; // characters
    
    // Patterns for identifying code blocks (methods, classes, functions)
    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile("(public|private|protected)?\\s+(static\\s+)?\\w+\\s+\\w+\\s*\\([^\\)]*\\)\\s*\\{");
    private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("(public|private)?\\s*(class|interface|enum)\\s+\\w+");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("function\\s+\\w+\\s*\\([^\\)]*\\)");
    private static final Pattern PYTHON_DEF_PATTERN = Pattern.compile("def\\s+\\w+\\s*\\([^\\)]*\\):");

    public List<CodeChunk> chunkFiles(Map<String, String> files, String repositoryUrl) {
        List<CodeChunk> chunks = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();
            
            List<CodeChunk> fileChunks = chunkSingleFile(content, filePath, repositoryUrl);
            chunks.addAll(fileChunks);
        }
        
        logger.info("Created {} chunks from {} files", chunks.size(), files.size());
        return chunks;
    }

    private List<CodeChunk> chunkSingleFile(String content, String filePath, String repositoryUrl) {
        List<CodeChunk> chunks = new ArrayList<>();
        
        // First, try to chunk by logical code blocks
        List<String> logicalChunks = chunkByCodeBlocks(content, filePath);
        
        // If logical chunking didn't work well, fall back to fixed-size chunking
        if (logicalChunks.isEmpty() || logicalChunks.size() == 1) {
            logicalChunks = chunkByFixedSize(content);
        }
        
        for (int i = 0; i < logicalChunks.size(); i++) {
            String chunkContent = logicalChunks.get(i);
            if (!chunkContent.trim().isEmpty()) {
                CodeChunk chunk = new CodeChunk(chunkContent, filePath, repositoryUrl, i);
                
                // Add metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("fileExtension", getFileExtension(filePath));
                metadata.put("chunkSize", chunkContent.length());
                metadata.put("totalChunks", logicalChunks.size());
                chunk.setMetadata(metadata);
                
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }

    private List<String> chunkByCodeBlocks(String content, String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        
        List<String> chunks = new ArrayList<>();
        
        switch (extension) {
            case "java":
                chunks = chunkJavaFile(content);
                break;
            case "js":
            case "ts":
                chunks = chunkJavaScriptFile(content);
                break;
            case "py":
                chunks = chunkPythonFile(content);
                break;
            default:
                // For other files, use fixed-size chunking
                chunks = chunkByFixedSize(content);
        }
        
        return chunks;
    }

    private List<String> chunkJavaFile(String content) {
        List<String> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        
        StringBuilder currentChunk = new StringBuilder();
        int braceLevel = 0;
        boolean inMethod = false;
        
        for (String line : lines) {
            currentChunk.append(line).append("\n");
            
            // Count braces to track code blocks
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceLevel++;
                    if (JAVA_METHOD_PATTERN.matcher(line).find()) {
                        inMethod = true;
                    }
                } else if (c == '}') {
                    braceLevel--;
                    if (braceLevel == 0 && inMethod) {
                        // End of method, create chunk
                        chunks.add(currentChunk.toString());
                        currentChunk = new StringBuilder();
                        inMethod = false;
                    }
                }
            }
            
            // If chunk gets too large, split anyway
            if (currentChunk.length() > DEFAULT_CHUNK_SIZE * 2) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
                braceLevel = 0;
                inMethod = false;
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    private List<String> chunkJavaScriptFile(String content) {
        List<String> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String line : lines) {
            currentChunk.append(line).append("\n");
            
            // Check for function boundaries
            if (FUNCTION_PATTERN.matcher(line).find()) {
                if (currentChunk.length() > DEFAULT_CHUNK_SIZE) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentChunk.append(line).append("\n");
                }
            }
            
            if (currentChunk.length() > DEFAULT_CHUNK_SIZE * 2) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    private List<String> chunkPythonFile(String content) {
        List<String> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String line : lines) {
            currentChunk.append(line).append("\n");
            
            // Check for function/class definitions
            if (PYTHON_DEF_PATTERN.matcher(line).find() || line.trim().startsWith("class ")) {
                if (currentChunk.length() > DEFAULT_CHUNK_SIZE) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentChunk.append(line).append("\n");
                }
            }
            
            if (currentChunk.length() > DEFAULT_CHUNK_SIZE * 2) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    private List<String> chunkByFixedSize(String content) {
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < content.length(); i += DEFAULT_CHUNK_SIZE - OVERLAP_SIZE) {
            int end = Math.min(i + DEFAULT_CHUNK_SIZE, content.length());
            String chunk = content.substring(i, end);
            chunks.add(chunk);
            
            if (end >= content.length()) {
                break;
            }
        }
        
        return chunks;
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}