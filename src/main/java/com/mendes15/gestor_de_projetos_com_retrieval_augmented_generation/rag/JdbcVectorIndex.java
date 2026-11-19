package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component @RequiredArgsConstructor @Slf4j
public class JdbcVectorIndex implements VectorIndex {
    private final JdbcTemplate jdbc;
    private final RagProperties properties;
    private volatile boolean initialized;

    private void requireInitialized() {
        if (!initialized) throw new AiUnavailableException("Índice vetorial indisponível.");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) return;
        try {
            initialize();
        } catch (DataAccessException ex) {
            log.warn("Índice RAG indisponível. Verifique a extensão pgvector e as permissões do banco.");
        }
    }

    private synchronized void initialize() {
        if (initialized) return;
        if (!properties.isEnabled()) throw new AiUnavailableException("Índice vetorial desativado.");
        int dims = properties.getDimensions();
        if (dims<1 || dims>2000) throw new IllegalStateException("rag.dimensions deve estar entre 1 e 2000 para o índice HNSW.");
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("CREATE TABLE IF NOT EXISTS rag_vetores (trecho_id BIGINT PRIMARY KEY REFERENCES rag_trechos(id) ON DELETE CASCADE, modelo VARCHAR(255) NOT NULL, embedding vector("+dims+") NOT NULL)");
        String type = jdbc.queryForObject("SELECT format_type(a.atttypid,a.atttypmod) FROM pg_attribute a WHERE a.attrelid='rag_vetores'::regclass AND a.attname='embedding'", String.class);
        if (!("vector("+dims+")").equals(type)) throw new IllegalStateException("Dimensão do índice existente incompatível; reindexe antes de alterar rag.dimensions.");
        jdbc.execute("CREATE INDEX IF NOT EXISTS rag_vetores_hnsw ON rag_vetores USING hnsw (embedding vector_cosine_ops)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS rag_trechos_documento ON rag_trechos(documento_id)");
        initialized=true;
    }

    private String vector(double[] values) {
        if (values.length!=properties.getDimensions()) throw new AiUnavailableException("Dimensão de embedding incompatível.");
        double norm=0;
        for (double value : values) { if (!Double.isFinite(value)) throw new AiUnavailableException("Embedding inválido."); norm+=value*value; }
        if (norm==0) throw new AiUnavailableException("Embedding vazio.");
        return Arrays.toString(values);
    }

    @Override public void put(Long trechoId, double[] vector, String model) {
        requireInitialized();
        jdbc.update("INSERT INTO rag_vetores(trecho_id,modelo,embedding) VALUES (?,?,CAST(? AS vector)) ON CONFLICT (trecho_id) DO UPDATE SET embedding=EXCLUDED.embedding,modelo=EXCLUDED.modelo",
                trechoId, model, vector(vector));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Match> search(Long projetoId, Long usuarioId, double[] query, String model, int limit, double minimumScore) {
        requireInitialized();
        String vector = vector(query);
        // O filtro de projeto e de associação à equipe ocorre antes de retornar conteúdo.
        return jdbc.query("""
                SELECT v.trecho_id, 1-(v.embedding <=> CAST(? AS vector)) AS score
                FROM rag_vetores v JOIN rag_trechos t ON t.id=v.trecho_id
                JOIN rag_documentos d ON d.id=t.documento_id
                WHERE d.projeto_id=? AND v.modelo=?
                  AND EXISTS (SELECT 1 FROM projeto_usuarios pu WHERE pu.projeto_id=d.projeto_id AND pu.usuario_id=?)
                  AND 1-(v.embedding <=> CAST(? AS vector)) >= ?
                ORDER BY v.embedding <=> CAST(? AS vector) LIMIT ?
                """, (rs,n) -> new Match(rs.getLong("trecho_id"), rs.getDouble("score")),
                vector, projetoId, model, usuarioId, vector, minimumScore, vector, limit);
    }
}
