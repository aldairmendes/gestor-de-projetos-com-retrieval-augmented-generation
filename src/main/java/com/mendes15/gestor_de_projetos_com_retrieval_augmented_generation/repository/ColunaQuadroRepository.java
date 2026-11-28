package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.ColunaQuadro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColunaQuadroRepository extends JpaRepository<ColunaQuadro, Long> {
    List<ColunaQuadro> findByQuadroIdOrderByPosicaoAsc(Long quadroId);
}
