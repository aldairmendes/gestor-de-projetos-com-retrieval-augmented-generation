package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRegisterRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.RoleNames;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.RoleRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Role;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;
    private final AccessPolicy access;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public List<UsuarioResponseDTO> findAll() {
        if (!RoleNames.hasRole(access.currentUser(), RoleNames.MANAGER)) access.requireAdmin();
        return usuarioRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public UsuarioResponseDTO findById(Long id) {
        access.requireSelfOrAdmin(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        return toDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO save(Usuario usuario) {
        access.requireAdmin();
        usuario.setId(null);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Role member = roleRepository.findByNome(RoleNames.MEMBER).orElseThrow();
        usuario.setRoles(new HashSet<>(Set.of(member)));
        Usuario savedUsuario = usuarioRepository.save(usuario);

        return toDTO(savedUsuario);
    }

    @Transactional
    public UsuarioResponseDTO update(Long id, UsuarioRegisterRequestDTO dto) {
        access.requireSelfOrAdmin(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        usuario.setUsername(dto.getUsername());
        usuario.setEmail(dto.getEmail());

        if (dto.getPassword() != null
                && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        Usuario updatedUsuario = usuarioRepository.save(usuario);

        return toDTO(updatedUsuario);
    }

    @Transactional
    public void delete(Long id) {
        access.requireAdmin();
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        usuarioRepository.delete(usuario);
    }

    @Transactional
    public UsuarioResponseDTO updateRoles(Long id, Set<TipoRole> roles) {
        access.requireAdmin();
        if (roles == null || roles.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um papel.");
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        Set<TipoRole> effectiveRoles = new HashSet<>(roles);
        effectiveRoles.add(TipoRole.ROLE_MEMBRO);
        Set<Role> entities = new HashSet<>();
        for (TipoRole role : effectiveRoles) {
            entities.add(roleRepository.findByNome(role.nome()).orElseThrow());
        }
        usuario.setRoles(entities);
        return toDTO(usuarioRepository.save(usuario));
    }

    private UsuarioResponseDTO toDTO(Usuario usuario) {
        UsuarioResponseDTO dto = modelMapper.map(usuario, UsuarioResponseDTO.class);
        dto.setUsername(usuario.getDisplayName());
        dto.setPapeis(usuario.getRoles().stream().map(role -> TipoRole.fromNome(role.getNome()))
                .collect(Collectors.toSet()));
        return dto;
    }
}
