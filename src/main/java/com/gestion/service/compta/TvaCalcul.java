package com.gestion.service.compta;

import com.gestion.service.compta.PieceSource.LigneSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Les calculs de la TVA, sans base : ventilation d'une facture par taux, part d'un paiement, lignes du formulaire. Tout à deux décimales, HALF_UP. */
public final class TvaCalcul {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal CENT = new BigDecimal("100");

    /** @param ttc net de remise, comme le montant final de la pièce */
    public record PartTaux(BigDecimal taux, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    public record Tranche(BigDecimal taux, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    /** Ce qu'une ligne du formulaire porte : la base en section A et B, la TVA des factures en section E ; la taxe partout où elle existe. */
    public record BaseTaxe(BigDecimal base, BigDecimal taxe) {
        public BaseTaxe plus(BigDecimal b, BigDecimal t) {
            return new BaseTaxe(base.add(b), taxe.add(t));
        }
    }

    /** Ce que la période a produit, avant le formulaire. @param ca ligne 010 ; horsTaxable lignes 020 à 050 ; ventes section B ; deductions section E */
    public record Periode(BigDecimal ca, Map<String, BigDecimal> horsTaxable, Map<String, BaseTaxe> ventes, Map<String, BaseTaxe> deductions) {
    }

    /** @param aPayer ligne 200 ; credit ligne 201 ; une période est soit due, soit créditrice */
    public record Resultat(BigDecimal exigible, BigDecimal deductions, BigDecimal creditAnterieur, BigDecimal aPayer, BigDecimal credit) {
    }

    public record LigneCalculee(String code, BigDecimal base, BigDecimal taxe) {
    }

    /** Lignes qui s'affichent même à zéro : ce sont les résultats que l'on recopie. */
    private static final Set<String> SAISIES = Set.of("120", "131", "180", "183", "185", "186", "187");

    private TvaCalcul() {
    }

    /**
     * La facture par taux, net de remise. En mode facturé, la remise ne réduit que le hors taxes et la TVA reste celle imprimée, comme l'écriture passée ;
     * c'est ce qui solde 4455 à la liquidation. En mode réduit, le hors taxes et la TVA baissent ensemble.
     */
    public static List<PartTaux> ventilation(List<LigneSource> lignes, BigDecimal remise, String remiseMode) {
        Map<BigDecimal, BigDecimal> ht = new TreeMap<>();
        Map<BigDecimal, BigDecimal> tva = new TreeMap<>();
        for (LigneSource l : lignes) {
            BigDecimal taux = l.tauxTva() == null ? Z : l.tauxTva().setScale(2, RoundingMode.HALF_UP);
            ht.merge(taux, l.ht(), BigDecimal::add);
            tva.merge(taux, l.tva(), BigDecimal::add);
        }
        Map<BigDecimal, BigDecimal> tvaNette = tva;
        BigDecimal surHt = remise;
        if ("NET_TVA_REDUITE".equals(remiseMode) && remise.signum() > 0) {
            BigDecimal totalHt = somme(ht);
            BigDecimal assiette = totalHt.add(somme(tva));
            surHt = assiette.signum() == 0 ? Z : remise.multiply(totalHt).divide(assiette, 2, RoundingMode.HALF_UP);
            tvaNette = ReglesComptabilisation.repartir(tva, remise.subtract(surHt));
        }
        Map<BigDecimal, BigDecimal> htNet = ReglesComptabilisation.repartir(ht, surHt);
        List<PartTaux> parts = new ArrayList<>();
        for (Map.Entry<BigDecimal, BigDecimal> e : htNet.entrySet()) {
            BigDecimal t = tvaNette.getOrDefault(e.getKey(), Z);
            parts.add(new PartTaux(e.getKey(), e.getValue(), t, e.getValue().add(t)));
        }
        return parts;
    }

    /**
     * Ce qu'un paiement apporte, taux par taux, au prorata de la ventilation. HT = TTC / (1 + t), puis TVA = TTC - HT, ce qui garde HT + TVA = TTC.
     * Le reliquat d'arrondi va sur le plus gros taux, et le total reste juste au centime.
     */
    public static List<Tranche> repartir(BigDecimal paye, List<PartTaux> parts) {
        BigDecimal total = parts.stream().map(PartTaux::ttc).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Tranche> tranches = new ArrayList<>();
        if (total.signum() == 0 || parts.isEmpty()) {
            return tranches;
        }
        BigDecimal reparti = BigDecimal.ZERO;
        int plusGros = 0;
        for (int i = 0; i < parts.size(); i++) {
            PartTaux p = parts.get(i);
            BigDecimal ttc = paye.multiply(p.ttc()).divide(total, 2, RoundingMode.HALF_UP);
            BigDecimal ht = paye.multiply(p.ht()).divide(total, 2, RoundingMode.HALF_UP);
            tranches.add(new Tranche(p.taux(), ht, ttc.subtract(ht), ttc));
            reparti = reparti.add(ttc);
            if (p.ttc().compareTo(parts.get(plusGros).ttc()) > 0) {
                plusGros = i;
            }
        }
        BigDecimal reste = paye.subtract(reparti);
        if (reste.signum() != 0) {
            Tranche t = tranches.get(plusGros);
            tranches.set(plusGros, new Tranche(t.taux(), t.ht().add(reste), t.tva(), t.ttc().add(reste)));
        }
        return tranches;
    }

    /**
     * Le dernier paiement d'une facture porte ce qui reste de sa TVA : deux tranches de 50 sur 16,67 de TVA donnent 8,33 et 8,34, jamais 16,66 au total.
     *
     * @param ttcDeja déjà déclaré sur cette facture et ce taux
     */
    public static Tranche solder(Tranche t, PartTaux part, BigDecimal ttcDeja, BigDecimal tvaDeja) {
        if (t.ttc().signum() <= 0 || ttcDeja.add(t.ttc()).compareTo(part.ttc()) < 0) {
            return t;
        }
        BigDecimal tva = part.tva().subtract(tvaDeja).max(Z);
        return new Tranche(t.taux(), t.ttc().subtract(tva), tva, t.ttc());
    }

    public static Tranche depuisTtc(BigDecimal ttc, BigDecimal taux) {
        BigDecimal ht = ttc.multiply(CENT).divide(CENT.add(taux), 2, RoundingMode.HALF_UP);
        return new Tranche(taux, ht, ttc.subtract(ht), ttc);
    }

    public static BigDecimal partDeductible(BigDecimal tva, BigDecimal prorata) {
        return tva.multiply(prorata).divide(CENT, 2, RoundingMode.HALF_UP);
    }

    /** Partage un montant selon des poids, au centime : le reliquat va sur le plus gros poids. */
    public static Map<String, BigDecimal> partager(BigDecimal montant, Map<String, BigDecimal> poids) {
        Map<String, BigDecimal> parts = new LinkedHashMap<>();
        BigDecimal total = poids.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (poids.isEmpty()) {
            return parts;
        }
        String plusGros = null;
        BigDecimal reparti = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : poids.entrySet()) {
            BigDecimal part = total.signum() == 0 ? Z : montant.multiply(e.getValue()).divide(total, 2, RoundingMode.HALF_UP);
            parts.put(e.getKey(), part);
            reparti = reparti.add(part);
            if (plusGros == null || e.getValue().compareTo(poids.get(plusGros)) > 0) {
                plusGros = e.getKey();
            }
        }
        parts.merge(plusGros, montant.subtract(reparti), BigDecimal::add);
        return parts;
    }

    public static Resultat resultat(BigDecimal exigible, BigDecimal totalDeductible, BigDecimal deductions, BigDecimal creditAnterieur) {
        BigDecimal ecart = exigible.subtract(totalDeductible);
        return new Resultat(exigible, deductions, creditAnterieur, ecart.max(Z), ecart.negate().max(Z));
    }

    /**
     * Les formules du formulaire. Une ligne corrigée à la main l'emporte sur le calcul, et ce qui en dépend suit.
     *
     * @param retenues les lignes corrigées, avec un motif : code et valeurs retenues
     */
    public static List<LigneCalculee> lignes(Periode p, BigDecimal credit, Map<String, BaseTaxe> retenues) {
        List<LigneCalculee> lignes = new ArrayList<>();
        BigDecimal ca = base(retenues, "010", p.ca());
        lignes.add(new LigneCalculee("010", ca, null));
        BigDecimal horsTaxable = Z;
        for (String code : List.of("020", "030", "040", "050")) {
            BigDecimal v = base(retenues, code, p.horsTaxable().getOrDefault(code, Z));
            horsTaxable = horsTaxable.add(v);
            if (v.signum() != 0) {
                lignes.add(new LigneCalculee(code, v, null));
            }
        }
        lignes.add(new LigneCalculee("060", ca.subtract(horsTaxable), null));
        BigDecimal exigible = Z;
        for (Map.Entry<String, BaseTaxe> e : new TreeMap<>(p.ventes()).entrySet()) {
            BaseTaxe v = retenues.getOrDefault(e.getKey(), e.getValue());
            exigible = exigible.add(v.taxe());
            lignes.add(new LigneCalculee(e.getKey(), v.base(), v.taxe()));
        }
        exigible = exigible.add(saisie(retenues, "120", lignes));
        BigDecimal retenueClients = saisie(retenues, "131", lignes);
        lignes.add(new LigneCalculee("130", null, exigible));
        BigDecimal l132 = exigible.subtract(retenueClients);
        lignes.add(new LigneCalculee("132", null, l132));
        BigDecimal deductions = Z;
        for (Map.Entry<String, BaseTaxe> e : new TreeMap<>(p.deductions()).entrySet()) {
            BaseTaxe v = retenues.getOrDefault(e.getKey(), e.getValue());
            deductions = deductions.add(v.taxe());
            lignes.add(new LigneCalculee(e.getKey(), v.base(), v.taxe()));
        }
        lignes.add(new LigneCalculee("182", null, deductions));
        BigDecimal l170 = retenues.containsKey("170") ? retenues.get("170").taxe() : credit;
        lignes.add(new LigneCalculee("170", null, l170));
        BigDecimal ajouts = saisie(retenues, "180", lignes).add(saisie(retenues, "183", lignes));
        BigDecimal annule = saisie(retenues, "185", lignes);
        BigDecimal remboursements = saisie(retenues, "186", lignes).add(saisie(retenues, "187", lignes));
        BigDecimal l190 = deductions.add(l170).add(ajouts).add(annule).subtract(remboursements);
        lignes.add(new LigneCalculee("190", null, l190));
        Resultat r = resultat(l132, l190, deductions, l170);
        lignes.add(new LigneCalculee("200", null, r.aPayer()));
        lignes.add(new LigneCalculee("201", null, r.credit()));
        lignes.add(new LigneCalculee("205", null, l132.subtract(deductions.add(ajouts)).abs()));
        return lignes;
    }

    private static BigDecimal base(Map<String, BaseTaxe> retenues, String code, BigDecimal calcule) {
        return retenues.containsKey(code) && retenues.get(code).base() != null ? retenues.get(code).base() : calcule;
    }

    private static BigDecimal saisie(Map<String, BaseTaxe> retenues, String code, List<LigneCalculee> lignes) {
        if (!SAISIES.contains(code) || !retenues.containsKey(code) || retenues.get(code).taxe() == null) {
            return Z;
        }
        BigDecimal v = retenues.get(code).taxe();
        lignes.add(new LigneCalculee(code, null, v));
        return v;
    }

    /** Fin du mois suivant en mensuel, fin du premier mois du trimestre suivant en trimestriel : art. 110 et 111. */
    public static LocalDate dateLimite(LocalDate finDePeriode) {
        LocalDate suivant = finDePeriode.plusMonths(1);
        return suivant.withDayOfMonth(suivant.lengthOfMonth());
    }

    public static LocalDate debutDePeriode(int annee, String periodicite, int numero) {
        return "TRIMESTRIELLE".equals(periodicite) ? LocalDate.of(annee, (numero - 1) * 3 + 1, 1) : LocalDate.of(annee, numero, 1);
    }

    public static LocalDate finDePeriode(int annee, String periodicite, int numero) {
        LocalDate debut = debutDePeriode(annee, periodicite, numero);
        return debut.plusMonths("TRIMESTRIELLE".equals(periodicite) ? 3 : 1).minusDays(1);
    }

    public static int nombreDePeriodes(String periodicite) {
        return "TRIMESTRIELLE".equals(periodicite) ? 4 : 12;
    }

    public static int regimeDgi(String periodicite) {
        return "TRIMESTRIELLE".equals(periodicite) ? 2 : 1;
    }

    /** Le code du relevé. Une carte ou un avis de débit reste à vérifier : ils partent sur 7, autres. Un crédit n'est pas un paiement et n'arrive jamais ici. */
    public static int modePaiementDgi(String mode) {
        return switch (mode == null ? "" : mode) {
            case "ESPECES" -> 1;
            case "CHEQUE" -> 2;
            case "VIREMENT" -> 4;
            case "EFFET", "LCN", "TRAITE" -> 5;
            default -> 7;
        };
    }

    private static BigDecimal somme(Map<BigDecimal, BigDecimal> valeurs) {
        return valeurs.values().stream().reduce(Z, BigDecimal::add);
    }
}
