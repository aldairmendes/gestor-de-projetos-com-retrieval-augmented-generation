package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.ProjetoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;

    public ProjetoService(ProjetoRepository projetoRepository) {
        this.projetoRepository = projetoRepository;
    }

    public Projeto save(Projeto projeto) {
        return projetoRepository.save(projeto);
    }

    public List<Projeto> findAll() {
        return projetoRepository.findAll();
    }

    public Projeto findById(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com o ID: " + id));
    }

    public Projeto update(Long id, ProjetoRequestDTO requestDTO) {
        Projeto projetoExistente = findById(id);
        projetoExistente.setNome(requestDTO.getNome());
        return projetoRepository.save(projetoExistente);
    }

    public void delete(Long id) {
        Projeto projeto = findById(id);
        projetoRepository.delete(projeto);
    }
}