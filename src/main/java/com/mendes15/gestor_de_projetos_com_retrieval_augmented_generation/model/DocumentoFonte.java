package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity @Table(name="rag_documentos") @Getter @Setter
public class DocumentoFonte {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Projeto projeto;
    @Column(nullable=false) private String nome;
    @Column(nullable=false) private String tipo;
    @Column(nullable=false) private String embeddingModel;
    private Long criadoPor;
    private int quantidadeTrechos;
    @CreationTimestamp private Instant criadoEm;
}
