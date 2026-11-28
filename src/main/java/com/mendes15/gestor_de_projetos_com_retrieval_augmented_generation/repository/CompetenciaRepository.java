package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Competencia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {
    List<Competencia> findByUsuarioId(Long usuarioId);
    List<Competencia> findByUsuarioIdIn(Collection<Long> usuarioIds);
    void deleteByUsuarioId(Long usuarioId);
}
