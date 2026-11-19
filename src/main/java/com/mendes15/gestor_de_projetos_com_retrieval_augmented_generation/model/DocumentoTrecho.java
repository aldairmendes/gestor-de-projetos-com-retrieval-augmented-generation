package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="rag_trechos") @Getter @Setter
public class DocumentoTrecho {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private DocumentoFonte documento;
    private int ordem;
    private Integer pagina;
    @Column(nullable=false, columnDefinition="TEXT") private String texto;
    @org.hibernate.annotations.CreationTimestamp private java.time.Instant criadoEm;
}
