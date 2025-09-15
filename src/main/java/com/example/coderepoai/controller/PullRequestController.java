package com.example.coderepoai.controller;

import com.example.coderepoai.model.CodeReview;
import com.example.coderepoai.model.PullRequest;
import com.example.coderepoai.model.ReviewComment;
import com.example.coderepoai.service.pullrequest.CodeReviewService;
import com.example.coderepoai.service.pullrequest.GitHubCommentService;
import com.example.coderepoai.service.pullrequest.GitHubPullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pullrequests")
@CrossOrigin(origins = "*")
public class PullRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(PullRequestController.class);
    
    private final GitHubPullRequestService pullRequestService;
    private final CodeReviewService codeReviewService;
    private final GitHubCommentService commentService;
    
    public PullRequestController(GitHubPullRequestService pullRequestService,
                               CodeReviewService codeReviewService,
                               GitHubCommentService commentService) {
        this.pullRequestService = pullRequestService;
        this.codeReviewService = codeReviewService;
        this.commentService = commentService;
    }
    
    /**
     * Get all open pull requests for a repository
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPullRequests(@RequestParam String repository_url) {
        logger.info("Received request to fetch pull requests for repository: {}", repository_url);
        
        try {
            List<PullRequest> pullRequests = pullRequestService.getOpenPullRequests(repository_url);
            
            return ResponseEntity.ok(Map.of(
                "pull_requests", pullRequests,
                "count", pullRequests.size(),
                "repository_url", repository_url,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching pull requests for repository: {}", repository_url, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch pull requests: " + e.getMessage(),
                "repository_url", repository_url,
                "status", "error"
            ));
        }
    }
    
    /**
     * Get a specific pull request by number
     */
    @GetMapping("/{pullRequestNumber}")
    public ResponseEntity<Map<String, Object>> getPullRequest(
            @RequestParam String repository_url,
            @PathVariable int pullRequestNumber) {
        
        logger.info("Received request to fetch pull request #{} for repository: {}", pullRequestNumber, repository_url);
        
        try {
            PullRequest pullRequest = pullRequestService.getPullRequest(repository_url, pullRequestNumber);
            
            return ResponseEntity.ok(Map.of(
                "pull_request", pullRequest,
                "repository_url", repository_url,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching pull request #{} for repository: {}", pullRequestNumber, repository_url, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch pull request: " + e.getMessage(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "error"
            ));
        }
    }
    
    /**
     * Get the diff content for a pull request
     */
    @GetMapping("/{pullRequestNumber}/diff")
    public ResponseEntity<Map<String, Object>> getPullRequestDiff(
            @RequestParam String repository_url,
            @PathVariable int pullRequestNumber) {
        
        logger.info("Received request to fetch diff for pull request #{} in repository: {}", pullRequestNumber, repository_url);
        
        try {
            String diff = pullRequestService.getPullRequestDiff(repository_url, pullRequestNumber);
            List<String> changedFiles = pullRequestService.getPullRequestChangedFiles(repository_url, pullRequestNumber);
            
            return ResponseEntity.ok(Map.of(
                "diff", diff,
                "changed_files", changedFiles,
                "file_count", changedFiles.size(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching diff for pull request #{} in repository: {}", pullRequestNumber, repository_url, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch diff: " + e.getMessage(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "error"
            ));
        }
    }
    
    /**
     * Perform AI code review on a pull request
     */
    @PostMapping("/{pullRequestNumber}/review")
    public ResponseEntity<Map<String, Object>> reviewPullRequest(
            @RequestParam String repository_url,
            @PathVariable int pullRequestNumber,
            @RequestParam(defaultValue = "false") boolean post_comments) {
        
        logger.info("Received request to review pull request #{} in repository: {} (post_comments: {})", 
                   pullRequestNumber, repository_url, post_comments);
        
        try {
            // Perform AI code review
            CodeReview codeReview = codeReviewService.reviewPullRequest(repository_url, pullRequestNumber);
            
            boolean commentsPosted = false;
            String commentStatus = "not_requested";
            
            // Post comments to GitHub if requested
            if (post_comments) {
                try {
                    commentsPosted = commentService.postCodeReviewComments(repository_url, codeReview);
                    commentStatus = commentsPosted ? "posted" : "failed";
                } catch (Exception e) {
                    logger.warn("Failed to post comments to GitHub: {}", e.getMessage());
                    commentStatus = "failed";
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "code_review", codeReview,
                "comments_posted", commentsPosted,
                "comment_status", commentStatus,
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error reviewing pull request #{} in repository: {}", pullRequestNumber, repository_url, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to review pull request: " + e.getMessage(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "error"
            ));
        }
    }
    
    /**
     * Get existing review comments for a pull request
     */
    @GetMapping("/{pullRequestNumber}/comments")
    public ResponseEntity<Map<String, Object>> getPullRequestComments(
            @RequestParam String repository_url,
            @PathVariable int pullRequestNumber) {
        
        logger.info("Received request to fetch comments for pull request #{} in repository: {}", pullRequestNumber, repository_url);
        
        try {
            List<ReviewComment> comments = pullRequestService.getPullRequestComments(repository_url, pullRequestNumber);
            
            return ResponseEntity.ok(Map.of(
                "comments", comments,
                "count", comments.size(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching comments for pull request #{} in repository: {}", pullRequestNumber, repository_url, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch comments: " + e.getMessage(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "error"
            ));
        }
    }
    
    /**
     * Post a simple review summary comment to a pull request
     */
    @PostMapping("/{pullRequestNumber}/comment")
    public ResponseEntity<Map<String, Object>> postComment(
            @RequestParam String repository_url,
            @PathVariable int pullRequestNumber,
            @RequestBody Map<String, String> request) {
        
        String comment = request.get("comment");
        if (comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Comment content is required",
                "status", "error"
            ));
        }
        
        logger.info("Received request to post comment to pull request #{} in repository: {}", pullRequestNumber, repository_url);
        
        try {
            boolean success = commentService.postSimpleReviewSummary(repository_url, pullRequestNumber, comment);
            
            return ResponseEntity.ok(Map.of(
                "comment_posted", success,
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", success ? "success" : "failed"
            ));
            
        } catch (Exception e) {
            logger.error("Error posting comment to pull request #{} in repository: {}", pullRequestNumber, repository_url, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to post comment: " + e.getMessage(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repository_url,
                "status", "error"
            ));
        }
    }
    
    /**
     * Comprehensive endpoint: Review PR and automatically post comments
     */
    @PostMapping("/review-and-comment")
    public ResponseEntity<Map<String, Object>> reviewAndComment(@RequestBody Map<String, Object> request) {
        String repositoryUrl = (String) request.get("repository_url");
        Integer pullRequestNumber = (Integer) request.get("pull_request_number");
        
        if (repositoryUrl == null || pullRequestNumber == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "repository_url and pull_request_number are required",
                "status", "error"
            ));
        }
        
        logger.info("Received request to review and comment on pull request #{} in repository: {}", pullRequestNumber, repositoryUrl);
        
        try {
            // Perform code review
            CodeReview codeReview = codeReviewService.reviewPullRequest(repositoryUrl, pullRequestNumber);
            
            // Post comments automatically
            boolean commentsPosted = commentService.postCodeReviewComments(repositoryUrl, codeReview);
            
            return ResponseEntity.ok(Map.of(
                "code_review", codeReview,
                "comments_posted", commentsPosted,
                "pull_request_number", pullRequestNumber,
                "repository_url", repositoryUrl,
                "status", "success"
            ));
            
        } catch (Exception e) {
            logger.error("Error in review and comment operation for pull request #{} in repository: {}", pullRequestNumber, repositoryUrl, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to review and comment: " + e.getMessage(),
                "pull_request_number", pullRequestNumber,
                "repository_url", repositoryUrl,
                "status", "error"
            ));
        }
    }
}