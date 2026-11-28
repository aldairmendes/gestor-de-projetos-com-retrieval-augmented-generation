package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers(disabledWithoutDocker = true)
class TarefaColaboracaoPostgresTest extends BaseIntegrationTest {

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

    private long novaTarefa(Projeto projeto, String titulo) {
        Tarefa nova = new Tarefa();
        nova.setTitulo(titulo);
        nova.setProjeto(projeto);
        return tarefas.saveAndFlush(nova).getId();
    }

    private String url(long id, String recurso) {
        return "/api/tarefas/" + id + "/" + recurso;
    }

    @Test
    void membroComentaETodaEquipeLeComentariosPersistidos() throws Exception {
        String url = url(tarefa.getId(), "comentarios");
        mvc.perform(post(url).with(as(membro)).contentType(APPLICATION_JSON)
                        .content("{\"texto\":\"  Revisar os critérios.  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorId").value(membro.getId()))
                .andExpect(jsonPath("$.texto").value("Revisar os critérios."));
        reload();
        mvc.perform(get(url).with(as(gerente))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Revisar os critérios."));
        mvc.perform(get(url).with(as(externo))).andExpect(status().isForbidden());
        mvc.perform(post(url).with(as(membro)).contentType(APPLICATION_JSON).content("{\"texto\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void membroAnexaArquivoComDownloadRestritoETarefaCorreta() throws Exception {
        String url = url(tarefa.getId(), "anexos");
        byte[] bytes = {0, 12, -1, 42};
        var response = mvc.perform(multipart(url).file(new MockMultipartFile("arquivo", "evidencia.bin",
                        "text/html", bytes)).with(as(membro))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("evidencia.bin"))
                .andExpect(jsonPath("$.tamanho").value(bytes.length))
                .andExpect(jsonPath("$.conteudo").doesNotExist()).andReturn().getResponse();
        long anexo = objectMapper.readTree(response.getContentAsByteArray()).get("id").asLong();
        reload();
        mvc.perform(get(url).with(as(membro))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        var download = mvc.perform(get(url + "/" + anexo).with(as(gerente))).andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff")).andReturn().getResponse();
        assertArrayEquals(bytes, download.getContentAsByteArray());
        mvc.perform(get(url + "/" + anexo).with(as(externo))).andExpect(status().isForbidden());
        mvc.perform(get(url(novaTarefa(projeto, "Outra"), "anexos") + "/" + anexo).with(as(gerente)))
                .andExpect(status().isNotFound());
        mvc.perform(delete(url + "/" + anexo).with(as(gerente))).andExpect(status().isNoContent());
        mvc.perform(get(url + "/" + anexo).with(as(membro))).andExpect(status().isNotFound());
    }

    @Test
    void gerenteRegistraDependenciasSemDuplicatasOuCiclos() throws Exception {
        long a = tarefa.getId();
        long b = novaTarefa(projeto, "B");
        long c = novaTarefa(projeto, "C");
        String dependenciasA = url(a, "dependencias");
        salvarDependencia(a, b);
        salvarDependencia(b, c);
        salvarDependencia(a, b);
        reload();
        mvc.perform(get(dependenciasA).with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(b));
        mvc.perform(put(url(c, "dependencias") + "/" + a).with(as(gerente))).andExpect(status().isConflict());
        mvc.perform(put(dependenciasA + "/" + a).with(as(gerente))).andExpect(status().isBadRequest());
        var outroProjeto = project("Outro", gerente);
        long externa = novaTarefa(outroProjeto, "Externa");
        mvc.perform(put(dependenciasA + "/" + externa).with(as(gerente))).andExpect(status().isNotFound());
        mvc.perform(put(dependenciasA + "/" + c).with(as(membro))).andExpect(status().isForbidden());
        mvc.perform(delete(dependenciasA + "/" + b).with(as(gerente))).andExpect(status().isNoContent());
        mvc.perform(get(dependenciasA).with(as(membro))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void projetoArquivadoPermiteLeituraEImpedeAlteracoes() throws Exception {
        long id = tarefa.getId();
        long prerequisito = novaTarefa(projeto, "Pré-requisito");
        projeto.setArquivado(true);
        projetos.saveAndFlush(projeto);
        mvc.perform(post(url(id, "comentarios")).with(as(membro)).contentType(APPLICATION_JSON)
                        .content("{\"texto\":\"Comentário\"}")).andExpect(status().isConflict());
        mvc.perform(multipart(url(id, "anexos")).file("arquivo", new byte[]{1}).with(as(membro)))
                .andExpect(status().isConflict());
        mvc.perform(put(url(id, "dependencias") + "/" + prerequisito).with(as(gerente)))
                .andExpect(status().isConflict());
        mvc.perform(get(url(id, "comentarios")).with(as(membro))).andExpect(status().isOk());
    }

    @Test
    void impedeTransferirTarefaVinculadaEAExclusaoRemoveDependencias() throws Exception {
        long id = tarefa.getId();
        long prerequisito = novaTarefa(projeto, "Pré-requisito");
        salvarDependencia(id, prerequisito);
        var destino = project("Destino", gerente);
        String transferencia = """
                {"titulo":"Pré-requisito","status":"PENDENTE","prioridade":"MEDIA","projetoId":%d}
                """.formatted(destino.getId());
        mvc.perform(put("/api/tarefas/" + prerequisito).with(as(gerente)).contentType(APPLICATION_JSON)
                        .content(transferencia)).andExpect(status().isConflict());

        mvc.perform(delete("/api/tarefas/" + prerequisito).with(as(gerente))).andExpect(status().isNoContent());
        reload();
        mvc.perform(get(url(id, "dependencias")).with(as(membro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejeitaArquivoVazioNomeInvalidoEArquivoAcimaDoLimite() throws Exception {
        String url = url(tarefa.getId(), "anexos");
        mvc.perform(multipart(url).file(new MockMultipartFile("arquivo", "vazio.txt", "text/plain",
                        new byte[0])).with(as(membro))).andExpect(status().isBadRequest());
        mvc.perform(multipart(url).file(new MockMultipartFile("arquivo", "../teste.txt", "text/plain",
                        new byte[]{1})).with(as(membro))).andExpect(status().isBadRequest());
        mvc.perform(multipart(url).file(new MockMultipartFile("arquivo", "grande.bin", "application/octet-stream",
                        new byte[5 * 1024 * 1024 + 1])).with(as(membro))).andExpect(status().isPayloadTooLarge());
        mvc.perform(get(url).with(as(membro))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private void salvarDependencia(long tarefaId, long prerequisito) throws Exception {
        mvc.perform(put(url(tarefaId, "dependencias") + "/" + prerequisito).with(as(gerente)))
                .andExpect(status().isNoContent());
    }
}
