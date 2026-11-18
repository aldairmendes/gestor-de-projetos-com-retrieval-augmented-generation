package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Role;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.RoleRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.RoleNames;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        for (String roleName : List.of(RoleNames.MEMBER, RoleNames.MANAGER, RoleNames.ADMIN)) {
            if (roleRepository.findByNome(roleName).isEmpty()) {
                roleRepository.save(new Role(null, roleName));
            }
        }

        if (!adminEmail.isBlank() && !adminPassword.isBlank() && !usuarioRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByNome(RoleNames.ADMIN).orElseThrow();
            usuarioRepository.save(Usuario.builder()
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(new HashSet<>(Set.of(adminRole, roleRepository.findByNome(RoleNames.MEMBER).orElseThrow())))
                    .build());
        }
    }
}
