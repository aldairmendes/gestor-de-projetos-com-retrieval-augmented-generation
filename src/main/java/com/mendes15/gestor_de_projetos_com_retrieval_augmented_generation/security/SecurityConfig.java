package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.JwtAuthenticationFilter;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.RoleNames;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(RoleNames.ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole(RoleNames.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole(RoleNames.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/papeis").hasRole(RoleNames.ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/projetos", "/api/projetos/*/equipe/membros", "/api/tarefas").hasRole(RoleNames.MANAGER)
                        .requestMatchers(HttpMethod.PUT, "/api/projetos/**", "/api/tarefas/**").hasRole(RoleNames.MANAGER)
                        .requestMatchers(HttpMethod.DELETE, "/api/projetos/**", "/api/tarefas/**").hasRole(RoleNames.MANAGER)
                        .requestMatchers(HttpMethod.PATCH, "/api/projetos/*/arquivar", "/api/projetos/*/desarquivar").hasRole(RoleNames.MANAGER)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}