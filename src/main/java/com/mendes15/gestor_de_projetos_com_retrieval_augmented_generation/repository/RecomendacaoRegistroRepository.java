package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.RecomendacaoRegistro;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RecomendacaoRegistroRepository extends JpaRepository<RecomendacaoRegistro, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecomendacaoRegistro> findByIdAndTarefaIdAndProjetoId(Long id, Long tarefaId, Long projetoId);
    List<RecomendacaoRegistro> findByTarefaIdAndProjetoIdOrderByIdDesc(Long tarefaId, Long projetoId);
}
