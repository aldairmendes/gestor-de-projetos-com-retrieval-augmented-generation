package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.ProjectInsights;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagService;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
@Tag(name = "Inteligência artificial")
@SecurityRequirement(name = "bearerAuth")
public class IaController {
    private final RagService rag;
    private final ProjectInsights insights;
    private final RecommendationService recommendations;

    @PostMapping("/consultar")
    @Operation(summary = "Consultar fontes do projeto", description = "Responde com citações de fontes acessíveis à equipe.")
    public Resposta consult(@RequestParam Long projetoId, @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json", examples = @ExampleObject(value = "{\"pergunta\":\"Quais tarefas estão em andamento?\"}")))
            @Valid @RequestBody Consulta request) {
        return rag.answer(projetoId, request.pergunta());
    }

    @GetMapping("/projetos/{projetoId}/resumo")
    @Operation(summary = "Resumir andamento", description = "Gerentes podem informar início e fim para resumir tarefas pelo prazo.")
    public Resposta summary(@PathVariable Long projetoId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return insights.summary(projetoId, inicio, fim);
    }

    @GetMapping("/projetos/{projetoId}/gargalos")
    @Operation(summary = "Identificar gargalos", description = "Apresenta indicadores de sobrecarga, atraso e inatividade a gerentes da equipe.")
    public Resposta bottlenecks(@PathVariable Long projetoId) {
        return insights.bottlenecks(projetoId);
    }

    @PostMapping("/tarefas/{tarefaId}/descricao")
    @Operation(summary = "Gerar ou refinar descrição", description = "Gera uma proposta para o gerente revisar antes de editar a tarefa.")
    public Resposta description(@PathVariable Long tarefaId, @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json", examples = @ExampleObject(value = "{\"pergunta\":\"Quais tarefas estão em andamento?\"}")))
            @Valid @RequestBody Consulta request) {
        return insights.description(tarefaId, request.pergunta());
    }

    @GetMapping("/tarefas/{tarefaId}/dependencias")
    @Operation(summary = "Sugerir dependências", description = "Apresenta possíveis pré-requisitos dentro do projeto para revisão do gerente.")
    public Resposta dependencies(@PathVariable Long tarefaId) {
        return insights.dependencies(tarefaId);
    }

    @PostMapping("/tarefas/{tarefaId}/recomendacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Recomendar responsáveis", description = "Gera e registra um ranking para o gerente após criar ou atualizar uma tarefa.")
    public Recomendacao recommend(@PathVariable Long tarefaId) {
        return recommendations.recommend(tarefaId);
    }

    @GetMapping("/tarefas/{tarefaId}/recomendacoes")
    @Operation(summary = "Listar recomendações registradas", description = "Exibe para o gerente as recomendações do projeto atual desta tarefa.")
    public List<Recomendacao> list(@PathVariable Long tarefaId) {
        return recommendations.list(tarefaId);
    }

    @PostMapping("/tarefas/{tarefaId}/recomendacoes/{id}/feedback")
    @Operation(summary = "Registrar decisão sobre recomendação", description = "O gerente pode aceitar, ajustar ou recusar a alocação.")
    public Recomendacao feedback(@PathVariable Long tarefaId, @PathVariable Long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json", examples = @ExampleObject(value = "{\"decisao\":\"ACEITA\"}")))
            @Valid @RequestBody Feedback request) {
        return recommendations.decide(tarefaId, id, request);
    }
}
