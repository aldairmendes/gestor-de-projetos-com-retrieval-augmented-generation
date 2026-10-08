package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjetoRequestDTO {
    @NotBlank(message = "O nome do projeto não pode estar vazio")
    private String nome;
}