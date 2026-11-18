package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoResponseDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.TeamMemberDTO;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.exception.ResourceNotFoundException;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Projeto;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Tarefa;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.ProjetoRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.RoleNames;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final String PROJECT_ROLE_MANAGER = "ROLE_GERENTE";
    private static final String PROJECT_ROLE_MEMBER = "ROLE_MEMBRO";

    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProjetoService(ProjetoRepository projetoRepository, UsuarioRepository usuarioRepository) {
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listAccessible() {
        Usuario currentUser = getCurrentUser();
        return projetoRepository.findAll().stream()
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

    public Projeto findById(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o ID: " + id));
    }

    @Transactional
    public ProjetoResponseDTO create(ProjetoRequestDTO request) {
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

    private boolean canView(Projeto project, Usuario user) {
        return RoleNames.hasRole(user, RoleNames.ADMIN)
                || isCreator(project, user)
                || isMember(project, user);
    }

    private boolean canManage(Projeto project, Usuario user) {
        return RoleNames.hasRole(user, RoleNames.ADMIN)
                || isCreator(project, user)
                || (RoleNames.hasRole(user, RoleNames.MANAGER) && isMember(project, user));
    }

    private void requireManagePermission(Projeto project, Usuario user) {
        if (!canManage(project, user)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar este projeto.");
        }
    }

    private boolean isCreator(Projeto project, Usuario user) {
        return project.getCriador() != null && Objects.equals(project.getCriador().getId(), user.getId());
    }

    private boolean isMember(Projeto project, Usuario user) {
        return project.getEquipe() != null
                && project.getEquipe().stream().anyMatch(member -> Objects.equals(member.getId(), user.getId()));
    }

    private List<Usuario> buildTeam(List<Long> memberIds, Usuario requiredMember) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (memberIds != null) {
            memberIds.stream().filter(Objects::nonNull).forEach(uniqueIds::add);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        if (authentication.getPrincipal() instanceof Usuario user) {
            return user;
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não encontrado."));
    }

    private ProjetoResponseDTO toDTO(Projeto project, Usuario currentUser) {
        List<Usuario> team = project.getEquipe() == null ? List.of() : project.getEquipe();
        List<TeamMemberDTO> members = team.stream()
                .sorted(Comparator.comparing(Usuario::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(member -> new TeamMemberDTO(member.getId(), member.getDisplayName(), member.getEmail()))
                .toList();

        String myRole = null;
        if (isCreator(project, currentUser)) {
            myRole = PROJECT_ROLE_MANAGER;
        } else if (isMember(project, currentUser)) {
            myRole = RoleNames.hasRole(currentUser, RoleNames.MANAGER) ? PROJECT_ROLE_MANAGER : PROJECT_ROLE_MEMBER;
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
