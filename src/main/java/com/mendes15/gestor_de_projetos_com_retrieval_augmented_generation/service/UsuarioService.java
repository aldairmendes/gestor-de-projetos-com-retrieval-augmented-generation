package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;

    public List<UsuarioResponseDTO> findAll() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuario ->
                        modelMapper.map(usuario, UsuarioResponseDTO.class))
                .toList();
    }

    public UsuarioResponseDTO findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        return modelMapper.map(usuario, UsuarioResponseDTO.class);
    }

    public UsuarioResponseDTO save(Usuario usuario) {
        Usuario savedUsuario = usuarioRepository.save(usuario);

        return modelMapper.map(
                savedUsuario,
                UsuarioResponseDTO.class
        );
    }

    public UsuarioResponseDTO update(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        usuario.setUsername(dto.getUsername());
        usuario.setEmail(dto.getEmail());

        if (dto.getPassword() != null
                && !dto.getPassword().isBlank()) {
            usuario.setPassword(dto.getPassword());
        }

        Usuario updatedUsuario = usuarioRepository.save(usuario);

        return modelMapper.map(
                updatedUsuario,
                UsuarioResponseDTO.class
        );
    }

    public void delete(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        usuarioRepository.delete(usuario);
    }

}
