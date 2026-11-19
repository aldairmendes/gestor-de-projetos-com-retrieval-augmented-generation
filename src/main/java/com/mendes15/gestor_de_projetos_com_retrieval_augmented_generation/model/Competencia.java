package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="competencias_usuario") @Getter @Setter
public class Competencia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Usuario usuario;
    @Column(nullable=false) private String tecnologia;
    @Column(nullable=false) private String tipoAtividade;
    private int nivel;
    @org.hibernate.annotations.CreationTimestamp private java.time.Instant criadoEm;
}
