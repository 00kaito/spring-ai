package com.example.coderepoai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CodeReview {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("pull_request_number")
    private int pullRequestNumber;
    
    @JsonProperty("repository_url")
    private String repositoryUrl;
    
    @JsonProperty("state")
    private String state; // "PENDING", "APPROVED", "CHANGES_REQUESTED", "COMMENTED"
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("commit_id")
    private String commitId;
    
    @JsonProperty("submitted_at")
    private LocalDateTime submittedAt;
    
    @JsonProperty("user")
    private PullRequest.User user;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("comments")
    private List<ReviewComment> comments = new ArrayList<>();
    
    // AI Review metadata
    private String aiModel; // e.g., "gpt-3.5-turbo", "fallback-analysis"
    private LocalDateTime reviewedAt;
    private String reviewSummary;
    private Integer overallScore; // 1-100, overall code quality score
    private List<String> detectedIssues = new ArrayList<>();
    private List<String> positiveAspects = new ArrayList<>();
    
    // Constructors
    public CodeReview() {}
    
    public CodeReview(int pullRequestNumber, String repositoryUrl) {
        this.pullRequestNumber = pullRequestNumber;
        this.repositoryUrl = repositoryUrl;
        this.state = "PENDING";
        this.reviewedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public int getPullRequestNumber() { return pullRequestNumber; }
    public void setPullRequestNumber(int pullRequestNumber) { this.pullRequestNumber = pullRequestNumber; }
    
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public PullRequest.User getUser() { return user; }
    public void setUser(PullRequest.User user) { this.user = user; }
    
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    
    public List<ReviewComment> getComments() { return comments; }
    public void setComments(List<ReviewComment> comments) { this.comments = comments; }
    
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public String getReviewSummary() { return reviewSummary; }
    public void setReviewSummary(String reviewSummary) { this.reviewSummary = reviewSummary; }
    
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    
    public List<String> getDetectedIssues() { return detectedIssues; }
    public void setDetectedIssues(List<String> detectedIssues) { this.detectedIssues = detectedIssues; }
    
    public List<String> getPositiveAspects() { return positiveAspects; }
    public void setPositiveAspects(List<String> positiveAspects) { this.positiveAspects = positiveAspects; }
    
    // Utility methods
    public void addComment(ReviewComment comment) {
        this.comments.add(comment);
    }
    
    public void addDetectedIssue(String issue) {
        this.detectedIssues.add(issue);
    }
    
    public void addPositiveAspect(String aspect) {
        this.positiveAspects.add(aspect);
    }
    
    public boolean hasComments() {
        return comments != null && !comments.isEmpty();
    }
    
    public int getCommentCount() {
        return comments != null ? comments.size() : 0;
    }
    
    public long getCriticalIssuesCount() {
        return comments.stream()
                .filter(comment -> comment.getSeverity() != null && comment.getSeverity() >= 4)
                .count();
    }
    
    @Override
    public String toString() {
        return "CodeReview{" +
                "pullRequestNumber=" + pullRequestNumber +
                ", state='" + state + '\'' +
                ", aiModel='" + aiModel + '\'' +
                ", overallScore=" + overallScore +
                ", commentCount=" + getCommentCount() +
                ", criticalIssues=" + getCriticalIssuesCount() +
                '}';
    }
}