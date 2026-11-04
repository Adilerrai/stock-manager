package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.ContrepassationRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.BrouillonRepository;
import com.gestion.repository.compta.BrouillonRepository.LigneBrouillon;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.EcritureEnregistree;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.repository.compta.PeriodeRepository.LignePeriode;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class EcritureService {

    private static final Set<String> NATURES_BROUILLON = Set.of("COURANTE", "INVENTAIRE", "A_NOUVEAU");
    /** Ces écritures ne se contre-passent que par leur module : il remet aussi sa fiche ou sa déclaration en état. */
    private static final Set<String> ORIGINES_PILOTEES = Set.of("IMMOBILISATION", "INVENTAIRE", "TVA", "IS", "OUVERTURE", "CLOTURE");

    private final BrouillonRepository brouillons;
    private final JournalRepository journal;
    private final JournauxRepository journaux;
    private final CompteRepository comptes;
    private final TiersRepository tiers;
    private final PeriodeRepository periodes;
    private final ExerciceRepository exercices;
    private final PosteurEcritures posteur;
    private final LettrageService lettrage;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public EcritureService(BrouillonRepository brouillons, JournalRepository journal, JournauxRepository journaux, CompteRepository comptes,
                           TiersRepository tiers, PeriodeRepository periodes, ExerciceRepository exercices, PosteurEcritures posteur,
                           LettrageService lettrage, LivreCourantService livreCourant, AuditService audit) {
        this.brouillons = brouillons;
        this.journal = journal;
        this.journaux = journaux;
        this.comptes = comptes;
        this.tiers = tiers;
        this.periodes = periodes;
        this.exercices = exercices;
        this.posteur = posteur;
        this.lettrage = lettrage;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<BrouillonDTO> listerBrouillons(Long journalId, Long periodeId) {
        Long livreId = livreCourant.exiger().livreId();
        LocalDate du = null;
        LocalDate au = null;
        if (periodeId != null) {
            LignePeriode periode = periodes.lire(livreId, periodeId).orElseThrow(() -> new ResourceNotFoundException("Période", "id", periodeId));
            du = periode.du();
            au = periode.au();
        }
        return brouillons.lister(livreId, journalId, du, au).stream().map(b -> versDto(livreId, b)).toList();
    }

    @Transactional(readOnly = true)
    public BrouillonDTO lireBrouillon(Long id) {
        Long livreId = livreCourant.exiger().livreId();
        return versDto(livreId, exigerBrouillon(livreId, id));
    }

    /** Un brouillon déséquilibré s'enregistre : seule la validation exige l'équilibre. */
    @Transactional
    public BrouillonDTO creerBrouillon(BrouillonRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        controlerBrouillon(livreId, demande);
        Long id = brouillons.creer(livreId, demande, livreCourant.auteur());
        brouillons.remplacerLignes(livreId, id, demande.lignes());
        return versDto(livreId, exigerBrouillon(livreId, id));
    }

    @Transactional
    public BrouillonDTO modifierBrouillon(Long id, BrouillonRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        exigerBrouillon(livreId, id);
        controlerBrouillon(livreId, demande);
        brouillons.modifier(livreId, id, demande);
        brouillons.remplacerLignes(livreId, id, demande.lignes());
        return versDto(livreId, exigerBrouillon(livreId, id));
    }

    @Transactional
    public void supprimerBrouillon(Long id) {
        Long livreId = livreCourant.exiger().livreId();
        exigerBrouillon(livreId, id);
        brouillons.supprimer(livreId, id);
    }

    /** Le brouillon entre au journal et disparaît, dans la même transaction. */
    @Transactional
    public EcritureDTO validerBrouillon(Long id) {
        Long livreId = livreCourant.exiger().livreId();
        LigneBrouillon b = exigerBrouillon(livreId, id);
        List<NouvelleLigne> lignes = brouillons.lignes(livreId, id).stream()
                .map(l -> new NouvelleLigne(l.compte(), l.tiersId(), l.debit(), l.credit(), l.libelle(), l.dateEcheance(), l.natureMouvement(),
                        l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), null, null))
                .toList();
        EcritureEnregistree ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(b.journalId(), b.dateEcriture(), b.datePiece(),
                b.refPiece(), b.libelle(), b.nature(), "A_NOUVEAU".equals(b.nature()) ? "OUVERTURE" : "SAISIE", null, null, b.motif(), b.aExtourner(),
                b.creeLe(), b.creePar(), lignes));
        brouillons.supprimer(livreId, id);
        return journal.lire(livreId, ecriture.id()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public EcritureDTO lire(Long id) {
        Long livreId = livreCourant.exiger().livreId();
        return exigerEcriture(livreId, id);
    }

    @Transactional(readOnly = true)
    public List<EcritureDTO> rechercher(String refPiece, LocalDate datePiece, BigDecimal montant, String libelle, String compte, LocalDate du, LocalDate au) {
        return journal.rechercher(livreCourant.exiger().livreId(), refPiece, datePiece, montant, libelle, compte, du, au);
    }

    /**
     * Le miroir exact, dans le même journal. Sur les comptes lettrables, l'origine et son miroir sont lettrés ensemble :
     * le tiers n'a plus de ligne ouverte fantôme.
     */
    @Transactional
    public EcritureDTO contrepasser(Long id, ContrepassationRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        EcritureDTO origine = exigerEcriture(livreId, id);
        ExerciceDTO exercice = exercices.lire(livreId, origine.exerciceId()).orElseThrow();
        if ("CLOTURE".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " est clos : passez une écriture de correction sur l'exercice ouvert",
                    HttpStatus.CONFLICT, "COMPTA_CORRECTION_REQUISE");
        }
        if (ORIGINES_PILOTEES.contains(origine.origine())) {
            throw new CommonException("L'écriture " + origine.numeroComplet() + " se corrige depuis son module (" + origine.origine() + ")",
                    HttpStatus.CONFLICT, "COMPTA_ECRITURE_PILOTEE");
        }
        if (origine.contrepasseeParId() != null) {
            throw new CommonException("L'écriture " + origine.numeroComplet() + " est déjà contre-passée", HttpStatus.CONFLICT, "COMPTA_DEJA_CONTREPASSEE");
        }
        if (origine.estContrepassation()) {
            throw new CommonException("Une contre-passation ne se contre-passe pas", HttpStatus.CONFLICT, "COMPTA_DEJA_CONTREPASSEE");
        }
        LocalDate date = demande.dateEcriture() != null ? demande.dateEcriture() : dateParDefaut(livreId, origine);
        List<LigneJournal> lignesOrigine = journal.lignesBrutes(livreId, id);
        List<NouvelleLigne> miroir = new ArrayList<>();
        for (LigneJournal l : lignesOrigine) {
            miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(),
                    l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
        }
        EcritureEnregistree enregistree = posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), date, origine.datePiece(),
                origine.refPiece(), "Contre-passation de " + origine.numeroComplet(), origine.nature(), origine.origine(), id, null,
                demande.motif().trim(), false, null, null, miroir));
        lettrage.lettrerMiroir(livreId, lignesOrigine, journal.lignesBrutes(livreId, enregistree.id()), date);
        audit.logAction(ActionAudit.ANNULATION, "compta_ecritures", id, "Contre-passée par " + enregistree.id() + " : " + demande.motif().trim());
        return journal.lire(livreId, enregistree.id()).orElseThrow();
    }

    @Transactional
    public EcritureDTO lierPieceJointe(Long id, Long documentId) {
        Long livreId = livreCourant.exiger().livreId();
        exigerEcriture(livreId, id);
        journal.lierPieceJointe(livreId, id, documentId, livreCourant.auteur());
        return exigerEcriture(livreId, id);
    }

    /** La date de l'origine si sa période est ouverte, sinon la première période ouverte. */
    private LocalDate dateParDefaut(Long livreId, EcritureDTO origine) {
        boolean ouverte = periodes.courantePourDate(livreId, origine.dateEcriture()).map(p -> "OUVERTE".equals(p.statut())).orElse(false);
        if (ouverte) {
            return origine.dateEcriture();
        }
        return periodes.premiereOuverte(livreId).map(LignePeriode::du).orElseThrow(() -> new CommonException(
                "Aucune période ouverte pour contre-passer", HttpStatus.CONFLICT, "COMPTA_PERIODE_FERMEE"));
    }

    /** Ce qui ne peut pas être stocké. Le reste s'enregistre et s'affiche comme anomalie. */
    private void controlerBrouillon(Long livreId, BrouillonRequest demande) {
        if (journaux.lire(livreId, demande.journalId()).isEmpty()) {
            throw new ResourceNotFoundException("Journal", "id", demande.journalId());
        }
        if (!NATURES_BROUILLON.contains(demande.nature())) {
            throw new CommonException("Nature de brouillon refusée : " + demande.nature(), HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
        }
        int numero = 0;
        for (BrouillonLigneRequest l : demande.lignes()) {
            numero++;
            if (l.debit().signum() < 0 || l.credit().signum() < 0 || (l.debit().signum() > 0 && l.credit().signum() > 0)) {
                throw new CommonException("Ligne " + numero + " : un seul sens, et un montant positif", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
            }
            if (l.compte() != null && !l.compte().isBlank() && comptes.lire(livreId, l.compte().trim()).isEmpty()) {
                throw new CommonException("Ligne " + numero + " : le compte " + l.compte() + " n'est pas au plan", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
            }
            if (l.tiersId() != null && tiers.lire(livreId, l.tiersId()).isEmpty()) {
                throw new CommonException("Ligne " + numero + " : ce tiers n'existe pas dans ce livre", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
            }
        }
    }

    private BrouillonDTO versDto(Long livreId, LigneBrouillon b) {
        List<BrouillonLigneDTO> lignes = brouillons.lignes(livreId, b.id());
        BigDecimal debit = lignes.stream().map(BrouillonLigneDTO::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lignes.stream().map(BrouillonLigneDTO::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BrouillonDTO(b.id(), b.journalId(), b.dateEcriture(), b.datePiece(), b.refPiece(), b.libelle(), b.nature(), b.motif(), b.aExtourner(), lignes,
                b.creeLe(), b.creePar(), b.modifieLe(), debit, credit, debit.subtract(credit),
                posteur.anomalies(livreId, b.refPiece(), b.libelle(), b.datePiece(), lignes.stream()
                        .map(l -> new PosteurEcritures.LigneAControler(l.compte(), l.tiersId(), l.debit(), l.credit())).toList()));
    }

    private LigneBrouillon exigerBrouillon(Long livreId, Long id) {
        return brouillons.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Brouillon", "id", id));
    }

    private EcritureDTO exigerEcriture(Long livreId, Long id) {
        return journal.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Écriture", "id", id));
    }
}
