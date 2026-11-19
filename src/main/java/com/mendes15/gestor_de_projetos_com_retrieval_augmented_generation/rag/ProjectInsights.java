package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.ProjetoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectInsights {
    private final ProjetoService projetos;
    private final TarefaRepository tarefas;
    private final AccessPolicy access;
    private final RagService rag;

    public Resposta description(Long tarefaId, String instruction) {
        Tarefa target = managedTask(tarefaId);
        List<Fonte> sources = taskContext(target);
        return rag.generate(instruction, "Escreva uma proposta de descrição para a tarefa "
                + target.getId() + ". Preserve os requisitos existentes e indique lacunas nas fontes.", sources);
    }

    public Resposta dependencies(Long tarefaId) {
        Tarefa target = managedTask(tarefaId);
        return rag.generate("Quais tarefas podem ser pré-requisitos desta tarefa?",
                "Avalie possíveis pré-requisitos da tarefa " + tarefaId + ". Use apenas outras tarefas "
                        + "do projeto, cite os IDs e justifique cada sugestão com evidências.",
                taskContext(target).stream().filter(f -> f.tipo().equals("TAREFA")).toList());
    }

    public Resposta summary(Long projetoId, LocalDate inicio, LocalDate fim) {
        var project = projetos.findById(projetoId);
        access.requireManage(project);
        if ((inicio == null) != (fim == null) || inicio != null && inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um intervalo de datas válido.");
        }
        List<Tarefa> all = tarefas.findByProjetoId(projetoId);
        List<Tarefa> selected = inicio == null ? all : all.stream().filter(t -> t.getPrazo() != null
                && !t.getPrazo().isBefore(inicio) && !t.getPrazo().isAfter(fim)).toList();
        List<Fonte> sources = new ArrayList<>();
        sources.add(RagService.overview(project, selected));
        selected.stream().sorted(Comparator.comparing(Tarefa::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Tarefa::getId))
                .limit(8).map(t -> RagService.taskSource(t, 1)).forEach(sources::add);
        String question = inicio == null ? "Resuma o andamento do projeto."
                : "Resuma as tarefas com prazo entre " + inicio + " e " + fim + ", inclusive.";
        return rag.generate(question, "Use os totais informados e destaque as tarefas recentes disponíveis.", sources);
    }

    public Resposta bottlenecks(Long projetoId) {
        var project = projetos.findById(projetoId);
        access.requireManage(project);
        List<Tarefa> all = tarefas.findByProjetoId(projetoId);
        List<Tarefa> open = all.stream().filter(ProjectInsights::isOpen).toList();
        LocalDate today = LocalDate.now();
        long staleDays = 7;
        List<Fonte> sources = new ArrayList<>();
        sources.add(RagService.overview(project, all));
        var loads = open.stream().filter(t -> t.getResponsavel() != null)
                .collect(Collectors.groupingBy(t -> t.getResponsavel().getId(), TreeMap::new, Collectors.counting()));
        double average = (double) loads.values().stream().mapToLong(Long::longValue).sum()
                / Math.max(1, project.getEquipe().size());
        var overloaded = new TreeMap<>(loads);
        overloaded.entrySet().removeIf(entry -> entry.getValue() < 3 || entry.getValue() <= average * 1.5);
        var overdue = open.stream().filter(t -> t.getPrazo() != null && t.getPrazo().isBefore(today)).toList();
        var stale = open.stream().filter(t -> t.getStatus() == StatusTarefa.EM_ANDAMENTO && t.getUpdatedAt() != null
                && t.getUpdatedAt().toInstant().isBefore(java.time.Instant.now().minus(staleDays, ChronoUnit.DAYS))).toList();
        sources.add(new Fonte("G" + projetoId, "INDICADORES", projetoId, "Indicadores de gargalo", null,
                "Carga média por integrante: %.1f. Possível sobrecarga (mínimo 3 abertas e acima de 1,5 vez a média, por ID): %s. "
                        .formatted(average, overloaded)
                        + "Atrasadas: " + overdue.size() + ". Em andamento sem atualização há mais de "
                        + staleDays + " dias: " + stale.size() + ".", 1));
        java.util.stream.Stream.concat(overdue.stream(), stale.stream()).distinct().limit(8)
                .map(t -> RagService.taskSource(t, 1)).forEach(sources::add);
        return rag.generate("Quais gargalos estão indicados pelos dados deste projeto?",
                "Explique os indicadores e cite as evidências. Trate sobrecarga e inatividade como sinais para revisão.", sources);
    }

    static boolean isOpen(Tarefa task) {
        return task.getStatus() != StatusTarefa.CONCLUIDA && task.getStatus() != StatusTarefa.CANCELADA;
    }

    private Tarefa managedTask(Long id) {
        Tarefa task = tarefas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
        access.requireManage(task.getProjeto());
        return task;
    }

    private List<Fonte> taskContext(Tarefa task) {
        List<Fonte> sources = new ArrayList<>();
        sources.add(RagService.taskSource(task, 1));
        String terms = task.getTitulo() + " " + task.getTecnologias() + " " + task.getRequisitos() + " " + task.getConteudo();
        rag.retrieve(task.getProjeto().getId(), RagService.limit(terms, 4000)).stream()
                .filter(f -> !f.id().equals(sources.getFirst().id())).forEach(sources::add);
        return sources;
    }
}
