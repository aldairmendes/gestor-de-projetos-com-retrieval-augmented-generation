package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {
    @Value("${application.security.jwt.secret-key}") String secret;

    String register(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UsuarioRegisterRequestDTO("novo_usuario", email, PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test void registrationHashesPasswordAssignsMemberAndReturnsUsableJwt() throws Exception {
        String token = register("novo@example.com");
        Usuario novo = usuarios.findByEmail("novo@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches(PASSWORD, novo.getPassword()));
        assertEquals("novo@example.com", jwt.extractUsername(token));
        assertEquals(List.of("MEMBRO"), jwt.extractClaim(token, c -> c.get("roles")));
        mvc.perform(get("/api/usuarios/" + novo.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("novo_usuario"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test void duplicateEmailReturns409() throws Exception {
        register("novo@example.com");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UsuarioRegisterRequestDTO("outronome", "novo@example.com", PASSWORD))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test void loginReturnsUsableToken() throws Exception {
        String token = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UsuarioLoginRequestDTO(membro.getEmail(), PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/projetos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(projeto.getId()));
    }

    @Test void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UsuarioLoginRequestDTO(membro.getEmail(), "incorreta"))))
                .andExpect(status().isUnauthorized());
    }

    @Test void unknownLoginReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UsuarioLoginRequestDTO("ausente@example.com", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test void registrationValidatesFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"email\":\"invalido\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.username").exists())
                .andExpect(jsonPath("$.erros.email").exists()).andExpect(jsonPath("$.erros.password").exists());
    }

    @Test void missingLoginAndMalformedBodyReturn400() throws Exception {
        for (String content : new String[]{"{}", "{"})
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(content))
                    .andExpect(status().isBadRequest());
    }

    @Test void invalidAndExpiredTokensReturn401() throws Exception {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        String expired = Jwts.builder().subject(membro.getEmail()).expiration(new Date(0)).signWith(key).compact();
        String forged = Jwts.builder().subject(membro.getEmail())
                .signWith(Keys.hmacShaKeyFor(new byte[64])).compact();
        for (String token : new String[]{"invalido", expired, forged})
            mvc.perform(get("/api/projetos").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
    }

    @Test void deletedAccountCannotReuseJwt() throws Exception {
        String token = jwt.generateToken(externo);
        usuarios.delete(externo); usuarios.flush();
        mvc.perform(get("/api/projetos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void registrationCannotInjectAdminRole() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"intruso\",\"email\":\"intruso@example.com\",\"password\":\"Senha123!\",\"roles\":[\"ADMINISTRADOR\"]}"))
                .andExpect(status().isOk());
        var user = usuarios.findByEmail("intruso@example.com").orElseThrow();
        assertEquals(Set.of("MEMBRO"), user.getRoles().stream()
                .map(r -> r.getNome()).collect(Collectors.toSet()));
    }
}
