package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IaEAdminControllerTest extends BaseIntegrationTest {

    @Test
    @WithMockUser(username = "membro@exemplo.com", roles = {"MEMBRO"})
    @DisplayName("Membros e Gerentes podem realizar consulta em linguagem natural à base do projeto (IA)")
    void qualquerMembroPodeConsultarIa() throws Exception {
        mockMvc.perform(get("/api/ia/consultar")
                        .param("query", "Quais tarefas estão atrasadas no projeto?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").exists());
    }

    @Test
    @WithMockUser(username = "gerente@exemplo.com", roles = {"GERENTE"})
    @DisplayName("Gerente de projeto pode solicitar recomendação de responsáveis via IA")
    void gerentePodeSolicitarRecomendacaoIa() throws Exception {
        mockMvc.perform(get("/api/ia/recomendar-responsaveis")
                        .param("tarefaId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "membro@exemplo.com", roles = {"MEMBRO"})
    @DisplayName("Membro NÃO pode alterar as regras globais de domínio do sistema")
    void membroNaoPodeConfigurarRegrasDeDominio() throws Exception {
        mockMvc.perform(post("/api/admin/regras-dominio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regra\": \"Nova Regra de Regras do Sistema\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@exemplo.com", roles = {"ADMINISTRADOR"})
    @DisplayName("Apenas o Administrador pode configurar regras globais de domínio")
    void adminPodeConfigurarRegrasDeDominio() throws Exception {
        mockMvc.perform(post("/api/admin/regras-dominio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regra\": \"Nova Regra de Regras do Sistema\"}"))
                .andExpect(status().isOk());
    }
}