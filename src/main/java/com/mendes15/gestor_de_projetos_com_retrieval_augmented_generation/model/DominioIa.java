package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="ia_dominio") @Getter @Setter
public class DominioIa {
    @Id private Long id = 1L;
    @Column(length=4000) private String regras = "Gestão de projetos e tarefas.";
    @Column(length=4000) private String vocabulario = "";
    @Version private Long versao;
}
