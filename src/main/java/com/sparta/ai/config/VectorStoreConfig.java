package com.sparta.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public ChromaVectorStore vectorStore(EmbeddingModel embeddingModel) {
        ChromaApi chromaApi = new ChromaApi("http://localhost:8000");
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("devvault-docs")
                .initializeSchema(true)
                .build();
    }
}
