package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Anexo;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Comentario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaColaboracaoDto.Prerequisito;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.AnexoTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.ComentarioTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.DependenciaTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.AnexoTarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.ComentarioTarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.DependenciaTarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarefaColaboracaoService {
    private static final int TAMANHO_MAXIMO_ANEXO = 5 * 1024 * 1024;
    private final TarefaService tarefas;
    private final TarefaRepository tarefaRepository;
    private final AccessPolicy access;
    private final ComentarioTarefaRepository comentarios;
    private final AnexoTarefaRepository anexos;
    private final DependenciaTarefaRepository dependencias;

    public List<Comentario> listarComentarios(Long tarefaId) {
        tarefas.findAccessible(tarefaId);
        return comentarios.findByTarefaIdOrderByIdAsc(tarefaId).stream().map(this::comentario).toList();
    }

    @Transactional
    public Comentario comentar(Long tarefaId, String texto) {
        var tarefa = tarefas.tarefaEditavel(tarefaId);
        return comentario(comentarios.save(new ComentarioTarefa(tarefa, access.currentUser(), texto.strip())));
    }

    public List<Anexo> listarAnexos(Long tarefaId) {
        tarefas.findAccessible(tarefaId);
        return anexos.listarMetadados(tarefaId);
    }

    @Transactional
    public Anexo anexar(Long tarefaId, MultipartFile arquivo) {
        var tarefa = tarefas.tarefaEditavel(tarefaId);
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank() || nome.length() > 255 || nome.equals(".") || nome.equals("..")
                || nome.indexOf('/') >= 0 || nome.indexOf('\\') >= 0 || nome.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        }
        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo com conteúdo.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ANEXO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo deve ter até 5 MB.");
        }
        try {
            var anexo = anexos.save(new AnexoTarefa(tarefa, access.currentUser(), nome, arquivo.getBytes()));
            return new Anexo(anexo.getId(), idAutor(anexo.getAutor()), anexo.getNome(), anexo.getTamanho(), anexo.getCriadoEm());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falha ao ler o arquivo.", ex);
        }
    }

    public AnexoTarefa baixar(Long tarefaId, Long anexoId) {
        tarefas.findAccessible(tarefaId);
        return anexos.findByIdAndTarefaId(anexoId, tarefaId)
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado nesta tarefa."));
    }

    @Transactional
    public void removerAnexo(Long tarefaId, Long anexoId) {
        var tarefa = tarefas.tarefaEditavel(tarefaId);
        access.requireManage(tarefa.getProjeto());
        if (anexos.remover(tarefaId, anexoId) == 0) {
            throw new ResourceNotFoundException("Anexo não encontrado nesta tarefa.");
        }
    }

    public List<Prerequisito> listarDependencias(Long tarefaId) {
        tarefas.findAccessible(tarefaId);
        return dependencias.listarPrerequisitos(tarefaId).stream().map(DependenciaTarefa::getPrerequisito)
                .map(tarefa -> new Prerequisito(tarefa.getId(), tarefa.getTitulo())).toList();
    }

    @Transactional
    public void registrarDependencia(Long tarefaId, Long prerequisitoId) {
        var tarefa = tarefas.tarefaEditavel(tarefaId);
        access.requireManage(tarefa.getProjeto());
        if (tarefaId.equals(prerequisitoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A tarefa não pode depender de si mesma.");
        }
        Long projetoId = tarefa.getProjeto().getId();
        var prerequisito = tarefaRepository.findByIdAndProjetoId(prerequisitoId, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pré-requisito não encontrado neste projeto."));
        if (dependencias.findByTarefaIdAndPrerequisitoId(tarefaId, prerequisitoId).isPresent()) {
            return;
        }
        validarCiclo(projetoId, tarefaId, prerequisitoId);
        dependencias.save(new DependenciaTarefa(tarefa, prerequisito));
    }

    @Transactional
    public void removerDependencia(Long tarefaId, Long prerequisitoId) {
        var tarefa = tarefas.tarefaEditavel(tarefaId);
        access.requireManage(tarefa.getProjeto());
        dependencias.findByTarefaIdAndPrerequisitoId(tarefaId, prerequisitoId).ifPresent(dependencias::delete);
    }

    private void validarCiclo(Long projetoId, Long tarefaId, Long prerequisitoId) {
        var grafo = dependencias.listarVinculos(projetoId).stream().collect(Collectors.groupingBy(
                DependenciaTarefaRepository.Vinculo::getTarefaId,
                Collectors.mapping(DependenciaTarefaRepository.Vinculo::getPrerequisitoId, Collectors.toList())));
        var visitadas = new HashSet<Long>();
        var pendentes = new ArrayDeque<Long>();
        pendentes.push(prerequisitoId);
        while (!pendentes.isEmpty()) {
            Long atual = pendentes.pop();
            if (atual.equals(tarefaId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A dependência criaria um ciclo.");
            }
            if (visitadas.add(atual)) {
                grafo.getOrDefault(atual, List.of()).forEach(pendentes::push);
            }
        }
    }

    private Comentario comentario(ComentarioTarefa item) {
        return new Comentario(item.getId(), idAutor(item.getAutor()), item.getTexto(), item.getCriadoEm());
    }

    private Long idAutor(Usuario autor) {
        return autor == null ? null : autor.getId();
    }
}
