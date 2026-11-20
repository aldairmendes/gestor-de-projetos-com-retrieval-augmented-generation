package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class QuadroDto {
    private QuadroDto() {}

    public record Configuracao(
            @NotBlank @Size(max = 120) String nome,
            @NotEmpty List<@NotNull @Valid ColunaConfiguracao> colunas) {}

    public record ColunaConfiguracao(
            @Positive Long id,
            @NotBlank @Size(max = 120) String nome) {}

    public record QuadroResposta(Long id, Long projetoId, String nome, List<ColunaResposta> colunas) {}

    public record ColunaResposta(Long id, String nome, int posicao) {}
}
