package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ParametreFiscalDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.AcompteDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.AcomptesDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.CelluleSaisieDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DeficitDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DeficitRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.IsDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LiquidationIsRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.PaiementAcompteRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.SourceFiscaleDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.FiscalComptaRepository;
import com.gestion.repository.compta.FiscalComptaRepository.CalculIs;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.repository.compta.ParametreFiscalRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L'IS de l'exercice, depuis le résultat comptable et le tableau T3. Sans taux à la date d'ouverture, le calcul est refusé : aucun repli.
 * Le bénéfice de référence des acomptes se relit sur l'exercice clos, jamais sur un CPC recalculé.
 */
@Service
public class IsComptaService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final String LIGNE_IS_T3 = "T3.REINT_NC.IS";
    /** Art. 144-I : chiffre d'affaires et autres produits d'exploitation, produits financiers, subventions et dons. Le détail reste à faire valider. */
    private static final List<String> COMPTES_BASE_CM = List.of("711", "716", "718", "732", "733", "738", "7577", "7578");
    private static final Set<String> BLOCS_REINT = Set.of("T3.REINT_COUR", "T3.REINT_NC");
    private static final Set<String> BLOCS_DEDUC = Set.of("T3.DEDUC_COUR", "T3.DEDUC_NC");

    private final FiscalComptaRepository repository;
    private final ExerciceRepository exercices;
    private final LivreRepository livres;
    private final ParametreFiscalRepository parametres;
    private final EtatService etats;
    private final LivreCourantService livreCourant;
    private final PosteurEcritures posteur;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final PeriodeRepository periodes;
    private final DatesComptables dates;
    private final CorrespondanceService correspondances;
    private final AuditService audit;
    private final ObjectMapper json;

    public IsComptaService(FiscalComptaRepository repository, ExerciceRepository exercices, LivreRepository livres, ParametreFiscalRepository parametres, EtatService etats, LivreCourantService livreCourant, PosteurEcritures posteur, JournauxRepository journaux,
                           JournalRepository journal, PeriodeRepository periodes, DatesComptables dates, CorrespondanceService correspondances, AuditService audit, ObjectMapper json) {
        this.repository = repository;
        this.exercices = exercices;
        this.livres = livres;
        this.parametres = parametres;
        this.etats = etats;
        this.livreCourant = livreCourant;
        this.posteur = posteur;
        this.journaux = journaux;
        this.journal = journal;
        this.periodes = periodes;
        this.dates = dates;
        this.correspondances = correspondances;
        this.audit = audit;
        this.json = json;
    }

    private Long livre() {
        return livreCourant.exiger().livreId();
    }

    private ExerciceDTO exiger(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }

    private ParametreFiscalDTO parametre(String code, String cle, LocalDate date) {
        return parametres.aLaDate(code, cle, date).orElseThrow(() -> new CommonException("Aucune valeur de " + code + (cle.isEmpty() ? "" : " (" + cle + ")") + " au " + date + " : le calcul ne peut pas se faire.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_PARAMETRE_ABSENT"));
    }

    // --- Calcul

    /** Le calcul le plus récent, sans rien écrire. Le premier passage demande un calcul. */
    @Transactional(readOnly = true)
    public IsDTO lire(Long exerciceId) {
        Long livreId = livre();
        return dto(livreId, exiger(livreId, exerciceId));
    }

    @Transactional
    public IsDTO calculer(Long exerciceId) {
        Long livreId = livre();
        ExerciceDTO e = exiger(livreId, exerciceId);
        if (repository.calculIs(livreId, exerciceId).filter(c -> "VALIDE".equals(c.statut())).isPresent()) {
            throw new CommonException("L'IS de " + e.code() + " est validé : rouvrez-le d'abord.", HttpStatus.CONFLICT, "COMPTA_IS_VALIDE");
        }
        var livre = livres.identite(livreId);
        BigDecimal chargeIs = repository.soldeHorsCloture(livreId, exerciceId, "670");
        BigDecimal comptable = etats.resultatNet(livreId, e).add(chargeIs);
        BigDecimal reint = Z;
        BigDecimal deduc = Z;
        for (CelluleSaisieDTO s : repository.saisiesDuTableau(livreId, exerciceId, "T3")) {
            if (s.valeurNum() == null || LIGNE_IS_T3.equals(s.codeLigne())) {
                continue;
            }
            String bloc = s.codeLigne().contains(".") ? s.codeLigne().substring(0, s.codeLigne().lastIndexOf('.')) : s.codeLigne();
            if (BLOCS_REINT.contains(bloc)) {
                reint = reint.add(s.valeurNum());
            } else if (BLOCS_DEDUC.contains(bloc)) {
                deduc = deduc.add(s.valeurNum());
            }
        }
        BigDecimal brut = comptable.add(reint).subtract(deduc);
        List<Map<String, Object>> imputations = new ArrayList<>();
        BigDecimal imputes = imputer(livreId, e, brut, imputations);
        BigDecimal fiscal = brut.subtract(imputes);
        BigDecimal benefice = fiscal.max(Z);
        String categorie = livre.categorieIs() == null ? "GENERAL" : livre.categorieIs();
        String cle = IsCalcul.cleDeTaux(categorie, benefice);
        LocalDate ouverture = e.du();
        ParametreFiscalDTO taux = parametre("IS_TAUX", cle, ouverture);
        ParametreFiscalDTO tauxCm = parametre("CM_TAUX", "GENERAL", ouverture);
        ParametreFiscalDTO minimum = parametre("CM_MINIMUM", "SOCIETE", ouverture);
        String cleCss = IsCalcul.cleCss(benefice);
        var css = cleCss == null ? java.util.Optional.<ParametreFiscalDTO>empty() : parametres.aLaDate("CSS_TAUX", cleCss, e.au());
        BigDecimal baseCm = repository.produits(livreId, exerciceId, COMPTES_BASE_CM);
        boolean exonere = IsCalcul.exonereDeCm(livre.dateConstitution(), livre.dateDebutExploitation(), e.au());
        BigDecimal acomptes = repository.acomptes(livreId, exerciceId).stream().map(AcompteDTO::montantPaye).filter(java.util.Objects::nonNull).reduce(Z, BigDecimal::add);
        BigDecimal anterieur = precedent(livreId, e).flatMap(p -> repository.calculIs(livreId, p.id())).map(CalculIs::excedent).orElse(Z);
        IsCalcul.Resultat r = IsCalcul.calculer(new IsCalcul.Entree(fiscal, taux.valeur(), baseCm, tauxCm.valeur(), minimum.valeur(), exonere, acomptes, anterieur, css.map(ParametreFiscalDTO::valeur).orElse(BigDecimal.ZERO)));
        Map<String, Object> memoire = new LinkedHashMap<>();
        List<ParametreFiscalDTO> sources = new ArrayList<>(List.of(taux, tauxCm, minimum));
        css.ifPresent(sources::add);
        memoire.put("sources", sources.stream().map(p -> new SourceFiscaleDTO(p.code(), p.cle(), p.valideDu(), p.valeur(), p.source(), p.fiabilite())).toList());
        memoire.put("imputations", imputations);
        repository.ecrireCalcul(livreId, exerciceId, new CalculIs(null, cle, comptable, reint, deduc, imputes, fiscal, r.baseArrondie(), taux.valeur().setScale(3, RoundingMode.HALF_UP), r.isTheorique(), baseCm, r.cm(), exonere, r.impotDu(), acomptes, anterieur, r.reliquat(), r.excedent(), r.css(),
                ecrire(memoire), "BROUILLON", null, null, null, null));
        return dto(livreId, e);
    }

    /** Du plus ancien au plus récent : quatre exercices hors amortissements (art. 12), sans limite pour les amortissements. */
    private BigDecimal imputer(Long livreId, ExerciceDTO e, BigDecimal brut, List<Map<String, Object>> imputations) {
        BigDecimal disponible = brut.max(Z);
        BigDecimal total = Z;
        int annee = e.du().getYear();
        for (DeficitDTO d : repository.deficits(livreId)) {
            if (disponible.signum() <= 0) {
                break;
            }
            boolean porteur = "AMORTISSEMENTS".equals(d.nature()) || annee - d.anneeOrigine() <= 4;
            if (d.anneeOrigine() >= annee || !porteur || d.montantRestant().signum() <= 0) {
                continue;
            }
            BigDecimal part = d.montantRestant().min(disponible);
            disponible = disponible.subtract(part);
            total = total.add(part);
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("id", d.id());
            i.put("annee", d.anneeOrigine());
            i.put("montant", part.toPlainString());
            imputations.add(i);
        }
        return total;
    }

    private java.util.Optional<ExerciceDTO> precedent(Long livreId, ExerciceDTO e) {
        return exercices.lister(livreId).stream().filter(x -> x.au().equals(e.du().minusDays(1))).findFirst();
    }

    private String ecrire(Object valeur) {
        try {
            return json.writeValueAsString(valeur);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private IsDTO dto(Long livreId, ExerciceDTO e) {
        boolean periodeOuverte = exercicePeriode99Ouverte(livreId, e);
        return repository.calculIs(livreId, e.id()).map(c -> {
            List<SourceFiscaleDTO> sources = new ArrayList<>();
            try {
                JsonNode n = json.readTree(c.parametres()).path("sources");
                for (JsonNode s : n) {
                    sources.add(new SourceFiscaleDTO(s.path("code").asText(), s.path("cle").asText(), LocalDate.parse(s.path("valideDu").asText()), new BigDecimal(s.path("valeur").asText()), s.path("source").asText(), s.path("fiabilite").asText()));
                }
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
            return new IsDTO(e.id(), e.code(), true, c.categorie(), c.resultatComptable(), c.reintegrations(), c.deductions(), c.deficits(), c.resultatFiscal(), c.baseArrondie(), c.tauxIs(), c.isTheorique(), c.baseCm(), c.cm(), c.cmExoneree(), c.impotDu(), c.acomptes(),
                    c.excedentAnterieur(), c.reliquat(), c.excedent(), c.css(), c.statut(), c.ecritureCharge(), c.ecritureLiquidation(), c.valideLe(), c.validePar(), sources, periodeOuverte);
        }).orElseGet(() -> new IsDTO(e.id(), e.code(), false, "", Z, Z, Z, Z, Z, Z, Z, Z, Z, Z, false, Z, Z, Z, Z, Z, Z, "BROUILLON", null, null, null, null, List.of(), periodeOuverte));
    }

    private boolean exercicePeriode99Ouverte(Long livreId, ExerciceDTO e) {
        return periodes.lister(livreId, e.id()).stream().anyMatch(p -> "CLOTURE".equals(p.typePeriode()) && "OUVERTE".equals(p.statut()));
    }

    // --- Validation

    /** Charge d'impôt en période 99 : D 6701 / C 4453. Le déficit de l'exercice et les imputations se reportent, et T3 reprend l'impôt. */
    @Transactional
    public IsDTO valider(Long exerciceId) {
        Long livreId = livre();
        ExerciceDTO e = exiger(livreId, exerciceId);
        calculer(exerciceId);
        CalculIs c = repository.calculIs(livreId, exerciceId).orElseThrow();
        if ("VALIDE".equals(c.statut())) {
            throw new CommonException("L'IS de " + e.code() + " est déjà validé.", HttpStatus.CONFLICT, "COMPTA_IS_VALIDE");
        }
        Long ecriture = null;
        if (c.impotDu().signum() > 0) {
            JournalDTO od = journal(livreId, "OD");
            ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), e.au(), e.au(), "IS-" + e.code(), "Impôt sur les résultats " + e.code(), "INVENTAIRE", "IS", null, null, null, false, null, null,
                    List.of(ligne("6701", c.impotDu(), Z), ligne("4453", Z, c.impotDu())))).id();
            repository.ecrireSaisie(livreId, exerciceId, "T3", new CelluleSaisieDTO(LIGNE_IS_T3, "MONTANT_1", c.impotDu(), "Impôt sur les résultats"), livreCourant.auteur());
        }
        appliquerDeficits(livreId, e, c);
        repository.validerIs(livreId, exerciceId, ecriture, livreCourant.auteur());
        audit.logAction(ActionAudit.VALIDATION, "compta_is_calculs", c.id(), "IS de " + e.code() + " : base " + c.baseArrondie() + ", impôt " + c.impotDu());
        return dto(livreId, e);
    }

    private void appliquerDeficits(Long livreId, ExerciceDTO e, CalculIs c) {
        Map<Long, BigDecimal> restes = new LinkedHashMap<>();
        repository.deficits(livreId).forEach(d -> restes.put(d.id(), d.montantRestant()));
        for (JsonNode i : imputations(c)) {
            Long id = i.path("id").asLong();
            repository.modifierRestant(livreId, id, restes.getOrDefault(id, Z).subtract(new BigDecimal(i.path("montant").asText())));
            int ecart = e.du().getYear() - i.path("annee").asInt();
            if (ecart >= 1 && ecart <= 4) {
                repository.ecrireSaisie(livreId, e.id(), "T3", new CelluleSaisieDTO("T3.REPORT_N" + ecart, "MONTANT_1", new BigDecimal(i.path("montant").asText()), null), livreCourant.auteur());
            }
        }
        if (c.resultatFiscal().signum() < 0) {
            repository.ecrireDeficit(livreId, e.id(), e.du().getYear(), "HORS_AMORT", c.resultatFiscal().negate(), false);
        }
    }

    private JsonNode imputations(CalculIs c) {
        try {
            return json.readTree(c.parametres()).path("imputations");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JournalDTO journal(Long livreId, String type) {
        return journaux.parType(livreId, type).orElseThrow(() -> new CommonException("Le journal " + type + " n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
    }

    private static NouvelleLigne ligne(String compte, BigDecimal debit, BigDecimal credit) {
        return new NouvelleLigne(compte, null, debit, credit, null, null, null, false, null, null, null, null, null, null);
    }

    @Transactional
    public IsDTO rouvrir(Long exerciceId, MotifRequest r) {
        Long livreId = livre();
        ExerciceDTO e = exiger(livreId, exerciceId);
        CalculIs c = repository.calculIs(livreId, exerciceId).filter(x -> "VALIDE".equals(x.statut())).orElseThrow(() -> new CommonException("L'IS n'est pas validé.", HttpStatus.CONFLICT, "COMPTA_IS_NON_VALIDE"));
        if (!"OUVERT".equals(e.statut())) {
            throw new CommonException("L'exercice " + e.code() + " n'est pas ouvert : levez d'abord l'arrêté.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_OUVERT");
        }
        if (c.ecritureCharge() != null) {
            EcritureDTO origine = journal.lire(livreId, c.ecritureCharge()).orElseThrow();
            List<NouvelleLigne> miroir = new ArrayList<>();
            for (LigneJournal l : journal.lignesBrutes(livreId, c.ecritureCharge())) {
                miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
            }
            posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), e.au(), origine.datePiece(), origine.refPiece(), "Contre-passation de " + origine.numeroComplet(), "INVENTAIRE", "IS", c.ecritureCharge(), null, r.motif().trim(), false, null, null, miroir));
        }
        repository.retirerSaisie(livreId, exerciceId, "T3", LIGNE_IS_T3, "MONTANT_1");
        Map<Long, BigDecimal> restes = new LinkedHashMap<>();
        repository.deficits(livreId).forEach(d -> restes.put(d.id(), d.montantRestant()));
        for (JsonNode i : imputations(c)) {
            Long id = i.path("id").asLong();
            repository.modifierRestant(livreId, id, restes.getOrDefault(id, Z).add(new BigDecimal(i.path("montant").asText())));
            int ecart = e.du().getYear() - i.path("annee").asInt();
            repository.retirerSaisie(livreId, exerciceId, "T3", "T3.REPORT_N" + ecart, "MONTANT_1");
        }
        if (c.resultatFiscal().signum() < 0) {
            repository.retirerDeficit(livreId, e.du().getYear(), "HORS_AMORT");
        }
        repository.rouvrirIs(livreId, exerciceId);
        audit.logAction(ActionAudit.ANNULATION, "compta_is_calculs", c.id(), "IS rouvert : " + r.motif().trim());
        return dto(livreId, e);
    }

    /** Pour l'arrêté : vrai si l'IS de l'exercice est validé. */
    @Transactional(readOnly = true)
    public boolean estValide(Long livreId, Long exerciceId) {
        return repository.calculIs(livreId, exerciceId).filter(c -> "VALIDE".equals(c.statut())).isPresent();
    }

    // --- Acomptes

    /**
     * Quatre échéances depuis l'ouverture de l'exercice. Le bénéfice de référence est celui de l'exercice clos précédent ; un premier exercice n'a rien à calculer.
     * Pendant la transition, l'acompte suit le taux de l'exercice en cours (IS-05) ; ensuite, 25 % de l'impôt dû de référence (IS-04).
     */
    @Transactional
    public AcomptesDTO acomptes(Long exerciceId) {
        Long livreId = livre();
        ExerciceDTO e = exiger(livreId, exerciceId);
        var reference = precedent(livreId, e).filter(p -> "CLOTURE".equals(p.statut()));
        if (reference.isEmpty()) {
            return new AcomptesDTO(null, null, "Pas d'exercice clos de référence : aucun acompte à calculer pour un premier exercice.", repository.acomptes(livreId, exerciceId));
        }
        ExerciceDTO ref = reference.get();
        BigDecimal benefice = ref.resultatNet() == null ? Z : ref.resultatNet();
        var calcul = repository.calculIs(livreId, ref.id());
        BigDecimal baseBenefice = calcul.map(CalculIs::resultatFiscal).orElse(benefice).max(Z);
        BigDecimal part = parametre("IS_ACOMPTE_PART", "", e.du()).valeur();
        BigDecimal montant;
        if (e.du().getYear() <= 2026) {
            String categorie = livres.identite(livreId).categorieIs();
            BigDecimal taux = parametre("IS_TAUX", IsCalcul.cleDeTaux(categorie, baseBenefice), e.du()).valeur();
            montant = IsCalcul.acompte(baseBenefice, taux, part);
        } else {
            BigDecimal impot = calcul.map(CalculIs::impotDu).orElse(Z);
            montant = IsCalcul.pourcent(impot, part).setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.HALF_UP);
        }
        for (int n = 1; n <= 4; n++) {
            repository.ecrireAcompte(livreId, exerciceId, n, IsCalcul.echeanceAcompte(e.du(), n), montant);
        }
        return new AcomptesDTO(ref.code(), baseBenefice, null, repository.acomptes(livreId, exerciceId));
    }

    /** D 3453 / C banque, journal de banque. */
    @Transactional
    public AcomptesDTO payerAcompte(Long exerciceId, int numero, PaiementAcompteRequest r) {
        Long livreId = livre();
        ExerciceDTO e = exiger(livreId, exerciceId);
        AcompteDTO a = repository.acomptes(livreId, exerciceId).stream().filter(x -> x.numero() == numero).findFirst().orElseThrow(() -> new ResourceNotFoundException("Acompte", "numero", (long) numero));
        if (a.ecritureId() != null) {
            throw new CommonException("L'acompte " + numero + " est déjà payé.", HttpStatus.CONFLICT, "COMPTA_IS_ACOMPTE_PAYE");
        }
        if (r.montant().signum() <= 0) {
            throw new CommonException("Le montant doit être positif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IS_MONTANT");
        }
        String banque = compteBanque(livreId, r.compteFinancierId());
        JournalDTO bq = journal(livreId, "BANQUE");
        Long ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(bq.id(), dates.resoudre(livreId, bq.id(), r.datePaiement()), r.datePaiement(), "IS-AC" + numero + "-" + e.code(), "Acompte " + numero + " d'IS " + e.code(), "COURANTE", "IS", null, null, null, false, null, null,
                List.of(ligne("3453", r.montant(), Z), ligne(banque, Z, r.montant())))).id();
        repository.payerAcompte(livreId, exerciceId, numero, r.montant(), r.datePaiement(), r.reference(), ecriture);
        audit.logAction(ActionAudit.MODIFICATION, "compta_is_acomptes", ecriture, "Acompte " + numero + " de " + e.code() + " : " + r.montant());
        return acomptes(exerciceId);
    }

    private String compteBanque(Long livreId, Long compteFinancierId) {
        Map<String, String> table = correspondances.assurer(livreId);
        String compte = compteFinancierId == null ? null : table.get("COMPTE_FINANCIER|" + compteFinancierId);
        return compte != null ? compte : table.getOrDefault("BANQUE_DEFAUT|", "5141");
    }

    /** En N+1 : D 4453 / C 3453 pour les acomptes / C banque pour le reliquat. L'excédent reste au 3453 et s'impute sur les acomptes suivants. */
    @Transactional
    public IsDTO liquider(Long exerciceId, LiquidationIsRequest r) {
        Long livreId = livre();
        ExerciceDTO e = exiger(livreId, exerciceId);
        CalculIs c = repository.calculIs(livreId, exerciceId).filter(x -> "VALIDE".equals(x.statut())).orElseThrow(() -> new CommonException("Validez d'abord l'IS de " + e.code() + ".", HttpStatus.CONFLICT, "COMPTA_IS_NON_VALIDE"));
        if (c.ecritureLiquidation() != null) {
            throw new CommonException("L'IS de " + e.code() + " est déjà liquidé.", HttpStatus.CONFLICT, "COMPTA_IS_LIQUIDE");
        }
        BigDecimal impute = c.impotDu().subtract(c.reliquat());
        List<NouvelleLigne> lignes = new ArrayList<>();
        lignes.add(ligne("4453", c.impotDu(), Z));
        if (impute.signum() > 0) {
            lignes.add(ligne("3453", Z, impute));
        }
        if (c.reliquat().signum() > 0) {
            lignes.add(ligne(compteBanque(livreId, r.compteFinancierId()), Z, c.reliquat()));
        }
        JournalDTO bq = journal(livreId, "BANQUE");
        Long ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(bq.id(), dates.resoudre(livreId, bq.id(), r.datePaiement()), r.datePaiement(), "IS-LIQ-" + e.code(), "Liquidation de l'IS " + e.code(), "COURANTE", "IS", null, null, null, false, null, null, lignes)).id();
        repository.lierLiquidation(livreId, exerciceId, ecriture);
        audit.logAction(ActionAudit.MODIFICATION, "compta_is_calculs", c.id(), "IS de " + e.code() + " liquidé, reliquat " + c.reliquat());
        return dto(livreId, e);
    }

    // --- Déficits

    @Transactional(readOnly = true)
    public List<DeficitDTO> deficits() {
        return repository.deficits(livre());
    }

    /** La reprise des déficits antérieurs au démarrage. */
    @Transactional
    public List<DeficitDTO> saisirDeficit(DeficitRequest r) {
        Long livreId = livre();
        if (!Set.of("HORS_AMORT", "AMORTISSEMENTS").contains(r.nature())) {
            throw new CommonException("Nature de déficit inconnue : " + r.nature() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IS_DEFICIT");
        }
        if (r.montant().signum() <= 0) {
            throw new CommonException("Le montant doit être positif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_IS_DEFICIT");
        }
        repository.ecrireDeficit(livreId, null, r.anneeOrigine(), r.nature(), r.montant(), true);
        return repository.deficits(livreId);
    }

    /** Pour les messages : les mois écoulés d'une date à l'autre. */
    static long mois(LocalDate du, LocalDate au) {
        return ChronoUnit.MONTHS.between(du, au);
    }
}
