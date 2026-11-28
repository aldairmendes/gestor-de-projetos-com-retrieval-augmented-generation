package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessPolicyTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AccessPolicy policy = new AccessPolicy(usuarios);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    static Usuario user(TipoRole role) {
        return Usuario.builder().id(1L).email("user@example.com").username("User")
                .roles(Set.of(new Role(1L, role.nome()))).build();
    }

    @ParameterizedTest @EnumSource(TipoRole.class)
    void outsidersCannotReadOrManageEvenWhenAdmin(TipoRole role) {
        Usuario user = user(role);
        Projeto projeto = new Projeto();
        projeto.setCriador(user); // A autoria não substitui participação na equipe.
        assertFalse(policy.canView(projeto, user));
        assertFalse(policy.canManage(projeto, user));
    }

    @ParameterizedTest @EnumSource(TipoRole.class)
    void teamMembersReadButOnlyManagerOrganizes(TipoRole role) {
        Usuario user = user(role);
        Projeto projeto = new Projeto();
        projeto.setEquipe(List.of(user));
        assertTrue(policy.canView(projeto, user));
        assertFalse(policy.canManage(projeto, user));
        projeto.setGerente(user);
        assertTrue(policy.canManage(projeto, user));
    }

    @ParameterizedTest @EnumSource(TipoRole.class)
    void authorityHasExactlyOnePrefix(TipoRole role) {
        Usuario user = user(role);
        assertEquals(role.name(), user.getAuthorities().iterator().next().getAuthority());
        user.setRoles(Set.of(new Role(1L, role.name())));
        assertEquals(role.name(), user.getAuthorities().iterator().next().getAuthority());
    }

    @Test void rolesAreReadFromDatabaseInsteadOfStalePrincipal() {
        Usuario stale = user(TipoRole.ROLE_ADMINISTRADOR);
        authenticate(stale);
        when(usuarios.findByEmail(stale.getEmail())).thenReturn(Optional.of(user(TipoRole.ROLE_MEMBRO)));
        assertThrows(AccessDeniedException.class, policy::requireAdmin);
    }

    @Test void cannotEditAnotherProfile() {
        Usuario user = user(TipoRole.ROLE_MEMBRO);
        authenticate(user);
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertDoesNotThrow(() -> policy.requireSelfOrAdmin(1L));
        assertThrows(AccessDeniedException.class, () -> policy.requireSelfOrAdmin(2L));
    }

    @Test void anonymousCannotResolveUser() {
        assertThrows(AccessDeniedException.class, policy::currentUser);
        verifyNoInteractions(usuarios);
    }

    static void authenticate(Usuario user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
