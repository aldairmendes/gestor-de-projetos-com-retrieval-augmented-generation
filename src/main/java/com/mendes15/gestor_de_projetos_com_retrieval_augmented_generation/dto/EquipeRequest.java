package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "Usuários que devem compor a equipe do projeto.")
public record EquipeRequest(
        @NotNull(message = "Informe os IDs dos membros")
        @Schema(description = "IDs dos usuários cadastrados. Repetições são ignoradas.", example = "[2,3]")
        List<@NotNull @Positive Long> membrosIds) {}
