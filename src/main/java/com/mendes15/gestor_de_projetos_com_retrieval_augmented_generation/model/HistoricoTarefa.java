package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity @Table(name="historico_tarefa") @Getter @Setter
public class HistoricoTarefa {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Tarefa tarefa;
    private Long projetoId;
    private String acao;
    private Long atorId;
    private Long responsavelId;
    private String status;
    @CreationTimestamp
    private Instant criadoEm;
}
