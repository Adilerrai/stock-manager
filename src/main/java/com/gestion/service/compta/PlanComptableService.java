package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ComptePatchRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.PageCompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TitrePlanDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class PlanComptableService {

    private static final int CHIFFRES_MINI_SOUS_COMPTE = 6;
    /** Collectifs cités tels quels par le CGNC, et comptes dont les divisions sont des extensions : ils restent imputables. */
    private static final Set<String> IMPUTABLES_MALGRE_DIVISIONS = Set.of("3421", "4411", "6393", "7195", "7393");

    private final CompteRepository comptes;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public PlanComptableService(CompteRepository comptes, LivreCourantService livreCourant, AuditService audit) {
        this.comptes = comptes;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageCompteDTO lister(String recherche, Integer classe, Boolean imputables, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        int limite = Curseur.taille(taille);
        List<CompteDTO> page = comptes.page(livreId, recherche, classe, imputables, Curseur.decoder(curseur), limite + 1);
        boolean suite = page.size() > limite;
        List<CompteDTO> contenu = suite ? page.subList(0, limite) : page;
        return new PageCompteDTO(contenu, suite ? Curseur.encoder(contenu.get(limite - 1).numero()) : null);
    }

    @Transactional(readOnly = true)
    public CompteDTO lire(String numero) {
        return exiger(livreCourant.exiger().livreId(), numero);
    }

    @Transactional(readOnly = true)
    public List<TitrePlanDTO> titres() {
        livreCourant.exiger();
        return comptes.titres();
    }

    @Transactional
    public CompteDTO creer(CompteCreationRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        String numero = demande.numero().trim();
        if (!numero.matches("[1-8][0-9]{" + (CHIFFRES_MINI_SOUS_COMPTE - 1) + ",11}")) {
            throw new CommonException("Un sous-compte compte au moins " + CHIFFRES_MINI_SOUS_COMPTE + " chiffres : " + numero,
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_COMPTE_INVALIDE");
        }
        if (comptes.lire(livreId, numero).isPresent()) {
            throw new CommonException("Le compte " + numero + " existe déjà", HttpStatus.CONFLICT, "COMPTA_COMPTE_EXISTANT");
        }
        CompteDTO parent = comptes.parent(livreId, numero).orElseThrow(() -> new CommonException(
                "Aucun compte parent au plan pour " + numero, HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_COMPTE_SANS_PARENT"));
        comptes.creer(livreId, numero, demande.libelle().trim(), parent);
        // Une seule façon de ranger une pièce : un compte divisé ne s'impute plus, sauf s'il porte déjà des écritures.
        if (parent.imputable() && !parent.mouvemente() && !IMPUTABLES_MALGRE_DIVISIONS.contains(parent.numero())) {
            comptes.modifier(livreId, parent.numero(), parent.libelle(), parent.actif(), false);
        }
        audit.logAction(ActionAudit.CREATION, "compta_comptes", livreId, "Sous-compte " + numero);
        return exiger(livreId, numero);
    }

    @Transactional
    public CompteDTO modifier(String numero, ComptePatchRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        CompteDTO compte = exiger(livreId, numero);
        String libelle = demande.libelle() != null ? demande.libelle().trim() : compte.libelle();
        if (!libelle.equals(compte.libelle()) && !"SOCIETE".equals(compte.origine())) {
            throw new CommonException("Le libellé d'un compte du plan officiel ne change pas : " + numero,
                    HttpStatus.CONFLICT, "COMPTA_COMPTE_OFFICIEL");
        }
        comptes.modifier(livreId, numero, libelle,
                demande.actif() != null ? demande.actif() : compte.actif(),
                demande.imputable() != null ? demande.imputable() : compte.imputable());
        audit.logAction(ActionAudit.MODIFICATION, "compta_comptes", livreId, "Compte " + numero);
        return exiger(livreId, numero);
    }

    @Transactional
    public void supprimer(String numero) {
        Long livreId = livreCourant.exiger().livreId();
        CompteDTO compte = exiger(livreId, numero);
        if (compte.mouvemente()) {
            throw new CommonException("Le compte " + numero + " porte des écritures : désactivez-le plutôt",
                    HttpStatus.CONFLICT, "COMPTA_COMPTE_MOUVEMENTE");
        }
        if (!"SOCIETE".equals(compte.origine())) {
            throw new CommonException("Un compte du plan officiel ne se supprime pas : " + numero,
                    HttpStatus.CONFLICT, "COMPTA_COMPTE_OFFICIEL");
        }
        comptes.supprimer(livreId, numero);
        audit.logAction(ActionAudit.SUPPRESSION, "compta_comptes", livreId, "Sous-compte " + numero);
    }

    private CompteDTO exiger(Long livreId, String numero) {
        return comptes.lire(livreId, numero).orElseThrow(() -> new ResourceNotFoundException("Compte", "numero", numero));
    }
}
