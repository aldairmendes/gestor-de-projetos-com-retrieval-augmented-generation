package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.DocumentoTrecho;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoTrechoRepository extends JpaRepository<DocumentoTrecho, Long> {
    java.util.List<DocumentoTrecho> findByDocumentoIdOrderByOrdem(Long documentoId);
    @org.springframework.data.jpa.repository.Query("select t from DocumentoTrecho t where t.documento.projeto.id=:projetoId and t.id in :ids")
    java.util.List<DocumentoTrecho> findScoped(Long projetoId, java.util.Collection<Long> ids);
    @org.springframework.data.jpa.repository.Query("select t from DocumentoTrecho t where t.documento.projeto.id=:projetoId order by t.id")
    java.util.List<DocumentoTrecho> findForProject(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
