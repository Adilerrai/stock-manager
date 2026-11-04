package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.DeclarationsDtos.AttestationDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.AttestationRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasLigneDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasMoisDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasMoisResumeDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasRetenueRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.TiersFiscalDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.DelaisComptaRepository;
import com.gestion.repository.compta.DelaisComptaRepository.Complement;
import com.gestion.repository.compta.DelaisComptaRepository.FactureLue;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.ParametreFiscalRepository;
import com.gestion.repository.compta.RasComptaRepository;
import com.gestion.repository.compta.RasComptaRepository.Operation;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.repository.compta.TvaRepository;
import com.gestion.service.AuditService;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La retenue sur les règlements fournisseurs, mois par mois et sur le montant de chaque règlement. La nature de l'achat, la forme du fournisseur
 * et l'attestation viennent de ce qui a été saisi : rien n'est supposé. La retenue se passe D 4411 / C 4458 ; le règlement du commercial a déjà passé D 4411 / C banque.
 */
@Service
public class RasComptaService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final Set<String> NATURES_RETENUES = Set.of("BIEN_EQUIPEMENT", "TRAVAUX", "SERVICE");

    private final RasComptaRepository repository;
    private final DelaisComptaRepository delais;
    private final DelaisComptaService delaisService;
    private final TvaRepository tva;
    private final TiersRepository tiers;
    private final ParametreFiscalRepository parametres;
    private final PosteurEcritures posteur;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final DatesComptables dates;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public RasComptaService(RasComptaRepository repository, DelaisComptaRepository delais, DelaisComptaService delaisService, TvaRepository tva, TiersRepository tiers, ParametreFiscalRepository parametres, PosteurEcritures posteur,
                            JournauxRepository journaux, JournalRepository journal, DatesComptables dates, LivreCourantService livreCourant, AuditService audit) {
        this.repository = repository;
        this.delais = delais;
        this.delaisService = delaisService;
        this.tva = tva;
        this.tiers = tiers;
        this.parametres = parametres;
        this.posteur = posteur;
        this.journaux = journaux;
        this.journal = journal;
        this.dates = dates;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    private Long livre() {
        return livreCourant.exiger().livreId();
    }

    private static void exigerMois(int mois) {
        if (mois < 1 || mois > 12) {
            throw new CommonException("Le mois va de 1 à 12.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_RAS_MOIS");
        }
    }

    // --- Lecture

    @Transactional
    public List<RasMoisResumeDTO> annee(int annee) {
        Long livreId = livre();
        Set<LocalDate> feries = parametres.joursFeries();
        List<RasMoisResumeDTO> liste = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDate du = LocalDate.of(annee, m, 1);
            List<RasLigneDTO> lignes = lignes(livreId, du, Map.of());
            BigDecimal retenu = lignes.stream().filter(l -> "RETENUE".equals(l.etat())).map(RasLigneDTO::montantRetenu).reduce(Z, BigDecimal::add);
            liste.add(new RasMoisResumeDTO(annee, m, limite(du, feries), (int) lignes.stream().map(RasLigneDTO::reglementId).distinct().count(), compte(lignes, "A_QUALIFIER"), compte(lignes, "A_RETENIR"), retenu));
        }
        return liste;
    }

    private static int compte(List<RasLigneDTO> lignes, String etat) {
        return (int) lignes.stream().filter(l -> etat.equals(l.etat())).count();
    }

    private static LocalDate limite(LocalDate du, Set<LocalDate> feries) {
        return DelaisCalcul.reporter(TvaCalcul.dateLimite(du.withDayOfMonth(du.lengthOfMonth())), feries);
    }

    @Transactional
    public RasMoisDTO mois(int annee, int mois) {
        exigerMois(mois);
        Long livreId = livre();
        LocalDate du = LocalDate.of(annee, mois, 1);
        List<RasLigneDTO> lignes = lignes(livreId, du, Map.of());
        BigDecimal retenu = lignes.stream().filter(l -> "RETENUE".equals(l.etat())).map(RasLigneDTO::montantRetenu).reduce(Z, BigDecimal::add);
        return new RasMoisDTO(annee, mois, limite(du, parametres.joursFeries()), retenu, RasCalcul.versement(retenu), lignes);
    }

    private record Tranche(TvaRepository.Reglement reglement, Long factureId, BigDecimal montant) {
    }

    /** Dans l'ordre des paiements : le cumul du mois par fournisseur compte tous ses services, y compris ceux de plus de 5 000 DH (RAS-05). */
    private List<RasLigneDTO> lignes(Long livreId, LocalDate du, Map<String, BigDecimal> bruts) {
        LocalDate au = du.withDayOfMonth(du.lengthOfMonth());
        List<TvaRepository.Reglement> reglements = tva.reglements(livreId, du, au);
        Map<Long, List<TvaRepository.AffectationReglement>> affectees = tva.affectationsReglements(livreId, reglements.stream().map(TvaRepository.Reglement::id).toList()).stream()
                .collect(Collectors.groupingBy(TvaRepository.AffectationReglement::reglementId));
        List<Tranche> tranches = new ArrayList<>();
        for (TvaRepository.Reglement r : reglements) {
            List<TvaRepository.AffectationReglement> a = affectees.get(r.id());
            if (a != null) {
                a.forEach(x -> tranches.add(new Tranche(r, x.factureAchatId(), x.montant())));
            } else {
                tranches.add(new Tranche(r, r.factureAchatId(), r.montant()));
            }
        }
        List<Long> ids = tranches.stream().map(Tranche::factureId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, FactureLue> factures = delais.factures(livreId, ids).stream().collect(Collectors.toMap(FactureLue::id, Function.identity()));
        Map<Long, Complement> complements = delais.complements(livreId, ids).stream().collect(Collectors.toMap(Complement::factureId, Function.identity()));
        Map<String, Operation> operations = repository.operations(livreId, du, au).stream().collect(Collectors.toMap(o -> cle(o.reglementId(), o.factureId()), Function.identity()));
        Map<Long, BigDecimal> cumuls = new HashMap<>();
        Map<Long, List<AttestationDTO>> attestations = new HashMap<>();
        List<RasLigneDTO> lignes = new ArrayList<>();
        for (Tranche t : tranches) {
            TvaRepository.Reglement r = t.reglement();
            FactureLue f = t.factureId() == null ? null : factures.get(t.factureId());
            if (f == null) {
                lignes.add(new RasLigneDTO(r.id(), r.numero(), r.date(), t.montant(), null, null, null, null, null, "ABSENTE", null, t.montant(), Z, Z, Z, Z, Z, Z, null, "Règlement sans facture : pas de calcul.", "SANS_FACTURE", null, null));
                continue;
            }
            Long tiersId = delaisService.assurerTiers(livreId, f);
            Boolean physique = repository.personnePhysique(livreId, tiersId).orElse(null);
            String etatAttestation = etatAttestation(attestations.computeIfAbsent(tiersId, x -> repository.attestations(livreId, x)), r.date());
            Complement c = complements.get(f.id());
            String nature = c == null ? null : c.nature();
            Operation o = operations.get(cle(r.id(), f.id()));
            BigDecimal brut = o != null ? o.ht().add(o.tva()) : bruts.getOrDefault(cle(r.id(), f.id()), t.montant());
            BigDecimal cumul = cumuls.getOrDefault(f.fournisseurId(), Z);
            if ("SERVICE".equals(nature)) {
                cumul = cumul.add(brut);
                cumuls.put(f.fournisseurId(), cumul);
            }
            if (o != null) {
                lignes.add(new RasLigneDTO(r.id(), r.numero(), r.date(), t.montant(), f.id(), f.numero(), tiersId, f.fournisseur(), physique, etatAttestation, nature, brut, cumul, o.ht(), o.tauxTva(), o.tva(), o.tauxRetenue(), o.retenu(), o.article(),
                        "Retenue passée.", "RETENUE", o.id(), o.ecritureId()));
                continue;
            }
            BigDecimal taux = tauxEffectif(f);
            Optional<RasCalcul.Bareme> bareme = bareme(r.date());
            RasCalcul.Resultat res = bareme.isEmpty()
                    ? new RasCalcul.Resultat(false, brut, Z, Z, Z, null, "Le régime s'applique aux opérations depuis le 01/07/2024.")
                    : RasCalcul.calculer(new RasCalcul.Entree(nature, physique, "VALIDE".equals(etatAttestation), seuilAtteint(livreId, r.date()), brut, taux, cumul, bareme.get()));
            String etat = res.aQualifier() ? "A_QUALIFIER" : res.aRetenir() ? "A_RETENIR" : "SANS_RETENUE";
            lignes.add(new RasLigneDTO(r.id(), r.numero(), r.date(), t.montant(), f.id(), f.numero(), tiersId, f.fournisseur(), physique, etatAttestation, nature, brut, cumul, res.ht(), taux, res.tva(), res.tauxRetenue(), res.retenue(), res.article(), res.raison(), etat, null, null));
        }
        return lignes;
    }

    private static String cle(Long reglementId, Long factureId) {
        return reglementId + "|" + factureId;
    }

    /** Une facture à plusieurs taux prend son taux effectif : sa TVA sur son hors taxes. */
    private static BigDecimal tauxEffectif(FactureLue f) {
        if (f.ht() == null || f.tva() == null || f.ht().signum() <= 0) {
            return Z;
        }
        return f.tva().multiply(CENT).divide(f.ht(), 2, RoundingMode.HALF_UP);
    }

    private Optional<RasCalcul.Bareme> bareme(LocalDate date) {
        var avec = parametres.aLaDate("RAS_TAUX", "AVEC_ATTESTATION", date);
        var sans = parametres.aLaDate("RAS_TAUX", "SANS_ATTESTATION", date);
        var petite = parametres.aLaDate("RAS_PETITE_PRESTATION", "MONTANT_TTC", date);
        var cumul = parametres.aLaDate("RAS_PETITE_PRESTATION", "CUMUL_MOIS_TTC", date);
        if (avec.isEmpty() || sans.isEmpty() || petite.isEmpty() || cumul.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RasCalcul.Bareme(avec.get().valeur(), sans.get().valeur(), petite.get().valeur(), cumul.get().valeur()));
    }

    /** Art. 247-XXXXVI : sur le chiffre d'affaires du dernier exercice clos. Pas d'exercice antérieur dans le livre : le seuil ne se suppose pas atteint. */
    private boolean seuilAtteint(Long livreId, LocalDate date) {
        return parametres.aLaDate("RAS_SEUIL_CA", "", date).map(p -> delaisService.chiffreAffaires(livreId, date).filter(ca -> ca.compareTo(p.valeur()) >= 0).isPresent()).orElse(false);
    }

    /** Art. 117-IV et NC 735 : délivrée depuis moins de six mois au paiement, et vérifiée sur le site de la DGI. */
    private String etatAttestation(List<AttestationDTO> liste, LocalDate paiement) {
        Optional<AttestationDTO> a = liste.stream().filter(x -> !x.dateDelivrance().isAfter(paiement)).findFirst();
        if (a.isEmpty()) {
            return "ABSENTE";
        }
        int mois = parametres.aLaDate("RAS_ATTESTATION_MOIS", "", paiement).map(p -> p.valeur().intValue()).orElse(0);
        if (!a.get().dateDelivrance().plusMonths(mois).isAfter(paiement)) {
            return "PERIMEE";
        }
        return a.get().verifieeSurDgi() ? "VALIDE" : "NON_VERIFIEE";
    }

    // --- Qualification

    @Transactional
    public void modifierNature(Long factureId, String nature) {
        Long livreId = livre();
        if (!RasCalcul.NATURES.contains(nature)) {
            throw new CommonException("Nature inconnue : " + nature + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_RAS_NATURE");
        }
        FactureLue f = delais.facture(livreId, factureId).orElseThrow(() -> new ResourceNotFoundException("Facture d'achat", "id", factureId));
        delais.ecrireNature(livreId, factureId, f.date(), nature);
        audit.logAction(ActionAudit.MODIFICATION, "compta_dp_factures", factureId, "Facture " + f.numero() + " : nature " + nature);
    }

    // --- Retenue

    @Transactional
    public RasMoisDTO retenir(Long reglementId, RasRetenueRequest r) {
        Long livreId = livre();
        TvaRepository.Reglement reglement = tva.reglementsParIds(livreId, List.of(reglementId)).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Règlement", "id", reglementId));
        LocalDate du = reglement.date().withDayOfMonth(1);
        Map<String, BigDecimal> bruts = new HashMap<>();
        if (r.montantBrut() != null) {
            if (r.montantBrut().signum() <= 0) {
                throw new CommonException("Le montant du règlement doit être positif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_RAS_MONTANT");
            }
            bruts.put(cle(reglementId, r.factureAchatId()), r.montantBrut());
        }
        RasLigneDTO l = lignes(livreId, du, bruts).stream().filter(x -> reglementId.equals(x.reglementId()) && r.factureAchatId().equals(x.factureAchatId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Règlement de cette facture", "id", reglementId));
        if ("RETENUE".equals(l.etat())) {
            throw new CommonException("La retenue de ce règlement est déjà passée.", HttpStatus.CONFLICT, "COMPTA_RAS_DEJA");
        }
        if (!"A_RETENIR".equals(l.etat()) || !NATURES_RETENUES.contains(l.nature())) {
            throw new CommonException(l.raison(), HttpStatus.CONFLICT, "COMPTA_RAS_SANS_RETENUE");
        }
        JournalDTO od = journaux.parType(livreId, "OD").orElseThrow(() -> new CommonException("Le journal OD n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        Long ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), dates.resoudre(livreId, od.id(), l.datePaiement()), l.datePaiement(), l.numeroReglement(), "Retenue à la source sur TVA, " + l.numeroFacture(), "COURANTE", "TVA", null, null, null, false, null, null,
                List.of(new NouvelleLigne("4411", l.tiersId(), l.montantRetenu(), Z, null, null, null, false, null, null, null, null, null, null), new NouvelleLigne("4458", null, Z, l.montantRetenu(), null, null, null, false, null, null, null, null, null, null)))).id();
        String natureOperation = "SERVICE".equals(l.nature()) ? "SERVICE" : l.nature();
        Long id = repository.ajouterOperation(livreId, new Operation(null, reglementId, r.factureAchatId(), l.tiersId(), natureOperation, l.datePaiement(), l.montantHt(), l.tauxTva(), l.montantTva(), l.tauxRetenue(), l.montantRetenu(), l.article(), ecriture));
        audit.logAction(ActionAudit.CREATION, "compta_ras_operations", id, "Retenue sur " + l.numeroReglement() + " : " + l.montantRetenu());
        return mois(du.getYear(), du.getMonthValue());
    }

    @Transactional
    public RasMoisDTO annuler(Long operationId, MotifRequest r) {
        Long livreId = livre();
        Operation o = repository.operation(livreId, operationId).orElseThrow(() -> new ResourceNotFoundException("Retenue", "id", operationId));
        if (o.ecritureId() != null) {
            EcritureDTO origine = journal.lire(livreId, o.ecritureId()).orElseThrow();
            List<NouvelleLigne> miroir = new ArrayList<>();
            for (LigneJournal l : journal.lignesBrutes(livreId, o.ecritureId())) {
                miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
            }
            posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), dates.resoudre(livreId, origine.journal().id(), origine.dateEcriture()), origine.datePiece(), origine.refPiece(), "Contre-passation de " + origine.numeroComplet(),
                    "COURANTE", "TVA", o.ecritureId(), null, r.motif().trim(), false, null, null, miroir));
        }
        repository.retirerOperation(livreId, operationId);
        audit.logAction(ActionAudit.ANNULATION, "compta_ras_operations", operationId, "Annulation : " + r.motif().trim());
        return mois(o.datePaiement().getYear(), o.datePaiement().getMonthValue());
    }

    // --- Bordereau mensuel

    @Transactional
    public byte[] bordereauXlsx(int annee, int mois) {
        exigerMois(mois);
        Long livreId = livre();
        LocalDate du = LocalDate.of(annee, mois, 1);
        List<RasLigneDTO> lignes = lignes(livreId, du, Map.of()).stream().filter(l -> "RETENUE".equals(l.etat())).toList();
        Map<Long, TiersDTO> fiches = new HashMap<>();
        lignes.forEach(l -> fiches.computeIfAbsent(l.tiersId(), id -> tiers.lire(livreId, id).orElseThrow()));
        BigDecimal total = lignes.stream().map(RasLigneDTO::montantRetenu).reduce(Z, BigDecimal::add);
        return TvaFichiers.classeur(c -> {
            Sheet f = c.createSheet("Bordereau");
            TvaFichiers.ligne(f, 0, "IF du fournisseur", "ICE", "Nom ou raison sociale", "N° de facture", "Date de paiement", "Nature de l'opération", "Montant hors TVA", "Taux TVA", "Montant TVA", "Taux de la retenue", "Montant de la retenue", "Article");
            int n = 1;
            for (RasLigneDTO l : lignes) {
                TiersDTO t = fiches.get(l.tiersId());
                TvaFichiers.ligne(f, n++, t.identifiantFiscal(), t.ice(), t.nom(), l.numeroFacture(), l.datePaiement(), l.nature(), l.montantHt(), l.tauxTva(), l.montantTva(), l.tauxRetenue(), l.montantRetenu(), l.article());
            }
            TvaFichiers.ligne(f, n + 1, "Total", null, null, null, null, null, null, null, null, null, total);
            TvaFichiers.ligne(f, n + 2, "Versement au dirham supérieur (art. 167)", null, null, null, null, null, null, null, null, null, RasCalcul.versement(total));
        });
    }

    // --- Fiche fiscale du fournisseur

    @Transactional(readOnly = true)
    public TiersFiscalDTO ficheTiers(Long tiersId) {
        Long livreId = livre();
        TiersDTO t = tiers.lire(livreId, tiersId).orElseThrow(() -> new ResourceNotFoundException("Tiers", "id", tiersId));
        return new TiersFiscalDTO(t.id(), t.nom(), repository.personnePhysique(livreId, tiersId).orElse(null), repository.attestations(livreId, tiersId));
    }

    @Transactional
    public TiersFiscalDTO modifierForme(Long tiersId, boolean physique) {
        Long livreId = livre();
        tiers.lire(livreId, tiersId).orElseThrow(() -> new ResourceNotFoundException("Tiers", "id", tiersId));
        repository.modifierForme(livreId, tiersId, physique);
        audit.logAction(ActionAudit.MODIFICATION, "compta_tiers", tiersId, physique ? "Personne physique" : "Personne morale");
        return ficheTiers(tiersId);
    }

    @Transactional
    public TiersFiscalDTO ajouterAttestation(Long tiersId, AttestationRequest r) {
        Long livreId = livre();
        tiers.lire(livreId, tiersId).orElseThrow(() -> new ResourceNotFoundException("Tiers", "id", tiersId));
        repository.ajouterAttestation(livreId, tiersId, r, livreCourant.auteur());
        audit.logAction(ActionAudit.CREATION, "compta_tiers_attestations", tiersId, "Attestation du " + r.dateDelivrance());
        return ficheTiers(tiersId);
    }

    @Transactional
    public TiersFiscalDTO retirerAttestation(Long tiersId, Long attestationId) {
        Long livreId = livre();
        if (repository.retirerAttestation(livreId, tiersId, attestationId) == 0) {
            throw new ResourceNotFoundException("Attestation", "id", attestationId);
        }
        audit.logAction(ActionAudit.SUPPRESSION, "compta_tiers_attestations", attestationId, "Attestation retirée");
        return ficheTiers(tiersId);
    }
}
