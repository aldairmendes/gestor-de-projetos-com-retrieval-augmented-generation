package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dao;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.ProjetoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProjetoDao {

    @Autowired
    private ProjetoRepository repository;

    public List<Projeto> findAll() {
        return repository.findAll();
    }

    public Projeto findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    public Projeto save(Projeto projeto) {
        return repository.save(projeto);
    }

    public void delete(long id) {
        Projeto projeto = findById(id);
        repository.delete(projeto);
    }
}