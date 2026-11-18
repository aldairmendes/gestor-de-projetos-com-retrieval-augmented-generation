package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TeamMemberDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.ProjetoRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.TipoRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Service
public class ProjetoService {

    private static final String PROJECT_ROLE_MANAGER = TipoRole.ROLE_GERENTE.name();
    private static final String PROJECT_ROLE_MEMBER = TipoRole.ROLE_MEMBRO.name();

    private final AccessPolicy access;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProjetoService(ProjetoRepository projetoRepository, UsuarioRepository usuarioRepository, AccessPolicy access) {
        this.access = access;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listAccessible() {
        Usuario currentUser = getCurrentUser();
        return projetoRepository.findDistinctByEquipe_Id(currentUser.getId()).stream()
                .filter(project -> canView(project, currentUser))
                .sorted(Comparator.comparing(Projeto::getId).reversed())
                .map(project -> toDTO(project, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponseDTO findAccessibleById(Long id) {
        Usuario currentUser = getCurrentUser();
        Projeto project = findById(id);
        if (!canView(project, currentUser)) {
            throw new AccessDeniedException("Você não tem acesso a este projeto.");
        }
        return toDTO(project, currentUser);
    }

    @Transactional(readOnly = true)
    public Projeto findById(Long id) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o ID: " + id));
        access.requireView(projeto);
        return projeto;
    }

    @Transactional
    public ProjetoResponseDTO create(ProjetoRequestDTO request) {
        access.requireManager();
        Usuario currentUser = getCurrentUser();
        Projeto project = new Projeto();
        project.setNome(request.getNome().trim());
        project.setDescricao(normalizeDescription(request.getDescricao()));
        project.setCriador(currentUser);
        project.setEquipe(buildTeam(request.getEquipeIds(), currentUser));
        return toDTO(projetoRepository.save(project), currentUser);
    }

    @Transactional
    public ProjetoResponseDTO update(Long id, ProjetoRequestDTO request) {
        Usuario currentUser = getCurrentUser();
        Projeto project = findById(id);
        requireManagePermission(project, currentUser);
        if (project.isArquivado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Projetos arquivados não podem ser editados. Desarquive o projeto primeiro.");
        }
        project.setNome(request.getNome().trim());
        project.setDescricao(normalizeDescription(request.getDescricao()));
        if (request.getEquipeIds() != null) {
            Usuario requiredMember = project.getCriador() != null ? project.getCriador() : currentUser;
            List<Usuario> newTeam = buildTeam(request.getEquipeIds(), requiredMember);
            project.getEquipe().clear();
            project.getEquipe().addAll(newTeam);
        }
        return toDTO(projetoRepository.save(project), currentUser);
    }

    @Transactional
    public ProjetoResponseDTO setArchived(Long id, boolean archived) {
        Usuario currentUser = getCurrentUser();
        Projeto project = findById(id);
        requireManagePermission(project, currentUser);
        project.setArquivado(archived);
        return toDTO(projetoRepository.save(project), currentUser);
    }

    @Transactional
    public void delete(Long id) {
        Usuario currentUser = getCurrentUser();
        Projeto project = findById(id);
        requireManagePermission(project, currentUser);
        projetoRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDTO> listTeam(Long projetoId) {
        return teamMembers(findById(projetoId));
    }

    @Transactional
    public List<TeamMemberDTO> defineTeam(Long projetoId, List<Long> membrosIds) {
        Projeto projeto = findManageableActiveProject(projetoId);
        List<Usuario> equipe = buildTeam(membrosIds, projeto.getCriador());
        projeto.getEquipe().clear();
        projeto.getEquipe().addAll(equipe);
        projetoRepository.save(projeto);
        return teamMembers(projeto);
    }

    @Transactional
    public List<TeamMemberDTO> addTeamMembers(Long projetoId, List<Long> membrosIds) {
        Projeto projeto = findManageableActiveProject(projetoId);
        List<Usuario> novosMembros = buildTeam(membrosIds, projeto.getCriador());
        Set<Long> presentes = projeto.getEquipe().stream().map(Usuario::getId)
                .collect(java.util.stream.Collectors.toSet());
        novosMembros.stream().filter(membro -> presentes.add(membro.getId()))
                .forEach(projeto.getEquipe()::add);
        projetoRepository.save(projeto);
        return teamMembers(projeto);
    }

    @Transactional
    public void removeTeamMember(Long projetoId, Long usuarioId) {
        Projeto projeto = findManageableActiveProject(projetoId);
        if (projeto.getCriador() != null && Objects.equals(projeto.getCriador().getId(), usuarioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O criador deve permanecer na equipe.");
        }
        if (!projeto.getEquipe().removeIf(membro -> Objects.equals(membro.getId(), usuarioId))) {
            throw new ResourceNotFoundException("Usuário não pertence à equipe deste projeto.");
        }
        projetoRepository.save(projeto);
    }

    private Projeto findManageableActiveProject(Long projetoId) {
        access.requireManager();
        Projeto projeto = findById(projetoId);
        access.requireManage(projeto);
        if (projeto.isArquivado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Desarquive o projeto antes de alterar sua equipe.");
        }
        return projeto;
    }

    private List<TeamMemberDTO> teamMembers(Projeto projeto) {
        List<Usuario> equipe = projeto.getEquipe() == null ? List.of() : projeto.getEquipe();
        return equipe.stream()
                .sorted(Comparator.comparing(Usuario::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(membro -> new TeamMemberDTO(membro.getId(), membro.getDisplayName(), membro.getEmail()))
                .toList();
    }

    private boolean canView(Projeto project, Usuario user) {
        return access.canView(project, user);
    }

    private boolean canManage(Projeto project, Usuario user) {
        return access.canManage(project, user);
    }

    private void requireManagePermission(Projeto project, Usuario user) {
        if (!canManage(project, user)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar este projeto.");
        }
    }

    private boolean isMember(Projeto project, Usuario user) {
        return project.getEquipe() != null
                && project.getEquipe().stream().anyMatch(member -> Objects.equals(member.getId(), user.getId()));
    }

    private List<Usuario> buildTeam(List<Long> memberIds, Usuario requiredMember) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (memberIds != null) {
            if (memberIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDs dos membros devem ser positivos e não nulos.");
            }
            uniqueIds.addAll(memberIds);
        }
        List<Usuario> foundUsers = usuarioRepository.findAllById(uniqueIds);
        if (foundUsers.size() != uniqueIds.size()) {
            List<Long> foundIds = foundUsers.stream().map(Usuario::getId).toList();
            List<Long> missingIds = uniqueIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuário(s) não encontrado(s): " + missingIds);
        }
        List<Usuario> team = new ArrayList<>(foundUsers);
        if (requiredMember != null
                && team.stream().noneMatch(member -> Objects.equals(member.getId(), requiredMember.getId()))) {
            team.add(0, requiredMember);
        }
        return team;
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private Usuario getCurrentUser() {
        return access.currentUser();
    }

    private ProjetoResponseDTO toDTO(Projeto project, Usuario currentUser) {
        List<TeamMemberDTO> members = teamMembers(project);

        String myRole = null;
        if (canManage(project, currentUser)) {
            myRole = PROJECT_ROLE_MANAGER;
        } else if (isMember(project, currentUser)) {
            myRole = PROJECT_ROLE_MEMBER;
        }

        ProjetoResponseDTO dto = new ProjetoResponseDTO();
        dto.setId(project.getId());
        dto.setNome(project.getNome());
        dto.setDescricao(project.getDescricao());
        dto.setArquivado(project.isArquivado());
        dto.setCriadorId(project.getCriador() != null ? project.getCriador().getId() : null);
        dto.setEquipe(members);
        dto.setEquipeIds(members.stream().map(TeamMemberDTO::getId).toList());
        dto.setTarefasIds(project.getTarefas() == null ? List.of()
                : project.getTarefas().stream().map(Tarefa::getId).toList());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setMeuPapelNoProjeto(myRole);
        dto.setPodeGerenciar(canManage(project, currentUser));
        return dto;
    }
}
