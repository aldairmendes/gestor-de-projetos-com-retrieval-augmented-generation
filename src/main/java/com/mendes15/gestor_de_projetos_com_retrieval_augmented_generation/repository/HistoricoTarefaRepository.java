package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.HistoricoTarefa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoTarefaRepository extends JpaRepository<HistoricoTarefa, Long> {
    List<HistoricoTarefa> findByTarefaIdAndProjetoIdOrderByIdAsc(Long tarefaId, Long projetoId);

    List<HistoricoTarefa> findByProjetoIdAndStatusAndResponsavelIdIsNotNull(Long projetoId, String status);
}
