package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity @Table(name="ia_recomendacoes") @Getter @Setter
public class RecomendacaoRegistro {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Tarefa tarefa;
    private Long projetoId;
    private Long solicitanteId;
    @Column(nullable=false, columnDefinition="TEXT") private String rankingJson;
    @Column(nullable=false, columnDefinition="TEXT") private String contextoJson;
    private String decisao;
    private Long escolhidoId;
    private Long decididoPor;
    private Instant decididoEm;
    @Column(length=2000) private String observacao;
    @Version private Long versao;
    @CreationTimestamp private Instant criadoEm;
}
