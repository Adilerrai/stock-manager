package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ClassementRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.LigneClasseeDTO;
import com.gestion.repository.compta.RapprochementComptaRepository;
import com.gestion.persistent.dto.compta.TvaDtos.TvaAccepterCodeRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaAccepterRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaComptesControlesDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaControleDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCorrectionLigneRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeclarationDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDepotRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEncaissementDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEtatFichierRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaFichierDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaIdentiteRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaLigneDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaOperationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPaiementRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPeriodeDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPieceVisee;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRattrapageDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRattraperRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRubriqueDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.EcritureEnregistree;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.repository.compta.LivreRepository.LigneLivre;
import com.gestion.repository.compta.TvaRepository;
import com.gestion.repository.compta.TvaRepository.Declaration;
import com.gestion.repository.compta.TvaRepository.DepenseVentilee;
import com.gestion.repository.compta.TvaRepository.LigneRetenue;
import com.gestion.repository.compta.TvaRepository.OperationCollectee;
import com.gestion.repository.compta.TvaRepository.OperationDeductible;
import com.gestion.repository.compta.TvaRepository.Reglement;
import com.gestion.service.AuditService;
import com.gestion.service.compta.FormulaireTva.LigneFormulaire;
import com.gestion.service.compta.FormulaireTva.Rubrique;
import com.gestion.service.compta.TvaCalcul.BaseTaxe;
import com.gestion.service.compta.TvaCalcul.LigneCalculee;
import com.gestion.service.compta.TvaCalcul.Periode;
import com.gestion.service.compta.TvaCalcul.Tranche;
import com.gestion.service.compta.TvaOperations.Surcharges;

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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Le cycle de la déclaration : brouillon recalculable, validée et figée, état DGI recopié, déposée, payée.
 * L'écriture de liquidation se passe au dépôt, au vu de la déclaration déposée ; le calcul de la TVA, lui, part des paiements.
 */
@Service
public class TvaDeclarationService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> FAITS = Set.of("ENCAISSEMENT", "DEBIT");
    private static final Set<String> PERIODICITES = Set.of("MENSUELLE", "TRIMESTRIELLE");
    private static final Set<String> TRAITEMENTS = Set.of("TAXABLE", "HORS_CHAMP", "EXONERE_91", "EXONERE_92", "SUSPENSION_94");
    private static final Set<String> LIGNES_SAISIES = Set.of("010", "020", "030", "040", "050", "120", "131", "170", "180", "183", "185", "186", "187");

    private final TvaRepository repository;
    private final TvaOperations operations;
    private final TvaControles controles;
    private final TvaFichiers fichiers;
    private final FormulaireTva formulaire;
    private final LivreRepository livres;
    private final LivreCourantService livreCourant;
    private final PosteurEcritures posteur;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final DatesComptables dates;
    private final CorrespondanceService correspondances;
    private final AuditService audit;
    private final RapprochementComptaRepository rapprochement;

    public TvaDeclarationService(RapprochementComptaRepository rapprochement, TvaRepository repository, TvaOperations operations, TvaControles controles, TvaFichiers fichiers, FormulaireTva formulaire, LivreRepository livres, LivreCourantService livreCourant,
                                 PosteurEcritures posteur, JournauxRepository journaux, JournalRepository journal, DatesComptables dates, CorrespondanceService correspondances, AuditService audit) {
        this.rapprochement = rapprochement;
        this.repository = repository;
        this.operations = operations;
        this.controles = controles;
        this.fichiers = fichiers;
        this.formulaire = formulaire;
        this.livres = livres;
        this.livreCourant = livreCourant;
        this.posteur = posteur;
        this.journaux = journaux;
        this.journal = journal;
        this.dates = dates;
        this.correspondances = correspondances;
        this.audit = audit;
    }

    private Long livre() {
        return livreCourant.exiger().livreId();
    }

    // --- Paramètres

    @Transactional(readOnly = true)
    public TvaParametresDTO parametres(int annee) {
        return parametresDe(livre(), annee);
    }

    private TvaParametresDTO parametresDe(Long livreId, int annee) {
        return repository.parametres(livreId, annee).orElseGet(() -> new TvaParametresDTO(annee, true, "ENCAISSEMENT", "MENSUELLE", new BigDecimal("100.00"), false, null, Z, false));
    }

    @Transactional
    public TvaParametresDTO enregistrerParametres(int annee, TvaParametresRequest r) {
        Long livreId = livre();
        if (!FAITS.contains(r.faitGenerateur())) {
            throw new CommonException("Fait générateur inconnu : " + r.faitGenerateur() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_PARAMETRE");
        }
        if (!PERIODICITES.contains(r.periodicite())) {
            throw new CommonException("Périodicité inconnue : " + r.periodicite() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_PARAMETRE");
        }
        if (r.prorata().signum() < 0 || r.prorata().compareTo(new BigDecimal("100")) > 0) {
            throw new CommonException("Le prorata se situe entre 0 et 100.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_PARAMETRE");
        }
        if (r.creditInitial().signum() < 0) {
            throw new CommonException("Le crédit initial ne peut pas être négatif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_PARAMETRE");
        }
        if (r.premierePeriode() != null && (r.premierePeriode() < 1 || r.premierePeriode() > TvaCalcul.nombreDePeriodes(r.periodicite()))) {
            throw new CommonException("La première période déclarée n'existe pas pour cette périodicité.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_PARAMETRE");
        }
        // Un changement de régime ou de périodicité vaut pour l'année entière : TVA-02 et art. 108.
        var existants = repository.parametres(livreId, annee);
        if (existants.isPresent() && repository.compter(livreId, annee) > 0
                && (!existants.get().faitGenerateur().equals(r.faitGenerateur()) || !existants.get().periodicite().equals(r.periodicite()))) {
            throw new CommonException("Des déclarations existent déjà pour " + annee + " : le fait générateur et la périodicité ne changent plus cette année.", HttpStatus.CONFLICT, "COMPTA_TVA_PARAMETRE");
        }
        repository.ecrireParametres(livreId, annee, r.assujetti(), r.faitGenerateur(), r.periodicite(), r.prorata().setScale(2, RoundingMode.HALF_UP), Boolean.TRUE.equals(r.prorataProvisoire()), r.premierePeriode(), r.creditInitial());
        audit.logAction(ActionAudit.MODIFICATION, "compta_tva_parametres", (long) annee, "Paramètres TVA " + annee + " : " + r.faitGenerateur() + ", " + r.periodicite() + ", prorata " + r.prorata());
        return parametresDe(livreId, annee);
    }

    @Transactional(readOnly = true)
    public List<TvaRubriqueDTO> rubriques() {
        return formulaire.rubriques().stream().map(r -> new TvaRubriqueDTO(r.code(), r.libelle(), r.compteTva(), List.copyOf(r.taux().keySet()))).toList();
    }

    // --- Périodes

    /** Une ligne par mois ou trimestre : la dernière version de chaque déclaration, et les périodes commencées qui n'en ont pas encore. */
    @Transactional(readOnly = true)
    public List<TvaPeriodeDTO> periodes(int annee) {
        Long livreId = livre();
        TvaParametresDTO p = parametresDe(livreId, annee);
        Map<Integer, Declaration> dernieres = new HashMap<>();
        repository.lister(livreId, annee).forEach(d -> dernieres.put(d.numeroPeriode(), d));
        LocalDate aujourdhui = LocalDate.now();
        List<TvaPeriodeDTO> lignes = new ArrayList<>();
        int debut = p.premierePeriode() == null ? 1 : p.premierePeriode();
        for (int n = debut; n <= TvaCalcul.nombreDePeriodes(p.periodicite()); n++) {
            Declaration d = dernieres.get(n);
            if (d != null) {
                boolean retard = !Set.of("DEPOSEE", "PAYEE").contains(d.statut()) && aujourdhui.isAfter(d.dateLimite());
                lignes.add(new TvaPeriodeDTO(d.id(), d.annee(), n, d.du(), d.au(), d.periodicite(), d.version(), d.statut(), d.dateLimite(), d.tvaExigible(), d.tvaDeductible(), d.tvaDue(), d.creditPeriode(), retard));
                continue;
            }
            LocalDate du = TvaCalcul.debutDePeriode(annee, p.periodicite(), n);
            if (du.isAfter(aujourdhui)) {
                break;
            }
            LocalDate au = TvaCalcul.finDePeriode(annee, p.periodicite(), n);
            LocalDate limite = TvaCalcul.dateLimite(au);
            lignes.add(new TvaPeriodeDTO(null, annee, n, du, au, p.periodicite(), 0, "A_CREER", limite, Z, Z, Z, Z, aujourdhui.isAfter(limite)));
        }
        return lignes;
    }

    @Transactional
    public TvaDeclarationDTO creer(TvaCreationRequest r) {
        Long livreId = livre();
        var p = repository.parametres(livreId, r.annee()).orElseThrow(() -> new CommonException("Renseignez d'abord les paramètres TVA de " + r.annee() + ".", HttpStatus.CONFLICT, "COMPTA_TVA_PARAMETRES_ABSENTS"));
        if (!p.assujetti()) {
            throw new CommonException("Le dossier n'est pas assujetti à la TVA en " + r.annee() + ".", HttpStatus.CONFLICT, "COMPTA_TVA_NON_ASSUJETTI");
        }
        if (r.numeroPeriode() < 1 || r.numeroPeriode() > TvaCalcul.nombreDePeriodes(p.periodicite())) {
            throw new CommonException("La période " + r.numeroPeriode() + " n'existe pas pour une déclaration " + p.periodicite().toLowerCase() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_PERIODE");
        }
        boolean existe = repository.lister(livreId, r.annee()).stream().anyMatch(d -> d.numeroPeriode() == r.numeroPeriode());
        if (existe) {
            throw new CommonException("La déclaration de cette période existe déjà.", HttpStatus.CONFLICT, "COMPTA_TVA_PERIODE_EXISTE");
        }
        LocalDate du = TvaCalcul.debutDePeriode(r.annee(), p.periodicite(), r.numeroPeriode());
        LocalDate au = TvaCalcul.finDePeriode(r.annee(), p.periodicite(), r.numeroPeriode());
        Long id = repository.creer(livreId, r.annee(), r.numeroPeriode(), du, au, p.periodicite(), TvaCalcul.regimeDgi(p.periodicite()), p.faitGenerateur(), TvaCalcul.dateLimite(au));
        audit.logAction(ActionAudit.CREATION, "compta_tva_declarations", id, "Déclaration TVA du " + du + " au " + au);
        recalculer(livreId, repository.lire(livreId, id).orElseThrow(), p);
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public TvaDeclarationDTO lire(Long id) {
        Long livreId = livre();
        return dto(livreId, exiger(livreId, id));
    }

    @Transactional
    public TvaDeclarationDTO calculer(Long id) {
        Long livreId = livre();
        Declaration d = exigerBrouillon(livreId, id);
        recalculer(livreId, d, parametresDe(livreId, d.annee()));
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    private Declaration exiger(Long livreId, Long id) {
        return repository.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Déclaration de TVA", "id", id));
    }

    private Declaration exigerBrouillon(Long livreId, Long id) {
        Declaration d = exiger(livreId, id);
        if (!"BROUILLON".equals(d.statut())) {
            throw new CommonException("La déclaration est " + d.statut() + " : sa photo est figée. Dévalidez-la d'abord, si elle n'a pas été envoyée.", HttpStatus.CONFLICT, "COMPTA_TVA_FIGEE");
        }
        return d;
    }

    private void recalculer(Long livreId, Declaration d, TvaParametresDTO p) {
        String remiseMode = livres.lire(livreId).remiseGlobaleMode();
        Surcharges s = operations.lireSurcharges(livreId, d.id());
        repository.viderOperations(livreId, d.id(), d.du());
        Periode travail = new Periode(Z, new HashMap<>(), new HashMap<>(), new HashMap<>());
        operations.collecter(livreId, d, remiseMode, s, travail);
        operations.deduire(livreId, d, p, s);
        operations.regulariser(livreId, d, travail);
        ecrireFormulaire(livreId, d, p, travail, remiseMode);
    }

    /** Refait les lignes depuis les opérations, en gardant les corrections motivées. Sert aussi après une correction ou un rattrapage. */
    private void ecrireFormulaire(Long livreId, Declaration d, TvaParametresDTO p, Periode travail, String remiseMode) {
        List<OperationDeductible> deductibles = repository.operationsDeductibles(livreId, d.id());
        Periode periode = new Periode(travail.horsTaxable().getOrDefault("010", Z), travail.horsTaxable(), travail.ventes(), operations.deductions(deductibles));
        var precedente = repository.precedente(livreId, d.du());
        BigDecimal creditAttendu = precedente.map(Declaration::creditPeriode).orElse(p.creditInitial());
        Map<String, BaseTaxe> retenues = new HashMap<>();
        Map<String, String> motifs = new HashMap<>();
        for (LigneRetenue l : repository.lignes(livreId, d.id())) {
            if (l.motif() != null) {
                retenues.put(l.code(), new BaseTaxe(l.baseRetenue(), l.taxeRetenue()));
                motifs.put(l.code(), l.motif());
            }
        }
        repository.viderLignesCalculees(livreId, d.id());
        Map<String, BigDecimal> taxes = new HashMap<>();
        for (LigneCalculee l : TvaCalcul.lignes(periode, creditAttendu, retenues)) {
            repository.ecrireLigne(livreId, d.id(), l.code(), l.base(), l.taxe());
            taxes.put(l.code(), l.taxe());
        }
        BigDecimal l132 = taxes.getOrDefault("132", Z);
        BigDecimal l182 = taxes.getOrDefault("182", Z);
        BigDecimal l170 = taxes.getOrDefault("170", Z);
        repository.ecrireResultat(livreId, d.id(), l170, l132, l182, taxes.getOrDefault("200", Z), taxes.getOrDefault("201", Z));
        Declaration maj = repository.lire(livreId, d.id()).orElseThrow();
        boolean premiere = p.premierePeriode() != null && d.numeroPeriode() == p.premierePeriode();
        controles.rejouer(livreId, maj, p, creditAttendu, precedente.isEmpty() && !premiere, retenues.containsKey("170"), taille(livreId, maj), remiseMode);
    }

    private long taille(Long livreId, Declaration d) {
        return fichiers.zip(releve(livreId, d)).length;
    }

    private TvaFichiers.Releve releve(Long livreId, Declaration d) {
        return fichiers.releve(livres.lire(livreId).identifiantFiscal(), d.annee(), d.numeroPeriode(), d.regimeDgi(), d.version(), repository.deductibles(livreId, d.id()));
    }

    // --- Lecture

    private TvaDeclarationDTO dto(Long livreId, Declaration d) {
        LigneLivre livre = livres.lire(livreId);
        Map<String, LigneRetenue> lignes = new HashMap<>();
        repository.lignes(livreId, d.id()).forEach(l -> lignes.put(l.code(), l));
        List<TvaLigneDTO> ordonnees = new ArrayList<>();
        for (LigneFormulaire f : formulaire.lignes()) {
            LigneRetenue l = lignes.get(f.code());
            if (l != null) {
                ordonnees.add(new TvaLigneDTO(f.code(), f.section(), f.libelle(), f.taux(), l.baseCalculee(), l.taxeCalculee(), l.baseRetenue(), l.taxeRetenue(), l.motif()));
            }
        }
        List<TvaControleDTO> liste = repository.controles(livreId, d.id());
        long bloquants = liste.stream().filter(c -> "OUVERT".equals(c.etat()) && "BLOQUANT".equals(c.gravite())).count();
        long alertes = liste.stream().filter(c -> "OUVERT".equals(c.etat()) && c.gravite().startsWith("ALERTE")).count();
        long nonEffectues = liste.stream().filter(c -> "NON_EFFECTUE".equals(c.gravite())).count();
        return new TvaDeclarationDTO(d.id(), d.annee(), d.numeroPeriode(), d.du(), d.au(), d.periodicite(), d.regimeDgi(), d.faitGenerateur(), d.version(), d.statut(), d.dateLimite(), livre.raisonSociale(), livre.identifiantFiscal(), livre.ice(),
                formulaire.modele(), d.creditAnterieur(), d.tvaExigible(), d.tvaDeductible(), d.tvaDue(), d.creditPeriode(), d.calculeLe(), d.valideLe(), d.valideePar(), d.deposeLe(), d.numeroRecu(), d.payeLe(),
                d.ecritureLiquidationId(), d.ecriturePaiementId(), ordonnees, new TvaComptesControlesDTO(bloquants, alertes, nonEffectues));
    }

    @Transactional(readOnly = true)
    public List<TvaEncaissementDTO> encaissements(Long id) {
        Long livreId = livre();
        exiger(livreId, id);
        return repository.collectees(livreId, id);
    }

    @Transactional(readOnly = true)
    public List<TvaDeductionDTO> deductions(Long id) {
        Long livreId = livre();
        exiger(livreId, id);
        return repository.deductibles(livreId, id);
    }

    @Transactional(readOnly = true)
    public List<TvaControleDTO> listeControles(Long id) {
        Long livreId = livre();
        exiger(livreId, id);
        return repository.controles(livreId, id);
    }

    @Transactional(readOnly = true)
    public List<TvaFichierDTO> listeFichiers(Long id) {
        Long livreId = livre();
        exiger(livreId, id);
        return repository.fichiers(livreId, id);
    }

    // --- Corrections sur le brouillon

    /** Rubrique, désignation, identité, référence ou traitement, toujours avec un motif. La correction survit au recalcul. */
    @Transactional
    public TvaDeclarationDTO modifierOperation(String type, Long operationId, TvaOperationRequest r) {
        Long livreId = livre();
        String motif = r.motif().trim();
        Long declarationId;
        if ("COLLECTEE".equals(type)) {
            declarationId = repository.declarationDeCollectee(livreId, operationId).orElseThrow(() -> new ResourceNotFoundException("Opération", "id", operationId));
            Declaration d = exigerBrouillon(livreId, declarationId);
            OperationCollectee o = repository.operationCollectee(livreId, operationId).orElseThrow();
            String traitement = r.traitement() != null ? r.traitement() : o.traitement();
            if (!TRAITEMENTS.contains(traitement)) {
                throw new CommonException("Traitement inconnu : " + traitement + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_OPERATION");
            }
            BigDecimal taux = r.taux() != null ? r.taux().setScale(2, RoundingMode.HALF_UP) : o.taux();
            if (r.taux() != null && o.factureNumero() != null) {
                throw new CommonException("Le taux vient de la facture " + o.factureNumero() + " : seul un acompte sans facture reçoit un taux ici.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_OPERATION");
            }
            Tranche t = taux.compareTo(o.taux()) == 0 ? new Tranche(o.taux(), o.ht(), o.tva(), o.ttc()) : TvaCalcul.depuisTtc(o.ttc(), taux);
            String ligne = "TAXABLE".equals(traitement) ? (taux.signum() == 0 ? o.ligneCode() : o.ligneCode() != null && taux.compareTo(o.taux()) == 0 ? o.ligneCode() : formulaire.ligneDeVente(null, taux)) : formulaire.ligneDeTraitement(traitement);
            repository.modifierCollectee(livreId, operationId, t.taux(), t.ttc(), t.ht(), t.tva(), traitement, ligne, motif);
            audit.logAction(ActionAudit.MODIFICATION, "compta_tva_operations_collectees", operationId, "Opération corrigée : " + motif);
            recalculer(livreId, d, parametresDe(livreId, d.annee()));
        } else if ("DEDUCTIBLE".equals(type)) {
            declarationId = repository.declarationDeDeductible(livreId, operationId).orElseThrow(() -> new ResourceNotFoundException("Opération", "id", operationId));
            Declaration d = exigerBrouillon(livreId, declarationId);
            OperationDeductible o = repository.operationDeductible(livreId, operationId).orElseThrow();
            String rubrique = r.rubriqueCode() != null ? r.rubriqueCode() : o.rubrique();
            Rubrique ru = formulaire.rubrique(rubrique).orElseThrow(() -> new CommonException("Rubrique inconnue : " + rubrique + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_OPERATION"));
            repository.modifierDeductible(livreId, operationId, rubrique, formulaire.ligneDeDeduction(rubrique, o.taux()), ru.compteTva(), r.designation() != null ? r.designation() : o.designation(),
                    r.numeroFacture() != null ? r.numeroFacture() : o.numeroFacture(), r.fournisseurIf() != null ? r.fournisseurIf() : o.fournisseurIf(), r.fournisseurIce() != null ? r.fournisseurIce() : o.fournisseurIce(),
                    r.referencePaiement() != null ? r.referencePaiement() : o.reference(), motif);
            audit.logAction(ActionAudit.MODIFICATION, "compta_tva_operations_deductibles", operationId, "Opération corrigée : " + motif);
            recalculer(livreId, d, parametresDe(livreId, d.annee()));
        } else {
            throw new ResourceNotFoundException("Type d'opération", "type", type);
        }
        return dto(livreId, repository.lire(livreId, declarationId).orElseThrow());
    }

    /** Sans valeur, la correction se retire et le calcul reprend. */
    @Transactional
    public TvaDeclarationDTO corrigerLigne(Long id, TvaCorrectionLigneRequest r) {
        Long livreId = livre();
        Declaration d = exigerBrouillon(livreId, id);
        if (!LIGNES_SAISIES.contains(r.code()) && formulaire.ligne(r.code()).isEmpty()) {
            throw new CommonException("La ligne " + r.code() + " n'existe pas au formulaire.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_LIGNE");
        }
        if (Set.of("130", "132", "182", "190", "200", "201", "205", "060").contains(r.code())) {
            throw new CommonException("La ligne " + r.code() + " est une formule : corrigez les lignes qui la composent.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_LIGNE");
        }
        if (r.baseRetenue() == null && r.taxeRetenue() == null) {
            repository.retirerCorrection(livreId, id, r.code());
        } else {
            repository.corrigerLigne(livreId, id, r.code(), r.baseRetenue(), r.taxeRetenue(), r.motif().trim());
        }
        audit.logAction(ActionAudit.MODIFICATION, "compta_tva_declaration_lignes", id, "Ligne " + r.code() + " : " + r.motif().trim());
        recalculer(livreId, d, parametresDe(livreId, d.annee()));
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    /** Le relevé de la période : chaque débit se pointe ou se classe. Sans relevé, le pointage reste non effectué. */
    @Transactional(readOnly = true)
    public List<LigneClasseeDTO> pointage(Long id) {
        Long livreId = livre();
        Declaration d = exiger(livreId, id);
        return rapprochement.lignesDePeriode(livreId, d.du(), d.au());
    }

    /** Sans classement demandé, la ligne redevient à classer. */
    @Transactional
    public LigneClasseeDTO classer(Long ligneReleveId, ClassementRequest r) {
        Long livreId = livre();
        var ligne = rapprochement.ligneReleve(livreId, ligneReleveId).orElseThrow(() -> new ResourceNotFoundException("Ligne de relevé", "id", ligneReleveId));
        if ("AUCUN".equals(r.classement())) {
            rapprochement.retirerClassement(livreId, ligneReleveId);
        } else {
            if (!Set.of("RUBRIQUE", "HORS_CHAMP", "NON_TAXABLE").contains(r.classement())) {
                throw new CommonException("Classement inconnu : " + r.classement() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_CLASSEMENT");
            }
            if ("RUBRIQUE".equals(r.classement()) && (r.rubriqueCode() == null || formulaire.rubrique(r.rubriqueCode()).isEmpty())) {
                throw new CommonException("Indiquez une rubrique connue.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_CLASSEMENT");
            }
            rapprochement.classer(livreId, ligneReleveId, ligne.debit(), ligne.credit(), r.classement(), "RUBRIQUE".equals(r.classement()) ? r.rubriqueCode() : null, r.libelle());
        }
        return rapprochement.lignesDePeriode(livreId, ligne.date(), ligne.date()).stream().filter(l -> l.ligneReleveId().equals(ligneReleveId)).findFirst().orElseThrow();
    }

    /** T01 et T31 : l'identité du dossier diffère de la source. Elle se corrige ici, avec un motif gardé en trace. */
    @Transactional
    public TvaDeclarationDTO corrigerIdentite(Long id, TvaIdentiteRequest r) {
        Long livreId = livre();
        Declaration d = exigerBrouillon(livreId, id);
        String fiscal = r.identifiantFiscal() == null || r.identifiantFiscal().isBlank() ? null : r.identifiantFiscal().trim();
        String ice = r.ice() == null || r.ice().isBlank() ? null : r.ice().trim();
        livres.modifierIdentifiants(livreId, fiscal, ice);
        audit.logAction(ActionAudit.MODIFICATION, "compta_livres", livreId, "IF et ICE du dossier corrigés depuis la TVA : " + r.motif().trim());
        recalculer(livreId, d, parametresDe(livreId, d.annee()));
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    /** Les paiements passés que personne n'a déclarés, depuis la première période tenue. Au-delà de 12 mois, le droit à déduction est à examiner. */
    @Transactional(readOnly = true)
    public List<TvaRattrapageDTO> aRattraper(Long id) {
        Long livreId = livre();
        Declaration d = exiger(livreId, id);
        TvaParametresDTO p = parametresDe(livreId, d.annee());
        LocalDate plancher = p.premierePeriode() == null ? livres.lire(livreId).dateDemarrage() : TvaCalcul.debutDePeriode(d.annee(), p.periodicite(), p.premierePeriode());
        if (!plancher.isBefore(d.du())) {
            return List.of();
        }
        LocalDate au = d.du().minusDays(1);
        Set<String> deja = repository.dejaDeclarees(livreId, false).stream().map(c -> c.substring(0, c.indexOf('|', c.indexOf('|') + 1))).collect(Collectors.toSet());
        List<TvaRattrapageDTO> lignes = new ArrayList<>();
        for (Reglement r : repository.reglements(livreId, plancher, au)) {
            if (!deja.contains("REGLEMENT_FOURNISSEUR|" + r.id()) && r.factureAchatId() != null) {
                lignes.add(new TvaRattrapageDTO("REGLEMENT_FOURNISSEUR", r.id(), r.numero(), r.date(), r.montant(), r.mode(), null, r.date().plusMonths(12).isBefore(d.du())));
            }
        }
        Set<Long> vues = new java.util.HashSet<>();
        for (DepenseVentilee v : repository.depensesVentilees(livreId, plancher, au)) {
            if (v.deductible() && !deja.contains("DEPENSE|" + v.id()) && vues.add(v.id())) {
                lignes.add(new TvaRattrapageDTO("DEPENSE", v.id(), v.reference(), v.date(), v.montant(), v.mode(), v.tiers(), v.date().plusMonths(12).isBefore(d.du())));
            }
        }
        return lignes;
    }

    @Transactional
    public TvaDeclarationDTO rattraper(Long id, TvaRattraperRequest r) {
        Long livreId = livre();
        Declaration d = exigerBrouillon(livreId, id);
        TvaParametresDTO p = parametresDe(livreId, d.annee());
        Surcharges s = operations.lireSurcharges(livreId, id);
        List<Long> reglements = r.pieces().stream().filter(x -> "REGLEMENT_FOURNISSEUR".equals(x.sourceType())).map(TvaPieceVisee::sourceId).toList();
        List<Long> depenses = r.pieces().stream().filter(x -> "DEPENSE".equals(x.sourceType())).map(TvaPieceVisee::sourceId).toList();
        operations.ajouterReglements(livreId, d, p, repository.reglementsParIds(livreId, reglements), s);
        operations.ajouterDepenses(livreId, d, p, repository.depensesVentileesParIds(livreId, depenses), s);
        audit.logAction(ActionAudit.MODIFICATION, "compta_tva_declarations", id, r.pieces().size() + " paiement(s) rattrapé(s)");
        recalculer(livreId, d, p);
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    // --- Contrôles

    /** T18 se lève avec une exception motivée. Les autres bloquants se corrigent, ils ne s'acceptent pas. */
    private static boolean acceptable(TvaControleDTO c) {
        return c.gravite().startsWith("ALERTE") || ("BLOQUANT".equals(c.gravite()) && "T18".equals(c.code()));
    }

    @Transactional
    public TvaControleDTO accepter(Long controleId, TvaAccepterRequest r) {
        Long livreId = livre();
        TvaControleDTO c = repository.controle(livreId, controleId).orElseThrow(() -> new ResourceNotFoundException("Contrôle", "id", controleId));
        exigerBrouillon(livreId, repository.declarationDuControle(livreId, controleId).orElseThrow());
        if (!acceptable(c)) {
            throw new CommonException("Le contrôle " + c.code() + " est " + c.gravite() + " : il se corrige, il ne s'accepte pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_CONTROLE");
        }
        repository.accepterControle(livreId, controleId, r.motif().trim(), livreCourant.auteur());
        return repository.controle(livreId, controleId).orElseThrow();
    }

    /** Le même motif pour toutes les alertes ouvertes d'un code : une par une sur soixante factures, personne ne lit. */
    @Transactional
    public List<TvaControleDTO> accepterCode(Long id, TvaAccepterCodeRequest r) {
        Long livreId = livre();
        exigerBrouillon(livreId, id);
        for (TvaControleDTO c : repository.controles(livreId, id)) {
            if (c.code().equals(r.code()) && "OUVERT".equals(c.etat()) && c.gravite().startsWith("ALERTE")) {
                repository.accepterControle(livreId, c.id(), r.motif().trim(), livreCourant.auteur());
            }
        }
        return repository.controles(livreId, id);
    }

    // --- Cycle de vie

    /** Figée : plus de calcul. Aucun bloquant ouvert, et chaque alerte porte un motif. */
    @Transactional
    public TvaDeclarationDTO valider(Long id) {
        Long livreId = livre();
        Declaration d = exigerBrouillon(livreId, id);
        recalculer(livreId, d, parametresDe(livreId, d.annee()));
        List<TvaControleDTO> liste = repository.controles(livreId, id);
        long bloquants = liste.stream().filter(c -> "BLOQUANT".equals(c.gravite()) && "OUVERT".equals(c.etat())).count();
        if (bloquants > 0) {
            throw new CommonException(bloquants + " contrôle(s) bloquant(s) restent ouverts : la déclaration ne se valide pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_BLOQUANTS");
        }
        long alertes = liste.stream().filter(c -> c.gravite().startsWith("ALERTE") && "OUVERT".equals(c.etat())).count();
        if (alertes > 0) {
            throw new CommonException(alertes + " alerte(s) restent sans motif : acceptez-les avec un motif, ou corrigez-les.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_ALERTES");
        }
        Declaration maj = repository.lire(livreId, id).orElseThrow();
        TvaFichiers.Releve releve = releve(livreId, maj);
        fichiers.verifier(releve.contenu());
        byte[] zip = fichiers.zip(releve);
        repository.ecrireFichier(livreId, id, "RD", releve.nomXml().replace(".xml", ".zip"), fichiers.empreinte(zip), zip, livreCourant.auteur());
        repository.changerStatut(livreId, id, "VALIDEE", livreCourant.auteur());
        audit.logAction(ActionAudit.VALIDATION, "compta_tva_declarations", id, "Déclaration validée : TVA due " + maj.tvaDue() + ", crédit " + maj.creditPeriode());
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    @Transactional
    public TvaDeclarationDTO devalider(Long id) {
        Long livreId = livre();
        Declaration d = exiger(livreId, id);
        if (!Set.of("VALIDEE", "EDI_REJETE").contains(d.statut())) {
            throw new CommonException("La déclaration est " + d.statut() + " : elle ne revient plus en brouillon.", HttpStatus.CONFLICT, "COMPTA_TVA_FIGEE");
        }
        repository.changerStatut(livreId, id, "BROUILLON", null);
        audit.logAction(ActionAudit.ANNULATION, "compta_tva_declarations", id, "Déclaration dévalidée");
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public Object[] fichier(Long id, String type) {
        Long livreId = livre();
        exiger(livreId, id);
        return repository.dernierFichier(livreId, id, type).orElseThrow(() -> new CommonException("Ce fichier n'existe qu'une fois la déclaration validée.", HttpStatus.CONFLICT, "COMPTA_TVA_FICHIER_ABSENT"));
    }

    /** L'état lu dans « Suivi Envoi EDI », recopié à la main : l'application ne se connecte pas à SIMPL-TVA. */
    @Transactional
    public TvaDeclarationDTO etatFichier(Long id, String type, TvaEtatFichierRequest r) {
        Long livreId = livre();
        Declaration d = exiger(livreId, id);
        String statut = switch (r.etat()) {
            case "EN_ATTENTE" -> "EDI_ENVOYE";
            case "TRAITE" -> "EDI_TRAITE";
            case "REJETE" -> "EDI_REJETE";
            default -> throw new CommonException("État DGI inconnu : " + r.etat() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TVA_ETAT");
        };
        if (!Set.of("VALIDEE", "EDI_ENVOYE", "EDI_TRAITE", "EDI_REJETE").contains(d.statut())) {
            throw new CommonException("La déclaration est " + d.statut() + " : l'état DGI ne se recopie plus.", HttpStatus.CONFLICT, "COMPTA_TVA_ETAT");
        }
        repository.dernierFichier(livreId, id, type).orElseThrow(() -> new CommonException("Aucun fichier " + type + " pour cette déclaration.", HttpStatus.CONFLICT, "COMPTA_TVA_FICHIER_ABSENT"));
        repository.etatFichier(livreId, id, type, r.etat(), r.commentaire());
        repository.changerStatut(livreId, id, statut, null);
        audit.logAction(ActionAudit.MODIFICATION, "compta_tva_fichiers_edi", id, "État DGI du fichier " + type + " : " + r.etat());
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    /** Le relevé accepté par la DGI, ou pas de relevé du tout : la déclaration se dépose, et l'écriture de liquidation se passe. */
    @Transactional
    public TvaDeclarationDTO depot(Long id, TvaDepotRequest r) {
        Long livreId = livre();
        Declaration d = exiger(livreId, id);
        List<OperationDeductible> deductibles = repository.operationsDeductibles(livreId, id);
        boolean relevePret = "EDI_TRAITE".equals(d.statut()) || ("VALIDEE".equals(d.statut()) && deductibles.isEmpty());
        if (!relevePret) {
            throw new CommonException("La déclaration est " + d.statut() + " : le relevé des déductions doit d'abord être traité par la DGI.", HttpStatus.CONFLICT, "COMPTA_TVA_DEPOT");
        }
        Long ecriture = liquider(livreId, d, deductibles, r.deposeLe());
        repository.deposer(livreId, id, r.deposeLe(), r.numeroRecu(), ecriture);
        audit.logAction(ActionAudit.MODIFICATION, "compta_tva_declarations", id, "Déclaration déposée le " + r.deposeLe());
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    private Long liquider(Long livreId, Declaration d, List<OperationDeductible> deductibles, LocalDate le) {
        List<NouvelleLigne> lignes = new ArrayList<>();
        BigDecimal exigible = d.tvaExigible();
        ligne(lignes, "4455", exigible, Z);
        ligne(lignes, "4456", Z, exigible);
        BigDecimal deduit = Z;
        for (String compte : List.of("34552", "34551")) {
            BigDecimal tva = deductibles.stream().filter(o -> compte.equals(o.compteTva())).map(OperationDeductible::tva).reduce(Z, BigDecimal::add);
            BigDecimal part = deductibles.stream().filter(o -> compte.equals(o.compteTva())).map(OperationDeductible::tvaDeductible).reduce(Z, BigDecimal::add);
            deduit = deduit.add(part);
            ligne(lignes, "4456", part, Z);
            // La part non déductible sort du compte : choix provisoire, à valider par un expert-comptable.
            ligne(lignes, "61678", tva.subtract(part), Z);
            ligne(lignes, compte, Z, tva);
        }
        ligne(lignes, "4456", d.creditAnterieur(), Z);
        ligne(lignes, "3456", Z, d.creditAnterieur());
        // Lignes saisies (180 à 187) : elles entrent dans le total déductible sans venir d'une opération.
        BigDecimal saisies = d.tvaExigible().subtract(d.tvaDue()).add(d.creditPeriode()).subtract(deduit).subtract(d.creditAnterieur());
        if (saisies.signum() > 0) {
            ligne(lignes, "4456", saisies, Z);
            ligne(lignes, "34552", Z, saisies);
        } else if (saisies.signum() < 0) {
            ligne(lignes, "34552", saisies.negate(), Z);
            ligne(lignes, "4456", Z, saisies.negate());
        }
        ligne(lignes, "3456", d.creditPeriode(), Z);
        ligne(lignes, "4456", Z, d.creditPeriode());
        if (lignes.size() < 2) {
            return null;
        }
        JournalDTO od = journaux.parType(livreId, "OD").orElseThrow(() -> new CommonException("Le journal des opérations diverses n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        String ref = "TVA-" + d.annee() + "-" + String.format("%02d", d.numeroPeriode()) + "-V" + d.version();
        return poster(livreId, od, le, ref, "Liquidation de la TVA " + d.du() + " au " + d.au(), lignes).id();
    }

    private static void ligne(List<NouvelleLigne> lignes, String compte, BigDecimal debit, BigDecimal credit) {
        if (debit.signum() != 0 || credit.signum() != 0) {
            lignes.add(new NouvelleLigne(compte, null, debit.signum() < 0 ? credit : debit, debit.signum() < 0 ? debit.negate() : credit.signum() < 0 ? BigDecimal.ZERO : credit, null, null, null, false, null, null, null, null, null, null));
        }
    }

    private EcritureEnregistree poster(Long livreId, JournalDTO j, LocalDate le, String ref, String libelle, List<NouvelleLigne> lignes) {
        LocalDate date = dates.resoudre(livreId, j.id(), le);
        return posteur.poster(livreId, new PosteurEcritures.Proposition(j.id(), date, le, ref, libelle, "COURANTE", "TVA", null, null, null, false, null, null, lignes));
    }

    /** D 4456 / C 5141x, journal BQ. Le compte financier précisé donne son compte ; sinon la banque par défaut. */
    @Transactional
    public TvaDeclarationDTO paiement(Long id, TvaPaiementRequest r) {
        Long livreId = livre();
        Declaration d = exiger(livreId, id);
        if (!"DEPOSEE".equals(d.statut())) {
            throw new CommonException("La déclaration est " + d.statut() + " : seule une déclaration déposée se paie.", HttpStatus.CONFLICT, "COMPTA_TVA_PAIEMENT");
        }
        if (d.tvaDue().signum() == 0) {
            throw new CommonException("Rien à payer : la période est créditrice ou nulle.", HttpStatus.CONFLICT, "COMPTA_TVA_PAIEMENT");
        }
        Map<String, String> table = correspondances.assurer(livreId);
        String compte = r.compteFinancierId() == null ? null : table.get("COMPTE_FINANCIER|" + r.compteFinancierId());
        if (compte == null) {
            compte = table.getOrDefault("BANQUE_DEFAUT|", "5141");
        }
        List<NouvelleLigne> lignes = new ArrayList<>();
        ligne(lignes, "4456", d.tvaDue(), Z);
        ligne(lignes, compte, Z, d.tvaDue());
        JournalDTO bq = journaux.parType(livreId, "BANQUE").orElseThrow(() -> new CommonException("Le journal de banque n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        String ref = "TVA-" + d.annee() + "-" + String.format("%02d", d.numeroPeriode()) + "-PAIE";
        Long ecriture = poster(livreId, bq, r.payeLe(), ref, "Paiement de la TVA " + d.du() + " au " + d.au(), lignes).id();
        repository.payer(livreId, id, r.payeLe(), r.modePaiement(), ecriture);
        audit.logAction(ActionAudit.MODIFICATION, "compta_tva_declarations", id, "Déclaration payée le " + r.payeLe());
        return dto(livreId, repository.lire(livreId, id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public EcritureDTO ecriture(Long ecritureId) {
        Long livreId = livre();
        return journal.lire(livreId, ecritureId).orElseThrow(() -> new ResourceNotFoundException("Écriture", "id", ecritureId));
    }

    // --- Exports

    @Transactional(readOnly = true)
    public byte[] fiche(Long id) {
        Long livreId = livre();
        return fichiers.fichePdf(dto(livreId, exiger(livreId, id)));
    }

    @Transactional(readOnly = true)
    public byte[] releveXlsx(Long id) {
        Long livreId = livre();
        return fichiers.releveXlsx(dto(livreId, exiger(livreId, id)), repository.deductibles(livreId, id));
    }

    @Transactional(readOnly = true)
    public byte[] couverture(Long id) {
        Long livreId = livre();
        return fichiers.couvertureXlsx(dto(livreId, exiger(livreId, id)));
    }

    @Transactional(readOnly = true)
    public byte[] detail(Long id) {
        Long livreId = livre();
        return fichiers.detailXlsx(dto(livreId, exiger(livreId, id)), repository.collectees(livreId, id), repository.deductibles(livreId, id));
    }
}
