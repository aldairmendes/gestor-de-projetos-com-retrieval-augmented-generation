package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Quadro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface QuadroRepository extends JpaRepository<Quadro, Long> {
    List<Quadro> findByProjetoIdOrderByIdAsc(Long projetoId);

    Optional<Quadro> findByIdAndProjetoId(Long id, Long projetoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Quadro> findForUpdateByIdAndProjetoId(Long id, Long projetoId);
}
