package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = {SecurityConfig.class, HttpRoleAccessTest.Config.class})
class HttpRoleAccessTest {
    @Configuration @EnableWebMvc
    static class Config {
        @Bean JwtAuthenticationFilter filter() {
            return new JwtAuthenticationFilter(mock(JwtService.class), mock(UserDetailsService.class));
        }
        @Bean AuthenticationProvider provider() { return mock(AuthenticationProvider.class); }
        @Bean Probe probe() { return new Probe(); }
    }

    @RestController static class Probe {
        @RequestMapping("/api/**") String ok() { return "ok"; }
    }

    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @CsvSource({"MEMBRO,200", "ADMINISTRADOR,200"})
    void authenticatedRolesCanReachProjectCreation(String role, int expected) throws Exception {
        mvc.perform(post("/api/projetos").with(user("user").roles(role)))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @CsvSource({"MEMBRO,403", "ADMINISTRADOR,200"})
    void onlyAdminChangesRoles(String role, int expected) throws Exception {
        mvc.perform(put("/api/usuarios/1/papeis").with(user("user").roles(role)))
                .andExpect(status().is(expected));
        mvc.perform(post("/api/admin/regras-dominio").with(user("user").roles(role)))
                .andExpect(status().is(expected));
    }

    @Test void unauthenticatedGets401() throws Exception {
        mvc.perform(get("/api/tarefas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/projetos")).andExpect(status().isUnauthorized());
    }

    @Test void registrationIsPublic() throws Exception {
        mvc.perform(post("/api/v1/auth/register")).andExpect(status().isOk());
    }

    @Test void authenticatedMemberReachesTaskEndpointsForProjectAuthorization() throws Exception {
        mvc.perform(patch("/api/tarefas/10/status").with(user("user").roles("MEMBRO")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/tarefas/10").with(user("user").roles("MEMBRO")))
                .andExpect(status().isOk());
    }
}
