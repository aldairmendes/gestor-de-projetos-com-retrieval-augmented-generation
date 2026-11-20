package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "anexos_tarefa", indexes = @Index(columnList = "tarefa_id"))
public class AnexoTarefa {
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

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false)
    private int tamanho;

    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(nullable = false, length = 5 * 1024 * 1024)
    private byte[] conteudo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public AnexoTarefa(Tarefa tarefa, Usuario autor, String nome, byte[] conteudo) {
        this.tarefa = tarefa;
        this.autor = autor;
        this.nome = nome;
        this.tamanho = conteudo.length;
        this.conteudo = conteudo;
    }
}
