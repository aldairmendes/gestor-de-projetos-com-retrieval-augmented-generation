package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.TarefaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.Historico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tarefas", description = "Tarefas seguem as permissões e a equipe do projeto ao qual pertencem.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping
    @Operation(summary = "Criar tarefa", description = "Exige permissão de gerente no projeto e participação na equipe do projeto informado. O projeto deve estar ativo.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"titulo\":\"Documentar API\",\"conteudo\":\"Adicionar documentação Swagger\",\"status\":\"PENDENTE\",\"prioridade\":\"ALTA\",\"projetoId\":1}")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TarefaResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<TarefaResponseDTO> criar(
            @Valid @RequestBody TarefaRequestDTO request) {

        Tarefa tarefa = toTask(request);
        Tarefa tarefaSalva = tarefaService.save(tarefa);

        TarefaResponseDTO response =
                tarefaService.toResponseDTO(tarefaSalva);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Listar minhas tarefas", description = "Retorna somente tarefas dos projetos em cujas equipes o usuário autenticado participa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TarefaResponseDTO.class)))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public ResponseEntity<List<TarefaResponseDTO>> listar() {

        return ResponseEntity.ok(tarefaService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar tarefa", description = "Exige participação na equipe do projeto da tarefa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TarefaResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<TarefaResponseDTO> findById(
            @Parameter(description = "ID do tarefa", example = "1", required = true) @PathVariable Long id) {

        TarefaResponseDTO response = tarefaService.findById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/projeto/{projetoId}")
    @Operation(summary = "Listar tarefas de um projeto", description = "Exige participação na equipe do projeto, inclusive para administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TarefaResponseDTO.class)))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<List<TarefaResponseDTO>> listarPorProjeto(
            @Parameter(description = "ID do projeto cuja equipe o usuário integra", example = "1", required = true) @PathVariable Long projetoId) {

        return ResponseEntity.ok(tarefaService.listByProjeto(projetoId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar ou transferir tarefa", description = "Exige permissão de gerente no projeto e participação na equipe. Ao alterar projetoId, exige permissão nos projetos de origem e destino. Ambos devem estar ativos.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"titulo\":\"Documentar API\",\"conteudo\":\"Revisar exemplos\",\"status\":\"EM_ANDAMENTO\",\"prioridade\":\"ALTA\",\"projetoId\":1}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TarefaResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<TarefaResponseDTO> update(
            @Parameter(description = "ID do tarefa", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody TarefaRequestDTO request) {

        return ResponseEntity.ok(tarefaService.update(id, toTask(request)));
    }

    private Tarefa toTask(TarefaRequestDTO request) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(request.getTitulo());
        tarefa.setConteudo(request.getConteudo());
        tarefa.setStatus(request.getStatus());
        tarefa.setPrioridade(request.getPrioridade());
        tarefa.setRequisitos(request.getRequisitos());
        tarefa.setTecnologias(request.getTecnologias());
        tarefa.setComplexidade(request.getComplexidade());
        tarefa.setPrazo(request.getPrazo());

        Projeto projeto = new Projeto();
        projeto.setId(request.getProjetoId());
        tarefa.setProjeto(projeto);
        if (request.getResponsavelId() != null) {
            Usuario responsavel = new Usuario();
            responsavel.setId(request.getResponsavelId());
            tarefa.setResponsavel(responsavel);
        }
        return tarefa;
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Consultar histórico da tarefa", description = "Exibe alterações feitas no projeto atual aos integrantes da equipe.")
    public List<Historico> history(@PathVariable Long id) {
        return tarefaService.history(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da tarefa", description = "Qualquer integrante da equipe pode atualizar somente o status. O projeto deve estar ativo. Os demais campos permanecem inalterados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TarefaResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<TarefaResponseDTO> updateStatus(@Parameter(description = "ID do tarefa", example = "1", required = true) @PathVariable Long id,
            @Parameter(description = "Novo status da tarefa", example = "EM_ANDAMENTO", required = true)
            @RequestParam StatusTarefa novoStatus) {
        return ResponseEntity.ok(tarefaService.updateStatus(id, novoStatus));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tarefa", description = "Exige permissão de gerente no projeto e participação na equipe do projeto. O projeto deve estar ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído; resposta sem corpo.", content = @Content),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do tarefa", example = "1", required = true) @PathVariable Long id) {

        tarefaService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
