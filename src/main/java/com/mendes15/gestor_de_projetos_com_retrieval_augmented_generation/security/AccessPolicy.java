package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AccessPolicy {
    private final UsuarioRepository usuarios;

    public Usuario currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        // Consulta os papéis atuais: alterações administrativas não dependem de renovar o JWT.
        return usuarios.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não encontrado."));
    }

    public boolean canView(Projeto projeto, Usuario user) {
        return projeto != null && user.getId() != null && projeto.getEquipe() != null
                && projeto.getEquipe().stream().anyMatch(m -> Objects.equals(m.getId(), user.getId()));
    }

    public boolean canManage(Projeto projeto, Usuario user) {
        return RoleNames.hasRole(user, RoleNames.MANAGER) && canView(projeto, user);
    }

    public void requireView(Projeto projeto) {
        if (!canView(projeto, currentUser())) throw new AccessDeniedException("Você não tem acesso a este projeto.");
    }

    public void requireManage(Projeto projeto) {
        if (!canManage(projeto, currentUser())) throw new AccessDeniedException("Você não pode gerenciar este projeto.");
    }

    public void requireManager() {
        if (!RoleNames.hasRole(currentUser(), RoleNames.MANAGER))
            throw new AccessDeniedException("Somente gerentes podem executar esta operação.");
    }

    public void requireAdmin() {
        if (!RoleNames.hasRole(currentUser(), RoleNames.ADMIN))
            throw new AccessDeniedException("Somente administradores podem executar esta operação.");
    }

    public void requireSelfOrAdmin(Long userId) {
        Usuario user = currentUser();
        if (!Objects.equals(user.getId(), userId) && !RoleNames.hasRole(user, RoleNames.ADMIN))
            throw new AccessDeniedException("Você só pode alterar seu próprio perfil.");
    }
}
