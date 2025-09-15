package com.example.coderepoai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key:#{null}}")
    private String openaiApiKey;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            // Return a mock implementation for development
            return null;
        }
        
        OpenAiApi openAiApi = new OpenAiApi(openaiApiKey);
        return new OpenAiChatModel(openAiApi);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        if (openAiChatModel == null) {
            // Return null when no API key is provided
            return null;
        }
        return ChatClient.create(openAiChatModel);
    }
}