package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final ModelMapper modelMapper;

    public List<TarefaResponseDTO> findAll() {
        return tarefaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public TarefaResponseDTO findById(Long id) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found."));

        return toResponseDTO(tarefa);
    }

    public Tarefa save(Tarefa tarefa) {
        Tarefa savedTarefa = tarefaRepository.save(tarefa);

        return savedTarefa;
    }

    public TarefaResponseDTO update(Long id, Tarefa tarefaDetails) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found."));

        tarefa.setTitulo(tarefaDetails.getTitulo());
        tarefa.setConteudo(tarefaDetails.getConteudo());
        tarefa.setStatus(tarefaDetails.getStatus());
        tarefa.setPrioridade(tarefaDetails.getPrioridade());
        tarefa.setProjeto(tarefaDetails.getProjeto());

        Tarefa updatedTarefa = tarefaRepository.save(tarefa);

        return toResponseDTO(updatedTarefa);
    }

    public void delete(Long id) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found."));

        tarefaRepository.delete(tarefa);
    }

    private TarefaResponseDTO toResponseDTO(Tarefa tarefa) {
        TarefaResponseDTO dto =
                modelMapper.map(tarefa, TarefaResponseDTO.class);

        if (tarefa.getProjeto() != null) {
            dto.setProjetoId(tarefa.getProjeto().getId());
        }

        return dto;
    }
}
