package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class RagDtos {
    private RagDtos() {}
    public record Consulta(@NotBlank @Size(max=4000) String pergunta) {}
    public record Fonte(String id, String tipo, Long recursoId, String titulo, Integer pagina, String trecho, double similaridade) {}
    public record Resposta(String resposta, List<Fonte> fontes, boolean alternativa, String aviso) {}
    public record Documento(Long id, String nome, String tipo, int trechos, Instant criadoEm) {}
    public record CompetenciaRequest(@NotBlank @Size(max=100) String tecnologia,
                                     @NotBlank @Size(max=100) String tipoAtividade, @Min(1) @Max(5) int nivel) {}
    public record Competencias(@NotNull @Size(max=50) List<@NotNull @Valid CompetenciaRequest> competencias) {}
    public record Dominio(@NotBlank @Size(max=4000) String regras, @NotNull @Size(max=4000) String vocabulario) {}
    public record Candidato(Long usuarioId, double pontuacao, String justificativa, List<String> fontes) {}
    public record Recomendacao(Long id, Long tarefaId, List<Candidato> ranking, Resposta contexto, String decisao, Long escolhidoId) {}
    public enum Decisao { ACEITA, AJUSTADA, RECUSADA }
    public record Feedback(@NotNull Decisao decisao, @Positive Long usuarioId, @Size(max=2000) String observacao) {}
    public record Historico(Long id, String acao, Long atorId, Long responsavelId, String status, Instant criadoEm) {}
}
