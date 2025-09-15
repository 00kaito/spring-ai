package com.example.coderepoai.service.ingestion;

import com.example.coderepoai.exception.FileParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class FileParser {
    
    private static final Logger logger = LoggerFactory.getLogger(FileParser.class);
    
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*[#/\\*].*");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*(import|include|require|using)\\s+");

    public Map<String, String> parseFiles(Map<String, String> rawFiles) {
        Map<String, String> parsedFiles = new HashMap<>();
        
        for (Map.Entry<String, String> entry : rawFiles.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();
            
            try {
                String parsedContent = parseFileContent(content, filePath);
                if (!parsedContent.trim().isEmpty()) {
                    parsedFiles.put(filePath, parsedContent);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse file: {}", filePath, e);
                throw new FileParseException("Failed to parse file: " + filePath, e);
            }
        }
        
        return parsedFiles;
    }

    private String parseFileContent(String content, String filePath) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // Remove common binary file indicators
        if (isBinaryFile(content)) {
            logger.debug("Skipping binary file: {}", filePath);
            return "";
        }

        // Clean up the content
        String cleanedContent = cleanContent(content);
        
        // Add file metadata
        StringBuilder result = new StringBuilder();
        result.append("// File: ").append(filePath).append("\n");
        result.append(cleanedContent);
        
        return result.toString();
    }

    private boolean isBinaryFile(String content) {
        // Simple heuristic: if content contains null bytes or too many non-printable chars
        long nullBytes = content.chars().filter(ch -> ch == 0).count();
        if (nullBytes > 0) {
            return true;
        }
        
        long nonPrintableChars = content.chars()
            .filter(ch -> ch < 32 && ch != 9 && ch != 10 && ch != 13) // Tab, LF, CR are OK
            .count();
        
        return nonPrintableChars > content.length() * 0.1; // More than 10% non-printable
    }

    private String cleanContent(String content) {
        String[] lines = content.split("\n");
        StringBuilder cleaned = new StringBuilder();
        
        boolean inBlockComment = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Handle block comments (/* ... */)
            if (trimmedLine.contains("/*")) {
                inBlockComment = true;
            }
            if (inBlockComment && trimmedLine.contains("*/")) {
                inBlockComment = false;
                continue;
            }
            if (inBlockComment) {
                continue;
            }
            
            // Skip empty lines and single-line comments
            if (trimmedLine.isEmpty() || COMMENT_PATTERN.matcher(trimmedLine).matches()) {
                continue;
            }
            
            // Keep import statements but mark them
            if (IMPORT_PATTERN.matcher(trimmedLine).matches()) {
                cleaned.append("// Import: ").append(trimmedLine).append("\n");
                continue;
            }
            
            cleaned.append(line).append("\n");
        }
        
        return cleaned.toString();
    }
}