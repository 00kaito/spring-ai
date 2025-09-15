package com.example.coderepoai.model;

import java.util.Map;

public class CodeChunk {
    private String content;
    private String filePath;
    private Map<String, Object> metadata;
    private String repositoryUrl;
    private Integer chunkIndex;

    public CodeChunk() {}

    public CodeChunk(String content, String filePath, String repositoryUrl, Integer chunkIndex) {
        this.content = content;
        this.filePath = filePath;
        this.repositoryUrl = repositoryUrl;
        this.chunkIndex = chunkIndex;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    @Override
    public String toString() {
        return "CodeChunk{" +
                "filePath='" + filePath + '\'' +
                ", repositoryUrl='" + repositoryUrl + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }
}