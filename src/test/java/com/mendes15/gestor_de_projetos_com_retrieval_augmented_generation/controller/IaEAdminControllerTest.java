package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IaEAdminControllerTest extends BaseIntegrationTest {
    @Test void reservedAdminNamespaceRejectsMembersAndManagers() throws Exception {
        mvc.perform(post("/api/admin/regras-dominio").with(as(membro))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/regras-dominio").with(as(gerente))).andExpect(status().isForbidden());
    }

    @Test void adminAuthorizationDoesNotCreateUnimplementedEndpoints() throws Exception {
        mvc.perform(post("/api/admin/regras-dominio").with(as(administrador))).andExpect(status().isNotFound());
        mvc.perform(get("/api/ia/consultar").with(as(membro))).andExpect(status().isNotFound());
    }
}
