package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "colunas_quadro", indexes = @Index(name = "idx_colunas_quadro_posicao", columnList = "quadro_id, posicao"))
public class ColunaQuadro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadro_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Quadro quadro;

    @Setter
    @Column(nullable = false, length = 120)
    private String nome;

    @Setter
    @Column(nullable = false)
    private int posicao;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusTarefa status;
}
