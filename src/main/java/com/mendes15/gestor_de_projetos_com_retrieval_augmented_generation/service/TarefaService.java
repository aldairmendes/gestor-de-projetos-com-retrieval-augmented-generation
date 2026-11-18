package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarefaService {
    private final TarefaRepository tarefaRepository;
    private final ModelMapper modelMapper;
    private final ProjetoService projetoService;
    private final AccessPolicy access;

    public List<TarefaResponseDTO> findAll() {
        return tarefaRepository.findDistinctByProjeto_Equipe_Id(access.currentUser().getId())
                .stream().map(this::toResponseDTO).toList();
    }

    public TarefaResponseDTO findById(Long id) {
        return toResponseDTO(findAccessible(id));
    }

    @Transactional
    public Tarefa save(Tarefa tarefa) {
        access.requireManager();
        if (tarefa.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma nova tarefa não pode ter ID.");
        }
        Projeto projeto = resolveProject(tarefa);
        access.requireManage(projeto);
        requireActive(projeto);
        tarefa.setProjeto(projeto);
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public TarefaResponseDTO update(Long id, Tarefa details) {
        access.requireManager();
        Tarefa tarefa = findAccessible(id);
        access.requireManage(tarefa.getProjeto());
        requireActive(tarefa.getProjeto());
        // Transferências exigem permissão tanto no projeto de origem quanto no destino.
        Projeto destino = resolveProject(details);
        access.requireManage(destino);
        requireActive(destino);
        tarefa.setTitulo(details.getTitulo());
        tarefa.setConteudo(details.getConteudo());
        tarefa.setStatus(details.getStatus());
        tarefa.setPrioridade(details.getPrioridade());
        tarefa.setProjeto(destino);
        return toResponseDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public TarefaResponseDTO updateStatus(Long id, StatusTarefa status) {
        Tarefa tarefa = findAccessible(id);
        requireActive(tarefa.getProjeto());
        if (status == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status obrigatório.");
        tarefa.setStatus(status);
        return toResponseDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public void delete(Long id) {
        access.requireManager();
        Tarefa tarefa = findAccessible(id);
        access.requireManage(tarefa.getProjeto());
        requireActive(tarefa.getProjeto());
        // Mantém a associação bidirecional consistente para evitar que o cascade
        // do projeto volte a persistir uma tarefa já marcada para exclusão.
        tarefa.getProjeto().getTarefas().removeIf(item -> java.util.Objects.equals(item.getId(), tarefa.getId()));
        tarefaRepository.delete(tarefa);
    }

    public List<TarefaResponseDTO> listByProjeto(Long projetoId) {
        projetoService.findById(projetoId);
        return tarefaRepository.findByProjetoId(projetoId).stream().map(this::toResponseDTO).toList();
    }

    private Tarefa findAccessible(Long id) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
        access.requireView(tarefa.getProjeto());
        return tarefa;
    }

    private Projeto resolveProject(Tarefa tarefa) {
        if (tarefa.getProjeto() == null || tarefa.getProjeto().getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Projeto obrigatório.");
        return projetoService.findById(tarefa.getProjeto().getId());
    }

    private void requireActive(Projeto projeto) {
        if (projeto.isArquivado()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Não é possível alterar tarefas de um projeto arquivado.");
    }

    public TarefaResponseDTO toResponseDTO(Tarefa tarefa) {
        TarefaResponseDTO dto = modelMapper.map(tarefa, TarefaResponseDTO.class);
        dto.setProjetoId(tarefa.getProjeto().getId());
        return dto;
    }
}
