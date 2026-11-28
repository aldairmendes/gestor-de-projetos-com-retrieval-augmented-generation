package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity @Table(name="rag_trechos") @Getter @Setter
public class DocumentoTrecho {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private DocumentoFonte documento;
    private int ordem;
    private Integer pagina;
    @Column(nullable=false, columnDefinition="TEXT") private String texto;
    @CreationTimestamp private Instant criadoEm;
}
