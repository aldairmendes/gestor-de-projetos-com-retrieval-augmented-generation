package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.PrioridadeTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
}
