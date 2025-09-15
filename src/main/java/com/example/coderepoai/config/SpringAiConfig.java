package com.example.coderepoai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key:#{null}}")
    private String openaiApiKey;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public OpenAiChatModel openAiChatModel() {
        OpenAiApi openAiApi = new OpenAiApi(openaiApiKey);
        return new OpenAiChatModel(openAiApi);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }
}