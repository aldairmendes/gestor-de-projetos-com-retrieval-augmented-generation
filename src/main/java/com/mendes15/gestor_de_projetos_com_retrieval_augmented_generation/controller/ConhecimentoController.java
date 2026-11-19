package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.DocumentService;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Base de conhecimento")
@SecurityRequirement(name = "bearerAuth")
public class ConhecimentoController {
    private final ProfileService profiles;
    private final DocumentService documents;

    @GetMapping("/usuarios/{usuarioId}/competencias")
    @Operation(summary = "Consultar competências", description = "Disponível ao titular do perfil ou a administradores.")
    public List<CompetenciaRequest> skills(@PathVariable Long usuarioId) {
        return profiles.skills(usuarioId);
    }

    @PutMapping("/usuarios/{usuarioId}/competencias")
    @Operation(summary = "Atualizar competências", description = "O titular ou administrador substitui a lista de competências do perfil.")
    public List<CompetenciaRequest> saveSkills(@PathVariable Long usuarioId, @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json", examples = @ExampleObject(value = "{\"competencias\":[{\"tecnologia\":\"Java\",\"tipoAtividade\":\"Backend\",\"nivel\":4}]}")))
            @Valid @RequestBody Competencias request) {
        return profiles.skills(usuarioId, request.competencias());
    }

    @GetMapping("/projetos/{projetoId}/documentos")
    @Operation(summary = "Listar documentos do projeto", description = "Exibe os documentos indexados aos integrantes do projeto.")
    public List<Documento> list(@PathVariable Long projetoId) {
        return documents.list(projetoId);
    }

    @GetMapping("/projetos/{projetoId}/documentos/{documentoId}/trechos")
    @Operation(summary = "Consultar trechos de um documento", description = "Permite aos integrantes verificar as fontes citadas pela IA.")
    public List<Fonte> chunks(@PathVariable Long projetoId, @PathVariable Long documentoId) {
        return documents.chunks(projetoId, documentoId);
    }
}
