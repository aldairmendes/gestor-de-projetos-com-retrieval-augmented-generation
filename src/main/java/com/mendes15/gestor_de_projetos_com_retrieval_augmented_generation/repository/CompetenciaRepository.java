package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {
    java.util.List<Competencia> findByUsuarioId(Long usuarioId);
    java.util.List<Competencia> findByUsuarioIdIn(java.util.Collection<Long> usuarioIds);
    void deleteByUsuarioId(Long usuarioId);
}
