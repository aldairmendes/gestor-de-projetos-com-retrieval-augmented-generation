package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.repository.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.model.*;
import com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.security.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag.RagDtos.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ProfileService {
    private final AccessPolicy access;
    private final CompetenciaRepository competencias;
    private final UsuarioRepository usuarios;
    private final DominioIaRepository dominio;
    public List<CompetenciaRequest> skills(Long usuarioId) {
        access.requireSelfOrAdmin(usuarioId);
        return competencias.findByUsuarioId(usuarioId).stream()
                .map(c -> new CompetenciaRequest(c.getTecnologia(),c.getTipoAtividade(),c.getNivel())).toList();
    }
    @Transactional public List<CompetenciaRequest> skills(Long usuarioId,List<CompetenciaRequest> skills) {
        access.requireSelfOrAdmin(usuarioId);
        Usuario user=usuarios.findById(usuarioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado."));
        Set<List<String>> unique = new HashSet<>();
        for (var skill:skills) if (!unique.add(List.of(skill.tecnologia().strip().toLowerCase(Locale.ROOT), skill.tipoAtividade().strip().toLowerCase(Locale.ROOT))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Competência duplicada.");
        competencias.deleteByUsuarioId(usuarioId); competencias.flush();
        for (var skill:skills) {
            var entity=new Competencia(); entity.setUsuario(user); entity.setTecnologia(skill.tecnologia().strip());
            entity.setTipoAtividade(skill.tipoAtividade().strip()); entity.setNivel(skill.nivel()); competencias.save(entity);
        }
        return skills(usuarioId);
    }
    public Dominio domain() {
        access.requireAdmin(); var value=dominio.findById(1L).orElseGet(DominioIa::new);
        return new Dominio(value.getRegras(),value.getVocabulario());
    }
    @Transactional public Dominio domain(Dominio request) {
        access.requireAdmin(); var value=dominio.findById(1L).orElseGet(DominioIa::new);
        value.setRegras(request.regras()); value.setVocabulario(request.vocabulario()); dominio.save(value); return request;
    }
}
