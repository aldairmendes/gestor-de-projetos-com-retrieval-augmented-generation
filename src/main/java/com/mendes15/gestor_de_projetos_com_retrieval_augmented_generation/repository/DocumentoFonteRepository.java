package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.DocumentoFonte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoFonteRepository extends JpaRepository<DocumentoFonte, Long> {
    java.util.List<DocumentoFonte> findByProjetoIdOrderByIdDesc(Long projetoId);
}
