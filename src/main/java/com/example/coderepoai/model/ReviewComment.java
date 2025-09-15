package com.example.coderepoai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ReviewComment {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("position")
    private Integer position;
    
    @JsonProperty("original_position")
    private Integer originalPosition;
    
    @JsonProperty("line")
    private Integer line;
    
    @JsonProperty("original_line")
    private Integer originalLine;
    
    @JsonProperty("side")
    private String side; // "LEFT" or "RIGHT"
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("user")
    private PullRequest.User user;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("pull_request_url")
    private String pullRequestUrl;
    
    @JsonProperty("diff_hunk")
    private String diffHunk;
    
    @JsonProperty("in_reply_to_id")
    private Long inReplyToId;
    
    // For AI-generated suggestions
    private String suggestionType; // "IMPROVEMENT", "BUG", "STYLE", "PERFORMANCE", "SECURITY"
    private Integer severity; // 1-5, where 5 is critical
    
    // Constructors
    public ReviewComment() {}
    
    public ReviewComment(String path, Integer line, String body) {
        this.path = path;
        this.line = line;
        this.body = body;
    }
    
    public ReviewComment(String path, Integer line, String body, String suggestionType, Integer severity) {
        this.path = path;
        this.line = line;
        this.body = body;
        this.suggestionType = suggestionType;
        this.severity = severity;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    
    public Integer getOriginalPosition() { return originalPosition; }
    public void setOriginalPosition(Integer originalPosition) { this.originalPosition = originalPosition; }
    
    public Integer getLine() { return line; }
    public void setLine(Integer line) { this.line = line; }
    
    public Integer getOriginalLine() { return originalLine; }
    public void setOriginalLine(Integer originalLine) { this.originalLine = originalLine; }
    
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public PullRequest.User getUser() { return user; }
    public void setUser(PullRequest.User user) { this.user = user; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    
    public String getPullRequestUrl() { return pullRequestUrl; }
    public void setPullRequestUrl(String pullRequestUrl) { this.pullRequestUrl = pullRequestUrl; }
    
    public String getDiffHunk() { return diffHunk; }
    public void setDiffHunk(String diffHunk) { this.diffHunk = diffHunk; }
    
    public Long getInReplyToId() { return inReplyToId; }
    public void setInReplyToId(Long inReplyToId) { this.inReplyToId = inReplyToId; }
    
    public String getSuggestionType() { return suggestionType; }
    public void setSuggestionType(String suggestionType) { this.suggestionType = suggestionType; }
    
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
    
    @Override
    public String toString() {
        return "ReviewComment{" +
                "path='" + path + '\'' +
                ", line=" + line +
                ", suggestionType='" + suggestionType + '\'' +
                ", severity=" + severity +
                ", body='" + (body != null ? body.substring(0, Math.min(50, body.length())) + "..." : null) + '\'' +
                '}';
    }
}