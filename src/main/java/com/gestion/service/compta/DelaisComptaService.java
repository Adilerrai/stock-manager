package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpCalculRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpDeclarationDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpDepotRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpLigneDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpTrimestreDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpValidationRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.JourFerieDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.DelaisComptaRepository;
import com.gestion.repository.compta.DelaisComptaRepository.Complement;
import com.gestion.repository.compta.DelaisComptaRepository.Declaration;
import com.gestion.repository.compta.DelaisComptaRepository.FactureLue;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.FiscalComptaRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.ParametreFiscalRepository;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * La déclaration trimestrielle des délais de paiement, sur les factures fournisseurs : c'est le client en retard qui paie l'amende.
 * Une déclaration à zéro se produit aussi. Pas de fichier XML : le cahier EDI n'a pas été lu, et aucun ICE n'est écrit en dur.
 */
@Service
public class DelaisComptaService {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String TRIMESTRIELLE = "TRIMESTRIELLE";

    private final DelaisComptaRepository repository;
    private final TvaRepository tva;
    private final TiersRepository tiers;
    private final ExerciceRepository exercices;
    private final FiscalComptaRepository fiscal;
    private final ParametreFiscalService parametres;
    private final ParametreFiscalRepository parametresLus;
    private final PosteurEcritures posteur;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final DatesComptables dates;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public DelaisComptaService(DelaisComptaRepository repository, TvaRepository tva, TiersRepository tiers, ExerciceRepository exercices, FiscalComptaRepository fiscal, ParametreFiscalService parametres,
                               ParametreFiscalRepository parametresLus, PosteurEcritures posteur, JournauxRepository journaux, JournalRepository journal, DatesComptables dates, LivreCourantService livreCourant, AuditService audit) {
        this.repository = repository;
        this.tva = tva;
        this.tiers = tiers;
        this.exercices = exercices;
        this.fiscal = fiscal;
        this.parametres = parametres;
        this.parametresLus = parametresLus;
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

    private static void exigerTrimestre(int trimestre) {
        if (trimestre < 1 || trimestre > 4) {
            throw new CommonException("Le trimestre va de 1 à 4.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_DP_TRIMESTRE");
        }
    }

    // --- Lecture

    @Transactional(readOnly = true)
    public List<DpTrimestreDTO> trimestres(int annee) {
        Long livreId = livre();
        Map<Integer, Declaration> lues = repository.declarations(livreId, annee).stream().collect(Collectors.toMap(Declaration::trimestre, d -> d));
        Set<LocalDate> feries = parametresLus.joursFeries();
        List<DpTrimestreDTO> liste = new ArrayList<>();
        for (int t = 1; t <= 4; t++) {
            LocalDate au = TvaCalcul.finDePeriode(annee, TRIMESTRIELLE, t);
            Declaration d = lues.get(t);
            liste.add(new DpTrimestreDTO(annee, t, TvaCalcul.debutDePeriode(annee, TRIMESTRIELLE, t), au, DelaisCalcul.reporter(TvaCalcul.dateLimite(au), feries), d == null ? null : d.id(), d == null ? null : d.statut(),
                    d == null ? Z : d.totalAmende(), d == null ? 0 : d.lignes()));
        }
        return liste;
    }

    @Transactional(readOnly = true)
    public DpDeclarationDTO lire(int annee, int trimestre) {
        exigerTrimestre(trimestre);
        return dto(livre(), annee, trimestre);
    }

    private DpDeclarationDTO dto(Long livreId, int annee, int trimestre) {
        LocalDate du = TvaCalcul.debutDePeriode(annee, TRIMESTRIELLE, trimestre);
        LocalDate au = TvaCalcul.finDePeriode(annee, TRIMESTRIELLE, trimestre);
        Optional<Declaration> lue = repository.declaration(livreId, annee, trimestre);
        if (lue.isEmpty()) {
            return new DpDeclarationDTO(false, null, annee, trimestre, du, au, DelaisCalcul.reporter(TvaCalcul.dateLimite(au), parametresLus.joursFeries()), chiffreAffaires(livreId, du).orElse(Z), Z, Z, Z, Z, Z, "BROUILLON", null, null, null, null, List.of());
        }
        Declaration d = lue.get();
        List<DpLigneDTO> lignes = repository.lignes(livreId, d.id());
        BigDecimal horsDelai = lignes.stream().map(DpLigneDTO::montantHorsDelai).reduce(Z, BigDecimal::add);
        return new DpDeclarationDTO(true, d.id(), annee, trimestre, du, au, d.dateLimite(), d.chiffreAffaires(), d.totalNonPayees(), horsDelai, d.totalAmende(), DelaisCalcul.versement(d.totalAmende()), d.totalLitiges(), d.statut(), d.valideLe(), d.validePar(), d.deposeLe(), d.ecritureAmendeId(), lignes);
    }

    /** Le chiffre d'affaires hors taxes du dernier exercice du livre avant cette date. Sans exercice antérieur, rien n'est supposé. */
    public Optional<BigDecimal> chiffreAffaires(Long livreId, LocalDate avant) {
        return exercices.lister(livreId).stream().filter(e -> e.au().isBefore(avant)).max(Comparator.comparing(ExerciceDTO::au))
                .map(e -> fiscal.soldeHorsCloture(livreId, e.id(), "711").add(fiscal.soldeHorsCloture(livreId, e.id(), "712")).negate().setScale(2, RoundingMode.HALF_UP));
    }

    // --- Calcul

    @Transactional
    public DpDeclarationDTO calculer(int annee, int trimestre, DpCalculRequest r) {
        exigerTrimestre(trimestre);
        Long livreId = livre();
        Optional<Declaration> existante = repository.declaration(livreId, annee, trimestre);
        if (existante.filter(d -> !"BROUILLON".equals(d.statut())).isPresent()) {
            throw new CommonException("La déclaration du trimestre " + trimestre + " est validée : rouvrez-la d'abord.", HttpStatus.CONFLICT, "COMPTA_DP_VALIDEE");
        }
        LocalDate du = TvaCalcul.debutDePeriode(annee, TRIMESTRIELLE, trimestre);
        LocalDate au = TvaCalcul.finDePeriode(annee, TRIMESTRIELLE, trimestre);
        BigDecimal saisi = r == null ? null : r.chiffreAffaires();
        BigDecimal ca = saisi != null ? saisi : chiffreAffaires(livreId, du).or(() -> existante.map(Declaration::chiffreAffaires)).orElseThrow(() -> new CommonException(
                "Aucun exercice antérieur dans le livre : indiquez le chiffre d'affaires hors taxes du dernier exercice.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_DP_CA_ABSENT"));
        BigDecimal minimum = parametres.aLaDate("DP_CA_MINIMUM", "", du).valeur();
        if (ca.compareTo(minimum) <= 0) {
            throw new CommonException("Un chiffre d'affaires de " + minimum.toPlainString() + " DH hors taxes ou moins n'est pas concerné par les délais de paiement (C. com. art. 78-1).", HttpStatus.CONFLICT, "COMPTA_DP_HORS_CHAMP");
        }
        Set<LocalDate> feries = parametresLus.joursFeries();
        List<FactureLue> factures = repository.facturesEmisesEntre(livreId, DelaisCalcul.applicableDepuis(ca), au);
        List<Long> ids = factures.stream().map(FactureLue::id).toList();
        Map<Long, Complement> complements = repository.complements(livreId, ids).stream().collect(Collectors.toMap(Complement::factureId, c -> c));
        Map<Long, List<DelaisCalcul.Paiement>> paiements = paiements(livreId, ids);
        List<DpLigneDTO> lignes = new ArrayList<>();
        BigDecimal litiges = Z;
        for (FactureLue f : factures) {
            Complement c = complements.get(f.id());
            if (exclue(f, c) || nonResident(livreId, f)) {
                continue;
            }
            LocalDate emission = emission(f, c);
            LocalDate echeance = echeance(f, c, feries);
            if (!echeance.isBefore(au)) {
                continue;
            }
            BigDecimal suspendu = Z;
            BigDecimal rattrape = Z;
            if (c != null && c.litigeMontant() != null) {
                LocalDate jugement = c.litigeJugementDate();
                if (jugement == null || jugement.isAfter(au)) {
                    suspendu = c.litigeMontant();
                } else if (!jugement.isBefore(du)) {
                    rattrape = c.litigeMontant();
                }
            }
            LocalDate dateDuTaux = DelaisCalcul.dateDuTaux(echeance);
            DelaisCalcul.Resultat res = DelaisCalcul.calculer(new DelaisCalcul.Entree(f.ttc(), echeance, paiements.getOrDefault(f.id(), List.of()), suspendu, rattrape,
                    parametres.aLaDate("DP_TAUX_DIRECTEUR", "", dateDuTaux).valeur(), parametres.aLaDate("DP_TAUX_MOIS_SUIVANT", "", dateDuTaux).valeur()), du, au);
            if (!res.aDeclarer()) {
                continue;
            }
            litiges = litiges.add(suspendu.add(rattrape));
            lignes.add(new DpLigneDTO(f.id(), f.numero(), assurerTiers(livreId, f), f.fournisseur(), f.ttc(), emission, echeance, res.nonPaye(), res.horsDelai(), res.dernierPaiementHorsDelai(), res.mois(), res.taux().setScale(3, RoundingMode.HALF_UP), res.amende()));
        }
        BigDecimal nonPayees = lignes.stream().map(DpLigneDTO::montantNonPaye).reduce(Z, BigDecimal::add);
        BigDecimal amende = lignes.stream().map(DpLigneDTO::amende).reduce(Z, BigDecimal::add);
        Long id = repository.ecrireDeclaration(livreId, annee, trimestre, ca, nonPayees, amende, litiges, DelaisCalcul.reporter(TvaCalcul.dateLimite(au), feries));
        repository.viderLignes(livreId, id);
        lignes.forEach(l -> repository.ajouterLigne(livreId, id, l));
        return dto(livreId, annee, trimestre);
    }

    private Map<Long, List<DelaisCalcul.Paiement>> paiements(Long livreId, List<Long> factures) {
        List<TvaRepository.Reglement> reglements = tva.reglementsDeFactures(livreId, factures);
        Map<Long, List<TvaRepository.AffectationReglement>> affectees = tva.affectationsReglements(livreId, reglements.stream().map(TvaRepository.Reglement::id).toList()).stream()
                .collect(Collectors.groupingBy(TvaRepository.AffectationReglement::reglementId));
        Map<Long, List<DelaisCalcul.Paiement>> parFacture = new LinkedHashMap<>();
        for (TvaRepository.Reglement x : reglements) {
            List<TvaRepository.AffectationReglement> a = affectees.get(x.id());
            if (a != null) {
                a.forEach(y -> parFacture.computeIfAbsent(y.factureAchatId(), k -> new ArrayList<>()).add(new DelaisCalcul.Paiement(x.date(), y.montant())));
            } else if (x.factureAchatId() != null) {
                parFacture.computeIfAbsent(x.factureAchatId(), k -> new ArrayList<>()).add(new DelaisCalcul.Paiement(x.date(), x.montant()));
            }
        }
        return parFacture;
    }

    /** § 10.5 : les factures de 10 000 DH TTC ou moins restent hors amende si elles sont émises avant le 01/01/2025. */
    private boolean exclue(FactureLue f, Complement c) {
        return parametresLus.aLaDate("DP_EXCLUSION_FACTURE", "", emission(f, c)).filter(p -> f.ttc().compareTo(p.valeur()) <= 0).isPresent();
    }

    private boolean nonResident(Long livreId, FactureLue f) {
        return tiers.parSource(livreId, "FOURNISSEUR", f.fournisseurId()).filter(TiersDTO::nonResident).isPresent();
    }

    private static LocalDate emission(FactureLue f, Complement c) {
        return c != null && c.dateEmission() != null ? c.dateEmission() : f.date();
    }

    /** Le délai convenu de la facture, sinon celui de la fiche fournisseur, sinon le délai légal sans accord. */
    private int delai(FactureLue f, Complement c) {
        if (c != null && c.delaiConvenu() != null) {
            return c.delaiConvenu();
        }
        LocalDate emission = emission(f, c);
        int maximum = parametres.aLaDate("DP_DELAI_JOURS", "CONVENU_MAX", emission).valeur().intValue();
        if (f.delaiFournisseur() != null && f.delaiFournisseur() >= 1) {
            return Math.min(f.delaiFournisseur(), maximum);
        }
        return parametres.aLaDate("DP_DELAI_JOURS", "SANS_ACCORD", emission).valeur().intValue();
    }

    private LocalDate echeance(FactureLue f, Complement c, Set<LocalDate> feries) {
        return DelaisCalcul.echeanceCommerciale(DelaisCalcul.depart(emission(f, c), c == null ? null : c.dateLivraison()), delai(f, c), feries);
    }

    /** Le tiers comptable naît de la première pièce qui le demande, depuis le commercial. */
    Long assurerTiers(Long livreId, FactureLue f) {
        return tiers.parSource(livreId, "FOURNISSEUR", f.fournisseurId()).map(TiersDTO::id).orElseGet(() -> {
            var identite = tiers.identiteSource(livreId, "FOURNISSEUR", f.fournisseurId());
            if (identite.isEmpty()) {
                return tiers.creer(livreId, new TiersCreationRequest("FOURNISSEUR", f.fournisseur(), null, null, "4411", null, null, null));
            }
            return tiers.creerDepuisSource(livreId, "FOURNISSEUR", f.fournisseurId(), identite.get().nom(), identite.get().ice(), identite.get().identifiantFiscal(), "4411");
        });
    }

    // --- Validation, réouverture, dépôt

    /** L'amende, au dirham supérieur : D 65831, non déductible / C 4458. Le compte de dette reste à faire vérifier. */
    @Transactional
    public DpDeclarationDTO valider(int annee, int trimestre, DpValidationRequest r) {
        Long livreId = livre();
        exigerTrimestre(trimestre);
        Declaration d = exigerDeclaration(livreId, annee, trimestre);
        if (!"BROUILLON".equals(d.statut())) {
            throw new CommonException("La déclaration est déjà validée.", HttpStatus.CONFLICT, "COMPTA_DP_VALIDEE");
        }
        calculer(annee, trimestre, new DpCalculRequest(d.chiffreAffaires()));
        Declaration lue = exigerDeclaration(livreId, annee, trimestre);
        BigDecimal versement = DelaisCalcul.versement(lue.totalAmende());
        Long ecriture = null;
        if (versement.signum() > 0) {
            JournalDTO od = journal(livreId);
            ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), dates.resoudre(livreId, od.id(), r.dateEcriture()), r.dateEcriture(), "DP-" + annee + "-T" + trimestre, "Amende pour délais de paiement, T" + trimestre + " " + annee,
                    "COURANTE", "DELAIS", null, null, null, false, null, null, List.of(ligne("65831", versement, Z), ligne("4458", Z, versement)))).id();
        }
        repository.valider(livreId, lue.id(), ecriture, livreCourant.auteur());
        audit.logAction(ActionAudit.VALIDATION, "compta_dp_declarations", lue.id(), "Délais de paiement T" + trimestre + " " + annee + " : amende " + lue.totalAmende());
        return dto(livreId, annee, trimestre);
    }

    @Transactional
    public DpDeclarationDTO rouvrir(int annee, int trimestre, MotifRequest r) {
        Long livreId = livre();
        Declaration d = exigerDeclaration(livreId, annee, trimestre);
        if (!"VALIDEE".equals(d.statut())) {
            throw new CommonException("Seule une déclaration validée et non déposée se rouvre.", HttpStatus.CONFLICT, "COMPTA_DP_NON_VALIDEE");
        }
        if (d.ecritureAmendeId() != null) {
            EcritureDTO origine = journal.lire(livreId, d.ecritureAmendeId()).orElseThrow();
            List<NouvelleLigne> miroir = new ArrayList<>();
            for (LigneJournal l : journal.lignesBrutes(livreId, d.ecritureAmendeId())) {
                miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(), l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
            }
            posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), dates.resoudre(livreId, origine.journal().id(), origine.dateEcriture()), origine.datePiece(), origine.refPiece(), "Contre-passation de " + origine.numeroComplet(),
                    "COURANTE", "DELAIS", d.ecritureAmendeId(), null, r.motif().trim(), false, null, null, miroir));
        }
        repository.rouvrir(livreId, d.id());
        audit.logAction(ActionAudit.ANNULATION, "compta_dp_declarations", d.id(), "Réouverture : " + r.motif().trim());
        return dto(livreId, annee, trimestre);
    }

    @Transactional
    public DpDeclarationDTO deposer(int annee, int trimestre, DpDepotRequest r) {
        Long livreId = livre();
        Declaration d = exigerDeclaration(livreId, annee, trimestre);
        if (!"VALIDEE".equals(d.statut())) {
            throw new CommonException("Validez d'abord la déclaration.", HttpStatus.CONFLICT, "COMPTA_DP_NON_VALIDEE");
        }
        repository.deposer(livreId, d.id(), r.deposeLe());
        audit.logAction(ActionAudit.MODIFICATION, "compta_dp_declarations", d.id(), "Dépôt enregistré le " + r.deposeLe());
        return dto(livreId, annee, trimestre);
    }

    private Declaration exigerDeclaration(Long livreId, int annee, int trimestre) {
        return repository.declaration(livreId, annee, trimestre).orElseThrow(() -> new CommonException("Calculez d'abord la déclaration du trimestre " + trimestre + ".", HttpStatus.CONFLICT, "COMPTA_DP_NON_CALCULEE"));
    }

    private JournalDTO journal(Long livreId) {
        return journaux.parType(livreId, "OD").orElseThrow(() -> new CommonException("Le journal OD n'existe pas.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
    }

    private static NouvelleLigne ligne(String compte, BigDecimal debit, BigDecimal credit) {
        return new NouvelleLigne(compte, null, debit, credit, null, null, null, false, null, null, null, null, null, null);
    }

    // --- Fiche de la facture

    @Transactional(readOnly = true)
    public DpFactureDTO fiche(Long factureId) {
        Long livreId = livre();
        return ficheDe(livreId, exigerFacture(livreId, factureId));
    }

    private DpFactureDTO ficheDe(Long livreId, FactureLue f) {
        Complement c = repository.complements(livreId, List.of(f.id())).stream().findFirst().orElse(null);
        return new DpFactureDTO(f.id(), f.numero(), f.fournisseur(), f.ttc(), emission(f, c), c == null ? null : c.dateLivraison(), c == null ? null : c.delaiConvenu(), delai(f, c), echeance(f, c, parametresLus.joursFeries()),
                c == null ? null : c.litigeMontant(), c == null ? null : c.litigeDate(), c == null ? null : c.litigeJugementDate());
    }

    @Transactional
    public DpFactureDTO modifierFiche(Long factureId, DpFactureRequest r) {
        Long livreId = livre();
        FactureLue f = exigerFacture(livreId, factureId);
        if (r.delaiConvenuJours() != null && (r.delaiConvenuJours() < 1 || r.delaiConvenuJours() > 120)) {
            throw new CommonException("Le délai convenu va de 1 à 120 jours (C. com. art. 78-2).", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_DP_DELAI");
        }
        if (r.litigeMontant() != null && (r.litigeMontant().signum() <= 0 || r.litigeDate() == null)) {
            throw new CommonException("Un litige porte un montant positif et la date du recours devant le tribunal.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_DP_LITIGE");
        }
        repository.ecrireComplement(livreId, factureId, r.dateEmission() != null ? r.dateEmission() : f.date(), r);
        audit.logAction(ActionAudit.MODIFICATION, "compta_dp_factures", factureId, "Facture " + f.numero() + " : délai " + r.delaiConvenuJours());
        return ficheDe(livreId, f);
    }

    private FactureLue exigerFacture(Long livreId, Long id) {
        return repository.facture(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Facture d'achat", "id", id));
    }

    // --- État annexe, à viser par un tiers

    @Transactional(readOnly = true)
    public byte[] etatXlsx(int annee, int trimestre) {
        exigerTrimestre(trimestre);
        Long livreId = livre();
        DpDeclarationDTO d = dto(livreId, annee, trimestre);
        return TvaFichiers.classeur(c -> {
            Sheet f = c.createSheet("État annexe");
            TvaFichiers.ligne(f, 0, "Facture", "Fournisseur", "Émise le", "Montant TTC", "Échéance", "Non payé", "Payé hors délai", "Payé le", "Mois de retard", "Taux %", "Amende");
            int n = 1;
            for (DpLigneDTO l : d.lignes()) {
                TvaFichiers.ligne(f, n++, l.numeroFacture(), l.fournisseur(), l.dateEmission(), l.montantTtc(), l.dateEcheance(), l.montantNonPaye(), l.montantHorsDelai(), l.datePaiement(), l.moisRetard(), l.tauxApplique(), l.amende());
            }
            TvaFichiers.ligne(f, n + 1, "Total", null, null, null, null, d.totalNonPayees(), d.totalHorsDelai(), null, null, null, d.totalAmende());
            TvaFichiers.ligne(f, n + 2, "Versement au dirham supérieur", null, null, null, null, null, null, null, null, null, d.versement());
            TvaFichiers.ligne(f, n + 3, "Ce tableau n'est pas le fichier officiel : le cahier EDI n'a pas été lu.");
        });
    }

    @Transactional(readOnly = true)
    public List<JourFerieDTO> joursFeries() {
        return parametresLus.listerFeries();
    }
}
