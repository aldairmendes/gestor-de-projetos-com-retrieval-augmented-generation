package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Set<TipoRole> papeis;

    private Long id;
    private String username;
    private String email;
    private Date createdAt;
    private Date updatedAt;
}