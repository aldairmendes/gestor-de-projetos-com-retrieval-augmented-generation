package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDTO {
    private Long id;
    private String username;
    private String email;
}
