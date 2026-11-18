package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.UsuarioRegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioControllerTest extends BaseIntegrationTest {
    String url(Long id) { return "/api/usuarios/"+id; }
    String profile(String email) throws Exception {
        return json(new UsuarioRegisterRequestDTO("Perfil Atualizado", email, "NovaSenha123!"));
    }

    @Test void onlyManagersAndAdminsMayListDirectory() throws Exception {
        mvc.perform(get("/api/usuarios").with(as(gerente))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4)).andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(get("/api/usuarios").with(as(administrador))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").with(as(membro))).andExpect(status().isForbidden());
    }

    @Test void ownProfileAndAdminCanReadButOtherMemberCannot() throws Exception {
        mvc.perform(get(url(membro.getId())).with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis[0]").value("ROLE_MEMBRO"));
        mvc.perform(get(url(membro.getId())).with(as(administrador))).andExpect(status().isOk());
        mvc.perform(get(url(gerente.getId())).with(as(membro))).andExpect(status().isForbidden());
    }

    @Test void selfUpdatePersistsHashAndDoesNotChangeRoles() throws Exception {
        mvc.perform(put(url(membro.getId())).with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content(profile(membro.getEmail()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Perfil Atualizado"))
                .andExpect(jsonPath("$.password").doesNotExist());
        reload(); var saved = usuarios.findById(membro.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NovaSenha123!", saved.getPassword()));
        assertEquals("MEMBRO", saved.getRoles().iterator().next().getNome());
    }

    @Test void cannotEditSomeoneElse() throws Exception {
        mvc.perform(put(url(gerente.getId())).with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content(profile(gerente.getEmail()))).andExpect(status().isForbidden());
    }

    @Test void administratorCanUpdateOthers() throws Exception {
        mvc.perform(put(url(membro.getId())).with(as(administrador)).contentType(MediaType.APPLICATION_JSON)
                        .content(profile(membro.getEmail()))).andExpect(status().isOk());
    }

    @Test void administratorCreatesMemberWithHashedPassword() throws Exception {
        String response = mvc.perform(post("/api/usuarios").with(as(administrador)).contentType(MediaType.APPLICATION_JSON)
                        .content(profile("novo@example.com"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.papeis[0]").value("ROLE_MEMBRO"))
                .andReturn().getResponse().getContentAsString();
        reload(); var saved = usuarios.findById(objectMapper.readTree(response).get("id").asLong()).orElseThrow();
        assertTrue(passwordEncoder.matches("NovaSenha123!", saved.getPassword()));
    }

    @Test void managerCannotCreateOrDeleteUsers() throws Exception {
        mvc.perform(post("/api/usuarios").with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(profile("novo@example.com"))).andExpect(status().isForbidden());
        mvc.perform(delete(url(externo.getId())).with(as(gerente))).andExpect(status().isForbidden());
    }

    @Test void administratorCanDeleteUnlinkedUser() throws Exception {
        mvc.perform(delete(url(externo.getId())).with(as(administrador))).andExpect(status().isNoContent());
        reload(); assertFalse(usuarios.existsById(externo.getId()));
    }

    @Test void adminAssignmentRetainsMemberAndTakesEffectWithExistingJwt() throws Exception {
        String token = jwt.generateToken(membro);
        mvc.perform(post("/api/projetos").header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Antes\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(url(membro.getId())+"/papeis").with(as(administrador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ROLE_GERENTE\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.papeis.length()").value(2));
        reload();
        assertEquals(Set.of("MEMBRO","GERENTE"), usuarios.findById(membro.getId()).orElseThrow().getRoles().stream()
                .map(r -> r.getNome()).collect(java.util.stream.Collectors.toSet()));
        mvc.perform(post("/api/projetos").header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Depois\"}"))
                .andExpect(status().isCreated());
    }

    @Test void memberCannotPromoteSelf() throws Exception {
        mvc.perform(put(url(membro.getId())+"/papeis").with(as(membro)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ROLE_ADMINISTRADOR\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test void emptyNullAndUnknownRolesAreRejected() throws Exception {
        for (String request : new String[]{"{}", "{\"papeis\":[]}", "{\"papeis\":[null]}", "{\"papeis\":[\"ROOT\"]}"})
            mvc.perform(put(url(membro.getId())+"/papeis").with(as(administrador)).contentType(MediaType.APPLICATION_JSON)
                            .content(request)).andExpect(status().isBadRequest());
    }

    @Test void missingUserReturns404ForAdministrator() throws Exception {
        mvc.perform(get(url(Long.MAX_VALUE)).with(as(administrador))).andExpect(status().isNotFound());
        mvc.perform(put(url(Long.MAX_VALUE)+"/papeis").with(as(administrador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ROLE_MEMBRO\"]}"))
                .andExpect(status().isNotFound());
    }
}
