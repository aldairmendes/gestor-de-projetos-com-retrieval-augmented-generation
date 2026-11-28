package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Role;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.Usuario;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.RoleRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.UsuarioRepository;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.RoleNames;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        for (String roleName : List.of(RoleNames.MEMBER, RoleNames.ADMIN)) {
            if (roleRepository.findByNome(roleName).isEmpty()) {
                roleRepository.save(new Role(null, roleName));
            }
        }

        roleRepository.flush();
        migrateProjectManagers();

        if (!adminEmail.isBlank() && !adminPassword.isBlank() && !usuarioRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByNome(RoleNames.ADMIN).orElseThrow();
            usuarioRepository.save(Usuario.builder()
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(new HashSet<>(Set.of(adminRole, roleRepository.findByNome(RoleNames.MEMBER).orElseThrow())))
                    .build());
        }
    }

    private void migrateProjectManagers() {
        jdbc.update("""
                update projetos set gerente_id = criador_id
                where gerente_id is null and criador_id is not null
                """);
        jdbc.update("""
                insert into tb_usuario_role (usuario_id, role_id)
                select distinct vinculo.usuario_id, membro.id
                from tb_usuario_role vinculo
                join tb_papeis legado on legado.id = vinculo.role_id
                cross join tb_papeis membro
                where legado.nome in ('GERENTE', 'ROLE_GERENTE') and membro.nome = 'MEMBRO'
                  and not exists (
                      select 1 from tb_usuario_role atual
                      where atual.usuario_id = vinculo.usuario_id and atual.role_id = membro.id
                  )
                """);
        jdbc.update("""
                delete from tb_usuario_role
                where role_id in (select id from tb_papeis where nome in ('GERENTE', 'ROLE_GERENTE'))
                """);
        jdbc.update("delete from tb_papeis where nome in ('GERENTE', 'ROLE_GERENTE')");
    }
}
