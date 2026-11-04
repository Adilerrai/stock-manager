package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.RattachementPointDeVenteRequest;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.enums.compta.EtatLivre;
import com.gestion.persistent.enums.compta.OrigineLivre;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.LivreCourantRepository;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.repository.compta.LivreRepository.LigneLivre;
import com.gestion.service.AuditService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class LivreService {

    private static final String CONTRAT = "compta/openapi-compta.json";

    private final LivreRepository livres;
    private final LivreCourantRepository courants;
    private final JournauxRepository journaux;
    private final ExerciceService exercices;
    private final LivreCourantService livreCourant;
    private final AuditService audit;
    private final String versionContrat;

    public LivreService(LivreRepository livres, LivreCourantRepository courants, JournauxRepository journaux, ExerciceService exercices,
                        LivreCourantService livreCourant, AuditService audit) {
        this.livres = livres;
        this.courants = courants;
        this.journaux = journaux;
        this.exercices = exercices;
        this.livreCourant = livreCourant;
        this.audit = audit;
        this.versionContrat = empreinteContrat();
    }

    /** Ne lève jamais et n'écrit jamais : c'est ce qui permet à l'écran d'afficher l'état du module sans erreur. */
    @Transactional(readOnly = true)
    public LivreDTO etat() {
        Optional<LivreCourantService.Contexte> contexte = livreCourant.contexte();
        if (contexte.isEmpty() || !livreCourant.abonnementActif(contexte.get())) {
            return vide(EtatLivre.NON_SOUSCRIT);
        }
        Optional<Long> livreId = livreCourant.livre(contexte.get());
        if (livreId.isEmpty()) {
            return vide(EtatLivre.A_ACTIVER);
        }
        return actif(livreId.get());
    }

    @Transactional(readOnly = true)
    public IdentiteLivreDTO identiteProposee() {
        LivreCourantService.Contexte contexte = contexteExige();
        Optional<IdentiteLivreDTO> identite = contexte.dossier()
                ? livres.identiteDuDossier(contexte.reference())
                : livres.identiteDuProfil(contexte.reference()).or(() -> livres.identiteDuPointDeVente(contexte.reference()));
        return identite.orElseThrow(() -> new CommonException("Aucune identité à reprendre pour ce dossier",
                HttpStatus.CONFLICT, "COMPTA_IDENTITE_ABSENTE"));
    }

    /** Une seule transaction, jamais au détour d'un GET. Rien n'est écrit dans societes ni dans point_de_vente. */
    @Transactional
    public LivreDTO activer(ActivationLivreRequest demande) {
        LivreCourantService.Contexte contexte = contexteExige();
        if (!livreCourant.abonnementActif(contexte)) {
            throw new CommonException("La comptabilité n'est pas souscrite", HttpStatus.CONFLICT, "COMPTA_MODULE_INACTIF");
        }
        if (livreCourant.livre(contexte).isPresent()) {
            throw new CommonException("La comptabilité est déjà activée pour ce dossier", HttpStatus.CONFLICT, "COMPTA_LIVRE_DEJA_ACTIF");
        }
        if (demande.dateDemarrage().isBefore(demande.premierExercice().du())
                || demande.dateDemarrage().isAfter(demande.premierExercice().au())) {
            throw new CommonException("La date de démarrage doit tomber dans le premier exercice",
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_DEMARRAGE_HORS_EXERCICE");
        }
        String auteur = livreCourant.auteur();
        Long livreId = livres.creer(contexte.dossier() ? contexte.reference() : null, demande, auteur);
        if (!contexte.dossier()) {
            livres.rattacher(livreId, contexte.reference(), demande.dateDemarrage());
        }
        livres.copierPlan(livreId);
        for (String[] journal : JournalService.JOURNAUX_DE_BASE) {
            journaux.creer(livreId, journal[0], journal[1], journal[2], null);
        }
        exercices.creer(livreId, demande.premierExercice(), null, demande.dateDemarrage(), auteur);
        audit.logAction(ActionAudit.CREATION, "compta_livres", livreId, "Activation : " + demande.identite().raisonSociale());
        return actif(livreId);
    }

    @Transactional
    public LivreDTO modifierIdentite(IdentiteLivreDTO identite) {
        Long livreId = livreCourant.exiger().livreId();
        livres.modifierIdentite(livreId, identite);
        audit.logAction(ActionAudit.MODIFICATION, "compta_livres", livreId, "Identité du livre");
        return actif(livreId);
    }

    @Transactional
    public LivreDTO rattacher(RattachementPointDeVenteRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        LigneLivre livre = livres.lire(livreId);
        if (courants.livreDuPointDeVente(demande.pointDeVenteId()).isPresent()) {
            throw new CommonException("Ce point de vente est déjà rattaché à un livre", HttpStatus.CONFLICT, "COMPTA_PDV_DEJA_RATTACHE");
        }
        Optional<IdentiteLivreDTO> profil = livres.identiteDuProfil(demande.pointDeVenteId());
        Optional<String> ice = profil.map(IdentiteLivreDTO::ice).filter(v -> v != null && !v.isBlank());
        if (ice.isPresent() && livre.ice() != null && !ice.get().equals(livre.ice())) {
            throw new CommonException("L'ICE du profil diffère de celui du livre", HttpStatus.CONFLICT, "COMPTA_IDENTITE_DIVERGENTE");
        }
        livres.rattacher(livreId, demande.pointDeVenteId(), demande.dateRattachement());
        audit.logAction(ActionAudit.MODIFICATION, "compta_livre_points_de_vente", demande.pointDeVenteId(), "Rattachement au livre " + livreId);
        return actif(livreId);
    }

    private LivreCourantService.Contexte contexteExige() {
        return livreCourant.contexte().orElseThrow(() -> new CommonException(
                "Aucun point de vente ni dossier dans le contexte de la requête", HttpStatus.CONFLICT, "COMPTA_CONTEXTE_ABSENT"));
    }

    private LivreDTO vide(EtatLivre etat) {
        return new LivreDTO(etat, null, null, null, null, null, null, null, null, List.of(), null, List.of(), null, versionContrat);
    }

    private LivreDTO actif(Long livreId) {
        LigneLivre livre = livres.lire(livreId);
        return new LivreDTO(EtatLivre.ACTIF, livre.id(), livre.raisonSociale(), livre.identifiantFiscal(), livre.ice(), livre.modele(),
                livre.dateDemarrage(), livre.remiseGlobaleMode(),
                livre.societeId() != null ? OrigineLivre.DOSSIER : OrigineLivre.POINT_DE_VENTE,
                livres.pointsDeVente(livreId), livres.exerciceCourant(livreId).orElse(null), livres.periodesOuvertes(livreId), livres.periodeInventaire(livreId).orElse(null), versionContrat);
    }

    /** SHA-256 de l'instantané du contrat. Le front le compare à sa copie : un écart s'affiche, sans rien bloquer. */
    private static String empreinteContrat() {
        ClassPathResource contrat = new ClassPathResource(CONTRAT);
        if (!contrat.exists()) {
            return null;
        }
        try (InputStream flux = contrat.getInputStream()) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(flux.readAllBytes()));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Lecture impossible de " + CONTRAT, e);
        }
    }
}
