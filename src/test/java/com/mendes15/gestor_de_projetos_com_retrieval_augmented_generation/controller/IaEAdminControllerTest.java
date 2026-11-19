package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IaEAdminControllerTest extends BaseIntegrationTest {
    @Test void onlyAdminsCanManageDomain() throws Exception {
        mvc.perform(post("/api/admin/regras-dominio").with(as(membro))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/regras-dominio").with(as(gerente))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/regras-dominio").with(as(administrador)).contentType("application/json")
                        .content("""
                                {"regras":"Projeto acadêmico","vocabulario":"Ciclo de entrega"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/regras-dominio").with(as(administrador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vocabulario").value("Ciclo de entrega"));
    }

    @Test void consultationRequiresMembershipAndProvidesSourcesWhenAiIsDisabled() throws Exception {
        String body = """
                {"pergunta":"Qual é o conteúdo da tarefa inicial?"}
                """;
        mvc.perform(post("/api/ia/consultar").param("projetoId", projeto.getId().toString())
                        .with(as(externo)).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/ia/consultar").param("projetoId", projeto.getId().toString())
                        .with(as(membro)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alternativa").value(true))
                .andExpect(jsonPath("$.fontes[1].id").value("T" + tarefa.getId()));
    }

    @Test void recommendationCanBeAcceptedOnceAndAssignmentIsAudited() throws Exception {
        tarefa.getTecnologias().add("Java");
        tarefas.saveAndFlush(tarefa);
        mvc.perform(put("/api/usuarios/{id}/competencias", membro.getId()).with(as(membro))
                        .contentType("application/json").content("""
                                {"competencias":[{"tecnologia":"Java","tipoAtividade":"Backend","nivel":5}]}
                                """))
                .andExpect(status().isOk());
        String route = "/api/ia/tarefas/" + tarefa.getId() + "/recomendacoes";
        mvc.perform(post(route).with(as(membro))).andExpect(status().isForbidden());

        String response = mvc.perform(post(route).with(as(gerente))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.ranking[0].usuarioId").value(membro.getId()))
                .andExpect(jsonPath("$.contexto.alternativa").value(true))
                .andReturn().getResponse().getContentAsString();
        long recommendationId = objectMapper.readTree(response).path("id").asLong();
        assertNull(tarefa.getResponsavel());

        String feedbackRoute = route + "/" + recommendationId + "/feedback";
        mvc.perform(post(feedbackRoute).with(as(gerente)).contentType("application/json")
                        .content("{\"decisao\":\"ACEITA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.escolhidoId").value(membro.getId()));
        reload();
        assertEquals(membro.getId(), tarefas.findById(tarefa.getId()).orElseThrow().getResponsavel().getId());
        mvc.perform(get("/api/tarefas/{id}/historico", tarefa.getId()).with(as(membro)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].acao").value("ATRIBUICAO"));
        mvc.perform(post(feedbackRoute).with(as(gerente)).contentType("application/json")
                        .content("{\"decisao\":\"RECUSADA\"}"))
                .andExpect(status().isConflict());
    }

    @Test void ingestionEnforcesProjectScopeAndReturns503WithoutEmbeddings() throws Exception {
        var file = new MockMultipartFile("arquivo", "guia.md", "text/markdown", "# Guia".getBytes());
        String route = "/api/admin/projetos/{id}/documentos";
        mvc.perform(multipart(route, projeto.getId()).file(file).with(as(administrador)))
                .andExpect(status().isForbidden());
        projeto.getEquipe().add(administrador);
        projetos.saveAndFlush(projeto);
        mvc.perform(multipart(route, projeto.getId()).file(file).with(as(administrador)))
                .andExpect(status().isServiceUnavailable());
    }
}
