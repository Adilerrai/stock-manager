package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.gestion.persistent.dto.compta.EcritureDtos.AnomalieEcritureDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.EcritureEnregistree;
import com.gestion.repository.compta.JournalRepository.NouvelleEcriture;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.repository.compta.PeriodeRepository.LignePeriode;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Seul chemin vers le journal. Refuse avec un message utile avant que la base ne refuse : le service et les
 * déclencheurs tiennent les mêmes invariants, et aucun numéro n'est fourni ici.
 */
@Service
public class PosteurEcritures {

    /** Ce qu'un service veut écrire au journal. */
    public record Proposition(Long journalId, LocalDate dateEcriture, LocalDate datePiece, String refPiece, String libelle, String nature,
                              String origine, Long contrepassationDeId, Long corrigeEcritureId, String motif, boolean aExtourner,
                              LocalDateTime saisieLe, String saisiePar, List<NouvelleLigne> lignes) {
    }

    private static final Map<String, String> JOURNAL_DE_NATURE = Map.of("A_NOUVEAU", "A_NOUVEAUX", "CLOTURE", "CLOTURE");
    private static final Map<String, String> PERIODE_DE_NATURE = Map.of("COURANTE", "COURANTE", "A_NOUVEAU", "OUVERTURE", "INVENTAIRE", "CLOTURE", "CLOTURE", "CLOTURE");

    private final JournalRepository journal;
    private final JournauxRepository journaux;
    private final PeriodeRepository periodes;
    private final ExerciceRepository exercices;
    private final CompteRepository comptes;
    private final TiersRepository tiers;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public PosteurEcritures(JournalRepository journal, JournauxRepository journaux, PeriodeRepository periodes, ExerciceRepository exercices,
                            CompteRepository comptes, TiersRepository tiers, LivreCourantService livreCourant, AuditService audit) {
        this.journal = journal;
        this.journaux = journaux;
        this.periodes = periodes;
        this.exercices = exercices;
        this.comptes = comptes;
        this.tiers = tiers;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    /** À appeler dans une transaction : l'écriture et ce qui l'accompagne passent ou échouent ensemble. */
    public EcritureEnregistree poster(Long livreId, Proposition p) {
        List<AnomalieEcritureDTO> anomalies = anomalies(livreId, p.refPiece(), p.libelle(), p.datePiece(), p.lignes().stream()
                .map(l -> new LigneAControler(l.compte(), l.tiersId(), l.debit(), l.credit())).toList());
        if (!anomalies.isEmpty()) {
            AnomalieEcritureDTO premiere = anomalies.get(0);
            throw new CommonException((premiere.ligne() != null ? "Ligne " + premiere.ligne() + " : " : "") + premiere.message(),
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
        }
        JournalDTO j = journaux.lire(livreId, p.journalId())
                .orElseThrow(() -> new CommonException("Journal inconnu", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE"));
        String typeAttendu = JOURNAL_DE_NATURE.get(p.nature());
        if (typeAttendu != null ? !typeAttendu.equals(j.typeJournal()) : JOURNAL_DE_NATURE.containsValue(j.typeJournal())) {
            throw new CommonException("Une écriture " + p.nature() + " ne va pas dans le journal " + j.code(),
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
        }
        LignePeriode periode = resoudrePeriode(livreId, p.nature(), p.dateEcriture());
        controlerExercice(livreId, periode, p.nature());
        controlerChronologie(livreId, periode, j, p.dateEcriture());

        BigDecimal debit = p.lignes().stream().map(NouvelleLigne::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = p.lignes().stream().map(NouvelleLigne::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        String auteur = livreCourant.auteur();
        EcritureEnregistree enregistree = journal.ajouter(livreId, new NouvelleEcriture(periode.exerciceId(), periode.id(), j.id(), j.typeJournal(),
                p.dateEcriture(), p.datePiece(), p.refPiece().trim(), p.libelle().trim(), p.nature(), p.origine(), p.contrepassationDeId(),
                p.corrigeEcritureId(), p.motif(), p.aExtourner(), debit, credit, p.saisieLe() != null ? p.saisieLe() : LocalDateTime.now(),
                p.saisiePar() != null ? p.saisiePar() : auteur, auteur, p.lignes()));
        audit.logAction(ActionAudit.VALIDATION, "compta_ecritures", enregistree.id(), j.code() + " n° " + enregistree.numero());
        return enregistree;
    }

    /** Une ligne vue par les contrôles, brouillon comme écriture. */
    public record LigneAControler(String compte, Long tiersId, BigDecimal debit, BigDecimal credit) {
    }

    /** Liste ce qui empêche la validation. Sert aussi à l'écran : un brouillon s'enregistre avec ses anomalies. */
    public List<AnomalieEcritureDTO> anomalies(Long livreId, String refPiece, String libelle, LocalDate datePiece, List<LigneAControler> lignes) {
        List<AnomalieEcritureDTO> anomalies = new ArrayList<>();
        if (refPiece == null || refPiece.isBlank()) {
            anomalies.add(new AnomalieEcritureDTO(null, "PIECE_MANQUANTE", "La référence de la pièce est obligatoire."));
        }
        if (libelle == null || libelle.isBlank()) {
            anomalies.add(new AnomalieEcritureDTO(null, "LIBELLE_MANQUANT", "Le libellé de l'écriture est obligatoire."));
        }
        if (datePiece == null) {
            anomalies.add(new AnomalieEcritureDTO(null, "DATE_PIECE_MANQUANTE", "La date de la pièce est obligatoire."));
        }
        if (lignes.size() < 2) {
            anomalies.add(new AnomalieEcritureDTO(null, "LIGNES_INSUFFISANTES", "Une écriture compte au moins deux lignes."));
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        int numero = 0;
        for (LigneAControler l : lignes) {
            numero++;
            debit = debit.add(l.debit());
            credit = credit.add(l.credit());
            controlerLigne(livreId, numero, l, anomalies);
        }
        if (debit.compareTo(credit) != 0) {
            anomalies.add(new AnomalieEcritureDTO(null, "ECART", "Écart de " + debit.subtract(credit).abs().toPlainString() + " entre débit et crédit."));
        }
        return anomalies;
    }

    private void controlerLigne(Long livreId, int numero, LigneAControler l, List<AnomalieEcritureDTO> anomalies) {
        if (l.debit().signum() < 0 || l.credit().signum() < 0) {
            anomalies.add(new AnomalieEcritureDTO(numero, "MONTANT_NEGATIF", "Un montant négatif est refusé : contre-passez."));
        } else if (l.debit().signum() > 0 && l.credit().signum() > 0) {
            anomalies.add(new AnomalieEcritureDTO(numero, "DEUX_SENS", "Une ligne porte un débit ou un crédit, pas les deux."));
        } else if (l.debit().signum() == 0 && l.credit().signum() == 0) {
            anomalies.add(new AnomalieEcritureDTO(numero, "LIGNE_VIDE", "La ligne n'a pas de montant."));
        }
        if (l.compte() == null || l.compte().isBlank()) {
            anomalies.add(new AnomalieEcritureDTO(numero, "COMPTE_MANQUANT", "La ligne n'a pas de compte."));
            return;
        }
        Optional<CompteDTO> compte = comptes.lire(livreId, l.compte());
        if (compte.isEmpty()) {
            anomalies.add(new AnomalieEcritureDTO(numero, "COMPTE_INCONNU", "Le compte " + l.compte() + " n'est pas au plan."));
            return;
        }
        if (!compte.get().imputable() || !compte.get().actif()) {
            anomalies.add(new AnomalieEcritureDTO(numero, "COMPTE_NON_IMPUTABLE", "Le compte " + l.compte() + " n'est pas imputable : choisissez un sous-compte."));
        }
        if (compte.get().exigeTiers() && l.tiersId() == null) {
            anomalies.add(new AnomalieEcritureDTO(numero, "TIERS_OBLIGATOIRE", "Le compte " + l.compte() + " exige un tiers."));
        }
        if (l.tiersId() != null) {
            if (!l.compte().matches("^[34].*")) {
                anomalies.add(new AnomalieEcritureDTO(numero, "TIERS_INTERDIT", "Un tiers ne se pose que sur les classes 3 et 4."));
            } else if (tiers.lire(livreId, l.tiersId()).isEmpty()) {
                anomalies.add(new AnomalieEcritureDTO(numero, "TIERS_INCONNU", "Ce tiers n'existe pas dans ce livre."));
            }
        }
    }

    private LignePeriode resoudrePeriode(Long livreId, String nature, LocalDate date) {
        String type = PERIODE_DE_NATURE.get(nature);
        if (type == null) {
            throw new CommonException("Nature d'écriture inconnue : " + nature, HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECRITURE_INVALIDE");
        }
        Optional<LignePeriode> trouvee = "COURANTE".equals(type)
                ? periodes.courantePourDate(livreId, date)
                : periodes.specialePourDate(livreId, type, date);
        LignePeriode periode = trouvee.orElseThrow(() -> new CommonException("Aucune période pour le " + date + " : cet exercice n'existe pas encore",
                HttpStatus.CONFLICT, "COMPTA_PERIODE_INCONNUE"));
        if ("CLOTUREE".equals(periode.statut())) {
            throw new CommonException("La période du " + periode.du() + " au " + periode.au() + " est clôturée", HttpStatus.CONFLICT, "COMPTA_PERIODE_FERMEE");
        }
        if ("A_VENIR".equals(periode.statut())) {
            throw new CommonException("La période du " + periode.du() + " au " + periode.au() + " n'est pas encore ouverte", HttpStatus.CONFLICT, "COMPTA_PERIODE_A_VENIR");
        }
        return periode;
    }

    private void controlerExercice(Long livreId, LignePeriode periode, String nature) {
        ExerciceDTO exercice = exercices.lire(livreId, periode.exerciceId()).orElseThrow();
        if ("CLOTURE".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " est clôturé", HttpStatus.CONFLICT, "COMPTA_EXERCICE_CLOS");
        }
        if ("ARRETE".equals(exercice.statut()) && !"CLOTURE".equals(nature)) {
            throw new CommonException("L'exercice " + exercice.code() + " est arrêté : levez l'arrêté pour écrire", HttpStatus.CONFLICT, "COMPTA_EXERCICE_ARRETE");
        }
    }

    private void controlerChronologie(Long livreId, LignePeriode periode, JournalDTO j, LocalDate date) {
        Optional<LocalDate> derniere = journal.derniereDate(livreId, periode.exerciceId(), j.id());
        if (derniere.isPresent() && date.withDayOfMonth(1).isBefore(derniere.get().withDayOfMonth(1))) {
            throw new CommonException("Le journal " + j.code() + " est déjà au mois de " + derniere.get().getMonthValue() + "/" + derniere.get().getYear()
                    + " : une écriture du " + date + " ne s'y insère plus", HttpStatus.CONFLICT, "COMPTA_CHRONOLOGIE_JOURNAL");
        }
    }
}
