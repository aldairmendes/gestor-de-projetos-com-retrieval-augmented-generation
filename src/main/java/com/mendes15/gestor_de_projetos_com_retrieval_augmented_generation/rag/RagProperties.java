package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private boolean enabled = false;
    @lombok.ToString.Exclude
    private String apiKey = "";
    private String chatModel = "gemini-3.5-flash-lite";
    private String embeddingModel = "gemini-embedding-001";
    private int dimensions = 768;
    private int timeoutSeconds = 30;
    private int topK = 6;
    private double minimumSimilarity = 0.3;
    private int chunkSize = 1200;
    private int overlap = 150;
    private int maxTextChars = 100000;
}
