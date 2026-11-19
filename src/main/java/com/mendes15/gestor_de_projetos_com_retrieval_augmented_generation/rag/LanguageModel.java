package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import java.util.List;

/** Retorna texto e IDs de fontes efetivamente usadas, sem executar ferramentas. */
public interface LanguageModel {
    record Generation(String resposta, List<String> fontes) {}
    Generation generate(String instruction, String context, String question);
}
