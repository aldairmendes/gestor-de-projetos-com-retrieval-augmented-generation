package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.enums.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TarefaAuthorizationTest {
    final TarefaRepository tarefas = mock(TarefaRepository.class);
    final ProjetoRepository projetos = mock(ProjetoRepository.class);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final AccessPolicy policy = new AccessPolicy(usuarios);
    final ProjetoService projetoService = new ProjetoService(projetos, usuarios, policy);
    final TarefaService service = new TarefaService(tarefas, new ModelMapper(), projetoService, policy, mock(HistoricoTarefaRepository.class), usuarios);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    Usuario login(TipoRole role) {
        Usuario user = AccessPolicyTest.user(role);
        AccessPolicyTest.authenticate(user);
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    Projeto project(long id, Usuario... team) {
        Projeto p = new Projeto(); p.setId(id); p.setEquipe(List.of(team));
        when(projetos.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    Tarefa task(Projeto projeto) {
        Tarefa t = new Tarefa(); t.setId(10L); t.setTitulo("Tarefa"); t.setProjeto(projeto);
        when(tarefas.findById(10L)).thenReturn(Optional.of(t));
        return t;
    }

    @ParameterizedTest @EnumSource(TipoRole.class)
    void cannotReadTaskFromAnotherTeam(TipoRole role) {
        login(role); task(project(20L));
        assertThrows(AccessDeniedException.class, () -> service.findById(10L));
        assertThrows(AccessDeniedException.class, () -> service.listByProjeto(20L));
        assertThrows(AccessDeniedException.class, () -> service.updateStatus(10L, StatusTarefa.CONCLUIDA));
        verify(tarefas, never()).save(any());
    }

    @Test void memberCanOnlyChangeStatus() {
        Usuario user = login(TipoRole.ROLE_MEMBRO);
        Tarefa task = task(project(20L, user));
        when(tarefas.save(task)).thenReturn(task);
        assertEquals(StatusTarefa.EM_ANDAMENTO, service.updateStatus(10L, StatusTarefa.EM_ANDAMENTO).getStatus());
        assertThrows(AccessDeniedException.class, () -> service.update(10L, task));
        assertThrows(AccessDeniedException.class, () -> service.delete(10L));
        Tarefa novo = new Tarefa(); novo.setProjeto(task.getProjeto());
        assertThrows(AccessDeniedException.class, () -> service.save(novo));
        assertThrows(AccessDeniedException.class, () -> projetoService.create(new com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.dto.ProjetoRequestDTO("Com equipe", null, List.of(2L))));
        verify(tarefas, never()).delete(any());
    }

    @Test void transferRequiresMembershipAtDestination() {
        Usuario user = login(TipoRole.ROLE_GERENTE);
        Tarefa source = task(project(20L, user));
        Tarefa update = new Tarefa(); update.setProjeto(project(30L));
        assertThrows(AccessDeniedException.class, () -> service.update(10L, update));
        assertEquals(20L, source.getProjeto().getId());
        verify(tarefas, never()).save(any());
    }

    @Test void transferRequiresMembershipAtOrigin() {
        Usuario user = login(TipoRole.ROLE_GERENTE);
        task(project(20L));
        Tarefa update = new Tarefa(); update.setProjeto(project(30L, user));
        assertThrows(AccessDeniedException.class, () -> service.update(10L, update));
        verify(tarefas, never()).save(any());
    }

    @Test void managerCanCreateAndDeleteInOwnTeam() {
        Usuario user = login(TipoRole.ROLE_GERENTE);
        Projeto project = project(20L, user);
        Tarefa novo = new Tarefa(); novo.setProjeto(project);
        when(tarefas.save(novo)).thenReturn(novo);
        assertSame(novo, service.save(novo));
        Tarefa existing = task(project);
        service.delete(10L);
        verify(tarefas).delete(existing);
    }

    @Test void archivedProjectsRejectTaskChanges() {
        Usuario user = login(TipoRole.ROLE_GERENTE);
        Projeto project = project(20L, user); project.setArquivado(true);
        task(project);
        assertThrows(ResponseStatusException.class, () -> service.updateStatus(10L, StatusTarefa.CONCLUIDA));
        assertThrows(ResponseStatusException.class, () -> service.delete(10L));
        verify(tarefas, never()).save(any());
        verify(tarefas, never()).delete(any());
    }

    @Test void listIsScopedToCurrentUserAtRepository() {
        login(TipoRole.ROLE_MEMBRO);
        when(tarefas.findDistinctByProjeto_Equipe_Id(1L)).thenReturn(List.of());
        assertTrue(service.findAll().isEmpty());
        verify(tarefas).findDistinctByProjeto_Equipe_Id(1L);
        verify(tarefas, never()).findAll();
    }
}
