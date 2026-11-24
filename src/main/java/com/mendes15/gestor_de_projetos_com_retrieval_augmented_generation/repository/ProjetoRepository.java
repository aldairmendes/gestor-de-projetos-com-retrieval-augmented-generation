package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    boolean existsByGerente_IdAndEquipe_Id(Long gerenteId, Long membroId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select projeto from Projeto projeto where projeto.id = :id")
    Optional<Projeto> findForUpdate(@Param("id") Long id);

    List<Projeto> findDistinctByEquipe_Id(Long usuarioId);
}
