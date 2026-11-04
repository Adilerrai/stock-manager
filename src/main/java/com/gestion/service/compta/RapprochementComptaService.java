package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.persistent.dto.compta.RapprochementDtos.ComptabiliserReleveRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.CompteRapprochementDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.EcritureNonPointeeDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.EtatRapprochementDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.EtatValideDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.PropositionPointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ReleveNonPointeDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ValiderEtatRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.RapprochementComptaRepository;
import com.gestion.repository.compta.RapprochementComptaRepository.CompteFinancier;
import com.gestion.repository.compta.RapprochementComptaRepository.LigneEcriture;
import com.gestion.repository.compta.RapprochementComptaRepository.LigneReleve;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le rapprochement, compte par compte. Une ligne d'écriture se pointe sur une ligne de relevé, avec un montant ; la base refuse de pointer au-delà de l'une ou de l'autre.
 * L'application ne touche ni au relevé ni à la trésorerie du commercial.
 */
@Service
public class RapprochementComptaService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    /** Une proposition ne regarde pas au-delà : au-delà, c'est au comptable de décider. */
    private static final int JOURS_DE_PROPOSITION = 7;

    private final RapprochementComptaRepository repository;
    private final CorrespondanceService correspondances;
    private final LivreCourantService livreCourant;
    private final PosteurEcritures posteur;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final DatesComptables dates;
    private final AuditService audit;
    private final ObjectMapper json;

    public RapprochementComptaService(RapprochementComptaRepository repository, CorrespondanceService correspondances, LivreCourantService livreCourant, PosteurEcritures posteur, JournauxRepository journaux, JournalRepository journal, DatesComptables dates, AuditService audit, ObjectMapper json) {
        this.repository = repository;
        this.correspondances = correspondances;
        this.livreCourant = livreCourant;
        this.posteur = posteur;
        this.journaux = journaux;
        this.journal = journal;
        this.dates = dates;
        this.audit = audit;
        this.json = json;
    }

    private Long livre() {
        return livreCourant.exiger().livreId();
    }

    private static String compteDe(CompteFinancier c, Map<String, String> table) {
        return table.getOrDefault("COMPTE_FINANCIER|" + c.id(), table.getOrDefault("BANQUE_DEFAUT|", "5141"));
    }

    private List<CompteRapprochementDTO> listerComptes(Long livreId) {
        Map<String, String> table = correspondances.assurer(livreId);
        List<CompteFinancier> financiers = repository.comptes(livreId);
        List<CompteRapprochementDTO> comptes = new ArrayList<>();
        for (CompteFinancier c : financiers) {
            String compte = compteDe(c, table);
            List<String> partage = financiers.stream().filter(a -> !a.id().equals(c.id()) && compteDe(a, table).equals(compte)).map(CompteFinancier::nom).toList();
            comptes.add(new CompteRapprochementDTO(c.id(), c.code(), c.nom(), compte, partage, repository.dernierEtat(livreId, c.id()).orElse(null)));
        }
        return comptes;
    }

    @Transactional
    public List<CompteRapprochementDTO> comptes() {
        return listerComptes(livre());
    }

    private CompteRapprochementDTO exigerCompte(Long livreId, Long id) {
        return listerComptes(livreId).stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Compte financier", "id", id));
    }

    @Transactional
    public EtatRapprochementDTO etat(Long compteFinancierId, LocalDate arrete) {
        Long livreId = livre();
        LocalDate date = arrete != null ? arrete : LocalDate.now();
        return calculer(livreId, exigerCompte(livreId, compteFinancierId), date);
    }

    private EtatRapprochementDTO calculer(Long livreId, CompteRapprochementDTO c, LocalDate date) {
        List<EcritureNonPointeeDTO> ecritures = repository.ecrituresNonPointees(livreId, c.compte(), date);
        List<ReleveNonPointeDTO> releve = repository.relevesNonPointes(livreId, c.id(), date);
        BigDecimal ecrituresDebit = ecritures.stream().filter(e -> e.debit().signum() > 0).map(EcritureNonPointeeDTO::reste).reduce(Z, BigDecimal::add);
        BigDecimal ecrituresCredit = ecritures.stream().filter(e -> e.credit().signum() > 0).map(EcritureNonPointeeDTO::reste).reduce(Z, BigDecimal::add);
        BigDecimal creditsReleve = releve.stream().filter(r -> r.credit().signum() > 0).map(ReleveNonPointeDTO::reste).reduce(Z, BigDecimal::add);
        BigDecimal debitsReleve = releve.stream().filter(r -> r.debit().signum() > 0).map(ReleveNonPointeDTO::reste).reduce(Z, BigDecimal::add);
        var lu = repository.releveALaDate(livreId, c.id(), date);
        BigDecimal soldeReleve = lu.map(r -> r.au() != null && !r.au().isAfter(date) && r.soldeFinal() != null ? r.soldeFinal() : r.soldeInitial().add(r.mouvements())).orElse(Z);
        BigDecimal comptable = repository.soldeComptable(livreId, c.compte(), date);
        EtatRapprochement e = EtatRapprochement.calculer(soldeReleve, ecrituresDebit, ecrituresCredit, comptable, creditsReleve, debitsReleve);
        return new EtatRapprochementDTO(c, date, lu.isPresent(), soldeReleve, ecrituresDebit, ecrituresCredit, e.soldeBanqueRectifie(), comptable, creditsReleve, debitsReleve, e.soldeComptableRectifie(), e.ecart(),
                repository.etatValide(livreId, c.id(), date), ecritures, releve);
    }

    /** Un débit de banque au journal correspond à un crédit du relevé. Le contrôle est là pour le message : le dernier mot revient à la base, à la validation de la transaction. */
    @Transactional
    public PointageDTO pointer(PointageRequest r) {
        Long livreId = livre();
        LigneReleve releve = repository.ligneReleve(livreId, r.ligneReleveId()).orElseThrow(() -> new ResourceNotFoundException("Ligne de relevé", "id", r.ligneReleveId()));
        LigneEcriture ligne = repository.ligneEcriture(livreId, r.ligneId()).orElseThrow(() -> new ResourceNotFoundException("Ligne d'écriture", "id", r.ligneId()));
        verifierPointage(livreId, releve, ligne, r.montant());
        if (r.datePointage().isBefore(releve.date()) || r.datePointage().isBefore(ligne.date())) {
            throw new CommonException("Un pointage ne précède ni l'écriture ni la ligne de relevé : au plus tôt le " + (releve.date().isAfter(ligne.date()) ? releve.date() : ligne.date()) + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_DATE");
        }
        if (repository.etatValideDepuis(livreId, releve.compteFinancierId(), r.datePointage()).isPresent()) {
            throw new CommonException("Un état validé couvre cette date : le pointage ne peut plus la changer.", HttpStatus.CONFLICT, "COMPTA_RAPPROCHEMENT_VALIDE");
        }
        String origine = "PROPOSITION".equals(r.origine()) ? "PROPOSITION" : "MANUEL";
        Long id = repository.pointer(livreId, r.ligneReleveId(), r.ligneId(), r.montant(), r.datePointage(), origine, livreCourant.auteur());
        return repository.pointages(livreId, releve.compteFinancierId()).stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    private void verifierPointage(Long livreId, LigneReleve releve, LigneEcriture ligne, BigDecimal montant) {
        if (montant.signum() <= 0) {
            throw new CommonException("Le montant pointé doit être positif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_MONTANT");
        }
        if ("A_NOUVEAU".equals(ligne.nature())) {
            throw new CommonException("Une ligne d'à-nouveau reprend un solde, pas une opération : elle ne se pointe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_A_NOUVEAU");
        }
        String compte = exigerCompte(livreId, releve.compteFinancierId()).compte();
        if (!compte.equals(ligne.compte())) {
            throw new CommonException("La ligne d'écriture est au compte " + ligne.compte() + ", ce relevé au compte " + compte + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_COMPTE");
        }
        if ((ligne.debit().signum() > 0) != (releve.credit().signum() > 0)) {
            throw new CommonException("Les sens se croisent : un débit au journal se pointe sur un crédit du relevé, et inversement.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_SENS");
        }
        BigDecimal resteEcriture = ligne.debit().add(ligne.credit()).subtract(ligne.pointe());
        BigDecimal resteReleve = releve.debit().add(releve.credit()).subtract(releve.pointe());
        if (montant.compareTo(resteEcriture) > 0 || montant.compareTo(resteReleve) > 0) {
            throw new CommonException("Pointage excessif : il reste " + MontantTexte.fr(resteEcriture) + " sur l'écriture et " + MontantTexte.fr(resteReleve) + " sur le relevé.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_EXCESSIF");
        }
    }

    @Transactional
    public void depointer(Long pointageId) {
        Long livreId = livre();
        var p = repository.pointage(livreId, pointageId).orElseThrow(() -> new ResourceNotFoundException("Pointage", "id", pointageId));
        LigneReleve releve = repository.ligneReleve(livreId, p.ligneReleveId()).orElse(null);
        if (releve != null && repository.etatValideDepuis(livreId, releve.compteFinancierId(), p.date()).isPresent()) {
            throw new CommonException("Un état validé couvre ce pointage : il ne se retire plus.", HttpStatus.CONFLICT, "COMPTA_RAPPROCHEMENT_VALIDE");
        }
        repository.supprimerPointage(livreId, pointageId);
    }

    @Transactional(readOnly = true)
    public List<PointageDTO> pointages(Long compteFinancierId) {
        Long livreId = livre();
        exigerCompte(livreId, compteFinancierId);
        return repository.pointages(livreId, compteFinancierId);
    }

    /** Même montant, sens croisés, date proche : une ligne de chaque côté au plus. Rien ne se pointe sans confirmation, et jamais d'un compte à l'autre. */
    @Transactional
    public List<PropositionPointageDTO> propositions(Long compteFinancierId, LocalDate arrete) {
        Long livreId = livre();
        LocalDate date = arrete != null ? arrete : LocalDate.now();
        EtatRapprochementDTO e = calculer(livreId, exigerCompte(livreId, compteFinancierId), date);
        List<PropositionPointageDTO> propositions = new ArrayList<>();
        Set<Long> prises = new HashSet<>();
        for (ReleveNonPointeDTO r : e.relevesNonPointes()) {
            EcritureNonPointeeDTO meilleure = null;
            long meilleurEcart = Long.MAX_VALUE;
            for (EcritureNonPointeeDTO l : e.ecrituresNonPointees()) {
                boolean sens = (l.debit().signum() > 0) == (r.credit().signum() > 0);
                long ecart = Math.abs(ChronoUnit.DAYS.between(l.date(), r.date()));
                if (sens && !prises.contains(l.ligneId()) && l.reste().compareTo(r.reste()) == 0 && ecart <= JOURS_DE_PROPOSITION && ecart < meilleurEcart) {
                    meilleure = l;
                    meilleurEcart = ecart;
                }
            }
            if (meilleure != null) {
                prises.add(meilleure.ligneId());
                propositions.add(new PropositionPointageDTO(r.ligneReleveId(), meilleure.ligneId(), r.reste(), r.date(), meilleure.date(), r.libelle(), meilleure.libelle(), (int) meilleurEcart));
            }
        }
        return propositions;
    }

    /** Frais depuis une ligne de relevé : l'écriture et le pointage dans la même transaction. Le compte est choisi par le comptable, jamais déduit du libellé. */
    @Transactional
    public PointageDTO comptabiliser(Long ligneReleveId, ComptabiliserReleveRequest r) {
        Long livreId = livre();
        LigneReleve releve = repository.ligneReleve(livreId, ligneReleveId).orElseThrow(() -> new ResourceNotFoundException("Ligne de relevé", "id", ligneReleveId));
        BigDecimal reste = releve.debit().add(releve.credit()).subtract(releve.pointe());
        if (reste.signum() <= 0) {
            throw new CommonException("Cette ligne de relevé est déjà pointée.", HttpStatus.CONFLICT, "COMPTA_POINTAGE_EXCESSIF");
        }
        BigDecimal tva = r.montantTva() == null ? Z : r.montantTva();
        if (tva.signum() < 0 || tva.compareTo(reste) >= 0) {
            throw new CommonException("La TVA doit être comprise entre 0 et le montant de la ligne.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_POINTAGE_MONTANT");
        }
        String compteBanque = exigerCompte(livreId, releve.compteFinancierId()).compte();
        boolean debitBanque = releve.credit().signum() > 0;
        BigDecimal ht = reste.subtract(tva);
        List<NouvelleLigne> lignes = new ArrayList<>();
        if (debitBanque) {
            lignes.add(ligne(compteBanque, reste, Z, null));
            lignes.add(ligne(r.compte(), Z, ht, null));
            if (tva.signum() > 0) {
                lignes.add(ligne("4455", Z, tva, r.tauxTva()));
            }
        } else {
            lignes.add(ligne(r.compte(), ht, Z, null));
            if (tva.signum() > 0) {
                lignes.add(ligne("34552", tva, Z, r.tauxTva()));
            }
            lignes.add(ligne(compteBanque, Z, reste, null));
        }
        JournalDTO bq = journaux.parType(livreId, "BANQUE").orElseThrow(() -> new CommonException("Le journal de banque n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        String ref = releve.reference() != null && !releve.reference().isBlank() ? releve.reference() : "REL-" + ligneReleveId;
        LocalDate ecritureDate = dates.resoudre(livreId, bq.id(), releve.date());
        var ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(bq.id(), ecritureDate, releve.date(), ref, releve.libelle(), "COURANTE", "RAPPROCHEMENT", null, null, null, false, null, null, lignes));
        LigneJournal banque = journal.lignesBrutes(livreId, ecriture.id()).stream().filter(l -> compteBanque.equals(l.compte())).findFirst().orElseThrow();
        // La date d'écriture peut suivre celle du relevé (chronologie du journal) : à une date antérieure, ni l'écriture ni le pointage n'existent encore.
        LocalDate datePointage = ecritureDate.isAfter(releve.date()) ? ecritureDate : releve.date();
        Long id = repository.pointer(livreId, ligneReleveId, banque.id(), reste, datePointage, "MANUEL", livreCourant.auteur());
        audit.logAction(ActionAudit.MODIFICATION, "compta_pointages", id, "Ligne de relevé " + ligneReleveId + " comptabilisée sur " + r.compte());
        return repository.pointages(livreId, releve.compteFinancierId()).stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    private static NouvelleLigne ligne(String compte, BigDecimal debit, BigDecimal credit, BigDecimal taux) {
        return new NouvelleLigne(compte, null, debit, credit, null, null, null, false, null, null, taux, null, null, null);
    }

    /** Un état validé tombe juste. Deux banques sur un même compte comptable ne se valident pas : leur solde serait fusionné. */
    @Transactional
    public EtatValideDTO valider(Long compteFinancierId, ValiderEtatRequest r) {
        Long livreId = livre();
        CompteRapprochementDTO c = exigerCompte(livreId, compteFinancierId);
        if (!c.partageAvec().isEmpty()) {
            throw new CommonException(c.nom() + " partage le compte " + c.compte() + " avec " + String.join(", ", c.partageAvec()) + " : donnez à chaque banque son sous-compte dans les correspondances.", HttpStatus.CONFLICT, "COMPTA_RAPPROCHEMENT_FUSION");
        }
        EtatRapprochementDTO e = calculer(livreId, c, r.arrete());
        if (!e.releveConnu()) {
            throw new CommonException("Aucun relevé connu à cette date : l'état ne se valide pas sans relevé.", HttpStatus.CONFLICT, "COMPTA_RAPPROCHEMENT_SANS_RELEVE");
        }
        if (e.ecart().signum() != 0) {
            throw new CommonException("Écart de " + MontantTexte.fr(e.ecart()) + " : un état validé tombe juste.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_RAPPROCHEMENT_ECART");
        }
        if (e.valide()) {
            throw new CommonException("L'état de cette date est déjà validé.", HttpStatus.CONFLICT, "COMPTA_RAPPROCHEMENT_VALIDE");
        }
        String contenu;
        try {
            contenu = json.writeValueAsString(e);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
        Long id = repository.valider(livreId, c.compte(), c.id(), r.arrete(), e.soldeComptable(), e.soldeReleve(), e.soldeComptableRectifie(), e.soldeBanqueRectifie(), contenu, livreCourant.auteur());
        audit.logAction(ActionAudit.VALIDATION, "compta_rapprochements", id, "État de " + c.nom() + " au " + r.arrete());
        return repository.etats(livreId, c.id()).stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<EtatValideDTO> etats(Long compteFinancierId) {
        Long livreId = livre();
        exigerCompte(livreId, compteFinancierId);
        return repository.etats(livreId, compteFinancierId);
    }
}
