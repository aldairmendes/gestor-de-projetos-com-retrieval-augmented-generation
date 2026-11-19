package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import java.util.List;

public interface VectorIndex {
    record Match(Long trechoId, double score) {}
    void put(Long trechoId, double[] vector, String model);
    List<Match> search(Long projetoId, Long usuarioId, double[] query, String model, int limit, double minimumScore);
}
