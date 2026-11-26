package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class QuadroDto {
    private QuadroDto() {}

    public record Configuracao(
            @NotBlank @Size(max = 120) String nome,
            @NotEmpty List<@NotNull @Valid ColunaConfiguracao> colunas,
            Map<@NotNull @Positive Long, @NotNull @Positive Long> realocacoes) {

        public Configuracao(String nome, List<ColunaConfiguracao> colunas) {
            this(nome, colunas, null);
        }
    }

    public record ColunaConfiguracao(
            @Positive Long id,
            @NotBlank @Size(max = 120) String nome,
            StatusTarefa status) {

        public ColunaConfiguracao(Long id, String nome) {
            this(id, nome, null);
        }
    }

    public record QuadroResposta(Long id, Long projetoId, String nome, List<ColunaResposta> colunas) {}

    public record ColunaResposta(Long id, String nome, int posicao, StatusTarefa status) {}
}
