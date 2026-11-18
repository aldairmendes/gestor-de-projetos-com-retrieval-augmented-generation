package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.controller;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SwaggerControllerTest extends BaseIntegrationTest {
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

    JsonNode specification() throws Exception {
        String document = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi.json"), document);
        return objectMapper.readTree(document);
    }

    @Test void everyActualControllerOperationHasSummaryDescriptionAndSecurity() throws Exception {
        JsonNode spec = specification(); int operations = 0;
        for (var entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getPackageName().endsWith(".controller")) continue;
            for (String path : entry.getKey().getPatternValues()) {
                for (RequestMethod method : entry.getKey().getMethodsCondition().getMethods()) {
                    JsonNode operation = spec.path("paths").path(path).path(method.name().toLowerCase(Locale.ROOT));
                    assertFalse(operation.isMissingNode(), method+" "+path);
                    assertFalse(operation.path("summary").asText().isBlank(), path);
                    assertFalse(operation.path("description").asText().isBlank(), path);
                    assertFalse(operation.path("tags").isEmpty(), path);
                    if (path.startsWith("/api/v1/auth/")) assertTrue(operation.path("security").isMissingNode());
                    else assertTrue(operation.path("security").get(0).has("bearerAuth"), path);
                    operations++;
                }
            }
        }
        assertEquals(26, operations);
        assertEquals("bearer", spec.at("/components/securitySchemes/bearerAuth/scheme").asText());
    }

    @Test void responsesExamplesSchemasAndEnumsAreUsable() throws Exception {
        JsonNode spec = specification();
        assertInternalReferences(spec, spec);
        var paths = spec.path("paths").fields();
        while (paths.hasNext()) {
            var path = paths.next(); var methods = path.getValue().fields();
            while (methods.hasNext()) {
                var op = methods.next().getValue();
                if (op.has("requestBody")) {
                    var content = op.at("/requestBody/content/application~1json");
                    assertTrue(content.has("schema"), path.getKey());
                    assertTrue(content.has("example") || content.has("examples"), path.getKey());
                }
                var responses = op.path("responses").fields();
                while (responses.hasNext()) {
                    var response = responses.next(); var value = response.getValue();
                    if (value.has("$ref")) assertFalse(spec.at(value.get("$ref").asText().substring(1)).isMissingNode());
                    if (response.getKey().equals("204")) assertFalse(value.has("content"));
                }
            }
        }
        assertEquals("string",spec.at("/paths/~1api~1v1~1auth~1login/post/responses/200/content/text~1plain/schema/type").asText());
        assertEquals(4, spec.at("/components/schemas/TarefaRequestDTO/properties/status/enum").size());
        assertEquals(3, spec.at("/components/schemas/AlterarPapeisRequest/properties/papeis/items/enum").size());
        assertTrue(spec.at("/components/schemas/ErroApi/properties").has("erros"),
                () -> spec.at("/components/schemas/ErroApi").toPrettyString());
    }

    void assertInternalReferences(JsonNode document, JsonNode node) {
        if (node.isObject() && node.has("$ref")) {
            String ref = node.get("$ref").asText();
            if (ref.startsWith("#/")) assertFalse(document.at(ref.substring(1)).isMissingNode(), ref);
        }
        if (node.isContainerNode()) node.forEach(child -> assertInternalReferences(document, child));
    }

    @Test void swaggerUiAndConfigurationArePublic() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }
}
