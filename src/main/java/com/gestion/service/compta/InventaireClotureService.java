package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.AnnulationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneInventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.StockOuvertureDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.InventaireComptaRepository;
import com.gestion.repository.compta.InventaireComptaRepository.Inventaire;
import com.gestion.repository.compta.InventaireComptaRepository.LigneProposee;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * L'inventaire de clôture, détaillé en quantités et en valeurs (CGI art. 145-II). Sans inventaire validé ni déclaration qu'il n'y a aucun stock, l'arrêté est refusé.
 * La méthode de valorisation est déclarée, jamais supposée : elle reste à valider par un expert-comptable.
 */
@Service
public class InventaireClotureService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final String PRIX_ACHAT_COURANT = "PRIX_ACHAT_COURANT";

    private final InventaireComptaRepository repository;
    private final ExerciceRepository exercices;
    private final PeriodeRepository periodes;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final PosteurEcritures posteur;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public InventaireClotureService(InventaireComptaRepository repository, ExerciceRepository exercices, PeriodeRepository periodes, JournauxRepository journaux, JournalRepository journal, PosteurEcritures posteur, LivreCourantService livreCourant, AuditService audit) {
        this.repository = repository;
        this.exercices = exercices;
        this.periodes = periodes;
        this.journaux = journaux;
        this.journal = journal;
        this.posteur = posteur;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    /** Stocks de marchandises 6114, matières 61241, autres 61242 et 61243, produits finis 71321. */
    public static String compteDeVariation(String compteStock) {
        if (compteStock.startsWith("311")) {
            return "6114";
        }
        if (compteStock.startsWith("3121")) {
            return "61241";
        }
        if (compteStock.startsWith("3122")) {
            return "61242";
        }
        if (compteStock.startsWith("3123")) {
            return "61243";
        }
        if (compteStock.startsWith("315")) {
            return "71321";
        }
        return null;
    }

    private ExerciceDTO exiger(Long livreId, Long exerciceId) {
        return exercices.lire(livreId, exerciceId).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", exerciceId));
    }

    @Transactional(readOnly = true)
    public InventaireDTO lire(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        return dto(livreId, exiger(livreId, exerciceId));
    }

    private InventaireDTO dto(Long livreId, ExerciceDTO e) {
        var inventaire = repository.lire(livreId, e.id());
        List<LigneInventaireDTO> lignes = inventaire.map(i -> repository.lignes(livreId, i.id())).orElse(List.of());
        List<StockOuvertureDTO> ouverture = repository.ouverture(livreId, e.id()).stream().map(o -> new StockOuvertureDTO(o.compteStock(), compteDeVariation(o.compteStock()), o.montant())).toList();
        BigDecimal initial = ouverture.stream().map(StockOuvertureDTO::montant).reduce(Z, BigDecimal::add);
        BigDecimal fin = lignes.stream().map(LigneInventaireDTO::valeur).reduce(Z, BigDecimal::add);
        boolean periodeOuverte = periodes.lister(livreId, e.id()).stream().anyMatch(this::estPeriode99Ouverte);
        return new InventaireDTO(inventaire.map(Inventaire::id).orElse(null), e.id(), e.code(), e.au(), inventaire.map(Inventaire::statut).orElse("BROUILLON"), inventaire.map(Inventaire::methode).orElse(null), inventaire.map(Inventaire::aucunStock).orElse(false),
                inventaire.map(Inventaire::ecritureInitial).orElse(null), inventaire.map(Inventaire::ecritureFinal).orElse(null), inventaire.map(Inventaire::valideLe).orElse(null), inventaire.map(Inventaire::validePar).orElse(null), initial, fin, ouverture, lignes, periodeOuverte);
    }

    private boolean estPeriode99Ouverte(PeriodeDTO p) {
        return "CLOTURE".equals(p.typePeriode()) && "OUVERTE".equals(p.statut());
    }

    /** Les lignes viennent des inventaires validés au commercial, figées ici en brouillon. Le prix est celui de l'inventaire. */
    @Transactional
    public InventaireDTO proposer(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        exigerBrouillon(livreId, e);
        List<LigneProposee> proposees = repository.proposition(livreId, e.du(), e.au());
        if (proposees.isEmpty()) {
            throw new CommonException("Aucun inventaire validé au commercial sur " + e.code() + " : saisissez les lignes, ou déclarez qu'il n'y a aucun stock.", HttpStatus.CONFLICT, "COMPTA_INVENTAIRE_RIEN");
        }
        Long id = repository.enregistrer(livreId, exerciceId, PRIX_ACHAT_COURANT, false);
        repository.viderLignes(livreId, id);
        for (LigneProposee p : proposees) {
            BigDecimal valeur = p.quantite().multiply(p.prixUnitaire()).setScale(2, RoundingMode.HALF_UP);
            repository.ajouterLigne(livreId, id, new LigneInventaireDTO(null, "3111", "6114", p.produitId(), p.designation(), p.quantite(), p.prixUnitaire(), valeur, p.origine()), valeur, "6114");
        }
        return dto(livreId, e);
    }

    @Transactional
    public InventaireDTO enregistrer(Long exerciceId, InventaireRequest r) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        exigerBrouillon(livreId, e);
        boolean aucun = Boolean.TRUE.equals(r.aucunStock());
        if (aucun && !r.lignes().isEmpty()) {
            throw new CommonException("Une déclaration « aucun stock » ne porte aucune ligne.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_INVENTAIRE_LIGNES");
        }
        String methode = r.methodeValorisation() == null || r.methodeValorisation().isBlank() ? (aucun ? "SANS_OBJET" : null) : r.methodeValorisation().trim();
        if (methode == null) {
            throw new CommonException("Déclarez la méthode de valorisation : elle reste à valider par un expert-comptable.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_INVENTAIRE_METHODE");
        }
        Long id = repository.enregistrer(livreId, exerciceId, methode, aucun);
        repository.viderLignes(livreId, id);
        for (LigneInventaireDTO l : r.lignes()) {
            if (l.quantite().signum() < 0 || l.prixUnitaire().signum() < 0) {
                throw new CommonException("La ligne " + l.designation() + " a une quantité ou un prix négatif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_INVENTAIRE_LIGNES");
            }
            String variation = l.compteVariation() != null && !l.compteVariation().isBlank() ? l.compteVariation() : compteDeVariation(l.compteStock());
            if (variation == null) {
                throw new CommonException("Indiquez le compte de variation du stock " + l.compteStock() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_INVENTAIRE_LIGNES");
            }
            repository.ajouterLigne(livreId, id, l, l.quantite().multiply(l.prixUnitaire()).setScale(2, RoundingMode.HALF_UP), variation);
        }
        return dto(livreId, e);
    }

    private void exigerBrouillon(Long livreId, ExerciceDTO e) {
        if (repository.lire(livreId, e.id()).filter(i -> "VALIDE".equals(i.statut())).isPresent()) {
            throw new CommonException("L'inventaire de " + e.code() + " est validé : rouvrez-le d'abord.", HttpStatus.CONFLICT, "COMPTA_INVENTAIRE_VALIDE");
        }
    }

    /** Solde le stock d'ouverture et constate le stock final, en période 99, à la date de fin d'exercice. */
    @Transactional
    public InventaireDTO valider(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        exigerBrouillon(livreId, e);
        Inventaire i = repository.lire(livreId, exerciceId).orElseThrow(() -> new CommonException("Enregistrez d'abord l'inventaire, ou la déclaration qu'il n'y a aucun stock.", HttpStatus.CONFLICT, "COMPTA_INVENTAIRE_ABSENT"));
        List<LigneInventaireDTO> lignes = repository.lignes(livreId, i.id());
        if (lignes.isEmpty() && !i.aucunStock()) {
            throw new CommonException("L'inventaire n'a aucune ligne : complétez-le, ou déclarez qu'il n'y a aucun stock.", HttpStatus.CONFLICT, "COMPTA_INVENTAIRE_LIGNES");
        }
        JournalDTO od = journaux.parType(livreId, "OD").orElseThrow(() -> new CommonException("Le journal des opérations diverses n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        Long initial = stockInitial(livreId, e, od);
        Long fin = stockFinal(livreId, e, od, lignes);
        repository.valider(livreId, i.id(), initial, fin, livreCourant.auteur());
        audit.logAction(ActionAudit.VALIDATION, "compta_inventaires", i.id(), "Inventaire de " + e.code() + (i.aucunStock() ? " : aucun stock" : " : " + lignes.size() + " ligne(s)"));
        return dto(livreId, e);
    }

    private Long stockInitial(Long livreId, ExerciceDTO e, JournalDTO od) {
        List<NouvelleLigne> ecriture = new ArrayList<>();
        Map<String, BigDecimal> parVariation = new TreeMap<>();
        for (StockOuvertureDTO o : repository.ouverture(livreId, e.id())) {
            String variation = compteDeVariation(o.compteStock());
            if (variation == null) {
                throw new CommonException("Aucun compte de variation connu pour le stock d'ouverture " + o.compteStock() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_INVENTAIRE_LIGNES");
            }
            parVariation.merge(variation, o.montant(), BigDecimal::add);
            ecriture.add(ligne(o.compteStock(), Z, o.montant()));
        }
        parVariation.forEach((c, m) -> ecriture.add(0, ligne(c, m, Z)));
        return ecriture.isEmpty() ? null : poster(livreId, od, e, "INV-" + e.code() + "-I", "Stock initial " + e.code(), ecriture);
    }

    private Long stockFinal(Long livreId, ExerciceDTO e, JournalDTO od, List<LigneInventaireDTO> lignes) {
        Map<String, BigDecimal> stocks = new TreeMap<>();
        Map<String, BigDecimal> variations = new TreeMap<>();
        for (LigneInventaireDTO l : lignes) {
            stocks.merge(l.compteStock(), l.valeur(), BigDecimal::add);
            variations.merge(l.compteVariation(), l.valeur(), BigDecimal::add);
        }
        List<NouvelleLigne> ecriture = new ArrayList<>();
        stocks.forEach((c, m) -> {
            if (m.signum() != 0) {
                ecriture.add(ligne(c, m, Z));
            }
        });
        variations.forEach((c, m) -> {
            if (m.signum() != 0) {
                ecriture.add(ligne(c, Z, m));
            }
        });
        return ecriture.isEmpty() ? null : poster(livreId, od, e, "INV-" + e.code() + "-F", "Stock final " + e.code(), ecriture);
    }

    private static NouvelleLigne ligne(String compte, BigDecimal debit, BigDecimal credit) {
        return new NouvelleLigne(compte, null, debit, credit, null, null, null, false, null, null, null, null, null, null);
    }

    private Long poster(Long livreId, JournalDTO od, ExerciceDTO e, String ref, String libelle, List<NouvelleLigne> lignes) {
        return posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), e.au(), e.au(), ref, libelle, "INVENTAIRE", "INVENTAIRE", null, null, null, false, null, null, lignes)).id();
    }

    /** Contre-passe les écritures passées à la validation : l'inventaire redevient modifiable. */
    @Transactional
    public InventaireDTO rouvrir(Long exerciceId, AnnulationRequest r) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        if (!"OUVERT".equals(e.statut())) {
            throw new CommonException("L'exercice " + e.code() + " n'est pas ouvert : levez d'abord l'arrêté.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_OUVERT");
        }
        Inventaire i = repository.lire(livreId, exerciceId).filter(x -> "VALIDE".equals(x.statut())).orElseThrow(() -> new CommonException("L'inventaire n'est pas validé.", HttpStatus.CONFLICT, "COMPTA_INVENTAIRE_ABSENT"));
        String motif = r.motif().trim();
        if (i.ecritureInitial() != null) {
            contrepasser(livreId, e, i.ecritureInitial(), motif);
        }
        if (i.ecritureFinal() != null) {
            contrepasser(livreId, e, i.ecritureFinal(), motif);
        }
        repository.rouvrir(livreId, i.id());
        audit.logAction(ActionAudit.ANNULATION, "compta_inventaires", i.id(), "Inventaire rouvert : " + motif);
        return dto(livreId, e);
    }

    private void contrepasser(Long livreId, ExerciceDTO e, Long ecritureId, String motif) {
        EcritureDTO origine = journal.lire(livreId, ecritureId).orElseThrow();
        List<NouvelleLigne> miroir = new ArrayList<>();
        for (LigneJournal l : journal.lignesBrutes(livreId, ecritureId)) {
            miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
        }
        posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), e.au(), origine.datePiece(), origine.refPiece(), "Contre-passation de " + origine.numeroComplet(), "INVENTAIRE", "INVENTAIRE", ecritureId, null, motif, false, null, null, miroir));
    }

    /** Pour l'arrêté : vrai si l'inventaire est validé, aucun stock compris. */
    @Transactional(readOnly = true)
    public boolean estValide(Long livreId, Long exerciceId) {
        return repository.lire(livreId, exerciceId).filter(i -> "VALIDE".equals(i.statut())).isPresent();
    }
}
