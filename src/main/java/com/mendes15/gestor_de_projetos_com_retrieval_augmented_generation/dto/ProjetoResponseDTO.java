package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoResponseDTO {

    private Long id;
    private String nome;
    private List<Long> equipeIds;
    private List<Long> tarefasIds;
    private Date createdAt;
}