package com.example.coderepoai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class PullRequest {
    @JsonProperty("number")
    private int number;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("base")
    private Branch base;
    
    @JsonProperty("head")
    private Branch head;
    
    @JsonProperty("user")
    private User user;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("diff_url")
    private String diffUrl;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("changed_files")
    private int changedFiles;
    
    @JsonProperty("additions")
    private int additions;
    
    @JsonProperty("deletions")
    private int deletions;
    
    // Constructors
    public PullRequest() {}
    
    public PullRequest(int number, String title, String state) {
        this.number = number;
        this.title = title;
        this.state = state;
    }
    
    // Getters and Setters
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public Branch getBase() { return base; }
    public void setBase(Branch base) { this.base = base; }
    
    public Branch getHead() { return head; }
    public void setHead(Branch head) { this.head = head; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    
    public String getDiffUrl() { return diffUrl; }
    public void setDiffUrl(String diffUrl) { this.diffUrl = diffUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public int getChangedFiles() { return changedFiles; }
    public void setChangedFiles(int changedFiles) { this.changedFiles = changedFiles; }
    
    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }
    
    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }
    
    // Nested classes for GitHub API response structure
    public static class Branch {
        @JsonProperty("ref")
        private String ref;
        
        @JsonProperty("sha")
        private String sha;
        
        @JsonProperty("repo")
        private Repository repo;
        
        // Getters and Setters
        public String getRef() { return ref; }
        public void setRef(String ref) { this.ref = ref; }
        
        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
        
        public Repository getRepo() { return repo; }
        public void setRepo(Repository repo) { this.repo = repo; }
    }
    
    public static class Repository {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("full_name")
        private String fullName;
        
        @JsonProperty("owner")
        private User owner;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public User getOwner() { return owner; }
        public void setOwner(User owner) { this.owner = owner; }
    }
    
    public static class User {
        @JsonProperty("login")
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
        
        // Getters and Setters
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    @Override
    public String toString() {
        return "PullRequest{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", state='" + state + '\'' +
                ", user=" + (user != null ? user.getLogin() : null) +
                ", changedFiles=" + changedFiles +
                '}';
    }
}