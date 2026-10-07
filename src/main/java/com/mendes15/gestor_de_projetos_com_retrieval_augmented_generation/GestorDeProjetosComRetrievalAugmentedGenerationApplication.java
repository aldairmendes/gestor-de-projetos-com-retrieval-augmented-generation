package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GestorDeProjetosComRetrievalAugmentedGenerationApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		SpringApplication.run(GestorDeProjetosComRetrievalAugmentedGenerationApplication.class, args);
	}

}