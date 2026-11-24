package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.config.DataInitializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GestorDeProjetosComRetrievaAugmentedGenerationApplicationTests extends BaseIntegrationTest {
    @Test void contextLoadsWithRealSecurityAndPersistence() {
        assertNotNull(context.getBean(DataInitializer.class));
        assertTrue(context.containsBean("securityFilterChain"));
        assertTrue(projetos.existsById(projeto.getId()));
    }

    @Test void initializerSeedsRolesIdempotently() {
        DataInitializer initializer = context.getBean(DataInitializer.class);
        initializer.run(); long count = roles.count(); initializer.run();
        assertEquals(count, roles.count());
        for (String nome : new String[]{"MEMBRO", "ADMINISTRADOR"})
            assertTrue(roles.findByNome(nome).isPresent());
    }
}
