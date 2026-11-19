package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.PrioridadeTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tarefas")
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTarefa status = StatusTarefa.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeTarefa prioridade = PrioridadeTarefa.MEDIA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Column(columnDefinition="TEXT")
    private String requisitos;

    @ElementCollection
    @CollectionTable(name="tarefa_tecnologias", joinColumns=@JoinColumn(name="tarefa_id"))
    @Column(name="tecnologia")
    private java.util.Set<String> tecnologias = new java.util.LinkedHashSet<>();

    private Integer complexidade;
    private java.time.LocalDate prazo;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="responsavel_id")
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private Usuario responsavel;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;
}