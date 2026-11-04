package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.gestion.repository.compta.LivreCourantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Le seul endroit qui transforme le contexte de la requête en livre. Aucun repli sur un tenant par défaut. */
@Service
public class LivreCourantService {

    /** En-tête ou tenant courant, et l'interprétation retenue. pointDeVenteAbonne porte l'abonnement à lire. */
    public record Contexte(Long reference, boolean dossier, Long pointDeVenteAbonne) {
    }

    private final LivreCourantRepository repository;

    public LivreCourantService(LivreCourantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LivreCourant exiger() {
        Contexte contexte = contexte().orElseThrow(() -> new CommonException(
                "Aucun point de vente ni dossier dans le contexte de la requête", HttpStatus.CONFLICT, "COMPTA_CONTEXTE_ABSENT"));
        if (!repository.abonnementActif(contexte.pointDeVenteAbonne())) {
            throw new CommonException("La comptabilité n'est pas souscrite", HttpStatus.CONFLICT, "COMPTA_MODULE_INACTIF");
        }
        Long livreId = livre(contexte).orElseThrow(() -> new CommonException(
                "La comptabilité n'est pas activée pour ce dossier", HttpStatus.CONFLICT, "COMPTA_LIVRE_NON_ACTIVE"));
        return new LivreCourant(livreId, repository.pointsDeVente(livreId));
    }

    @Transactional(readOnly = true)
    public Optional<Contexte> contexte() {
        Long reference = TenantContext.getCurrentTenant();
        if (reference == null) {
            return Optional.empty();
        }
        User utilisateur = utilisateur();
        Long pointDeVente = utilisateur != null ? utilisateur.getPointDeVenteId() : null;
        Long tenant = utilisateur != null ? utilisateur.getTenantId() : null;
        boolean proprePointDeVente = reference.equals(pointDeVente) || reference.equals(tenant);
        // Les ids de societes et de point_de_vente peuvent se recouvrir : le point de vente homonyme n'est jamais lu.
        boolean dossier = !proprePointDeVente && repository.societeExiste(reference);
        if (!dossier) {
            return Optional.of(new Contexte(reference, false, reference));
        }
        Long abonne = pointDeVente != null ? pointDeVente : repository.tenantDeSociete(reference);
        return Optional.of(new Contexte(reference, true, abonne));
    }

    public Optional<Long> livre(Contexte contexte) {
        return contexte.dossier()
                ? repository.livreDuDossier(contexte.reference())
                : repository.livreDuPointDeVente(contexte.reference());
    }

    public boolean abonnementActif(Contexte contexte) {
        return repository.abonnementActif(contexte.pointDeVenteAbonne());
    }

    public String auteur() {
        User utilisateur = utilisateur();
        return utilisateur != null ? utilisateur.getUsername() : "systeme";
    }

    private User utilisateur() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        return authentification != null && authentification.getPrincipal() instanceof User utilisateur ? utilisateur : null;
    }
}
