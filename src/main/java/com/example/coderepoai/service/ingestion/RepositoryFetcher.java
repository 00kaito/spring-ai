package com.example.coderepoai.service.ingestion;

import com.example.coderepoai.exception.RepositoryFetchException;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class RepositoryFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryFetcher.class);
    
    private final GitHub github;
    
    public RepositoryFetcher(@Value("${github.token:}") String githubToken) {
        try {
            // Try Spring property first, then environment variable
            String token = githubToken;
            if (token == null || token.trim().isEmpty()) {
                token = System.getenv("GITHUB_TOKEN");
            }
            
            if (token == null || token.trim().isEmpty()) {
                this.github = GitHub.connectAnonymously();
                logger.warn("Using anonymous GitHub connection - rate limits will be stricter");
            } else {
                this.github = GitHub.connectUsingOAuth(token);
                logger.info("Connected to GitHub using OAuth token");
            }
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to connect to GitHub", e);
        }
    }

    public Map<String, String> fetchRepositoryFiles(String repositoryUrl) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            
            Map<String, String> files = new HashMap<>();
            fetchFilesRecursively(repository, "", files);
            
            return files;
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to fetch repository: " + repositoryUrl, e);
        }
    }

    private void fetchFilesRecursively(GHRepository repository, String path, Map<String, String> files) throws IOException {
        List<GHContent> contents = repository.getDirectoryContent(path);
        
        for (GHContent content : contents) {
            if (content.isFile()) {
                String filename = content.getName().toLowerCase();
                if (shouldProcessFile(filename)) {
                    try {
                        String fileContent = content.getContent();
                        files.put(content.getPath(), fileContent);
                        logger.debug("Fetched file: {}", content.getPath());
                    } catch (IOException e) {
                        logger.warn("Failed to fetch content for file: {}", content.getPath(), e);
                    }
                }
            } else if (content.isDirectory()) {
                fetchFilesRecursively(repository, content.getPath(), files);
            }
        }
    }

    private boolean shouldProcessFile(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        
        String lowerName = filename.toLowerCase();
        
        // Common code file extensions
        Set<String> supportedExtensions = Set.of(
            ".java", ".js", ".ts", ".py", ".cpp", ".c", ".h", ".hpp",
            ".cs", ".go", ".rs", ".php", ".rb", ".swift", ".kt",
            ".scala", ".clj", ".hs", ".ml", ".r", ".sql", ".md",
            ".txt", ".json", ".yml", ".yaml", ".xml", ".gradle",
            ".properties", ".proto", ".sh", ".bash", ".css", ".html"
        );
        
        // Check file extensions
        if (supportedExtensions.stream().anyMatch(lowerName::endsWith)) {
            return true;
        }
        
        // Check for common files without extensions
        Set<String> commonFiles = Set.of(
            "readme", "license", "dockerfile", "makefile", "rakefile",
            "gemfile", "requirements", "package", "composer", "gulpfile"
        );
        
        return commonFiles.stream().anyMatch(lowerName::startsWith);
    }

    private String extractRepoPathFromUrl(String repositoryUrl) {
        // Extract owner/repo from GitHub URL
        // Examples: 
        // https://github.com/owner/repo -> owner/repo
        // https://github.com/owner/repo.git -> owner/repo
        String cleanUrl = repositoryUrl.replace("https://github.com/", "").replace(".git", "");
        if (cleanUrl.contains("/")) {
            return cleanUrl;
        }
        throw new IllegalArgumentException("Invalid GitHub repository URL: " + repositoryUrl);
    }
}