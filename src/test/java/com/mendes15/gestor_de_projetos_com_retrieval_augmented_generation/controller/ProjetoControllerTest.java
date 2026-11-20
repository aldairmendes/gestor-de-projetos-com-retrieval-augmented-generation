package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjetoControllerTest extends BaseIntegrationTest {
    String url() { return "/api/projetos/" + projeto.getId(); }

    @Test void managerCreatesProjectWithDeduplicatedTeamAndCreator() throws Exception {
        mvc.perform(post("/api/projetos").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO(" Novo projeto ", " Descrição ", List.of(membro.getId(), membro.getId())))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Novo projeto"))
                .andExpect(jsonPath("$.descricao").value("Descrição"))
                .andExpect(jsonPath("$.equipe.length()").value(2))
                .andExpect(jsonPath("$.criadorId").value(gerente.getId()))
                .andExpect(jsonPath("$.meuPapelNoProjeto").value("ROLE_GERENTE"))
                .andExpect(jsonPath("$.podeGerenciar").value(true));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", ",\"equipeIds\":[]", ",\"equipeIds\":null"})
    void memberCreatesEmptyProjectAndAddsPeopleLater(String equipe) throws Exception {
        String token = jwt.generateToken(membro);
        var criado = mvc.perform(post("/api/projetos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Meu projeto\"" + equipe + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criadorId").value(membro.getId()))
                .andExpect(jsonPath("$.equipeIds.length()").value(1))
                .andExpect(jsonPath("$.equipeIds[0]").value(membro.getId()))
                .andExpect(jsonPath("$.tarefasIds.length()").value(0))
                .andExpect(jsonPath("$.podeGerenciar").value(false))
                .andExpect(jsonPath("$.podeAdicionarMembros").value(true))
                .andReturn().getResponse();
        String novo = "/api/projetos/" + objectMapper.readTree(criado.getContentAsByteArray()).get("id").asLong();
        reload();
        mvc.perform(get(novo).with(as(externo))).andExpect(status().isForbidden());
        mvc.perform(post(novo + "/equipe/membros").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membrosIds\":[" + externo.getId() + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        reload();
        mvc.perform(get(novo).with(as(externo))).andExpect(status().isOk())
                .andExpect(jsonPath("$.equipeIds.length()").value(2));
        assertEquals(java.util.Set.of("MEMBRO"), usuarios.findById(membro.getId()).orElseThrow().getRoles()
                .stream().map(role -> role.getNome()).collect(java.util.stream.Collectors.toSet()));
        mvc.perform(post(url() + "/equipe/membros").with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membrosIds\":[" + externo.getId() + "]}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url()).with(as(membro))).andExpect(status().isForbidden());
    }

    @Test void memberCannotSetInitialTeamOrAddPeopleToArchivedProject() throws Exception {
        mvc.perform(post("/api/projetos").with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO("Com equipe", null, List.of(externo.getId())))))
                .andExpect(status().isForbidden());
        var arquivado = project("Arquivado", membro);
        arquivado.setArquivado(true);
        projetos.saveAndFlush(arquivado);
        String urlArquivado = "/api/projetos/" + arquivado.getId();
        mvc.perform(post(urlArquivado + "/equipe/membros").with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membrosIds\":[" + externo.getId() + "]}"))
                .andExpect(status().isConflict());
        mvc.perform(get(urlArquivado).with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.podeAdicionarMembros").value(false));
    }

    @Test void listOnlyIncludesParticipatingProjects() throws Exception {
        project("Segredo", externo);
        mvc.perform(get("/api/projetos").with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(projeto.getId()))
                .andExpect(jsonPath("$[0].podeGerenciar").value(false));
        mvc.perform(get("/api/projetos").with(as(administrador))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void outsidersCannotReadUpdateArchiveOrDelete() throws Exception {
        mvc.perform(get(url()).with(as(externo))).andExpect(status().isForbidden());
        mvc.perform(put(url()).with(as(externo)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO("Invasão", null))))
                .andExpect(status().isForbidden());
        mvc.perform(patch(url()+"/arquivar").with(as(externo))).andExpect(status().isForbidden());
        mvc.perform(delete(url()).with(as(externo))).andExpect(status().isForbidden());
    }

    @Test void managerUpdatesMetadataWithoutChangingTeamWhenOmitted() throws Exception {
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO("Renomeado", " "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Renomeado"))
                .andExpect(jsonPath("$.equipe.length()").value(2));
        reload(); assertEquals("Renomeado", projetos.findById(projeto.getId()).orElseThrow().getNome());
    }

    @Test void projectUpdateCanReplaceTeamWhilePreservingCreator() throws Exception {
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO("Equipe nova", null, List.of(externo.getId())))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.equipe.length()").value(2));
        mvc.perform(get(url()).with(as(membro))).andExpect(status().isForbidden());
        mvc.perform(get(url()).with(as(externo))).andExpect(status().isOk());
    }

    @Test void archivePreventsEditingAndUnarchiveRestoresIt() throws Exception {
        mvc.perform(patch(url()+"/arquivar").with(as(gerente))).andExpect(status().isOk())
                .andExpect(jsonPath("$.arquivado").value(true));
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO("Bloqueado", null))))
                .andExpect(status().isConflict());
        mvc.perform(get(url()).with(as(membro))).andExpect(status().isOk());
        mvc.perform(patch(url()+"/desarquivar").with(as(gerente))).andExpect(status().isOk())
                .andExpect(jsonPath("$.arquivado").value(false));
    }

    @Test void managerDeletionCascadesTasks() throws Exception {
        Long taskId = tarefa.getId(), projectId = projeto.getId();
        mvc.perform(delete(url()).with(as(gerente))).andExpect(status().isNoContent());
        reload(); assertFalse(projetos.existsById(projectId)); assertFalse(tarefas.existsById(taskId));
    }

    @Test void missingProjectReturns404() throws Exception {
        mvc.perform(get("/api/projetos/"+Long.MAX_VALUE).with(as(gerente))).andExpect(status().isNotFound());
    }

    @Test void invalidProjectAndUnknownMemberReturn400() throws Exception {
        mvc.perform(post("/api/projetos").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO(" ", null))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").exists());
        mvc.perform(post("/api/projetos").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjetoRequestDTO("Novo", null, List.of(Long.MAX_VALUE)))))
                .andExpect(status().isBadRequest());
    }

    @Test void demotedCreatorLosesManagementEvenWithOldJwt() throws Exception {
        String token = jwt.generateToken(gerente);
        gerente.setRoles(new java.util.HashSet<>(java.util.Set.of(roles.findByNome("MEMBRO").orElseThrow())));
        usuarios.saveAndFlush(gerente);
        mvc.perform(patch(url()+"/arquivar").header("Authorization", "Bearer "+token))
                .andExpect(status().isForbidden());
        mvc.perform(get(url()).header("Authorization", "Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.podeGerenciar").value(false));
    }
}
