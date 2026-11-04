package com.gestion.service.compta;

import com.gestion.persistent.dto.compta.RapprochementDtos.LigneClasseeDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresDTO;
import com.gestion.repository.compta.RapprochementComptaRepository;
import com.gestion.repository.compta.SourcesRepository;
import com.gestion.repository.compta.TvaRepository;
import com.gestion.repository.compta.TvaRepository.Affectation;
import com.gestion.repository.compta.TvaRepository.AffectationReglement;
import com.gestion.repository.compta.TvaRepository.ChargeImport;
import com.gestion.repository.compta.TvaRepository.Cumul;
import com.gestion.repository.compta.TvaRepository.Declaration;
import com.gestion.repository.compta.TvaRepository.DepenseVentilee;
import com.gestion.repository.compta.TvaRepository.Encaissement;
import com.gestion.repository.compta.TvaRepository.EnTetePiece;
import com.gestion.repository.compta.TvaRepository.FactureAchat;
import com.gestion.repository.compta.TvaRepository.LignePiece;
import com.gestion.repository.compta.TvaRepository.NouvelleCollectee;
import com.gestion.repository.compta.TvaRepository.NouvelleDeductible;
import com.gestion.repository.compta.TvaRepository.OperationCollectee;
import com.gestion.repository.compta.TvaRepository.OperationDeductible;
import com.gestion.repository.compta.TvaRepository.Reglement;
import com.gestion.repository.compta.TvaRepository.VentilationAchat;
import com.gestion.service.compta.FormulaireTva.Rubrique;
import com.gestion.service.compta.PieceSource.LigneSource;
import com.gestion.service.compta.TvaCalcul.BaseTaxe;
import com.gestion.service.compta.TvaCalcul.PartTaux;
import com.gestion.service.compta.TvaCalcul.Periode;
import com.gestion.service.compta.TvaCalcul.Tranche;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le calcul d'une période : des paiements aux opérations, et des opérations au formulaire.
 * Les comptes 4455 et 3455 ne servent ici à rien : ils ne viennent qu'au contrôle T29.
 */
@Component
public class TvaOperations {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final String ACOMPTE = "ACOMPTE";

    private final TvaRepository repository;
    private final SourcesRepository sources;
    private final FormulaireTva formulaire;
    private final RapprochementComptaRepository rapprochement;
    private final LivreCourantService livreCourant;

    public TvaOperations(TvaRepository repository, SourcesRepository sources, FormulaireTva formulaire, RapprochementComptaRepository rapprochement, LivreCourantService livreCourant) {
        this.rapprochement = rapprochement;
        this.livreCourant = livreCourant;
        this.repository = repository;
        this.sources = sources;
        this.formulaire = formulaire;
    }

    /** Une pièce de vente éclatée par taux, net de remise. */
    private record PieceVentilee(Long factureId, String numero, LocalDate date, List<PartTaux> parts, Map<BigDecimal, Map<String, BigDecimal>> lignes, Map<BigDecimal, String> traitements) {
    }

    private record Imputation(PieceVentilee piece, BigDecimal montant) {
    }

    /** Ce qui reste de la main de l'utilisateur quand on recalcule : une correction d'opération ne se perd pas. */
    private record SurchargeCollectee(BigDecimal taux, String traitement, String ligne, String motif) {
    }

    private record SurchargeDeductible(String rubrique, String ligne, String compte, String designation, String numero, String fiscal, String ice, String reference, String motif) {
    }

    /** Les surcharges se lisent avant de vider : elles sont réappliquées par clé métier. */
    public record Surcharges(Map<String, Object> collectees, Map<String, Object> deductibles) {
    }

    public Surcharges lireSurcharges(Long livreId, Long declarationId) {
        Map<String, Object> c = new HashMap<>();
        for (OperationCollectee o : repository.operationsCollectees(livreId, declarationId)) {
            if (o.motif() != null) {
                c.put(cleCollectee(o.sourceType(), o.sourceId(), o.factureId(), o.factureNumero() == null ? null : o.taux()), new SurchargeCollectee(o.taux(), o.traitement(), o.ligneCode(), o.motif()));
            }
        }
        Map<String, Object> d = new HashMap<>();
        for (OperationDeductible o : repository.operationsDeductibles(livreId, declarationId)) {
            if (o.motif() != null) {
                d.put(cle(o.sourceType(), o.sourceId(), o.factureAchatId(), o.taux()), new SurchargeDeductible(o.rubrique(), o.ligneCode(), o.compteTva(), o.designation(), o.numeroFacture(), o.fournisseurIf(), o.fournisseurIce(), o.reference(), o.motif()));
            }
        }
        return new Surcharges(c, d);
    }

    private static String cle(String type, Long id, Long facture, BigDecimal taux) {
        return type + "|" + id + "|" + (facture == null ? 0 : facture) + "|" + FormulaireTva.cleTaux(taux);
    }

    private static String cleCollectee(String type, Long id, Long facture, BigDecimal taux) {
        return taux == null ? type + "|" + id + "|0|" + ACOMPTE : cle(type, id, facture, taux);
    }

    // --- TVA collectée

    public void collecter(Long livreId, Declaration d, String remiseMode, Surcharges s, Periode cumul) {
        boolean debit = "DEBIT".equals(d.faitGenerateur());
        Set<String> deja = repository.dejaDeclarees(livreId, true);
        List<Encaissement> encaissements = repository.encaissements(livreId, d.du(), d.au());
        Map<Long, List<Affectation>> affectations = repository.affectations(livreId, encaissements.stream().map(Encaissement::id).toList()).stream().collect(Collectors.groupingBy(Affectation::paiementId));
        Set<Long> ventesVues = encaissements.stream().map(Encaissement::venteId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, EnTetePiece> ventes = parId(repository.ventes(livreId, ventesVues));
        Set<Long> facturesVues = new LinkedHashSet<>();
        encaissements.forEach(e -> {
            if (e.factureId() != null) {
                facturesVues.add(e.factureId());
            }
            affectations.getOrDefault(e.id(), List.of()).forEach(a -> facturesVues.add(a.factureId()));
        });
        ventes.values().stream().map(EnTetePiece::factureLieeId).filter(java.util.Objects::nonNull).forEach(facturesVues::add);
        Map<Long, EnTetePiece> factures = parId(repository.factures(livreId, facturesVues));
        Map<Long, List<LignePiece>> lignesFactures = repository.lignesFactures(livreId, facturesVues).stream().collect(Collectors.groupingBy(LignePiece::pieceId));
        Set<Long> ventesSeules = ventes.values().stream().filter(v -> v.factureLieeId() == null).map(EnTetePiece::id).collect(Collectors.toSet());
        Map<Long, List<LignePiece>> lignesVentes = repository.lignesVentes(livreId, ventesSeules).stream().collect(Collectors.groupingBy(LignePiece::pieceId));
        Map<Long, String[]> traitements = repository.traitementsProduits(livreId);
        Map<String, Cumul> cumuls = new HashMap<>(repository.cumulsCollectes(livreId, facturesVues));

        for (Encaissement e : encaissements) {
            List<Imputation> imputations = new ArrayList<>();
            BigDecimal reste = e.montant();
            for (Affectation a : affectations.getOrDefault(e.id(), List.of())) {
                EnTetePiece f = factures.get(a.factureId());
                if (f != null) {
                    imputations.add(new Imputation(ventiler(f.id(), f, lignesFactures.getOrDefault(f.id(), List.of()), remiseMode, traitements), a.montant()));
                    reste = reste.subtract(a.montant());
                }
            }
            if (imputations.isEmpty()) {
                Long factureId = e.factureId();
                EnTetePiece vente = e.venteId() == null ? null : ventes.get(e.venteId());
                if (factureId == null && vente != null) {
                    factureId = vente.factureLieeId();
                }
                if (factureId != null && factures.containsKey(factureId)) {
                    EnTetePiece f = factures.get(factureId);
                    imputations.add(new Imputation(ventiler(f.id(), f, lignesFactures.getOrDefault(f.id(), List.of()), remiseMode, traitements), reste));
                    reste = Z;
                } else if (vente != null) {
                    imputations.add(new Imputation(ventiler(null, vente, lignesVentes.getOrDefault(vente.id(), List.of()), remiseMode, traitements), reste));
                    reste = Z;
                }
            }
            for (Imputation i : imputations) {
                // Au régime des débits, seul l'encaissement antérieur à la facture se taxe ici : la facture taxe le reste à sa date.
                if (debit && (i.piece().date() == null || !e.date().isBefore(i.piece().date()))) {
                    continue;
                }
                collecterImputation(livreId, d, e, i, s, deja, cumuls, cumul);
            }
            if (reste.signum() > 0 && (!debit || imputations.isEmpty())) {
                collecterAcompte(livreId, d, e, reste, s, deja, cumul);
            }
        }
        if (debit) {
            collecterFactures(livreId, d, remiseMode, s, deja, traitements, cumul);
        }
    }

    private PieceVentilee ventiler(Long factureId, EnTetePiece piece, List<LignePiece> lignes, String remiseMode, Map<Long, String[]> traitements) {
        List<LigneSource> sourcesLignes = lignes.stream().map(l -> new LigneSource(l.produitId(), l.typeProduit(), null, l.taux(), l.ht(), l.tva(), l.ttc())).toList();
        return new PieceVentilee(factureId, piece.numero(), piece.date(), TvaCalcul.ventilation(sourcesLignes, piece.remise(), remiseMode), lignesDeVente(lignes, traitements), traitementsParTaux(lignes, traitements));
    }

    /** Par taux, le poids de chaque ligne B : une facture mêlant marchandises et services se partage entre 080 et 081. */
    private Map<BigDecimal, Map<String, BigDecimal>> lignesDeVente(List<LignePiece> lignes, Map<Long, String[]> traitements) {
        Map<BigDecimal, Map<String, BigDecimal>> parTaux = new HashMap<>();
        for (LignePiece l : lignes) {
            BigDecimal taux = taux(l.taux());
            if (taux.signum() == 0) {
                continue;
            }
            String[] t = l.produitId() == null ? null : traitements.get(l.produitId());
            String code = t != null && t[1] != null ? t[1] : formulaire.ligneDeVente(l.typeProduit(), taux);
            if (code != null) {
                parTaux.computeIfAbsent(taux, k -> new LinkedHashMap<>()).merge(code, l.ht(), BigDecimal::add);
            }
        }
        return parTaux;
    }

    private static Map<BigDecimal, String> traitementsParTaux(List<LignePiece> lignes, Map<Long, String[]> traitements) {
        Map<BigDecimal, String> parTaux = new HashMap<>();
        for (LignePiece l : lignes) {
            if (taux(l.taux()).signum() == 0 && l.produitId() != null && traitements.containsKey(l.produitId())) {
                parTaux.put(taux(l.taux()), traitements.get(l.produitId())[0]);
            }
        }
        return parTaux;
    }

    private static BigDecimal taux(BigDecimal t) {
        return t == null ? Z : t.setScale(2, RoundingMode.HALF_UP);
    }

    private void collecterImputation(Long livreId, Declaration d, Encaissement e, Imputation i, Surcharges s, Set<String> deja, Map<String, Cumul> cumuls, Periode cumul) {
        PieceVentilee p = i.piece();
        List<Tranche> tranches = TvaCalcul.repartir(i.montant(), p.parts());
        for (int n = 0; n < tranches.size(); n++) {
            Tranche t = tranches.get(n);
            if (t.ttc().signum() == 0) {
                continue;
            }
            String cleOp = cle("PAIEMENT", e.id(), p.factureId(), t.taux());
            if (deja.contains(cleOp)) {
                continue;
            }
            if (p.factureId() != null) {
                String cleCumul = p.factureId() + "|" + FormulaireTva.cleTaux(t.taux());
                Cumul c = cumuls.getOrDefault(cleCumul, new Cumul(Z, Z));
                t = TvaCalcul.solder(t, p.parts().get(n), c.ttc(), c.tva());
                cumuls.put(cleCumul, new Cumul(c.ttc().add(t.ttc()), c.tva().add(t.tva())));
            }
            ecrireCollectee(livreId, d, "PAIEMENT", e.id(), p.factureId(), e.pointDeVenteId(), e.empreinte(), e.date(), e.compteFinancierId(), e.mode(), e.reference(), e.client(), p.numero(), t, p, s, cumul);
        }
    }

    private void collecterAcompte(Long livreId, Declaration d, Encaissement e, BigDecimal montant, Surcharges s, Set<String> deja, Periode cumul) {
        SurchargeCollectee sc = (SurchargeCollectee) s.collectees().get(cleCollectee("PAIEMENT", e.id(), null, null));
        BigDecimal t = sc == null ? Z : sc.taux();
        if (deja.contains(cle("PAIEMENT", e.id(), null, t))) {
            return;
        }
        Tranche tranche = t.signum() == 0 ? new Tranche(Z, montant, Z, montant) : TvaCalcul.depuisTtc(montant, t);
        String ligne = sc != null ? sc.ligne() : null;
        String motif = sc != null ? sc.motif() : null;
        String traitement = sc != null ? sc.traitement() : "TAXABLE";
        repository.ajouterCollectee(livreId, d.id(), new NouvelleCollectee("PAIEMENT", e.id(), null, e.pointDeVenteId(), e.empreinte(), e.date(), e.compteFinancierId(), e.mode(), e.reference(), e.client(), null,
                tranche.taux(), tranche.ttc(), tranche.ht(), tranche.tva(), traitement, ligne, motif));
        cumuler(cumul, tranche, traitement, ligne == null ? Map.of() : Map.of(ligne, tranche.ht()));
    }

    /** Régime des débits : la facture de la période, moins ce que ses encaissements antérieurs ont déjà porté. */
    private void collecterFactures(Long livreId, Declaration d, String remiseMode, Surcharges s, Set<String> deja, Map<Long, String[]> traitements, Periode cumul) {
        List<SourcesRepository.EnTete> enTetes = sources.enTetes(livreId, "FACTURE", d.du(), d.au(), null).stream().filter(f -> !f.annulee()).toList();
        Map<Long, List<LignePiece>> lignes = repository.lignesFactures(livreId, enTetes.stream().map(SourcesRepository.EnTete::id).toList()).stream().collect(Collectors.groupingBy(LignePiece::pieceId));
        Map<String, Cumul> cumuls = repository.cumulsCollectes(livreId, lignes.keySet());
        for (SourcesRepository.EnTete f : enTetes) {
            PieceVentilee p = ventiler(f.id(), new EnTetePiece(f.id(), f.numero(), f.montant(), f.remise(), f.date(), null), lignes.getOrDefault(f.id(), List.of()), remiseMode, traitements);
            for (PartTaux part : p.parts()) {
                Cumul c = cumuls.getOrDefault(f.id() + "|" + FormulaireTva.cleTaux(part.taux()), new Cumul(Z, Z));
                BigDecimal ttc = part.ttc().subtract(c.ttc());
                if (ttc.signum() == 0 || deja.contains(cle("FACTURE", f.id(), f.id(), part.taux()))) {
                    continue;
                }
                BigDecimal tva = part.tva().subtract(c.tva());
                ecrireCollectee(livreId, d, "FACTURE", f.id(), f.id(), f.pointDeVenteId(), f.empreinte(), f.date(), null, null, f.numero(), f.tiersNom(), f.numero(),
                        new Tranche(part.taux(), ttc.subtract(tva), tva, ttc), p, s, cumul);
            }
        }
        for (SourcesRepository.EnTete a : sources.enTetes(livreId, "AVOIR_CLIENT", d.du(), d.au(), null)) {
            if (a.annulee()) {
                continue;
            }
            Map<BigDecimal, BigDecimal[]> parTaux = new TreeMap<>();
            for (SourcesRepository.LigneLue l : sources.lignes(livreId, "AVOIR_CLIENT", a.date(), a.date(), a.id())) {
                BigDecimal[] v = parTaux.computeIfAbsent(taux(l.tauxTva()), k -> new BigDecimal[]{Z, Z});
                v[0] = v[0].add(l.ht());
                v[1] = v[1].add(l.tva());
            }
            parTaux.forEach((t, v) -> {
                if (!deja.contains(cle("AVOIR_CLIENT", a.id(), null, t))) {
                    Tranche inverse = new Tranche(t, v[0].negate(), v[1].negate(), v[0].add(v[1]).negate());
                    repository.ajouterCollectee(livreId, d.id(), new NouvelleCollectee("AVOIR_CLIENT", a.id(), null, a.pointDeVenteId(), a.empreinte(), a.date(), null, null, a.numero(), a.tiersNom(), a.numero(), t, inverse.ttc(), inverse.ht(), inverse.tva(), "TAXABLE",
                            formulaire.ligneDeVente(null, t), null));
                    String ligne = formulaire.ligneDeVente(null, t);
                    cumuler(cumul, inverse, "TAXABLE", ligne == null ? Map.of() : Map.of(ligne, inverse.ht()));
                }
            });
        }
    }

    private void ecrireCollectee(Long livreId, Declaration d, String type, Long id, Long factureId, Long pdv, String empreinte, LocalDate date, Long financier, String mode, String reference, String tiers, String numero, Tranche t, PieceVentilee p, Surcharges s, Periode cumul) {
        SurchargeCollectee sc = (SurchargeCollectee) s.collectees().get(cle(type, id, factureId, t.taux()));
        Map<String, BigDecimal> poids = p.lignes().getOrDefault(t.taux(), Map.of());
        String traitement = sc != null ? sc.traitement() : t.taux().signum() == 0 ? p.traitements().getOrDefault(t.taux(), "TAXABLE") : "TAXABLE";
        String ligne = sc != null ? sc.ligne() : t.taux().signum() == 0 ? formulaire.ligneDeTraitement(traitement) : plusLourde(poids);
        repository.ajouterCollectee(livreId, d.id(), new NouvelleCollectee(type, id, factureId, pdv, empreinte, date, financier, mode, reference, tiers, numero, t.taux(), t.ttc(), t.ht(), t.tva(), traitement, ligne, sc == null ? null : sc.motif()));
        cumuler(cumul, t, traitement, sc != null && ligne != null ? Map.of(ligne, BigDecimal.ONE) : poids);
    }

    private static String plusLourde(Map<String, BigDecimal> poids) {
        return poids.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private void cumuler(Periode cumul, Tranche t, String traitement, Map<String, BigDecimal> poids) {
        BigDecimal ca = cumul.horsTaxable().getOrDefault("010", Z).add(t.ht());
        cumul.horsTaxable().put("010", ca);
        if (!"TAXABLE".equals(traitement)) {
            String code = formulaire.ligneDeTraitement(traitement);
            if (code != null) {
                cumul.horsTaxable().merge(code, t.ht(), BigDecimal::add);
            }
            return;
        }
        if (t.taux().signum() == 0 || poids.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> bases = TvaCalcul.partager(t.ht(), poids);
        Map<String, BigDecimal> taxes = TvaCalcul.partager(t.tva(), poids);
        bases.forEach((code, base) -> cumul.ventes().merge(code, new BaseTaxe(base, taxes.get(code)), (a, b) -> a.plus(b.base(), b.taxe())));
    }

    // --- TVA déductible

    public void deduire(Long livreId, Declaration d, TvaParametresDTO p, Surcharges s) {
        ajouterReglements(livreId, d, p, repository.reglements(livreId, d.du(), d.au()), s);
        ajouterDepenses(livreId, d, p, repository.depensesVentilees(livreId, d.du(), d.au()), s);
        ajouterChargesImport(livreId, d, p, s);
        ajouterLignesReleve(livreId, d, p, s);
    }

    /** Une ligne classée en rubrique porte sa TVA au taux de la rubrique, en prélèvement. L'identité du fournisseur se complète sur l'opération : la banque n'a ni IF ni ICE au commercial. */
    private void ajouterLignesReleve(Long livreId, Declaration d, TvaParametresDTO p, Surcharges s) {
        Set<String> deja = repository.dejaDeclarees(livreId, false);
        for (LigneClasseeDTO l : rapprochement.lignesDePeriode(livreId, d.du(), d.au())) {
            if (!"RUBRIQUE".equals(l.classement()) || l.rubriqueCode() == null || l.debit().signum() == 0) {
                continue;
            }
            Rubrique r = formulaire.rubrique(l.rubriqueCode()).orElse(null);
            if (r == null || r.taux().isEmpty()) {
                continue;
            }
            BigDecimal taux = new BigDecimal(r.taux().keySet().iterator().next());
            if (deja.contains(cle("LIGNE_RELEVE", l.ligneReleveId(), null, taux))) {
                continue;
            }
            Tranche t = TvaCalcul.depuisTtc(l.debit(), taux);
            String numero = l.reference() != null && !l.reference().isBlank() ? l.reference() : "REL-" + l.ligneReleveId();
            String empreinte = org.springframework.util.DigestUtils.md5DigestAsHex((l.date() + "|" + l.debit() + "|" + l.rubriqueCode()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ecrireDeductible(livreId, d, p, "LIGNE_RELEVE", l.ligneReleveId(), null, livreCourant.exiger().pdvIds().get(0), empreinte, numero, l.date(), l.libelle(), null, null, r.libelle(), l.rubriqueCode(), t, 3, numero, l.date(), null, s);
        }
        repository.numeroterDeductibles(livreId, d.id());
    }

    public void ajouterReglements(Long livreId, Declaration d, TvaParametresDTO p, List<Reglement> reglements, Surcharges s) {
        Set<String> deja = repository.dejaDeclarees(livreId, false);
        Map<Long, List<AffectationReglement>> affectations = repository.affectationsReglements(livreId, reglements.stream().map(Reglement::id).toList()).stream().collect(Collectors.groupingBy(AffectationReglement::reglementId));
        Set<Long> factureIds = new LinkedHashSet<>();
        reglements.forEach(r -> {
            if (r.factureAchatId() != null) {
                factureIds.add(r.factureAchatId());
            }
            affectations.getOrDefault(r.id(), List.of()).forEach(a -> factureIds.add(a.factureAchatId()));
        });
        Map<Long, FactureAchat> factures = repository.facturesAchat(livreId, factureIds).stream().collect(Collectors.toMap(FactureAchat::id, Function.identity()));
        Map<Long, List<LignePiece>> lignes = repository.lignesAchats(livreId, factureIds).stream().collect(Collectors.groupingBy(LignePiece::pieceId));
        Map<Long, List<VentilationAchat>> ventilations = repository.ventilationsAchats(livreId, factureIds).stream().collect(Collectors.groupingBy(VentilationAchat::factureAchatId));
        Map<String, Cumul> cumuls = new HashMap<>(repository.cumulsDeduits(livreId, factureIds));
        for (Reglement r : reglements) {
            Map<Long, BigDecimal> imputations = new LinkedHashMap<>();
            affectations.getOrDefault(r.id(), List.of()).forEach(a -> imputations.merge(a.factureAchatId(), a.montant(), BigDecimal::add));
            if (imputations.isEmpty() && r.factureAchatId() != null) {
                imputations.put(r.factureAchatId(), r.montant());
            }
            imputations.forEach((factureId, montant) -> {
                FactureAchat f = factures.get(factureId);
                if (f != null) {
                    deduireFacture(livreId, d, p, r, f, montant, lignes.getOrDefault(factureId, List.of()), ventilations.getOrDefault(factureId, List.of()), deja, cumuls, s);
                }
            });
        }
        repository.numeroterDeductibles(livreId, d.id());
    }

    private void deduireFacture(Long livreId, Declaration d, TvaParametresDTO p, Reglement r, FactureAchat f, BigDecimal montant, List<LignePiece> lignes, List<VentilationAchat> ventilations, Set<String> deja, Map<String, Cumul> cumuls, Surcharges s) {
        List<LigneSource> sourcesLignes = lignes.stream().map(l -> new LigneSource(l.produitId(), l.typeProduit(), null, l.taux(), l.ht(), l.tva(), l.ttc())).toList();
        List<PartTaux> parts = TvaCalcul.ventilation(sourcesLignes, Z, "NET_TVA_FACTUREE");
        List<Tranche> tranches = TvaCalcul.repartir(montant, parts);
        for (int n = 0; n < tranches.size(); n++) {
            Tranche t = tranches.get(n);
            String cleOp = cle("REGLEMENT_FOURNISSEUR", r.id(), f.id(), t.taux());
            if (t.taux().signum() == 0 || t.ttc().signum() == 0 || deja.contains(cleOp)) {
                continue;
            }
            BigDecimal taux = t.taux();
            VentilationAchat v = ventilations.stream().filter(x -> x.taux() != null && x.taux().compareTo(taux) == 0).findFirst().orElse(ventilations.isEmpty() ? null : ventilations.get(0));
            if (v != null && !v.deductible()) {
                continue;
            }
            String cleCumul = f.id() + "|" + FormulaireTva.cleTaux(taux);
            Cumul c = cumuls.getOrDefault(cleCumul, new Cumul(Z, Z));
            t = TvaCalcul.solder(t, parts.get(n), c.ttc(), c.tva());
            cumuls.put(cleCumul, new Cumul(c.ttc().add(t.ttc()), c.tva().add(t.tva())));
            String rubrique = v != null && v.rubrique() != null ? v.rubrique() : v != null && v.immobilisation() ? "IMMOBILISATION_INTERIEUR" : rubriqueDeLignes(lignes, taux);
            String numero = v != null && v.numeroFournisseur() != null ? v.numeroFournisseur() : f.numero();
            LocalDate dateFacture = v != null && v.dateFournisseur() != null ? v.dateFournisseur() : f.date();
            String designation = f.observations() != null && !f.observations().isBlank() ? f.observations() : formulaire.rubrique(rubrique).map(Rubrique::libelle).orElse(rubrique);
            ecrireDeductible(livreId, d, p, "REGLEMENT_FOURNISSEUR", r.id(), f.id(), r.pointDeVenteId(), r.empreinte(), numero, dateFacture, f.fournisseur(), f.fournisseurIf(), f.fournisseurIce(), designation, rubrique, t, TvaCalcul.modePaiementDgi(r.mode()), r.reference(), r.date(), r.compteFinancierId(), s);
        }
    }

    private static String rubriqueDeLignes(List<LignePiece> lignes, BigDecimal taux) {
        BigDecimal services = Z;
        BigDecimal autres = Z;
        for (LignePiece l : lignes) {
            if (taux(l.taux()).compareTo(taux) == 0) {
                if ("SERVICE".equals(l.typeProduit())) {
                    services = services.add(l.ht());
                } else {
                    autres = autres.add(l.ht());
                }
            }
        }
        return services.compareTo(autres) > 0 ? "SERVICES" : "ACHATS_INTERIEUR";
    }

    public void ajouterDepenses(Long livreId, Declaration d, TvaParametresDTO p, List<DepenseVentilee> depenses, Surcharges s) {
        Set<String> deja = repository.dejaDeclarees(livreId, false);
        Map<String, List<DepenseVentilee>> parTaux = depenses.stream().filter(DepenseVentilee::deductible).collect(Collectors.groupingBy(v -> v.id() + "|" + FormulaireTva.cleTaux(v.taux()), LinkedHashMap::new, Collectors.toList()));
        for (List<DepenseVentilee> groupe : parTaux.values()) {
            DepenseVentilee v = groupe.get(0);
            BigDecimal taux = taux(v.taux());
            if (deja.contains(cle("DEPENSE", v.id(), null, taux))) {
                continue;
            }
            BigDecimal ht = groupe.stream().map(DepenseVentilee::ht).reduce(Z, BigDecimal::add);
            BigDecimal tva = groupe.stream().map(DepenseVentilee::tva).reduce(Z, BigDecimal::add);
            String rubrique = v.rubrique() != null ? v.rubrique() : v.immobilisation() ? "IMMOBILISATION_INTERIEUR" : "SERVICES";
            String numero = v.numeroFacture() != null ? v.numeroFacture() : v.justificatif() != null ? v.justificatif() : v.reference();
            LocalDate dateFacture = v.dateFacture() != null ? v.dateFacture() : v.date();
            String designation = v.designation() != null && !v.designation().isBlank() ? v.designation() : formulaire.rubrique(rubrique).map(Rubrique::libelle).orElse(rubrique);
            ecrireDeductible(livreId, d, p, "DEPENSE", v.id(), null, v.pointDeVenteId(), v.empreinte(), numero, dateFacture, v.tiers(), v.tiersIf(), v.tiersIce(), designation, rubrique, new Tranche(taux, ht, tva, ht.add(tva)), TvaCalcul.modePaiementDgi(v.mode()), v.reference(), v.date(), null, s);
        }
        repository.numeroterDeductibles(livreId, d.id());
    }

    private void ajouterChargesImport(Long livreId, Declaration d, TvaParametresDTO p, Surcharges s) {
        Set<String> deja = repository.dejaDeclarees(livreId, false);
        BigDecimal taux = new BigDecimal("20.00");
        for (ChargeImport c : repository.chargesImport(livreId, d.du(), d.au())) {
            if (deja.contains(cle("CHARGE_IMPORT", c.id(), null, taux))) {
                continue;
            }
            BigDecimal ht = c.tva().multiply(new BigDecimal("100")).divide(taux, 2, RoundingMode.HALF_UP);
            String numero = c.quittance() != null ? c.quittance() : "CHARGE-" + c.id();
            ecrireDeductible(livreId, d, p, "CHARGE_IMPORT", c.id(), null, c.pointDeVenteId(), c.empreinte(), numero, c.date(), c.tiers() != null ? c.tiers() : "Douane", c.tiersIf(), c.tiersIce(), "TVA de la quittance de douane", "ACHATS_IMPORT",
                    new Tranche(taux, ht, c.tva(), ht.add(c.tva())), TvaCalcul.modePaiementDgi(c.mode()), c.quittance(), c.date(), null, s);
        }
        repository.numeroterDeductibles(livreId, d.id());
    }

    private void ecrireDeductible(Long livreId, Declaration d, TvaParametresDTO p, String type, Long id, Long factureId, Long pdv, String empreinte, String numero, LocalDate dateFacture, String nom, String fiscal, String ice, String designation, String rubrique, Tranche t, int mp, String reference, LocalDate datePaiement, Long financier, Surcharges s) {
        SurchargeDeductible sd = (SurchargeDeductible) s.deductibles().get(cle(type, id, factureId, t.taux()));
        String rubriqueRetenue = sd != null ? sd.rubrique() : rubrique;
        String compte = sd != null ? sd.compte() : formulaire.rubrique(rubriqueRetenue).map(Rubrique::compteTva).orElse("34552");
        String ligne = sd != null ? sd.ligne() : formulaire.ligneDeDeduction(rubriqueRetenue, t.taux());
        repository.ajouterDeductible(livreId, d.id(), new NouvelleDeductible(type, id, factureId, pdv, empreinte, tronquer(sd != null ? sd.numero() : numero, 100), dateFacture, tronquer(nom == null ? "-" : nom, 255),
                sd != null ? sd.fiscal() : fiscal, sd != null ? sd.ice() : ice, tronquer(sd != null ? sd.designation() : designation, 255), rubriqueRetenue, ligne, compte, t.ht(), t.taux(), t.tva(), t.ttc(), p.prorata(),
                TvaCalcul.partDeductible(t.tva(), p.prorata()), mp, sd != null ? sd.reference() : reference, datePaiement, financier, sd == null ? null : sd.motif()));
    }

    private static String tronquer(String texte, int taille) {
        return texte == null || texte.length() <= taille ? texte : texte.substring(0, taille);
    }

    // --- Régularisations

    /** Une déclaration validée ne se recalcule pas : ce qui a changé depuis se contre-passe sur la période ouverte. Son traitement fiscal reste à valider. */
    public void regulariser(Long livreId, Declaration d, Periode cumul) {
        for (OperationCollectee o : repository.collecteesEcartees(livreId, d.id())) {
            Tranche inverse = new Tranche(o.taux(), o.ht().negate(), o.tva().negate(), o.ttc().negate());
            repository.ajouterCollectee(livreId, d.id(), new NouvelleCollectee("REGULARISATION", o.id(), o.factureId(), o.pointDeVenteId(), o.empreinte(), d.au(), o.compteFinancierId(), o.mode(), o.reference(), o.tiersNom(), o.factureNumero(),
                    o.taux(), inverse.ttc(), inverse.ht(), inverse.tva(), o.traitement(), o.ligneCode(), null));
            cumuler(cumul, inverse, o.traitement(), o.ligneCode() == null ? Map.of() : Map.of(o.ligneCode(), BigDecimal.ONE));
        }
        for (OperationDeductible o : repository.deductiblesEcartees(livreId, d.id())) {
            repository.ajouterDeductible(livreId, d.id(), new NouvelleDeductible("REGULARISATION", o.id(), o.factureAchatId(), o.pointDeVenteId(), o.empreinte(), o.numeroFacture(), o.dateFacture(), o.fournisseurNom(), o.fournisseurIf(), o.fournisseurIce(),
                    o.designation(), o.rubrique(), o.ligneCode(), o.compteTva(), o.ht().negate(), o.taux(), o.tva().negate(), o.ttc().negate(), o.prorata(), o.tvaDeductible().negate(), o.mpId(), o.reference(), d.au(), o.compteFinancierId(), null));
        }
        repository.numeroterDeductibles(livreId, d.id());
    }

    /** La section E se relit toujours depuis les opérations : rattrapages et corrections y sont déjà. */
    public Map<String, BaseTaxe> deductions(List<OperationDeductible> operations) {
        Map<String, BaseTaxe> lignes = new TreeMap<>();
        for (OperationDeductible o : operations) {
            if (o.ligneCode() != null) {
                lignes.merge(o.ligneCode(), new BaseTaxe(o.tva(), o.tvaDeductible()), (a, b) -> a.plus(b.base(), b.taxe()));
            }
        }
        return lignes;
    }

    private static Map<Long, EnTetePiece> parId(List<EnTetePiece> pieces) {
        return pieces.stream().collect(Collectors.toMap(EnTetePiece::id, Function.identity()));
    }
}
