package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dao;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TarefaDao {

    @Autowired
    private TarefaRepository repository;

    public List<Tarefa> findAll() {
        return repository.findAll();
    }

    public Tarefa findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
    }

    public Tarefa save(Tarefa tarefa) {
        return repository.save(tarefa);
    }

    public void delete(long id) {
        Tarefa tarefa = findById(id);
        repository.delete(tarefa);
    }
}