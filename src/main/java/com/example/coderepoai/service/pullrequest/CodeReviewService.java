package com.example.coderepoai.service.pullrequest;

import com.example.coderepoai.model.CodeReview;
import com.example.coderepoai.model.PullRequest;
import com.example.coderepoai.model.ReviewComment;
import com.example.coderepoai.service.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeReviewService.class);
    
    private final ChatClient chatClient;
    private final GitHubPullRequestService pullRequestService;
    
    @Autowired
    public CodeReviewService(ChatClient chatClient, GitHubPullRequestService pullRequestService) {
        this.chatClient = chatClient;
        this.pullRequestService = pullRequestService;
    }
    
    /**
     * Perform AI-powered code review on a pull request
     */
    public CodeReview reviewPullRequest(String repositoryUrl, int pullRequestNumber) {
        logger.info("Starting AI code review for PR #{} in repository: {}", pullRequestNumber, repositoryUrl);
        
        try {
            // Get pull request details
            PullRequest pullRequest = pullRequestService.getPullRequest(repositoryUrl, pullRequestNumber);
            
            // Get the diff content
            String diffContent = pullRequestService.getPullRequestDiff(repositoryUrl, pullRequestNumber);
            
            // Create code review object
            CodeReview codeReview = new CodeReview(pullRequestNumber, repositoryUrl);
            codeReview.setCommitId(pullRequest.getHead().getSha());
            codeReview.setReviewedAt(LocalDateTime.now());
            
            // Perform AI analysis
            if (chatClient != null) {
                performAICodeReview(codeReview, pullRequest, diffContent);
                codeReview.setAiModel("gpt-3.5-turbo");
            } else {
                performFallbackCodeReview(codeReview, pullRequest, diffContent);
                codeReview.setAiModel("fallback-analysis");
            }
            
            // Determine overall review state
            determineReviewState(codeReview);
            
            logger.info("Completed code review for PR #{}: {} comments, overall score: {}", 
                       pullRequestNumber, codeReview.getCommentCount(), codeReview.getOverallScore());
            
            return codeReview;
            
        } catch (Exception e) {
            logger.error("Failed to review pull request #{} in repository: {}", pullRequestNumber, repositoryUrl, e);
            throw new RuntimeException("Code review failed: " + e.getMessage(), e);
        }
    }
    
    private void performAICodeReview(CodeReview codeReview, PullRequest pullRequest, String diffContent) {
        try {
            String reviewPrompt = buildReviewPrompt(pullRequest, diffContent);
            
            String aiResponse = chatClient.prompt(reviewPrompt)
                    .call()
                    .content();
            
            // Parse AI response and extract review insights
            parseAIReviewResponse(codeReview, aiResponse, diffContent);
            
            logger.info("AI review completed successfully for PR #{}", pullRequest.getNumber());
            
        } catch (Exception e) {
            logger.warn("AI review failed, falling back to heuristic analysis: {}", e.getMessage());
            performFallbackCodeReview(codeReview, pullRequest, diffContent);
            codeReview.setAiModel("fallback-analysis (AI failed)");
        }
    }
    
    private void performFallbackCodeReview(CodeReview codeReview, PullRequest pullRequest, String diffContent) {
        logger.info("Performing fallback code review analysis for PR #{}", pullRequest.getNumber());
        
        // Basic heuristic analysis
        List<ReviewComment> comments = new ArrayList<>();
        
        // Check for common code issues
        comments.addAll(checkForCommonIssues(diffContent));
        
        // Check file changes
        comments.addAll(checkFileChanges(pullRequest, diffContent));
        
        // Set review summary
        String summary = generateFallbackSummary(pullRequest, diffContent, comments);
        codeReview.setReviewSummary(summary);
        codeReview.setComments(comments);
        
        // Calculate basic score
        int score = calculateFallbackScore(pullRequest, comments);
        codeReview.setOverallScore(score);
        
        // Add detected issues and positive aspects
        addFallbackInsights(codeReview, pullRequest, diffContent);
    }
    
    private String buildReviewPrompt(PullRequest pullRequest, String diffContent) {
        return """
            Please perform a comprehensive code review for this pull request.
            
            Pull Request Details:
            - Title: %s
            - Description: %s
            - Changed Files: %d
            - Additions: %d, Deletions: %d
            
            Diff Content:
            %s
            
            Please provide a detailed code review including:
            1. Overall assessment and score (1-100)
            2. Specific line-by-line comments for issues found
            3. Security concerns
            4. Performance implications
            5. Code quality and maintainability
            6. Best practices compliance
            7. Positive aspects of the code
            8. Suggested improvements
            
            Format your response as:
            OVERALL_SCORE: [number]
            SUMMARY: [overall assessment]
            
            ISSUES:
            - [issue 1]
            - [issue 2]
            
            POSITIVES:
            - [positive 1]
            - [positive 2]
            
            COMMENTS:
            FILE: [filename]
            LINE: [line number]
            TYPE: [BUG|STYLE|PERFORMANCE|SECURITY|IMPROVEMENT]
            SEVERITY: [1-5]
            COMMENT: [detailed comment]
            ---
            [repeat for each comment]
            """.formatted(
                pullRequest.getTitle(),
                pullRequest.getBody() != null ? pullRequest.getBody() : "No description",
                pullRequest.getChangedFiles(),
                pullRequest.getAdditions(),
                pullRequest.getDeletions(),
                diffContent.length() > 8000 ? diffContent.substring(0, 8000) + "...[truncated]" : diffContent
            );
    }
    
    private void parseAIReviewResponse(CodeReview codeReview, String aiResponse, String diffContent) {
        // Extract overall score
        Pattern scorePattern = Pattern.compile("OVERALL_SCORE:\\s*(\\d+)");
        Matcher scoreMatcher = scorePattern.matcher(aiResponse);
        if (scoreMatcher.find()) {
            codeReview.setOverallScore(Integer.parseInt(scoreMatcher.group(1)));
        }
        
        // Extract summary
        Pattern summaryPattern = Pattern.compile("SUMMARY:\\s*([^\\n]+(?:\\n(?!\\w+:)[^\\n]+)*)");
        Matcher summaryMatcher = summaryPattern.matcher(aiResponse);
        if (summaryMatcher.find()) {
            codeReview.setReviewSummary(summaryMatcher.group(1).trim());
        }
        
        // Extract issues
        Pattern issuesPattern = Pattern.compile("ISSUES:\\s*([^\\n]+(?:\\n-[^\\n]+)*)");
        Matcher issuesMatcher = issuesPattern.matcher(aiResponse);
        if (issuesMatcher.find()) {
            String issuesText = issuesMatcher.group(1);
            String[] issues = issuesText.split("\\n-\\s*");
            for (String issue : issues) {
                if (!issue.trim().isEmpty() && !issue.startsWith("ISSUES:")) {
                    codeReview.addDetectedIssue(issue.trim().replaceFirst("^-\\s*", ""));
                }
            }
        }
        
        // Extract positives
        Pattern positivesPattern = Pattern.compile("POSITIVES:\\s*([^\\n]+(?:\\n-[^\\n]+)*)");
        Matcher positivesMatcher = positivesPattern.matcher(aiResponse);
        if (positivesMatcher.find()) {
            String positivesText = positivesMatcher.group(1);
            String[] positives = positivesText.split("\\n-\\s*");
            for (String positive : positives) {
                if (!positive.trim().isEmpty() && !positive.startsWith("POSITIVES:")) {
                    codeReview.addPositiveAspect(positive.trim().replaceFirst("^-\\s*", ""));
                }
            }
        }
        
        // Parse individual comments
        parseIndividualComments(codeReview, aiResponse);
    }
    
    private void parseIndividualComments(CodeReview codeReview, String aiResponse) {
        String[] commentBlocks = aiResponse.split("---");
        
        for (String block : commentBlocks) {
            if (block.contains("FILE:") && block.contains("COMMENT:")) {
                try {
                    String file = extractValue(block, "FILE:");
                    String lineStr = extractValue(block, "LINE:");
                    String type = extractValue(block, "TYPE:");
                    String severityStr = extractValue(block, "SEVERITY:");
                    String comment = extractValue(block, "COMMENT:");
                    
                    if (file != null && comment != null) {
                        ReviewComment reviewComment = new ReviewComment();
                        reviewComment.setPath(file);
                        if (lineStr != null) {
                            try {
                                reviewComment.setLine(Integer.parseInt(lineStr));
                            } catch (NumberFormatException e) {
                                // Skip if line number is invalid
                            }
                        }
                        reviewComment.setBody(comment);
                        reviewComment.setSuggestionType(type);
                        if (severityStr != null) {
                            try {
                                reviewComment.setSeverity(Integer.parseInt(severityStr));
                            } catch (NumberFormatException e) {
                                reviewComment.setSeverity(3); // Default severity
                            }
                        }
                        
                        codeReview.addComment(reviewComment);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse comment block: {}", e.getMessage());
                }
            }
        }
    }
    
    private String extractValue(String text, String key) {
        Pattern pattern = Pattern.compile(key + "\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
    
    private List<ReviewComment> checkForCommonIssues(String diffContent) {
        List<ReviewComment> comments = new ArrayList<>();
        
        // Check for common security issues
        if (diffContent.contains("password") && diffContent.contains("=")) {
            comments.add(new ReviewComment("", null, 
                "⚠️ Potential security issue: Hardcoded password detected. Use environment variables or secure vaults.", 
                "SECURITY", 5));
        }
        
        if (diffContent.contains("TODO") || diffContent.contains("FIXME")) {
            comments.add(new ReviewComment("", null, 
                "📝 TODO/FIXME comments found. Consider creating issues for tracking.", 
                "IMPROVEMENT", 2));
        }
        
        if (diffContent.contains("console.log") || diffContent.contains("System.out.print")) {
            comments.add(new ReviewComment("", null, 
                "🔍 Debug statements found. Consider removing or using proper logging.", 
                "STYLE", 2));
        }
        
        return comments;
    }
    
    private List<ReviewComment> checkFileChanges(PullRequest pullRequest, String diffContent) {
        List<ReviewComment> comments = new ArrayList<>();
        
        if (pullRequest.getChangedFiles() > 20) {
            comments.add(new ReviewComment("", null, 
                "📊 Large PR with " + pullRequest.getChangedFiles() + " files changed. Consider breaking into smaller PRs for easier review.", 
                "IMPROVEMENT", 3));
        }
        
        if (pullRequest.getAdditions() > 500) {
            comments.add(new ReviewComment("", null, 
                "📈 Large number of additions (" + pullRequest.getAdditions() + " lines). Ensure adequate testing.", 
                "IMPROVEMENT", 3));
        }
        
        return comments;
    }
    
    private String generateFallbackSummary(PullRequest pullRequest, String diffContent, List<ReviewComment> comments) {
        return String.format(
            "Code review completed for PR #%d. " +
            "Changes: %d files modified, %d additions, %d deletions. " +
            "Found %d issues to address. " +
            "The changes appear to be %s in scope.",
            pullRequest.getNumber(),
            pullRequest.getChangedFiles(),
            pullRequest.getAdditions(),
            pullRequest.getDeletions(),
            comments.size(),
            pullRequest.getChangedFiles() > 10 ? "large" : "moderate"
        );
    }
    
    private int calculateFallbackScore(PullRequest pullRequest, List<ReviewComment> comments) {
        int baseScore = 85;
        
        // Deduct points for issues
        long criticalIssues = comments.stream().filter(c -> c.getSeverity() != null && c.getSeverity() >= 4).count();
        long moderateIssues = comments.stream().filter(c -> c.getSeverity() != null && c.getSeverity() == 3).count();
        
        baseScore -= (int)(criticalIssues * 15);
        baseScore -= (int)(moderateIssues * 8);
        
        // Deduct points for large changes
        if (pullRequest.getChangedFiles() > 20) baseScore -= 10;
        if (pullRequest.getAdditions() > 500) baseScore -= 10;
        
        return Math.max(baseScore, 0);
    }
    
    private void addFallbackInsights(CodeReview codeReview, PullRequest pullRequest, String diffContent) {
        // Add detected issues
        if (diffContent.contains("password") || diffContent.contains("secret")) {
            codeReview.addDetectedIssue("Potential hardcoded credentials detected");
        }
        
        if (pullRequest.getChangedFiles() > 20) {
            codeReview.addDetectedIssue("Large pull request with many file changes");
        }
        
        // Add positive aspects
        if (pullRequest.getTitle().toLowerCase().contains("test")) {
            codeReview.addPositiveAspect("Includes test-related changes");
        }
        
        if (diffContent.contains("@Test") || diffContent.contains("test(")) {
            codeReview.addPositiveAspect("Contains unit tests");
        }
        
        if (pullRequest.getBody() != null && pullRequest.getBody().length() > 50) {
            codeReview.addPositiveAspect("Well-documented pull request with detailed description");
        }
    }
    
    private void determineReviewState(CodeReview codeReview) {
        long criticalIssues = codeReview.getCriticalIssuesCount();
        
        if (criticalIssues > 0) {
            codeReview.setState("CHANGES_REQUESTED");
        } else if (codeReview.getCommentCount() > 0) {
            codeReview.setState("COMMENTED");
        } else {
            codeReview.setState("APPROVED");
        }
    }
}