package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.GeminiGateway;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.LanguageModel;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.VectorIndex;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.DocumentoTrechoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:gestor_rag_tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class RagDocumentIntegrationTest extends BaseIntegrationTest {
    @MockBean GeminiGateway provider;
    @MockBean VectorIndex index;
    @Autowired DocumentoTrechoRepository trechos;

    @Test void indexesTextAndFiltersForeignChunksBeforeCallingModel() throws Exception {
        var foreignProject = project("Projeto externo", externo, administrador);
        projeto.getEquipe().add(administrador);
        projetos.saveAndFlush(projeto);
        when(provider.fingerprint()).thenReturn("teste:2");
        when(provider.embed(anyList(), anyBoolean())).thenReturn(List.of(new double[]{1, 0}));

        long document = ingest(projeto.getId(), "manual.md", "# Política de aprovação");
        long foreignDocument = ingest(foreignProject.getId(), "privado.txt", "Conteúdo exclusivo do outro projeto");
        long ownChunk = trechos.findByDocumentoIdOrderByOrdem(document).getFirst().getId();
        long foreignChunk = trechos.findByDocumentoIdOrderByOrdem(foreignDocument).getFirst().getId();
        verify(index).put(eq(ownChunk), any(double[].class), eq("teste:2"));
        when(index.search(eq(projeto.getId()), eq(membro.getId()), any(double[].class), eq("teste:2"),
                anyInt(), anyDouble())).thenReturn(List.of(new VectorIndex.Match(foreignChunk, 0.99),
                new VectorIndex.Match(ownChunk, 0.95)));
        when(provider.generate(anyString(), anyString(), anyString())).thenReturn(
                new LanguageModel.Generation("Encontrei a política de aprovação.", List.of("D" + ownChunk)),
                new LanguageModel.Generation("Fonte indevida", List.of("D" + foreignChunk)));

        String query = """
                {"pergunta":"Existe uma política de aprovação?"}
                """;
        mvc.perform(post("/api/ia/consultar").param("projetoId", projeto.getId().toString())
                        .with(as(membro)).contentType("application/json").content(query))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alternativa").value(false))
                .andExpect(jsonPath("$.fontes[0].id").value("D" + ownChunk));
        verify(provider).generate(anyString(), argThat(context ->
                context.contains("Política de aprovação") && !context.contains("Conteúdo exclusivo")), anyString());

        mvc.perform(post("/api/ia/consultar").param("projetoId", projeto.getId().toString())
                        .with(as(membro)).contentType("application/json").content(query))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alternativa").value(true));
        mvc.perform(get("/api/projetos/{id}/documentos/{doc}/trechos", projeto.getId(), foreignDocument)
                        .with(as(membro)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/projetos/{id}/documentos/{doc}", projeto.getId(), document)
                        .with(as(administrador)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/projetos/{id}/documentos", projeto.getId()).with(as(membro)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private long ingest(Long projetoId, String name, String content) throws Exception {
        var file = new MockMultipartFile("arquivo", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
        String result = mvc.perform(multipart("/api/admin/projetos/{id}/documentos", projetoId)
                        .file(file).with(as(administrador)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.trechos").value(1))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(result).path("id").asLong();
    }
}
