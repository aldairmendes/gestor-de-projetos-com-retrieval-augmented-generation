package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultRepository<E> extends JpaRepository<Long, E> {
}
