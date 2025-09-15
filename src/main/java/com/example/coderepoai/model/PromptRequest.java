package com.example.coderepoai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PromptRequest {
    @JsonProperty("query")
    private String query;

    @JsonProperty("repository_url")
    private String repositoryUrl;

    @JsonProperty("max_results")
    private Integer maxResults = 5;

    public PromptRequest() {}

    public PromptRequest(String query, String repositoryUrl) {
        this.query = query;
        this.repositoryUrl = repositoryUrl;
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public String toString() {
        return "PromptRequest{" +
                "query='" + query + '\'' +
                ", repositoryUrl='" + repositoryUrl + '\'' +
                ", maxResults=" + maxResults +
                '}';
    }
}