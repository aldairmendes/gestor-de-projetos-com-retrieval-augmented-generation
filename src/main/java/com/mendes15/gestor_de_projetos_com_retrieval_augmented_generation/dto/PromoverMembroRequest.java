package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PromoverMembroRequest {
    private Long membroId;
    private String novoRole;
}