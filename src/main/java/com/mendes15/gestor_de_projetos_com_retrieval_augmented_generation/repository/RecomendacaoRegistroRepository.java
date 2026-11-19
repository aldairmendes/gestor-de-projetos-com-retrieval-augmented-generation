package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.RecomendacaoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecomendacaoRegistroRepository extends JpaRepository<RecomendacaoRegistro, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<RecomendacaoRegistro> findByIdAndTarefaIdAndProjetoId(Long id, Long tarefaId, Long projetoId);
    java.util.List<RecomendacaoRegistro> findByTarefaIdAndProjetoIdOrderByIdDesc(Long tarefaId, Long projetoId);
}
