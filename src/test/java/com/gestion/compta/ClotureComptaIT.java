package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ClotureDtos.AffectationLigneRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.AffectationRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreteDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.ClotureDefinitiveDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ClotureExerciceDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.LeverArreteRequest;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RapportControlesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ResultatControleDTO;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.repository.compta.MesuresRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ClotureService;
import com.gestion.service.compta.ControleService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.EtatService;
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
class ClotureComptaIT extends AbstractComptaIT {

    @Autowired private com.gestion.service.compta.InventaireClotureService inventaires;
    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private EcritureService ecritures;
    @Autowired private LettrageService lettrage;
    @Autowired private JournalService journaux;
    @Autowired private TiersService tiers;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private EtatService etats;
    @Autowired private ControleService controles;
    @Autowired private ClotureService cloture;
    @Autowired private MesuresRepository mesures;
    @Autowired private JdbcTemplate jdbc;

    private LivreDTO livre;
    private Long ex2025;
    private Map<String, Long> journal;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(societe.pointDeVenteId());
        abonnements.modifier(societe.pointDeVenteId(), true);
        livre = livres.activer(new ActivationLivreRequest(LocalDate.parse("2025-01-01"),
                new ExerciceDatesRequest("2025", LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31")),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        ex2025 = livre.exerciceCourantId();
        journal = journaux.lister().stream().collect(Collectors.toMap(JournalDTO::code, JournalDTO::id));
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    /** S1, et S16.c et S16.d : pas d'écart égal au résultat, pas de capital doublé, d'un exercice à l'autre. */
    @Test
    void troisExercicesSEnchainentSansRienDoubler() {
        exerciceDeBase();
        assertEquals("30000.00", valeur(ex2025, "CPC", "CPC.XVI", "TOTAUX"));
        assertEquals("130000.00", valeur(ex2025, "BILAN_ACTIF", "ACT.TOTAL_GENERAL", "NET"));
        assertEquals("130000.00", valeur(ex2025, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));
        List<MesuresRepository.Mesure> avant = mesuresDe(ex2025);

        fermerPeriodes(ex2025);
        ArreteDTO arrete = arreter(ex2025);
        assertEquals(1, arrete.figeage());
        assertEquals(new BigDecimal("30000.00"), arrete.resultatNet());
        EtatDTO fige = etats.etat(ex2025, "CPC", null);
        assertEquals("FIGE", fige.origine());
        assertEquals(1, fige.figeage());

        ClotureDefinitiveDTO clos = cloture.cloturer(ex2025, new CloturerRequest("2025"));
        assertEquals("CLOTURE", clos.statut());
        assertNotNull(clos.ecritureClotureId());
        assertNotNull(clos.ecritureOuvertureId());
        assertNotNull(clos.sha256Fec());
        assertEquals(avant, mesuresDe(ex2025), "la clôture ne change aucune mesure de l'exercice");
        assertEquals("30000.00", valeur(ex2025, "CPC", "CPC.XVI", "TOTAUX"));
        assertEquals("130000.00", valeur(ex2025, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));

        Long ex2026 = clos.exerciceSuivantId();
        assertEquals("130000.00", ouverture(ex2026, "5141", true));
        assertEquals("100000.00", ouverture(ex2026, "1111", false));
        assertEquals("30000.00", ouverture(ex2026, "1191", false));
        assertEquals("130000.00", valeur(ex2026, "BILAN_ACTIF", "ACT.TOTAL_GENERAL", "NET"));
        assertEquals("130000.00", valeur(ex2026, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));
        assertEquals("130000.00", valeur(ex2026, "BILAN_ACTIF", "ACT.514", "NET_N1"));
        assertEquals("0.00", valeur(ex2026, "CPC", "CPC.XVI", "TOTAUX"));
        assertEquals("30000.00", valeur(ex2026, "CPC", "CPC.XVI", "TOTAUX_N1"));

        passer("VT", "2026-01-15", "FV 26", l("5141", null, "10000.00", "0"), l("7111", null, "0", "10000.00"));
        assertEquals("140000.00", valeur(ex2026, "BILAN_ACTIF", "ACT.TOTAL_GENERAL", "NET"));
        assertEquals("140000.00", valeur(ex2026, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));

        RapportControlesDTO rapport = controles.rejouer(ex2026);
        assertEquals(0, rapport.bloquants(), () -> rapport.resultats().toString());
        assertTrue(resultat(rapport, "AN_EGAL_CLOTURE_N1").reussi());
        assertTrue(resultat(rapport, "BILAN_EQUILIBRE_N1").reussi());
        assertTrue(resultat(rapport, "TF_TOTAUX_EGAUX").reussi(), () -> resultat(rapport, "TF_TOTAUX_EGAUX").toString());
        assertFalse(resultat(rapport, "RESULTAT_N1_NON_AFFECTE").reussi());

        fermerPeriodes(ex2026);
        arreter(ex2026);
        Long ex2027 = cloture.cloturer(ex2026, new CloturerRequest("2026")).exerciceSuivantId();
        assertEquals("140000.00", ouverture(ex2027, "5141", true));
        assertEquals("100000.00", ouverture(ex2027, "1111", false), "le capital ne double pas");
        assertEquals("140000.00", valeur(ex2027, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));
    }

    @Test
    void lOuvertureCalculeeDonneLesMemesValeursQueLaClotureReelle() {
        exerciceDeBase();
        fermerPeriodes(ex2025);
        Long ex2026 = exercices.creerSuivant(ex2025, new ExerciceDatesRequest("2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"))).id();
        passer("VT", "2026-01-15", "FV 26", l("5141", null, "10000.00", "0"), l("7111", null, "0", "10000.00"));

        assertTrue(etats.etat(ex2026, "BILAN_ACTIF", null).ouvertureProvisoire());
        List<MesuresRepository.Mesure> calculees = mesuresDe(ex2026);
        String bilan = valeur(ex2026, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET");

        arreter(ex2025);
        cloture.cloturer(ex2025, new CloturerRequest("2025"));

        assertFalse(etats.etat(ex2026, "BILAN_ACTIF", null).ouvertureProvisoire());
        assertEquals(calculees, mesuresDe(ex2026));
        assertEquals(bilan, valeur(ex2026, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));
    }

    @Test
    void unLettrageAChevalSuitSaLigneDANouveau() {
        TiersDTO client = client("Droguerie Moulay Idriss");
        EcritureDTO ouverte = passer("VT", "2025-01-10", "F1", l("3421", client.id(), "1000.00", "0"), l("7111", null, "0", "1000.00"));
        EcritureDTO soldee = passer("VT", "2025-01-11", "F2", l("3421", client.id(), "500.00", "0"), l("7111", null, "0", "500.00"));
        EcritureDTO reglement = passer("BQ", "2025-01-20", "R2", l("5141", null, "500.00", "0"), l("3421", client.id(), "0", "500.00"));
        lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2025-01-20"), List.of(ligneTiers(soldee), ligneTiers(reglement))));
        fermerPeriodes(ex2025);
        Long ex2026 = exercices.creerSuivant(ex2025, new ExerciceDatesRequest("2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"))).id();
        EcritureDTO regleEn2026 = passer("BQ", "2026-01-05", "R1", l("5141", null, "1000.00", "0"), l("3421", client.id(), "0", "1000.00"));
        Long ligneOuverte = ligneTiers(ouverte);
        lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-05"), List.of(ligneOuverte, ligneTiers(regleEn2026))));

        arreter(ex2025);
        ClotureDefinitiveDTO clos = cloture.cloturer(ex2025, new CloturerRequest("2025"));

        List<Map<String, Object>> reprises = jdbc.queryForList("SELECT id, ref_piece, debit, ligne_origine_id FROM compta_lignes WHERE livre_id = ? AND exercice_id = ? AND nature_ecriture = 'A_NOUVEAU' AND compte_numero = '3421'",
                livre.livreId(), ex2026);
        assertEquals(1, reprises.size(), "seule la pièce encore ouverte repart, pièce par pièce : " + reprises);
        assertEquals("F1", reprises.get(0).get("ref_piece"));
        assertEquals(ligneOuverte, reprises.get(0).get("ligne_origine_id"));
        assertEquals(0L, compter("compta_lettrage_lignes WHERE ligne_id = " + ligneOuverte), "la ligne de N a cédé sa place");
        assertEquals(1L, compter("compta_lettrage_lignes WHERE ligne_id = " + reprises.get(0).get("id")));
        assertTrue(mesures.parTiersDuCompte(livre.livreId(), ex2026, List.of(), "3421", LocalDate.parse("2026-12-31")).stream()
                .allMatch(m -> m.sd().signum() == 0 && m.sc().signum() == 0), "le tiers est soldé");
        assertTrue(clos.lignesAnouveau() > 0);
    }

    @Test
    void lesRefusDeLaCloture() {
        exerciceDeBase();
        refus("COMPTA_ARRETE_IMPOSSIBLE", () -> arreter(ex2025));
        refus("COMPTA_EXERCICE_NON_ARRETE", () -> cloture.cloturer(ex2025, new CloturerRequest("2025")));
        refus("COMPTA_CONFIRMATION", () -> cloture.cloturer(ex2025, new CloturerRequest("2024")));

        fermerPeriodes(ex2025);
        Long ex2026 = exercices.creerSuivant(ex2025, new ExerciceDatesRequest("2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"))).id();
        fermerPeriodes(ex2026);
        refus("COMPTA_ARRETE_IMPOSSIBLE", () -> arreter(ex2026));

        BrouillonDTO enAttente = ecritures.creerBrouillon(inventaire("2025-12-31", "INV", false, l("6111", null, "5.00", "0"), l("5141", null, "0", "5.00")));
        refus("COMPTA_ARRETE_IMPOSSIBLE", () -> arreter(ex2025));
        ecritures.supprimerBrouillon(enAttente.id());

        arreter(ex2025);
        BrouillonDTO apresArrete = ecritures.creerBrouillon(inventaire("2025-12-31", "INV", false, l("6111", null, "5.00", "0"), l("5141", null, "0", "5.00")));
        assertThrows(CommonException.class, () -> ecritures.validerBrouillon(apresArrete.id()), "écrire dans un exercice arrêté est refusé");
        ecritures.supprimerBrouillon(apresArrete.id());

        cloture.cloturer(ex2025, new CloturerRequest("2025"));
        List<MesuresRepository.Mesure> avant = mesuresDe(ex2025);
        refus("COMPTA_EXERCICE_CLOS", () -> cloture.cloturer(ex2025, new CloturerRequest("2025")));
        assertEquals(avant, mesuresDe(ex2025));
        DataAccessException rouvrir = assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compta_exercices SET statut = 'OUVERT' WHERE id = ?", ex2025));
        assertTrue(rouvrir.getMostSpecificCause().getMessage().contains("compta_exercice_clos"), rouvrir.getMostSpecificCause().getMessage());
    }

    @Test
    void unCompteASolderEmpecheLaClotureDefinitive() {
        exerciceDeBase();
        passer("BQ", "2025-02-25", "DEPOT", l("5115", null, "1200.00", "0"), l("5141", null, "0", "1200.00"));
        fermerPeriodes(ex2025);
        arreter(ex2025);

        CommonException refus = assertThrows(CommonException.class, () -> cloture.cloturer(ex2025, new CloturerRequest("2025")));
        assertEquals("COMPTA_CONTROLES_BLOQUE_CLOTURE", refus.getErrorCode());
        assertTrue(refus.getMessage().contains("5115"), refus.getMessage());
        assertEquals("ARRETE", exercice(ex2025).statut());
        assertEquals(0L, compter("compta_ecritures WHERE livre_id = " + livre.livreId() + " AND nature = 'CLOTURE'"));
    }

    /** Une panne à chaque étape de la clôture : tout est annulé, et la clôture repasse ensuite. */
    @Test
    void unePanneAChaqueEtapeLaisseLExerciceArrete() {
        exerciceDeBase();
        passer("OD", "2025-02-26", "CHARGE", l("6111", null, "1000.00", "0"), l("5141", null, "0", "1000.00"));
        fermerPeriodes(ex2025);
        passerInventaire("2025-12-31", "CCA", true, l("3491", null, "400.00", "0"), l("6111", null, "0", "400.00"));
        arreter(ex2025);
        long ecrituresAvant = compter("compta_ecritures WHERE livre_id = " + livre.livreId());

        List<String[]> pannes = List.of(
                new String[]{"BEFORE INSERT ON compta_ecritures", "NEW.nature = 'CLOTURE'"},
                new String[]{"BEFORE INSERT ON compta_exercices", "TRUE"},
                new String[]{"BEFORE INSERT ON compta_ecritures", "NEW.nature = 'A_NOUVEAU'"},
                new String[]{"BEFORE UPDATE ON compta_periodes", "NEW.statut = 'CLOTUREE' AND NEW.type_periode = 'CLOTURE'"},
                new String[]{"BEFORE UPDATE ON compta_exercices", "NEW.statut = 'CLOTURE'"},
                new String[]{"BEFORE INSERT ON compta_ecritures", "NEW.contrepassation_de_id IS NOT NULL"});
        for (String[] panne : pannes) {
            poserPanne(panne[0], panne[1]);
            try {
                assertThrows(RuntimeException.class, () -> cloture.cloturer(ex2025, new CloturerRequest("2025")), panne[1]);
            } finally {
                retirerPanne(panne[0]);
            }
            assertEquals("ARRETE", exercice(ex2025).statut(), panne[1]);
            assertEquals(ecrituresAvant, compter("compta_ecritures WHERE livre_id = " + livre.livreId()), panne[1] + " : aucune écriture partielle");
            assertEquals(1L, compter("compta_exercices WHERE livre_id = " + livre.livreId()), panne[1] + " : pas d'exercice suivant");
            assertEquals(0L, compter("compta_archives_cloture WHERE livre_id = " + livre.livreId()), panne[1]);
        }

        ClotureDefinitiveDTO clos = cloture.cloturer(ex2025, new CloturerRequest("2025"));
        assertEquals(1, clos.extournes());
        assertEquals("CLOTURE", exercice(ex2025).statut());
    }

    @Test
    void unePanneDeSubstitutionDuLettrageAnnuleAussiTout() {
        TiersDTO client = client("Bazar Anfa");
        EcritureDTO facture = passer("VT", "2025-01-10", "F1", l("3421", client.id(), "1000.00", "0"), l("7111", null, "0", "1000.00"));
        fermerPeriodes(ex2025);
        exercices.creerSuivant(ex2025, new ExerciceDatesRequest("2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")));
        EcritureDTO reglement = passer("BQ", "2026-01-05", "R1", l("5141", null, "1000.00", "0"), l("3421", client.id(), "0", "1000.00"));
        lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-05"), List.of(ligneTiers(facture), ligneTiers(reglement))));
        arreter(ex2025);

        poserPanne("BEFORE INSERT ON compta_lettrage_lignes", "TRUE");
        try {
            assertThrows(RuntimeException.class, () -> cloture.cloturer(ex2025, new CloturerRequest("2025")));
        } finally {
            retirerPanne("BEFORE INSERT ON compta_lettrage_lignes");
        }
        assertEquals("ARRETE", exercice(ex2025).statut());
        assertEquals(1L, compter("compta_lettrage_lignes WHERE ligne_id = " + ligneTiers(facture)));
        assertEquals(0L, compter("compta_ecritures WHERE livre_id = " + livre.livreId() + " AND nature IN ('CLOTURE', 'A_NOUVEAU')"));
    }

    @Test
    void uneArchiveEnPanneLaisseLExerciceClosEtSeRejoue() {
        exerciceDeBase();
        fermerPeriodes(ex2025);
        arreter(ex2025);

        poserPanne("BEFORE INSERT ON compta_archives_cloture", "TRUE");
        ClotureDefinitiveDTO clos;
        try {
            clos = cloture.cloturer(ex2025, new CloturerRequest("2025"));
        } finally {
            retirerPanne("BEFORE INSERT ON compta_archives_cloture");
        }
        assertEquals("CLOTURE", clos.statut());
        assertNull(clos.sha256Fec());
        assertNull(cloture.etat(ex2025).sha256Fec());
        assertFalse(resultat(controles.rejouer(ex2025), "ARCHIVE_ABSENTE").reussi());

        String sha = cloture.archiver(ex2025);
        assertNotNull(sha);
        assertEquals(sha, cloture.etat(ex2025).sha256Fec());
        assertTrue(resultat(controles.rejouer(ex2025), "ARCHIVE_ABSENTE").reussi());
        assertEquals(sha, cloture.archiver(ex2025), "rejouer une archive présente ne réécrit rien");
        assertEquals(2L, compter("compta_archives_cloture WHERE exercice_id = " + ex2025));
    }

    @Test
    void leFecRegenereDUnExerciceClosALaMemeEmpreinteQueLArchive() {
        TiersDTO client = client("Bazar Anfa");
        EcritureDTO facture = passer("VT", "2025-01-10", "F1", l("3421", client.id(), "1000.00", "0"), l("7111", null, "0", "1000.00"));
        EcritureDTO reglement = passer("BQ", "2025-01-20", "R1", l("5141", null, "1000.00", "0"), l("3421", client.id(), "0", "1000.00"));
        lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2025-01-20"), List.of(ligneTiers(facture), ligneTiers(reglement))));
        fermerPeriodes(ex2025);
        arreter(ex2025);
        ClotureDefinitiveDTO clos = cloture.cloturer(ex2025, new CloturerRequest("2025"));

        assertEquals(clos.sha256Fec(), cloture.empreinteFec(livre.livreId(), ex2025));
        // L'exercice suivant vit sa vie : le FEC de l'exercice clos ne bouge pas
        passer("VT", "2026-01-15", "FV 26", l("5141", null, "10.00", "0"), l("7111", null, "0", "10.00"));
        assertEquals(clos.sha256Fec(), cloture.empreinteFec(livre.livreId(), ex2025));
    }

    @Test
    void leverLArreteDemandeUnMotifEtLeFigeageSuivantPrendLeRelais() {
        exerciceDeBase();
        fermerPeriodes(ex2025);
        aucunStock(ex2025);
        assertTrue(cloture.etat(ex2025).peutArreter());
        assertEquals(1, arreter(ex2025).figeage());
        ClotureExerciceDTO arrete = cloture.etat(ex2025);
        assertTrue(arrete.peutLever());
        assertTrue(arrete.peutCloturer());
        assertFalse(arrete.peutArreter());

        assertEquals("OUVERT", cloture.leverArrete(ex2025, new LeverArreteRequest("Provision oubliée")).statut());
        assertEquals("CALCUL", etats.etat(ex2025, "CPC", null).origine());
        assertEquals(1L, compter("compta_etats_figes WHERE exercice_id = " + ex2025 + " AND figeage = 1 AND code_etat = 'CPC'"), "rien n'est supprimé");

        passerInventaire("2025-12-31", "PROV", false, l("61964", null, "1000.00", "0"), l("3942", null, "0", "1000.00"));
        ArreteDTO second = arreter(ex2025);
        assertEquals(2, second.figeage());
        assertEquals(new BigDecimal("29000.00"), second.resultatNet());
        assertEquals("29000.00", valeur(ex2025, "CPC", "CPC.XVI", "TOTAUX"));
        assertEquals(2, etats.etat(ex2025, "CPC", null).figeage());
    }

    @Test
    void uneChargeConstateeDAvanceSExtourneEnN1() {
        exerciceDeBase();
        fermerPeriodes(ex2025);
        EcritureDTO cca = passerInventaire("2025-12-31", "CCA", true, l("3491", null, "1000.00", "0"), l("6111", null, "0", "1000.00"));
        assertTrue(cca.aExtourner());
        arreter(ex2025);

        ClotureDefinitiveDTO clos = cloture.cloturer(ex2025, new CloturerRequest("2025"));

        assertEquals(1, clos.extournes());
        Long ex2026 = clos.exerciceSuivantId();
        List<Map<String, Object>> miroir = jdbc.queryForList("SELECT date_ecriture, est_contrepassation, nature FROM compta_ecritures WHERE contrepassation_de_id = ?", cca.id());
        assertEquals(1, miroir.size());
        assertEquals(Boolean.FALSE, miroir.get(0).get("est_contrepassation"), "un vrai mouvement de N+1");
        assertEquals("COURANTE", miroir.get(0).get("nature"));
        MesuresRepository.Mesure stock = mesure(ex2026, "3491");
        assertEquals(0, stock.sd().signum(), "3491 ressort à zéro");
        assertEquals(0, stock.sc().signum());
        assertEquals(0, new BigDecimal("1000.00").compareTo(mesure(ex2026, "6111").md()), "la charge revient sur N+1");
    }

    @Test
    void lAffectationDUnBeneficeSoldeLeResultat() {
        exerciceDeBase();
        fermerPeriodes(ex2025);
        arreter(ex2025);
        Long ex2026 = cloture.cloturer(ex2025, new CloturerRequest("2025")).exerciceSuivantId();
        assertEquals(0, new BigDecimal("30000.00").compareTo(cloture.etat(ex2025).resteAAffecter()));

        LocalDate decision = LocalDate.parse("2026-01-20");
        refus("COMPTA_AFFECTATION_ECART", () -> cloture.affecter(ex2025, new AffectationRequest(decision, List.of(a("1161", "100.00")))));
        refus("COMPTA_AFFECTATION_INVALIDE", () -> cloture.affecter(ex2025, new AffectationRequest(decision, List.of(a("5141", "30000.00")))));

        EcritureDTO e = cloture.affecter(ex2025, new AffectationRequest(decision, List.of(a("1140", "1500.00"), a("1161", "8500.00"), a("4465", "20000.00"))));

        assertEquals("AFFECTATION", e.origine());
        assertEquals(0, cloture.etat(ex2025).resteAAffecter().signum());
        refus("COMPTA_AFFECTATION_FAITE", () -> cloture.affecter(ex2025, new AffectationRequest(decision, List.of(a("1161", "1.00")))));
        assertEquals(0, mesure(ex2026, "1191").sc().signum());
        assertEquals("130000.00", valeur(ex2026, "BILAN_PASSIF", "PAS.TOTAL_GENERAL", "NET"));
        RapportControlesDTO rapport = controles.rejouer(ex2026);
        assertTrue(resultat(rapport, "RESULTAT_N1_NON_AFFECTE").reussi());
        assertTrue(resultat(rapport, "DISTRIBUTION_T14").reussi(), () -> resultat(rapport, "DISTRIBUTION_T14").toString());
        assertTrue(e.lignes().stream().filter(l -> l.compte().numero().equals("4465")).findFirst().map(l -> !l.horsFlux()).orElse(false), "les dividendes sont un flux");
    }

    @Test
    void lAffectationDUnePerteVaSurLeReportDebiteur() {
        passer("OD", "2025-01-10", "APPORT", l("5141", null, "100000.00", "0"), l("1111", null, "0", "100000.00"));
        passer("OD", "2025-01-11", "CHARGE", l("6111", null, "4000.00", "0"), l("5141", null, "0", "4000.00"));
        fermerPeriodes(ex2025);
        arreter(ex2025);
        Long ex2026 = cloture.cloturer(ex2025, new CloturerRequest("2025")).exerciceSuivantId();
        assertEquals(0, new BigDecimal("-4000.00").compareTo(cloture.etat(ex2025).resteAAffecter()));
        refus("COMPTA_AFFECTATION_INVALIDE", () -> cloture.affecter(ex2025, new AffectationRequest(LocalDate.parse("2026-01-20"), List.of(a("1161", "4000.00")))));

        cloture.affecter(ex2025, new AffectationRequest(LocalDate.parse("2026-01-20"), List.of(a("1169", "4000.00"))));

        assertEquals(0, mesure(ex2026, "1199").sd().signum());
        assertEquals(0, new BigDecimal("4000.00").compareTo(mesure(ex2026, "1169").sd()));
    }

    private void exerciceDeBase() {
        passer("OD", "2025-01-10", "APPORT", l("5141", null, "100000.00", "0"), l("1111", null, "0", "100000.00"));
        passer("VT", "2025-02-10", "FV 1", l("5141", null, "50000.00", "0"), l("7111", null, "0", "50000.00"));
        passer("OD", "2025-02-20", "ACHAT", l("6111", null, "20000.00", "0"), l("5141", null, "0", "20000.00"));
    }

    private void aucunStock(Long exerciceId) {
        if (!"VALIDE".equals(inventaires.lire(exerciceId).statut())) {
            inventaires.enregistrer(exerciceId, new com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest(null, true, java.util.List.of()));
            inventaires.valider(exerciceId);
        }
    }

    private void fermerPeriodes(Long exerciceId) {
        for (PeriodeDTO p : exercices.periodes(exerciceId)) {
            if ("COURANTE".equals(p.typePeriode()) && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
    }

    private ArreteDTO arreter(Long exerciceId) {
        aucunStock(exerciceId);
        return cloture.arreter(exerciceId, new ArreterRequest("Aucun impôt dans cet essai.", null));
    }

    private void poserPanne(String ou, String condition) {
        jdbc.execute("CREATE OR REPLACE FUNCTION compta_essai_panne() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'panne simulée'; END $$");
        jdbc.execute("CREATE TRIGGER trg_compta_essai_panne " + ou + " FOR EACH ROW WHEN (" + condition + ") EXECUTE FUNCTION compta_essai_panne()");
    }

    private void retirerPanne(String ou) {
        jdbc.execute("DROP TRIGGER trg_compta_essai_panne ON " + ou.substring(ou.lastIndexOf(' ') + 1));
    }

    private ExerciceDTO exercice(Long id) {
        return exercices.lister().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }

    private List<MesuresRepository.Mesure> mesuresDe(Long exerciceId) {
        return mesures.parCompte(livre.livreId(), exerciceId, mesures.chaineNonClose(livre.livreId(), exerciceId), exercice(exerciceId).au(), true);
    }

    private MesuresRepository.Mesure mesure(Long exerciceId, String compte) {
        return mesuresDe(exerciceId).stream().filter(m -> m.compte().equals(compte)).findFirst().orElseThrow(() -> new AssertionError(compte));
    }

    private String ouverture(Long exerciceId, String compte, boolean debit) {
        MesuresRepository.Mesure m = mesure(exerciceId, compte);
        return (debit ? m.od() : m.oc()).toPlainString();
    }

    private String valeur(Long exerciceId, String etat, String ligne, String colonne) {
        return etats.etat(exerciceId, etat, null).lignes().stream().filter(l -> l.code().equals(ligne)).findFirst().orElseThrow().valeurs().get(colonne);
    }

    private static ResultatControleDTO resultat(RapportControlesDTO rapport, String code) {
        return rapport.resultats().stream().filter(r -> r.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError(code));
    }

    private TiersDTO client(String nom) {
        return tiers.creer(new TiersCreationRequest("CLIENT", nom, null, null, "3421", null, null, null));
    }

    private EcritureDTO passer(String journalCode, String date, String piece, BrouillonLigneRequest... lignes) {
        LocalDate jour = LocalDate.parse(date);
        BrouillonDTO b = ecritures.creerBrouillon(new BrouillonRequest(journal.get(journalCode), jour, jour, piece, "Écriture " + piece, "COURANTE", null, null, List.of(lignes)));
        return ecritures.validerBrouillon(b.id());
    }

    private BrouillonRequest inventaire(String date, String piece, boolean aExtourner, BrouillonLigneRequest... lignes) {
        LocalDate jour = LocalDate.parse(date);
        return new BrouillonRequest(journal.get("OD"), jour, jour, piece, "Inventaire " + piece, "INVENTAIRE", null, aExtourner, List.of(lignes));
    }

    private EcritureDTO passerInventaire(String date, String piece, boolean aExtourner, BrouillonLigneRequest... lignes) {
        return ecritures.validerBrouillon(ecritures.creerBrouillon(inventaire(date, piece, aExtourner, lignes)).id());
    }

    private static BrouillonLigneRequest l(String compte, Long tiersId, String debit, String credit) {
        return new BrouillonLigneRequest(compte, tiersId, new BigDecimal(debit), new BigDecimal(credit), null, null, null, null, null, null, null, null);
    }

    private static AffectationLigneRequest a(String compte, String montant) {
        return new AffectationLigneRequest(compte, new BigDecimal(montant));
    }

    private static Long ligneTiers(EcritureDTO e) {
        return e.lignes().stream().filter(l -> l.tiers() != null).findFirst().orElseThrow().id();
    }

    private long compter(String deOu) {
        Long nombre = jdbc.queryForObject("SELECT COUNT(*) FROM " + deOu, Long.class);
        return nombre == null ? 0 : nombre;
    }

    private static void refus(String code, Runnable action) {
        CommonException refus = assertThrows(CommonException.class, action::run, code);
        assertEquals(code, refus.getErrorCode(), refus.getMessage());
    }
}
