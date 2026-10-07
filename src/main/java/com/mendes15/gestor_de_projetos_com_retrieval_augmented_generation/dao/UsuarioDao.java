package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dao;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioDao {

    @Autowired
    private UsuarioRepository repository;

    public List<Usuario> findAll() {
        return repository.findAll();
    }

    public Usuario findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    public Optional<Usuario> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Usuario save(Usuario usuario) {
        return repository.save(usuario);
    }

    public void delete(long id) {
        Usuario usuario = findById(id);
        repository.delete(usuario);
    }
}