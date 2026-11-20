package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.QuadroDto.Configuracao;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.QuadroDto.QuadroResposta;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.QuadroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/projetos/{projetoId}/quadros")
@RequiredArgsConstructor
@Tag(name = "Quadros", description = "Quadros e colunas personalizáveis por projeto.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
@ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
@ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
public class QuadroController {
    private final QuadroService quadros;

    @GetMapping
    @Operation(summary = "Listar quadros do projeto", description = "Disponível aos integrantes da equipe.")
    @ApiResponse(responseCode = "200", description = "Quadros do projeto.")
    public List<QuadroResposta> listar(@PathVariable Long projetoId) {
        return quadros.listar(projetoId);
    }

    @GetMapping("/{quadroId}")
    @Operation(summary = "Consultar configuração do quadro", description = "Retorna as colunas na ordem configurada.")
    @ApiResponse(responseCode = "200", description = "Configuração do quadro.")
    public QuadroResposta consultar(@PathVariable Long projetoId, @PathVariable Long quadroId) {
        return quadros.consultar(projetoId, quadroId);
    }

    @PostMapping
    @Operation(summary = "Criar quadro com colunas na ordem informada",
            description = "Exige um gerente da equipe e um projeto ativo. Informe pelo menos uma coluna, sem IDs.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {"nome":"Entrega","colunas":[{"nome":"A fazer"},{"nome":"Concluído"}]}
                    """)))
    @ApiResponse(responseCode = "201", description = "Quadro criado.")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public ResponseEntity<QuadroResposta> criar(@PathVariable Long projetoId, @Valid @RequestBody Configuracao configuracao) {
        QuadroResposta quadro = quadros.criar(projetoId, configuracao);
        var location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{quadroId}").buildAndExpand(quadro.id()).toUri();
        return ResponseEntity.created(location).body(quadro);
    }

    @PutMapping("/{quadroId}")
    @Operation(summary = "Configurar quadro e colunas",
            description = "A lista define a ordem. Envie IDs para manter colunas, omita IDs para criar e retire da lista as colunas a remover.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {"nome":"Entrega atualizada","colunas":[{"id":1,"nome":"Em andamento"},{"nome":"Revisão"}]}
                    """)))
    @ApiResponse(responseCode = "200", description = "Quadro atualizado.")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public QuadroResposta atualizar(@PathVariable Long projetoId, @PathVariable Long quadroId,
                                    @Valid @RequestBody Configuracao configuracao) {
        return quadros.atualizar(projetoId, quadroId, configuracao);
    }
}
