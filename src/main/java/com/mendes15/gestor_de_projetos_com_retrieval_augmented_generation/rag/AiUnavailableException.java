package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AiUnavailableException extends ResponseStatusException {
    public AiUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
