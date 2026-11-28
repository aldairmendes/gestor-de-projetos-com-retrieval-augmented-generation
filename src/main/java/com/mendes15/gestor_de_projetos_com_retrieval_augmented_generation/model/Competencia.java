package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity @Table(name="competencias_usuario") @Getter @Setter
public class Competencia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(nullable=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Usuario usuario;
    @Column(nullable=false) private String tecnologia;
    @Column(nullable=false) private String tipoAtividade;
    private int nivel;
    @CreationTimestamp
    private Instant criadoEm;
}
