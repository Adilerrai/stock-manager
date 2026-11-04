package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.AcomptesDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.CelluleSaisieDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DeficitRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.DepotLiasseRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.IsDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LiasseDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LigneRegistreDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LiquidationIsRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.PaiementAcompteRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.RegistreRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.SaisiesRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.TableauDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.TableauResumeDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.ValeurRegistreDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneInventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.SortieRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ClotureService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.EtatService;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.ImmobilisationComptaService;
import com.gestion.service.compta.InventaireClotureService;
import com.gestion.service.compta.IsComptaService;
import com.gestion.service.compta.LiasseComptaService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.PeriodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FiscalComptaIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2025-01-01");
    private static final LocalDate AU = LocalDate.parse("2025-12-31");

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private IsComptaService is;
    @Autowired private LiasseComptaService liasse;
    @Autowired private EtatService etats;
    @Autowired private ClotureService cloture;
    @Autowired private InventaireClotureService inventaires;
    @Autowired private ImmobilisationComptaService immobilisations;
    @Autowired private EcritureService ecritures;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private JournauxRepository journaux;
    @Autowired private TiersRepository tiers;
    @Autowired private JdbcTemplate jdbc;

    private LivreDTO livre;
    private Long exercice;
    private Long client;
    private Long fournisseur;
    private int numero = 1;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(societe.pointDeVenteId());
        abonnements.modifier(societe.pointDeVenteId(), true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2025", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, "Rue des essais", "Casablanca", "Négoce", null, null, "GENERAL")));
        exercice = livre.exerciceCourantId();
        client = tiers.creer(livre.livreId(), new TiersCreationRequest("CLIENT", "Droguerie", null, null, "3421", null, null, null));
        fournisseur = tiers.creer(livre.livreId(), new TiersCreationRequest("FOURNISSEUR", "Sebou", null, null, "4411", null, null, null));
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    // --- S15

    /** Le capital de S16 : sans lui, la banque finit créditrice et la clôture la refuse. */
    private void capital() {
        passer("2025-01-02", "BQ", "5141", null, "1111", null, "100000.00");
    }

    /** Les écritures de S15 : 490 000 de résultat avant IS, dont 40 000 de pénalités sur 6583. */
    private void s15(String comptePenalite) {
        passer("2025-01-15", "AC", "6111", null, "4411", fournisseur, "500000.00");
        passer("2025-02-10", "VT", "3421", client, "7111", null, "1000000.00");
        passer("2025-02-20", "BQ", comptePenalite, null, "5141", null, "40000.00");
        fermerPeriodes(exercice);
        inventaires.enregistrer(exercice, new InventaireRequest("PRIX_ACHAT_COURANT", false, List.of(new LigneInventaireDTO(null, "3111", null, null, "Stock", BigDecimal.ONE, new BigDecimal("30000"), null, null))));
        inventaires.valider(exercice);
    }

    @Test
    void s15LeCpcEtLEsgDonnentLeMemeResultat() {
        s15("65831");

        montant("490000.00", valeur(etats.etat(exercice, "CPC", null), "CPC.XVI", "TOTAUX"));
        montant("490000.00", valeur(etats.etat(exercice, "ESG_TFR", null), "TFR.X", "N"));
        montant("470000.00", valeur(etats.etat(exercice, "ESG_TFR", null), "TFR.2", "N"));
    }

    @Test
    void s15LesReintegrationsDeT3PortentLaBaseA530000() {
        s15("65831");
        liasse.saisir(exercice, "T3", new SaisiesRequest(List.of(new CelluleSaisieDTO("T3.REINT_COUR.1", "MONTANT_1", new BigDecimal("40000.00"), "Pénalités"))));

        IsDTO i = is.calculer(exercice);

        montant("490000.00", i.resultatComptable());
        montant("40000.00", i.reintegrations());
        montant("530000.00", i.resultatFiscal());
        montant("530000.00", i.baseArrondie());
        assertEquals("BN_300K_A_1M", i.categorieTaux());
        montant("20", i.tauxIs());
        montant("106000.00", i.isTheorique());
        montant("3000.00", i.cm(), "2 500 sur la base de 1 000 000, relevés au minimum");
        assertFalse(i.sources().isEmpty(), "chaque taux porte sa source");
        // T3 tient le même raisonnement, sur la même chaîne
        TableauDTO t3 = liasse.tableau(exercice, "T3");
        montant("530000.00", cellule(t3, "T3.BENEF_NET", "MONTANT_1"));
        montant("40000.00", cellule(t3, "T3.II", "MONTANT_1"));
    }

    @Test
    void s15LaMemeChargeSur6585SansReintegrationResteA490000() {
        s15("6585");

        IsDTO i = is.calculer(exercice);

        montant("0", i.reintegrations());
        montant("490000.00", i.baseArrondie());
        montant("98000.00", i.isTheorique());
    }

    @Test
    void lIsValidePasseSaChargeEtT3ReprendLImpot() {
        s15("65831");
        liasse.saisir(exercice, "T3", new SaisiesRequest(List.of(new CelluleSaisieDTO("T3.REINT_COUR.1", "MONTANT_1", new BigDecimal("40000.00"), "Pénalités"))));

        IsDTO valide = is.valider(exercice);

        assertEquals("VALIDE", valide.statut());
        assertNotNull(valide.ecritureChargeId());
        solde("6701", "106000.00");
        solde("4453", "-106000.00");
        assertEquals("INVENTAIRE", jdbc.queryForObject("SELECT nature FROM compta_ecritures WHERE id = ?", String.class, valide.ecritureChargeId()));
        // Le résultat net a baissé de l'impôt, mais la base ne bouge pas : T3 réintègre l'impôt
        montant("384000.00", valeur(etats.etat(exercice, "CPC", null), "CPC.XVI", "TOTAUX"));
        montant("530000.00", cellule(liasse.tableau(exercice, "T3"), "T3.BENEF_NET", "MONTANT_1"));
        assertEquals("COMPTA_IS_VALIDE", assertThrows(CommonException.class, () -> is.calculer(exercice)).getErrorCode());
        assertEquals("COMPTA_LIASSE_SAISIE", assertThrows(CommonException.class, () -> liasse.saisir(exercice, "T3", new SaisiesRequest(List.of(new CelluleSaisieDTO(IsComptaService.LIGNE_IS_T3, "MONTANT_1", BigDecimal.ONE, null))))).getErrorCode());

        IsDTO rouvert = is.rouvrir(exercice, new MotifRequest("Réintégration oubliée"));

        assertEquals("BROUILLON", rouvert.statut());
        solde("6701", "0");
        solde("4453", "0");
        montant("490000.00", valeur(etats.etat(exercice, "CPC", null), "CPC.XVI", "TOTAUX"));
        montant("530000.00", is.calculer(exercice).baseArrondie());
    }

    // --- S16

    @Test
    void s16ApresClotureRienNeBouge() {
        capital();
        s15("65831");
        liasse.saisir(exercice, "T3", new SaisiesRequest(List.of(new CelluleSaisieDTO("T3.REINT_COUR.1", "MONTANT_1", new BigDecimal("40000.00"), "Pénalités"))));
        is.calculer(exercice);
        BigDecimal actifAvant = valeur(etats.etat(exercice, "BILAN_ACTIF", null), "ACT.TOTAL_GENERAL", "NET");
        montant("1090000.00", actifAvant);

        cloture.arreter(exercice, new ArreterRequest("IS calculé, pas encore passé pour cet essai", null));
        cloture.cloturer(exercice, new CloturerRequest("2025"));

        montant("490000.00", valeur(etats.etat(exercice, "CPC", null), "CPC.XVI", "TOTAUX"));
        montant("1090000.00", valeur(etats.etat(exercice, "BILAN_ACTIF", null), "ACT.TOTAL_GENERAL", "NET"));
        montant("1090000.00", valeur(etats.etat(exercice, "BILAN_PASSIF", null), "PAS.TOTAL_GENERAL", "NET"));
        // Pas de plancher de 3 000 par erreur, ni de CPC relu à zéro : le calcul reste celui d'avant la clôture
        montant("106000.00", is.lire(exercice).isTheorique());
        montant("490000.00", is.lire(exercice).resultatComptable());
        ExerciceDTO suivant = exercices.lister().stream().filter(e -> "2026".equals(e.code())).findFirst().orElseThrow();
        montant("100000.00", valeur(etats.etat(suivant.id(), "BILAN_PASSIF", null), "PAS.111", "NET"));
    }

    // --- Taux, acomptes, déficits

    @Test
    void unExerciceSansTauxALaDateRefuseLeCalcul() {
        SocieteDeTest ancienne = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(ancienne.pointDeVenteId());
        abonnements.modifier(ancienne.pointDeVenteId(), true);
        LocalDate du = LocalDate.parse("2020-01-01");
        LivreDTO l = livres.activer(new ActivationLivreRequest(du, new ExerciceDatesRequest("2020", du, LocalDate.parse("2020-12-31")),
                new IdentiteLivreDTO("Ancienne SARL", "SARL", "99900003", "999000000000003", null, null, null, "Fès", "Négoce", null, null, "GENERAL")));

        assertEquals("COMPTA_PARAMETRE_ABSENT", assertThrows(CommonException.class, () -> is.calculer(l.exerciceCourantId())).getErrorCode());
    }

    @Test
    void unPremierExerciceNACalculeAucunAcompte() {
        AcomptesDTO a = is.acomptes(exercice);

        assertNull(a.exerciceReference());
        assertTrue(a.acomptes().isEmpty());
        assertTrue(a.raison().contains("référence"));
    }

    @Test
    void lesAcomptesViennentDeLExerciceClosEtSePaient() {
        capital();
        s15("6585");
        is.valider(exercice);
        cloture.arreter(exercice, new ArreterRequest(null, null));
        cloture.cloturer(exercice, new CloturerRequest("2025"));
        ExerciceDTO suivant = exercices.lister().stream().filter(e -> "2026".equals(e.code())).findFirst().orElseThrow();

        AcomptesDTO a = is.acomptes(suivant.id());

        assertEquals("2025", a.exerciceReference());
        montant("490000.00", a.beneficeReference());
        assertEquals(4, a.acomptes().size());
        assertEquals(LocalDate.parse("2026-03-31"), a.acomptes().get(0).echeance());
        assertEquals(LocalDate.parse("2026-12-31"), a.acomptes().get(3).echeance());
        // 490 000 au taux de 2026 (20 %) : 98 000, soit 24 500 par acompte
        montant("24500.00", a.acomptes().get(0).montantDu());

        AcomptesDTO paye = is.payerAcompte(suivant.id(), 1, new PaiementAcompteRequest(LocalDate.parse("2026-01-15"), new BigDecimal("24500.00"), "QUIT-1", null));

        assertNotNull(paye.acomptes().get(0).ecritureId());
        BigDecimal auCompte = jdbc.queryForObject("SELECT SUM(debit - credit) FROM compta_lignes WHERE livre_id = ? AND compte_numero = '3453' AND nature_ecriture = 'COURANTE'", BigDecimal.class, livre.livreId());
        montant("24500.00", auCompte);
        assertEquals("COMPTA_IS_ACOMPTE_PAYE", assertThrows(CommonException.class, () -> is.payerAcompte(suivant.id(), 1, new PaiementAcompteRequest(LocalDate.parse("2026-01-16"), BigDecimal.ONE, null, null))).getErrorCode());
    }

    @Test
    void laLiquidationSoldeLImpotEnN1() {
        capital();
        s15("6585");
        IsDTO valide = is.valider(exercice);
        montant("98000.00", valide.impotDu());
        cloture.arreter(exercice, new ArreterRequest(null, null));
        cloture.cloturer(exercice, new CloturerRequest("2025"));

        IsDTO liquide = is.liquider(exercice, new LiquidationIsRequest(LocalDate.parse("2026-01-20"), null));

        assertNotNull(liquide.ecritureLiquidationId());
        BigDecimal dette = jdbc.queryForObject("SELECT SUM(debit - credit) FROM compta_lignes l JOIN compta_exercices e ON e.id = l.exercice_id AND e.code = '2026' WHERE l.livre_id = ? AND l.compte_numero = '4453'", BigDecimal.class, livre.livreId());
        montant("0", dette);
        assertEquals("COMPTA_IS_LIQUIDE", assertThrows(CommonException.class, () -> is.liquider(exercice, new LiquidationIsRequest(LocalDate.parse("2026-01-21"), null))).getErrorCode());
    }

    @Test
    void unDeficitAnterieurSImputeEtSeRestitue() {
        is.saisirDeficit(new DeficitRequest(2023, "HORS_AMORT", new BigDecimal("100000.00")));
        is.saisirDeficit(new DeficitRequest(2019, "HORS_AMORT", new BigDecimal("50000.00")));
        is.saisirDeficit(new DeficitRequest(2019, "AMORTISSEMENTS", new BigDecimal("20000.00")));
        s15("6585");

        IsDTO i = is.calculer(exercice);

        // 2019 hors amortissements est trop ancien ; 2019 en amortissements reste imputable (art. 12)
        montant("120000.00", i.deficitsImputes());
        montant("370000.00", i.resultatFiscal());
        is.valider(exercice);
        assertEquals(0, new BigDecimal("0").compareTo(is.deficits().stream().filter(d -> d.anneeOrigine() == 2023).findFirst().orElseThrow().montantRestant()));
        montant("100000.00", cellule(liasse.tableau(exercice, "T3"), "T3.REPORT_N2", "MONTANT_1"));

        is.rouvrir(exercice, new MotifRequest("Essai"));

        montant("100000.00", is.deficits().stream().filter(d -> d.anneeOrigine() == 2023).findFirst().orElseThrow().montantRestant());
    }

    @Test
    void unResultatFiscalNegatifDevientUnDeficit() {
        passer("2025-01-15", "AC", "6111", null, "4411", fournisseur, "1000.00");
        fermerPeriodes(exercice);

        IsDTO i = is.valider(exercice);

        montant("-1000.00", i.resultatFiscal());
        montant("3000.00", i.impotDu());
        assertEquals("HORS_AMORT", is.deficits().get(0).nature());
        montant("1000.00", is.deficits().get(0).montantRestant());
    }

    // --- Arrêté

    @Test
    void lArreteDemandeLIsValideOuUnMotif() {
        fermerPeriodes(exercice);
        inventaires.enregistrer(exercice, new InventaireRequest(null, true, List.of()));
        inventaires.valider(exercice);

        CommonException refus = assertThrows(CommonException.class, () -> cloture.arreter(exercice, new ArreterRequest(" ", null)));

        assertEquals("COMPTA_ARRETE_IMPOSSIBLE", refus.getErrorCode());
        assertTrue(refus.getMessage().contains("IS"), refus::getMessage);
        assertEquals("A_FAIRE", cloture.etat(exercice).etapes().stream().filter(e -> "IS".equals(e.code())).findFirst().orElseThrow().etat());
        is.valider(exercice);
        assertEquals("FAIT", cloture.etat(exercice).etapes().stream().filter(e -> "IS".equals(e.code())).findFirst().orElseThrow().etat());
        assertEquals("ARRETE", cloture.arreter(exercice, new ArreterRequest(null, null)).statut());
    }

    // --- Liasse

    private TableauResumeDTO resume(LiasseDTO l, String code) {
        return l.tableaux().stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError(code));
    }

    @Test
    void chaqueTableauPorteSonStatut() {
        LiasseDTO l = liasse.liste(exercice);

        assertTrue(l.nonValideEdi());
        assertEquals("CALCULE", resume(l, "T8").statut());
        assertEquals("A", resume(l, "T8").categorie());
        assertEquals("A_COMPLETER", resume(l, "T3").statut());
        assertEquals("A_COMPLETER", resume(l, "T13").statut());
        assertEquals("NON_APPLICABLE", resume(l, "T15").statut());
        assertEquals("CALCULE", resume(l, "T10").statut());
    }

    @Test
    void unTableauNonServiPorteUneNoteTechniqueJamaisDesZeros() {
        LiasseDTO l = liasse.liste(exercice);

        TableauResumeDTO b6 = resume(l, "B6");
        assertEquals("NOTE_TECHNIQUE", b6.statut());
        assertNotNull(b6.raison());
        TableauDTO t = liasse.tableau(exercice, "C4");
        assertEquals("NOTE_TECHNIQUE", t.statut());
        assertNotNull(t.raison());
        t.lignes().forEach(x -> x.valeurs().values().forEach(v -> assertFalse(v != null && new BigDecimal(v).signum() == 0, () -> "un zéro dans " + x.code())));
    }

    @Test
    void lesCellulesSaisiesSeRelisentEtSeRetirent() {
        TableauDTO avant = liasse.tableau(exercice, "A1");
        assertFalse(avant.saisissables().isEmpty(), "A1 se saisit");
        String[] cellule = avant.saisissables().get(0).split("\\|");

        TableauDTO apres = liasse.saisir(exercice, "A1", new SaisiesRequest(List.of(new CelluleSaisieDTO(cellule[0], cellule[1], null, "Coût d'acquisition"))));

        assertEquals("SAISI", apres.statut());
        assertEquals("Coût d'acquisition", apres.lignes().stream().filter(x -> x.code().equals(cellule[0])).findFirst().orElseThrow().valeurs().get(cellule[1]));
        TableauDTO retire = liasse.saisir(exercice, "A1", new SaisiesRequest(List.of(new CelluleSaisieDTO(cellule[0], cellule[1], null, " "))));
        assertEquals("A_COMPLETER", retire.statut());
        assertEquals("COMPTA_LIASSE_SAISIE", assertThrows(CommonException.class, () -> liasse.saisir(exercice, "A1", new SaisiesRequest(List.of(new CelluleSaisieDTO("A1.INCONNUE", "TEXTE", null, "X"))))).getErrorCode());
    }

    @Test
    void lesDonneesPersonnellesDuCapitalNEntrentPasDansLaTrace() {
        String secret = "AB123456";
        liasse.enregistrerRegistre(exercice, "T13", new RegistreRequest(List.of(new LigneRegistreDTO(1, List.of(new ValeurRegistreDTO("NOM", "Associé fictif"), new ValeurRegistreDTO("CIN_CE", secret))))));

        TableauDTO t = liasse.tableau(exercice, "T13");

        assertEquals("SAISI", t.statut());
        assertEquals(1, t.registre().size());
        Long traces = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE CAST(audit_log AS text) LIKE ?", Long.class, "%" + secret + "%");
        assertEquals(0L, traces);
        assertEquals("COMPTA_LIASSE_SAISIE", assertThrows(CommonException.class, () -> liasse.enregistrerRegistre(exercice, "T13", new RegistreRequest(List.of(new LigneRegistreDTO(1, List.of(new ValeurRegistreDTO("INCONNUE", "X"))))))).getErrorCode());
        assertEquals("COMPTA_LIASSE_SAISIE", assertThrows(CommonException.class, () -> liasse.enregistrerRegistre(exercice, "T10", new RegistreRequest(List.of()))).getErrorCode(), "T10 vient du registre des immobilisations");
    }

    @Test
    void t10EtT16LisentLeRegistreDesImmobilisations() {
        var f = immobilisations.creer(new ImmobilisationRequest("Chariot", "23321", null, null, LocalDate.parse("2025-01-02"), LocalDate.parse("2025-01-01"), new BigDecimal("60000.00"), BigDecimal.ZERO, BigDecimal.ZERO, "LINEAIRE", 60, null, "BANQUE", null, "FA-1", null, null, null));
        var cede = immobilisations.creer(new ImmobilisationRequest("Bureau", "2351", null, null, LocalDate.parse("2025-01-02"), LocalDate.parse("2025-01-01"), new BigDecimal("12000.00"), BigDecimal.ZERO, BigDecimal.ZERO, "LINEAIRE", 60, null, "BANQUE", null, "FA-2", null, null, null));
        immobilisations.sortir(cede.id(), new SortieRequest("CESSION", LocalDate.parse("2025-02-28"), new BigDecimal("13000.00"), BigDecimal.ZERO, null));
        fermerPeriodes(exercice);
        immobilisations.dotations(exercice, new DotationsRequest(false));

        TableauDTO t10 = liasse.tableau(exercice, "T10");
        assertTrue(t10.registreLectureSeule());
        assertEquals(1, t10.registre().size());
        assertEquals("13000.00", valeurRegistre(t10.registre().get(0), "PRODUIT"));
        assertNotNull(valeurRegistre(t10.registre().get(0), "PLUS"), "une plus-value");

        TableauDTO t16 = liasse.tableau(exercice, "T16");
        assertEquals(2, t16.registre().size());
        LigneRegistreDTO chariot = t16.registre().stream().filter(l -> "Chariot".equals(valeurRegistre(l, "IMMOBILISATION"))).findFirst().orElseThrow();
        assertEquals("60000.00", valeurRegistre(chariot, "PRIX"));
        assertEquals("20.00", valeurRegistre(chariot, "TAUX"));
        assertEquals(f.designation(), valeurRegistre(chariot, "IMMOBILISATION"));
    }

    @Test
    void lesCadragesSontCalculesOuDitsNonEffectues() {
        passer("2025-01-15", "OD", "6111", null, "4411", fournisseur, "1000.00");

        TableauDTO t8 = liasse.tableau(exercice, "T8");

        assertFalse(t8.cadrages().isEmpty());
        assertEquals(Boolean.TRUE, t8.cadrages().get(0).reussi());
    }

    @Test
    void leFichierResteMarqueNonValide() {
        passer("2025-02-10", "VT", "3421", client, "7111", null, "1000.00");

        String xml = new String(liasse.produireXml(exercice), StandardCharsets.UTF_8);

        assertTrue(xml.contains(LiasseComptaService.MARQUE_NON_VALIDE));
        assertTrue(xml.contains("<identifiantFiscal>99900001</identifiantFiscal>"));
        assertTrue(xml.contains("<exerciceFiscalDu>2025-01-01</exerciceFiscalDu>"));
        assertTrue(xml.contains("<ValeurCellule>"));
        LiasseDTO l = liasse.liste(exercice);
        assertTrue(l.nonValideEdi(), "non_valide_edi reste vrai");
        assertNotNull(l.xmlSha256());
        assertEquals("DEPOSEE", liasse.deposer(exercice, new DepotLiasseRequest(LocalDate.parse("2026-03-30"), "R-1")).statut());
        assertTrue(liasse.liste(exercice).nonValideEdi());
    }

    @Test
    void sansFichierLeDepotEstRefuseEtUnAutreLivreNeVoitRien() {
        assertEquals("COMPTA_LIASSE_SANS_FICHIER", assertThrows(CommonException.class, () -> liasse.deposer(exercice, new DepotLiasseRequest(LocalDate.parse("2026-03-30"), null))).getErrorCode());
        SocieteDeTest autre = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(autre.pointDeVenteId());
        abonnements.modifier(autre.pointDeVenteId(), true);
        livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2025", DU, AU), new IdentiteLivreDTO("Autre", "SARL", "99900002", "999000000000002", null, null, null, "Rabat", "Négoce", null, null, "GENERAL")));

        assertThrows(ResourceNotFoundException.class, () -> liasse.tableau(exercice, "T3"));
        assertThrows(ResourceNotFoundException.class, () -> is.lire(exercice));
    }

    // --- Outils

    private static String valeurRegistre(LigneRegistreDTO l, String colonne) {
        return l.valeurs().stream().filter(v -> colonne.equals(v.colonne())).map(ValeurRegistreDTO::valeur).findFirst().orElse(null);
    }

    private BigDecimal valeur(EtatDTO etat, String ligne, String colonne) {
        String v = etat.lignes().stream().filter(l -> l.code().equals(ligne)).findFirst().orElseThrow(() -> new AssertionError(ligne + " absente de " + etat.code())).valeurs().get(colonne);
        return v == null ? null : new BigDecimal(v);
    }

    private BigDecimal cellule(TableauDTO t, String ligne, String colonne) {
        Map<String, String> valeurs = t.lignes().stream().filter(l -> l.code().equals(ligne)).findFirst().orElseThrow(() -> new AssertionError(ligne)).valeurs();
        return valeurs.get(colonne) == null ? null : new BigDecimal(valeurs.get(colonne));
    }

    private static void montant(String attendu, BigDecimal valeur) {
        assertNotNull(valeur, "attendu " + attendu);
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    private static void montant(String attendu, BigDecimal valeur, String message) {
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), message + " : attendu " + attendu + ", obtenu " + valeur);
    }

    private void solde(String compte, String attendu) {
        BigDecimal solde = jdbc.queryForObject("SELECT COALESCE(SUM(debit - credit), 0) FROM compta_lignes WHERE livre_id = ? AND compte_numero = ? AND nature_ecriture IN ('COURANTE', 'INVENTAIRE')", BigDecimal.class, livre.livreId(), compte);
        assertEquals(0, new BigDecimal(attendu).compareTo(solde), "compte " + compte + " : " + solde + " au lieu de " + attendu);
    }

    private void fermerPeriodes(Long exerciceId) {
        for (PeriodeDTO p : exercices.periodes(exerciceId)) {
            if ("COURANTE".equals(p.typePeriode()) && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
    }

    private EcritureDTO passer(String date, String journal, String compteDebit, Long tiersDebit, String compteCredit, Long tiersCredit, String montant) {
        JournalDTO j = journaux.lister(livre.livreId()).stream().filter(x -> x.code().equals(journal)).findFirst().orElseThrow();
        LocalDate jour = LocalDate.parse(date);
        BigDecimal m = new BigDecimal(montant);
        BrouillonDTO b = ecritures.creerBrouillon(new BrouillonRequest(j.id(), jour, jour, "P-" + (numero++), "Écriture d'essai", "COURANTE", null, false, List.of(
                new BrouillonLigneRequest(compteDebit, tiersDebit, m, BigDecimal.ZERO, null, null, null, null, null, null, null, null),
                new BrouillonLigneRequest(compteCredit, tiersCredit, BigDecimal.ZERO, m, null, null, null, null, null, null, null, null))));
        return ecritures.validerBrouillon(b.id());
    }
}
