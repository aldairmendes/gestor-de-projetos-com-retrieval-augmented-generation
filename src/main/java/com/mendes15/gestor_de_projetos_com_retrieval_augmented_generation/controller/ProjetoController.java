package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.ProjetoService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;
    private final ModelMapper modelMapper;

    public ProjetoController(ProjetoService projetoService, ModelMapper modelMapper) {
        this.projetoService = projetoService;
        this.modelMapper = modelMapper;
    }

    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> create(@RequestBody @Valid ProjetoRequestDTO requestDTO) {
        Projeto projeto = modelMapper.map(requestDTO, Projeto.class);
        Projeto projetoSalvo = projetoService.save(projeto);
        ProjetoResponseDTO response = modelMapper.map(projetoSalvo, ProjetoResponseDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjetoResponseDTO>> findAll() {
        List<Projeto> projetos = projetoService.findAll();
        List<ProjetoResponseDTO> response = projetos.stream()
                .map(projeto -> modelMapper.map(projeto, ProjetoResponseDTO.class))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> findById(@PathVariable Long id) {
        Projeto projeto = projetoService.findById(id);
        ProjetoResponseDTO response = modelMapper.map(projeto, ProjetoResponseDTO.class);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid ProjetoRequestDTO requestDTO) {
        Projeto projetoAtualizado = projetoService.update(id, requestDTO);
        ProjetoResponseDTO response = modelMapper.map(projetoAtualizado, ProjetoResponseDTO.class);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projetoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}