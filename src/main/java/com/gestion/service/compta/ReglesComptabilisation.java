package com.gestion.service.compta;

import com.gestion.service.compta.ContexteRegles.LigneVentilation;
import com.gestion.service.compta.PieceSource.LigneSource;
import com.gestion.service.compta.PieceSource.TiersSource;
import com.gestion.service.compta.PropositionEcriture.AnomaliePiece;
import com.gestion.service.compta.PropositionEcriture.LigneProposee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Les règles R01 à R12. Fonctions pures, sur des données déjà lues : ce qui s'affiche est ce qui se poste, et tout se vérifie sans base.
 * Aucun compte n'est inventé : sans correspondance, la pièce passe en anomalie COMPTE_A_CHOISIR.
 */
public final class ReglesComptabilisation {

    public static final String COMPTABILISATION = "COMPTABILISATION";
    public static final String REMISE = "REMISE";
    public static final String ENCAISSEMENT = "ENCAISSEMENT";
    public static final String REJET = "REJET";
    public static final String ECHEANCE = "ECHEANCE";
    public static final String IMPUTATION_ACOMPTE = "IMPUTATION_ACOMPTE";
    public static final String DEPART = "DEPART";
    public static final String ARRIVEE = "ARRIVEE";
    public static final TiersSource CLIENTS_DIVERS = new TiersSource("CLIENT", null, "CLIENTS DIVERS", null);

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final Set<String> EFFETS_A_PAYER = Set.of("TRAITE", "LCN", "EFFET");

    private ReglesComptabilisation() {
    }

    /** Ce qu'une pièce demande, compte tenu de son état commercial. Une liste vide : rien à passer, comme un paiement à crédit ou un ajustement de solde. */
    public static List<String> evenementsDus(PieceSource p, LocalDate aujourdhui) {
        switch (p.type()) {
            case "PAIEMENT":
                return "CREDIT".equals(p.attr("mode")) ? List.of() : List.of(COMPTABILISATION);
            case "EFFET":
                return evenementsEffet(p);
            case "REGLEMENT_FOURNISSEUR":
                return evenementsReglement(p, aujourdhui);
            case "MOUVEMENT_TRESORERIE":
                String type = p.attr("type_mouvement");
                if ("AJUSTEMENT_SOLDE".equals(type)) {
                    return List.of();
                }
                return "DEPOT_BANQUE".equals(type) || "TRANSFERT_INTERNE".equals(type) ? List.of(DEPART, ARRIVEE) : List.of(COMPTABILISATION);
            default:
                return List.of(COMPTABILISATION);
        }
    }

    private static List<String> evenementsEffet(PieceSource p) {
        List<String> dus = new ArrayList<>();
        boolean client = "ENCAISSEMENT_CLIENT".equals(p.attr("sens"));
        if (client) {
            if (p.attr("date_remise") != null) {
                dus.add(REMISE);
            }
            if ("ENCAISSE_COMPTABILISE".equals(p.statut())) {
                dus.add(ENCAISSEMENT);
            }
        } else if ("ENCAISSE_COMPTABILISE".equals(p.statut()) && "TRAITE".equals(p.attr("type_effet"))) {
            // Un chèque fournisseur encaissé n'écrit rien : la banque a été créditée au règlement
            dus.add(ECHEANCE);
        }
        if ("IMPAYE_REJETE".equals(p.statut())) {
            dus.add(REJET);
        }
        return dus;
    }

    private static List<String> evenementsReglement(PieceSource p, LocalDate aujourdhui) {
        String mode = p.attr("mode");
        if ("CREDIT".equals(mode)) {
            return List.of();
        }
        List<String> dus = new ArrayList<>(List.of(COMPTABILISATION));
        if ("ACOMPTE_IMPUTE".equals(p.attr("type_reglement"))) {
            dus.add(IMPUTATION_ACOMPTE);
        }
        // Une traite a sa ligne d'effet ; LCN et effet n'en ont pas : proposés à l'échéance, confirmés par l'utilisateur
        LocalDate echeance = p.dateAttr("date_echeance");
        if (("LCN".equals(mode) || "EFFET".equals(mode)) && echeance != null && !echeance.isAfter(aujourdhui)) {
            dus.add(ECHEANCE);
        }
        return dus;
    }

    /** La pièce ne demande aucune écriture, sans que ce soit une annulation. */
    public static boolean sansObjet(PieceSource p) {
        return switch (p.type()) {
            case "VENTE" -> p.attr("facture_id") != null || !"VALIDEE".equals(p.statut());
            case "DEPENSE" -> p.attr("charge_facture_achat_id") != null;
            case "LIGNE_RELEVE" -> false;
            default -> false;
        };
    }

    public static PropositionEcriture proposer(PieceSource p, String evenement, ContexteRegles c) {
        return switch (p.type()) {
            case "FACTURE" -> facture(p, c);
            case "AVOIR_CLIENT" -> avoirClient(p, c);
            case "AVOIR_FOURNISSEUR" -> avoirFournisseur(p, c);
            case "FACTURE_ACHAT" -> factureAchat(p, c);
            case "PAIEMENT" -> paiement(p, c);
            case "EFFET" -> effet(p, evenement, c);
            case "REGLEMENT_FOURNISSEUR" -> reglement(p, evenement, c);
            case "DEPENSE" -> depense(p, c);
            case "VENTE" -> vente(p, c);
            case "MOUVEMENT_TRESORERIE" -> mouvement(p, evenement, c);
            case "LIGNE_RELEVE" -> ligneReleve(p, c);
            default -> throw new IllegalArgumentException("Type de pièce inconnu : " + p.type());
        };
    }

    /** Plusieurs ventes d'un même jour, d'un même point de vente et d'un même client tiennent en une écriture : COMPTA-05. */
    public static PropositionEcriture regrouperVentes(List<PropositionEcriture> propositions, String refPiece) {
        PropositionEcriture premiere = propositions.get(0);
        Map<String, LigneProposee> parCle = new LinkedHashMap<>();
        List<AnomaliePiece> anomalies = new ArrayList<>();
        for (PropositionEcriture p : propositions) {
            anomalies.addAll(p.anomalies());
            for (LigneProposee l : p.lignes()) {
                String cle = l.compte() + "|" + (l.tiers() == null ? "" : l.tiers().type() + l.tiers().id()) + "|" + l.taux() + "|" + (l.debit().signum() > 0 ? "D" : "C");
                parCle.merge(cle, l, (a, b) -> new LigneProposee(a.compte(), a.tiers(), a.debit().add(b.debit()), a.credit().add(b.credit()),
                        a.taux(), somme(a.base(), b.base()), a.natureMouvement(), a.horsFlux()));
            }
        }
        return new PropositionEcriture(premiere.evenement(), premiere.journal(), premiere.datePiece(), refPiece, "Ventes de caisse du " + premiere.datePiece(),
                List.copyOf(parCle.values()), anomalies, List.of());
    }

    // --- R01

    private static PropositionEcriture facture(PieceSource p, ContexteRegles c) {
        Constructeur b = new Constructeur(COMPTABILISATION, "VT", p.date(), p.numero(), "Facture " + p.numero());
        controlerEnTete(p, b);
        if (p.remise().compareTo(p.ht()) > 0) {
            b.anomalie("REMISE_SUPERIEURE_AU_HT", "La remise de " + MontantTexte.fr(p.remise()) + " dépasse le hors taxes de la facture.");
        }
        if (p.annulee() && !"ANNULEE".equals(p.statut())) {
            b.anomalie("FACTURE_ANNULEE_REVALIDEE", "Facture annulée puis revalidée : rien n'est posté en silence, le commercial doit trancher.");
        }
        b.debit(compteTiers("3421"), p.tiers(), p.montant());
        creditsDeVente(p, c, b);
        return b.fin();
    }

    /** HT par compte, remise répartie au prorata, TVA par taux. En mode facturé la TVA reste celle imprimée ; en mode réduit, elle baisse aussi. */
    private static void creditsDeVente(PieceSource p, ContexteRegles c, Constructeur b) {
        Map<String, BigDecimal> htParCompte = new TreeMap<>();
        Map<BigDecimal, BigDecimal[]> tvaParTaux = new TreeMap<>();
        for (LigneSource l : p.lignes()) {
            String compte = compteDeVente(l, p.tiers(), c);
            if (compte == null) {
                b.compteAChoisir("Aucun compte de vente pour le produit " + l.produitId() + ".");
            } else {
                htParCompte.merge(compte, l.ht(), BigDecimal::add);
            }
            tvaParTaux.computeIfAbsent(l.tauxTva(), k -> new BigDecimal[]{Z, Z});
            BigDecimal[] t = tvaParTaux.get(l.tauxTva());
            t[0] = t[0].add(l.ht());
            t[1] = t[1].add(l.tva());
        }
        boolean tvaReduite = "NET_TVA_REDUITE".equals(c.remiseMode()) && p.remise().signum() > 0;
        BigDecimal aRepartir = p.remise();
        if (tvaReduite) {
            BigDecimal assiette = p.ht().add(p.tva());
            aRepartir = assiette.signum() == 0 ? Z : p.remise().multiply(p.ht()).divide(assiette, 2, RoundingMode.HALF_UP);
        }
        Map<String, BigDecimal> nets = repartir(htParCompte, aRepartir);
        nets.forEach((compte, montant) -> b.credit(compte, null, montant));
        BigDecimal resteTva = p.remise().subtract(aRepartir);
        Map<BigDecimal, BigDecimal> tvas = new TreeMap<>();
        tvaParTaux.forEach((taux, t) -> tvas.put(taux, t[1]));
        Map<BigDecimal, BigDecimal> tvasNettes = tvaReduite ? repartir(tvas, resteTva) : tvas;
        tvasNettes.forEach((taux, tva) -> {
            if (tva.signum() != 0) {
                b.tva("4455", false, tva, taux, tvaParTaux.get(taux)[0]);
            }
        });
    }

    /** Répartit au prorata. Le reliquat d'arrondi va sur le plus gros, et le total reste juste au centime. */
    static <K extends Comparable<K>> Map<K, BigDecimal> repartir(Map<K, BigDecimal> montants, BigDecimal aRetirer) {
        Map<K, BigDecimal> resultat = new TreeMap<>(montants);
        if (aRetirer.signum() == 0 || montants.isEmpty()) {
            return resultat;
        }
        BigDecimal total = montants.values().stream().reduce(Z, BigDecimal::add);
        if (total.signum() == 0) {
            return resultat;
        }
        BigDecimal reparti = Z;
        K plusGros = null;
        for (Map.Entry<K, BigDecimal> e : montants.entrySet()) {
            BigDecimal part = aRetirer.multiply(e.getValue()).divide(total, 2, RoundingMode.HALF_UP);
            resultat.put(e.getKey(), e.getValue().subtract(part));
            reparti = reparti.add(part);
            if (plusGros == null || e.getValue().compareTo(montants.get(plusGros)) > 0) {
                plusGros = e.getKey();
            }
        }
        resultat.merge(plusGros, reparti.subtract(aRetirer), BigDecimal::add);
        return resultat;
    }

    private static String compteDeVente(LigneSource l, TiersSource tiers, ContexteRegles c) {
        return compteParPrecision(l, c, "VENTE", typeDeVente(l, tiers));
    }

    /** Du plus précis au plus général : produit, catégorie, type de produit. */
    private static String compteParPrecision(LigneSource l, ContexteRegles c, String famille, String typeProduit) {
        if (l.produitId() != null) {
            var produit = c.compte(famille + "_PRODUIT", String.valueOf(l.produitId()));
            if (produit.isPresent()) {
                return produit.get();
            }
        }
        if (l.categorieId() != null) {
            var categorie = c.compte(famille + "_CATEGORIE", String.valueOf(l.categorieId()));
            if (categorie.isPresent()) {
                return categorie.get();
            }
        }
        return c.compte(famille + "_TYPE_PRODUIT", typeProduit).orElse(null);
    }

    private static String typeDeVente(LigneSource l, TiersSource tiers) {
        String type = l.typeProduit() == null ? "MARCHANDISE" : l.typeProduit();
        boolean etranger = tiers != null && tiers.pays() != null && !tiers.pays().isBlank() && !tiers.pays().trim().equalsIgnoreCase("Maroc");
        return etranger ? type + "_ETRANGER" : type;
    }

    private static void controlerEnTete(PieceSource p, Constructeur b) {
        BigDecimal ht = Z;
        BigDecimal tva = Z;
        for (LigneSource l : p.lignes()) {
            ht = ht.add(l.ht());
            tva = tva.add(l.tva());
        }
        if (ht.subtract(p.ht()).abs().compareTo(TOLERANCE) > 0 || tva.subtract(p.tva()).abs().compareTo(TOLERANCE) > 0) {
            b.anomalie("LIGNES_DIFFERENTES_DE_L_EN_TETE", "Les lignes font " + MontantTexte.fr(ht) + " HT et " + MontantTexte.fr(tva) + " de TVA, l'en-tête " + MontantTexte.fr(p.ht()) + " et " + MontantTexte.fr(p.tva()) + ".");
        }
        if (p.ht().add(p.tva()).subtract(p.ttc()).abs().compareTo(TOLERANCE) > 0) {
            b.anomalie("HT_PLUS_TVA_DIFFERENT_DU_TTC", "HT et TVA font " + MontantTexte.fr(p.ht().add(p.tva())) + ", le TTC est de " + MontantTexte.fr(p.ttc()) + ".");
        }
    }

    // --- R02, R03

    private static PropositionEcriture avoirClient(PieceSource p, ContexteRegles c) {
        Constructeur b = new Constructeur(COMPTABILISATION, "VT", p.date(), p.numero(), "Avoir " + p.numero());
        controlerEnTete(p, b);
        boolean commercial = "AVOIR_COMMERCIAL".equals(p.attr("nature_avoir"));
        if ("ERREUR_FACTURATION".equals(p.attr("nature_avoir"))) {
            b.alerte("ERREUR_FACTURATION", "Avoir pour erreur de facturation : à relire.");
        }
        Map<String, BigDecimal> htParCompte = new TreeMap<>();
        for (LigneSource l : p.lignes()) {
            String type = l.typeProduit() == null ? "MARCHANDISE" : l.typeProduit();
            String compte = commercial ? c.compte("RRR_VENTE", type).orElse(null) : compteDeVente(l, p.tiers(), c);
            if (compte == null) {
                b.compteAChoisir("Aucun compte pour le produit " + l.produitId() + ".");
            } else {
                htParCompte.merge(compte, l.ht(), BigDecimal::add);
            }
        }
        htParCompte.forEach((compte, montant) -> b.debit(compte, null, montant));
        tvaParTaux(p).forEach((taux, t) -> b.tva("4455", true, t[1], taux, t[0]));
        b.credit(compteTiers("3421"), p.tiers(), p.ttc());
        return b.fin();
    }

    private static PropositionEcriture avoirFournisseur(PieceSource p, ContexteRegles c) {
        Constructeur b = new Constructeur(COMPTABILISATION, "AC", p.date(), p.numero(), "Avoir " + p.numero());
        controlerEnTete(p, b);
        boolean commercial = "AVOIR_COMMERCIAL".equals(p.attr("nature_avoir"));
        b.debit(compteTiers("4411"), p.tiers(), p.ttc());
        Map<String, BigDecimal> htParCompte = new TreeMap<>();
        for (LigneSource l : p.lignes()) {
            String type = l.typeProduit() == null ? "MARCHANDISE" : l.typeProduit();
            String compte = commercial ? c.compte("RRR_ACHAT", type).orElse(null) : compteParPrecision(l, c, "ACHAT", type);
            if (compte == null) {
                b.compteAChoisir("Aucun compte pour le produit " + l.produitId() + ".");
            } else {
                htParCompte.merge(compte, l.ht(), BigDecimal::add);
            }
        }
        htParCompte.forEach((compte, montant) -> b.credit(compte, null, montant));
        tvaParTaux(p).forEach((taux, t) -> b.tva("34552", false, t[1], taux, t[0]));
        return b.fin();
    }

    private static Map<BigDecimal, BigDecimal[]> tvaParTaux(PieceSource p) {
        Map<BigDecimal, BigDecimal[]> parTaux = new TreeMap<>();
        for (LigneSource l : p.lignes()) {
            if (l.tva().signum() != 0) {
                BigDecimal[] t = parTaux.computeIfAbsent(l.tauxTva(), k -> new BigDecimal[]{Z, Z});
                t[0] = t[0].add(l.ht());
                t[1] = t[1].add(l.tva());
            }
        }
        return parTaux;
    }

    // --- R04

    private static PropositionEcriture factureAchat(PieceSource p, ContexteRegles c) {
        String refFournisseur = c.ventilation().stream().map(LigneVentilation::numeroFactureFournisseur).filter(n -> n != null && !n.isBlank()).findFirst().orElse(null);
        Constructeur b = new Constructeur(COMPTABILISATION, "AC", p.date(), refFournisseur != null ? refFournisseur : p.numero(), "Facture d'achat " + p.numero());
        if (refFournisseur == null) {
            b.alerte("REF_FOURNISSEUR_ABSENTE", "Pas de numéro de facture du fournisseur : la pièce garde le numéro interne.");
        }
        if (c.ventilation().isEmpty()) {
            controlerEnTete(p, b);
            Map<String, BigDecimal> htParCompte = new TreeMap<>();
            for (LigneSource l : p.lignes()) {
                String compte = compteParPrecision(l, c, "ACHAT", l.typeProduit() == null ? "MARCHANDISE" : l.typeProduit());
                if (compte == null) {
                    b.compteAChoisir("Aucun compte d'achat pour le produit " + l.produitId() + ".");
                } else {
                    htParCompte.merge(compte, l.ht(), BigDecimal::add);
                }
            }
            htParCompte.forEach((compte, montant) -> b.debit(compte, null, montant));
            tvaParTaux(p).forEach((taux, t) -> b.tva("34552", true, t[1], taux, t[0]));
        } else {
            debitsVentiles(c.ventilation(), b);
        }
        b.credit(compteTiers("4411"), p.tiers(), p.ttc());
        return b.fin();
    }

    /** Une TVA non déductible reste en charge. Une ligne immobilisée va en classe 2, avec sa TVA sur 34551. */
    private static void debitsVentiles(List<LigneVentilation> ventilation, Constructeur b) {
        Map<BigDecimal, BigDecimal[]> tva34552 = new TreeMap<>();
        Map<BigDecimal, BigDecimal[]> tva34551 = new TreeMap<>();
        for (LigneVentilation v : ventilation) {
            BigDecimal enCharge = v.deductible() ? v.ht() : v.ht().add(v.tva());
            if (v.immobilisation()) {
                b.ligne(new LigneProposee(v.compte(), null, enCharge, Z, null, null, "ACQUISITION", false));
            } else {
                b.debit(v.compte(), null, enCharge);
            }
            if (v.deductible() && v.tva().signum() != 0) {
                BigDecimal[] t = (v.immobilisation() ? tva34551 : tva34552).computeIfAbsent(v.tauxTva(), k -> new BigDecimal[]{Z, Z});
                t[0] = t[0].add(v.ht());
                t[1] = t[1].add(v.tva());
            }
        }
        tva34552.forEach((taux, t) -> b.tva("34552", true, t[1], taux, t[0]));
        tva34551.forEach((taux, t) -> b.tva("34551", true, t[1], taux, t[0]));
    }

    // --- R05

    private static PropositionEcriture paiement(PieceSource p, ContexteRegles c) {
        String mode = p.attr("mode");
        boolean especes = "ESPECES".equals(mode);
        Constructeur b = new Constructeur(COMPTABILISATION, especes ? "CA" : "BQ", p.date(), p.numero(), "Encaissement " + p.numero());
        TiersSource tiers = p.tiers();
        if (tiers == null) {
            if (p.attr("vente_id") == null) {
                b.anomalie("SENS_INCONNU", "Paiement sans client ni vente : on ne sait pas de quel tiers il vient.");
            } else {
                tiers = CLIENTS_DIVERS;
            }
        }
        String debit;
        if (especes) {
            debit = compteFinancier(c, "CAISSE_DEFAUT", b);
        } else if ("CHEQUE".equals(mode)) {
            debit = "51111";
        } else if (EFFETS_A_PAYER.contains(mode)) {
            debit = compteTiers("3425");
        } else {
            debit = compteFinancier(c, "BANQUE_DEFAUT", b);
        }
        b.debit(debit, "3425".equals(debit) ? tiers : null, p.montant());
        b.credit(compteTiers("3421"), tiers, p.montant());
        return b.fin();
    }

    // --- R06, et R07 pour les effets fournisseurs

    private static PropositionEcriture effet(PieceSource p, String evenement, ContexteRegles c) {
        LocalDate date = dateEffet(p, evenement, c);
        Constructeur b = new Constructeur(evenement, "BQ", date != null ? date : p.date(), p.numero(), libelleEffet(evenement) + " " + p.numero());
        if ("DECAISSEMENT_FOURNISSEUR".equals(p.attr("sens"))) {
            effetFournisseur(p, evenement, c, b);
            return b.fin();
        }
        if (p.attr("paiement_id") == null) {
            b.anomalie("EFFET_SANS_PAIEMENT", "Aucun paiement ne correspond à la référence de cet effet.");
        } else if (!c.prealablePasse()) {
            b.anomalie("PIECE_PREALABLE", "Le paiement " + p.attr("numero_paiement") + " doit être comptabilisé d'abord.");
        }
        boolean cheque = "CHEQUE".equals(p.attr("type_effet"));
        boolean remisPasse = c.evenementsPasses().contains(REMISE);
        boolean encaissePasse = c.evenementsPasses().contains(ENCAISSEMENT);
        String portefeuille = cheque ? "51111" : compteTiers("3425");
        String remis = cheque ? "51112" : "51132";
        TiersSource tiersPortefeuille = cheque ? null : p.tiers();
        switch (evenement) {
            case REMISE -> {
                b.debit(remis, null, p.montant());
                b.credit(portefeuille, tiersPortefeuille, p.montant());
            }
            case ENCAISSEMENT -> {
                b.debit(compteFinancier(c, "BANQUE_DEFAUT", b), null, p.montant());
                if (remisPasse) {
                    b.credit(remis, null, p.montant());
                } else {
                    b.credit(portefeuille, tiersPortefeuille, p.montant());
                }
            }
            case REJET -> {
                if (date == null) {
                    b.anomalie("DATE_A_SAISIR", "Le commercial ne garde pas la date du rejet : saisissez-la.");
                    b.requis("DATE_EFFET");
                }
                b.debit(compteTiers("3421"), p.tiers(), p.montant());
                if (encaissePasse) {
                    b.credit(compteFinancier(c, "BANQUE_DEFAUT", b), null, p.montant());
                } else if (remisPasse) {
                    b.credit(remis, null, p.montant());
                } else {
                    b.credit(portefeuille, tiersPortefeuille, p.montant());
                }
            }
            default -> b.anomalie("EVENEMENT_INCONNU", "Événement " + evenement + " inattendu pour un effet client.");
        }
        return b.fin();
    }

    private static void effetFournisseur(PieceSource p, String evenement, ContexteRegles c, Constructeur b) {
        boolean cheque = "CHEQUE".equals(p.attr("type_effet"));
        if (ECHEANCE.equals(evenement)) {
            b.debit(compteTiers("4415"), p.tiers(), p.montant());
            b.credit(compteFinancier(c, "BANQUE_DEFAUT", b), null, p.montant());
        } else if (REJET.equals(evenement)) {
            if (c.complement().dateEffet() == null) {
                b.anomalie("DATE_A_SAISIR", "Le commercial ne garde pas la date du rejet : saisissez-la.");
                b.requis("DATE_EFFET");
            }
            if (cheque) {
                b.debit(compteFinancier(c, "BANQUE_DEFAUT", b), null, p.montant());
            } else {
                b.debit(compteTiers("4415"), p.tiers(), p.montant());
            }
            b.credit(compteTiers("4411"), p.tiers(), p.montant());
        } else {
            b.anomalie("EVENEMENT_INCONNU", "Événement " + evenement + " inattendu pour un effet fournisseur.");
        }
    }

    private static LocalDate dateEffet(PieceSource p, String evenement, ContexteRegles c) {
        return switch (evenement) {
            case REMISE -> p.dateAttr("date_remise");
            case ENCAISSEMENT, ECHEANCE -> p.dateAttr("date_encaissement");
            case REJET -> c.complement().dateEffet();
            default -> p.date();
        };
    }

    private static String libelleEffet(String evenement) {
        return switch (evenement) {
            case REMISE -> "Remise";
            case ENCAISSEMENT -> "Encaissement";
            case REJET -> "Rejet";
            case ECHEANCE -> "Échéance";
            default -> "Effet";
        };
    }

    // --- R07

    private static PropositionEcriture reglement(PieceSource p, String evenement, ContexteRegles c) {
        String mode = p.attr("mode");
        boolean especes = "ESPECES".equals(mode);
        if (IMPUTATION_ACOMPTE.equals(evenement)) {
            Constructeur b = new Constructeur(evenement, "OD", p.dateAttr("date_facture") != null ? p.dateAttr("date_facture") : p.date(), p.numero(), "Imputation de l'acompte " + p.numero());
            b.debit(compteTiers("4411"), p.tiers(), p.montant());
            b.credit(compteTiers("3411"), p.tiers(), p.montant());
            return b.fin();
        }
        if (ECHEANCE.equals(evenement)) {
            Constructeur b = new Constructeur(evenement, "BQ", p.dateAttr("date_echeance") != null ? p.dateAttr("date_echeance") : p.date(), p.numero(), "Échéance " + p.numero());
            b.debit(compteTiers("4415"), p.tiers(), p.montant());
            b.credit(compteFinancier(c, "BANQUE_DEFAUT", b), null, p.montant());
            return b.fin();
        }
        Constructeur b = new Constructeur(COMPTABILISATION, especes ? "CA" : "BQ", p.date(), p.numero(), "Règlement " + p.numero());
        if (p.tiers() == null) {
            b.anomalie("TIERS_INTROUVABLE", "Ni facture ni commande ne donnent le fournisseur de ce règlement.");
        }
        boolean acompte = "ACOMPTE_COMMANDE".equals(p.attr("type_reglement")) || "ACOMPTE_IMPUTE".equals(p.attr("type_reglement"));
        b.debit(compteTiers(acompte ? "3411" : "4411"), p.tiers(), p.montant());
        if (EFFETS_A_PAYER.contains(mode)) {
            b.credit(compteTiers("4415"), p.tiers(), p.montant());
        } else if (especes) {
            b.credit(compteFinancier(c, "CAISSE_DEFAUT", b), null, p.montant());
        } else {
            b.credit(compteFinancier(c, "BANQUE_DEFAUT", b), null, p.montant());
        }
        return b.fin();
    }

    // --- R08

    private static PropositionEcriture depense(PieceSource p, ContexteRegles c) {
        boolean especes = "ESPECES".equals(p.attr("mode"));
        boolean credit = "CREDIT".equals(p.attr("mode"));
        Constructeur b = new Constructeur(COMPTABILISATION, credit ? "AC" : especes ? "CA" : "BQ", p.date(),
                p.attr("justificatif") != null ? p.attr("justificatif") : p.numero(), p.attr("designation") != null ? p.attr("designation") : "Dépense " + p.numero());
        if ("ESTIMEE".equals(p.attr("charge_statut"))) {
            b.anomalie("CHARGE_ESTIMEE", "La charge liée est encore estimée : en attente du montant réel.");
        }
        BigDecimal montant = p.montant();
        if (!c.ventilation().isEmpty()) {
            debitsVentiles(c.ventilation(), b);
            BigDecimal ventile = c.ventilation().stream().map(v -> v.ht().add(v.tva())).reduce(Z, BigDecimal::add);
            if (ventile.subtract(montant).abs().compareTo(TOLERANCE) > 0) {
                b.anomalie("VENTILATION_DIFFERENTE", "La ventilation fait " + MontantTexte.fr(ventile) + ", la dépense " + MontantTexte.fr(montant) + ".");
            }
        } else if ("TVA_IMPORT".equals(p.attr("type_charge"))) {
            b.debit("34552", null, montant);
        } else {
            String compte = p.numero() != null && p.numero().startsWith("DEP-SWIFT-") ? "61473" : compteDeCharge(p, c);
            if (compte == null) {
                b.compteAChoisir("Catégorie " + p.attr("categorie") + " : aucun compte fixé. Ventilez la dépense ou fixez une correspondance.");
                b.requis("VENTILATION");
            } else {
                b.debit(compte, null, montant);
            }
            b.alerte("TVA_NON_VENTILEE", "TVA non ventilée : tout passe en charge, le montant ne se découpe jamais à 20/120.");
        }
        if (credit) {
            if (p.tiers() == null) {
                b.anomalie("TIERS_REQUIS", "Dépense à crédit sans fournisseur ni prestataire.");
            }
            b.credit(compteTiers("4411"), p.tiers(), montant);
        } else {
            b.credit(compteFinancier(c, especes ? "CAISSE_DEFAUT" : "BANQUE_DEFAUT", b), null, montant);
        }
        return b.fin();
    }

    private static String compteDeCharge(PieceSource p, ContexteRegles c) {
        if (p.attr("type_charge") != null) {
            var parCharge = c.compte("TYPE_CHARGE", p.attr("type_charge"));
            if (parCharge.isPresent()) {
                return parCharge.get();
            }
        }
        return c.compte("CATEGORIE_DEPENSE", p.attr("categorie")).orElse(null);
    }

    // --- R09

    private static PropositionEcriture vente(PieceSource p, ContexteRegles c) {
        Constructeur b = new Constructeur(COMPTABILISATION, "VT", p.date(), p.numero(), "Vente " + p.numero());
        controlerEnTete(p, b);
        TiersSource tiers = p.tiers() != null ? p.tiers() : CLIENTS_DIVERS;
        b.debit(compteTiers("3421"), tiers, p.montant());
        creditsDeVente(p, c, b);
        return b.fin();
    }

    // --- R10

    private static PropositionEcriture mouvement(PieceSource p, String evenement, ContexteRegles c) {
        Constructeur b = new Constructeur(evenement, "OD", p.date(), p.numero(), "Mouvement " + p.numero());
        String source = compteDuCompteFinancier(p.attr("compte_source_id"), c);
        String destination = compteDuCompteFinancier(p.attr("compte_destination_id"), c);
        String type = p.attr("type_mouvement");
        switch (type == null ? "" : type) {
            case "DEPOT_BANQUE", "TRANSFERT_INTERNE" -> {
                if (DEPART.equals(evenement)) {
                    b.debit("5115", null, p.montant());
                    exigerCompte(source, b, "source");
                    b.credit(source == null ? "5141" : source, null, p.montant());
                } else {
                    exigerCompte(destination, b, "destination");
                    b.debit(destination == null ? "5141" : destination, null, p.montant());
                    b.credit("5115", null, p.montant());
                }
            }
            case "APPORT_FONDS" -> {
                exigerCompte(destination, b, "destination");
                b.debit(destination == null ? "5141" : destination, null, p.montant());
                b.credit("4463", null, p.montant());
            }
            case "RETRAIT_ESPECES_GERANT" -> {
                exigerCompte(source, b, "source");
                b.debit("4463", null, p.montant());
                b.credit(source == null ? "51611" : source, null, p.montant());
            }
            default -> {
                if (c.ventilation().isEmpty()) {
                    b.compteAChoisir("Choisissez la contrepartie de ce mouvement : un encaissement divers peut doubler un règlement.");
                    b.requis("VENTILATION");
                    return b.fin();
                }
                String contrepartie = c.ventilation().get(0).compte();
                boolean entree = "ENCAISSEMENT_DIVERS".equals(type);
                String tresorerie = entree ? destination : source;
                exigerCompte(tresorerie, b, entree ? "destination" : "source");
                String t = tresorerie == null ? "5141" : tresorerie;
                b.debit(entree ? t : contrepartie, null, p.montant());
                b.credit(entree ? contrepartie : t, null, p.montant());
            }
        }
        return b.fin();
    }

    private static void exigerCompte(String compte, Constructeur b, String quel) {
        if (compte == null) {
            b.compteAChoisir("Le compte financier " + quel + " de ce mouvement n'a pas de compte comptable.");
        }
    }

    // --- R11

    private static PropositionEcriture ligneReleve(PieceSource p, ContexteRegles c) {
        boolean sortie = p.ht().signum() > 0;
        Constructeur b = new Constructeur(COMPTABILISATION, "BQ", p.date(), p.numero(), p.attr("libelle") != null ? p.attr("libelle") : "Relevé " + p.numero());
        String banque = compteDuCompteFinancier(p.attr("compte_financier_id"), c);
        if (banque == null) {
            banque = c.compte("BANQUE_DEFAUT", "").orElse("5141");
        }
        if (c.ventilation().isEmpty()) {
            b.compteAChoisir("Choisissez le compte de cette ligne de relevé.");
            b.requis("VENTILATION");
            return b.fin();
        }
        if (sortie) {
            debitsVentiles(c.ventilation(), b);
            b.credit(banque, null, p.montant());
        } else {
            b.debit(banque, null, p.montant());
            for (LigneVentilation v : c.ventilation()) {
                b.credit(v.compte(), null, v.ht().add(v.tva()));
            }
        }
        return b.fin();
    }

    // --- Communs

    private static String compteTiers(String compte) {
        return compte;
    }

    /** Le compte de trésorerie du compte financier précisé, sinon le compte par défaut avec une alerte : le commercial ne dit pas quel compte a bougé. */
    private static String compteFinancier(ContexteRegles c, String parDefaut, Constructeur b) {
        Long financier = c.complement().compteFinancierId();
        if (financier != null) {
            var compte = c.compte("COMPTE_FINANCIER", String.valueOf(financier));
            if (compte.isPresent()) {
                return compte.get();
            }
        }
        var defaut = c.compte(parDefaut, "");
        if (defaut.isEmpty()) {
            b.compteAChoisir("Aucun compte par défaut pour " + parDefaut + ".");
            return "BANQUE_DEFAUT".equals(parDefaut) ? "5141" : "51611";
        }
        b.alerte("COMPTE_FINANCIER_PAR_DEFAUT", "Compte de trésorerie par défaut : le commercial ne dit pas quel compte a bougé.");
        return defaut.get();
    }

    private static String compteDuCompteFinancier(String id, ContexteRegles c) {
        return id == null ? null : c.compte("COMPTE_FINANCIER", id).orElse(null);
    }

    private static BigDecimal somme(BigDecimal a, BigDecimal b) {
        return a == null ? b : b == null ? a : a.add(b);
    }

    /** Accumule les lignes dans un sens lisible. Un montant nul n'écrit pas de ligne. */
    private static final class Constructeur {
        private final String evenement;
        private final String journal;
        private final LocalDate date;
        private final String ref;
        private final String libelle;
        private final List<LigneProposee> lignes = new ArrayList<>();
        private final List<AnomaliePiece> anomalies = new ArrayList<>();
        private final List<String> requis = new ArrayList<>();

        private Constructeur(String evenement, String journal, LocalDate date, String ref, String libelle) {
            this.evenement = evenement;
            this.journal = journal;
            this.date = date;
            this.ref = ref;
            this.libelle = libelle;
        }

        private void debit(String compte, TiersSource tiers, BigDecimal montant) {
            if (montant.signum() != 0) {
                lignes.add(new LigneProposee(compte, tiers, montant, Z, null, null, null, false));
            }
        }

        private void credit(String compte, TiersSource tiers, BigDecimal montant) {
            if (montant.signum() != 0) {
                lignes.add(new LigneProposee(compte, tiers, Z, montant, null, null, null, false));
            }
        }

        private void tva(String compte, boolean auDebit, BigDecimal montant, BigDecimal taux, BigDecimal base) {
            if (montant.signum() != 0) {
                lignes.add(new LigneProposee(compte, null, auDebit ? montant : Z, auDebit ? Z : montant, taux, base, null, false));
            }
        }

        private void ligne(LigneProposee ligne) {
            lignes.add(ligne);
        }

        private void anomalie(String code, String message) {
            anomalies.add(new AnomaliePiece(code, message, true));
        }

        private void alerte(String code, String message) {
            anomalies.add(new AnomaliePiece(code, message, false));
        }

        private void compteAChoisir(String message) {
            anomalie("COMPTE_A_CHOISIR", message);
        }

        private void requis(String complement) {
            if (!requis.contains(complement)) {
                requis.add(complement);
            }
        }

        private PropositionEcriture fin() {
            return new PropositionEcriture(evenement, journal, date, ref, libelle, List.copyOf(lignes), List.copyOf(anomalies), List.copyOf(requis));
        }
    }
}
