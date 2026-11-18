package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioLoginRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRegisterRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Role;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.RoleRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.JwtService;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.RoleNames;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String registrar(UsuarioRegisterRequestDTO request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }
        Role memberRole = roleRepository.findByNome(RoleNames.MEMBER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleNames.MEMBER)));

        var usuario = Usuario.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(memberRole)))
                .build();

        repository.save(usuario);
        return generateToken(usuario);
    }

    public String autenticar(UsuarioLoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var usuario = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return generateToken(usuario);
    }

    private String generateToken(Usuario user) {
        var roleNames = user.getRoles().stream().map(Role::getNome).sorted().toList();
        return jwtService.generateToken(Map.of("roles", roleNames), user);
    }
}
