package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> save(
            @Valid @RequestBody UsuarioRequestDTO request) {

        Usuario user = modelMapper.map(request, Usuario.class);

        Usuario savedUser = usuarioService.save(user);

        UsuarioResponseDTO response =
                modelMapper.map(savedUser, UsuarioResponseDTO.class);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> findAll() {

        List<Usuario> users = usuarioService.findAll();

        List<UsuarioResponseDTO> response = users.stream()
                .map(usuario ->
                        modelMapper.map(usuario, UsuarioResponseDTO.class))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> findById(
            @PathVariable Long id) {

        Usuario usuario = usuarioService.findById(id);

        UsuarioResponseDTO response =
                modelMapper.map(usuario, UsuarioResponseDTO.class);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO request) {

        Usuario usuario = modelMapper.map(request, Usuario.class);

        Usuario updatedUser =
                usuarioService.update(id, usuario);

        UsuarioResponseDTO response =
                modelMapper.map(updatedUser, UsuarioResponseDTO.class);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        usuarioService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
