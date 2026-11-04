package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceAuxiliaireDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.GrandLivreCompteDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageRequest;
import com.gestion.persistent.dto.compta.ConsultationDtos.LigneBalanceDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.ContrepassationRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ConsultationService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.JournalService;
import com.gestion.service.compta.LettrageService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.PeriodeService;
import com.gestion.service.compta.TiersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JournalComptaIT extends AbstractComptaIT {

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private EcritureService ecritures;
    @Autowired private ConsultationService consultation;
    @Autowired private LettrageService lettrage;
    @Autowired private JournalService journaux;
    @Autowired private TiersService tiers;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private JdbcTemplate jdbc;

    private LivreDTO livre;
    private Map<String, Long> journal;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(societe.pointDeVenteId());
        abonnements.modifier(societe.pointDeVenteId(), true);
        livre = livres.activer(new ActivationLivreRequest(LocalDate.parse("2026-01-01"),
                new ExerciceDatesRequest("2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        journal = journaux.lister().stream().collect(Collectors.toMap(JournalDTO::code, JournalDTO::id));
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    @Test
    void unBrouillonDesequilibreSEnregistreMaisNeSeValidePas() {
        BrouillonDTO b = ecritures.creerBrouillon(brouillon("AC", "2026-01-14", "FC 4471",
                l("6111", null, "1875.00", "0"), l("34552", null, "375.00", "0"), l("4411", null, "0", "2000.00")));

        assertEquals(new BigDecimal("250.00"), b.ecart());
        assertTrue(b.anomalies().stream().anyMatch(a -> "ECART".equals(a.code())));
        assertTrue(b.anomalies().stream().anyMatch(a -> "TIERS_OBLIGATOIRE".equals(a.code()) && a.ligne() == 3));
        assertRefus("COMPTA_ECRITURE_INVALIDE", () -> ecritures.validerBrouillon(b.id()));
        assertEquals(0, compter("compta_ecritures WHERE livre_id = " + livre.livreId()));
    }

    @Test
    void validerNumeroteSansTrouEtRetireLeBrouillon() {
        TiersDTO fournisseur = tiers("FOURNISSEUR", "Imprimerie Al Massira");
        BrouillonDTO b1 = ecritures.creerBrouillon(brouillon("AC", "2026-01-14", "FC 1",
                l("6111", null, "1875.00", "0"), l("34552", null, "375.00", "0"), l("4411", fournisseur.id(), "0", "2250.00")));
        BrouillonDTO b2 = ecritures.creerBrouillon(brouillon("AC", "2026-01-15", "FC 2",
                l("6111", null, "100.00", "0"), l("4411", fournisseur.id(), "0", "100.00")));

        assertEquals(List.of(), b1.anomalies());
        EcritureDTO premiere = ecritures.validerBrouillon(b1.id());
        EcritureDTO seconde = ecritures.validerBrouillon(b2.id());

        assertEquals(1, premiere.numero());
        assertEquals(2, seconde.numero());
        assertEquals("AC-2026-000001", premiere.numeroComplet());
        assertEquals("SAISIE", premiere.origine());
        assertEquals(3, premiere.lignes().size());
        assertEquals(0, compter("compta_brouillons WHERE livre_id = " + livre.livreId()));
        assertThrows(ResourceNotFoundException.class, () -> ecritures.lireBrouillon(b1.id()));
    }

    @Test
    void uneEcritureValideeNeSeModifiePlusMemeDirectementEnBase() {
        EcritureDTO e = passer("OD", "2026-01-20", "OD 1", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00"));

        assertRefusBase("compta_immuable", () -> jdbc.update("UPDATE compta_ecritures SET libelle = 'x' WHERE id = ?", e.id()));
        assertRefusBase("compta_immuable", () -> jdbc.update("DELETE FROM compta_lignes WHERE ecriture_id = ?", e.id()));
        assertEquals(new BigDecimal("10.00"), ecritures.lire(e.id()).totalDebit());
    }

    @Test
    void laContrepassationEstUnMiroirUniqueEtSAnnuleDansLaBalanceExpurgee() {
        EcritureDTO e = passer("OD", "2026-01-20", "OD 1", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00"));

        EcritureDTO miroir = ecritures.contrepasser(e.id(), new ContrepassationRequest(null, "Compte erroné"));

        assertTrue(miroir.estContrepassation());
        assertEquals(e.id(), miroir.contrepassationDeId());
        assertEquals(miroir.id(), ecritures.lire(e.id()).contrepasseeParId());
        assertEquals(new BigDecimal("10.00"), miroir.lignes().get(0).credit());
        assertRefus("COMPTA_DEJA_CONTREPASSEE", () -> ecritures.contrepasser(e.id(), new ContrepassationRequest(null, "Encore")));
        assertRefus("COMPTA_DEJA_CONTREPASSEE", () -> ecritures.contrepasser(miroir.id(), new ContrepassationRequest(null, "Encore")));
        LigneBalanceDTO brute = ligne(consultation.balance(livre.exerciceCourantId(), null, null, false, null, null), "6111");
        assertEquals(new BigDecimal("10.00"), brute.mouvementsDebit());
        assertEquals(new BigDecimal("10.00"), brute.mouvementsCredit());
        LigneBalanceDTO expurgee = ligne(consultation.balance(livre.exerciceCourantId(), null, null, true, null, null), "6111");
        assertEquals(0, expurgee.mouvementsDebit().signum());
        assertEquals(0, expurgee.mouvementsCredit().signum());
    }

    @Test
    void contrepasserLibereLesLignesDuTiersEtDefaitSonLettrage() {
        TiersDTO client = tiers("CLIENT", "Client A");
        EcritureDTO facture = passer("VT", "2026-01-10", "FV 1", l("3421", client.id(), "100.00", "0"), l("7111", null, "0", "100.00"));
        EcritureDTO reglement = passer("BQ", "2026-01-20", "VIR 1", l("5141", null, "100.00", "0"), l("3421", client.id(), "0", "100.00"));
        lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-20"),
                List.of(ligneTiers(facture), ligneTiers(reglement))));

        ecritures.contrepasser(facture.id(), new ContrepassationRequest(null, "Facture annulée"));

        // Le règlement redevient ouvert ; la facture et son miroir sont lettrés ensemble, sans ligne fantôme.
        assertEquals(new BigDecimal("-100.00"), lettrage.lignes("3421", client.id(), "OUVERTES", null, null).soldeOuvert());
        assertEquals(1, lettrage.lignes("3421", client.id(), "OUVERTES", null, null).contenu().size());
    }

    @Test
    void uneEcritureHorsPeriodeOuverteOuAntichronologiqueEstRefusee() {
        assertRefus("COMPTA_PERIODE_A_VENIR", () -> passer("OD", "2026-03-10", "OD 1", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00")));
        assertRefus("COMPTA_PERIODE_INCONNUE", () -> passer("OD", "2027-01-10", "OD 2", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00")));
        passer("OD", "2026-02-10", "OD 3", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00"));
        assertRefus("COMPTA_CHRONOLOGIE_JOURNAL", () -> passer("OD", "2026-01-10", "OD 4", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00")));
        passer("BQ", "2026-01-10", "BQ 1", l("5141", null, "10.00", "0"), l("7111", null, "0", "10.00"));
        // Dans le mois, le désordre est permis.
        passer("OD", "2026-02-05", "OD 5", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00"));

        PeriodeDTO janvier = exercices.periodes(livre.exerciceCourantId()).stream().filter(p -> p.numero() == 1).findFirst().orElseThrow();
        periodes.cloturer(janvier.id());
        assertRefus("COMPTA_PERIODE_FERMEE", () -> passer("BQ", "2026-01-20", "BQ 2", l("5141", null, "10.00", "0"), l("7111", null, "0", "10.00")));
    }

    @Test
    void unBrouillonRestantEmpecheDeFermerSaPeriode() {
        BrouillonDTO b = ecritures.creerBrouillon(brouillon("OD", "2026-01-20", "OD 1", l("6111", null, "10.00", "0")));
        PeriodeDTO janvier = exercices.periodes(livre.exerciceCourantId()).stream().filter(p -> p.numero() == 1).findFirst().orElseThrow();

        assertEquals(1, janvier.nbBrouillons());
        assertRefus("COMPTA_PERIODE_NON_CLOTURABLE", () -> periodes.cloturer(janvier.id()));
        ecritures.supprimerBrouillon(b.id());
        assertEquals("CLOTUREE", periodes.cloturer(janvier.id()).statut());
    }

    /** S3 : le même code pour deux clients était possible. Ici, le service refuse et la base aussi. */
    @Test
    void onNeLettrePasDeuxClientsEnsemble() {
        TiersDTO a = tiers("CLIENT", "Client A");
        TiersDTO b = tiers("CLIENT", "Client B");
        EcritureDTO factureA = passer("VT", "2026-01-10", "FV 1", l("3421", a.id(), "100.00", "0"), l("7111", null, "0", "100.00"));
        EcritureDTO reglementB = passer("BQ", "2026-01-20", "VIR 1", l("5141", null, "100.00", "0"), l("3421", b.id(), "0", "100.00"));

        assertRefus("COMPTA_LETTRAGE_LIGNE_REFUSEE", () -> lettrage.lettrer(new LettrageRequest("3421", a.id(), "TOTAL", LocalDate.parse("2026-01-20"),
                List.of(ligneTiers(factureA), ligneTiers(reglementB)))));
        assertEquals(List.of(), lettrage.propositions("3421", a.id()));
        // Même sans le service, la clé composite refuse : la ligne de B ne rentre pas dans un groupe de A.
        assertRefusBase("fk_compta_lel_ligne", () -> jdbc.update("""
                WITH g AS (INSERT INTO compta_lettrages (livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par)
                           VALUES (?, '3421', ?, 0, '', 'PARTIEL', 'MANUEL', '2026-01-20', 'test') RETURNING id)
                INSERT INTO compta_lettrage_lignes (ligne_id, lettrage_id, livre_id, compte_numero, tiers_id)
                SELECT ?, g.id, ?, '3421', ? FROM g
                """, livre.livreId(), a.id(), ligneTiers(reglementB), livre.livreId(), a.id()));
    }

    /** S4 : le code restait bloqué à ZZ puis repartait de AAA, et un délettrage défaisait plusieurs groupes. */
    @Test
    void lesCodesDeLettrageNeRepartentJamaisEtUnDelettrageNeTouchequUnGroupe() {
        assertEquals("AA", jdbc.queryForObject("SELECT compta_code_lettrage(1)", String.class));
        assertEquals("ZZ", jdbc.queryForObject("SELECT compta_code_lettrage(676)", String.class));
        assertEquals("AAA", jdbc.queryForObject("SELECT compta_code_lettrage(677)", String.class));
        assertEquals("AAB", jdbc.queryForObject("SELECT compta_code_lettrage(678)", String.class));
        TiersDTO client = tiers("CLIENT", "Client A");
        List<LettrageDTO> groupes = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            EcritureDTO facture = passer("VT", "2026-01-1" + i, "FV " + i, l("3421", client.id(), "100.00", "0"), l("7111", null, "0", "100.00"));
            EcritureDTO reglement = passer("BQ", "2026-01-2" + i, "VIR " + i, l("5141", null, "100.00", "0"), l("3421", client.id(), "0", "100.00"));
            groupes.add(lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-25"),
                    List.of(ligneTiers(facture), ligneTiers(reglement)))));
        }
        assertEquals(List.of("AA", "AB"), groupes.stream().map(LettrageDTO::code).toList());

        lettrage.delettrer(groupes.get(0).id());

        assertEquals(2, lettrage.lignes("3421", client.id(), "OUVERTES", null, null).contenu().size());
        assertEquals(2, lettrage.lignes("3421", client.id(), "LETTREES", null, null).contenu().size());
        // Le code d'un groupe défait n'est jamais réutilisé.
        EcritureDTO facture = passer("VT", "2026-01-28", "FV 9", l("3421", client.id(), "50.00", "0"), l("7111", null, "0", "50.00"));
        EcritureDTO reglement = passer("BQ", "2026-01-28", "VIR 9", l("5141", null, "50.00", "0"), l("3421", client.id(), "0", "50.00"));
        assertEquals("AC", lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-28"),
                List.of(ligneTiers(facture), ligneTiers(reglement)))).code());
    }

    @Test
    void unLettrageTotalDesequilibreDonneSonEcart() {
        TiersDTO client = tiers("CLIENT", "Client A");
        EcritureDTO facture = passer("VT", "2026-01-10", "FV 1", l("3421", client.id(), "100.00", "0"), l("7111", null, "0", "100.00"));
        EcritureDTO reglement = passer("BQ", "2026-01-20", "VIR 1", l("5141", null, "60.00", "0"), l("3421", client.id(), "0", "60.00"));
        List<Long> lignes = List.of(ligneTiers(facture), ligneTiers(reglement));

        CommonException refus = assertThrows(CommonException.class, () -> lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-20"), lignes)));
        assertEquals("COMPTA_LETTRAGE_ECART", refus.getErrorCode());
        assertTrue(refus.getMessage().contains("40.00"), refus.getMessage());
        LettrageDTO partiel = lettrage.lettrer(new LettrageRequest("3421", client.id(), "PARTIEL", LocalDate.parse("2026-01-20"), lignes));
        assertEquals(new BigDecimal("40.00"), partiel.ecart());
    }

    @Test
    void laBalanceEstEquilibreeEtSepareLesTiersDeSensContraire() {
        TiersDTO a = tiers("CLIENT", "Client débiteur");
        TiersDTO b = tiers("CLIENT", "Client trop payé");
        passer("VT", "2026-01-10", "FV 1", l("3421", a.id(), "800.00", "0"), l("7111", null, "0", "800.00"));
        passer("BQ", "2026-01-20", "VIR 1", l("5141", null, "500.00", "0"), l("3421", b.id(), "0", "500.00"));

        BalanceDTO balance = consultation.balance(livre.exerciceCourantId(), null, null, false, null, null);

        LigneBalanceDTO clients = ligne(balance, "3421");
        assertEquals(new BigDecimal("800.00"), clients.soldeDebit());
        assertEquals(new BigDecimal("500.00"), clients.soldeCredit());
        assertEquals(0, balance.totaux().soldeDebit().compareTo(balance.totaux().soldeCredit()));
        assertEquals(0, balance.totaux().mouvementsDebit().compareTo(balance.totaux().mouvementsCredit()));
        assertEquals(new BigDecimal("800.00"), balance.resultat().produits());
        assertEquals(new BigDecimal("800.00"), balance.resultat().net());
        BalanceAuxiliaireDTO auxiliaire = consultation.balanceAuxiliaire(livre.exerciceCourantId(), "3421", null, null, null, null);
        assertEquals(clients.soldeDebit(), auxiliaire.totaux().soldeDebit());
        assertEquals(clients.soldeCredit(), auxiliaire.totaux().soldeCredit());
        assertEquals(1, consultation.balance(livre.exerciceCourantId(), LocalDate.parse("2026-01-15"), 5, false, null, null).totauxClasses().stream()
                .filter(t -> t.classe() == 7).count());
    }

    @Test
    void leGrandLivrePortePagesEtSoldeProgressifContinu() {
        TiersDTO client = tiers("CLIENT", "Client A");
        for (int i = 1; i <= 5; i++) {
            passer("VT", "2026-01-0" + i, "FV " + i, l("3421", client.id(), "100.00", "0"), l("7111", null, "0", "100.00"));
        }

        GrandLivreCompteDTO premiere = consultation.grandLivre(livre.exerciceCourantId(), "3421", client.id(), null, null, false, null, 2);
        GrandLivreCompteDTO seconde = consultation.grandLivre(livre.exerciceCourantId(), "3421", client.id(), null, null, false, premiere.curseurSuivant(), 2);

        assertEquals(new BigDecimal("500.00"), premiere.solde());
        assertEquals(new BigDecimal("500.00"), premiere.cumulDebit());
        assertEquals(new BigDecimal("200.00"), premiere.contenu().get(1).soldeProgressif());
        assertEquals(new BigDecimal("400.00"), seconde.contenu().get(1).soldeProgressif());
        assertNotNull(seconde.curseurSuivant());
        assertNull(consultation.grandLivre(livre.exerciceCourantId(), "3421", client.id(), null, null, false, seconde.curseurSuivant(), 2).curseurSuivant());
    }

    @Test
    void lOuvertureDeLExerciceSuivantEstCalculeeEtMarqueeProvisoire() {
        TiersDTO client = tiers("CLIENT", "Client A");
        passer("VT", "2026-01-10", "FV 1", l("3421", client.id(), "800.00", "0"), l("7111", null, "0", "800.00"));
        Long suivant = exercices.creerSuivant(livre.exerciceCourantId(),
                new ExerciceDatesRequest("2027", LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31"))).id();

        BalanceDTO balance = consultation.balance(suivant, null, null, false, null, null);

        assertTrue(balance.ouvertureProvisoire());
        assertEquals(new BigDecimal("800.00"), ligne(balance, "3421").ouvertureDebit());
        assertEquals(new BigDecimal("800.00"), ligne(balance, "1191").ouvertureCredit(), "le bénéfice de N va sur 1191");
        assertEquals(0, balance.resultat().net().signum(), "le résultat de N ne compte pas dans celui de N+1");
    }

    @Test
    void unObjetDUnAutreLivreEstIntrouvable() {
        EcritureDTO e = passer("OD", "2026-01-20", "OD 1", l("6111", null, "10.00", "0"), l("5141", null, "0", "10.00"));
        Long exercice = livre.exerciceCourantId();
        preparer();

        assertThrows(ResourceNotFoundException.class, () -> ecritures.lire(e.id()));
        assertThrows(ResourceNotFoundException.class, () -> consultation.balance(exercice, null, null, false, null, null));
    }

    @Test
    void numerotationSansTrouNiDoublonSousConcurrence() throws Exception {
        Long od = journal.get("OD");
        long pointDeVente = jdbc.queryForObject("SELECT point_de_vente_id FROM compta_livre_points_de_vente WHERE livre_id = ?", Long.class, livre.livreId());
        List<Thread> fils = new ArrayList<>();
        List<Throwable> erreurs = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int f = 0; f < 8; f++) {
            Thread fil = new Thread(() -> {
                ContexteDeTest.surPointDeVente(pointDeVente);
                try {
                    for (int i = 0; i < 10; i++) {
                        BrouillonDTO b = ecritures.creerBrouillon(new BrouillonRequest(od, LocalDate.parse("2026-01-20"), LocalDate.parse("2026-01-20"), "P" + i, "Concurrence", "COURANTE", null, null,
                                List.of(l("6111", null, "1.00", "0"), l("5141", null, "0", "1.00"))));
                        ecritures.validerBrouillon(b.id());
                    }
                } catch (Throwable t) {
                    erreurs.add(t);
                } finally {
                    ContexteDeTest.effacer();
                }
            });
            fils.add(fil);
            fil.start();
        }
        for (Thread fil : fils) {
            fil.join();
        }
        assertEquals(List.of(), erreurs);
        List<Long> numeros = jdbc.queryForList("SELECT numero FROM compta_ecritures WHERE livre_id = ? AND journal_id = ? ORDER BY numero", Long.class, livre.livreId(), od);
        assertEquals(80, numeros.size());
        for (int i = 0; i < 80; i++) {
            assertEquals(i + 1L, numeros.get(i));
        }
    }

    private TiersDTO tiers(String type, String nom) {
        return tiers.creer(new TiersCreationRequest(type, nom, null, null, null, null, null, null));
    }

    private EcritureDTO passer(String journalCode, String date, String piece, BrouillonLigneRequest... lignes) {
        BrouillonDTO b = ecritures.creerBrouillon(brouillon(journalCode, date, piece, lignes));
        try {
            return ecritures.validerBrouillon(b.id());
        } catch (RuntimeException e) {
            ecritures.supprimerBrouillon(b.id());
            throw e;
        }
    }

    private BrouillonRequest brouillon(String journalCode, String date, String piece, BrouillonLigneRequest... lignes) {
        LocalDate jour = LocalDate.parse(date);
        return new BrouillonRequest(journal.get(journalCode), jour, jour, piece, "Écriture " + piece, "COURANTE", null, null, List.of(lignes));
    }

    private static BrouillonLigneRequest l(String compte, Long tiersId, String debit, String credit) {
        return new BrouillonLigneRequest(compte, tiersId, new BigDecimal(debit), new BigDecimal(credit), null, null, null, null, null, null, null, null);
    }

    private static Long ligneTiers(EcritureDTO e) {
        return e.lignes().stream().filter(l -> l.tiers() != null).findFirst().orElseThrow().id();
    }

    private static LigneBalanceDTO ligne(BalanceDTO balance, String compte) {
        return balance.contenu().stream().filter(l -> l.compte().equals(compte)).findFirst().orElseThrow();
    }

    private long compter(String deOu) {
        return jdbc.queryForObject("SELECT count(*) FROM " + deOu, Long.class);
    }

    private static void assertRefus(String code, Runnable action) {
        CommonException refus = assertThrows(CommonException.class, action::run);
        assertEquals(code, refus.getErrorCode(), refus.getMessage());
        assertFalse(refus.getMessage().isBlank());
    }

    private static void assertRefusBase(String contrainte, Runnable action) {
        DataAccessException refus = assertThrows(DataAccessException.class, action::run);
        assertTrue(refus.getMostSpecificCause().getMessage().contains(contrainte), refus.getMostSpecificCause().getMessage());
    }
}
