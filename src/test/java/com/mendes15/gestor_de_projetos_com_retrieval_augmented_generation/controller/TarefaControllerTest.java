package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TarefaControllerTest extends BaseIntegrationTest {
    String url() { return "/api/tarefas/" + tarefa.getId(); }
    TarefaRequestDTO request(Long projectId) {
        return new TarefaRequestDTO("Nova tarefa", "Descrição", StatusTarefa.PENDENTE, PrioridadeTarefa.ALTA, projectId);
    }

    @Test void managerCreatesAndPersistsTaskInSpecifiedProject() throws Exception {
        String response = mvc.perform(post("/api/tarefas").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(projeto.getId())))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Nova tarefa"))
                .andExpect(jsonPath("$.projetoId").value(projeto.getId())).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong(); reload();
        assertEquals(projeto.getId(), tarefas.findById(id).orElseThrow().getProjeto().getId());
    }

    @Test void memberMayReadAllProjectEndpoints() throws Exception {
        mvc.perform(get("/api/tarefas").with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(url()).with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tarefa.getId()));
        mvc.perform(get("/api/tarefas/projeto/"+projeto.getId()).with(as(membro)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void outsidersCannotReadTasksAndListIsEmpty() throws Exception {
        mvc.perform(get(url()).with(as(externo))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tarefas").with(as(externo))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/tarefas/projeto/"+projeto.getId()).with(as(externo)))
                .andExpect(status().isForbidden());
    }

    @Test void memberCanChangeOnlyStatus() throws Exception {
        mvc.perform(patch(url()+"/status").with(as(membro)).param("novoStatus", "EM_ANDAMENTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        reload(); var saved = tarefas.findById(tarefa.getId()).orElseThrow();
        assertEquals("Tarefa inicial", saved.getTitulo()); assertEquals("Conteúdo inicial", saved.getConteudo());
        assertEquals(StatusTarefa.EM_ANDAMENTO, saved.getStatus());
        mvc.perform(put(url()).with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(projeto.getId())))).andExpect(status().isForbidden());
        mvc.perform(delete(url()).with(as(membro))).andExpect(status().isForbidden());
        mvc.perform(post("/api/tarefas").with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(projeto.getId())))).andExpect(status().isForbidden());
    }

    @Test void managerUpdatesTask() throws Exception {
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(projeto.getId())))).andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Nova tarefa")).andExpect(jsonPath("$.prioridade").value("ALTA"));
        reload(); assertEquals("Nova tarefa", tarefas.findById(tarefa.getId()).orElseThrow().getTitulo());
    }

    @Test void managerCanTransferBetweenOwnProjects() throws Exception {
        Projeto destino = project("Destino", gerente);
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(destino.getId())))).andExpect(status().isOk())
                .andExpect(jsonPath("$.projetoId").value(destino.getId()));
        reload(); assertEquals(destino.getId(), tarefas.findById(tarefa.getId()).orElseThrow().getProjeto().getId());
        mvc.perform(get(url()).with(as(membro))).andExpect(status().isForbidden());
    }

    @Test void transferToForeignProjectIsRejectedWithoutChangingTask() throws Exception {
        Projeto destino = project("Privado", externo);
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(destino.getId())))).andExpect(status().isForbidden());
        assertEquals(projeto.getId(), tarefa.getProjeto().getId());
        assertEquals("Tarefa inicial", tarefa.getTitulo());
    }

    @Test void archivedProjectsRejectTaskChanges() throws Exception {
        projeto.setArquivado(true); projetos.saveAndFlush(projeto);
        mvc.perform(post("/api/tarefas").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(projeto.getId())))).andExpect(status().isConflict());
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(projeto.getId())))).andExpect(status().isConflict());
        mvc.perform(patch(url()+"/status").with(as(membro)).param("novoStatus","CONCLUIDA"))
                .andExpect(status().isConflict());
        mvc.perform(delete(url()).with(as(gerente))).andExpect(status().isConflict());
    }

    @Test void managerDeletesTask() throws Exception {
        mvc.perform(delete(url()).with(as(gerente))).andExpect(status().isNoContent());
        reload(); assertFalse(tarefas.existsById(tarefa.getId()));
    }

    @Test void invalidEnumMissingStatusAndMissingFieldsReturn400() throws Exception {
        for (String value : new String[]{"INVALIDO", ""})
            mvc.perform(patch(url()+"/status").with(as(membro)).param("novoStatus",value))
                    .andExpect(status().isBadRequest());
        mvc.perform(patch(url()+"/status").with(as(membro))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tarefas").with(as(gerente)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.projetoId").exists());
    }

    @Test void missingTaskAndProjectReturn404() throws Exception {
        mvc.perform(get("/api/tarefas/"+Long.MAX_VALUE).with(as(gerente))).andExpect(status().isNotFound());
        mvc.perform(post("/api/tarefas").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(Long.MAX_VALUE)))).andExpect(status().isNotFound());
    }
}
