package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.QuadroDto.ColunaConfiguracao;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.QuadroDto.ColunaResposta;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.QuadroDto.Configuracao;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.QuadroDto.QuadroResposta;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.ColunaQuadro;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Quadro;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.QuadroRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuadroService {
    private final ProjetoService projetos;
    private final AccessPolicy access;
    private final QuadroRepository quadros;
    private final TarefaRepository tarefas;

    public List<QuadroResposta> listar(Long projetoId) {
        projetos.findById(projetoId);
        return quadros.findByProjetoIdOrderByIdAsc(projetoId).stream().map(this::resposta).toList();
    }

    public QuadroResposta consultar(Long projetoId, Long quadroId) {
        projetos.findById(projetoId);
        return resposta(quadros.findByIdAndProjetoId(quadroId, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Quadro não encontrado neste projeto.")));
    }

    @Transactional
    public QuadroResposta criar(Long projetoId, Configuracao configuracao) {
        Projeto projeto = projetoEditavel(projetoId);
        Quadro quadro = new Quadro();
        quadro.setProjeto(projeto);
        configurar(quadro, configuracao);
        return resposta(quadros.saveAndFlush(quadro));
    }

    @Transactional
    public QuadroResposta atualizar(Long projetoId, Long quadroId, Configuracao configuracao) {
        projetoEditavel(projetoId);
        Quadro quadro = quadros.findForUpdateByIdAndProjetoId(quadroId, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Quadro não encontrado neste projeto."));
        configurar(quadro, configuracao);
        return resposta(quadros.saveAndFlush(quadro));
    }

    private Projeto projetoEditavel(Long projetoId) {
        Projeto projeto = projetos.findById(projetoId);
        access.requireManage(projeto);
        if (projeto.isArquivado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O projeto está arquivado.");
        }
        return projeto;
    }

    private void configurar(Quadro quadro, Configuracao configuracao) {
        var existentes = quadro.getColunas().stream()
                .collect(Collectors.toMap(ColunaQuadro::getId, Function.identity()));
        var nomes = new HashSet<String>();
        var idsMantidos = new HashSet<Long>();
        for (var coluna : configuracao.colunas()) {
            if (!nomes.add(coluna.nome().strip().toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de coluna duplicado.");
            }
            if (coluna.id() != null && (!existentes.containsKey(coluna.id()) || !idsMantidos.add(coluna.id()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de coluna inválido ou repetido neste quadro.");
            }
        }

        realocarTarefas(existentes, idsMantidos, configuracao);
        quadro.setNome(configuracao.nome().strip());
        quadro.getColunas().removeIf(coluna -> !idsMantidos.contains(coluna.getId()));
        int posicao = 0;
        for (var item : configuracao.colunas()) {
            ColunaQuadro coluna = existentes.get(item.id());
            if (coluna == null) {
                coluna = new ColunaQuadro();
                coluna.setQuadro(quadro);
                quadro.getColunas().add(coluna);
            }
            coluna.setNome(item.nome().strip());
            coluna.setPosicao(posicao++);
            coluna.setStatus(item.status());
        }
    }

    private void realocarTarefas(Map<Long, ColunaQuadro> existentes, Set<Long> idsMantidos, Configuracao configuracao) {
        Map<Long, Long> realocacoes = configuracao.realocacoes() == null ? Map.of() : configuracao.realocacoes();
        for (var removida : existentes.values()) {
            if (idsMantidos.contains(removida.getId())) continue;
            var afetadas = tarefas.findByColunaId(removida.getId());
            if (afetadas.isEmpty()) continue;
            Long destinoId = realocacoes.get(removida.getId());
            if (destinoId == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A coluna \"" + removida.getNome() + "\" tem tarefas. Informe em realocacoes para qual coluna mantida elas devem ir.");
            }
            if (!idsMantidos.contains(destinoId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A coluna de destino deve ser uma coluna mantida deste quadro.");
            }
            ColunaQuadro destino = existentes.get(destinoId);
            var statusDestino = configuracao.colunas().stream()
                    .filter(item -> destinoId.equals(item.id())).findFirst().map(ColunaConfiguracao::status).orElse(null);
            for (var tarefa : afetadas) {
                tarefa.setColuna(destino);
                if (statusDestino != null) tarefa.setStatus(statusDestino);
            }
            tarefas.saveAll(afetadas);
        }
    }

    private QuadroResposta resposta(Quadro quadro) {
        List<ColunaResposta> colunas = quadro.getColunas().stream()
                .sorted(Comparator.comparingInt(ColunaQuadro::getPosicao))
                .map(coluna -> new ColunaResposta(coluna.getId(), coluna.getNome(), coluna.getPosicao(), coluna.getStatus()))
                .toList();
        return new QuadroResposta(quadro.getId(), quadro.getProjeto().getId(), quadro.getNome(), colunas);
    }
}
