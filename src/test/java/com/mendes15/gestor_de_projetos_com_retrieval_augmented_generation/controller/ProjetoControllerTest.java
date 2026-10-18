package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.PromoverMembroRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjetoControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "criador@exemplo.com", roles = {"MEMBRO"})
    @DisplayName("Criador do projeto deve se tornar o Gerente do projeto automaticamente")
    void criadorDeveVirarGerenteDoProjeto() throws Exception {
        ProjetoRequestDTO request = new ProjetoRequestDTO("Sistema RAG", "Descrição do projeto RAG");

        mockMvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Sistema RAG"))
                .andExpect(jsonPath("$.meuPapelNoProjeto").value("ROLE_GERENTE"));
    }

    @Test
    @WithMockUser(username = "membro_comum@exemplo.com", roles = {"MEMBRO"})
    @DisplayName("Usuário comum (ROLE_MEMBRO) NÃO deve conseguir deletar um projeto")
    void membroNaoPodeDeletarProjeto() throws Exception {
        mockMvc.perform(delete("/api/projetos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gerente@exemplo.com", roles = {"GERENTE"})
    @DisplayName("Gerente do projeto (ROLE_GERENTE) DEVE conseguir deletar o projeto")
    void gerentePodeDeletarProjeto() throws Exception {
        mockMvc.perform(delete("/api/projetos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "gerente@exemplo.com", roles = {"GERENTE"})
    @DisplayName("Gerente pode promover outro usuário a gerente dentro do projeto")
    void gerentePodePromoverMembro() throws Exception {
        PromoverMembroRequest request = new PromoverMembroRequest(2L, "ROLE_GERENTE");

        mockMvc.perform(put("/api/projetos/1/membros/promover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}