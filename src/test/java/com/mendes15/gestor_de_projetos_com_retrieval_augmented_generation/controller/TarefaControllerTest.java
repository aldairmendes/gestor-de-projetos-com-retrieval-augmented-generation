package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TarefaControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "membro@exemplo.com", roles = {"MEMBRO"})
    @DisplayName("Membro do projeto pode criar tarefas no quadro")
    void membroPodeCriarTarefa() throws Exception {
        TarefaRequestDTO request = new TarefaRequestDTO();
        request.setTitulo("Ajustar Pipeline CI");
        request.setConteudo("Configurar GitHub Actions");
        request.setStatus(StatusTarefa.PENDENTE);
        request.setProjetoId(1L);

        mockMvc.perform(post("/api/projetos/1/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Ajustar Pipeline CI"));
    }

    @Test
    @WithMockUser(username = "membro@exemplo.com", roles = {"MEMBRO"})
    @DisplayName("Membro pode atualizar o status da tarefa")
    void membroPodeAtualizarStatusTarefa() throws Exception {
        mockMvc.perform(patch("/api/projetos/1/tarefas/10/status")
                        .param("novoStatus", "EM_ANDAMENTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }
}