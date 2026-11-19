package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import java.util.List;

public interface EmbeddingProvider {
    List<double[]> embed(List<String> texts, boolean query);
    String fingerprint();
}
