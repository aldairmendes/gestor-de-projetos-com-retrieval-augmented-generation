package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlterarPapelMembroRequest {

    @NotNull(message = "O ID do usuário não pode ser nulo")
    private Long usuarioId;

    @NotBlank(message = "O novo papel não pode estar em branco")
    private String novoPapel;
}