package com.example.coderepoai.service.pullrequest;

import com.example.coderepoai.exception.RepositoryFetchException;
import com.example.coderepoai.model.PullRequest;
import com.example.coderepoai.model.ReviewComment;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GitHubPullRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubPullRequestService.class);
    
    private final GitHub github;
    
    public GitHubPullRequestService(@Value("${github.token:}") String githubToken) {
        try {
            String token = githubToken;
            if (token == null || token.trim().isEmpty()) {
                token = System.getenv("GITHUB_TOKEN");
            }
            
            if (token == null || token.trim().isEmpty()) {
                this.github = GitHub.connectAnonymously();
                logger.warn("Using anonymous GitHub connection - PR operations may be limited");
            } else {
                this.github = GitHub.connectUsingOAuth(token);
                logger.info("Connected to GitHub using OAuth token for PR operations");
            }
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to connect to GitHub for PR operations", e);
        }
    }
    
    /**
     * Fetch all open pull requests for a repository
     */
    public List<PullRequest> getOpenPullRequests(String repositoryUrl) {
        return getPullRequests(repositoryUrl, GHIssueState.OPEN);
    }
    
    /**
     * Fetch all pull requests (open and closed) for a repository
     */
    public List<PullRequest> getAllPullRequests(String repositoryUrl) {
        return getPullRequests(repositoryUrl, GHIssueState.ALL);
    }
    
    /**
     * Fetch pull requests with specified state
     */
    public List<PullRequest> getPullRequests(String repositoryUrl, GHIssueState state) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            
            List<GHPullRequest> ghPullRequests = repository.getPullRequests(state);
            List<PullRequest> pullRequests = new ArrayList<>();
            
            for (GHPullRequest ghPr : ghPullRequests) {
                PullRequest pr = convertToPullRequest(ghPr);
                pullRequests.add(pr);
            }
            
            logger.info("Fetched {} pull requests from repository: {}", pullRequests.size(), repositoryUrl);
            return pullRequests;
            
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to fetch pull requests from: " + repositoryUrl, e);
        }
    }
    
    /**
     * Get a specific pull request by number
     */
    public PullRequest getPullRequest(String repositoryUrl, int pullRequestNumber) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            
            GHPullRequest ghPr = repository.getPullRequest(pullRequestNumber);
            PullRequest pr = convertToPullRequest(ghPr);
            
            logger.info("Fetched pull request #{} from repository: {}", pullRequestNumber, repositoryUrl);
            return pr;
            
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to fetch pull request #" + pullRequestNumber + " from: " + repositoryUrl, e);
        }
    }
    
    /**
     * Get the diff content for a pull request
     */
    public String getPullRequestDiff(String repositoryUrl, int pullRequestNumber) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            
            GHPullRequest ghPr = repository.getPullRequest(pullRequestNumber);
            
            // Get the diff using GitHub API
            GHCompare comparison = repository.getCompare(ghPr.getBase().getSha(), ghPr.getHead().getSha());
            
            StringBuilder diffContent = new StringBuilder();
            for (GHCommit commit : comparison.getCommits()) {
                diffContent.append("Commit: ").append(commit.getSHA1()).append("\n");
                diffContent.append("Message: ").append(commit.getCommitShortInfo().getMessage()).append("\n\n");
            }
            
            // Get changed files
            PagedIterable<GHPullRequestFileDetail> files = ghPr.listFiles();
            for (GHPullRequestFileDetail file : files) {
                diffContent.append("File: ").append(file.getFilename()).append("\n");
                diffContent.append("Status: ").append(file.getStatus()).append("\n");
                diffContent.append("Additions: ").append(file.getAdditions())
                          .append(", Deletions: ").append(file.getDeletions()).append("\n");
                if (file.getPatch() != null) {
                    diffContent.append("Patch:\n").append(file.getPatch()).append("\n\n");
                }
            }
            
            logger.info("Fetched diff for pull request #{} from repository: {}", pullRequestNumber, repositoryUrl);
            return diffContent.toString();
            
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to fetch diff for pull request #" + pullRequestNumber + " from: " + repositoryUrl, e);
        }
    }
    
    /**
     * Get changed files in a pull request
     */
    public List<String> getPullRequestChangedFiles(String repositoryUrl, int pullRequestNumber) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            
            GHPullRequest ghPr = repository.getPullRequest(pullRequestNumber);
            PagedIterable<GHPullRequestFileDetail> files = ghPr.listFiles();
            
            List<String> changedFiles = new ArrayList<>();
            for (GHPullRequestFileDetail file : files) {
                changedFiles.add(file.getFilename());
            }
            
            logger.info("Found {} changed files in pull request #{}", changedFiles.size(), pullRequestNumber);
            return changedFiles;
            
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to fetch changed files for pull request #" + pullRequestNumber + " from: " + repositoryUrl, e);
        }
    }
    
    /**
     * Get existing review comments for a pull request
     */
    public List<ReviewComment> getPullRequestComments(String repositoryUrl, int pullRequestNumber) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            
            GHPullRequest ghPr = repository.getPullRequest(pullRequestNumber);
            PagedIterable<GHPullRequestReviewComment> ghComments = ghPr.listReviewComments();
            
            List<ReviewComment> comments = new ArrayList<>();
            for (GHPullRequestReviewComment ghComment : ghComments) {
                ReviewComment comment = convertToReviewComment(ghComment);
                comments.add(comment);
            }
            
            logger.info("Fetched {} review comments for pull request #{}", comments.size(), pullRequestNumber);
            return comments;
            
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to fetch comments for pull request #" + pullRequestNumber + " from: " + repositoryUrl, e);
        }
    }
    
    private PullRequest convertToPullRequest(GHPullRequest ghPr) throws IOException {
        PullRequest pr = new PullRequest();
        
        pr.setNumber(ghPr.getNumber());
        pr.setTitle(ghPr.getTitle());
        pr.setBody(ghPr.getBody());
        pr.setState(ghPr.getState().toString());
        pr.setHtmlUrl(ghPr.getHtmlUrl().toString());
        pr.setDiffUrl(ghPr.getDiffUrl().toString());
        
        if (ghPr.getCreatedAt() != null) {
            pr.setCreatedAt(convertToLocalDateTime(ghPr.getCreatedAt()));
        }
        if (ghPr.getUpdatedAt() != null) {
            pr.setUpdatedAt(convertToLocalDateTime(ghPr.getUpdatedAt()));
        }
        
        pr.setChangedFiles(ghPr.getChangedFiles());
        pr.setAdditions(ghPr.getAdditions());
        pr.setDeletions(ghPr.getDeletions());
        
        // Set base branch
        PullRequest.Branch base = new PullRequest.Branch();
        base.setRef(ghPr.getBase().getRef());
        base.setSha(ghPr.getBase().getSha());
        pr.setBase(base);
        
        // Set head branch  
        PullRequest.Branch head = new PullRequest.Branch();
        head.setRef(ghPr.getHead().getRef());
        head.setSha(ghPr.getHead().getSha());
        pr.setHead(head);
        
        // Set user
        PullRequest.User user = new PullRequest.User();
        user.setLogin(ghPr.getUser().getLogin());
        user.setAvatarUrl(ghPr.getUser().getAvatarUrl());
        pr.setUser(user);
        
        return pr;
    }
    
    private ReviewComment convertToReviewComment(GHPullRequestReviewComment ghComment) throws IOException {
        ReviewComment comment = new ReviewComment();
        
        comment.setId(ghComment.getId());
        comment.setPath(ghComment.getPath());
        comment.setPosition(ghComment.getPosition());
        comment.setOriginalPosition(ghComment.getOriginalPosition());
        comment.setLine(ghComment.getLine());
        comment.setSide(ghComment.getSide() != null ? ghComment.getSide().toString() : null);
        comment.setBody(ghComment.getBody());
        comment.setHtmlUrl(ghComment.getHtmlUrl().toString());
        comment.setDiffHunk(ghComment.getDiffHunk());
        
        if (ghComment.getCreatedAt() != null) {
            comment.setCreatedAt(convertToLocalDateTime(ghComment.getCreatedAt()));
        }
        if (ghComment.getUpdatedAt() != null) {
            comment.setUpdatedAt(convertToLocalDateTime(ghComment.getUpdatedAt()));
        }
        
        // Set user
        PullRequest.User user = new PullRequest.User();
        user.setLogin(ghComment.getUser().getLogin());
        user.setAvatarUrl(ghComment.getUser().getAvatarUrl());
        comment.setUser(user);
        
        return comment;
    }
    
    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDateTime();
    }
    
    private String extractRepoPathFromUrl(String repositoryUrl) {
        // Extract owner/repo from GitHub URL
        String[] parts = repositoryUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid repository URL: " + repositoryUrl);
        }
        return parts[0] + "/" + parts[1];
    }
}