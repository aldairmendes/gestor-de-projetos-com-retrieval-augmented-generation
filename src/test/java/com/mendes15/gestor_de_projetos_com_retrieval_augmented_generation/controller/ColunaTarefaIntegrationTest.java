package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;




class ColunaTarefaIntegrationTest extends BaseIntegrationTest {

    private Long quadroId;
    private Long aFazer;
    private Long emRevisao;
    private Long concluido;

    @BeforeEach
    void criarQuadro() throws Exception {
        JsonNode quadro = criarQuadro(projeto, List.of(
                Map.of("nome", "A fazer", "status", "PENDENTE"),
                Map.of("nome", "Em revisão"),
                Map.of("nome", "Concluído", "status", "CONCLUIDA")));
        quadroId = quadro.get("id").asLong();
        aFazer = quadro.at("/colunas/0/id").asLong();
        emRevisao = quadro.at("/colunas/1/id").asLong();
        concluido = quadro.at("/colunas/2/id").asLong();
    }

    private JsonNode criarQuadro(Projeto alvo, List<Map<String, String>> colunas) throws Exception {
        String body = mvc.perform(post("/api/projetos/" + alvo.getId() + "/quadros").with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Principal", "colunas", colunas))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String novaTarefa(Long colunaId) throws Exception {
        var body = new HashMap<String, Object>(Map.of(
                "titulo", "Revisar API", "status", "PENDENTE", "prioridade", "ALTA", "projetoId", projeto.getId()));
        if (colunaId != null) body.put("colunaId", colunaId);
        return json(body);
    }

    @Test
    void quadroDevolveOStatusVinculadoDeCadaColuna() throws Exception {
        mvc.perform(get("/api/projetos/" + projeto.getId() + "/quadros/" + quadroId).with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunas[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.colunas[1].status").doesNotExist())
                .andExpect(jsonPath("$.colunas[2].status").value("CONCLUIDA"));
    }

    @Test
    void criarNaColunaAplicaOStatusVinculado() throws Exception {
        mvc.perform(post("/api/tarefas").with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON).content(novaTarefa(concluido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.colunaId").value(concluido))
                .andExpect(jsonPath("$.quadroId").value(quadroId))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void tarefaSemColunaContinuaFuncionandoComoAntes() throws Exception {
        mvc.perform(post("/api/tarefas").with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON).content(novaTarefa(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.colunaId").doesNotExist())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void membroMoveEntreColunasERegistraNoHistorico() throws Exception {
        String url = "/api/tarefas/" + tarefa.getId() + "/coluna";
        mvc.perform(patch(url).param("colunaId", emRevisao.toString()).with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunaId").value(emRevisao))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
        mvc.perform(patch(url).param("colunaId", concluido.toString()).with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
        mvc.perform(get("/api/tarefas/" + tarefa.getId() + "/historico").with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.acao == 'COLUNA')]").isNotEmpty());
    }

    @Test
    void mudarStatusLevaParaAColunaVinculadaAoNovoStatus() throws Exception {
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", concluido.toString()).with(as(membro)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/status").param("novoStatus", "PENDENTE").with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunaId").value(aFazer));
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/status").param("novoStatus", "CANCELADA").with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunaId").doesNotExist());
    }

    @Test
    void colunaSemStatusVinculadoNaoMudaComOStatus() throws Exception {
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", emRevisao.toString()).with(as(membro)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/status").param("novoStatus", "EM_ANDAMENTO").with(as(membro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunaId").value(emRevisao));
    }

    @Test
    void editarSemColunaIdMantemAColuna() throws Exception {
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", emRevisao.toString()).with(as(gerente)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/tarefas/" + tarefa.getId()).with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON).content(novaTarefa(null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Revisar API"))
                .andExpect(jsonPath("$.colunaId").value(emRevisao));
    }

    @Test
    void colunaDeOutroProjetoERecusada() throws Exception {
        Projeto outro = project("Outro", gerente);
        Long colunaAlheia = criarQuadro(outro, List.of(Map.of("nome", "Fila"))).at("/colunas/0/id").asLong();
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", colunaAlheia.toString()).with(as(gerente)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tarefas").with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON).content(novaTarefa(colunaAlheia)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", "999999").with(as(gerente)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void foraDaEquipeOuProjetoArquivadoNaoMove() throws Exception {
        String url = "/api/tarefas/" + tarefa.getId() + "/coluna";
        mvc.perform(patch(url).param("colunaId", emRevisao.toString()).with(as(externo)))
                .andExpect(status().isForbidden());
        projeto.setArquivado(true);
        projetos.saveAndFlush(projeto);
        mvc.perform(patch(url).param("colunaId", emRevisao.toString()).with(as(membro)))
                .andExpect(status().isConflict());
    }

    @Test
    void removerColunaComTarefasExigeRealocacao() throws Exception {
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", emRevisao.toString()).with(as(gerente)))
                .andExpect(status().isOk());
        var semRevisao = List.of(
                Map.of("id", aFazer, "nome", "A fazer", "status", "PENDENTE"),
                Map.of("id", concluido, "nome", "Concluído", "status", "CONCLUIDA"));
        String url = "/api/projetos/" + projeto.getId() + "/quadros/" + quadroId;

        mvc.perform(put(url).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Principal", "colunas", semRevisao))))
                .andExpect(status().isConflict());
        reload();
        assertEquals(emRevisao, tarefas.findById(tarefa.getId()).orElseThrow().getColuna().getId());

        mvc.perform(put(url).with(as(gerente)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Principal", "colunas", semRevisao,
                                "realocacoes", Map.of(emRevisao.toString(), concluido)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunas.length()").value(2));
        reload();
        Tarefa movida = tarefas.findById(tarefa.getId()).orElseThrow();
        assertEquals(concluido, movida.getColuna().getId());
        assertEquals(StatusTarefa.CONCLUIDA, movida.getStatus());
    }

    @Test
    void realocacaoParaColunaRemovidaERecusada() throws Exception {
        mvc.perform(patch("/api/tarefas/" + tarefa.getId() + "/coluna").param("colunaId", emRevisao.toString()).with(as(gerente)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/projetos/" + projeto.getId() + "/quadros/" + quadroId).with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Principal",
                                "colunas", List.of(Map.of("id", aFazer, "nome", "A fazer")),
                                "realocacoes", Map.of(emRevisao.toString(), concluido)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removerColunaVaziaNaoExigeRealocacao() throws Exception {
        mvc.perform(put("/api/projetos/" + projeto.getId() + "/quadros/" + quadroId).with(as(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Principal", "colunas", List.of(Map.of("id", aFazer, "nome", "A fazer"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunas[0].status").doesNotExist());
        reload();
        assertNull(tarefas.findById(tarefa.getId()).orElseThrow().getColuna());
    }
}
