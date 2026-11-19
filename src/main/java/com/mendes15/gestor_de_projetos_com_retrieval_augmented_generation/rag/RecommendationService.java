package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.TarefaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {
    private final TarefaRepository tarefas;
    private final CompetenciaRepository competencias;
    private final RecomendacaoRegistroRepository registros;
    private final HistoricoTarefaRepository historico;
    private final TarefaService tarefaService;
    private final AccessPolicy access;
    private final RagService rag;
    private final ObjectMapper json;

    @Transactional
    public Recomendacao recommend(Long tarefaId) {
        Tarefa tarefa = managedTask(tarefaId);
        Projeto projeto = tarefa.getProjeto();
        requireActive(projeto);
        List<Tarefa> projectTasks = tarefas.findByProjetoId(projeto.getId());
        var byOwner = projectTasks.stream()
                .filter(t -> !t.getId().equals(tarefaId) && t.getResponsavel() != null)
                .collect(Collectors.groupingBy(t -> t.getResponsavel().getId()));
        var skills = competencias.findByUsuarioIdIn(projeto.getEquipe().stream().map(Usuario::getId).toList())
                .stream().collect(Collectors.groupingBy(c -> c.getUsuario().getId()));
        var completions = historico.findByProjetoIdAndStatusAndResponsavelIdIsNotNull(
                        projeto.getId(), StatusTarefa.CONCLUIDA.name()).stream()
                .filter(event -> !event.getTarefa().getId().equals(tarefaId))
                .collect(Collectors.groupingBy(HistoricoTarefa::getResponsavelId,
                        Collectors.mapping(event -> event.getTarefa().getId(), Collectors.toSet())));
        List<Candidato> ranking = projeto.getEquipe().stream()
                .map(member -> rank(tarefa, member.getId(), skills.getOrDefault(member.getId(), List.of()),
                        byOwner.getOrDefault(member.getId(), List.of()), completions.getOrDefault(member.getId(), Set.of())))
                .sorted(Comparator.comparingDouble(Candidato::pontuacao).reversed().thenComparing(Candidato::usuarioId))
                .toList();

        List<Fonte> sources = new ArrayList<>();
        sources.add(RagService.taskSource(tarefa, 1));
        sources.add(RagService.overview(projeto, projectTasks));
        ranking.stream().limit(8).forEach(candidate -> sources.add(new Fonte("U" + candidate.usuarioId(),
                "PERFIL_ALOCACAO", candidate.usuarioId(), "Dados de alocação", null,
                "Integrante " + candidate.usuarioId() + ". Pontuação: " + candidate.pontuacao()
                        + "%. " + candidate.justificativa(), 1)));
        rag.lexicalDocuments(projeto.getId(), RagService.taskText(tarefa), 3).forEach(sources::add);
        Resposta context = rag.generate("Explique a adequação dos integrantes mais bem pontuados para esta tarefa.",
                "Explique o ranking fornecido sem recalcular a pontuação nem mudar a ordem.", sources);

        var registro = new RecomendacaoRegistro();
        registro.setTarefa(tarefa);
        registro.setProjetoId(projeto.getId());
        registro.setSolicitanteId(access.currentUser().getId());
        registro.setRankingJson(encode(ranking));
        registro.setContextoJson(encode(context));
        return response(registros.save(registro), ranking, context);
    }

    public List<Recomendacao> list(Long tarefaId) {
        Tarefa tarefa = managedTask(tarefaId);
        return registros.findByTarefaIdAndProjetoIdOrderByIdDesc(tarefaId, tarefa.getProjeto().getId())
                .stream().map(this::decode).toList();
    }

    @Transactional
    public Recomendacao decide(Long tarefaId, Long id, Feedback feedback) {
        Tarefa tarefa = managedTask(tarefaId);
        requireActive(tarefa.getProjeto());
        RecomendacaoRegistro registro = registros.findByIdAndTarefaIdAndProjetoId(id, tarefaId, tarefa.getProjeto().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recomendação não encontrada para esta tarefa."));
        if (registro.getDecisao() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A decisão já foi registrada.");
        }

        Recomendacao recomendacao = decode(registro);
        Long escolhido = feedback.usuarioId();
        switch (feedback.decisao()) {
            case ACEITA -> {
                if (escolhido == null && !recomendacao.ranking().isEmpty()) {
                    escolhido = recomendacao.ranking().getFirst().usuarioId();
                }
                Long indicado = escolhido;
                if (indicado == null || recomendacao.ranking().stream().noneMatch(c -> c.usuarioId().equals(indicado))) {
                    throw invalid("Selecione um integrante do ranking.");
                }
            }
            case AJUSTADA -> {
                if (escolhido == null) throw invalid("Informe o integrante escolhido.");
            }
            case RECUSADA -> {
                if (escolhido != null) throw invalid("Uma recusa não deve indicar responsável.");
            }
        }
        if (escolhido != null) tarefaService.assign(tarefaId, escolhido);
        registro.setDecisao(feedback.decisao().name());
        registro.setEscolhidoId(escolhido);
        registro.setDecididoPor(access.currentUser().getId());
        registro.setDecididoEm(Instant.now());
        registro.setObservacao(feedback.observacao());
        return response(registro, recomendacao.ranking(), recomendacao.contexto());
    }

    private Candidato rank(Tarefa tarefa, Long member, List<Competencia> skills, List<Tarefa> previous, Set<Long> historicalCompletions) {
        var required = tarefa.getTecnologias().stream().map(RecommendationService::normalize).distinct().toList();
        String description = tarefa.getTitulo() + " " + tarefa.getConteudo() + " " + tarefa.getRequisitos();
        double technology = required.stream().mapToInt(tech -> skills.stream()
                .filter(skill -> normalize(skill.getTecnologia()).equals(tech))
                .mapToInt(Competencia::getNivel).max().orElse(0)).average().orElseGet(() -> skills.stream()
                .filter(s -> RagService.similarity(s.getTecnologia(), description) == 1)
                .mapToInt(Competencia::getNivel).max().orElse(0)) / 5;
        double activity = skills.stream().mapToDouble(s ->
                RagService.similarity(s.getTipoAtividade(), description) * s.getNivel() / 5).max().orElse(0);
        long completed = previous.stream().filter(t -> t.getStatus() == StatusTarefa.CONCLUIDA).count();
        long open = previous.stream().filter(ProjectInsights::isOpen).count();
        Set<Long> completedIds = new HashSet<>(historicalCompletions);
        previous.stream().filter(t -> t.getStatus() == StatusTarefa.CONCLUIDA)
                .map(Tarefa::getId).forEach(completedIds::add);
        double experience = Math.min(completedIds.size() / 5.0, 1);
        // A ausência de histórico não é tratada como desempenho insuficiente.
        long closed = completed + previous.stream().filter(t -> t.getStatus() == StatusTarefa.CANCELADA).count();
        double delivery = closed == 0 ? 0.5 : (double) completed / closed;
        double score = 45 * technology + 15 * activity + 10 * experience + 10 * delivery + 20.0 / (1 + open);
        String evidence = String.format(Locale.ROOT,
                "Aderência tecnológica: %.0f%%. Aderência ao tipo de atividade: %.0f%%. "
                        + "No projeto: %d conclusões no histórico, %d tarefas abertas. Taxa de conclusão entre tarefas encerradas: %s.",
                technology * 100, activity * 100, completedIds.size(), open, closed == 0 ? "sem tarefas encerradas"
                        : String.format(Locale.ROOT, "%.0f%%", delivery * 100));
        return new Candidato(member, Math.round(score * 100) / 100.0, evidence,
                List.of("T" + tarefa.getId(), "P" + tarefa.getProjeto().getId()));
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private Tarefa managedTask(Long id) {
        Tarefa tarefa = tarefas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
        access.requireManage(tarefa.getProjeto());
        return tarefa;
    }

    private void requireActive(Projeto projeto) {
        if (projeto.isArquivado()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Projeto arquivado.");
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao registrar recomendação.", ex);
        }
    }

    private Recomendacao decode(RecomendacaoRegistro value) {
        try {
            List<Candidato> ranking = json.readValue(value.getRankingJson(), new TypeReference<>() {});
            return response(value, ranking, json.readValue(value.getContextoJson(), Resposta.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao ler recomendação.", ex);
        }
    }

    private Recomendacao response(RecomendacaoRegistro value, List<Candidato> ranking, Resposta context) {
        return new Recomendacao(value.getId(), value.getTarefa().getId(), ranking, context,
                value.getDecisao(), value.getEscolhidoId());
    }
}
