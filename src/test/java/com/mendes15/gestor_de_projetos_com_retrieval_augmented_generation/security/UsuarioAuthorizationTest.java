package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.UsuarioService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioAuthorizationTest {
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final RoleRepository roles = mock(RoleRepository.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final UsuarioService service = new UsuarioService(usuarios, new ModelMapper(),
            new AccessPolicy(usuarios), encoder, roles);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    void login(TipoRole role) {
        Usuario user = AccessPolicyTest.user(role);
        AccessPolicyTest.authenticate(user);
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @ParameterizedTest @EnumSource(value = TipoRole.class, names = {"ROLE_MEMBRO", "ROLE_GERENTE"})
    void nonAdminCannotAssignGlobalRoles(TipoRole role) {
        login(role);
        assertThrows(AccessDeniedException.class,
                () -> service.updateRoles(1L, Set.of(TipoRole.ROLE_ADMINISTRADOR)));
        verify(usuarios, never()).save(any());
    }

    @Test void adminAssignmentRetainsMinimumMemberRole() {
        login(TipoRole.ROLE_ADMINISTRADOR);
        Usuario target = AccessPolicyTest.user(TipoRole.ROLE_MEMBRO); target.setId(2L);
        when(usuarios.findById(2L)).thenReturn(Optional.of(target));
        when(roles.findByNome("MEMBRO")).thenReturn(Optional.of(new Role(1L, "MEMBRO")));
        when(roles.findByNome("GERENTE")).thenReturn(Optional.of(new Role(2L, "GERENTE")));
        when(usuarios.save(target)).thenReturn(target);
        assertEquals(Set.of(TipoRole.ROLE_MEMBRO, TipoRole.ROLE_GERENTE),
                service.updateRoles(2L, Set.of(TipoRole.ROLE_GERENTE)).getPapeis());
    }

    @Test void adminCreationHashesPasswordAndIgnoresInjectedRoles() {
        login(TipoRole.ROLE_ADMINISTRADOR);
        Usuario target = AccessPolicyTest.user(TipoRole.ROLE_ADMINISTRADOR);
        target.setPassword("senha-segura");
        when(roles.findByNome("MEMBRO")).thenReturn(Optional.of(new Role(1L, "MEMBRO")));
        when(usuarios.save(target)).thenReturn(target);
        assertEquals(Set.of(TipoRole.ROLE_MEMBRO), service.save(target).getPapeis());
        assertTrue(encoder.matches("senha-segura", target.getPassword()));
        assertNull(target.getId());
    }
}
