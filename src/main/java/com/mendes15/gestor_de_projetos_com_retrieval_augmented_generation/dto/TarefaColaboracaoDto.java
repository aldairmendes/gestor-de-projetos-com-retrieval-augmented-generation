package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TarefaColaboracaoDto {
    private TarefaColaboracaoDto() {}

    public record NovoComentario(@NotBlank @Size(max = 4000) String texto) {}

    public record Comentario(Long id, Long autorId, String texto, Instant criadoEm) {}

    public record Anexo(Long id, Long autorId, String nome, int tamanho, Instant criadoEm) {}

    public record Prerequisito(Long id, String titulo) {}
}
