package com.gestion.compta;

import com.gestion.service.compta.ContexteRegles;
import com.gestion.service.compta.ContexteRegles.Complement;
import com.gestion.service.compta.ContexteRegles.LigneVentilation;
import com.gestion.service.compta.PieceSource;
import com.gestion.service.compta.PieceSource.LigneSource;
import com.gestion.service.compta.PieceSource.TiersSource;
import com.gestion.service.compta.PropositionEcriture;
import com.gestion.service.compta.PropositionEcriture.LigneProposee;
import com.gestion.service.compta.ReglesComptabilisation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Une règle, ses variantes, les comptes exacts. Sans base : ce qui s'affiche est ce qui se poste. */
@Tag("compta")
class ReglesComptabilisationTest {

    private static final LocalDate JOUR = LocalDate.parse("2026-01-15");
    private static final TiersSource CLIENT = new TiersSource("CLIENT", 7L, "Droguerie", "Maroc");
    private static final TiersSource FOURNISSEUR = new TiersSource("FOURNISSEUR", 8L, "Imprimerie", "Maroc");
    private static final Map<String, String> TABLE = table();

    private static Map<String, String> table() {
        Map<String, String> t = new HashMap<>();
        t.put("VENTE_TYPE_PRODUIT|MARCHANDISE", "7111");
        t.put("VENTE_TYPE_PRODUIT|SERVICE", "71243");
        t.put("VENTE_TYPE_PRODUIT|MARCHANDISE_ETRANGER", "7113");
        t.put("ACHAT_TYPE_PRODUIT|MARCHANDISE", "6111");
        t.put("ACHAT_TYPE_PRODUIT|SERVICE", "61263");
        t.put("RRR_VENTE|MARCHANDISE", "7119");
        t.put("RRR_ACHAT|MARCHANDISE", "6119");
        t.put("BANQUE_DEFAUT|", "5141");
        t.put("CAISSE_DEFAUT|", "51611");
        t.put("COMPTE_FINANCIER|2", "514102");
        t.put("CATEGORIE_DEPENSE|LOYER", "61312");
        t.put("CATEGORIE_DEPENSE|FRAIS_BANCAIRES", "61473");
        t.put("TYPE_CHARGE|DROITS_DOUANE", "6111");
        return t;
    }

    @Test
    void r01FactureAvecRemiseSurLesComptesDeVenteS6() {
        PropositionEcriture r = proposer(facture("1000.00", "200.00", "1200.00", "200.00", "1000.00", ligne("MARCHANDISE", "20.00", "1000.00", "200.00")), "NET_TVA_FACTUREE");

        assertEquals("VT", r.journal());
        attendre(r, l("3421", "1000.00", "0"), l("7111", "0", "800.00"), l("4455", "0", "200.00"));
        LigneProposee tva = r.lignes().get(2);
        assertEquals(0, new BigDecimal("20.00").compareTo(tva.taux()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(tva.base()));
    }

    @Test
    void r01ModeNetTvaReduiteBaisseAussiLaTva() {
        PropositionEcriture r = proposer(facture("1000.00", "200.00", "1200.00", "120.00", "1080.00", ligne("MARCHANDISE", "20.00", "1000.00", "200.00")), "NET_TVA_REDUITE");

        attendre(r, l("3421", "1080.00", "0"), l("7111", "0", "900.00"), l("4455", "0", "180.00"));
    }

    @Test
    void r01PlusieursTauxEtPlusieursComptes() {
        PropositionEcriture r = proposer(facture("1500.00", "250.00", "1750.00", "0", "1750.00",
                ligne("MARCHANDISE", "20.00", "1000.00", "200.00"), ligne("SERVICE", "10.00", "500.00", "50.00")), "NET_TVA_FACTUREE");

        attendre(r, l("3421", "1750.00", "0"), l("7111", "0", "1000.00"), l("71243", "0", "500.00"), l("4455", "0", "50.00"), l("4455", "0", "200.00"));
    }

    @Test
    void r01LaRemiseSeRepartitAuCentimePres() {
        PropositionEcriture r = proposer(facture("300.00", "0", "300.00", "100.00", "200.00",
                ligne("MARCHANDISE", "0", "100.00", "0"), ligne("SERVICE", "0", "200.00", "0")), "NET_TVA_FACTUREE");

        BigDecimal credits = r.lignes().stream().map(LigneProposee::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("200.00").compareTo(credits));
        attendre(r, l("3421", "200.00", "0"), l("7111", "0", "66.67"), l("71243", "0", "133.33"));
    }

    @Test
    void r01UnClientEtrangerVaSurLesVentesALExport() {
        PieceSource p = new PieceSource("FACTURE", 1L, "FV-1", JOUR, 1L, new TiersSource("CLIENT", 7L, "Acme", "France"), bd("100.00"), bd("100.00"), bd("0"), bd("100.00"), bd("0"), "VALIDEE", false, null, "e",
                Map.of(), List.of(ligne("MARCHANDISE", "0", "100.00", "0")));
        attendre(proposer(p, "NET_TVA_FACTUREE"), l("3421", "100.00", "0"), l("7113", "0", "100.00"));
    }

    @Test
    void r01AnomaliesDeLaFacture() {
        PropositionEcriture ecart = proposer(facture("1000.00", "200.00", "1200.00", "0", "1200.00", ligne("MARCHANDISE", "20.00", "900.00", "180.00")), "NET_TVA_FACTUREE");
        assertTrue(ecart.anomalies().stream().anyMatch(a -> a.code().equals("LIGNES_DIFFERENTES_DE_L_EN_TETE")));
        assertTrue(ecart.bloquee());

        PropositionEcriture remise = proposer(facture("100.00", "20.00", "120.00", "150.00", "0", ligne("MARCHANDISE", "20.00", "100.00", "20.00")), "NET_TVA_FACTUREE");
        assertTrue(remise.anomalies().stream().anyMatch(a -> a.code().equals("REMISE_SUPERIEURE_AU_HT")));

        PieceSource revalidee = new PieceSource("FACTURE", 1L, "FV-1", JOUR, 1L, CLIENT, bd("120.00"), bd("100.00"), bd("20.00"), bd("120.00"), bd("0"), "VALIDEE", true, null, "e", Map.of(), List.of(ligne("MARCHANDISE", "20.00", "100.00", "20.00")));
        assertTrue(proposer(revalidee, "NET_TVA_FACTUREE").anomalies().stream().anyMatch(a -> a.code().equals("FACTURE_ANNULEE_REVALIDEE")));
    }

    @Test
    void unCompteSansCorrespondancePasseEnAnomalieSansEtreInvente() {
        PieceSource p = new PieceSource("FACTURE", 1L, "FV-1", JOUR, 1L, CLIENT, bd("100.00"), bd("100.00"), bd("0"), bd("100.00"), bd("0"), "VALIDEE", false, null, "e", Map.of(), List.of(new LigneSource(1L, "INCONNU", null, bd("0"), bd("100.00"), bd("0"), bd("100.00"))));
        PropositionEcriture r = proposer(p, "NET_TVA_FACTUREE");
        assertTrue(r.anomalies().stream().anyMatch(a -> a.code().equals("COMPTE_A_CHOISIR")));
        assertTrue(r.bloquee());
    }

    @Test
    void laRechercheVaDuProduitAuTypeEnPassantParLaCategorie() {
        Map<String, String> table = new HashMap<>(TABLE);
        table.put("VENTE_CATEGORIE|5", "7121");
        table.put("VENTE_PRODUIT|9", "7122");
        PieceSource p = new PieceSource("FACTURE", 1L, "F", JOUR, 1L, CLIENT, bd("300.00"), bd("300.00"), bd("0"), bd("300.00"), bd("0"), "VALIDEE", false, null, "e", Map.of(), List.of(
                new LigneSource(9L, "MARCHANDISE", 5L, bd("0"), bd("100.00"), bd("0"), bd("100.00")),
                new LigneSource(1L, "MARCHANDISE", 5L, bd("0"), bd("100.00"), bd("0"), bd("100.00")),
                new LigneSource(2L, "MARCHANDISE", 6L, bd("0"), bd("100.00"), bd("0"), bd("100.00"))));
        attendre(ReglesComptabilisation.proposer(p, "COMPTABILISATION", contexte(table, "NET_TVA_FACTUREE", List.of(), Complement.AUCUN, Set.of(), true)),
                l("3421", "300.00", "0"), l("7111", "0", "100.00"), l("7121", "0", "100.00"), l("7122", "0", "100.00"));
    }

    @Test
    void r02AvoirClientRetourEtCommercial() {
        PieceSource retour = avoir("AVOIR_CLIENT", CLIENT, "RETOUR_MARCHANDISE");
        attendre(proposer(retour, null), l("7111", "100.00", "0"), l("4455", "20.00", "0"), l("3421", "0", "120.00"));
        PieceSource commercial = avoir("AVOIR_CLIENT", CLIENT, "AVOIR_COMMERCIAL");
        attendre(proposer(commercial, null), l("7119", "100.00", "0"), l("4455", "20.00", "0"), l("3421", "0", "120.00"));
        assertTrue(proposer(avoir("AVOIR_CLIENT", CLIENT, "ERREUR_FACTURATION"), null).anomalies().stream().anyMatch(a -> !a.bloquante() && a.code().equals("ERREUR_FACTURATION")));
    }

    @Test
    void r03AvoirFournisseur() {
        attendre(proposer(avoir("AVOIR_FOURNISSEUR", FOURNISSEUR, "RETOUR_MARCHANDISE"), null), l("4411", "120.00", "0"), l("6111", "0", "100.00"), l("34552", "0", "20.00"));
        attendre(proposer(avoir("AVOIR_FOURNISSEUR", FOURNISSEUR, "AVOIR_COMMERCIAL"), null), l("4411", "120.00", "0"), l("6119", "0", "100.00"), l("34552", "0", "20.00"));
    }

    @Test
    void r04FactureDAchatEtVentilation() {
        PieceSource achat = new PieceSource("FACTURE_ACHAT", 1L, "FA-1", JOUR, 1L, FOURNISSEUR, bd("1200.00"), bd("1000.00"), bd("200.00"), bd("1200.00"), bd("0"), "ANNULEE", false, null, "e", Map.of(), List.of(ligne("MARCHANDISE", "20.00", "1000.00", "200.00")));
        PropositionEcriture r = proposer(achat, null);
        assertEquals("AC", r.journal());
        attendre(r, l("6111", "1000.00", "0"), l("34552", "200.00", "0"), l("4411", "0", "1200.00"));
        assertTrue(r.anomalies().stream().anyMatch(a -> !a.bloquante() && a.code().equals("REF_FOURNISSEUR_ABSENTE")));

        LigneVentilation machine = new LigneVentilation("2332", bd("800.00"), bd("20.00"), bd("160.00"), true, true, "F-771");
        LigneVentilation repas = new LigneVentilation("61321", bd("200.00"), bd("20.00"), bd("40.00"), false, false, null);
        PropositionEcriture ventilee = ReglesComptabilisation.proposer(achat, "COMPTABILISATION", contexte(TABLE, null, List.of(machine, repas), Complement.AUCUN, Set.of(), true));
        assertEquals("F-771", ventilee.refPiece());
        attendre(ventilee, l("2332", "800.00", "0"), l("61321", "240.00", "0"), l("34551", "160.00", "0"), l("4411", "0", "1200.00"));
        assertEquals("ACQUISITION", ventilee.lignes().get(0).natureMouvement());
    }

    @Test
    void r05PaiementSelonLeMode() {
        attendre(proposer(paiement("ESPECES", CLIENT), null), l("51611", "1000.00", "0"), l("3421", "0", "1000.00"));
        assertEquals("CA", proposer(paiement("ESPECES", CLIENT), null).journal());
        attendre(proposer(paiement("VIREMENT", CLIENT), null), l("5141", "1000.00", "0"), l("3421", "0", "1000.00"));
        attendre(proposer(paiement("CARTE_BANCAIRE", CLIENT), null), l("5141", "1000.00", "0"), l("3421", "0", "1000.00"));
        attendre(proposer(paiement("CHEQUE", CLIENT), null), l("51111", "1000.00", "0"), l("3421", "0", "1000.00"));
        attendre(proposer(paiement("LCN", CLIENT), null), l("3425", "1000.00", "0"), l("3421", "0", "1000.00"));
        assertEquals(List.of(), ReglesComptabilisation.evenementsDus(paiement("CREDIT", CLIENT), JOUR), "un crédit n'est pas un encaissement");
    }

    @Test
    void r05LeCompteFinancierPreciseRemplaceLeDefautSansAlerte() {
        PropositionEcriture parDefaut = proposer(paiement("VIREMENT", CLIENT), null);
        assertTrue(parDefaut.anomalies().stream().anyMatch(a -> a.code().equals("COMPTE_FINANCIER_PAR_DEFAUT") && !a.bloquante()));

        PropositionEcriture precise = ReglesComptabilisation.proposer(paiement("VIREMENT", CLIENT), "COMPTABILISATION", contexte(TABLE, null, List.of(), new Complement(2L, null, null), Set.of(), true));
        attendre(precise, l("514102", "1000.00", "0"), l("3421", "0", "1000.00"));
        assertFalse(precise.anomalies().stream().anyMatch(a -> a.code().equals("COMPTE_FINANCIER_PAR_DEFAUT")));
    }

    @Test
    void r05SansClientNiVenteLeSensEstInconnu() {
        assertTrue(proposer(paiement("VIREMENT", null), null).anomalies().stream().anyMatch(a -> a.code().equals("SENS_INCONNU") && a.bloquante()));
        PieceSource deVente = new PieceSource("PAIEMENT", 1L, "PAY-1", JOUR, 1L, null, bd("10.00"), bd("10.00"), bd("0"), bd("10.00"), bd("0"), "VALIDE", false, null, "e", Map.of("mode", "ESPECES", "vente_id", "9"), List.of());
        PropositionEcriture r = proposer(deVente, null);
        assertFalse(r.bloquee());
        assertEquals("CLIENTS DIVERS", r.lignes().get(1).tiers().nom());
    }

    @Test
    void r06ChequeDuPortefeuilleALaBanque() {
        PieceSource cheque = effet("CHEQUE", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE");
        attendre(effetEvenement(cheque, "REMISE", Set.of()), l("51112", "1000.00", "0"), l("51111", "0", "1000.00"));
        attendre(effetEvenement(cheque, "ENCAISSEMENT", Set.of("REMISE")), l("5141", "1000.00", "0"), l("51112", "0", "1000.00"));
        attendre(effetEvenement(cheque, "ENCAISSEMENT", Set.of()), l("5141", "1000.00", "0"), l("51111", "0", "1000.00"));
    }

    @Test
    void r06LeRejetDependDeLaPositionDeLEffet() {
        PieceSource cheque = effet("CHEQUE", "ENCAISSEMENT_CLIENT", "IMPAYE_REJETE");
        Complement date = new Complement(null, JOUR, null);
        attendre(rejet(cheque, Set.of(), date), l("3421", "1000.00", "0"), l("51111", "0", "1000.00"));
        attendre(rejet(cheque, Set.of("REMISE"), date), l("3421", "1000.00", "0"), l("51112", "0", "1000.00"));
        attendre(rejet(cheque, Set.of("REMISE", "ENCAISSEMENT"), date), l("3421", "1000.00", "0"), l("5141", "0", "1000.00"));

        PropositionEcriture sansDate = rejet(cheque, Set.of(), Complement.AUCUN);
        assertTrue(sansDate.anomalies().stream().anyMatch(a -> a.code().equals("DATE_A_SAISIR") && a.bloquante()), "le commercial ne garde pas la date du rejet");
        assertEquals(List.of("DATE_EFFET"), sansDate.complementsRequis());
    }

    @Test
    void r06TraiteOuLcn() {
        PieceSource lcn = effet("LCN", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE");
        attendre(effetEvenement(lcn, "REMISE", Set.of()), l("51132", "1000.00", "0"), l("3425", "0", "1000.00"));
        attendre(effetEvenement(lcn, "ENCAISSEMENT", Set.of("REMISE")), l("5141", "1000.00", "0"), l("51132", "0", "1000.00"));
        attendre(effetEvenement(lcn, "ENCAISSEMENT", Set.of()), l("5141", "1000.00", "0"), l("3425", "0", "1000.00"));
    }

    @Test
    void r06LePaiementDoitPasserDAbord() {
        PieceSource cheque = effet("CHEQUE", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE");
        PropositionEcriture r = ReglesComptabilisation.proposer(cheque, "ENCAISSEMENT", contexte(TABLE, null, List.of(), Complement.AUCUN, Set.of(), false));
        assertTrue(r.anomalies().stream().anyMatch(a -> a.code().equals("PIECE_PREALABLE")));
        PieceSource orphelin = new PieceSource("EFFET", 3L, "CHQ-1", JOUR, 1L, CLIENT, bd("1000.00"), bd("1000.00"), bd("0"), bd("1000.00"), bd("0"), "ENCAISSE_COMPTABILISE", false, null, "e", Map.of("type_effet", "CHEQUE", "sens", "ENCAISSEMENT_CLIENT"), List.of());
        assertTrue(effetEvenement(orphelin, "ENCAISSEMENT", Set.of()).anomalies().stream().anyMatch(a -> a.code().equals("EFFET_SANS_PAIEMENT")));
    }

    @Test
    void lesEvenementsDusSuiventLEtatDeLEffet() {
        assertEquals(List.of(), ReglesComptabilisation.evenementsDus(effet("CHEQUE", "ENCAISSEMENT_CLIENT", "EN_PORTEFEUILLE"), JOUR));
        assertEquals(List.of("ENCAISSEMENT"), ReglesComptabilisation.evenementsDus(effet("CHEQUE", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE"), JOUR));
        Map<String, String> remis = new HashMap<>(effet("CHEQUE", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE").attributs());
        remis.put("date_remise", "2026-01-10");
        PieceSource avecRemise = new PieceSource("EFFET", 3L, "CHQ-1", JOUR, 1L, CLIENT, bd("1000.00"), bd("1000.00"), bd("0"), bd("1000.00"), bd("0"), "ENCAISSE_COMPTABILISE", false, null, "e", remis, List.of());
        assertEquals(List.of("REMISE", "ENCAISSEMENT"), ReglesComptabilisation.evenementsDus(avecRemise, JOUR));
        assertEquals(List.of(), ReglesComptabilisation.evenementsDus(effet("CHEQUE", "DECAISSEMENT_FOURNISSEUR", "ENCAISSE_COMPTABILISE"), JOUR), "la banque a été créditée au règlement");
        assertEquals(List.of("ECHEANCE"), ReglesComptabilisation.evenementsDus(effet("TRAITE", "DECAISSEMENT_FOURNISSEUR", "ENCAISSE_COMPTABILISE"), JOUR));
    }

    @Test
    void r07EffetFournisseur() {
        PieceSource traite = effet("TRAITE", "DECAISSEMENT_FOURNISSEUR", "ENCAISSE_COMPTABILISE");
        attendre(effetEvenement(traite, "ECHEANCE", Set.of()), l("4415", "1000.00", "0"), l("5141", "0", "1000.00"));
        Complement date = new Complement(null, JOUR, null);
        PieceSource cheque = effet("CHEQUE", "DECAISSEMENT_FOURNISSEUR", "IMPAYE_REJETE");
        attendre(rejet(cheque, Set.of(), date), l("5141", "1000.00", "0"), l("4411", "0", "1000.00"));
        attendre(rejet(effet("TRAITE", "DECAISSEMENT_FOURNISSEUR", "IMPAYE_REJETE"), Set.of(), date), l("4415", "1000.00", "0"), l("4411", "0", "1000.00"));
    }

    @Test
    void r07ReglementFournisseurSelonLeMode() {
        attendre(proposer(reglement("VIREMENT", "FACTURE"), null), l("4411", "1000.00", "0"), l("5141", "0", "1000.00"));
        attendre(proposer(reglement("ESPECES", "FACTURE"), null), l("4411", "1000.00", "0"), l("51611", "0", "1000.00"));
        attendre(proposer(reglement("CHEQUE", "FACTURE"), null), l("4411", "1000.00", "0"), l("5141", "0", "1000.00"));
        attendre(proposer(reglement("TRAITE", "FACTURE"), null), l("4411", "1000.00", "0"), l("4415", "0", "1000.00"));
        attendre(proposer(reglement("VIREMENT", "ACOMPTE_COMMANDE"), null), l("3411", "1000.00", "0"), l("5141", "0", "1000.00"));
        attendre(proposer(reglement("TRAITE", "ACOMPTE_COMMANDE"), null), l("3411", "1000.00", "0"), l("4415", "0", "1000.00"));
    }

    @Test
    void r07ImputationDAcompteEtEcheance() {
        PieceSource impute = reglement("VIREMENT", "ACOMPTE_IMPUTE");
        assertEquals(List.of("COMPTABILISATION", "IMPUTATION_ACOMPTE"), ReglesComptabilisation.evenementsDus(impute, JOUR));
        attendre(proposer(impute, null), l("3411", "1000.00", "0"), l("5141", "0", "1000.00"));
        attendre(ReglesComptabilisation.proposer(impute, "IMPUTATION_ACOMPTE", contexte(TABLE, null, List.of(), Complement.AUCUN, Set.of(), true)), l("4411", "1000.00", "0"), l("3411", "0", "1000.00"));

        Map<String, String> attributs = new HashMap<>(reglement("LCN", "FACTURE").attributs());
        attributs.put("date_echeance", "2026-01-10");
        PieceSource lcn = new PieceSource("REGLEMENT_FOURNISSEUR", 5L, "REG-1", JOUR, 1L, FOURNISSEUR, bd("1000.00"), bd("1000.00"), bd("0"), bd("1000.00"), bd("0"), "VALIDE", false, null, "e", attributs, List.of());
        assertEquals(List.of("COMPTABILISATION", "ECHEANCE"), ReglesComptabilisation.evenementsDus(lcn, JOUR));
        assertEquals(List.of("COMPTABILISATION"), ReglesComptabilisation.evenementsDus(lcn, LocalDate.parse("2026-01-09")), "pas avant l'échéance");
        attendre(ReglesComptabilisation.proposer(lcn, "ECHEANCE", contexte(TABLE, null, List.of(), Complement.AUCUN, Set.of(), true)), l("4415", "1000.00", "0"), l("5141", "0", "1000.00"));
    }

    @Test
    void r08DepensePayeeEtACredit() {
        PropositionEcriture payee = proposer(depense("LOYER", "VIREMENT", null, Map.of()), null);
        attendre(payee, l("61312", "1200.00", "0"), l("5141", "0", "1200.00"));
        assertTrue(payee.anomalies().stream().anyMatch(a -> a.code().equals("TVA_NON_VENTILEE") && !a.bloquante()), "la TVA ne se fabrique pas");
        attendre(proposer(depense("LOYER", "ESPECES", null, Map.of()), null), l("61312", "1200.00", "0"), l("51611", "0", "1200.00"));
        attendre(proposer(depense("LOYER", "CREDIT", FOURNISSEUR, Map.of()), null), l("61312", "1200.00", "0"), l("4411", "0", "1200.00"));
        assertTrue(proposer(depense("LOYER", "CREDIT", null, Map.of()), null).anomalies().stream().anyMatch(a -> a.code().equals("TIERS_REQUIS")));
    }

    @Test
    void r08AutresChargesDemandeUnCompte() {
        PropositionEcriture r = proposer(depense("AUTRES_CHARGES", "VIREMENT", null, Map.of()), null);
        assertTrue(r.anomalies().stream().anyMatch(a -> a.code().equals("COMPTE_A_CHOISIR")));
        assertEquals(List.of("VENTILATION"), r.complementsRequis());

        LigneVentilation choix = new LigneVentilation("61671", bd("1000.00"), bd("20.00"), bd("200.00"), true, false, null);
        PropositionEcriture ventilee = ReglesComptabilisation.proposer(depense("AUTRES_CHARGES", "VIREMENT", null, Map.of()), "COMPTABILISATION", contexte(TABLE, null, List.of(choix), Complement.AUCUN, Set.of(), true));
        attendre(ventilee, l("61671", "1000.00", "0"), l("34552", "200.00", "0"), l("5141", "0", "1200.00"));
        assertFalse(ventilee.bloquee());

        LigneVentilation fausse = new LigneVentilation("61671", bd("1.00"), bd("0"), bd("0"), true, false, null);
        assertTrue(ReglesComptabilisation.proposer(depense("AUTRES_CHARGES", "VIREMENT", null, Map.of()), "COMPTABILISATION", contexte(TABLE, null, List.of(fausse), Complement.AUCUN, Set.of(), true))
                .anomalies().stream().anyMatch(a -> a.code().equals("VENTILATION_DIFFERENTE")));
    }

    @Test
    void r08ChargesDApproche() {
        attendre(proposer(depense("AUTRES_CHARGES", "VIREMENT", null, Map.of("type_charge", "TVA_IMPORT")), null), l("34552", "1200.00", "0"), l("5141", "0", "1200.00"));
        attendre(proposer(depense("AUTRES_CHARGES", "VIREMENT", null, Map.of("type_charge", "DROITS_DOUANE")), null), l("6111", "1200.00", "0"), l("5141", "0", "1200.00"));
        assertTrue(proposer(depense("LOYER", "VIREMENT", null, Map.of("charge_statut", "ESTIMEE")), null).anomalies().stream().anyMatch(a -> a.code().equals("CHARGE_ESTIMEE")));
        assertTrue(ReglesComptabilisation.sansObjet(depense("LOYER", "VIREMENT", null, Map.of("charge_facture_achat_id", "4"))), "seule la facture est passée");
    }

    @Test
    void r09VentesRegroupees() {
        PieceSource v1 = vente(1L, null, "120.00", "100.00", "20.00");
        PieceSource v2 = vente(2L, null, "60.00", "50.00", "10.00");
        ContexteRegles c = contexte(TABLE, "NET_TVA_FACTUREE", List.of(), Complement.AUCUN, Set.of(), true);
        PropositionEcriture p1 = ReglesComptabilisation.proposer(v1, "COMPTABILISATION", c);
        attendre(p1, l("3421", "120.00", "0"), l("7111", "0", "100.00"), l("4455", "0", "20.00"));
        assertEquals("CLIENTS DIVERS", p1.lignes().get(0).tiers().nom());

        PropositionEcriture groupe = ReglesComptabilisation.regrouperVentes(List.of(p1, ReglesComptabilisation.proposer(v2, "COMPTABILISATION", c)), "VENTES-2026-01-15");
        attendre(groupe, l("3421", "180.00", "0"), l("7111", "0", "150.00"), l("4455", "0", "30.00"));
        assertEquals(0, new BigDecimal("150.00").compareTo(groupe.lignes().get(2).base()));
        assertTrue(ReglesComptabilisation.sansObjet(new PieceSource("VENTE", 1L, "T", JOUR, 1L, null, bd("1"), bd("1"), bd("0"), bd("1"), bd("0"), "VALIDEE", false, null, "e", Map.of("facture_id", "4"), List.of())), "la facture fait foi");
    }

    @Test
    void r10MouvementsDeTresorerie() {
        Map<String, String> table = new HashMap<>(TABLE);
        table.put("COMPTE_FINANCIER|1", "51611");
        table.put("COMPTE_FINANCIER|2", "5141");
        PieceSource depot = mouvement("DEPOT_BANQUE");
        assertEquals(List.of("DEPART", "ARRIVEE"), ReglesComptabilisation.evenementsDus(depot, JOUR));
        ContexteRegles c = contexte(table, null, List.of(), Complement.AUCUN, Set.of(), true);
        attendre(ReglesComptabilisation.proposer(depot, "DEPART", c), l("5115", "500.00", "0"), l("51611", "0", "500.00"));
        attendre(ReglesComptabilisation.proposer(depot, "ARRIVEE", c), l("5141", "500.00", "0"), l("5115", "0", "500.00"));
        attendre(ReglesComptabilisation.proposer(mouvement("APPORT_FONDS"), "COMPTABILISATION", c), l("5141", "500.00", "0"), l("4463", "0", "500.00"));
        attendre(ReglesComptabilisation.proposer(mouvement("RETRAIT_ESPECES_GERANT"), "COMPTABILISATION", c), l("4463", "500.00", "0"), l("51611", "0", "500.00"));
        assertTrue(ReglesComptabilisation.proposer(mouvement("ENCAISSEMENT_DIVERS"), "COMPTABILISATION", c).anomalies().stream().anyMatch(a -> a.code().equals("COMPTE_A_CHOISIR")));
        assertEquals(List.of(), ReglesComptabilisation.evenementsDus(mouvement("AJUSTEMENT_SOLDE"), JOUR), "un ajustement de solde ne s'écrit jamais");
    }

    @Test
    void r11LigneDeReleveSurActionExplicite() {
        PieceSource frais = new PieceSource("LIGNE_RELEVE", 1L, "REL-1", JOUR, 1L, null, bd("110.00"), bd("110.00"), bd("0"), bd("0"), bd("0"), "VALIDE", false, null, "e", Map.of("compte_financier_id", "2", "libelle", "Frais"), List.of());
        assertTrue(proposer(frais, null).anomalies().stream().anyMatch(a -> a.code().equals("COMPTE_A_CHOISIR")), "un relevé ne produit rien seul");
        LigneVentilation choix = new LigneVentilation("61473", bd("100.00"), bd("10.00"), bd("10.00"), true, false, null);
        attendre(ReglesComptabilisation.proposer(frais, "COMPTABILISATION", contexte(TABLE, null, List.of(choix), Complement.AUCUN, Set.of(), true)), l("61473", "100.00", "0"), l("34552", "10.00", "0"), l("514102", "0", "110.00"));
    }

    @Test
    void chaqueProposerSEquilibre() {
        List<PropositionEcriture> propositions = new ArrayList<>(List.of(
                proposer(facture("1000.00", "200.00", "1200.00", "37.13", "1162.87", ligne("MARCHANDISE", "20.00", "700.00", "140.00"), ligne("SERVICE", "20.00", "300.00", "60.00")), "NET_TVA_FACTUREE"),
                proposer(facture("1000.00", "200.00", "1200.00", "37.13", "1162.87", ligne("MARCHANDISE", "20.00", "700.00", "140.00"), ligne("SERVICE", "10.00", "300.00", "60.00")), "NET_TVA_REDUITE"),
                proposer(avoir("AVOIR_CLIENT", CLIENT, "RETOUR_MARCHANDISE"), null), proposer(avoir("AVOIR_FOURNISSEUR", FOURNISSEUR, "AVOIR_COMMERCIAL"), null),
                proposer(paiement("CHEQUE", CLIENT), null), proposer(reglement("TRAITE", "FACTURE"), null), proposer(depense("LOYER", "CREDIT", FOURNISSEUR, Map.of()), null)));
        for (PropositionEcriture p : propositions) {
            BigDecimal d = p.lignes().stream().map(LigneProposee::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal c = p.lignes().stream().map(LigneProposee::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, d.compareTo(c), p.libelle() + " : " + p.lignes());
        }
    }

    private static PropositionEcriture proposer(PieceSource p, String remiseMode) {
        return ReglesComptabilisation.proposer(p, "COMPTABILISATION", contexte(TABLE, remiseMode, List.of(), Complement.AUCUN, Set.of(), true));
    }

    private static PropositionEcriture effetEvenement(PieceSource p, String evenement, Set<String> passes) {
        return ReglesComptabilisation.proposer(p, evenement, contexte(TABLE, null, List.of(), Complement.AUCUN, passes, true));
    }

    private static PropositionEcriture rejet(PieceSource p, Set<String> passes, Complement complement) {
        return ReglesComptabilisation.proposer(p, "REJET", contexte(TABLE, null, List.of(), complement, passes, true));
    }

    private static ContexteRegles contexte(Map<String, String> table, String remiseMode, List<LigneVentilation> ventilation, Complement complement, Set<String> passes, boolean prealable) {
        return new ContexteRegles(table, remiseMode, ventilation, complement, passes, prealable);
    }

    private static PieceSource facture(String ht, String tva, String ttc, String remise, String finale, LigneSource... lignes) {
        return new PieceSource("FACTURE", 1L, "FV-1", JOUR, 1L, CLIENT, bd(finale), bd(ht), bd(tva), bd(ttc), bd(remise), "VALIDEE", false, null, "e", Map.of(), List.of(lignes));
    }

    private static PieceSource avoir(String type, TiersSource tiers, String nature) {
        return new PieceSource(type, 1L, "AV-1", JOUR, 1L, tiers, bd("120.00"), bd("100.00"), bd("20.00"), bd("120.00"), bd("0"), "VALIDE", false, null, "e", Map.of("nature_avoir", nature), List.of(ligne("MARCHANDISE", "20.00", "100.00", "20.00")));
    }

    private static PieceSource paiement(String mode, TiersSource tiers) {
        return new PieceSource("PAIEMENT", 1L, "PAY-1", JOUR, 1L, tiers, bd("1000.00"), bd("1000.00"), bd("0"), bd("1000.00"), bd("0"), "VALIDE", false, null, "e", Map.of("mode", mode), List.of());
    }

    private static PieceSource effet(String type, String sens, String statut) {
        TiersSource tiers = "ENCAISSEMENT_CLIENT".equals(sens) ? CLIENT : FOURNISSEUR;
        return new PieceSource("EFFET", 3L, "CHQ-1", JOUR, 1L, tiers, bd("1000.00"), bd("1000.00"), bd("0"), bd("1000.00"), bd("0"), statut, false, null, "e",
                Map.of("type_effet", type, "sens", sens, "paiement_id", "1", "numero_paiement", "PAY-1", "date_encaissement", "2026-01-20"), List.of());
    }

    private static PieceSource reglement(String mode, String type) {
        return new PieceSource("REGLEMENT_FOURNISSEUR", 5L, "REG-1", JOUR, 1L, FOURNISSEUR, bd("1000.00"), bd("1000.00"), bd("0"), bd("1000.00"), bd("0"), "VALIDE", false, null, "e", Map.of("mode", mode, "type_reglement", type), List.of());
    }

    private static PieceSource depense(String categorie, String mode, TiersSource tiers, Map<String, String> plus) {
        Map<String, String> attributs = new HashMap<>(plus);
        attributs.put("categorie", categorie);
        attributs.put("mode", mode);
        return new PieceSource("DEPENSE", 6L, "DEP-1", JOUR, 1L, tiers, bd("1200.00"), bd("1200.00"), bd("0"), bd("1200.00"), bd("0"), "VALIDE", false, null, "e", attributs, List.of());
    }

    private static PieceSource vente(Long id, TiersSource tiers, String finale, String ht, String tva) {
        return new PieceSource("VENTE", id, "T-" + id, JOUR, 1L, tiers, bd(finale), bd(ht), bd(tva), bd(finale), bd("0"), "VALIDEE", false, null, "e", Map.of(), List.of(ligne("MARCHANDISE", "20.00", ht, tva)));
    }

    private static PieceSource mouvement(String type) {
        return new PieceSource("MOUVEMENT_TRESORERIE", 9L, "MVT-1", JOUR, 1L, null, bd("500.00"), bd("500.00"), bd("0"), bd("500.00"), bd("0"), "VALIDE", false, null, "e",
                Map.of("type_mouvement", type, "compte_source_id", "1", "compte_destination_id", "2"), List.of());
    }

    private static LigneSource ligne(String type, String taux, String ht, String tva) {
        return new LigneSource(1L, type, null, bd(taux), bd(ht), bd(tva), bd(ht).add(bd(tva)));
    }

    private static BigDecimal bd(String valeur) {
        return new BigDecimal(valeur);
    }

    private static String[] l(String compte, String debit, String credit) {
        return new String[]{compte, debit, credit};
    }

    /** Les lignes attendues, dans l'ordre et au centime. */
    private static void attendre(PropositionEcriture r, String[]... attendues) {
        List<String> vues = r.lignes().stream().map(x -> x.compte() + " D" + x.debit().stripTrailingZeros().toPlainString() + " C" + x.credit().stripTrailingZeros().toPlainString()).toList();
        List<String> voulues = new ArrayList<>();
        for (String[] a : attendues) {
            voulues.add(a[0] + " D" + bd(a[1]).stripTrailingZeros().toPlainString() + " C" + bd(a[2]).stripTrailingZeros().toPlainString());
        }
        assertEquals(voulues, vues);
    }
}
