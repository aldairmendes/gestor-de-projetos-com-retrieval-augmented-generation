package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.HistoricoTarefa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoTarefaRepository extends JpaRepository<HistoricoTarefa, Long> {
    java.util.List<HistoricoTarefa> findByTarefaIdAndProjetoIdOrderByIdAsc(Long tarefaId, Long projetoId);

    java.util.List<HistoricoTarefa> findByProjetoIdAndStatusAndResponsavelIdIsNotNull(Long projetoId, String status);
}
