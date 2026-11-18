package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;

public final class RoleNames {
    public static final String MEMBER = "MEMBRO";
    public static final String MANAGER = "GERENTE";
    public static final String ADMIN = "ADMINISTRADOR";

    private RoleNames() {
    }

    public static boolean hasRole(Usuario user, String roleName) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> roleName.equals(role.getNome()));
    }
}
