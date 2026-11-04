package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ReferentielDtos.PageTiersDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersPatchRequest;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TiersService {

    private final TiersRepository tiers;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public TiersService(TiersRepository tiers, LivreCourantService livreCourant, AuditService audit) {
        this.tiers = tiers;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageTiersDTO lister(String type, String recherche, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        int limite = Curseur.taille(taille);
        String cle = Curseur.decoder(curseur);
        Long apres;
        try {
            apres = cle != null ? Long.valueOf(cle) : null;
        } catch (NumberFormatException e) {
            throw new CommonException("Curseur de page invalide", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_CURSEUR_INVALIDE");
        }
        List<TiersDTO> page = tiers.page(livreId, type, recherche, apres, limite + 1);
        boolean suite = page.size() > limite;
        List<TiersDTO> contenu = suite ? page.subList(0, limite) : page;
        return new PageTiersDTO(contenu, suite ? Curseur.encoder(String.valueOf(contenu.get(limite - 1).id())) : null);
    }

    @Transactional(readOnly = true)
    public TiersDTO lire(Long id) {
        return exiger(livreCourant.exiger().livreId(), id);
    }

    @Transactional
    public TiersDTO creer(TiersCreationRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        Long id = tiers.creer(livreId, demande);
        audit.logAction(ActionAudit.CREATION, "compta_tiers", id, demande.nom());
        return exiger(livreId, id);
    }

    @Transactional
    public TiersDTO modifier(Long id, TiersPatchRequest d) {
        Long livreId = livreCourant.exiger().livreId();
        TiersDTO t = exiger(livreId, id);
        tiers.modifier(livreId, new TiersDTO(t.id(), t.typeTiers(), t.codeAuxiliaire(),
                d.nom() != null ? d.nom() : t.nom(),
                d.ice() != null ? d.ice() : t.ice(),
                d.identifiantFiscal() != null ? d.identifiantFiscal() : t.identifiantFiscal(),
                t.sourceType(), t.sourceId(),
                d.compteCollectif() != null ? d.compteCollectif() : t.compteCollectif(),
                d.entrepriseLiee() != null ? d.entrepriseLiee() : t.entrepriseLiee(),
                d.organismePublic() != null ? d.organismePublic() : t.organismePublic(),
                d.nonResident() != null ? d.nonResident() : t.nonResident(),
                d.actif() != null ? d.actif() : t.actif()));
        audit.logAction(ActionAudit.MODIFICATION, "compta_tiers", id, t.codeAuxiliaire());
        return exiger(livreId, id);
    }

    /** Relit nom, ICE et IF dans le commercial. N'écrit jamais dans une table commerciale. */
    @Transactional
    public TiersDTO synchroniser(Long id) {
        Long livreId = livreCourant.exiger().livreId();
        TiersDTO t = exiger(livreId, id);
        if (t.sourceType() == null) {
            throw new CommonException("Ce tiers n'est pas lié au commercial : " + t.codeAuxiliaire(),
                    HttpStatus.CONFLICT, "COMPTA_TIERS_SANS_SOURCE");
        }
        TiersRepository.IdentiteSource source = tiers.identiteSource(livreId, t.sourceType(), t.sourceId())
                .orElseThrow(() -> new CommonException("La fiche commerciale de " + t.codeAuxiliaire() + " est introuvable",
                        HttpStatus.CONFLICT, "COMPTA_TIERS_SOURCE_ABSENTE"));
        tiers.modifier(livreId, new TiersDTO(t.id(), t.typeTiers(), t.codeAuxiliaire(), source.nom(), source.ice(),
                source.identifiantFiscal() != null ? source.identifiantFiscal() : t.identifiantFiscal(),
                t.sourceType(), t.sourceId(), t.compteCollectif(), t.entrepriseLiee(), t.organismePublic(), t.nonResident(), t.actif()));
        audit.logAction(ActionAudit.MODIFICATION, "compta_tiers", id, "Synchronisation " + t.codeAuxiliaire());
        return exiger(livreId, id);
    }

    private TiersDTO exiger(Long livreId, Long id) {
        return tiers.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Tiers", "id", id));
    }
}
