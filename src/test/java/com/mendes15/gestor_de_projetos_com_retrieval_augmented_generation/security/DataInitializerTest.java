package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config.DataInitializer;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {
    final RoleRepository roles = mock(RoleRepository.class);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final DataInitializer initializer = new DataInitializer(roles, usuarios, encoder, mock(org.springframework.jdbc.core.JdbcTemplate.class));

    @BeforeEach void setup() {
        long id = 1;
        for (String nome : List.of("MEMBRO", "ADMINISTRADOR"))
            when(roles.findByNome(nome)).thenReturn(Optional.of(new Role(id++, nome)));
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "SenhaAdmin123!");
    }

    @Test void bootstrapAdminHasMinimumRoleAndHashedPassword() {
        initializer.run();
        var capture = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(capture.capture());
        Usuario admin = capture.getValue();
        assertEquals("admin@example.com", admin.getEmail());
        assertTrue(encoder.matches("SenhaAdmin123!", admin.getPassword()));
        assertEquals(Set.of("MEMBRO", "ADMINISTRADOR"), admin.getRoles().stream()
                .map(Role::getNome).collect(Collectors.toSet()));
    }

    @Test void existingAdminIsNotRecreatedOrOverwritten() {
        when(usuarios.existsByEmail("admin@example.com")).thenReturn(true);
        initializer.run();
        verify(usuarios, never()).save(any());
    }

    @Test void absentCredentialsDoNotCreateAdmin() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "");
        initializer.run();
        verify(usuarios, never()).save(any());
    }
}
