package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import jakarta.persistence.EntityManager;
import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Integra MVC, filtros JWT, serviços e JPA. Cada teste reverte seus dados. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource("classpath:application-test.properties")
@Transactional
public abstract class BaseIntegrationTest {
    protected static final String PASSWORD = "SenhaTeste123!";
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);
    @Autowired protected WebApplicationContext context;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected RoleRepository roles;
    @Autowired protected ProjetoRepository projetos;
    @Autowired protected TarefaRepository tarefas;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtService jwt;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected EntityManager entityManager;
    protected MockMvc mvc;
    protected Usuario gerente, membro, externo, administrador;
    protected Projeto projeto;
    protected Tarefa tarefa;

    @BeforeEach protected void setupApplication() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        gerente = usuario("gerente", "GERENTE");
        membro = usuario("membro", "MEMBRO");
        externo = usuario("externo", "GERENTE");
        administrador = usuario("admin", "ADMINISTRADOR");
        projeto = project("Projeto", gerente, membro);
        tarefa = new Tarefa(); tarefa.setTitulo("Tarefa inicial"); tarefa.setConteudo("Conteúdo inicial");
        tarefa.setProjeto(projeto); tarefa = tarefas.saveAndFlush(tarefa);
        projeto.getTarefas().add(tarefa);
    }

    protected Usuario usuario(String nome, String papel) {
        Role role = roles.findByNome(papel).orElseGet(() -> roles.save(new Role(null, papel)));
        return usuarios.saveAndFlush(Usuario.builder().username(nome).email(nome + "@example.com")
                .password(PASSWORD_HASH).roles(new HashSet<>(Set.of(role))).build());
    }

    protected Projeto project(String nome, Usuario creator, Usuario... members) {
        Projeto p = new Projeto(); p.setNome(nome); p.setCriador(creator);
        p.setEquipe(new ArrayList<>(List.of(members))); p.getEquipe().add(creator);
        return projetos.saveAndFlush(p);
    }

    protected RequestPostProcessor as(Usuario usuario) {
        return user(usuario.getEmail()).authorities(usuario.getAuthorities());
    }
    protected String json(Object body) throws Exception { return objectMapper.writeValueAsString(body); }
    protected void reload() { entityManager.flush(); entityManager.clear(); }
}
