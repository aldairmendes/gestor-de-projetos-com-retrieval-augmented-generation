package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AlterarPapeisRequest(@NotEmpty Set<@NotNull TipoRole> papeis) {}
