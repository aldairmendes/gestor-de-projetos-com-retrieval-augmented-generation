package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioLoginRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRegisterRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Cadastro público e emissão de tokens JWT.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    @Operation(summary = "Cadastrar usuário", description = "Acesso público. Cria uma conta com o papel ROLE_MEMBRO e retorna o JWT como texto puro.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"SenhaExemplo123!\"}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", description = "Token JWT de acesso"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<String> register(@Valid @RequestBody UsuarioRegisterRequestDTO request) {
        return ResponseEntity.ok(service.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Acesso público. Valida e-mail e senha e retorna o JWT como texto puro. Use o token no botão Authorize.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"email\":\"maria@example.com\",\"password\":\"SenhaExemplo123!\"}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", description = "Token JWT de acesso"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    public ResponseEntity<String> authenticate(@Valid @RequestBody UsuarioLoginRequestDTO request) {
        return ResponseEntity.ok(service.autenticar(request));
    }
}