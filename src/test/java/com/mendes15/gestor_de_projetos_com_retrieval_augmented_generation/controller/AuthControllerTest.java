package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioLoginRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRegisterRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso")
    void deveRegistrarUsuarioComSucesso() throws Exception {
        UsuarioRegisterRequestDTO request = new UsuarioRegisterRequestDTO("aldair_mendes", "aldair@exemplo.com", "senha123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Não deve permitir registrar usuário com e-mail duplicado")
    void naoDevePermitirEmailDuplicado() throws Exception {
        UsuarioRegisterRequestDTO request = new UsuarioRegisterRequestDTO("aldair", "duplicado@exemplo.com", "senha123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError()); // Ou isConflict() dependendo do GlobalExceptionHandler
    }

    @Test
    @DisplayName("Deve autenticar usuário cadastrado e retornar token JWT")
    void deveAutenticarComSucesso() throws Exception {
        UsuarioRegisterRequestDTO register = new UsuarioRegisterRequestDTO("user_login", "login@exemplo.com", "senha123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        UsuarioLoginRequestDTO login = new UsuarioLoginRequestDTO("login@exemplo.com", "senha123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").exists());
    }
}