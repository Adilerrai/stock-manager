package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.AnnulationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneInventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.SortieRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComptabiliserRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ClotureService;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.ImmobilisationComptaService;
import com.gestion.service.compta.InventaireClotureService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.MoteurComptabilisation;
import com.gestion.service.compta.PeriodeService;
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

@SpringBootTest
class ImmobilisationComptaIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private ImmobilisationComptaService immobilisations;
    @Autowired private InventaireClotureService inventaires;
    @Autowired private ClotureService cloture;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private MoteurComptabilisation moteur;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private LivreDTO livre;
    private Long pdv;
    private Long exercice;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        pdv = societe.pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        abonnements.modifier(pdv, true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        exercice = livre.exerciceCourantId();
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    // --- Acquisition

    @Test
    void uneFicheNaitDUneEcritureDAcquisition() {
        ImmobilisationDTO f = chariot("2026-02-10", "2026-02-15", "100000.00", "20000.00", "0");

        assertEquals("IMM-0001", f.code());
        assertEquals("28332", f.compteAmort());
        assertEquals("61933", f.compteDotation());
        assertNotNull(f.ecritureAcquisitionId());
        solde("23321", "100000.00");
        solde("34551", "20000.00");
        solde("5141", "-120000.00");
        assertEquals("ACQUISITION", jdbc.queryForObject("SELECT nature_mouvement FROM compta_lignes WHERE livre_id = ? AND compte_numero = '23321'", String.class, livre.livreId()));
    }

    @Test
    void uneFicheSansEcritureEstImpossibleEnBase() {
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO compta_immobilisations (livre_id, code, designation, compte_immo, date_acquisition, valeur_brute, mode_amortissement, cree_par)
                VALUES (?, 'IMM-X', 'Sans écriture', '23321', '2026-01-01', 100, 'NON_AMORTISSABLE', 'essai')
                """, livre.livreId()), "ecriture_acquisition_id est obligatoire");
    }

    @Test
    void lesFichesIncoherentesSontRefusees() {
        assertRefus("COMPTA_IMMO_COMPTE", () -> immobilisations.creer(requete("6111", "2026-01-10", "2026-01-10", "1000.00", "LINEAIRE", 60, "0", "BANQUE", null)));
        assertRefus("COMPTA_IMMO_DUREE", () -> immobilisations.creer(requete("23321", "2026-01-10", "2026-01-10", "1000.00", "DEGRESSIF", 12, "0", "BANQUE", null)));
        assertRefus("COMPTA_IMMO_MODE", () -> immobilisations.creer(requete("23211", "2026-01-10", "2026-01-10", "1000.00", "DEGRESSIF", 60, "0", "BANQUE", null)));
        assertRefus("COMPTA_IMMO_DUREE", () -> immobilisations.creer(requete("23321", "2026-01-10", "2026-01-10", "1000.00", "LINEAIRE", null, "0", "BANQUE", null)));
        assertRefus("COMPTA_IMMO_VALEUR", () -> immobilisations.creer(requete("23321", "2026-01-10", "2026-01-10", "1000.00", "LINEAIRE", 60, "2000.00", "BANQUE", null)));
        assertRefus("COMPTA_IMMO_CONTREPARTIE", () -> immobilisations.creer(requete("23321", "2026-01-10", "2026-01-10", "1000.00", "LINEAIRE", 60, "0", "FOURNISSEUR", null)));
        assertEquals(0, compter("compta_immobilisations WHERE livre_id = " + livre.livreId()));
    }

    @Test
    void uneEcritureDejaAuJournalPeutPorterUneFiche() {
        ImmobilisationDTO premiere = chariot("2026-01-10", "2026-01-10", "1000.00", "0", "0");
        ImmobilisationRequest trop = new ImmobilisationRequest("Double", "23321", null, null, LocalDate.parse("2026-01-10"), null, new BigDecimal("2000.00"), null, null, "LINEAIRE", 60, null, null, premiere.ecritureAcquisitionId(), null, null, null, null);
        assertRefus("COMPTA_IMMO_ECRITURE", () -> immobilisations.creer(trop));

        ImmobilisationRequest juste = new ImmobilisationRequest("Reprise", "23321", null, null, LocalDate.parse("2026-01-10"), null, new BigDecimal("1000.00"), null, null, "LINEAIRE", 60, null, null, premiere.ecritureAcquisitionId(), null, null, null, null);
        assertEquals(premiere.ecritureAcquisitionId(), immobilisations.creer(juste).ecritureAcquisitionId());
        solde("23321", "1000.00");
    }

    @Test
    void leDegressifReprendSonCoefficientDatable() {
        ImmobilisationDTO f = immobilisations.creer(requete("23321", "2026-01-10", "2026-01-10", "1000.00", "DEGRESSIF", 60, "0", "BANQUE", null));

        assertEquals(0, new BigDecimal("2").compareTo(f.coefficientDegressif()));
    }

    @Test
    void lePlanPrevisionnelNEcritRien() {
        ImmobilisationDTO f = chariot("2026-01-10", "2026-01-01", "1000.00", "0", "0");
        long avant = compter("compta_ecritures WHERE livre_id = " + livre.livreId());

        ImmobilisationDTO relue = immobilisations.lire(f.id());

        assertFalse(relue.planPrevisionnel().isEmpty());
        assertEquals(avant, compter("compta_ecritures WHERE livre_id = " + livre.livreId()));
        assertEquals(0, compter("compta_amortissements WHERE livre_id = " + livre.livreId()));
        montant("0", relue.cumulAmortissements());
        solde("61933", "0");
    }

    // --- S11

    @Test
    void s11UneCessionPasseLeComplementLaSortieEtLePrix() {
        ouvrirJusqua(6);
        ImmobilisationDTO f = chariotReprise();

        ImmobilisationDTO sortie = immobilisations.sortir(f.id(), new SortieRequest("CESSION", LocalDate.parse("2026-06-30"), new BigDecimal("70000.00"), new BigDecimal("14000.00"), null));

        assertEquals("SORTIE", sortie.statut());
        var s = sortie.sorties().get(0);
        montant("50000.00", s.amortissementsCumules());
        montant("50000.00", s.valeurNette());
        montant("20000.00", s.plusOuMoinsValue());
        montant("70000.00", s.prixCessionHt());
        montant("50000.00", sortie.cumulAmortissements());
        solde("61933", "10000.00");
        solde("6513", "50000.00");
        solde("7513", "-70000.00");
        solde("4455", "-14000.00");
        solde("3481", "84000.00");
        solde("23321", "0");
        // Plus-value lue au grand livre : 70 000 - 50 000
        BigDecimal auGrandLivre = jdbc.queryForObject("SELECT SUM(credit - debit) FROM compta_lignes WHERE livre_id = ? AND compte_numero IN ('7513', '6513')", BigDecimal.class, livre.livreId());
        montant(s.plusOuMoinsValue().toPlainString(), auGrandLivre);
        assertEquals("CESSION", jdbc.queryForObject("SELECT nature_mouvement FROM compta_lignes WHERE livre_id = ? AND compte_numero = '23321' AND credit > 0", String.class, livre.livreId()));
    }

    @Test
    void s11UneDeuxiemeCessionEtUneDotationApresSortieSontRefusees() {
        ouvrirJusqua(6);
        ImmobilisationDTO f = chariotReprise();
        SortieRequest cession = new SortieRequest("CESSION", LocalDate.parse("2026-06-30"), new BigDecimal("70000.00"), BigDecimal.ZERO, null);
        ImmobilisationDTO sortie = immobilisations.sortir(f.id(), cession);

        assertRefus("COMPTA_IMMO_DEJA_SORTIE", () -> immobilisations.sortir(f.id(), cession));
        // La base refuse aussi, sans passer par le service
        Long ecriture = sortie.sorties().get(0).ecritureSortieId();
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO compta_immobilisation_sorties (immobilisation_id, livre_id, nature, date_sortie, amortissements_cumules, valeur_nette, plus_ou_moins_value, ecriture_sortie_id, cree_par)
                VALUES (?, ?, 'RETRAIT', '2026-06-30', 0, 0, 0, ?, 'essai')
                """, f.id(), livre.livreId(), ecriture), "uq_compta_sor_active");
        DataAccessException refus = assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO compta_amortissements (livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) VALUES (?, ?, ?, 'ANNUELLE', '2026-01-01', '2026-12-31', 20000, ?)
                """, livre.livreId(), f.id(), exercice, ecriture));
        assertTrue(refus.getMostSpecificCause().getMessage().contains("compta_immo_sortie"), refus::getMessage);
        assertEquals("SORTIE", immobilisations.lire(f.id()).statut());
        montant("50000.00", immobilisations.lire(f.id()).cumulAmortissements());
        assertTrue(immobilisations.dotations(exercice, new DotationsRequest(true)).lignes().isEmpty(), "s11.g : pas d'annuité pleine après la cession");
    }

    @Test
    void uneDotationAuDelaDuBrutEstRefuseeEnBase() {
        ImmobilisationDTO f = chariot("2026-01-10", "2026-01-01", "1000.00", "0", "0");

        DataAccessException refus = assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO compta_amortissements (livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) VALUES (?, ?, ?, 'ANNUELLE', '2026-01-01', '2026-12-31', 1000.01, ?)
                """, livre.livreId(), f.id(), exercice, f.ecritureAcquisitionId()));
        assertTrue(refus.getMostSpecificCause().getMessage().contains("compta_immo_suramortie"), refus::getMessage);
    }

    @Test
    void uneSortieErroneeSAnnuleParContrePassation() {
        ouvrirJusqua(6);
        ImmobilisationDTO f = chariotReprise();
        immobilisations.sortir(f.id(), new SortieRequest("CESSION", LocalDate.parse("2026-06-30"), new BigDecimal("70000.00"), new BigDecimal("14000.00"), null));

        ImmobilisationDTO annulee = immobilisations.annulerSortie(f.id(), new AnnulationRequest("Prix erroné"));

        assertEquals("EN_SERVICE", annulee.statut());
        assertTrue(annulee.sorties().get(0).annulee());
        assertEquals("Prix erroné", annulee.sorties().get(0).motifAnnulation());
        montant("40000.00", annulee.cumulAmortissements());
        solde("7513", "0");
        solde("6513", "0");
        solde("61933", "0");
        solde("3481", "0");
        solde("23321", "100000.00");
        assertRefus("COMPTA_IMMO_PAS_DE_SORTIE", () -> immobilisations.annulerSortie(f.id(), new AnnulationRequest("Encore")));

        // Elle peut sortir de nouveau
        assertEquals("SORTIE", immobilisations.sortir(f.id(), new SortieRequest("RETRAIT", LocalDate.parse("2026-06-30"), null, null, null)).statut());
        assertEquals("RETRAIT", jdbc.queryForObject("SELECT nature_mouvement FROM compta_lignes WHERE livre_id = ? AND compte_numero = '23321' AND credit > 0 ORDER BY id DESC LIMIT 1", String.class, livre.livreId()));
    }

    @Test
    void lesEntreesDUneSortieSontControlees() {
        ImmobilisationDTO f = chariot("2026-01-10", "2026-01-01", "1000.00", "0", "0");

        assertRefus("COMPTA_IMMO_SORTIE", () -> immobilisations.sortir(f.id(), new SortieRequest("RETRAIT", LocalDate.parse("2026-01-20"), new BigDecimal("1.00"), null, null)));
        assertRefus("COMPTA_IMMO_SORTIE", () -> immobilisations.sortir(f.id(), new SortieRequest("CESSION", LocalDate.parse("2026-01-20"), null, null, null)));
        assertRefus("COMPTA_IMMO_SORTIE", () -> immobilisations.sortir(f.id(), new SortieRequest("VOL", LocalDate.parse("2026-01-20"), null, null, null)));
    }

    // --- Dotations

    @Test
    void lesDotationsSeMontrentPuisPassentEnPeriode99() {
        chariot("2026-01-10", "2026-01-01", "60000.00", "0", "0");
        ImmobilisationDTO bureau = immobilisations.creer(requete("2355", "2026-01-10", "2026-07-01", "12000.00", "LINEAIRE", 24, "0", "BANQUE", null));

        DotationsDTO apercu = immobilisations.dotations(exercice, new DotationsRequest(true));
        assertEquals(2, apercu.lignes().size());
        montant("15000.00", apercu.total());
        assertNull(apercu.ecritureId());
        assertEquals(0, compter("compta_amortissements WHERE livre_id = " + livre.livreId()));

        assertRefus("COMPTA_PERIODE_A_VENIR", () -> immobilisations.dotations(exercice, new DotationsRequest(false)));

        fermerPeriodes(exercice);
        DotationsDTO passees = immobilisations.dotations(exercice, new DotationsRequest(false));

        assertNotNull(passees.ecritureId());
        solde("61933", "12000.00");
        solde("28332", "-12000.00");
        solde("61935", "3000.00");
        solde("28355", "-3000.00");
        assertEquals("INVENTAIRE", jdbc.queryForObject("SELECT nature FROM compta_ecritures WHERE id = ?", String.class, passees.ecritureId()));
        montant("3000.00", immobilisations.lire(bureau.id()).cumulAmortissements());
        assertRefus("COMPTA_DOTATION_RIEN", () -> immobilisations.dotations(exercice, new DotationsRequest(false)));
    }

    @Test
    void desDotationsErroneesSAnnulentEtSeRepassent() {
        chariot("2026-01-10", "2026-01-01", "60000.00", "0", "0");
        fermerPeriodes(exercice);
        immobilisations.dotations(exercice, new DotationsRequest(false));

        DotationsDTO apres = immobilisations.dotations(exercice, new DotationsRequest(true));
        assertTrue(apres.lignes().isEmpty());

        DotationsDTO annulees = immobilisations.annulerDotations(exercice, new AnnulationRequest("Durée erronée"));

        assertEquals(1, annulees.lignes().size(), "de nouveau à passer");
        solde("61933", "0");
        immobilisations.dotations(exercice, new DotationsRequest(false));
        solde("61933", "12000.00");
        assertThrows(ResourceNotFoundException.class, () -> immobilisations.annulerDotations(exercice + 999, new AnnulationRequest("X")));
    }

    // --- Inventaire

    @Test
    void im5LeStockFinalRemonteLeResultatEtPasseAuBilan() {
        fermerPeriodes(exercice);

        inventaires.enregistrer(exercice, new InventaireRequest("PRIX_ACHAT_COURANT", false, List.of(ligne("3111", "Article A", "100", "2000.00"), ligne("3111", "Article B", "50", "2000.00"))));
        InventaireDTO valide = inventaires.valider(exercice);

        assertEquals("VALIDE", valide.statut());
        montant("300000.00", valide.stockFinal());
        assertNull(valide.ecritureInitialId(), "sans stock initial");
        solde("3111", "300000.00");
        solde("6114", "-300000.00");
        assertEquals("INVENTAIRE", jdbc.queryForObject("SELECT nature FROM compta_ecritures WHERE id = ?", String.class, valide.ecritureFinalId()));
        assertEquals(2, compter("compta_inventaire_lignes WHERE livre_id = " + livre.livreId()));
    }

    @Test
    void leStockDOuvertureSeSoldeAuProchainInventaire() {
        // Le stock vient d'achats : sans eux, 6114 serait créditeur et la clôture refuserait une ligne négative.
        Long fa = inserer("INSERT INTO factures_achat (numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) VALUES (?, '2026-01-10', ?, 20000, 4000, 24000, 'EN_ATTENTE', ?) RETURNING id",
                "FA-S-" + pdv, inserer("INSERT INTO fournisseurs (raison_social, pays, point_de_vente_id) VALUES ('Sebou', 'Maroc', ?) RETURNING id", pdv), pdv);
        Long article = inserer("INSERT INTO produits (reference, description, type_produit, point_de_vente_id) VALUES ('P' || nextval('produits_id_seq'), 'Article', 'MARCHANDISE', ?) RETURNING id", pdv);
        commercial("INSERT INTO lignes_facture_achat (facture_achat_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, 20000, 4000, 24000)", fa, article);
        assertTrue(moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("FACTURE_ACHAT", fa, "COMPTABILISATION", null, null)))).resultats().get(0).reussi());
        fermerPeriodes(exercice);
        inventaires.enregistrer(exercice, new InventaireRequest("PRIX_ACHAT_COURANT", false, List.of(ligne("3111", "Article A", "10", "1000.00"))));
        inventaires.valider(exercice);
        cloture.arreter(exercice, new ArreterRequest("Pas d'impôt.", null));
        cloture.cloturer(exercice, new CloturerRequest("2026"));
        ExerciceDTO suivant = exercices.lister().stream().filter(e -> "2027".equals(e.code())).findFirst().orElseThrow();

        InventaireDTO i = inventaires.lire(suivant.id());
        montant("10000.00", i.stockInitial());
        assertEquals("6114", i.ouverture().get(0).compteVariation());
        fermerPeriodes(suivant.id());
        inventaires.enregistrer(suivant.id(), new InventaireRequest("PRIX_ACHAT_COURANT", false, List.of(ligne("3111", "Article A", "4", "1000.00"))));
        InventaireDTO valide = inventaires.valider(suivant.id());

        assertNotNull(valide.ecritureInitialId());
        assertNotNull(valide.ecritureFinalId());
        BigDecimal stock = jdbc.queryForObject("SELECT SUM(debit - credit) FROM compta_lignes WHERE livre_id = ? AND exercice_id = ? AND compte_numero = '3111'", BigDecimal.class, livre.livreId(), suivant.id());
        montant("4000.00", stock);
        BigDecimal variation = jdbc.queryForObject("SELECT SUM(debit - credit) FROM compta_lignes WHERE livre_id = ? AND exercice_id = ? AND compte_numero = '6114'", BigDecimal.class, livre.livreId(), suivant.id());
        montant("6000.00", variation);
    }

    @Test
    void lesLignesSeProposentDepuisLesInventairesDuCommercial() {
        Long produit = inserer("INSERT INTO produits (reference, description, type_produit, point_de_vente_id) VALUES ('P' || nextval('produits_id_seq'), 'Article', 'MARCHANDISE', ?) RETURNING id", pdv);
        Long ancien = inserer("INSERT INTO inventaires (reference, date_inventaire, statut, depot_id, point_de_vente_id) VALUES (?, '2026-03-01', 'VALIDE', 1, ?) RETURNING id", "INV-A-" + pdv, pdv);
        commercial("INSERT INTO lignes_inventaire (inventaire_id, produit_id, quantite_theorique, quantite_reelle, prix_unitaire) VALUES (?, ?, 1, 999, 1)", ancien, produit);
        Long recent = inserer("INSERT INTO inventaires (reference, date_inventaire, statut, depot_id, point_de_vente_id) VALUES (?, '2026-12-20', 'VALIDE', 1, ?) RETURNING id", "INV-B-" + pdv, pdv);
        commercial("INSERT INTO lignes_inventaire (inventaire_id, produit_id, quantite_theorique, quantite_reelle, prix_unitaire) VALUES (?, ?, 12, 10, 25.5)", recent, produit);

        InventaireDTO i = inventaires.proposer(exercice);

        assertEquals(1, i.lignes().size(), "seul le dernier inventaire du dépôt compte");
        assertEquals("PRIX_ACHAT_COURANT", i.methodeValorisation());
        montant("255.00", i.lignes().get(0).valeur());
        assertEquals("3111", i.lignes().get(0).compteStock());
    }

    @Test
    void sansInventaireCommercialLaPropositionLeDit() {
        assertRefus("COMPTA_INVENTAIRE_RIEN", () -> inventaires.proposer(exercice));
    }

    @Test
    void lInventaireExigeSaMethodeEtSesLignes() {
        assertRefus("COMPTA_INVENTAIRE_METHODE", () -> inventaires.enregistrer(exercice, new InventaireRequest(null, false, List.of(ligne("3111", "A", "1", "1")))));
        assertRefus("COMPTA_INVENTAIRE_LIGNES", () -> inventaires.enregistrer(exercice, new InventaireRequest(null, true, List.of(ligne("3111", "A", "1", "1")))));
        assertRefus("COMPTA_INVENTAIRE_LIGNES", () -> inventaires.enregistrer(exercice, new InventaireRequest("X", false, List.of(ligne("3999", "A", "1", "1")))));
        assertRefus("COMPTA_INVENTAIRE_ABSENT", () -> inventaires.valider(exercice));
        inventaires.enregistrer(exercice, new InventaireRequest("X", false, List.of()));
        assertRefus("COMPTA_INVENTAIRE_LIGNES", () -> inventaires.valider(exercice));
    }

    @Test
    void unInventaireValideSeRouvreParContrePassation() {
        fermerPeriodes(exercice);
        inventaires.enregistrer(exercice, new InventaireRequest("PRIX_ACHAT_COURANT", false, List.of(ligne("3111", "A", "1", "500"))));
        inventaires.valider(exercice);
        assertRefus("COMPTA_INVENTAIRE_VALIDE", () -> inventaires.enregistrer(exercice, new InventaireRequest("X", true, List.of())));

        InventaireDTO rouvert = inventaires.rouvrir(exercice, new AnnulationRequest("Comptage erroné"));

        assertEquals("BROUILLON", rouvert.statut());
        solde("3111", "0");
        solde("6114", "0");
        inventaires.enregistrer(exercice, new InventaireRequest("PRIX_ACHAT_COURANT", false, List.of(ligne("3111", "A", "2", "500"))));
        inventaires.valider(exercice);
        solde("3111", "1000.00");
    }

    // --- Arrêté

    @Test
    void sansInventaireNiDeclarationLArreteEstRefuse() {
        fermerPeriodes(exercice);

        CommonException refus = assertThrows(CommonException.class, () -> cloture.arreter(exercice, new ArreterRequest("Pas d'IS.", null)));

        assertEquals("COMPTA_ARRETE_IMPOSSIBLE", refus.getErrorCode());
        assertTrue(refus.getMessage().contains("inventaire"), refus::getMessage);
        assertFalse(cloture.etat(exercice).peutArreter());
        inventaires.enregistrer(exercice, new InventaireRequest(null, true, List.of()));
        inventaires.valider(exercice);
        assertTrue(cloture.etat(exercice).peutArreter());
        assertEquals("ARRETE", cloture.arreter(exercice, new ArreterRequest("Pas d'IS.", null)).statut());
    }

    @Test
    void uneDotationManquanteDemandeUnMotifALArrete() {
        chariot("2026-01-10", "2026-01-01", "60000.00", "0", "0");
        fermerPeriodes(exercice);
        inventaires.enregistrer(exercice, new InventaireRequest(null, true, List.of()));
        inventaires.valider(exercice);

        assertEquals("A_FAIRE", cloture.etat(exercice).etapes().stream().filter(e -> "DOTATIONS".equals(e.code())).findFirst().orElseThrow().etat());
        CommonException refus = assertThrows(CommonException.class, () -> cloture.arreter(exercice, new ArreterRequest("Pas d'IS.", " ")));
        assertTrue(refus.getMessage().contains("dotation"), refus::getMessage);

        assertEquals("ARRETE", cloture.arreter(exercice, new ArreterRequest("Pas d'IS.", "Bien hors d'usage, sortie prévue")).statut());
    }

    @Test
    void passerLesDotationsLeveLEtape() {
        chariot("2026-01-10", "2026-01-01", "60000.00", "0", "0");
        fermerPeriodes(exercice);
        immobilisations.dotations(exercice, new DotationsRequest(false));
        inventaires.enregistrer(exercice, new InventaireRequest(null, true, List.of()));
        inventaires.valider(exercice);

        assertEquals("FAIT", cloture.etat(exercice).etapes().stream().filter(e -> "DOTATIONS".equals(e.code())).findFirst().orElseThrow().etat());
        assertEquals("ARRETE", cloture.arreter(exercice, new ArreterRequest("Pas d'IS.", null)).statut());
    }

    @Test
    void unAutreLivreNeVoitPasLaFiche() {
        ImmobilisationDTO f = chariot("2026-01-10", "2026-01-01", "1000.00", "0", "0");
        SocieteDeTest autre = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(autre.pointDeVenteId());
        abonnements.modifier(autre.pointDeVenteId(), true);
        livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU), new IdentiteLivreDTO("Autre", "SARL", "99900002", "999000000000002", null, null, null, "Rabat", "Négoce", null, null, "GENERAL")));

        assertThrows(ResourceNotFoundException.class, () -> immobilisations.lire(f.id()));
        assertTrue(immobilisations.lister().isEmpty());
    }

    @Test
    void lesLignesDeClasseDeuxSansFicheSeVoient() {
        Long fa = inserer("INSERT INTO factures_achat (numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) VALUES (?, '2026-01-10', ?, 1000, 200, 1200, 'EN_ATTENTE', ?) RETURNING id",
                "FA-I-" + pdv, inserer("INSERT INTO fournisseurs (raison_social, pays, point_de_vente_id) VALUES ('Sebou', 'Maroc', ?) RETURNING id", pdv), pdv);
        jdbc.update("INSERT INTO compta_ventilations (livre_id, source_type, source_id, numero, compte_numero, montant_ht, taux_tva, montant_tva, tva_deductible, est_immobilisation, saisie_par) VALUES (?, 'FACTURE_ACHAT', ?, 1, '2355', 1000, 20, 200, TRUE, TRUE, 'essai')", livre.livreId(), fa);
        assertTrue(moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("FACTURE_ACHAT", fa, "COMPTABILISATION", null, null)))).resultats().get(0).reussi());

        var lignes = immobilisations.aEnregistrer();

        assertEquals(1, lignes.size());
        assertEquals("2355", lignes.get(0).compte());
        montant("1000.00", lignes.get(0).montant());
        solde("34551", "200.00");
        ImmobilisationDTO f = immobilisations.creer(new ImmobilisationRequest("Serveur", "2355", null, null, LocalDate.parse("2026-01-10"), LocalDate.parse("2026-01-10"), new BigDecimal("1000.00"), new BigDecimal("200.00"), null, "LINEAIRE", 60, null, null, lignes.get(0).ecritureId(), null, null, "FACTURE_ACHAT", fa));
        assertEquals("28355", f.compteAmort());
        assertTrue(immobilisations.aEnregistrer().isEmpty());
    }

    // --- Outils

    private ImmobilisationDTO chariot(String acquisition, String service, String brut, String tva, String reprise) {
        return immobilisations.creer(new ImmobilisationRequest("Chariot élévateur", "23321", null, null, LocalDate.parse(acquisition), LocalDate.parse(service), new BigDecimal(brut), new BigDecimal(tva), new BigDecimal(reprise), "LINEAIRE", 60, null, "BANQUE", null, "FA-2026-0091", null, null, null));
    }

    /** S11 : acquise en 2024, 40 000 d'amortissements repris au démarrage. */
    private ImmobilisationDTO chariotReprise() {
        return immobilisations.creer(new ImmobilisationRequest("Chariot S11", "23321", null, null, LocalDate.parse("2026-01-01"), LocalDate.parse("2024-01-01"), new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), "LINEAIRE", 60, null, "BANQUE", null, "IMM-S11", null, null, null));
    }

    private static ImmobilisationRequest requete(String compte, String acquisition, String service, String brut, String mode, Integer duree, String reprise, String contrepartie, Long tiers) {
        return new ImmobilisationRequest("Bien", compte, null, null, LocalDate.parse(acquisition), LocalDate.parse(service), new BigDecimal(brut), BigDecimal.ZERO, new BigDecimal(reprise), mode, duree, tiers, contrepartie, null, "PIECE-1", null, null, null);
    }

    private static LigneInventaireDTO ligne(String compte, String designation, String quantite, String prix) {
        return new LigneInventaireDTO(null, compte, null, null, designation, new BigDecimal(quantite), new BigDecimal(prix), null, null);
    }

    private void ouvrirJusqua(int mois) {
        for (PeriodeDTO p : exercices.periodes(exercice)) {
            if ("COURANTE".equals(p.typePeriode()) && p.du().getMonthValue() < mois && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
    }

    private void fermerPeriodes(Long exerciceId) {
        for (PeriodeDTO p : exercices.periodes(exerciceId)) {
            if ("COURANTE".equals(p.typePeriode()) && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
    }

    private static void montant(String attendu, BigDecimal valeur) {
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    private void solde(String compte, String attendu) {
        BigDecimal solde = jdbc.queryForObject("SELECT COALESCE(SUM(debit - credit), 0) FROM compta_lignes WHERE livre_id = ? AND compte_numero = ?", BigDecimal.class, livre.livreId(), compte);
        assertEquals(0, new BigDecimal(attendu).compareTo(solde), "compte " + compte + " : " + solde + " au lieu de " + attendu);
    }

    private long compter(String deOu) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + deOu, Long.class);
        return n == null ? 0 : n;
    }

    private static void assertRefus(String code, Runnable action) {
        assertEquals(code, assertThrows(CommonException.class, action::run).getErrorCode());
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
}
