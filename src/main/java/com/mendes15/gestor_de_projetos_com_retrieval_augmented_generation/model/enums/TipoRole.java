package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums;

public enum TipoRole {
    ROLE_MEMBRO, ROLE_GERENTE, ROLE_ADMINISTRADOR;

    public String nome() {
        return name().substring("ROLE_".length());
    }

    public static TipoRole fromNome(String nome) {
        return valueOf(nome.startsWith("ROLE_") ? nome : "ROLE_" + nome);
    }
}
