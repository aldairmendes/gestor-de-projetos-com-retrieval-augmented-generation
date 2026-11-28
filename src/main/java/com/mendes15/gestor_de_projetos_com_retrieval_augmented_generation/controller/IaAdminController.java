package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.DocumentService;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administração da IA")
@SecurityRequirement(name = "bearerAuth")
public class IaAdminController {
    private final ProfileService profiles;
    private final DocumentService documents;

    @GetMapping("/regras-dominio")
    @Operation(summary = "Consultar configuração de domínio", description = "Exibe as regras e o vocabulário da IA ao administrador.")
    public Dominio domain() {
        return profiles.domain();
    }

    @PostMapping("/regras-dominio")
    @Operation(summary = "Salvar configuração de domínio", description = "Atualiza as regras e o vocabulário usados nas próximas gerações.")
    public Dominio saveDomain(@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json", examples = @ExampleObject(value = "{\"regras\":\"Gestão de projetos\",\"vocabulario\":\"Sprint: ciclo de trabalho\"}")))
            @Valid @RequestBody Dominio request) {
        return profiles.domain(request);
    }

    @PostMapping(path = "/projetos/{projetoId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Indexar documento", description = "Administrador integrante envia PDF, Markdown ou texto UTF-8 de até 5 MB.")
    public Documento ingest(@PathVariable Long projetoId, @RequestPart("arquivo") MultipartFile file) {
        return documents.ingest(projetoId, file);
    }

    @DeleteMapping("/projetos/{projetoId}/documentos/{documentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Documento excluído.", content = @Content)
    @Operation(summary = "Excluir documento indexado", description = "Administrador integrante remove o documento, seus trechos e vetores.")
    public void delete(@PathVariable Long projetoId, @PathVariable Long documentoId) {
        documents.delete(projetoId, documentoId);
    }
}
