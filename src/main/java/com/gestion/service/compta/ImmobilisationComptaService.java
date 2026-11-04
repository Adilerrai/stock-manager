package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ParametreFiscalDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.AnnuiteDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.AnnulationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ComptesLiesDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationResumeDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneAEnregistrerDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneDotationDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.SortieDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.SortieRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.ImmobilisationComptaRepository;
import com.gestion.repository.compta.ImmobilisationComptaRepository.Dotation;
import com.gestion.repository.compta.ImmobilisationComptaRepository.Fiche;
import com.gestion.repository.compta.ImmobilisationComptaRepository.NouvelleFiche;
import com.gestion.repository.compta.ImmobilisationComptaRepository.NouvelleSortie;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.ParametreFiscalRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.service.AuditService;
import com.gestion.service.compta.PlanAmortissement.Annuite;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Le registre : une fiche naît d'une écriture, le cumul se lit dans les dotations passées, une dotation ou une sortie erronée se contre-passe.
 * Le plan prévisionnel ne s'écrit nulle part : les états ne lisent que le journal.
 */
@Service
public class ImmobilisationComptaService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> MODES = Set.of("LINEAIRE", "DEGRESSIF", "NON_AMORTISSABLE");
    /** Les comptes proposés : immobilisation, amortissement, dotation. */
    private static final Map<String, String[]> COMPTES_LIES = new LinkedHashMap<>();

    static {
        for (String c : List.of("23211", "23214", "23218")) {
            COMPTES_LIES.put(c, new String[]{"28321", "61932"});
        }
        for (String c : List.of("23321", "23324")) {
            COMPTES_LIES.put(c, new String[]{"28332", "61933"});
        }
        COMPTES_LIES.put("2340", new String[]{"2834", "61934"});
        COMPTES_LIES.put("2351", new String[]{"28351", "61935"});
        COMPTES_LIES.put("2352", new String[]{"28352", "61935"});
        COMPTES_LIES.put("2355", new String[]{"28355", "61935"});
        COMPTES_LIES.put("2356", new String[]{"28356", "61935"});
        COMPTES_LIES.put("2358", new String[]{"28358", "61935"});
        COMPTES_LIES.put("2380", new String[]{"2838", "61938"});
    }

    private final ImmobilisationComptaRepository repository;
    private final ExerciceRepository exercices;
    private final PeriodeRepository periodes;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final PosteurEcritures posteur;
    private final DatesComptables dates;
    private final LettrageService lettrage;
    private final ParametreFiscalRepository parametres;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public ImmobilisationComptaService(ImmobilisationComptaRepository repository, ExerciceRepository exercices, PeriodeRepository periodes, JournauxRepository journaux, JournalRepository journal, PosteurEcritures posteur,
                                 DatesComptables dates, LettrageService lettrage, ParametreFiscalRepository parametres, LivreCourantService livreCourant, AuditService audit) {
        this.repository = repository;
        this.exercices = exercices;
        this.periodes = periodes;
        this.journaux = journaux;
        this.journal = journal;
        this.posteur = posteur;
        this.dates = dates;
        this.lettrage = lettrage;
        this.parametres = parametres;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    private Long livre() {
        return livreCourant.exiger().livreId();
    }

    private static CommonException refus(String message, HttpStatus statut, String code) {
        return new CommonException(message, statut, code);
    }

    // --- Lecture

    @Transactional(readOnly = true)
    public ComptesLiesDTO comptesLies(String compte) {
        String[] lies = COMPTES_LIES.get(compte);
        return new ComptesLiesDTO(compte, lies == null ? null : lies[0], lies == null ? null : lies[1]);
    }

    @Transactional(readOnly = true)
    public List<ImmobilisationResumeDTO> lister() {
        return repository.lister(livre());
    }

    @Transactional(readOnly = true)
    public List<LigneAEnregistrerDTO> aEnregistrer() {
        return repository.aEnregistrer(livre());
    }

    @Transactional(readOnly = true)
    public ImmobilisationDTO lire(Long id) {
        Long livreId = livre();
        return dto(livreId, exiger(livreId, id));
    }

    private Fiche exiger(Long livreId, Long id) {
        return repository.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Immobilisation", "id", id));
    }

    private static PlanAmortissement.Fiche plan(Fiche f) {
        return new PlanAmortissement.Fiche(f.valeurBrute(), f.cumulReprise(), f.mode(), f.dureeMois() == null ? 0 : f.dureeMois(), f.coefficient(), f.dateMiseEnService());
    }

    private ImmobilisationDTO dto(Long livreId, Fiche f) {
        BigDecimal cumul = f.cumulReprise().add(f.passees());
        List<AnnuiteDTO> prevision = new ArrayList<>();
        if ("EN_SERVICE".equals(f.statut())) {
            exercices.lister(livreId).stream().filter(e -> "OUVERT".equals(e.statut())).findFirst().ifPresent(e -> {
                for (Annuite a : PlanAmortissement.prevision(plan(f), f.passees(), debutSansDotation(livreId, f, e), e.au())) {
                    prevision.add(new AnnuiteDTO(a.du(), a.au(), a.montant()));
                }
            });
        }
        return new ImmobilisationDTO(f.id(), f.code(), f.designation(), f.compteImmo(), f.compteAmort(), f.compteDotation(), f.dateAcquisition(), f.dateMiseEnService(), f.valeurBrute(), f.cumulReprise(), f.tvaDeduite(), f.mode(), f.dureeMois(),
                f.coefficient(), f.statut(), f.tiersId(), f.ecritureAcquisitionId(), cumul, f.valeurBrute().subtract(cumul), prevision, repository.dotations(livreId, f.id()), repository.sorties(livreId, f.id()), f.creeLe());
    }

    /** La prévision commence à l'exercice ouvert, ou au suivant s'il est déjà doté. */
    private LocalDate debutSansDotation(Long livreId, Fiche f, ExerciceDTO e) {
        boolean dote = repository.dotationsActives(livreId, e.id(), "ANNUELLE").stream().anyMatch(d -> d.immobilisationId().equals(f.id()));
        return dote ? e.du().plusYears(1) : e.du();
    }

    // --- Acquisition

    /** R13 : la fiche ne peut pas exister sans écriture. Le service la passe, ou reprend une écriture déjà au journal. */
    @Transactional
    public ImmobilisationDTO creer(ImmobilisationRequest r) {
        Long livreId = livre();
        if (!r.compteImmo().startsWith("2") || r.compteImmo().startsWith("28") || r.compteImmo().startsWith("29")) {
            throw refus("Le compte " + r.compteImmo() + " n'est pas un compte d'immobilisation.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_COMPTE");
        }
        if (!MODES.contains(r.modeAmortissement())) {
            throw refus("Mode d'amortissement inconnu : " + r.modeAmortissement() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_MODE");
        }
        if (r.valeurBrute().signum() <= 0) {
            throw refus("La valeur brute doit être positive.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_VALEUR");
        }
        BigDecimal reprise = r.cumulReprise() == null ? Z : r.cumulReprise();
        BigDecimal tva = r.tvaDeduite() == null ? Z : r.tvaDeduite();
        if (reprise.signum() < 0 || reprise.compareTo(r.valeurBrute()) > 0) {
            throw refus("La reprise d'amortissement se situe entre 0 et la valeur brute.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_VALEUR");
        }
        boolean amortissable = !"NON_AMORTISSABLE".equals(r.modeAmortissement());
        String[] lies = COMPTES_LIES.get(r.compteImmo());
        String compteAmort = r.compteAmort() != null ? r.compteAmort() : lies == null ? null : lies[0];
        String compteDotation = r.compteDotation() != null ? r.compteDotation() : lies == null ? null : lies[1];
        if (amortissable && (compteAmort == null || compteDotation == null)) {
            throw refus("Indiquez le compte d'amortissement et le compte de dotation de " + r.compteImmo() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_COMPTE");
        }
        if (amortissable && (r.dureeMois() == null || r.dureeMois() <= 0)) {
            throw refus("Indiquez la durée d'amortissement.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_DUREE");
        }
        BigDecimal coefficient = null;
        if ("DEGRESSIF".equals(r.modeAmortissement())) {
            if (r.dureeMois() < 36) {
                throw refus("Le dégressif demande une durée d'au moins trois ans (CGI art. 10).", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_DUREE");
            }
            if (r.compteImmo().startsWith("232")) {
                throw refus("Le dégressif exclut les immeubles (CGI art. 10).", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_MODE");
            }
            coefficient = coefficient(r.dureeMois(), r.dateAcquisition());
        }
        Long ecriture = r.ecritureAcquisitionId() != null ? verifierEcriture(livreId, r) : passerAcquisition(livreId, r, tva);
        Long id = repository.creer(livreId, new NouvelleFiche(repository.prochainCode(livreId), r.designation().trim(), r.compteImmo(), amortissable ? compteAmort : null, amortissable ? compteDotation : null, r.dateAcquisition(),
                r.dateMiseEnService(), r.valeurBrute(), reprise, tva, r.modeAmortissement(), amortissable ? r.dureeMois() : null, coefficient, r.tiersId(), r.sourceType(), r.sourceId(), ecriture, livreCourant.auteur()));
        audit.logAction(ActionAudit.CREATION, "compta_immobilisations", id, "Immobilisation " + r.designation().trim() + " : " + r.valeurBrute());
        return dto(livreId, exiger(livreId, id));
    }

    private BigDecimal coefficient(int dureeMois, LocalDate date) {
        int annees = dureeMois / 12;
        String cle = annees <= 4 ? "DUREE_3_A_4_ANS" : annees <= 6 ? "DUREE_5_A_6_ANS" : "DUREE_PLUS_6_ANS";
        ParametreFiscalDTO p = parametres.aLaDate("AMORT_DEGRESSIF_COEF", cle, date).orElseThrow(() -> refus("Aucun coefficient dégressif au " + date + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_PARAMETRE_ABSENT"));
        return p.valeur();
    }

    private Long verifierEcriture(Long livreId, ImmobilisationRequest r) {
        journal.lire(livreId, r.ecritureAcquisitionId()).orElseThrow(() -> refus("L'écriture d'acquisition n'existe pas dans ce livre.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_ECRITURE"));
        BigDecimal auCompte = journal.lignesBrutes(livreId, r.ecritureAcquisitionId()).stream().filter(l -> r.compteImmo().equals(l.compte())).map(LigneJournal::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (auCompte.compareTo(r.valeurBrute()) < 0) {
            throw refus("L'écriture porte " + MontantTexte.fr(auCompte) + " au débit de " + r.compteImmo() + ", pour une valeur brute de " + MontantTexte.fr(r.valeurBrute()) + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_ECRITURE");
        }
        return r.ecritureAcquisitionId();
    }

    private Long passerAcquisition(Long livreId, ImmobilisationRequest r, BigDecimal tva) {
        String contrepartie = r.contrepartie() == null ? "FOURNISSEUR" : r.contrepartie();
        String typeJournal = switch (contrepartie) {
            case "FOURNISSEUR" -> "ACHATS";
            case "BANQUE" -> "BANQUE";
            case "CAISSE" -> "CAISSE";
            default -> throw refus("Contrepartie inconnue : " + contrepartie + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_CONTREPARTIE");
        };
        String compte = switch (contrepartie) {
            case "FOURNISSEUR" -> "4411";
            case "BANQUE" -> "5141";
            default -> "51611";
        };
        if ("FOURNISSEUR".equals(contrepartie) && r.tiersId() == null) {
            throw refus("Indiquez le fournisseur de cette acquisition.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_CONTREPARTIE");
        }
        JournalDTO j = journaux.parType(livreId, typeJournal).orElseThrow(() -> refus("Le journal " + typeJournal + " n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        LocalDate piece = r.datePiece() != null ? r.datePiece() : r.dateAcquisition();
        List<NouvelleLigne> lignes = new ArrayList<>();
        lignes.add(ligne(r.compteImmo(), null, r.valeurBrute(), Z, "ACQUISITION"));
        if (tva.signum() != 0) {
            lignes.add(ligne("34551", null, tva, Z, null));
        }
        lignes.add(ligne(compte, "FOURNISSEUR".equals(contrepartie) ? r.tiersId() : null, Z, r.valeurBrute().add(tva), null));
        String ref = r.refPiece() != null && !r.refPiece().isBlank() ? r.refPiece().trim() : "IMMO-" + r.dateAcquisition();
        return posteur.poster(livreId, new PosteurEcritures.Proposition(j.id(), dates.resoudre(livreId, j.id(), piece), piece, ref, "Acquisition : " + r.designation().trim(), "COURANTE", "IMMOBILISATION", null, null, null, false, null, null, lignes)).id();
    }

    private static NouvelleLigne ligne(String compte, Long tiers, BigDecimal debit, BigDecimal credit, String natureMouvement) {
        return new NouvelleLigne(compte, tiers, debit, credit, null, null, natureMouvement, false, null, null, null, null, null, null);
    }

    // --- Dotations

    /** R14 : calcule, montre, puis passe en période 99, une ligne par famille, et une dotation par immobilisation. */
    @Transactional
    public DotationsDTO dotations(Long exerciceId, DotationsRequest r) {
        Long livreId = livre();
        ExerciceDTO exercice = exercices.lire(livreId, exerciceId).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", exerciceId));
        List<LigneDotationDTO> lignes = lignesDues(livreId, exercice);
        BigDecimal total = lignes.stream().map(LigneDotationDTO::montant).reduce(Z, BigDecimal::add);
        if (r.apercu()) {
            return new DotationsDTO(lignes, total, null);
        }
        if (lignes.isEmpty()) {
            throw refus("Aucune dotation à passer pour " + exercice.code() + ".", HttpStatus.CONFLICT, "COMPTA_DOTATION_RIEN");
        }
        Map<String, BigDecimal> debits = new TreeMap<>();
        Map<String, BigDecimal> credits = new TreeMap<>();
        for (LigneDotationDTO l : lignes) {
            debits.merge(l.compteDotation(), l.montant(), BigDecimal::add);
            credits.merge(l.compteAmortissement(), l.montant(), BigDecimal::add);
        }
        List<NouvelleLigne> ecriture = new ArrayList<>();
        debits.forEach((c, m) -> ecriture.add(ligne(c, null, m, Z, null)));
        credits.forEach((c, m) -> ecriture.add(ligne(c, null, Z, m, null)));
        JournalDTO od = journalOd(livreId);
        Long id = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), exercice.au(), exercice.au(), "DOT-" + exercice.code(), "Dotations aux amortissements " + exercice.code(), "INVENTAIRE", "IMMOBILISATION", null, null, null, false, null, null, ecriture)).id();
        for (LigneDotationDTO l : lignes) {
            repository.ajouterDotation(livreId, l.immobilisationId(), exerciceId, "ANNUELLE", l.du(), l.au(), l.montant(), id);
        }
        audit.logAction(ActionAudit.GENERATION_DOTATION, "compta_exercices", exerciceId, lignes.size() + " dotation(s), total " + total);
        return new DotationsDTO(lignes, total, id);
    }

    /** Ce qui reste à doter : les fiches en service sans dotation annuelle active sur cet exercice. */
    public List<LigneDotationDTO> lignesDues(Long livreId, ExerciceDTO exercice) {
        Set<Long> dejaDotees = new LinkedHashSet<>();
        repository.dotationsActives(livreId, exercice.id(), "ANNUELLE").forEach(d -> dejaDotees.add(d.immobilisationId()));
        List<LigneDotationDTO> lignes = new ArrayList<>();
        for (Fiche f : repository.fiches(livreId)) {
            if (!"EN_SERVICE".equals(f.statut()) || "NON_AMORTISSABLE".equals(f.mode()) || dejaDotees.contains(f.id()) || f.dateMiseEnService() == null) {
                continue;
            }
            BigDecimal montant = PlanAmortissement.dotation(plan(f), f.passees(), exercice.du(), exercice.au());
            if (montant.signum() > 0) {
                LocalDate du = f.dateMiseEnService().isAfter(exercice.du()) ? f.dateMiseEnService() : exercice.du();
                LocalDate terme = PlanAmortissement.finDePlan(plan(f));
                lignes.add(new LigneDotationDTO(f.id(), f.code(), f.designation(), du, terme.isBefore(exercice.au()) ? terme : exercice.au(), montant, f.compteDotation(), f.compteAmort()));
            }
        }
        return lignes;
    }

    @Transactional
    public DotationsDTO annulerDotations(Long exerciceId, AnnulationRequest r) {
        Long livreId = livre();
        ExerciceDTO exercice = exercices.lire(livreId, exerciceId).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", exerciceId));
        exigerOuvert(exercice);
        List<Dotation> actives = repository.dotationsActives(livreId, exerciceId, "ANNUELLE");
        if (actives.isEmpty()) {
            throw refus("Aucune dotation active sur " + exercice.code() + ".", HttpStatus.CONFLICT, "COMPTA_DOTATION_RIEN");
        }
        new LinkedHashSet<>(actives.stream().map(Dotation::ecritureId).toList()).forEach(e -> contrepasser(livreId, e, r.motif().trim()));
        actives.forEach(d -> repository.marquerContrepassee(livreId, d.id()));
        audit.logAction(ActionAudit.ANNULATION, "compta_exercices", exerciceId, "Dotations annulées : " + r.motif().trim());
        return dotations(exerciceId, new DotationsRequest(true));
    }

    private void exigerOuvert(ExerciceDTO exercice) {
        if ("CLOTURE".equals(exercice.statut())) {
            throw refus("L'exercice " + exercice.code() + " est clos : passez une écriture de correction sur l'exercice ouvert.", HttpStatus.CONFLICT, "COMPTA_CORRECTION_REQUISE");
        }
    }

    private JournalDTO journalOd(Long livreId) {
        return journaux.parType(livreId, "OD").orElseThrow(() -> refus("Le journal des opérations diverses n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
    }

    private Long contrepasser(Long livreId, Long ecritureId, String motif) {
        EcritureDTO origine = journal.lire(livreId, ecritureId).orElseThrow();
        List<LigneJournal> lignesOrigine = journal.lignesBrutes(livreId, ecritureId);
        List<NouvelleLigne> miroir = new ArrayList<>();
        for (LigneJournal l : lignesOrigine) {
            miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
        }
        LocalDate date = "INVENTAIRE".equals(origine.nature()) ? origine.dateEcriture() : dates.resoudre(livreId, origine.journal().id(), origine.dateEcriture());
        var mirage = posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), date, origine.datePiece(), origine.refPiece(), "Contre-passation de " + origine.numeroComplet(), origine.nature(), origine.origine(), ecritureId, null, motif, false, null, null, miroir));
        lettrage.lettrerMiroir(livreId, lignesOrigine, journal.lignesBrutes(livreId, mirage.id()), date);
        return mirage.id();
    }

    // --- Sortie

    /** R15 : complément de dotation, sortie, prix. Le résultat de cession se lit au grand livre et sur la fiche, à l'identique. */
    @Transactional
    public ImmobilisationDTO sortir(Long id, SortieRequest r) {
        Long livreId = livre();
        Fiche f = exiger(livreId, id);
        if (!"CESSION".equals(r.nature()) && !"RETRAIT".equals(r.nature())) {
            throw refus("Nature de sortie inconnue : " + r.nature() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_SORTIE");
        }
        BigDecimal prix = r.prixCessionHt() == null ? Z : r.prixCessionHt();
        BigDecimal tvaCession = r.tvaCession() == null ? Z : r.tvaCession();
        if ("RETRAIT".equals(r.nature()) && (prix.signum() != 0 || tvaCession.signum() != 0)) {
            throw refus("Un retrait n'a pas de prix de cession.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_SORTIE");
        }
        if ("CESSION".equals(r.nature()) && prix.signum() <= 0) {
            throw refus("Indiquez le prix de cession.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IMMO_SORTIE");
        }
        if (!"EN_SERVICE".equals(f.statut()) || repository.sortieActive(livreId, id).isPresent()) {
            throw refus("L'immobilisation " + f.code() + " est déjà sortie.", HttpStatus.CONFLICT, "COMPTA_IMMO_DEJA_SORTIE");
        }
        JournalDTO od = journalOd(livreId);
        LocalDate date = dates.resoudre(livreId, od.id(), r.dateSortie());
        var periode = periodes.courantePourDate(livreId, r.dateSortie()).orElseThrow(() -> refus("Aucun exercice ne couvre le " + r.dateSortie() + ".", HttpStatus.CONFLICT, "COMPTA_PERIODE_INCONNUE"));
        ExerciceDTO exercice = exercices.lire(livreId, periode.exerciceId()).orElseThrow();
        BigDecimal passees = f.passees();
        boolean annuelleDejaPassee = repository.dotationsActives(livreId, exercice.id(), "ANNUELLE").stream().anyMatch(d -> d.immobilisationId().equals(id));
        BigDecimal complement = annuelleDejaPassee ? Z : PlanAmortissement.dotation(plan(f), passees, exercice.du(), r.dateSortie());
        if (complement.signum() > 0) {
            String ref = "SORT-" + f.code();
            Long e = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), date, r.dateSortie(), ref, "Dotation complémentaire jusqu'à la sortie : " + f.designation(), "COURANTE", "IMMOBILISATION", null, null, null, false, null, null,
                    List.of(ligne(f.compteDotation(), null, complement, Z, null), ligne(f.compteAmort(), null, Z, complement, null)))).id();
            LocalDate du = f.dateMiseEnService().isAfter(exercice.du()) ? f.dateMiseEnService() : exercice.du();
            repository.ajouterDotation(livreId, id, exercice.id(), "COMPLEMENT", du, r.dateSortie(), complement, e);
            passees = passees.add(complement);
        }
        BigDecimal cumul = f.cumulReprise().add(passees);
        BigDecimal net = f.valeurBrute().subtract(cumul);
        String[] resultat = comptesDeResultat(f.compteImmo());
        List<NouvelleLigne> lignes = new ArrayList<>();
        if (cumul.signum() > 0) {
            lignes.add(ligne(f.compteAmort(), null, cumul, Z, null));
        }
        if (net.signum() > 0) {
            lignes.add(ligne(resultat[0], null, net, Z, null));
        }
        lignes.add(ligne(f.compteImmo(), null, Z, f.valeurBrute(), r.nature()));
        Long sortie = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), date, r.dateSortie(), "SORT-" + f.code(), ("CESSION".equals(r.nature()) ? "Cession : " : "Retrait : ") + f.designation(), "COURANTE", "IMMOBILISATION", null, null, null, false, null, null, lignes)).id();
        Long ecriturePrix = null;
        if (prix.signum() > 0) {
            List<NouvelleLigne> lignesPrix = new ArrayList<>();
            lignesPrix.add(ligne("3481", r.tiersId(), prix.add(tvaCession), Z, null));
            lignesPrix.add(ligne(resultat[1], null, Z, prix, null));
            if (tvaCession.signum() != 0) {
                lignesPrix.add(ligne("4455", null, Z, tvaCession, null));
            }
            ecriturePrix = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), date, r.dateSortie(), "SORT-" + f.code(), "Prix de cession : " + f.designation(), "COURANTE", "IMMOBILISATION", null, null, null, false, null, null, lignesPrix)).id();
        }
        BigDecimal reversement = PlanAmortissement.reversementTva(f.tvaDeduite(), f.dateAcquisition(), r.dateSortie());
        repository.ajouterSortie(livreId, new NouvelleSortie(id, r.nature(), r.dateSortie(), prix, tvaCession, cumul, net, prix.subtract(net), reversement, r.tiersId(), sortie, ecriturePrix, livreCourant.auteur()));
        repository.changerStatut(livreId, id, "SORTIE");
        audit.logAction(ActionAudit.MODIFICATION, "compta_immobilisations", id, r.nature() + " le " + r.dateSortie() + ", valeur nette " + net + ", prix " + prix);
        return dto(livreId, exiger(livreId, id));
    }

    /** Incorporelles 6512 et 7512, corporelles 6513 et 7513, financières 6514 et 7514. */
    private static String[] comptesDeResultat(String compte) {
        if (compte.startsWith("21") || compte.startsWith("22")) {
            return new String[]{"6512", "7512"};
        }
        if (compte.startsWith("24") || compte.startsWith("25")) {
            return new String[]{"6514", "7514"};
        }
        return new String[]{"6513", "7513"};
    }

    @Transactional
    public ImmobilisationDTO annulerSortie(Long id, AnnulationRequest r) {
        Long livreId = livre();
        Fiche f = exiger(livreId, id);
        SortieDTO s = repository.sortieActive(livreId, id).orElseThrow(() -> refus("L'immobilisation " + f.code() + " n'a pas de sortie active.", HttpStatus.CONFLICT, "COMPTA_IMMO_PAS_DE_SORTIE"));
        EcritureDTO e = journal.lire(livreId, s.ecritureSortieId()).orElseThrow();
        exigerOuvert(exercices.lire(livreId, e.exerciceId()).orElseThrow());
        String motif = r.motif().trim();
        contrepasser(livreId, s.ecritureSortieId(), motif);
        if (s.ecriturePrixId() != null) {
            contrepasser(livreId, s.ecriturePrixId(), motif);
        }
        for (Dotation d : repository.complementsActifs(livreId, id)) {
            contrepasser(livreId, d.ecritureId(), motif);
            repository.marquerContrepassee(livreId, d.id());
        }
        repository.annulerSortie(livreId, s.id(), motif);
        repository.changerStatut(livreId, id, "EN_SERVICE");
        audit.logAction(ActionAudit.ANNULATION, "compta_immobilisations", id, "Sortie annulée : " + motif);
        return dto(livreId, exiger(livreId, id));
    }

    /** Pour l'arrêté : les fiches en service dont la dotation manque encore. */
    @Transactional(readOnly = true)
    public long dotationsManquantes(Long livreId, ExerciceDTO exercice) {
        return lignesDues(livreId, exercice).size();
    }
}
