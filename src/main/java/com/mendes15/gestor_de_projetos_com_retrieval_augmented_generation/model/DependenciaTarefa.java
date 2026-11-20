package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "dependencias_tarefa", uniqueConstraints = @UniqueConstraint(columnNames = {"tarefa_id", "prerequisito_id"}),
        indexes = @Index(columnList = "prerequisito_id"))
public class DependenciaTarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarefa_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tarefa tarefa;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisito_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tarefa prerequisito;

    public DependenciaTarefa(Tarefa tarefa, Tarefa prerequisito) {
        this.tarefa = tarefa;
        this.prerequisito = prerequisito;
    }
}
