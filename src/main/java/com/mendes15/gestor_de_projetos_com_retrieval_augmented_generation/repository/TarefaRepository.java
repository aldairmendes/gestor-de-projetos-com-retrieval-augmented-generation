package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findDistinctByProjeto_Equipe_Id(Long usuarioId);

    List<Tarefa> findByProjetoId(Long projetoId);
    @Query("select t.projeto.id from Tarefa t where t.id = :id")
    Optional<Long> buscarProjetoId(Long id);

    Optional<Tarefa> findByIdAndProjetoId(Long id, Long projetoId);
}
