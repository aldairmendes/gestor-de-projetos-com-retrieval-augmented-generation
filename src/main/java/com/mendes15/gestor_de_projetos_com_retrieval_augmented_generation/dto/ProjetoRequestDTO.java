package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoRequestDTO {
    @NotBlank(message = "O nome do projeto não pode estar vazio")
    @Size(max = 255, message = "O nome do projeto deve ter no máximo 255 caracteres")
    private String nome;
    private String descricao;
    private List<Long> equipeIds;
    @Positive
    private Long gerenteId;

    public ProjetoRequestDTO(String nome, String descricao, List<Long> equipeIds) {
        this(nome, descricao, equipeIds, null);
    }

    public ProjetoRequestDTO(String nome, String descricao) {
        this(nome, descricao, null);
    }
}
