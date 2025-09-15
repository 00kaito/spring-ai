# Overview

This is a complete Java Spring Boot application that fetches and analyzes GitHub repositories and enables AI-powered conversations with code. The application follows a layered architecture with proper separation of concerns and includes comprehensive GitHub integration, Spring AI for intelligent code analysis, and both synchronous and asynchronous processing capabilities. The application serves as a full-featured code analysis platform with intelligent chat functionality powered by OpenAI's language models.

# User Preferences

Preferred communication style: Simple, everyday language.

# System Architecture

## Core Framework
- **Spring Boot 3.2.10**: Main application framework with auto-configuration and embedded Tomcat server
- **Spring AI 1.0.0-M3**: AI integration framework for intelligent code analysis with OpenAI ChatClient
- **Maven**: Build and dependency management system
- **Java 17**: Primary programming language with modern language features

## Application Layers
The application follows a clean layered architecture:

- **Controller Layer**: REST API endpoints (`/api/refresh`, `/api/chat`) for client communication
- **Service Layer**: Business logic split into specialized services:
  - **Ingestion Services**: Repository fetching, file parsing, and code chunking
  - **Chat Services**: AI-powered conversation and code retrieval
- **Repository Layer**: Vector storage for code chunks with efficient similarity search
- **Model Layer**: Domain objects for code chunks and API requests
- **Configuration Layer**: Spring configuration for AI models and application settings
- **Exception Layer**: Custom exceptions for repository and parsing errors

## Key Features
- **GitHub Integration**: Fetches repository files with authentication support
- **AI-Powered Chat**: Uses OpenAI GPT models for intelligent code analysis
- **Asynchronous Processing**: Background repository processing with thread pools
- **Fallback Mechanisms**: Graceful degradation when AI services are unavailable
- **Vector Storage**: In-memory code chunk storage with similarity search
- **File Processing**: Intelligent filtering and parsing of common file types

# External Dependencies

## Core Dependencies
- **Spring Boot Starter Web**: REST API and MVC capabilities
- **Spring Boot Starter WebFlux**: Reactive web capabilities
- **Spring Boot Starter Data JPA**: Data persistence layer
- **Spring AI OpenAI Starter**: OpenAI integration for language models
- **Spring AI Core**: Core AI functionality and abstractions

## GitHub Integration
- **GitHub API**: Official GitHub Java client for repository access
- **Apache Commons Text**: Text processing and utilities

## Data Storage
- **H2 Database**: In-memory database for development
- **Jackson**: JSON processing and serialization

## Testing Framework
- **Spring Boot Starter Test**: Comprehensive testing with JUnit and Mockito
- **AssertJ**: Fluent test assertions
- **JSON Assert**: JSON testing utilities

## Configuration
All dependencies are managed through Maven with Spring Boot's dependency management ensuring version compatibility. The application includes milestone dependencies from Spring AI which require the Spring Milestones repository.

# API Usage

## Repository Ingestion
- **POST /api/refresh**: Synchronous repository processing
- **POST /api/refresh/async**: Asynchronous repository processing (recommended for large repos)
- **GET /api/refresh/status**: Check service status

## AI Chat
- **GET /api/chat/simple?query={query}**: Simple chat with auto-repository detection
- **POST /api/chat**: Advanced chat with repository filtering and result limits

## Configuration
To enable full AI functionality, set the environment variable:
- `SPRING_AI_OPENAI_API_KEY`: Your OpenAI API key

Without an API key, the application gracefully falls back to simple code snippet retrieval.