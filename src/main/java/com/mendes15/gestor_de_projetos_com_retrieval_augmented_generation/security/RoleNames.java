package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;

public final class RoleNames {
    public static final String MEMBER = TipoRole.ROLE_MEMBRO.nome();
    public static final String MANAGER = TipoRole.ROLE_GERENTE.nome();
    public static final String ADMIN = TipoRole.ROLE_ADMINISTRADOR.nome();

    private RoleNames() {}

    public static boolean hasRole(Usuario user, String roleName) {
        TipoRole expected = TipoRole.fromNome(roleName);
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role ->
                expected.nome().equals(role.getNome()) || expected.name().equals(role.getNome()));
    }
}
