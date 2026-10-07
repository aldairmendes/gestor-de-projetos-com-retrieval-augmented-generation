package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.PrioridadeTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarefaResponseDTO {

    private Long id;
    private String titulo;
    private String conteudo;
    private StatusTarefa status;
    private PrioridadeTarefa prioridade;
    private Long projetoId;
    private Date createdAt;
    private Date updatedAt;
}