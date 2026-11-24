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

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.AlterarPapeisRequest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRegisterRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Usuários", description = "Perfis de usuário e administração dos papéis globais.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ModelMapper modelMapper;

    @PostMapping
    @Operation(summary = "Criar usuário como administrador", description = "Exige ROLE_ADMINISTRADOR. Cria uma conta com ROLE_MEMBRO e armazena a senha com BCrypt. Para cadastro público, utilize /api/v1/auth/register.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"username\":\"joao\",\"email\":\"joao@example.com\",\"password\":\"SenhaExemplo123!\"}")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<UsuarioResponseDTO> save(
            @Valid @RequestBody UsuarioRegisterRequestDTO request) {

        Usuario user = modelMapper.map(request, Usuario.class);

        UsuarioResponseDTO savedUser = usuarioService.save(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedUser);
    }

    @GetMapping
    @Operation(summary = "Consultar diretório de usuários", description = "Exige ser gerente de um projeto ou administrador. Permite consultar os usuários para formar equipes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponseDTO.class)))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public ResponseEntity<List<UsuarioResponseDTO>> findAll() {

        List<UsuarioResponseDTO> users = usuarioService.findAll();

        List<UsuarioResponseDTO> response = users.stream()
                .map(usuario ->
                        modelMapper.map(usuario, UsuarioResponseDTO.class))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar perfil", description = "Permite consultar o próprio perfil. ROLE_ADMINISTRADOR pode consultar outros usuários.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<UsuarioResponseDTO> findById(
            @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable Long id) {

        UsuarioResponseDTO usuario = usuarioService.findById(id);

        UsuarioResponseDTO response =
                modelMapper.map(usuario, UsuarioResponseDTO.class);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar perfil", description = "Permite atualizar o próprio perfil; ROLE_ADMINISTRADOR pode atualizar outros usuários. Não altera papéis. A validação exige username, email e password; a senha informada é armazenada com BCrypt.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"NovaSenhaExemplo123!\"}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<UsuarioResponseDTO> update(
            @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody UsuarioRegisterRequestDTO request) {

        UsuarioResponseDTO updatedUser =
                usuarioService.update(id, request);

        UsuarioResponseDTO response =
                modelMapper.map(updatedUser, UsuarioResponseDTO.class);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/papeis")
    @Operation(summary = "Substituir papéis globais", description = "Exige ROLE_ADMINISTRADOR. Substitui os papéis informados e sempre preserva ROLE_MEMBRO. As novas permissões são verificadas no banco sem exigir novo JWT.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"papeis\":[\"ROLE_ADMINISTRADOR\"]}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public ResponseEntity<UsuarioResponseDTO> updateRoles(@Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody AlterarPapeisRequest request) {
        return ResponseEntity.ok(usuarioService.updateRoles(id, request.papeis()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário", description = "Exige ROLE_ADMINISTRADOR. Pode ser impedido por vínculos e restrições de integridade do banco.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído; resposta sem corpo.", content = @Content),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable Long id) {

        usuarioService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
