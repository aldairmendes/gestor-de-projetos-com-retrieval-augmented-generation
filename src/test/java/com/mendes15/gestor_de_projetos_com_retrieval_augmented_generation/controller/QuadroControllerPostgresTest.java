package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
class QuadroControllerPostgresTest extends BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    private String url() {
        return "/api/projetos/" + projeto.getId() + "/quadros";
    }

    private long criarQuadro() throws Exception {
        var response = mvc.perform(post(url()).with(as(gerente)).contentType(APPLICATION_JSON).content("""
                {"nome":"Entrega","colunas":[{"nome":"A fazer"},{"nome":"Em andamento"},{"nome":"Concluído"}]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Entrega"))
                .andExpect(jsonPath("$.colunas[0].id").isNumber())
                .andExpect(jsonPath("$.colunas[2].nome").value("Concluído"))
                .andReturn().getResponse();
        return objectMapper.readTree(response.getContentAsByteArray()).get("id").asLong();
    }

    @Test
    void criaMaisDeUmQuadroEConsultaColunasNaOrdemConfigurada() throws Exception {
        long primeiro = criarQuadro();
        long segundo = criarQuadro();
        reload();

        mvc.perform(get(url()).with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(primeiro))
                .andExpect(jsonPath("$[1].id").value(segundo))
                .andExpect(jsonPath("$[0].colunas[0].nome").value("A fazer"))
                .andExpect(jsonPath("$[0].colunas[1].nome").value("Em andamento"))
                .andExpect(jsonPath("$[0].colunas[2].nome").value("Concluído"));
    }

    @Test
    void renomeiaReordenaAdicionaERemoveColunasPreservandoOsIdsMantidos() throws Exception {
        String quadro = url() + "/" + criarQuadro();
        var response = mvc.perform(get(quadro).with(as(gerente))).andReturn().getResponse();
        var colunas = objectMapper.readTree(response.getContentAsByteArray()).get("colunas");
        long primeira = colunas.get(0).get("id").asLong();
        long terceira = colunas.get(2).get("id").asLong();

        mvc.perform(put(quadro).with(as(gerente)).contentType(APPLICATION_JSON).content("""
                {"nome":"Entrega 2","colunas":[{"id":%d,"nome":"Pronto"},{"id":%d,"nome":"A fazer"},{"nome":"Revisão"}]}
                """.formatted(terceira, primeira))).andExpect(status().isOk());
        reload();

        mvc.perform(get(quadro).with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Entrega 2"))
                .andExpect(jsonPath("$.colunas.length()").value(3))
                .andExpect(jsonPath("$.colunas[0].id").value(terceira))
                .andExpect(jsonPath("$.colunas[0].nome").value("Pronto"))
                .andExpect(jsonPath("$.colunas[1].id").value(primeira))
                .andExpect(jsonPath("$.colunas[2].nome").value("Revisão"));
    }

    @Test
    void restringeAlteracoesAoGerenteDoProjetoEProtegeProjetoArquivado() throws Exception {
        long quadroId = criarQuadro();
        String quadro = url() + "/" + quadroId;
        String body = """
                {"nome":"Outro","colunas":[{"nome":"A fazer"}]}
                """;
        mvc.perform(post(url()).with(as(membro)).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get(quadro).with(as(externo))).andExpect(status().isForbidden());
        var outro = project("Outro projeto", gerente);
        mvc.perform(get("/api/projetos/" + outro.getId() + "/quadros/" + quadroId)
                        .with(as(gerente))).andExpect(status().isNotFound());

        projeto.setArquivado(true);
        projetos.saveAndFlush(projeto);
        mvc.perform(put(quadro).with(as(gerente)).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(get(quadro).with(as(membro))).andExpect(status().isOk());
    }

    @Test
    void rejeitaConfiguracaoInvalidaSemAlterarQuadro() throws Exception {
        String quadro = url() + "/" + criarQuadro();
        mvc.perform(put(quadro).with(as(gerente)).contentType(APPLICATION_JSON).content("""
                {"nome":"Inválido","colunas":[{"nome":"Revisão"},{"nome":" revisão "}]}
                """)).andExpect(status().isBadRequest());
        mvc.perform(put(quadro).with(as(gerente)).contentType(APPLICATION_JSON).content("""
                {"nome":"Inválido","colunas":[{"id":9223372036854775807,"nome":"Revisão"}]}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post(url()).with(as(gerente)).contentType(APPLICATION_JSON).content("""
                {"nome":" ","colunas":[null]}
                """)).andExpect(status().isBadRequest());
        mvc.perform(get(quadro).with(as(gerente)))
                .andExpect(jsonPath("$.nome").value("Entrega"))
                .andExpect(jsonPath("$.colunas.length()").value(3));
    }
}
