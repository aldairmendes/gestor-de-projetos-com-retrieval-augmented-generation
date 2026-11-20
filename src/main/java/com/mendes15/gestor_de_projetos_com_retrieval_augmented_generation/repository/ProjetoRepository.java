package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    List<Projeto> findDistinctByEquipe_Id(Long usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Projeto> findForUpdateById(Long id);
}
