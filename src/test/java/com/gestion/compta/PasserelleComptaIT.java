package com.gestion.compta;

import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuCompteDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComplementsRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.CompteurSourceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComptabiliserRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.CorrespondanceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.LotDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PageFileDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceFileDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PropositionDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ResultatPieceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.VentilationLigneDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.VentilationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.repository.compta.MesuresRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ClotureService;
import com.gestion.service.compta.CorrespondanceService;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.FileComptabilisationService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.MoteurComptabilisation;
import com.gestion.service.compta.PeriodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le commercial est inséré en SQL, comme le ferait n'importe quelle application : la passerelle ne connaît que ses tables. */
@SpringBootTest
class PasserelleComptaIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");

    @Autowired private com.gestion.service.compta.InventaireClotureService inventaires;
    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private FileComptabilisationService file;
    @Autowired private MoteurComptabilisation moteur;
    @Autowired private CorrespondanceService correspondances;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private ClotureService cloture;
    @Autowired private com.gestion.service.compta.LettrageService lettrage;
    @Autowired private MesuresRepository mesures;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private LivreDTO livre;
    private Long pdv;
    private Long client;
    private Long fournisseur;
    private Long prestataire;
    private Long produit;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        pdv = societe.pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        abonnements.modifier(pdv, true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        client = inserer("INSERT INTO clients (nom, nom_complet, pays, point_de_vente_id) VALUES ('Droguerie', 'Droguerie Moulay Idriss', 'Maroc', ?) RETURNING id", pdv);
        fournisseur = inserer("INSERT INTO fournisseurs (raison_social, pays, point_de_vente_id) VALUES ('Imprimerie Al Massira', 'Maroc', ?) RETURNING id", pdv);
        prestataire = inserer("INSERT INTO prestataires (raison_sociale, point_de_vente_id) VALUES ('Bureau Conseil', ?) RETURNING id", pdv);
        produit = inserer("INSERT INTO produits (reference, description, type_produit, point_de_vente_id) VALUES ('P' || nextval('produits_id_seq'), 'Article', 'MARCHANDISE', ?) RETURNING id", pdv);
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    @Test
    void uneFactureArriveAuJournalEtSonLienTient() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);

        PieceFileDTO avant = piece("FACTURE", f);
        assertEquals("A_COMPTABILISER", avant.etat());
        PropositionDTO proposition = file.proposition("FACTURE", f, "COMPTABILISATION");
        assertEquals("VT", proposition.journal().code());
        assertEquals(LocalDate.parse("2026-01-12"), proposition.dateEcriture());
        assertEquals(3, proposition.lignes().size());
        assertEquals("Droguerie Moulay Idriss", proposition.lignes().get(0).tiers().nom());

        ResultatPieceDTO r = passer("FACTURE", f, "COMPTABILISATION", avant.empreinte());

        assertTrue(r.reussi(), r::message);
        assertNotNull(r.numeroComplet());
        assertEquals("COMPTABILISEE", piece("FACTURE", f).etat());
        assertSolde("3421", "1200.00");
        assertSolde("7111", "-1000.00");
        assertSolde("4455", "-200.00");
        assertEquals("COMPTA_PIECE_RIEN_A_FAIRE", passer("FACTURE", f, "COMPTABILISATION", null).codeErreur(), "pas de double comptabilisation");
    }

    @Test
    void s5UnChequeDeMilleFaitMilleEnBanqueEtRienAilleurs() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "0", "0", "VALIDEE", false);
        passer("FACTURE", f, "COMPTABILISATION", null);
        Long p = paiement("PAY-1", "2026-01-15", "CHEQUE", "1000.00", false);
        Long e = effet("CHQ-1", "CHEQUE", "ENCAISSEMENT_CLIENT", "ENCAISSE_COMPTABILISE", "1000.00", "2026-01-20", "2026-01-25", "PAY-1", client, null);

        assertEquals("ANOMALIE", etat("EFFET", e, "REMISE"), "le paiement doit passer d'abord");
        assertTrue(passer("PAIEMENT", p, "COMPTABILISATION", null).reussi());
        assertEquals("A_COMPTABILISER", etat("EFFET", e, "REMISE"));

        LotDTO lot = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("EFFET", e, "ENCAISSEMENT", null, null))));

        assertEquals(2, lot.reussies(), "la remise passe d'abord, l'écran montre les deux écritures : " + lot.resultats());
        assertSolde("5141", "1000.00");
        assertSolde("51111", "0");
        assertSolde("51112", "0");
        assertSolde("3421", "0");
    }

    @Test
    void unEffetDUnPaiementAnnuleJamaisComptabiliseEstSansObjet() {
        Long p = paiement("PAY-2", "2026-01-15", "CHEQUE", "500.00", true);
        Long e = effet("CHQ-2", "CHEQUE", "ENCAISSEMENT_CLIENT", "IMPAYE_REJETE", "500.00", null, null, "PAY-2", client, null);

        assertEquals("SANS_OBJET", etat("PAIEMENT", p, "COMPTABILISATION"), "annulé avant comptabilisation");
        assertEquals("SANS_OBJET", etat("EFFET", e, "REJET"), "rien n'a été écrit et aucun encaissement n'a eu lieu");
    }

    @Test
    void unRejetApresComptabilisationNInversePasDeuxFois() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "0", "0", "VALIDEE", false);
        passer("FACTURE", f, "COMPTABILISATION", null);
        Long p = paiement("PAY-3", "2026-01-15", "CHEQUE", "1000.00", false);
        passer("PAIEMENT", p, "COMPTABILISATION", null);
        assertSolde("3421", "0");

        // Le service commercial annule le paiement au rejet
        Long e = effet("CHQ-3", "CHEQUE", "ENCAISSEMENT_CLIENT", "IMPAYE_REJETE", "1000.00", null, null, "PAY-3", client, null);
        commercial("UPDATE paiements SET annule = TRUE WHERE id = ?", p);
        assertEquals("COMPTABILISEE", etat("PAIEMENT", p, "COMPTABILISATION"), "le rejet s'écrit sur l'effet, pas par une inversion du paiement");
        assertEquals("ANOMALIE", etat("EFFET", e, "REJET"), "le commercial ne garde pas la date du rejet");

        ResultatPieceDTO r = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("EFFET", e, "REJET", null, new ComplementsRequest(null, LocalDate.parse("2026-01-28"), null))))).resultats().get(0);

        assertTrue(r.reussi(), r::message);
        assertSolde("51111", "0");
        assertSolde("3421", "1000.00");
        assertEquals(1L, compter("compta_lettrages WHERE livre_id = " + livre.livreId() + " AND origine = 'REJET'"), "la ligne du paiement se lettre avec celle du rejet");
    }

    @Test
    void unAvoirTotalSurUneFactureDAchatNInversePasDeuxFois() {
        Long fa = factureAchat("FA-1", "2026-01-10", "1000.00", "200.00", "EN_ATTENTE");
        assertEquals("A_COMPTABILISER", etat("FACTURE_ACHAT", fa, "COMPTABILISATION"));
        passer("FACTURE_ACHAT", fa, "COMPTABILISATION", null);
        assertSolde("4411", "-1200.00");

        // L'avoir total passe la facture ANNULEE : ce n'est pas une annulation, l'avoir fait l'inversion
        commercial("UPDATE factures_achat SET statut = 'ANNULEE' WHERE id = ?", fa);
        assertEquals("COMPTABILISEE", etat("FACTURE_ACHAT", fa, "COMPTABILISATION"));
        Long a = avoirFournisseur("AV-1", "2026-01-20", fa);
        passer("AVOIR_FOURNISSEUR", a, "COMPTABILISATION", null);
        assertSolde("4411", "0");
        assertSolde("6111", "0");
        assertSolde("34552", "0");
    }

    @Test
    void uneFactureModifieeSeContrePasseEtRepartEnVersionSuivante() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);
        passer("FACTURE", f, "COMPTABILISATION", null);
        String empreinte = piece("FACTURE", f).empreinte();

        // Un règlement ne change pas l'empreinte
        commercial("UPDATE factures SET montant_paye = 100, montant_restant = 1100, statut = 'PAYEE_PARTIELLEMENT' WHERE id = ?", f);
        assertEquals(empreinte, piece("FACTURE", f).empreinte());
        assertEquals("COMPTABILISEE", piece("FACTURE", f).etat());

        // Une modification du montant la change
        commercial("UPDATE factures SET montant_ht = 2000, montant_tva = 400, montant_ttc = 2400, montant_final = 2400 WHERE id = ?", f);
        commercial("UPDATE lignes_facture SET montant_ht = 2000, montant_tva = 400, montant_ttc = 2400 WHERE facture_id = ?", f);
        assertNotEquals(empreinte, piece("FACTURE", f).empreinte());
        assertEquals("MODIFIEE", piece("FACTURE", f).etat());
        assertEquals("COMPTA_PIECE_MODIFIEE", passer("FACTURE", f, "COMPTABILISATION", empreinte).codeErreur(), "ce que l'utilisateur a vu n'est plus ce qui existe");

        ResultatPieceDTO r = passer("FACTURE", f, "COMPTABILISATION", piece("FACTURE", f).empreinte());

        assertTrue(r.reussi(), r::message);
        assertEquals(2, piece("FACTURE", f).version());
        assertEquals("COMPTABILISEE", piece("FACTURE", f).etat());
        assertSolde("3421", "2400.00");
        assertSolde("7111", "-2000.00");
        assertEquals(1L, compter("compta_lettrages WHERE livre_id = " + livre.livreId() + " AND origine = 'CONTREPASSATION'"), "l'origine et son miroir ne laissent pas de ligne fantôme");
    }

    @Test
    void uneFactureAnnuleeEtUneDepenseSupprimeeSeContrePassent() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);
        passer("FACTURE", f, "COMPTABILISATION", null);
        commercial("UPDATE factures SET annulee = TRUE, statut = 'ANNULEE', date_annulation = '2026-01-20' WHERE id = ?", f);
        assertEquals("ANNULEE", piece("FACTURE", f).etat());
        assertTrue(passer("FACTURE", f, "COMPTABILISATION", null).reussi());
        assertEquals("SANS_OBJET", piece("FACTURE", f).etat());
        assertSolde("3421", "0");
        assertSolde("7111", "0");

        Long d = depense("DEP-1", "2026-01-15", "LOYER", "VIREMENT", "3000.00", null);
        passer("DEPENSE", d, "COMPTABILISATION", null);
        assertSolde("61312", "3000.00");
        commercial("DELETE FROM depenses WHERE id = ?", d);
        assertEquals("DISPARUE", piece("DEPENSE", d).etat());
        assertTrue(file.verifier(DU, AU).stream().anyMatch(p -> p.sourceId().equals(d) && "DISPARUE".equals(p.etat())));
        assertTrue(passer("DEPENSE", d, "COMPTABILISATION", null).reussi());
        assertSolde("61312", "0");
    }

    @Test
    void uneFactureAnnuleePuisRevalideeResteEnAnomalie() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", true);
        assertEquals("ANOMALIE", piece("FACTURE", f).etat());
        assertEquals("FACTURE_ANNULEE_REVALIDEE", passer("FACTURE", f, "COMPTABILISATION", null).codeErreur());
    }

    @Test
    void unePieceEnErreurNEmportePasLeLot() {
        List<PieceDemandee> pieces = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            pieces.add(new PieceDemandee("FACTURE", facture("FV-" + i, "2026-01-12", "100.00", "20.00", "0", "VALIDEE", false), "COMPTABILISATION", null, null));
        }
        Long autre = depense("DEP-A", "2026-01-13", "AUTRES_CHARGES", "VIREMENT", "50.00", null);
        pieces.add(4, new PieceDemandee("DEPENSE", autre, "COMPTABILISATION", null, null));

        LotDTO lot = moteur.comptabiliser(new ComptabiliserRequest(pieces));

        assertEquals(9, lot.reussies());
        assertEquals(1, lot.echecs());
        ResultatPieceDTO echec = lot.resultats().stream().filter(r -> !r.reussi()).findFirst().orElseThrow();
        assertEquals("COMPTE_A_CHOISIR", echec.codeErreur());
        assertEquals(autre, echec.sourceId());
        assertEquals(10, moteur.lot(lot.lotId()).resultats().size(), "le rapport du lot est gardé");
        assertEquals(9, moteur.lot(lot.lotId()).reussies());
    }

    @Test
    void lApercuDonneLesMemesTotauxQueLesEcrituresPassees() {
        List<PieceDemandee> pieces = List.of(
                new PieceDemandee("FACTURE", facture("FV-1", "2026-01-12", "1000.00", "200.00", "100.00", "VALIDEE", false), "COMPTABILISATION", null, null),
                new PieceDemandee("FACTURE_ACHAT", factureAchat("FA-1", "2026-01-10", "400.00", "80.00", "EN_ATTENTE"), "COMPTABILISATION", null, null),
                new PieceDemandee("DEPENSE", depense("DEP-1", "2026-01-15", "LOYER", "ESPECES", "300.00", null), "COMPTABILISATION", null, null));

        ApercuDTO apercu = file.apercu(new ApercuRequest(DU, AU, pieces));
        assertEquals(0, apercu.totalDebit().compareTo(apercu.totalCredit()));
        assertEquals(3, apercu.pieces());
        assertEquals(0L, compter("compta_ecritures WHERE livre_id = " + livre.livreId()), "l'aperçu n'écrit rien");

        assertEquals(3, moteur.comptabiliser(new ComptabiliserRequest(pieces)).reussies());
        for (ApercuCompteDTO c : apercu.parCompte()) {
            assertSolde(c.compte(), c.debit().subtract(c.credit()).toPlainString());
        }
    }

    @Test
    void deuxComptabilisationsEnParalleleUneSeulePasse() throws Exception {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);
        ExecutorService fils = Executors.newFixedThreadPool(2);
        Callable<ResultatPieceDTO> tache = () -> {
            ContexteDeTest.surPointDeVente(pdv);
            try {
                return passer("FACTURE", f, "COMPTABILISATION", null);
            } finally {
                ContexteDeTest.effacer();
            }
        };
        List<Future<ResultatPieceDTO>> futurs = fils.invokeAll(List.of(tache, tache));
        fils.shutdown();
        long reussies = 0;
        for (Future<ResultatPieceDTO> futur : futurs) {
            reussies += futur.get().reussi() ? 1 : 0;
        }
        assertEquals(1, reussies);
        assertEquals(1L, compter("compta_liens_source WHERE livre_id = " + livre.livreId() + " AND source_id = " + f + " AND source_type = 'FACTURE'"));
        assertSolde("3421", "1200.00");
    }

    @Test
    void uneVenteSeRegroupeParJourEtParClient() {
        Long v1 = vente("T-1", "2026-01-15", null, "100.00", "20.00");
        Long v2 = vente("T-2", "2026-01-15", null, "50.00", "10.00");
        Long v3 = vente("T-3", "2026-01-16", null, "10.00", "2.00");

        LotDTO lot = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("VENTE", v1, "COMPTABILISATION", null, null),
                new PieceDemandee("VENTE", v2, "COMPTABILISATION", null, null), new PieceDemandee("VENTE", v3, "COMPTABILISATION", null, null))));

        assertEquals(3, lot.reussies(), () -> lot.resultats().toString());
        assertEquals(lot.resultats().get(0).ecritureId(), lot.resultats().get(1).ecritureId(), "même jour, même écriture : COMPTA-05");
        assertNotEquals(lot.resultats().get(0).ecritureId(), lot.resultats().get(2).ecritureId());
        assertSolde("3421", "192.00");
        assertEquals(1L, compter("compta_tiers WHERE livre_id = " + livre.livreId() + " AND nom = 'CLIENTS DIVERS'"));

        // Annulée après regroupement : l'écriture commune reste, seule sa part s'inverse
        commercial("UPDATE ventes SET statut = 'ANNULEE' WHERE id = ?", v2);
        assertEquals("ANNULEE", etat("VENTE", v2, "COMPTABILISATION"));
        assertTrue(passer("VENTE", v2, "COMPTABILISATION", null).reussi());
        assertSolde("3421", "132.00");
        assertEquals("SANS_OBJET", etat("VENTE", v2, "COMPTABILISATION"));
    }

    @Test
    void uneDepenseAutresChargesAttendSaVentilation() {
        Long d = depense("DEP-A", "2026-01-13", "AUTRES_CHARGES", "VIREMENT", "1200.00", null);
        assertEquals("ANOMALIE", piece("DEPENSE", d).etat());
        assertEquals(List.of("VENTILATION"), file.proposition("DEPENSE", d, "COMPTABILISATION").complementsRequis());

        moteur.ventiler("DEPENSE", d, new VentilationRequest(List.of(new VentilationLigneDTO("61671", new BigDecimal("1000.00"), new BigDecimal("20.00"), new BigDecimal("200.00"), true, null, false, "F-1", null, null))));

        assertEquals("A_COMPTABILISER", piece("DEPENSE", d).etat());
        assertTrue(passer("DEPENSE", d, "COMPTABILISATION", null).reussi());
        assertSolde("61671", "1000.00");
        assertSolde("34552", "200.00");
        assertSolde("5141", "-1200.00");
    }

    @Test
    void uneCorrespondanceModifieeChangeLeCompteProposeEtUnCompteInconnuEstRefuse() {
        Long d = depense("DEP-1", "2026-01-15", "LOYER", "VIREMENT", "100.00", null);
        assertEquals("61312", file.proposition("DEPENSE", d, "COMPTABILISATION").lignes().get(0).compte().numero());
        correspondances.modifier(List.of(new CorrespondanceDTO("CATEGORIE_DEPENSE", "LOYER", "61311")));
        assertEquals("61311", file.proposition("DEPENSE", d, "COMPTABILISATION").lignes().get(0).compte().numero());
        assertTrue(correspondances.lister().stream().anyMatch(c -> c.typeCle().equals("BANQUE_DEFAUT") && c.compte().equals("5141")));
        assertThrowsCommon(() -> correspondances.modifier(List.of(new CorrespondanceDTO("CATEGORIE_DEPENSE", "LOYER", "999999"))));
    }

    @Test
    void uneDepenseEcarteeSortDeLaFileEtSeReprend() {
        Long d = depense("DEP-1", "2026-01-15", "LOYER", "VIREMENT", "100.00", null);
        moteur.ecarter("DEPENSE", d, "Doublon d'import");
        assertEquals("ECARTEE", piece("DEPENSE", d).etat());
        assertEquals("COMPTA_PIECE_RIEN_A_FAIRE", passer("DEPENSE", d, "COMPTABILISATION", null).codeErreur());
        moteur.reprendre("DEPENSE", d);
        assertEquals("A_COMPTABILISER", piece("DEPENSE", d).etat());
    }

    @Test
    void unePieceAVenirResteDansLaFileEtUnePieceTardivePasseSurLaPeriodeOuverte() {
        Long future = facture("FV-5", "2026-05-12", "100.00", "20.00", "0", "VALIDEE", false);
        assertEquals("COMPTA_PERIODE_A_VENIR", passer("FACTURE", future, "COMPTABILISATION", null).codeErreur());
        assertEquals("A_COMPTABILISER", piece("FACTURE", future).etat());

        fermerPeriode(1);
        Long tardive = facture("FV-J", "2026-01-20", "100.00", "20.00", "0", "VALIDEE", false);
        PropositionDTO p = file.proposition("FACTURE", tardive, "COMPTABILISATION");
        assertEquals(LocalDate.parse("2026-02-01"), p.dateEcriture(), "LOG-06 : sur la période ouverte");
        assertEquals(LocalDate.parse("2026-01-20"), p.datePiece(), "avec sa date de pièce d'origine");
        assertTrue(passer("FACTURE", tardive, "COMPTABILISATION", null).reussi());
    }

    @Test
    void unePieceDUnExerciceClosSeCorrigeSurLesComptesAnterieurs() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);
        passer("FACTURE", f, "COMPTABILISATION", null);
        for (PeriodeDTO p : exercices.periodes(livre.exerciceCourantId())) {
            if ("COURANTE".equals(p.typePeriode()) && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
        inventaires.enregistrer(livre.exerciceCourantId(), new com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest(null, true, List.of()));
        inventaires.valider(livre.exerciceCourantId());
        cloture.arreter(livre.exerciceCourantId(), new ArreterRequest("Pas d'IS.", null));
        Long suivant = cloture.cloturer(livre.exerciceCourantId(), new CloturerRequest("2026")).exerciceSuivantId();

        commercial("UPDATE factures SET annulee = TRUE, statut = 'ANNULEE', date_annulation = '2027-01-10' WHERE id = ?", f);
        assertEquals("ANNULEE", piece("FACTURE", f).etat());
        ResultatPieceDTO r = passer("FACTURE", f, "COMPTABILISATION", null);

        assertTrue(r.reussi(), r::message);
        Map<String, Object> correction = jdbc.queryForMap("SELECT corrige_ecriture_id, contrepassation_de_id, exercice_id FROM compta_ecritures WHERE id = ?", r.ecritureId());
        assertNotNull(correction.get("corrige_ecriture_id"));
        assertEquals(null, correction.get("contrepassation_de_id"), "un exercice clos ne se contre-passe pas en miroir");
        assertEquals(suivant, correction.get("exercice_id"));
        BigDecimal anterieur = jdbc.queryForObject("SELECT SUM(debit - credit) FROM compta_lignes WHERE ecriture_id = ? AND compte_numero = '7118'", BigDecimal.class, r.ecritureId());
        assertEquals(0, new BigDecimal("1000.00").compareTo(anterieur), "7111 donne 7118, colonne « Concernant les exercices précédents » du CPC");
        assertEquals("SANS_OBJET", piece("FACTURE", f).etat());
    }

    @Test
    void aucuneTableCommercialeNEstEcritePendantLaComptabilisation() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);
        Long d = depense("DEP-1", "2026-01-15", "LOYER", "VIREMENT", "300.00", null);
        Long p = paiement("PAY-1", "2026-01-15", "VIREMENT", "1200.00", false);
        Long fa = factureAchat("FA-1", "2026-01-10", "400.00", "80.00", "EN_ATTENTE");
        String avant = instantane(f, d, p, fa);

        LotDTO lot = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("FACTURE", f, "COMPTABILISATION", null, null), new PieceDemandee("DEPENSE", d, "COMPTABILISATION", null, null),
                new PieceDemandee("PAIEMENT", p, "COMPTABILISATION", null, null), new PieceDemandee("FACTURE_ACHAT", fa, "COMPTABILISATION", null, null))));

        assertEquals(4, lot.reussies(), () -> lot.resultats().toString());
        assertEquals(avant, instantane(f, d, p, fa));
    }

    @Test
    void lesAffectationsDuCommercialProposentLeLettrage() {
        Long f1 = facture("FV-1", "2026-01-12", "1000.00", "0", "0", "VALIDEE", false);
        Long f2 = facture("FV-2", "2026-01-13", "500.00", "0", "0", "VALIDEE", false);
        Long f3 = facture("FV-3", "2026-01-14", "700.00", "0", "0", "VALIDEE", false);
        Long p1 = paiement("PAY-1", "2026-01-20", "VIREMENT", "1500.00", false);
        Long p2 = paiement("PAY-2", "2026-01-21", "VIREMENT", "300.00", false);
        commercial("INSERT INTO paiement_affectations (paiement_id, facture_id, montant_affecte) VALUES (?, ?, 1000), (?, ?, 500), (?, ?, 300)", p1, f1, p1, f2, p2, f3);
        LotDTO lot = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("FACTURE", f1, "COMPTABILISATION", null, null), new PieceDemandee("FACTURE", f2, "COMPTABILISATION", null, null),
                new PieceDemandee("FACTURE", f3, "COMPTABILISATION", null, null), new PieceDemandee("PAIEMENT", p1, "COMPTABILISATION", null, null), new PieceDemandee("PAIEMENT", p2, "COMPTABILISATION", null, null))));
        assertEquals(5, lot.reussies(), () -> lot.resultats().toString());
        Long tiersId = jdbc.queryForObject("SELECT id FROM compta_tiers WHERE livre_id = ? AND source_type = 'CLIENT' AND source_id = ?", Long.class, livre.livreId(), client);

        var propositions = lettrage.propositions("3421", tiersId);

        assertEquals(1, propositions.stream().filter(x -> x.origine().equals("AFFECTATION")).count(), "un paiement partiel ne se propose pas : " + propositions);
        var proposee = propositions.stream().filter(x -> x.origine().equals("AFFECTATION")).findFirst().orElseThrow();
        assertEquals(3, proposee.lignes().size(), "un paiement qui couvre deux factures");
        lettrage.lettrer(new com.gestion.persistent.dto.compta.ConsultationDtos.LettrageRequest("3421", tiersId, "TOTAL", LocalDate.parse("2026-01-31"), proposee.lignes()));
        assertTrue(lettrage.propositions("3421", tiersId).stream().noneMatch(x -> x.origine().equals("AFFECTATION")));
    }

    @Test
    void lesCompteursEtLaFileSuiventLesEtats() {
        Long f = facture("FV-1", "2026-01-12", "1000.00", "200.00", "0", "VALIDEE", false);
        facture("FV-2", "2026-01-13", "100.00", "20.00", "0", "VALIDEE", false);
        depense("DEP-A", "2026-01-13", "AUTRES_CHARGES", "VIREMENT", "50.00", null);
        passer("FACTURE", f, "COMPTABILISATION", null);

        Map<String, CompteurSourceDTO> compteurs = new java.util.HashMap<>();
        file.compteurs(livre.exerciceCourantId()).forEach(c -> compteurs.put(c.sourceType(), c));
        assertEquals(1, compteurs.get("FACTURE").aComptabiliser());
        assertEquals(1, compteurs.get("FACTURE").comptabilisees());
        assertEquals(1, compteurs.get("DEPENSE").anomalies());

        PageFileDTO page = file.file(DU, AU, "FACTURE", "A_COMPTABILISER", null, null, null);
        assertEquals(1, page.contenu().size());
        assertEquals("FV-2-" + pdv, page.contenu().get(0).numero());
        assertEquals(1, file.file(DU, AU, null, null, "dep-a", null, null).contenu().size());
        PageFileDTO premiere = file.file(DU, AU, null, null, null, null, 2);
        assertEquals(2, premiere.contenu().size());
        assertNotNull(premiere.curseurSuivant());
        assertEquals(1, file.file(DU, AU, null, null, null, premiere.curseurSuivant(), 2).contenu().size());
        assertFalse(file.file(LocalDate.parse("2026-03-01"), AU, "FACTURE", null, null, null, null).contenu().stream().anyMatch(x -> ("FV-1-" + pdv).equals(x.numero())));
    }

    private String instantane(Long f, Long d, Long p, Long fa) {
        return String.valueOf(jdbc.queryForMap("SELECT * FROM factures WHERE id = ?", f)) + jdbc.queryForMap("SELECT * FROM depenses WHERE id = ?", d)
                + jdbc.queryForMap("SELECT * FROM paiements WHERE id = ?", p) + jdbc.queryForMap("SELECT * FROM factures_achat WHERE id = ?", fa);
    }

    private void fermerPeriode(int numero) {
        for (PeriodeDTO p : exercices.periodes(livre.exerciceCourantId())) {
            if (p.numero() == numero) {
                periodes.cloturer(p.id());
            }
        }
    }

    private ResultatPieceDTO passer(String type, Long id, String evenement, String empreinte) {
        return moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee(type, id, evenement, empreinte, null)))).resultats().get(0);
    }

    private PieceFileDTO piece(String type, Long id) {
        return file.file(DU, LocalDate.parse("2027-12-31"), type, null, null, null, 500).contenu().stream().filter(p -> p.sourceId().equals(id)).findFirst().orElseThrow(() -> new AssertionError(type + " " + id));
    }

    private String etat(String type, Long id, String evenement) {
        return file.proposition(type, id, evenement).etat();
    }

    private void assertSolde(String compte, String attendu) {
        BigDecimal solde = jdbc.queryForObject("SELECT COALESCE(SUM(debit - credit), 0) FROM compta_lignes WHERE livre_id = ? AND compte_numero = ?", BigDecimal.class, livre.livreId(), compte);
        assertEquals(0, new BigDecimal(attendu).compareTo(solde), "compte " + compte + " : " + solde + " au lieu de " + attendu);
    }

    private long compter(String deOu) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + deOu, Long.class);
        return n == null ? 0 : n;
    }

    private static void assertThrowsCommon(Runnable action) {
        org.junit.jupiter.api.Assertions.assertThrows(com.acommon.exception.CommonException.class, action::run);
    }

    /** Sans les clés étrangères de l'utilisateur et de la vente : ce n'est pas ce que cet essai regarde. */
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

    private Long facture(String numero, String date, String ht, String tva, String remise, String statut, boolean annulee) {
        BigDecimal ttc = new BigDecimal(ht).add(new BigDecimal(tva));
        BigDecimal finale = ttc.subtract(new BigDecimal(remise));
        Long id = inserer("INSERT INTO factures (numero_facture, date_facture, client_id, emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, annulee, point_de_vente_id) VALUES (?, ?::date, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                numero + "-" + pdv, date, client, new BigDecimal(ht), new BigDecimal(tva), ttc, new BigDecimal(remise), finale, statut, annulee, pdv);
        commercial("INSERT INTO lignes_facture (facture_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, ?, ?, ?)", id, produit, new BigDecimal(ht), new BigDecimal(tva), ttc);
        return id;
    }

    private Long factureAchat(String numero, String date, String ht, String tva, String statut) {
        BigDecimal ttc = new BigDecimal(ht).add(new BigDecimal(tva));
        Long id = inserer("INSERT INTO factures_achat (numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) VALUES (?, ?::timestamp, ?, ?, ?, ?, ?, ?) RETURNING id",
                numero + "-" + pdv, date, fournisseur, new BigDecimal(ht), new BigDecimal(tva), ttc, statut, pdv);
        commercial("INSERT INTO lignes_facture_achat (facture_achat_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, ?, ?, ?)", id, produit, new BigDecimal(ht), new BigDecimal(tva), ttc);
        return id;
    }

    private Long avoirFournisseur(String numero, String date, Long factureOrigine) {
        Long id = inserer("INSERT INTO avoirs (numero_avoir, type_avoir, date_avoir, statut, nature_avoir, fournisseur_id, facture_origine_id, montant_ht, montant_tva, montant_ttc, point_de_vente_id) VALUES (?, 'FOURNISSEUR', ?::date, 'VALIDE', 'RETOUR_MARCHANDISE', ?, ?, 1000, 200, 1200, ?) RETURNING id",
                numero + "-" + pdv, date, fournisseur, factureOrigine, pdv);
        commercial("INSERT INTO lignes_avoir (avoir_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, 1000, 200, 1200)", id, produit);
        return id;
    }

    private Long paiement(String numero, String date, String mode, String montant, boolean annule) {
        return inserer("INSERT INTO paiements (numero_paiement, date_paiement, mode_paiement, encaisse_par_user_id, montant, client_id, annule, point_de_vente_id) VALUES (?, ?::timestamp, ?, 1, ?, ?, ?, ?) RETURNING id",
                numero + "-" + pdv, date, mode, new BigDecimal(montant), client, annule, pdv);
    }

    private Long effet(String numero, String type, String sens, String statut, String montant, String remise, String encaissement, String reference, Long clientId, Long fournisseurId) {
        return inserer("INSERT INTO cheques_effets (numero_piece, type_effet, sens, statut, montant, date_remise, date_encaissement, reference_paiement, client_id, fournisseur_id, point_de_vente_id) VALUES (?, ?, ?, ?, ?, ?::date, ?::date, ?, ?, ?, ?) RETURNING id",
                numero + "-" + pdv, type, sens, statut, new BigDecimal(montant), remise, encaissement, reference + "-" + pdv, clientId, fournisseurId, pdv);
    }

    private Long depense(String reference, String date, String categorie, String mode, String montant, Long prestataireId) {
        return inserer("INSERT INTO depenses (reference, designation, date_depense, categorie, mode_paiement, montant, prestataire_id, point_de_vente_id) VALUES (?, 'Essai', ?::date, ?, ?, ?, ?, ?) RETURNING id",
                reference + "-" + pdv, date, categorie, mode, new BigDecimal(montant), prestataireId, pdv);
    }

    private Long vente(String ticket, String date, Long clientId, String ht, String tva) {
        BigDecimal ttc = new BigDecimal(ht).add(new BigDecimal(tva));
        Long id = inserer("INSERT INTO ventes (numero_ticket, date_vente, vendeur_id, client_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, point_de_vente_id) VALUES (?, ?::timestamp, 1, ?, ?, ?, ?, 0, ?, 'VALIDEE', ?) RETURNING id",
                ticket + "-" + pdv, date, clientId, new BigDecimal(ht), new BigDecimal(tva), ttc, ttc, pdv);
        commercial("INSERT INTO lignes_vente (vente_id, produit_id, taux_tva, montant_ht, montant_tva, montant_ttc) VALUES (?, ?, 20, ?, ?, ?)", id, produit, new BigDecimal(ht), new BigDecimal(tva), ttc);
        return id;
    }
}
