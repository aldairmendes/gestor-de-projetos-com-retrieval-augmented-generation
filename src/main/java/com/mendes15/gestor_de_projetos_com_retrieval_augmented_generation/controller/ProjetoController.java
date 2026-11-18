package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.ProjetoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> create(@RequestBody @Valid ProjetoRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.create(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<ProjetoResponseDTO>> findAll() {
        return ResponseEntity.ok(projetoService.listAccessible());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projetoService.findAccessibleById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid ProjetoRequestDTO requestDTO) {
        return ResponseEntity.ok(projetoService.update(id, requestDTO));
    }

    @PatchMapping("/{id}/arquivar")
    public ResponseEntity<ProjetoResponseDTO> archive(@PathVariable Long id) {
        return ResponseEntity.ok(projetoService.setArchived(id, true));
    }

    @PatchMapping("/{id}/desarquivar")
    public ResponseEntity<ProjetoResponseDTO> unarchive(@PathVariable Long id) {
        return ResponseEntity.ok(projetoService.setArchived(id, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projetoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
