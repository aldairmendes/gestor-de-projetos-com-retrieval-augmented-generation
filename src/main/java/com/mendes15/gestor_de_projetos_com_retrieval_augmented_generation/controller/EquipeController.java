package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.EquipeRequest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TeamMemberDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/projetos/{projetoId}/equipe")
@RequiredArgsConstructor
@Tag(name = "Equipes", description = "Formação e gestão da equipe de cada projeto. Papéis são globais.")
@SecurityRequirement(name = "bearerAuth")
public class EquipeController {
    private final ProjetoService projetoService;

    @GetMapping
    @Operation(summary = "Consultar equipe do projeto", description = "Disponível aos integrantes da equipe, inclusive em projetos arquivados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Integrantes da equipe ordenados por nome.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TeamMemberDTO.class)))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<List<TeamMemberDTO>> list(
            @Parameter(description = "ID do projeto", example = "1") @PathVariable Long projetoId) {
        return ResponseEntity.ok(projetoService.listTeam(projetoId));
    }

    @PutMapping
    @Operation(summary = "Definir equipe do projeto", description = "Exige ROLE_GERENTE e participação na equipe de um projeto ativo. Substitui os integrantes, preservando o criador. Uma lista vazia mantém somente o criador. Não altera nome ou descrição do projeto. Se o gerente remover a si mesmo, perderá acesso ao projeto.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"membrosIds\":[2,3]}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Integrantes da equipe ordenados por nome.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TeamMemberDTO.class)))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<List<TeamMemberDTO>> define(
            @Parameter(description = "ID do projeto", example = "1") @PathVariable Long projetoId,
            @Valid @RequestBody EquipeRequest request) {
        return ResponseEntity.ok(projetoService.defineTeam(projetoId, request.membrosIds()));
    }

    @PostMapping("/membros")
    @Operation(summary = "Adicionar membros à equipe", description = "Exige ROLE_GERENTE e participação na equipe de um projeto ativo. Adiciona usuários cadastrados e preserva os integrantes atuais. IDs repetidos ou já presentes não duplicam membros. A inclusão concede acesso ao projeto e às suas tarefas; não promove papéis globais.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"membrosIds\":[2,3]}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Integrantes da equipe ordenados por nome.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TeamMemberDTO.class)))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<List<TeamMemberDTO>> addMembers(
            @Parameter(description = "ID do projeto", example = "1") @PathVariable Long projetoId,
            @Valid @RequestBody EquipeRequest request) {
        return ResponseEntity.ok(projetoService.addTeamMembers(projetoId, request.membrosIds()));
    }

    @DeleteMapping("/membros/{usuarioId}")
    @Operation(summary = "Remover membro da equipe", description = "Exige ROLE_GERENTE e participação na equipe de um projeto ativo. O criador não pode ser removido. A remoção revoga o acesso ao projeto e às suas tarefas, sem excluir a conta do usuário ou alterar seus papéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido.", content = @Content),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "ID do projeto", example = "1") @PathVariable Long projetoId,
            @Parameter(description = "ID do integrante a remover", example = "2") @PathVariable Long usuarioId) {
        projetoService.removeTeamMember(projetoId, usuarioId);
        return ResponseEntity.noContent().build();
    }

}
