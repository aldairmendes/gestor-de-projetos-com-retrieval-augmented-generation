package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.DocumentoTrecho;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentoTrechoRepository extends JpaRepository<DocumentoTrecho, Long> {
    List<DocumentoTrecho> findByDocumentoIdOrderByOrdem(Long documentoId);
    @Query("select t from DocumentoTrecho t where t.documento.projeto.id=:projetoId and t.id in :ids")
    List<DocumentoTrecho> findScoped(Long projetoId, Collection<Long> ids);
    @Query("select t from DocumentoTrecho t where t.documento.projeto.id=:projetoId order by t.id")
    List<DocumentoTrecho> findForProject(Long projetoId, Pageable pageable);
}
