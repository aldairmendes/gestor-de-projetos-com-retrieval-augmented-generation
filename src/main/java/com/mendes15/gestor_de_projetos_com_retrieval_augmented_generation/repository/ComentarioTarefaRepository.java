package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.ComentarioTarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioTarefaRepository extends JpaRepository<ComentarioTarefa, Long> {
    List<ComentarioTarefa> findByTarefaIdOrderByIdAsc(Long tarefaId);
}
