package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.DependenciaTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DependenciaTarefaRepository extends JpaRepository<DependenciaTarefa, Long> {
    interface Vinculo {
        Long getTarefaId();
        Long getPrerequisitoId();
    }

    @Query("""
            select d.tarefa.id as tarefaId, d.prerequisito.id as prerequisitoId from DependenciaTarefa d
            where d.tarefa.projeto.id = :projetoId
            """)
    List<Vinculo> listarVinculos(Long projetoId);

    @Query("""
            select d from DependenciaTarefa d join fetch d.prerequisito p
            where d.tarefa.id = :tarefaId order by p.id
            """)
    List<DependenciaTarefa> listarPrerequisitos(Long tarefaId);

    Optional<DependenciaTarefa> findByTarefaIdAndPrerequisitoId(Long tarefaId, Long prerequisitoId);

    @Query("select count(d) > 0 from DependenciaTarefa d where d.tarefa.id = :id or d.prerequisito.id = :id")
    boolean possuiVinculos(Long id);
}
