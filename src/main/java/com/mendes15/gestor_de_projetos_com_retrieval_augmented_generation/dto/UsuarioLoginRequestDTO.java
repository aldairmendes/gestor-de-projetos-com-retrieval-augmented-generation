package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLoginRequestDTO {

    @NotBlank(message = "O email não pode estar em branco")
    private String email;

    @NotBlank(message = "A senha não pode estar em branco")
    private String password;
}