package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="ia_recomendacoes") @Getter @Setter
public class RecomendacaoRegistro {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Tarefa tarefa;
    private Long projetoId;
    private Long solicitanteId;
    @Column(nullable=false, columnDefinition="TEXT") private String rankingJson;
    @Column(nullable=false, columnDefinition="TEXT") private String contextoJson;
    private String decisao;
    private Long escolhidoId;
    private Long decididoPor;
    private java.time.Instant decididoEm;
    @Column(length=2000) private String observacao;
    @Version private Long versao;
    @org.hibernate.annotations.CreationTimestamp private java.time.Instant criadoEm;
}
