package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.PrioridadeTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarefaRequestDTO {

    @NotBlank(message = "Title is required")
    private String titulo;

    private String conteudo;

    @NotNull(message = "Status is required")
    private StatusTarefa status;

    @NotNull(message = "Priority is required")
    private PrioridadeTarefa prioridade;

    @NotNull(message = "Project ID is required")
    private Long projetoId;
    @Size(max=10000)
    private String requisitos;
    @Size(max=30)
    private Set<@NotBlank @Size(max=100) String> tecnologias = new LinkedHashSet<>();
    @Min(1) @Max(5)
    private Integer complexidade;
    private LocalDate prazo;
    private Long responsavelId;

    @Positive
    private Long colunaId;

    public TarefaRequestDTO(String titulo, String conteudo, StatusTarefa status, PrioridadeTarefa prioridade, Long projetoId) {
        this.titulo=titulo; this.conteudo=conteudo; this.status=status; this.prioridade=prioridade; this.projetoId=projetoId;
    }
}
