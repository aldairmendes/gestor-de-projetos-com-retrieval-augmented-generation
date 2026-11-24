package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comentarios_tarefa", indexes = @Index(columnList = "tarefa_id"))
public class ComentarioTarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarefa_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tarefa tarefa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Usuario autor;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public ComentarioTarefa(Tarefa tarefa, Usuario autor, String texto) {
        this.tarefa = tarefa;
        this.autor = autor;
        this.texto = texto;
    }
}
