package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.ContrepassationRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ColonneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ControleEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.DetailCelluleDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.LigneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ParametreFiscalDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RapportControlesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseLigneDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseSoldesRequest;
import com.gestion.persistent.dto.compta.EtatDtos.ResultatControleDTO;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ControleService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.EtatExports;
import com.gestion.service.compta.EtatService;
import com.gestion.service.compta.FecService;
import com.gestion.service.compta.JournalService;
import com.gestion.service.compta.LettrageService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.ParametreFiscalService;
import com.gestion.service.compta.TiersService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class EtatComptaIT extends AbstractComptaIT {

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private EcritureService ecritures;
    @Autowired private LettrageService lettrage;
    @Autowired private JournalService journaux;
    @Autowired private TiersService tiers;
    @Autowired private EtatService etats;
    @Autowired private EtatExports exports;
    @Autowired private ControleService controles;
    @Autowired private FecService fec;
    @Autowired private ParametreFiscalService parametres;

    private LivreDTO livre;
    private Long exerciceId;
    private Map<String, Long> journal;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(societe.pointDeVenteId());
        abonnements.modifier(societe.pointDeVenteId(), true);
        livre = livres.activer(new ActivationLivreRequest(LocalDate.parse("2026-01-01"),
                new ExerciceDatesRequest("2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        exerciceId = livre.exerciceCourantId();
        journal = journaux.lister().stream().collect(Collectors.toMap(JournalDTO::code, JournalDTO::id));
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    /** S2 sur de vraies écritures. */
    @Test
    void unDecouvertPasseAuPassifEtLeBilanResteEquilibre() {
        passer("OD", "2026-01-05", "APPORT", l("5141", null, "10000.00", "0"), l("1111", null, "0", "10000.00"));
        passer("BQ", "2026-01-10", "ACH 1", l("6111", null, "30000.00", "0"), l("5141", null, "0", "30000.00"));

        EtatDTO actif = etats.etat(exerciceId, "BILAN_ACTIF", null);
        EtatDTO passif = etats.etat(exerciceId, "BILAN_PASSIF", null);
        assertEquals("0.00", valeur(actif, "ACT.514", "NET"));
        assertEquals("20000.00", valeur(passif, "PAS.554", "NET"));
        assertEquals("0.00", valeur(actif, "ACT.TOTAL_GENERAL", "NET"));
        assertEquals("0.00", valeur(passif, "PAS.TOTAL_GENERAL", "NET"));
        assertEquals("-30000.00", valeur(etats.etat(exerciceId, "CPC", null), "CPC.XVI", "TOTAUX"));
        assertTrue(controle(actif, "BILAN_EQUILIBRE").reussi());
    }

    @Test
    void unClientCrediteurNeCompensePasUnClientDebiteur() {
        TiersDTO debiteur = client("Droguerie Moulay Idriss");
        TiersDTO crediteur = client("Bazar Anfa");
        passer("VT", "2026-01-05", "FV 1", l("3421", debiteur.id(), "1000.00", "0"), l("7111", null, "0", "1000.00"));
        passer("BQ", "2026-01-06", "VIR 1", l("5141", null, "300.00", "0"), l("3421", crediteur.id(), "0", "300.00"));

        assertEquals("1000.00", valeur(etats.etat(exerciceId, "BILAN_ACTIF", null), "ACT.342", "BRUT"));
        assertEquals("300.00", valeur(etats.etat(exerciceId, "BILAN_PASSIF", null), "PAS.442", "NET"));
    }

    @Test
    void lesMentionsEtLeProvisoireSontDansLEtat() {
        passer("OD", "2026-01-05", "APPORT", l("5141", null, "10000.00", "0"), l("1111", null, "0", "10000.00"));
        EtatDTO sans = etats.etat(exerciceId, "CPC", null);
        assertEquals(0, sans.nbBrouillons());
        assertEquals("Atlas Négoce SARL", sans.mentions().entite());
        assertEquals("2026", sans.mentions().exercice());
        assertEquals("01/01/2026 au 31/12/2026", sans.mentions().periode());
        assertNotNull(sans.mentions().produitLe());
        assertTrue(sans.mentions().logiciel().startsWith("Tadbeer"));
        assertTrue(sans.mentions().typeEtat().startsWith("COMPTE DE PRODUITS"));
        assertEquals("CALCUL", sans.origine());
        assertEquals(LocalDate.parse("2026-12-31"), sans.arrete());

        ecritures.creerBrouillon(brouillon("OD", "2026-01-06", "BR", l("6111", null, "5.00", "0"), l("5141", null, "0", "5.00")));
        assertEquals(1, etats.etat(exerciceId, "CPC", null).nbBrouillons());
    }

    @Test
    void uneDateDArretLimiteLesMouvements() {
        passer("VT", "2026-01-05", "FV 1", l("3421", client("A").id(), "100.00", "0"), l("7111", null, "0", "100.00"));
        passer("VT", "2026-02-05", "FV 2", l("3421", client("B").id(), "50.00", "0"), l("7111", null, "0", "50.00"));

        assertEquals("150.00", valeur(etats.etat(exerciceId, "CPC", null), "CPC.711", "TOTAUX"));
        assertEquals("100.00", valeur(etats.etat(exerciceId, "CPC", LocalDate.parse("2026-01-31")), "CPC.711", "TOTAUX"));
    }

    @Test
    void uneEcritureEtSaContrepassationNeGonflentPasLesMesures() {
        EcritureDTO e = passer("VT", "2026-01-05", "FV 1", l("3421", client("A").id(), "100.00", "0"), l("7111", null, "0", "100.00"));
        ecritures.contrepasser(e.id(), new ContrepassationRequest(LocalDate.parse("2026-01-20"), "Erreur de client"));

        assertEquals("0.00", valeur(etats.etat(exerciceId, "CPC", null), "CPC.711", "TOTAUX"));
        assertEquals("0.00", valeur(etats.etat(exerciceId, "BILAN_ACTIF", null), "ACT.342", "BRUT"));
    }

    @Test
    void sansBalanceRepriseLesColonnesPrecedentesRestentVides() {
        passer("OD", "2026-01-05", "APPORT", l("5141", null, "10000.00", "0"), l("1111", null, "0", "10000.00"));
        EtatDTO actif = etats.etat(exerciceId, "BILAN_ACTIF", null);
        assertNull(valeur(actif, "ACT.514", "NET_N1"));
        assertNull(valeur(actif, "ACT.TOTAL_GENERAL", "NET_N1"));
        assertNull(controle(actif, "BILAN_EQUILIBRE_N1").reussi());

        etats.enregistrerReprise(exerciceId, new RepriseSoldesRequest(List.of(
                new RepriseLigneDTO("5141", new BigDecimal("2500.00"), BigDecimal.ZERO),
                new RepriseLigneDTO("1111", BigDecimal.ZERO, new BigDecimal("2000.00")),
                new RepriseLigneDTO("7111", BigDecimal.ZERO, new BigDecimal("500.00")))));

        EtatDTO repris = etats.etat(exerciceId, "BILAN_ACTIF", null);
        assertEquals("2500.00", valeur(repris, "ACT.514", "NET_N1"));
        assertTrue(controle(repris, "BILAN_EQUILIBRE_N1").reussi());
        assertEquals("500.00", valeur(etats.etat(exerciceId, "CPC", null), "CPC.711", "TOTAUX_N1"));
        assertEquals("500.00", valeur(etats.etat(exerciceId, "BILAN_PASSIF", null), "PAS.119", "NET_N1"));

        CommonException refus = assertThrows(CommonException.class, () -> etats.enregistrerReprise(exerciceId,
                new RepriseSoldesRequest(List.of(new RepriseLigneDTO("5141", new BigDecimal("1.00"), BigDecimal.ZERO)))));
        assertEquals("COMPTA_REPRISE_DESEQUILIBREE", refus.getErrorCode());
        assertEquals("2500.00", valeur(etats.etat(exerciceId, "BILAN_ACTIF", null), "ACT.514", "NET_N1"));
    }

    @Test
    void leDetailDUnPosteDonneSesComptesEtSaFormule() {
        passer("OD", "2026-01-05", "ACH", l("2351", null, "1000.00", "0"), l("5141", null, "0", "1000.00"));
        passer("OD", "2026-01-06", "AMORT", l("61935", null, "400.00", "0"), l("28351", null, "0", "400.00"));

        DetailCelluleDTO detail = etats.detail(exerciceId, "BILAN_ACTIF", "ACT.235", "NET", null);
        assertEquals(new BigDecimal("600.00"), detail.comptes().stream().map(c -> c.montant()).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertTrue(detail.comptes().stream().anyMatch(c -> c.compte().equals("2351") && c.montant().compareTo(new BigDecimal("1000.00")) == 0));
        assertTrue(detail.comptes().stream().anyMatch(c -> c.compte().equals("28351") && c.montant().compareTo(new BigDecimal("-400.00")) == 0));
        assertNotNull(detail.formule());
        assertThrows(ResourceNotFoundException.class, () -> etats.detail(exerciceId, "BILAN_ACTIF", "PAS.111", "NET", null));
    }

    @Test
    void unEtatInconnuOuDUnAutreLivreEstRefuse() {
        assertThrows(ResourceNotFoundException.class, () -> etats.etat(exerciceId, "MEMO", null));
        assertThrows(ResourceNotFoundException.class, () -> etats.etat(999_999L, "CPC", null));
    }

    @Test
    void lEcranLePdfEtLExcelMontrentLesMemesValeurs() throws Exception {
        passer("OD", "2026-01-05", "APPORT", l("5141", null, "1234567.89", "0"), l("1111", null, "0", "1234567.89"));
        passer("VT", "2026-01-06", "FV 1", l("3421", client("A").id(), "14400.00", "0"), l("7111", null, "0", "12000.00"), l("4455", null, "0", "2400.00"));

        for (String code : List.of("BILAN_ACTIF", "BILAN_PASSIF", "CPC", "ESG_TFR", "ESG_CAF", "TF_MASSES", "TF_EMPLOIS_RESSOURCES")) {
            EtatDTO etat = etats.etat(exerciceId, code, null);
            String pdf = textePdf(exports.pdf(etat));
            // L'extraction rend les espaces de groupement à sa façon : on compare les chiffres
            String sansBlancs = pdf.replaceAll("[\\s\\u00A0\\u202F]", "");
            assertTrue(pdf.contains("Atlas N"), code + " : mentions absentes du PDF");
            assertTrue(pdf.contains("Tadbeer"), code);
            assertTrue(pdf.contains("Exercice 2026"), code);
            try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(exports.xlsx(etat)))) {
                Sheet feuille = classeur.getSheetAt(0);
                assertTrue(feuille.getRow(1).getCell(0).getStringCellValue().contains("Exercice 2026"));
                int rang = 3;
                for (LigneEtatDTO ligne : etat.lignes()) {
                    Row r = feuille.getRow(++rang);
                    assertEquals(ligne.libelle(), r.getCell(0).getStringCellValue());
                    int indice = 1;
                    for (ColonneEtatDTO colonne : etat.colonnes()) {
                        String valeur = ligne.valeurs().get(colonne.code());
                        if (valeur == null) {
                            assertNull(r.getCell(indice), code + " " + ligne.code());
                        } else {
                            assertEquals(new BigDecimal(valeur).doubleValue(), r.getCell(indice).getNumericCellValue(), 0.001, code + " " + ligne.code());
                            if (new BigDecimal(valeur).signum() != 0) {
                                String attendu = new BigDecimal(valeur).toPlainString().replace('.', ',');
                                assertTrue(sansBlancs.contains(attendu), code + " " + ligne.code() + " : " + attendu + " absent du PDF");
                            }
                        }
                        indice++;
                    }
                }
            }
        }
    }

    @Test
    void unControleNonExecuteSAfficheNonEffectueJamaisReussi() {
        RapportControlesDTO jamais = controles.dernier(exerciceId);
        assertNull(jamais.execution());
        assertTrue(jamais.resultats().stream().allMatch(r -> r.reussi() == null), "aucun contrôle n'a tourné");
        assertTrue(jamais.resultats().size() > 20);

        passer("OD", "2026-01-05", "APPORT", l("5141", null, "10000.00", "0"), l("1111", null, "0", "10000.00"));
        RapportControlesDTO rejoue = controles.rejouer(exerciceId);
        assertNotNull(rejoue.execution());
        assertEquals(0, rejoue.bloquants(), () -> rejoue.resultats().toString());
        assertTrue(resultat(rejoue, "BILAN_EQUILIBRE").reussi());
        assertTrue(resultat(rejoue, "EQUILIBRE_ECRITURE").reussi());
        assertTrue(resultat(rejoue, "NUMEROTATION").reussi());
        assertNull(resultat(rejoue, "BILAN_EQUILIBRE_N1").reussi());
        assertNull(resultat(rejoue, "AN_EGAL_CLOTURE_N1").reussi());
        assertTrue(resultat(rejoue, "DISTRIBUTION_T14").reussi());
        assertEquals(rejoue.execution(), controles.dernier(exerciceId).execution());
    }

    @Test
    void unCompteASolderEtUneCaisseCreditriceBloquentLaCloture() {
        passer("OD", "2026-01-05", "APPORT", l("5141", null, "10000.00", "0"), l("1111", null, "0", "10000.00"));
        passer("BQ", "2026-01-06", "DEPOT", l("5115", null, "1200.00", "0"), l("5141", null, "0", "1200.00"));
        passer("CA", "2026-01-07", "SORTIE", l("6111", null, "50.00", "0"), l("51611", null, "0", "50.00"));

        RapportControlesDTO rapport = controles.rejouer(exerciceId);
        ResultatControleDTO solder = rapport.resultats().stream().filter(r -> r.code().equals("COMPTES_A_SOLDER") && Boolean.FALSE.equals(r.reussi())).findFirst().orElseThrow();
        assertEquals("BLOQUE_CLOTURE", solder.gravite());
        assertTrue(solder.message().startsWith("5115 "), solder.message());
        assertEquals("/comptabilite/grand-livre?compte=5115", solder.lien());
        assertEquals(new BigDecimal("1200.00"), solder.valeurA());

        ResultatControleDTO caisse = rapport.resultats().stream().filter(r -> r.code().equals("SENS_ANORMAL") && Boolean.FALSE.equals(r.reussi())).findFirst().orElseThrow();
        assertTrue(caisse.message().startsWith("51611 "), caisse.message());
        assertEquals("BLOQUE_CLOTURE", caisse.gravite());
        assertEquals(0, rapport.bloquants());

        LigneEtatDTO ligne = ligne(etats.etat(exerciceId, "BILAN_ACTIF", null), "ACT.516");
        assertEquals("-50.00", ligne.valeurs().get("BRUT"));
        assertTrue(ligne.alertes().contains("SENS_ANORMAL"), ligne.alertes().toString());
    }

    @Test
    void leFecSortDansLOrdreAvecSesDixHuitChamps() throws Exception {
        TiersDTO client = client("Droguerie; Moulay | Idriss");
        EcritureDTO facture = passer("VT", "2026-01-05", "FV 1", l("3421", client.id(), "120.00", "0"), l("7111", null, "0", "100.00"), l("4455", null, "0", "20.00"));
        EcritureDTO reglement = passer("BQ", "2026-01-06", "VIR 1", l("5141", null, "120.00", "0"), l("3421", client.id(), "0", "120.00"));
        ecritures.creerBrouillon(brouillon("OD", "2026-01-07", "PAS DANS LE FEC", l("6111", null, "5.00", "0"), l("5141", null, "0", "5.00")));
        lettrage.lettrer(new LettrageRequest("3421", client.id(), "TOTAL", LocalDate.parse("2026-01-31"),
                List.of(ligneTiers(facture), ligneTiers(reglement))));

        List<String> lignes = fec(FecService.OptionsFec.parDefaut());
        assertEquals("Code_Journal|Libelle_Journal|Num_Ecriture|Date_Ecriture|Num_Compte|Libelle_Compte|Num_Compte_Aux|Libelle_Compte_Aux|Ref_Piece|Date_Piece|Libelle_Ecriture|Montant_Debit|Montant_Credit|Lettrage_Ecriture|Date_Lettrage|Date_Validation|Montant_Devise|Id_devise", lignes.get(0));
        assertEquals(6, lignes.size(), lignes.toString());
        assertTrue(lignes.stream().noneMatch(l -> l.contains("PAS DANS LE FEC")));
        for (String ligne : lignes) {
            assertEquals(18, ligne.split("\\|", -1).length, ligne);
        }
        String[] premiere = lignes.get(1).split("\\|", -1);
        assertEquals("BQ", premiere[0]);
        assertEquals("BQ-2026-000001", premiere[2]);
        assertEquals("20260106", premiere[3]);
        assertEquals("120.00", premiere[11]);
        String[] tiersLigne = lignes.get(2).split("\\|", -1);
        assertEquals("3421", tiersLigne[4]);
        assertEquals(client.codeAuxiliaire(), tiersLigne[6]);
        assertEquals("Droguerie; Moulay   Idriss", tiersLigne[7]);
        assertEquals("AA", tiersLigne[13]);
        assertEquals("20260131", tiersLigne[14]);

        List<String> pointVirgule = fec(new FecService.OptionsFec(";", "windows-1252", "dd/MM/yyyy"));
        String[] champs = pointVirgule.get(1).split(";", -1);
        assertEquals(18, champs.length);
        assertEquals("06/01/2026", champs[3]);
        assertThrows(CommonException.class, () -> fec.preparer(exerciceId, new FecService.OptionsFec("x", "UTF-8", "yyyyMMdd")));
    }

    @Test
    void unParametreSansValeurALaDateRefuseLeCalcul() {
        ParametreFiscalDTO taux = parametres.aLaDate("TVA_TAUX", "20.00", LocalDate.parse("2026-06-30"));
        assertEquals(0, new BigDecimal("20").compareTo(taux.valeur()));
        assertEquals("VERIFIE", taux.fiabilite());
        assertFalse(taux.source().isBlank());
        assertEquals(0, new BigDecimal("2.25").compareTo(parametres.aLaDate("DP_TAUX_DIRECTEUR", "", LocalDate.parse("2026-01-01")).valeur()));

        CommonException refus = assertThrows(CommonException.class, () -> parametres.aLaDate("TVA_TAUX", "14.00", LocalDate.parse("2026-01-01")));
        assertEquals("COMPTA_PARAMETRE_ABSENT", refus.getErrorCode());
    }

    private List<String> fec(FecService.OptionsFec options) throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        FecService.ExportFec export = fec.preparer(exerciceId, options);
        assertTrue(export.nomFichier().startsWith("FEC_99900001_2026"), export.nomFichier());
        fec.ecrire(export, sortie);
        String charset = options.encodage();
        return sortie.toString(charset).lines().toList();
    }

    private static String textePdf(byte[] pdf) throws Exception {
        PdfReader lecteur = new PdfReader(pdf);
        StringBuilder texte = new StringBuilder();
        PdfTextExtractor extracteur = new PdfTextExtractor(lecteur);
        for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
            texte.append(extracteur.getTextFromPage(page)).append('\n');
        }
        lecteur.close();
        return new String(texte.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private static String valeur(EtatDTO etat, String ligne, String colonne) {
        return ligne(etat, ligne).valeurs().get(colonne);
    }

    private static LigneEtatDTO ligne(EtatDTO etat, String code) {
        return etat.lignes().stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError(code));
    }

    private static ControleEtatDTO controle(EtatDTO etat, String code) {
        return etat.controles().stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError(code));
    }

    private static ResultatControleDTO resultat(RapportControlesDTO rapport, String code) {
        return rapport.resultats().stream().filter(r -> r.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError(code));
    }

    private TiersDTO client(String nom) {
        return tiers.creer(new TiersCreationRequest("CLIENT", nom, null, null, "3421", null, null, null));
    }

    private EcritureDTO passer(String journalCode, String date, String piece, BrouillonLigneRequest... lignes) {
        BrouillonDTO b = ecritures.creerBrouillon(brouillon(journalCode, date, piece, lignes));
        return ecritures.validerBrouillon(b.id());
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
}
