package com.example.coderepoai.service.pullrequest;

import com.example.coderepoai.exception.RepositoryFetchException;
import com.example.coderepoai.model.CodeReview;
import com.example.coderepoai.model.ReviewComment;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubCommentService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubCommentService.class);
    
    private final GitHub github;
    
    public GitHubCommentService(@Value("${github.token:}") String githubToken) {
        try {
            String token = githubToken;
            if (token == null || token.trim().isEmpty()) {
                token = System.getenv("GITHUB_TOKEN");
            }
            
            if (token == null || token.trim().isEmpty()) {
                throw new RepositoryFetchException("GitHub token is required for commenting on pull requests");
            }
            
            this.github = GitHub.connectUsingOAuth(token);
            logger.info("Connected to GitHub for commenting operations");
            
        } catch (IOException e) {
            throw new RepositoryFetchException("Failed to connect to GitHub for commenting", e);
        }
    }
    
    /**
     * Post a complete AI code review as comments on a GitHub pull request
     */
    public boolean postCodeReviewComments(String repositoryUrl, CodeReview codeReview) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            GHPullRequest pullRequest = repository.getPullRequest(codeReview.getPullRequestNumber());
            
            // Post summary comment first
            postSummaryComment(pullRequest, codeReview);
            
            // Post individual line comments
            int successfulComments = 0;
            List<ReviewComment> failedComments = new ArrayList<>();
            
            for (ReviewComment comment : codeReview.getComments()) {
                try {
                    if (comment.getPath() != null && !comment.getPath().isEmpty()) {
                        postLineComment(pullRequest, comment);
                        successfulComments++;
                    } else {
                        // Post as general comment if no specific file/line
                        postGeneralComment(pullRequest, comment);
                        successfulComments++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to post comment: {}", e.getMessage());
                    failedComments.add(comment);
                }
            }
            
            logger.info("Posted {} out of {} comments successfully for PR #{}", 
                       successfulComments, codeReview.getComments().size(), codeReview.getPullRequestNumber());
            
            // Post failed comments summary if any
            if (!failedComments.isEmpty()) {
                postFailedCommentsReport(pullRequest, failedComments);
            }
            
            return failedComments.isEmpty();
            
        } catch (Exception e) {
            logger.error("Failed to post code review comments for PR #{}: {}", 
                        codeReview.getPullRequestNumber(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Post a summary comment with overall review findings
     */
    private void postSummaryComment(GHPullRequest pullRequest, CodeReview codeReview) throws IOException {
        StringBuilder summary = new StringBuilder();
        
        summary.append("## 🤖 AI Code Review Summary\n\n");
        
        // Overall score and state
        summary.append(String.format("**Overall Score:** %d/100\n", 
                      codeReview.getOverallScore() != null ? codeReview.getOverallScore() : 0));
        summary.append(String.format("**Review State:** %s\n", getStateEmoji(codeReview.getState())));
        summary.append(String.format("**AI Model:** %s\n", codeReview.getAiModel()));
        summary.append(String.format("**Comments Generated:** %d\n\n", codeReview.getCommentCount()));
        
        // Review summary
        if (codeReview.getReviewSummary() != null) {
            summary.append("### Summary\n");
            summary.append(codeReview.getReviewSummary()).append("\n\n");
        }
        
        // Issues found
        if (!codeReview.getDetectedIssues().isEmpty()) {
            summary.append("### 🔍 Issues Detected\n");
            for (String issue : codeReview.getDetectedIssues()) {
                summary.append("- ").append(issue).append("\n");
            }
            summary.append("\n");
        }
        
        // Positive aspects
        if (!codeReview.getPositiveAspects().isEmpty()) {
            summary.append("### ✅ Positive Aspects\n");
            for (String positive : codeReview.getPositiveAspects()) {
                summary.append("- ").append(positive).append("\n");
            }
            summary.append("\n");
        }
        
        // Critical issues warning
        long criticalIssues = codeReview.getCriticalIssuesCount();
        if (criticalIssues > 0) {
            summary.append(String.format("### ⚠️ Critical Issues\n"));
            summary.append(String.format("Found **%d critical issues** that should be addressed before merging.\n\n", criticalIssues));
        }
        
        summary.append("---\n");
        summary.append("*This review was generated by AI. Please review the suggestions and use your judgment.*");
        
        pullRequest.comment(summary.toString());
        logger.info("Posted summary comment for PR #{}", pullRequest.getNumber());
    }
    
    /**
     * Post a comment on a specific line of code
     */
    private void postLineComment(GHPullRequest pullRequest, ReviewComment comment) throws IOException {
        if (comment.getLine() == null) {
            // If no specific line, post as general comment
            postGeneralComment(pullRequest, comment);
            return;
        }
        
        String formattedComment = formatReviewComment(comment);
        
        try {
            // Try to create a review comment on the specific line
            pullRequest.createReviewComment(
                formattedComment,
                pullRequest.getHead().getSha(),
                comment.getPath(),
                comment.getLine()
            );
            
            logger.debug("Posted line comment on {}:{} for PR #{}", 
                        comment.getPath(), comment.getLine(), pullRequest.getNumber());
            
        } catch (IOException e) {
            // If line comment fails, fall back to general comment
            logger.warn("Failed to post line comment, posting as general comment: {}", e.getMessage());
            postGeneralComment(pullRequest, comment);
        }
    }
    
    /**
     * Post a general comment on the pull request
     */
    private void postGeneralComment(GHPullRequest pullRequest, ReviewComment comment) throws IOException {
        String formattedComment = formatReviewComment(comment);
        
        // Add file context if available
        if (comment.getPath() != null && !comment.getPath().isEmpty()) {
            formattedComment = String.format("**File:** `%s`%s\n\n%s", 
                                           comment.getPath(),
                                           comment.getLine() != null ? " (Line " + comment.getLine() + ")" : "",
                                           formattedComment);
        }
        
        pullRequest.comment(formattedComment);
        logger.debug("Posted general comment for PR #{}", pullRequest.getNumber());
    }
    
    /**
     * Format a review comment with appropriate styling
     */
    private String formatReviewComment(ReviewComment comment) {
        StringBuilder formatted = new StringBuilder();
        
        // Add type and severity indicators
        String typeEmoji = getTypeEmoji(comment.getSuggestionType());
        String severityText = getSeverityText(comment.getSeverity());
        
        if (typeEmoji != null || severityText != null) {
            formatted.append("**");
            if (typeEmoji != null) {
                formatted.append(typeEmoji).append(" ");
            }
            if (comment.getSuggestionType() != null) {
                formatted.append(comment.getSuggestionType());
            }
            if (severityText != null) {
                formatted.append(" (").append(severityText).append(")");
            }
            formatted.append("**\n\n");
        }
        
        // Add the main comment body
        formatted.append(comment.getBody());
        
        return formatted.toString();
    }
    
    /**
     * Post a report of failed comments
     */
    private void postFailedCommentsReport(GHPullRequest pullRequest, List<ReviewComment> failedComments) throws IOException {
        StringBuilder report = new StringBuilder();
        
        report.append("## 📝 Additional Review Comments\n\n");
        report.append("Some comments could not be posted to specific lines. Here are the additional review points:\n\n");
        
        for (int i = 0; i < failedComments.size(); i++) {
            ReviewComment comment = failedComments.get(i);
            report.append(String.format("### %d. ", i + 1));
            
            if (comment.getPath() != null) {
                report.append(String.format("File: `%s`", comment.getPath()));
                if (comment.getLine() != null) {
                    report.append(String.format(" (Line %d)", comment.getLine()));
                }
                report.append("\n");
            }
            
            report.append(formatReviewComment(comment)).append("\n\n");
        }
        
        pullRequest.comment(report.toString());
        logger.info("Posted failed comments report with {} items for PR #{}", 
                   failedComments.size(), pullRequest.getNumber());
    }
    
    /**
     * Post a simple review summary without detailed comments
     */
    public boolean postSimpleReviewSummary(String repositoryUrl, int pullRequestNumber, String summary) {
        try {
            String repoPath = extractRepoPathFromUrl(repositoryUrl);
            GHRepository repository = github.getRepository(repoPath);
            GHPullRequest pullRequest = repository.getPullRequest(pullRequestNumber);
            
            String formattedSummary = "## 🤖 AI Code Review\n\n" + summary + 
                                    "\n\n---\n*This review was generated by AI.*";
            
            pullRequest.comment(formattedSummary);
            
            logger.info("Posted simple review summary for PR #{}", pullRequestNumber);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to post simple review summary for PR #{}: {}", pullRequestNumber, e.getMessage());
            return false;
        }
    }
    
    private String getStateEmoji(String state) {
        if (state == null) return "❓ Unknown";
        
        return switch (state.toLowerCase()) {
            case "approved" -> "✅ Approved";
            case "changes_requested" -> "❌ Changes Requested";
            case "commented" -> "💬 Commented";
            case "pending" -> "⏳ Pending";
            default -> "❓ " + state;
        };
    }
    
    private String getTypeEmoji(String type) {
        if (type == null) return null;
        
        return switch (type.toLowerCase()) {
            case "bug" -> "🐛";
            case "security" -> "🔒";
            case "performance" -> "⚡";
            case "style" -> "🎨";
            case "improvement" -> "💡";
            default -> "📝";
        };
    }
    
    private String getSeverityText(Integer severity) {
        if (severity == null) return null;
        
        return switch (severity) {
            case 1 -> "Low";
            case 2 -> "Minor";
            case 3 -> "Moderate";
            case 4 -> "High";
            case 5 -> "Critical";
            default -> "Unknown";
        };
    }
    
    private String extractRepoPathFromUrl(String repositoryUrl) {
        String[] parts = repositoryUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid repository URL: " + repositoryUrl);
        }
        return parts[0] + "/" + parts[1];
    }
}