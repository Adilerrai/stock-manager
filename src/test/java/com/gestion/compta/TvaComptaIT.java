package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComptabiliserRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.PasserelleDtos.ResultatPieceDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaAccepterCodeRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaAccepterRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaControleDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCorrectionLigneRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeclarationDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDepotRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEncaissementDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEtatFichierRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaIdentiteRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaLigneDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaOperationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPaiementRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPeriodeDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPieceVisee;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRattrapageDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRattraperRequest;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.MoteurComptabilisation;
import com.gestion.service.compta.TvaDeclarationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le commercial est inséré en SQL : la TVA part de ses paiements, et n'écrit dans aucune de ses tables. */
@SpringBootTest
class TvaComptaIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");
    private static final String ICE = "001234567000089";

    @Autowired private LivreService livres;
    @Autowired private LivreRepository livreRepository;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private MoteurComptabilisation moteur;
    @Autowired private TvaDeclarationService tva;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private LivreDTO livre;
    private Long pdv;
    private Long client;
    private Long fournisseur;
    private Long produit;
    private int numero = 1;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        pdv = societe.pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        abonnements.modifier(pdv, true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        client = inserer("INSERT INTO clients (nom, nom_complet, pays, point_de_vente_id) VALUES ('Droguerie', 'Droguerie Moulay Idriss', 'Maroc', ?) RETURNING id", pdv);
        fournisseur = inserer("INSERT INTO fournisseurs (raison_social, pays, numero_identification_fiscale, ice, point_de_vente_id) VALUES ('Comptoir Sebou', 'Maroc', '12345678', ?, ?) RETURNING id", ICE, pdv);
        produit = inserer("INSERT INTO produits (reference, description, type_produit, point_de_vente_id) VALUES ('P' || nextval('produits_id_seq'), 'Article', 'MARCHANDISE', ?) RETURNING id", pdv);
        parametres(2026, "MENSUELLE", "ENCAISSEMENT", "100", "0", 1);
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    // --- Les scénarios du lot

    @Test
    void s7CentDixEncaissesSurUneFactureADixPourCentDonnentDix() {
        Long f = facture("2026-01-05", "100.00", "10.00", "0", "10");
        encaissement("2026-01-10", "ESPECES", "110.00", f);

        TvaDeclarationDTO d = ouvrir(2026, 1);

        assertEquals(1, tva.encaissements(d.id()).size());
        montant("10.00", tva.encaissements(d.id()).get(0).tva());
        montant("100.00", tva.encaissements(d.id()).get(0).ht());
        montant("10.00", d.tvaExigible());
        montant("100.00", ligne(d, "118").baseRetenue());
        montant("10.00", ligne(d, "118").taxeRetenue());
        montant("100.00", ligne(d, "060").baseRetenue());
        assertAucun(d, "T11");
    }

    @Test
    void s8LeCreditSeReporteEnTrimestrielEtDuDernierTrimestreAJanvierMensuel() {
        parametres(2026, "TRIMESTRIELLE", "ENCAISSEMENT", "100", "0", 1);
        reglement("2026-02-10", "VIREMENT", "1200.00", factureAchat("2026-02-01", "1000.00", "200.00"));

        TvaDeclarationDTO t1 = ouvrir(2026, 1);
        assertEquals(2, t1.regimeDgi());
        montant("200.00", t1.creditPeriode());
        valider(t1.id());

        TvaDeclarationDTO t2 = ouvrir(2026, 2);
        montant("200.00", t2.creditAnterieur());
        montant("200.00", ligne(t2, "170").taxeRetenue());
        montant("200.00", t2.creditPeriode());
        valider(t2.id());
        valider(ouvrir(2026, 3).id());
        TvaDeclarationDTO t4 = ouvrir(2026, 4);
        montant("200.00", t4.creditPeriode());
        valider(t4.id());

        parametres(2027, "MENSUELLE", "ENCAISSEMENT", "100", "0", 1);
        TvaDeclarationDTO janvier = ouvrir(2027, 1);
        montant("200.00", janvier.creditAnterieur());
        assertEquals(1, janvier.regimeDgi());
    }

    @Test
    void s8ApresDeuxLiquidationsLesComptesSontSoldes() {
        Long fa = factureAchat("2026-01-02", "1000.00", "200.00");
        passer("FACTURE_ACHAT", fa);
        reglement("2026-01-10", "VIREMENT", "1200.00", fa);
        TvaDeclarationDTO janvier = ouvrir(2026, 1);
        montant("200.00", janvier.creditPeriode());
        deposer(janvier.id(), "2026-01-20");

        Long f = facture("2026-02-02", "1000.00", "200.00", "0", "20");
        passer("FACTURE", f);
        encaissement("2026-02-05", "ESPECES", "1200.00", f);
        TvaDeclarationDTO fevrier = ouvrir(2026, 2);
        montant("200.00", fevrier.creditAnterieur());
        montant("200.00", fevrier.tvaExigible());
        montant("0.00", fevrier.tvaDue());
        deposer(fevrier.id(), "2026-02-20");

        solde("3456", "0");
        solde("4456", "0");
        solde("4455", "0");
        solde("34552", "0");
    }

    @Test
    void uneFactureAvecRemisePayeeEnTotaliteSolde4455() {
        Long f = facture("2026-01-05", "1000.00", "200.00", "200.00", "20");
        passer("FACTURE", f);
        encaissement("2026-01-10", "ESPECES", "1000.00", f);

        TvaDeclarationDTO d = ouvrir(2026, 1);

        montant("200.00", d.tvaExigible());
        montant("800.00", ligne(d, "060").baseRetenue());
        assertPresent(d, "T27");
        deposer(d.id(), "2026-01-20");
        solde("4455", "0");
        solde("4456", "-200.00");
    }

    @Test
    void s9UneFactureDeJanvierPayeeEnFevrierEstDeduiteEnFevrierUneSeuleFois() {
        Long fa = factureAchat("2026-01-15", "1000.00", "200.00");
        reglementRef("RF-S9", "2026-02-10", "CHEQUE", "1200.00", fa);
        effet("CHQ-S9", "CHEQUE", "DECAISSEMENT_FOURNISSEUR", "ENCAISSE_COMPTABILISE", "1200.00", "2026-02-12", "RF-S9", null, fournisseur);

        TvaDeclarationDTO janvier = ouvrir(2026, 1);
        assertTrue(tva.deductions(janvier.id()).isEmpty(), "une facture sans paiement n'entre pas au relevé");
        TvaDeclarationDTO fevrier = ouvrir(2026, 2);
        List<TvaDeductionDTO> deductions = tva.deductions(fevrier.id());
        assertEquals(1, deductions.size());
        assertEquals(2, deductions.get(0).mpId(), "le mode réel, jamais un virement inventé");
        assertEquals(LocalDate.parse("2026-02-12"), deductions.get(0).datePaiement());
        montant("200.00", fevrier.tvaDeductible());
        assertEquals(0, new BigDecimal("200.00").compareTo(janvier.tvaDeductible().add(fevrier.tvaDeductible())), "cumul de 200");

        TvaDeductionDTO o = deductions.get(0);
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO compta_tva_operations_deductibles (livre_id, declaration_id, source_type, source_id, facture_achat_id, point_de_vente_id, empreinte_source, numero_facture, date_facture, fournisseur_nom, designation, rubrique_code, compte_tva, ht, taux, tva, ttc, tva_deductible, mp_id, date_paiement)
                VALUES (?, ?, 'REGLEMENT_FOURNISSEUR', ?, ?, ?, repeat('0', 32), 'X', '2026-01-15', 'X', 'X', 'ACHATS_INTERIEUR', '34552', 1000, 20, 200, 1200, 200, 2, '2026-03-01')
                """, livre.livreId(), janvier.id(), o.sourceId(), fa, pdv), "uq_compta_tvo_unique refuse la seconde déduction");
    }

    @Test
    void lesDeuxExemplesDeLiquidationDonnentLEcritureAttendue() {
        parametres(2026, "MENSUELLE", "ENCAISSEMENT", "100", "5.00", 1);
        Long f = facture("2026-01-05", "500.00", "100.00", "0", "20");
        passer("FACTURE", f);
        encaissement("2026-01-10", "ESPECES", "600.00", f);
        Long charges = factureAchat("2026-01-05", "250.00", "50.00");
        passer("FACTURE_ACHAT", charges);
        reglement("2026-01-10", "VIREMENT", "300.00", charges);
        Long immo = factureAchat("2026-01-05", "50.00", "10.00");
        ventilationImmobilisation(immo);
        reglement("2026-01-10", "VIREMENT", "60.00", immo);

        TvaDeclarationDTO d = ouvrir(2026, 1);
        montant("35.00", d.tvaDue());
        d = deposer(d.id(), "2026-01-20");

        assertNotNull(d.ecritureLiquidationId());
        ligneEcriture(d.ecritureLiquidationId(), "4455", "100.00", "0");
        ligneEcriture(d.ecritureLiquidationId(), "34552", "0", "50.00");
        ligneEcriture(d.ecritureLiquidationId(), "34551", "0", "10.00");
        ligneEcriture(d.ecritureLiquidationId(), "3456", "0", "5.00");
        solde("4456", "-35.00");

        TvaDeclarationDTO payee = tva.paiement(d.id(), new TvaPaiementRequest(LocalDate.parse("2026-01-25"), "VIREMENT", null));
        assertEquals("PAYEE", payee.statut());
        solde("4456", "0");
        solde("5141", "-35.00");

        // Second exemple : exigible 40, déductions 60, crédit antérieur 5
        parametres(2026, "MENSUELLE", "ENCAISSEMENT", "100", "5.00", 1);
        Long f2 = facture("2026-02-05", "200.00", "40.00", "0", "20");
        passer("FACTURE", f2);
        encaissement("2026-02-10", "ESPECES", "240.00", f2);
        Long a2 = factureAchat("2026-02-05", "300.00", "60.00");
        passer("FACTURE_ACHAT", a2);
        reglement("2026-02-10", "VIREMENT", "360.00", a2);
        // La période précédente est payée sans crédit : le crédit de 5 se saisit, avec son motif
        TvaDeclarationDTO e = ouvrir(2026, 2);
        e = tva.corrigerLigne(e.id(), new TvaCorrectionLigneRequest("170", null, new BigDecimal("5.00"), "Reprise du crédit de l'exemple"));
        montant("25.00", e.creditPeriode());
        assertPresent(e, "T25");
        e = deposer(e.id(), "2026-02-20");
        ligneEcriture(e.ecritureLiquidationId(), "3456", "25.00", "5.00");
        solde("4456", "0");
    }

    @Test
    void leCompte3455SeulEstRefuse() {
        reglement("2026-01-10", "VIREMENT", "120.00", factureAchat("2026-01-05", "100.00", "20.00"));
        TvaDeclarationDTO d = ouvrir(2026, 1);

        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compta_tva_operations_deductibles SET compte_tva = '3455' WHERE declaration_id = ?", d.id()), "ck_compta_tvo_compte");
    }

    @Test
    void uneDeclarationValideeEstFigeeEtUnChangementDeSourceSeRegularise() {
        Long f = facture("2026-01-05", "1000.00", "200.00", "0", "20");
        passer("FACTURE", f);
        Long p = encaissement("2026-01-10", "ESPECES", "1200.00", f);
        TvaDeclarationDTO janvier = ouvrir(2026, 1);
        valider(janvier.id());

        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compta_tva_operations_collectees SET ht = 1, tva = 1, ttc = 2 WHERE declaration_id = ?", janvier.id()), "compta_tva_figee");
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compta_tva_declarations SET tva_exigible = 1 WHERE id = ?", janvier.id()), "compta_tva_figee");
        assertEquals("COMPTA_TVA_FIGEE", assertThrows(CommonException.class, () -> tva.calculer(janvier.id())).getErrorCode());

        commercial("UPDATE paiements SET annule = TRUE WHERE id = ?", p);
        montant("200.00", tva.lire(janvier.id()).tvaExigible());
        assertEquals(1, tva.encaissements(janvier.id()).size(), "la photo ne bouge pas");

        TvaDeclarationDTO fevrier = ouvrir(2026, 2);
        List<TvaEncaissementDTO> ops = tva.encaissements(fevrier.id());
        assertEquals(1, ops.size());
        assertEquals("REGULARISATION", ops.get(0).sourceType());
        montant("-200.00", ops.get(0).tva());
        assertPresent(fevrier, "T32");

        // Le recalcul ne la double pas, et une période suivante ne la reprend pas une deuxième fois
        TvaDeclarationDTO recalcul = tva.calculer(fevrier.id());
        assertEquals(1, tva.encaissements(recalcul.id()).size());
        valider(fevrier.id());
        assertTrue(tva.encaissements(ouvrir(2026, 3).id()).isEmpty());
    }

    @Test
    void unRejetApresDeclarationProposeUneRegularisation() {
        Long f = facture("2026-01-05", "1000.00", "200.00", "0", "20");
        encaissementRef("PAY-R", "2026-01-08", "CHEQUE", "1200.00", f);
        Long e = effet("CHQ-R", "CHEQUE", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE", "1200.00", "2026-01-12", "PAY-R", client, null);
        TvaDeclarationDTO janvier = ouvrir(2026, 1);
        assertEquals(1, tva.encaissements(janvier.id()).size());
        assertEquals(LocalDate.parse("2026-01-12"), tva.encaissements(janvier.id()).get(0).date(), "la date de l'encaissement effectif");
        valider(janvier.id());

        commercial("UPDATE cheques_effets SET statut = 'IMPAYE_REJETE' WHERE id = ?", e);

        TvaDeclarationDTO fevrier = ouvrir(2026, 2);
        assertEquals("REGULARISATION", tva.encaissements(fevrier.id()).get(0).sourceType());
        montant("-200.00", fevrier.tvaExigible());
    }

    @Test
    void t29ConfronteLesOperationsAuxComptes() {
        Long f = facture("2026-01-05", "1000.00", "200.00", "0", "20");
        encaissement("2026-01-10", "ESPECES", "1200.00", f);

        TvaDeclarationDTO sansPiece = ouvrir(2026, 1);
        assertPresent(sansPiece, "T29");

        passer("FACTURE", f);
        assertAucun(tva.calculer(sansPiece.id()), "T29");
    }

    // --- Ce qui entre, et ce qui n'entre pas

    @Test
    void ceQuiNEstPasUnEncaissementNEntreJamais() {
        Long f = facture("2026-01-05", "1000.00", "200.00", "0", "20");
        encaissementRef("PAY-P", "2026-01-08", "CHEQUE", "1200.00", f);
        effet("CHQ-P", "CHEQUE", "ENCAISSEMENT_CLIENT", "EN_PORTEFEUILLE", "1200.00", null, "PAY-P", client, null);
        encaissement("2026-01-08", "CREDIT", "1200.00", f);
        Long annule = encaissement("2026-01-09", "ESPECES", "1200.00", f);
        commercial("UPDATE paiements SET annule = TRUE WHERE id = ?", annule);
        facture("2026-01-06", "500.00", "100.00", "0", "20");

        TvaDeclarationDTO d = ouvrir(2026, 1);

        assertTrue(tva.encaissements(d.id()).isEmpty(), "portefeuille, crédit, annulation et facture impayée restent dehors");
        montant("0.00", d.tvaExigible());
    }

    @Test
    void unPaiementPartielEnDeuxTranchesFaitDeuxLignesEtLaTvaDeLaFacture() {
        Long fa = factureAchat("2026-01-05", "83.33", "16.67");
        reglement("2026-01-10", "VIREMENT", "50.00", fa);
        reglement("2026-01-20", "VIREMENT", "50.00", fa);

        TvaDeclarationDTO d = ouvrir(2026, 1);

        List<TvaDeductionDTO> o = tva.deductions(d.id());
        assertEquals(2, o.size());
        assertEquals(1, o.get(0).ordre());
        assertEquals(2, o.get(1).ordre());
        montant("16.67", d.tvaDeductible());
    }

    @Test
    void unChequeSurTroisFacturesFaitTroisLignesSansEcart() {
        Long a = factureAchat("2026-01-05", "100.00", "20.00");
        Long b = factureAchat("2026-01-06", "200.00", "40.00");
        Long c = factureAchat("2026-01-07", "300.00", "60.00");
        Long r = reglementRef("RF-3F", "2026-01-10", "CHEQUE", "720.00", null);
        effet("CHQ-3", "CHEQUE", "DECAISSEMENT_FOURNISSEUR", "ENCAISSE_COMPTABILISE", "720.00", "2026-01-12", "RF-3F", null, fournisseur);
        for (Object[] x : new Object[][]{{a, "120.00"}, {b, "240.00"}, {c, "360.00"}}) {
            jdbc.update("INSERT INTO compta_reglement_affectations (livre_id, reglement_id, facture_achat_id, montant, saisi_par) VALUES (?, ?, ?, ?, 'essai')", livre.livreId(), r, x[0], new BigDecimal((String) x[1]));
        }

        TvaDeclarationDTO d = ouvrir(2026, 1);

        assertEquals(3, tva.deductions(d.id()).size());
        montant("120.00", d.tvaDeductible());
        assertAucun(d, "T21");
    }

    @Test
    void lesDeuxPointsDeVenteDUnLivreEntrentDansUneSeuleDeclaration() {
        SocieteDeTest autre = SocieteDeTest.creer();
        livreRepository.rattacher(livre.livreId(), autre.pointDeVenteId(), DU);
        Long clientAutre = inserer("INSERT INTO clients (nom, nom_complet, pays, point_de_vente_id) VALUES ('Autre', 'Autre', 'Maroc', ?) RETURNING id", autre.pointDeVenteId());
        Long f1 = facture("2026-01-05", "100.00", "20.00", "0", "20");
        encaissement("2026-01-10", "ESPECES", "120.00", f1);
        Long f2 = inserer("INSERT INTO factures (numero_facture, date_facture, client_id, emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, annulee, point_de_vente_id) VALUES (?, '2026-01-05', ?, 1, 100, 20, 120, 0, 120, 'VALIDEE', FALSE, ?) RETURNING id",
                "FV-A-" + autre.pointDeVenteId(), clientAutre, autre.pointDeVenteId());
        commercial("INSERT INTO lignes_facture (facture_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, 100, 20, 120)", f2, produit);
        inserer("INSERT INTO paiements (numero_paiement, date_paiement, mode_paiement, encaisse_par_user_id, montant, client_id, facture_id, annule, point_de_vente_id) VALUES (?, '2026-01-11', 'ESPECES', 1, 120, ?, ?, FALSE, ?) RETURNING id",
                "PAY-A-" + autre.pointDeVenteId(), clientAutre, f2, autre.pointDeVenteId());

        TvaDeclarationDTO d = ouvrir(2026, 1);

        assertEquals(2, tva.encaissements(d.id()).size());
        montant("40.00", d.tvaExigible());
    }

    @Test
    void unAutreLivreNeVoitRienDeCeLivre() {
        reglement("2026-01-10", "VIREMENT", "120.00", factureAchat("2026-01-05", "100.00", "20.00"));
        TvaDeclarationDTO d = ouvrir(2026, 1);

        SocieteDeTest autre = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(autre.pointDeVenteId());
        abonnements.modifier(autre.pointDeVenteId(), true);
        livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Autre SARL", "SARL", "99900002", "999000000000002", null, null, null, "Rabat", "Négoce", null, null, "GENERAL")));

        assertThrows(ResourceNotFoundException.class, () -> tva.lire(d.id()));
        assertThrows(ResourceNotFoundException.class, () -> tva.deductions(d.id()));
    }

    @Test
    void auRegimeDesDebitsLaFactureSeTaxeASaDateMoinsLesAcomptesDejaTaxes() {
        parametres(2026, "MENSUELLE", "DEBIT", "100", "0", 1);
        Long sansPaiement = facture("2026-01-05", "1000.00", "200.00", "0", "20");
        assertNotNull(sansPaiement);
        Long fevrier = facture("2026-02-05", "500.00", "100.00", "0", "20");
        encaissement("2026-01-20", "ESPECES", "120.00", fevrier);

        TvaDeclarationDTO janvier = ouvrir(2026, 1);
        montant("220.00", janvier.tvaExigible());
        valider(janvier.id());
        TvaDeclarationDTO suivante = ouvrir(2026, 2);
        montant("80.00", suivante.tvaExigible());
    }

    // --- Contrôles

    @Test
    void uneLigneAZeroSansTraitementBloqueEtSonTraitementLaLeve() {
        Long f = facture("2026-01-05", "1000.00", "0", "0", "0");
        encaissement("2026-01-10", "ESPECES", "1000.00", f);

        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertPresent(d, "T13");
        assertTrue(d.controles().bloquants() > 0);
        assertEquals("COMPTA_TVA_BLOQUANTS", assertThrows(CommonException.class, () -> tva.valider(d.id())).getErrorCode());

        TvaEncaissementDTO o = tva.encaissements(d.id()).get(0);
        TvaDeclarationDTO corrigee = tva.modifierOperation("COLLECTEE", o.id(), new TvaOperationRequest("Exonérée sans droit à déduction", null, null, "EXONERE_91", null, null, null, null, null));

        assertAucun(corrigee, "T13");
        assertAucun(corrigee, "T11");
        montant("1000.00", ligne(corrigee, "030").baseRetenue());
        assertEquals("Exonérée sans droit à déduction", tva.encaissements(d.id()).get(0).motifSurcharge(), "la correction survit au recalcul");
    }

    @Test
    void unAcompteSansFactureAttendSonTaux() {
        Long p = encaissement("2026-01-10", "ESPECES", "120.00", null);
        assertNotNull(p);

        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertPresent(d, "T14");
        Long op = tva.encaissements(d.id()).get(0).id();

        TvaDeclarationDTO corrigee = tva.modifierOperation("COLLECTEE", op, new TvaOperationRequest("Acompte sur commande à 20 %", null, null, null, new BigDecimal("20"), null, null, null, null));

        assertAucun(corrigee, "T14");
        montant("20.00", corrigee.tvaExigible());
        montant("20.00", ligne(corrigee, "102").taxeRetenue());
    }

    @Test
    void unProrataDeQuatreVingtPourCentSeReporteEtSaPartSortDuCompte() {
        parametres(2026, "MENSUELLE", "ENCAISSEMENT", "80", "0", 1);
        Long fa = factureAchat("2026-01-05", "1000.00", "200.00");
        passer("FACTURE_ACHAT", fa);
        reglement("2026-01-10", "VIREMENT", "1200.00", fa);

        TvaDeclarationDTO d = ouvrir(2026, 1);

        TvaDeductionDTO o = tva.deductions(d.id()).get(0);
        montant("200.00", o.tva());
        montant("160.00", o.tvaDeductible());
        montant("160.00", d.tvaDeductible());
        montant("200.00", ligne(d, "146").baseRetenue());
        montant("160.00", ligne(d, "146").taxeRetenue());
        TvaDeclarationDTO deposee = deposer(d.id(), "2026-01-20");
        ligneEcriture(deposee.ecritureLiquidationId(), "61678", "40.00", "0");
        solde("34552", "0");
    }

    @Test
    void lesEspecesAuDelaDesSeuilsBloquentSaufExceptionMotivee() {
        Long fa = factureAchat("2026-01-05", "5000.00", "1000.00");
        reglement("2026-01-10", "ESPECES", "6000.00", fa);

        TvaDeclarationDTO d = ouvrir(2026, 1);

        TvaControleDTO t18 = tva.listeControles(d.id()).stream().filter(c -> "T18".equals(c.code())).findFirst().orElseThrow(() -> new AssertionError("T18 attendu"));
        assertEquals("BLOQUANT", t18.gravite());
        assertEquals("COMPTA_TVA_BLOQUANTS", assertThrows(CommonException.class, () -> tva.valider(d.id())).getErrorCode());
        tva.accepter(t18.id(), new TvaAccepterRequest("Achat de produits agricoles, exception de l'article"));
        assertEquals("ACCEPTE", tva.listeControles(d.id()).stream().filter(c -> "T18".equals(c.code())).findFirst().orElseThrow().etat());
        valider(d.id());
    }

    @Test
    void unBloquantNeSAcceptePas() {
        commercial("UPDATE fournisseurs SET ice = NULL, numero_identification_fiscale = NULL WHERE id = ?", fournisseur);
        reglement("2026-01-10", "VIREMENT", "120.00", factureAchat("2026-01-05", "100.00", "20.00"));

        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertPresent(d, "T05");
        assertPresent(d, "T07");
        TvaControleDTO t05 = tva.listeControles(d.id()).stream().filter(c -> "T05".equals(c.code())).findFirst().orElseThrow();
        assertEquals("COMPTA_TVA_CONTROLE", assertThrows(CommonException.class, () -> tva.accepter(t05.id(), new TvaAccepterRequest("Non"))).getErrorCode());

        Long op = tva.deductions(d.id()).get(0).id();
        TvaDeclarationDTO corrigee = tva.modifierOperation("DEDUCTIBLE", op, new TvaOperationRequest("Identité lue sur la facture", null, null, null, null, null, "12345678", ICE, null));

        assertAucun(corrigee, "T05");
        assertAucun(corrigee, "T07");
    }

    @Test
    void unMotifDonneSurUneAlerteSurvitAuRecalcul() {
        reglement("2026-01-10", "VIREMENT", "120.00", factureAchat("2026-01-05", "100.00", "20.00"));
        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertPresent(d, "T20");

        tva.accepterCode(d.id(), new TvaAccepterCodeRequest("T20", "Numéros vérifiés sur les factures"));
        tva.calculer(d.id());

        TvaControleDTO t20 = tva.listeControles(d.id()).stream().filter(c -> "T20".equals(c.code())).findFirst().orElseThrow();
        assertEquals("ACCEPTE", t20.etat());
        assertEquals("Numéros vérifiés sur les factures", t20.motif());
    }

    @Test
    void lePointageResteNonEffectueJamaisReussi() {
        TvaDeclarationDTO d = ouvrir(2026, 1);

        for (String code : List.of("T22", "T23", "T24")) {
            assertEquals("NON_EFFECTUE", tva.listeControles(d.id()).stream().filter(c -> code.equals(c.code())).findFirst().orElseThrow(() -> new AssertionError(code)).gravite());
        }
        assertTrue(d.controles().nonEffectues() >= 3);
    }

    @Test
    void unIdentifiantFiscalAbsentDuDossierBloque() {
        jdbc.update("UPDATE compta_livres SET identifiant_fiscal = 'ABC' WHERE id = ?", livre.livreId());

        assertPresent(ouvrir(2026, 1), "T01");
    }

    @Test
    void uneIdentiteDifferenteDeCelleDuPointDeVenteBloque() {
        commercial("INSERT INTO entreprise_profiles (point_de_vente_id, nom_entreprise, numero_identification_fiscale, ice) VALUES (?, 'Atlas', '11111111', '999000000000001')", pdv);

        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertPresent(d, "T31");

        TvaDeclarationDTO corrigee = tva.corrigerIdentite(d.id(), new TvaIdentiteRequest("11111111", "999000000000001", "Lu sur le profil"));

        assertAucun(corrigee, "T31");
        assertEquals("11111111", corrigee.identifiantFiscal());
    }

    // --- Cycle de vie et rattrapage

    @Test
    void unPaiementOublieSeRattrapeUneSeuleFois() {
        Long fa = factureAchat("2026-01-05", "100.00", "20.00");
        Long r = reglement("2026-01-10", "VIREMENT", "120.00", fa);

        TvaDeclarationDTO fevrier = ouvrir(2026, 2);
        List<TvaRattrapageDTO> liste = tva.aRattraper(fevrier.id());
        assertEquals(1, liste.size());
        assertEquals(r, liste.get(0).sourceId());
        assertFalse(liste.get(0).plusDeDouzeMois());

        TvaDeclarationDTO apres = tva.rattraper(fevrier.id(), new TvaRattraperRequest(List.of(new TvaPieceVisee("REGLEMENT_FOURNISSEUR", r))));

        montant("20.00", apres.tvaDeductible());
        assertPresent(apres, "T15");
        assertTrue(tva.aRattraper(fevrier.id()).isEmpty());
        montant("20.00", tva.calculer(fevrier.id()).tvaDeductible());
    }

    @Test
    void leCycleSuitSesEtapesEtRefuseLesRaccourcis() {
        reglement("2026-01-10", "VIREMENT", "120.00", factureAchat("2026-01-05", "100.00", "20.00"));
        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertEquals("BROUILLON", d.statut());
        assertEquals(LocalDate.parse("2026-02-28"), d.dateLimite());
        assertEquals("COMPTA_TVA_ALERTES", assertThrows(CommonException.class, () -> tva.valider(d.id())).getErrorCode());
        assertEquals("COMPTA_TVA_FICHIER_ABSENT", assertThrows(CommonException.class, () -> tva.fichier(d.id(), "RD")).getErrorCode());

        accepterAlertes(d.id());
        TvaDeclarationDTO validee = tva.valider(d.id());
        assertEquals("VALIDEE", validee.statut());
        assertNotNull(validee.valideePar());
        assertEquals("RD_99900001_2026_01_v1.zip", tva.fichier(d.id(), "RD")[0]);
        assertEquals("COMPTA_TVA_DEPOT", assertThrows(CommonException.class, () -> tva.depot(d.id(), new TvaDepotRequest(LocalDate.parse("2026-01-20"), null))).getErrorCode(), "le relevé doit passer par la DGI");

        assertEquals("BROUILLON", tva.devalider(d.id()).statut());
        assertNull(tva.lire(d.id()).valideLe());
        tva.valider(d.id());

        tva.etatFichier(d.id(), "RD", new TvaEtatFichierRequest("REJETE", "Montants"));
        assertEquals("EDI_REJETE", tva.lire(d.id()).statut());
        assertEquals("COMPTA_TVA_DEPOT", assertThrows(CommonException.class, () -> tva.depot(d.id(), new TvaDepotRequest(LocalDate.parse("2026-01-20"), null))).getErrorCode());
        tva.etatFichier(d.id(), "RD", new TvaEtatFichierRequest("TRAITE", null));
        TvaDeclarationDTO deposee = tva.depot(d.id(), new TvaDepotRequest(LocalDate.parse("2026-01-20"), "R-55102"));
        assertEquals("DEPOSEE", deposee.statut());
        assertEquals("R-55102", deposee.numeroRecu());
        assertEquals("COMPTA_TVA_FIGEE", assertThrows(CommonException.class, () -> tva.devalider(d.id())).getErrorCode());
        assertEquals("COMPTA_TVA_PAIEMENT", assertThrows(CommonException.class, () -> tva.paiement(d.id(), new TvaPaiementRequest(LocalDate.parse("2026-01-25"), "VIREMENT", null))).getErrorCode(), "période créditrice");
    }

    @Test
    void lesPeriodesSansDeclarationSeVoientEtLesParametresTiennent() {
        List<TvaPeriodeDTO> periodes = tva.periodes(2026);
        assertEquals("A_CREER", periodes.get(0).statut());
        assertNull(periodes.get(0).id());
        assertTrue(periodes.get(0).enRetard());

        TvaDeclarationDTO d = ouvrir(2026, 1);
        assertEquals(d.id(), tva.periodes(2026).get(0).id());
        assertEquals("COMPTA_TVA_PERIODE_EXISTE", assertThrows(CommonException.class, () -> tva.creer(new TvaCreationRequest(2026, 1))).getErrorCode());
        assertEquals("COMPTA_TVA_PARAMETRE", assertThrows(CommonException.class, () -> parametres(2026, "TRIMESTRIELLE", "ENCAISSEMENT", "100", "0", 1)).getErrorCode(), "plus de changement une fois une déclaration ouverte");
        assertEquals("COMPTA_TVA_PARAMETRES_ABSENTS", assertThrows(CommonException.class, () -> tva.creer(new TvaCreationRequest(2031, 1))).getErrorCode());
        assertEquals("COMPTA_TVA_PERIODE", assertThrows(CommonException.class, () -> tva.creer(new TvaCreationRequest(2026, 13))).getErrorCode());
    }

    @Test
    void uneLigneSaisieEntreDansLeCalculEtSeRetire() {
        TvaDeclarationDTO d = ouvrir(2026, 1);

        TvaDeclarationDTO saisie = tva.corrigerLigne(d.id(), new TvaCorrectionLigneRequest("120", null, new BigDecimal("50.00"), "Reversement"));
        montant("50.00", saisie.tvaDue());
        assertEquals("COMPTA_TVA_LIGNE", assertThrows(CommonException.class, () -> tva.corrigerLigne(d.id(), new TvaCorrectionLigneRequest("200", null, BigDecimal.ONE, "Non"))).getErrorCode(), "une formule ne se saisit pas");

        montant("50.00", tva.calculer(d.id()).tvaDue());
        montant("0.00", tva.corrigerLigne(d.id(), new TvaCorrectionLigneRequest("120", null, null, "Retrait")).tvaDue());
    }

    @Test
    void lesExportsSeProduisent() {
        reglement("2026-01-10", "VIREMENT", "120.00", factureAchat("2026-01-05", "100.00", "20.00"));
        TvaDeclarationDTO d = ouvrir(2026, 1);

        assertTrue(tva.fiche(d.id()).length > 500);
        assertTrue(tva.releveXlsx(d.id()).length > 500);
        assertTrue(tva.couverture(d.id()).length > 500);
        assertTrue(tva.detail(d.id()).length > 500);
    }

    // --- Outils

    private void parametres(int annee, String periodicite, String fait, String prorata, String credit, Integer premiere) {
        tva.enregistrerParametres(annee, new TvaParametresRequest(true, fait, periodicite, new BigDecimal(prorata), false, premiere, new BigDecimal(credit)));
    }

    private TvaDeclarationDTO ouvrir(int annee, int numeroPeriode) {
        return tva.creer(new TvaCreationRequest(annee, numeroPeriode));
    }

    private void accepterAlertes(Long id) {
        tva.listeControles(id).stream().filter(c -> c.gravite().startsWith("ALERTE") && "OUVERT".equals(c.etat())).map(TvaControleDTO::code).distinct()
                .forEach(code -> tva.accepterCode(id, new TvaAccepterCodeRequest(code, "Vu pour l'essai")));
    }

    private TvaDeclarationDTO valider(Long id) {
        accepterAlertes(id);
        return tva.valider(id);
    }

    private TvaDeclarationDTO deposer(Long id, String date) {
        TvaDeclarationDTO d = valider(id);
        if (!tva.deductions(id).isEmpty()) {
            tva.etatFichier(id, "RD", new TvaEtatFichierRequest("TRAITE", null));
        }
        assertEquals(d.id(), id);
        return tva.depot(id, new TvaDepotRequest(LocalDate.parse(date), null));
    }

    private TvaLigneDTO ligne(TvaDeclarationDTO d, String code) {
        return d.lignes().stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError("ligne " + code + " absente de " + d.lignes().stream().map(TvaLigneDTO::code).toList()));
    }

    private static void montant(String attendu, BigDecimal valeur) {
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    private void assertPresent(TvaDeclarationDTO d, String code) {
        assertTrue(tva.listeControles(d.id()).stream().anyMatch(c -> code.equals(c.code())), () -> code + " attendu parmi " + tva.listeControles(d.id()));
    }

    private void assertAucun(TvaDeclarationDTO d, String code) {
        assertFalse(tva.listeControles(d.id()).stream().anyMatch(c -> code.equals(c.code())), () -> code + " inattendu parmi " + tva.listeControles(d.id()));
    }

    private void solde(String compte, String attendu) {
        BigDecimal solde = jdbc.queryForObject("SELECT COALESCE(SUM(debit - credit), 0) FROM compta_lignes WHERE livre_id = ? AND compte_numero = ?", BigDecimal.class, livre.livreId(), compte);
        assertEquals(0, new BigDecimal(attendu).compareTo(solde), "compte " + compte + " : " + solde + " au lieu de " + attendu);
    }

    private void ligneEcriture(Long ecriture, String compte, String debit, String credit) {
        BigDecimal d = jdbc.queryForObject("SELECT COALESCE(SUM(debit), 0) FROM compta_lignes WHERE livre_id = ? AND ecriture_id = ? AND compte_numero = ?", BigDecimal.class, livre.livreId(), ecriture, compte);
        BigDecimal c = jdbc.queryForObject("SELECT COALESCE(SUM(credit), 0) FROM compta_lignes WHERE livre_id = ? AND ecriture_id = ? AND compte_numero = ?", BigDecimal.class, livre.livreId(), ecriture, compte);
        assertEquals(0, new BigDecimal(debit).compareTo(d), compte + " au débit : " + d);
        assertEquals(0, new BigDecimal(credit).compareTo(c), compte + " au crédit : " + c);
    }

    private void passer(String type, Long id) {
        ResultatPieceDTO r = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee(type, id, "COMPTABILISATION", null, null)))).resultats().get(0);
        assertTrue(r.reussi(), r::message);
    }

    private void commercial(String sql, Object... parametres) {
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            jdbc.execute("SET LOCAL session_replication_role = replica");
            jdbc.update(sql, parametres);
        });
    }

    private Long inserer(String sql, Object... parametres) {
        return new TransactionTemplate(transactions).execute(s -> {
            jdbc.execute("SET LOCAL session_replication_role = replica");
            return jdbc.queryForObject(sql, Long.class, parametres);
        });
    }

    private Long facture(String date, String ht, String tva, String remise, String taux) {
        BigDecimal ttc = new BigDecimal(ht).add(new BigDecimal(tva));
        BigDecimal finale = ttc.subtract(new BigDecimal(remise));
        Long id = inserer("INSERT INTO factures (numero_facture, date_facture, client_id, emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, annulee, point_de_vente_id) VALUES (?, ?::date, ?, 1, ?, ?, ?, ?, ?, 'VALIDEE', FALSE, ?) RETURNING id",
                "FV-" + (numero++) + "-" + pdv, date, client, new BigDecimal(ht), new BigDecimal(tva), ttc, new BigDecimal(remise), finale, pdv);
        commercial("INSERT INTO lignes_facture (facture_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, ?, ?, ?, ?)", id, produit, new BigDecimal(taux), new BigDecimal(ht), new BigDecimal(tva), ttc);
        return id;
    }

    private Long factureAchat(String date, String ht, String tva) {
        BigDecimal ttc = new BigDecimal(ht).add(new BigDecimal(tva));
        Long id = inserer("INSERT INTO factures_achat (numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) VALUES (?, ?::timestamp, ?, ?, ?, ?, 'EN_ATTENTE', ?) RETURNING id",
                "FA-" + (numero++) + "-" + pdv, date, fournisseur, new BigDecimal(ht), new BigDecimal(tva), ttc, pdv);
        commercial("INSERT INTO lignes_facture_achat (facture_achat_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, ?, ?, ?)", id, produit, new BigDecimal(ht), new BigDecimal(tva), ttc);
        return id;
    }

    private void ventilationImmobilisation(Long factureAchat) {
        jdbc.update("INSERT INTO compta_ventilations (livre_id, source_type, source_id, numero, compte_numero, montant_ht, taux_tva, montant_tva, tva_deductible, est_immobilisation, numero_facture_fournisseur, saisie_par) VALUES (?, 'FACTURE_ACHAT', ?, 1, '6111', 50, 20, 10, TRUE, TRUE, 'F-IMMO-1', 'essai')",
                livre.livreId(), factureAchat);
    }

    private Long encaissement(String date, String mode, String montant, Long facture) {
        return encaissementRef("PAY-" + (numero++), date, mode, montant, facture);
    }

    private Long encaissementRef(String reference, String date, String mode, String montant, Long facture) {
        return inserer("INSERT INTO paiements (numero_paiement, date_paiement, mode_paiement, encaisse_par_user_id, montant, client_id, facture_id, annule, point_de_vente_id) VALUES (?, ?::timestamp, ?, 1, ?, ?, ?, FALSE, ?) RETURNING id",
                reference + "-" + pdv, date, mode, new BigDecimal(montant), client, facture, pdv);
    }

    private Long reglement(String date, String mode, String montant, Long factureAchat) {
        return reglementRef("RF-" + (numero++), date, mode, montant, factureAchat);
    }

    private Long reglementRef(String reference, String date, String mode, String montant, Long factureAchat) {
        return inserer("INSERT INTO reglements_fournisseur (numero_reglement, date_reglement, montant, mode_paiement, reference_paiement, type_reglement, facture_achat_id, point_de_vente_id) VALUES (?, ?::timestamp, ?, ?, ?, 'FACTURE', ?, ?) RETURNING id",
                reference + "-" + pdv, date, new BigDecimal(montant), mode, "REF-" + reference, factureAchat, pdv);
    }

    private Long effet(String numeroPiece, String type, String sens, String statut, String montant, String encaissement, String reference, Long clientId, Long fournisseurId) {
        return inserer("INSERT INTO cheques_effets (numero_piece, type_effet, sens, statut, montant, date_encaissement, reference_paiement, client_id, fournisseur_id, point_de_vente_id) VALUES (?, ?, ?, ?, ?, ?::date, ?, ?, ?, ?) RETURNING id",
                numeroPiece + "-" + pdv, type, sens, statut, new BigDecimal(montant), encaissement, reference + "-" + pdv, clientId, fournisseurId, pdv);
    }
}
