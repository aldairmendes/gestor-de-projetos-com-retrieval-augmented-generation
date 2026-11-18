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

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.ProjetoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Projetos", description = "Projetos visíveis somente aos integrantes da equipe, inclusive administradores.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @PostMapping
    @Operation(summary = "Criar projeto", description = "Exige ROLE_GERENTE. Inclui automaticamente o criador na equipe. Administradores precisam também do papel de gerente.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"nome\":\"Plataforma RAG\",\"descricao\":\"Gestão de projetos\",\"equipeIds\":[2,3]}")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjetoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public ResponseEntity<ProjetoResponseDTO> create(@RequestBody @Valid ProjetoRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.create(requestDTO));
    }

    @GetMapping
    @Operation(summary = "Listar meus projetos", description = "Retorna somente projetos das equipes do usuário autenticado, incluindo os arquivados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProjetoResponseDTO.class)))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public ResponseEntity<List<ProjetoResponseDTO>> findAll() {
        return ResponseEntity.ok(projetoService.listAccessible());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar projeto", description = "Exige participação na equipe do projeto. Ser administrador ou criador não dispensa essa associação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjetoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<ProjetoResponseDTO> findById(@Parameter(description = "ID do projeto", example = "1", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(projetoService.findAccessibleById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar projeto e equipe", description = "Exige ROLE_GERENTE e participação na equipe. Não permite editar projeto arquivado. equipeIds nulo preserva a equipe; quando informado, substitui a equipe e mantém o criador.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"nome\":\"Plataforma RAG\",\"descricao\":\"Descrição atualizada\",\"equipeIds\":[2,3]}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjetoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<ProjetoResponseDTO> update(
            @Parameter(description = "ID do projeto", example = "1", required = true) @PathVariable Long id,
            @RequestBody @Valid ProjetoRequestDTO requestDTO) {
        return ResponseEntity.ok(projetoService.update(id, requestDTO));
    }

    @PatchMapping("/{id}/arquivar")
    @Operation(summary = "Arquivar projeto", description = "Exige ROLE_GERENTE e participação na equipe. O projeto continua visível, mas suas tarefas não podem ser alteradas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjetoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<ProjetoResponseDTO> archive(@Parameter(description = "ID do projeto", example = "1", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(projetoService.setArchived(id, true));
    }

    @PatchMapping("/{id}/desarquivar")
    @Operation(summary = "Desarquivar projeto", description = "Exige ROLE_GERENTE e participação na equipe. Reabilita as alterações no projeto e nas tarefas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjetoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<ProjetoResponseDTO> unarchive(@Parameter(description = "ID do projeto", example = "1", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(projetoService.setArchived(id, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir projeto", description = "Exige ROLE_GERENTE e participação na equipe. Exclui o projeto e suas tarefas definitivamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído; resposta sem corpo.", content = @Content),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "ID do projeto", example = "1", required = true) @PathVariable Long id) {
        projetoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
