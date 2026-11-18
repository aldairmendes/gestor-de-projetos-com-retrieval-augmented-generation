package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectOpenApi() {
        Schema<?> error = new ObjectSchema()
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("erro", new StringSchema().example("Bad Request"))
                .addProperty("mensagem", new StringSchema().example("Verifique os dados informados."))
                .addProperty("timestamp", new DateTimeSchema())
                .addProperty("erros", new MapSchema().additionalProperties(true)
                        .description("Mensagens de erro por campo, presentes nas falhas de validação."));

        Components components = new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Obtenha o JWT em /api/v1/auth/login ou /api/v1/auth/register. "
                                + "Informe somente o token; a interface adiciona o prefixo Bearer."))
                .addSchemas("ErroApi", error)
                .addResponses("BadRequest", errorResponse("Dados inválidos, parâmetro ausente ou formato incorreto."))
                // O filtro de segurança pode responder sem corpo; não impõe um schema JSON.
                .addResponses("Unauthorized", new ApiResponse().description("Autenticação ausente, inválida ou expirada; no login, credenciais inválidas."))
                .addResponses("Forbidden", new ApiResponse().description("Usuário autenticado sem o papel exigido ou sem participação na equipe do projeto."))
                .addResponses("NotFound", errorResponse("Recurso não encontrado."))
                .addResponses("Conflict", errorResponse("Conflito de dados, restrição de integridade ou projeto arquivado."));

        return new OpenAPI()
                .info(new Info().title("API de Gestão de Projetos")
                        .version("v1")
                        .description("Gestão de usuários, projetos e tarefas com controle de acesso por papéis globais. "
                                + "Autentique-se e utilize Authorize para acessar as operações protegidas. "
                                + "A participação na equipe limita o acesso aos projetos e tarefas, inclusive para administradores."))
                .components(components);
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErroApi"))));
    }
}
