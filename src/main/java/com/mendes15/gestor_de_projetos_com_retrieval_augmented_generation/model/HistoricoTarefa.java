package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="historico_tarefa") @Getter @Setter
public class HistoricoTarefa {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Tarefa tarefa;
    private Long projetoId;
    private String acao;
    private Long atorId;
    private Long responsavelId;
    private String status;
    @org.hibernate.annotations.CreationTimestamp private java.time.Instant criadoEm;
}
