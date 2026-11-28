package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.service.ProjetoService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DocumentService {
    private final ProjetoService projetos;
    private final AccessPolicy access;
    private final DocumentoFonteRepository documentos;
    private final DocumentoTrechoRepository trechos;
    private final DocumentParser parser;
    private final EmbeddingProvider embeddings;
    private final VectorIndex index;

    @Transactional
    public Documento ingest(Long projetoId, MultipartFile file) {
        access.requireAdmin(); Projeto projeto=projetos.findById(projetoId); active(projeto);
        if (file.isEmpty() || file.getSize()>5*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Envie um arquivo de até 5 MB.");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("documento.txt").replace('\\','/');
        name=name.substring(name.lastIndexOf('/')+1);
        if (name.isBlank() || name.length()>200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nome do arquivo inválido.");
        List<DocumentParser.Chunk> chunks;
        try { chunks=parser.parse(name,file.getBytes()); }
        catch (IOException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falha ao ler arquivo."); }
        List<double[]> vectors=embeddings.embed(chunks.stream().map(DocumentParser.Chunk::texto).toList(),false);
        if (vectors.size()!=chunks.size()) throw new AiUnavailableException("Embeddings incompletos.");
        DocumentoFonte document=new DocumentoFonte(); document.setProjeto(projeto); document.setNome(name);
        document.setTipo(name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT));
        document.setEmbeddingModel(embeddings.fingerprint()); document.setCriadoPor(access.currentUser().getId());
        document.setQuantidadeTrechos(chunks.size()); documentos.saveAndFlush(document);
        for (int i=0;i<chunks.size();i++) {
            var chunk=chunks.get(i); var entity=new DocumentoTrecho(); entity.setDocumento(document);
            entity.setOrdem(chunk.ordem()); entity.setPagina(chunk.pagina()); entity.setTexto(chunk.texto());
            trechos.saveAndFlush(entity); index.put(entity.getId(),vectors.get(i),embeddings.fingerprint());
        }
        return dto(document);
    }

    public List<Documento> list(Long projetoId) {
        projetos.findById(projetoId);
        return documentos.findByProjetoIdOrderByIdDesc(projetoId).stream().map(this::dto).toList();
    }

    public List<Fonte> chunks(Long projetoId,Long documentoId) {
        scoped(projetoId,documentoId);
        return trechos.findByDocumentoIdOrderByOrdem(documentoId).stream().map(t -> fonte(t,1)).toList();
    }

    @Transactional public void delete(Long projetoId,Long documentoId) {
        access.requireAdmin(); DocumentoFonte document=scoped(projetoId,documentoId); active(document.getProjeto());
        // A exclusão em cascata das FKs remove trechos e vetores na mesma transação.
        documentos.delete(document); documentos.flush();
    }

    private DocumentoFonte scoped(Long projetoId,Long id) {
        projetos.findById(projetoId);
        return documentos.findById(id).filter(d -> d.getProjeto().getId().equals(projetoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento não encontrado neste projeto."));
    }
    private void active(Projeto projeto) {
        if (projeto.isArquivado()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Projeto arquivado.");
    }
    private Documento dto(DocumentoFonte d) { return new Documento(d.getId(),d.getNome(),d.getTipo(),d.getQuantidadeTrechos(),d.getCriadoEm()); }
    public static Fonte fonte(DocumentoTrecho t,double score) {
        return new Fonte("D"+t.getId(),"DOCUMENTO",t.getDocumento().getId(),t.getDocumento().getNome(),t.getPagina(),t.getTexto(),score);
    }
}
