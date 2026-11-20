package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Anexo;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.AnexoTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnexoTarefaRepository extends JpaRepository<AnexoTarefa, Long> {
    @Query("""
            select new com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto$Anexo(
                a.id, autor.id, a.nome, a.tamanho, a.criadoEm)
            from AnexoTarefa a left join a.autor autor
            where a.tarefa.id = :tarefaId order by a.id
            """)
    List<Anexo> listarMetadados(Long tarefaId);

    Optional<AnexoTarefa> findByIdAndTarefaId(Long anexoId, Long tarefaId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from AnexoTarefa a where a.id = :anexoId and a.tarefa.id = :tarefaId")
    int remover(Long tarefaId, Long anexoId);
}
