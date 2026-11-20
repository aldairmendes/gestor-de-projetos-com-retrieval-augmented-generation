package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Anexo;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Comentario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.NovoComentario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Prerequisito;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.TarefaColaboracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tarefas/{tarefaId}")
@RequiredArgsConstructor
@Tag(name = "Colaboração em tarefas")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
@ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
@ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
public class TarefaColaboracaoController {
    private final TarefaColaboracaoService colaboracao;

    @GetMapping("/comentarios")
    @Operation(summary = "Listar comentários", description = "A equipe do projeto pode consultar os comentários em ordem de criação.")
    @ApiResponse(responseCode = "200", description = "Comentários da tarefa.")
    public List<Comentario> comentarios(@PathVariable Long tarefaId) {
        return colaboracao.listarComentarios(tarefaId);
    }

    @PostMapping("/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comentar tarefa", description = "Um integrante da equipe pode publicar um comentário em projeto ativo.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"texto\":\"Revisar os critérios.\"}")))
    @ApiResponse(responseCode = "201", description = "Comentário registrado.")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public Comentario comentar(@PathVariable Long tarefaId, @Valid @RequestBody NovoComentario comentario) {
        return colaboracao.comentar(tarefaId, comentario.texto());
    }

    @GetMapping("/anexos")
    @Operation(summary = "Listar anexos", description = "Retorna os metadados dos anexos para integrantes da equipe.")
    @ApiResponse(responseCode = "200", description = "Metadados dos anexos.")
    public List<Anexo> anexos(@PathVariable Long tarefaId) {
        return colaboracao.listarAnexos(tarefaId);
    }

    @PostMapping(path = "/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Anexar arquivo", description = "Integrantes da equipe podem enviar arquivos de até 5 MB em projeto ativo.")
    @ApiResponse(responseCode = "201", description = "Anexo registrado.")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @ApiResponse(responseCode = "413", description = "Arquivo acima do limite de 5 MB.")
    public ResponseEntity<Anexo> anexar(@PathVariable Long tarefaId, @RequestPart("arquivo") MultipartFile arquivo) {
        Anexo anexo = colaboracao.anexar(tarefaId, arquivo);
        var location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{anexoId}").buildAndExpand(anexo.id()).toUri();
        return ResponseEntity.created(location).body(anexo);
    }

    @GetMapping(path = "/anexos/{anexoId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Baixar anexo", description = "Entrega o arquivo da tarefa como download para integrantes da equipe.")
    @ApiResponse(responseCode = "200", description = "Conteúdo do anexo.")
    public ResponseEntity<byte[]> baixar(@PathVariable Long tarefaId, @PathVariable Long anexoId) {
        var anexo = colaboracao.baixar(tarefaId, anexoId);
        var disposition = ContentDisposition.attachment().filename(anexo.getNome(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(anexo.getTamanho())
                .cacheControl(CacheControl.noStore())
                .headers(headers -> headers.setContentDisposition(disposition))
                .header("X-Content-Type-Options", "nosniff")
                .body(anexo.getConteudo());
    }

    @DeleteMapping("/anexos/{anexoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover anexo", description = "Um gerente da equipe pode excluir anexos de uma tarefa em projeto ativo.")
    @ApiResponse(responseCode = "204", description = "Anexo removido.", content = @Content)
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public void removerAnexo(@PathVariable Long tarefaId, @PathVariable Long anexoId) {
        colaboracao.removerAnexo(tarefaId, anexoId);
    }

    @GetMapping("/dependencias")
    @Operation(summary = "Listar dependências", description = "Retorna os pré-requisitos registrados para a tarefa.")
    @ApiResponse(responseCode = "200", description = "Pré-requisitos da tarefa.")
    public List<Prerequisito> dependencias(@PathVariable Long tarefaId) {
        return colaboracao.listarDependencias(tarefaId);
    }

    @PutMapping("/dependencias/{prerequisitoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registrar dependência", description = "Um gerente da equipe pode registrar um pré-requisito do mesmo projeto. Uma repetição preserva o vínculo existente.")
    @ApiResponse(responseCode = "204", description = "Dependência registrada.", content = @Content)
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public void registrarDependencia(@PathVariable Long tarefaId, @PathVariable Long prerequisitoId) {
        colaboracao.registrarDependencia(tarefaId, prerequisitoId);
    }

    @DeleteMapping("/dependencias/{prerequisitoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover dependência", description = "Um gerente da equipe pode desvincular um pré-requisito em projeto ativo.")
    @ApiResponse(responseCode = "204", description = "A tarefa está sem o vínculo informado.", content = @Content)
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public void removerDependencia(@PathVariable Long tarefaId, @PathVariable Long prerequisitoId) {
        colaboracao.removerDependencia(tarefaId, prerequisitoId);
    }
}
