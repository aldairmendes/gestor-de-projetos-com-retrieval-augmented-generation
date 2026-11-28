package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EquipeControllerIntegrationTest extends BaseIntegrationTest {
    String url() { return "/api/projetos/" + projeto.getId() + "/equipe"; }
    String body(Long... ids) {
        return "{\"membrosIds\":" + List.of(ids) + "}";
    }

    @Test void managerDefinesTeamAndKeepsCreator() throws Exception {
        mvc.perform(put(url()).with(user(gerente.getEmail()).roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(externo.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        assertEquals(Set.of(gerente.getId(), externo.getId()), projeto.getEquipe().stream()
                .map(Usuario::getId).collect(Collectors.toSet()));
        assertEquals("Projeto", projeto.getNome());
    }

    @Test void emptyTeamRetainsCreator() throws Exception {
        mvc.perform(put(url()).with(user(gerente.getEmail()).roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(gerente.getId()));
    }

    @Test void additionsAreIdempotentAndGrantProjectAccess() throws Exception {
        String request = body(externo.getId(), externo.getId(), membro.getId());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(url() + "/membros").with(user(gerente.getEmail()).roles("GERENTE"))
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        }
        mvc.perform(get("/api/projetos/" + projeto.getId()).with(user(externo.getEmail()).roles("GERENTE")))
                .andExpect(status().isOk());
        assertEquals(1, externo.getRoles().size());
    }

    @Test void removedMemberLosesProjectAndTaskAccess() throws Exception {
        Tarefa tarefa = new Tarefa(); tarefa.setTitulo("Tarefa"); tarefa.setProjeto(projeto);
        tarefa = tarefas.saveAndFlush(tarefa);
        mvc.perform(delete(url() + "/membros/" + membro.getId())
                        .with(user(gerente.getEmail()).roles("GERENTE"))).andExpect(status().isNoContent());
        mvc.perform(get("/api/projetos/" + projeto.getId()).with(user(membro.getEmail()).roles("MEMBRO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tarefas/" + tarefa.getId()).with(user(membro.getEmail()).roles("MEMBRO")))
                .andExpect(status().isForbidden());
        assertTrue(usuarios.existsById(membro.getId()));
    }

    @Test void memberCanListButCannotAdd() throws Exception {
        mvc.perform(get(url()).with(user(membro.getEmail()).roles("MEMBRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(post(url() + "/membros").with(user(membro.getEmail()).roles("MEMBRO"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(externo.getId())))
                .andExpect(status().isForbidden());
    }

    @Test void outsidersCannotReadOrManageTeams() throws Exception {
        mvc.perform(get(url()).with(user(externo.getEmail()).roles("GERENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post(url() + "/membros").with(user(externo.getEmail()).roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(externo.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get(url()).with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test void unknownUserRejectsEntireAddition() throws Exception {
        mvc.perform(post(url() + "/membros").with(user(gerente.getEmail()).roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(externo.getId(), Long.MAX_VALUE)))
                .andExpect(status().isBadRequest());
        assertEquals(2, projeto.getEquipe().size());
    }

    @Test void invalidIdsAreRejected() throws Exception {
        for (String request : List.of("{}", "{\"membrosIds\":null}", "{\"membrosIds\":[null]}", "{\"membrosIds\":[0]}", "{\"membrosIds\":[-1]}")) {
            mvc.perform(put(url()).with(user(gerente.getEmail()).roles("GERENTE"))
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(2, projeto.getEquipe().size());
    }

    @Test void creatorCannotBeRemoved() throws Exception {
        mvc.perform(delete(url() + "/membros/" + gerente.getId())
                        .with(user(gerente.getEmail()).roles("GERENTE"))).andExpect(status().isConflict());
    }

    @Test void nonMemberRemovalReturns404() throws Exception {
        mvc.perform(delete(url() + "/membros/" + externo.getId())
                        .with(user(gerente.getEmail()).roles("GERENTE"))).andExpect(status().isNotFound());
    }

    @Test void archivedProjectsAllowReadingButRejectTeamChanges() throws Exception {
        projeto.setArquivado(true); projetos.saveAndFlush(projeto);
        mvc.perform(get(url()).with(user(membro.getEmail()).roles("MEMBRO"))).andExpect(status().isOk());
        mvc.perform(put(url()).with(user(gerente.getEmail()).roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(externo.getId())))
                .andExpect(status().isConflict());
        mvc.perform(post(url() + "/membros").with(user(gerente.getEmail()).roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(externo.getId())))
                .andExpect(status().isConflict());
        mvc.perform(delete(url() + "/membros/" + membro.getId())
                        .with(user(gerente.getEmail()).roles("GERENTE"))).andExpect(status().isConflict());
    }

    @Test void authenticationIsRequired() throws Exception {
        mvc.perform(get(url())).andExpect(status().isUnauthorized());
    }

    @Test void definingTeamPersistsMembershipAndRevokesRemovedUser() throws Exception {
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(externo.getId())))
                .andExpect(status().isOk());
        reload();
        assertEquals(Set.of(gerente.getId(), externo.getId()),
                projetos.findById(projeto.getId()).orElseThrow().getEquipe().stream()
                        .map(usuario -> usuario.getId()).collect(Collectors.toSet()));
        mvc.perform(get(url()).with(as(membro))).andExpect(status().isForbidden());
        mvc.perform(get(url()).with(as(externo))).andExpect(status().isOk());
    }

    @Test void replacementWithUnknownUserDoesNotClearExistingTeam() throws Exception {
        mvc.perform(put(url()).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Long.MAX_VALUE)))
                .andExpect(status().isBadRequest());
        assertEquals(2, projeto.getEquipe().size());
    }

    @Test void nonCreatorManagerCanLeaveAndLosesAccess() throws Exception {
        projeto.getEquipe().add(externo); projetos.saveAndFlush(projeto);
        mvc.perform(delete(url()+"/membros/"+externo.getId()).with(as(externo)))
                .andExpect(status().isNoContent());
        mvc.perform(get(url()).with(as(externo))).andExpect(status().isForbidden());
    }

    @Test void unknownProjectReturns404() throws Exception {
        mvc.perform(get("/api/projetos/"+Long.MAX_VALUE+"/equipe").with(as(gerente)))
                .andExpect(status().isNotFound());
    }
}
