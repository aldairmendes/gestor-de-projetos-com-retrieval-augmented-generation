package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TarefaResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.StatusTarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.TarefaRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.ProjetoRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.DependenciaTarefaRepository;
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
    private final com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.HistoricoTarefaRepository historico;
    private final com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository usuarios;

    private final ProjetoRepository projetos;
    private final DependenciaTarefaRepository dependencias;

    public List<TarefaResponseDTO> findAll() {
        return tarefaRepository.findDistinctByProjeto_Equipe_Id(access.currentUser().getId())
                .stream().map(this::toResponseDTO).toList();
    }

    public TarefaResponseDTO findById(Long id) {
        return toResponseDTO(findAccessible(id));
    }

    @Transactional
    public Tarefa save(Tarefa tarefa) {
        if (tarefa.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma nova tarefa não pode ter ID.");
        }
        Projeto projeto = resolveProject(tarefa);
        access.requireManage(projeto);
        requireActive(projeto);
        tarefa.setProjeto(projeto);
        normalizeMetadata(tarefa, projeto);
        Tarefa saved=tarefaRepository.save(tarefa);
        audit(saved,"CRIACAO");
        return saved;
    }

    @Transactional
    public TarefaResponseDTO update(Long id, Tarefa details) {
        access.requireManager();
        Tarefa tarefa = tarefaEditavel(id);
        access.requireManage(tarefa.getProjeto());
        // Transferências exigem permissão tanto no projeto de origem quanto no destino.
        Projeto destino = resolveProject(details);
        access.requireManage(destino);
        requireActive(destino);
        if (!tarefa.getProjeto().getId().equals(destino.getId()) && dependencias.possuiVinculos(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remova as dependências antes de transferir a tarefa.");
        }
        normalizeMetadata(details, destino);
        tarefa.setRequisitos(details.getRequisitos()); tarefa.setTecnologias(details.getTecnologias());
        tarefa.setComplexidade(details.getComplexidade()); tarefa.setPrazo(details.getPrazo());
        tarefa.setResponsavel(details.getResponsavel());
        tarefa.setTitulo(details.getTitulo());
        tarefa.setConteudo(details.getConteudo());
        tarefa.setStatus(details.getStatus());
        tarefa.setPrioridade(details.getPrioridade());
        tarefa.setProjeto(destino);
        Tarefa saved=tarefaRepository.save(tarefa);
        audit(saved,"ATUALIZACAO");
        return toResponseDTO(saved);
    }

    @Transactional
    public TarefaResponseDTO updateStatus(Long id, StatusTarefa status) {
        Tarefa tarefa = tarefaEditavel(id);
        if (status == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status obrigatório.");
        tarefa.setStatus(status);
        Tarefa saved=tarefaRepository.save(tarefa); audit(saved,"STATUS");
        return toResponseDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        access.requireManager();
        Tarefa tarefa = tarefaEditavel(id);
        access.requireManage(tarefa.getProjeto());
        // Mantém a associação bidirecional consistente para evitar que o cascade
        // do projeto volte a persistir uma tarefa já marcada para exclusão.
        tarefa.getProjeto().getTarefas().removeIf(item -> java.util.Objects.equals(item.getId(), tarefa.getId()));
        tarefaRepository.delete(tarefa);
    }

    public List<TarefaResponseDTO> listByProjeto(Long projetoId) {
        projetoService.findById(projetoId);
        return tarefaRepository.findByProjetoId(projetoId).stream().map(this::toResponseDTO).toList();
    }

    public List<com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.Historico> history(Long id) {
        Tarefa tarefa = findAccessible(id);
        return historico.findByTarefaIdAndProjetoIdOrderByIdAsc(id, tarefa.getProjeto().getId()).stream().map(h ->
                new com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.Historico(h.getId(),h.getAcao(),h.getAtorId(),h.getResponsavelId(),h.getStatus(),h.getCriadoEm())).toList();
    }

    @Transactional public void assign(Long id,Long usuarioId) {
        Tarefa tarefa = tarefaEditavel(id);
        access.requireManage(tarefa.getProjeto());
        var user=usuarios.findById(usuarioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Responsável não encontrado."));
        if (tarefa.getProjeto().getEquipe().stream().noneMatch(m -> m.getId().equals(usuarioId)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Responsável deve pertencer à equipe.");
        tarefa.setResponsavel(user); tarefaRepository.save(tarefa); audit(tarefa,"ATRIBUICAO");
    }

    private void normalizeMetadata(Tarefa tarefa,Projeto projeto) {
        if (tarefa.getTecnologias()==null) tarefa.setTecnologias(new java.util.LinkedHashSet<>());
        else tarefa.setTecnologias(tarefa.getTecnologias().stream().map(String::strip).filter(t -> !t.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));
        if (tarefa.getResponsavel()!=null) {
            Long userId=tarefa.getResponsavel().getId();
            if (userId==null || projeto.getEquipe().stream().noneMatch(u -> u.getId().equals(userId)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Responsável deve pertencer à equipe do projeto de destino.");
            tarefa.setResponsavel(usuarios.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Responsável não encontrado.")));
        }
    }

    private void audit(Tarefa tarefa,String action) {
        var event=new com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.HistoricoTarefa(); event.setTarefa(tarefa);
        event.setProjetoId(tarefa.getProjeto().getId());
        event.setAcao(action); event.setAtorId(access.currentUser().getId());
        event.setResponsavelId(tarefa.getResponsavel()==null ? null : tarefa.getResponsavel().getId());
        event.setStatus(tarefa.getStatus().name()); historico.save(event);
    }

    Tarefa tarefaEditavel(Long id) {
        Long projetoId = tarefaRepository.buscarProjetoId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
        // O bloqueio do projeto serializa mudanças no grafo e transferências a partir dele.
        Projeto projeto = projetos.findForUpdateById(projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado."));
        access.requireView(projeto);
        requireActive(projeto);
        return tarefaRepository.findByIdAndProjetoId(id, projetoId)
                .filter(tarefa -> tarefa.getProjeto().getId().equals(projetoId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada neste projeto."));
    }

    Tarefa findAccessible(Long id) {
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
        dto.setResponsavelId(tarefa.getResponsavel()==null ? null : tarefa.getResponsavel().getId());
        return dto;
    }
}
