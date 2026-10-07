package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.TarefaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @Valid @RequestBody TarefaRequestDTO request) {

        Tarefa tarefa = modelMapper.map(request, Tarefa.class);

        Tarefa tarefaSalva = tarefaService.save(tarefa);

        TarefaResponseDTO response =
                modelMapper.map(tarefaSalva, TarefaResponseDTO.class);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listar() {

        List<TarefaResponseDTO> tarefas = tarefaService.findAll();

        List<TarefaResponseDTO> response = tarefas.stream()
                .map(tarefa ->
                        modelMapper.map(tarefa, TarefaResponseDTO.class))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> findById(
            @PathVariable Long id) {

        TarefaResponseDTO response = tarefaService.findById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/projeto/{projetoId}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorProjeto(
            @PathVariable Long projetoId) {

        List<Tarefa> tarefas =
                tarefaService.listByProjeto(projetoId);

        List<TarefaResponseDTO> response = tarefas.stream()
                .map(tarefa ->
                        modelMapper.map(tarefa, TarefaResponseDTO.class))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody TarefaRequestDTO request) {

        Tarefa tarefa = modelMapper.map(request, Tarefa.class);

        Tarefa tarefaAtualizada =
                tarefaService.atualizar(id, tarefa);

        TarefaResponseDTO response =
                modelMapper.map(tarefaAtualizada, TarefaResponseDTO.class);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        tarefaService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}
