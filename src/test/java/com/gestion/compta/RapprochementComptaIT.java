package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ClassementRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.ComptabiliserReleveRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.EtatRapprochementDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.PropositionPointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ValiderEtatRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeclarationDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresRequest;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ClotureService;
import com.gestion.service.compta.CorrespondanceService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.EtatRapprochement;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.InventaireClotureService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.PeriodeService;
import com.gestion.service.compta.RapprochementComptaService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le relevé est inséré en SQL : le module le lit, et n'écrit que ses pointages. */
@SpringBootTest
class RapprochementComptaIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");
    private static final LocalDate FIN_JANVIER = LocalDate.parse("2026-01-31");

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private RapprochementComptaService rapprochement;
    @Autowired private CorrespondanceService correspondances;
    @Autowired private EcritureService ecritures;
    @Autowired private JournauxRepository journaux;
    @Autowired private TiersRepository tiers;
    @Autowired private ClotureService cloture;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private InventaireClotureService inventaires;
    @Autowired private TvaDeclarationService tva;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private LivreDTO livre;
    private Long pdv;
    private Long banque;
    private Long client;
    private Long fournisseur;
    private int numero = 1;

    @BeforeEach
    void preparer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        pdv = societe.pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        abonnements.modifier(pdv, true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        banque = compteFinancier("BQ-" + pdv, "Banque A", "5000.00");
        client = tiers.creer(livre.livreId(), new TiersCreationRequest("CLIENT", "Droguerie", null, null, "3421", null, null, null));
        fournisseur = tiers.creer(livre.livreId(), new TiersCreationRequest("FOURNISSEUR", "Sebou", null, null, "4411", null, null, null));
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    // --- La formule

    @Test
    void lesTroisCasDeLaSpecificationTombentJuste() {
        EtatRapprochement cheque = EtatRapprochement.calculer(m("10000"), m("0"), m("2000"), m("8000"), m("0"), m("0"));
        montant("8000", cheque.soldeBanqueRectifie());
        montant("8000", cheque.soldeComptableRectifie());
        montant("0", cheque.ecart());
        montant("0", EtatRapprochement.calculer(m("10000"), m("3000"), m("0"), m("13000"), m("0"), m("0")).ecart());
        montant("0", EtatRapprochement.calculer(m("9900"), m("0"), m("0"), m("10000"), m("0"), m("100")).ecart());
    }

    // --- S13

    @Test
    void s13UnChequeNonDebiteLaisseUnEcartNulEtLeSoldeVientDuGrandLivre() {
        passerEncaissement("2026-01-05", "10000.00");
        Long cheque = passerReglement("2026-01-20", "2000.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "10000.00");
        Long credit = ligneReleve(releve, "2026-01-06", "VIR CLIENT", "0", "10000.00");
        pointer(credit, ligneBanque("2026-01-05"), "10000.00");

        EtatRapprochementDTO e = rapprochement.etat(banque, FIN_JANVIER);

        montant("8000.00", e.soldeComptable());
        montant("10000.00", e.soldeReleve());
        montant("2000.00", e.ecrituresCreditNonPointees());
        montant("8000.00", e.soldeBanqueRectifie());
        montant("8000.00", e.soldeComptableRectifie());
        montant("0", e.ecart());
        assertEquals(1, e.ecrituresNonPointees().size());
        assertEquals(cheque, e.ecrituresNonPointees().get(0).ligneId());
        assertTrue(e.relevesNonPointes().isEmpty());
    }

    @Test
    void s13UneRemiseEnTransitTombeJuste() {
        passerEncaissement("2026-01-05", "10000.00");
        passerEncaissement("2026-01-28", "3000.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "10000.00");
        pointer(ligneReleve(releve, "2026-01-06", "VIR", "0", "10000.00"), ligneBanque("2026-01-05"), "10000.00");

        EtatRapprochementDTO e = rapprochement.etat(banque, FIN_JANVIER);

        montant("13000.00", e.soldeComptable());
        montant("3000.00", e.ecrituresDebitNonPointees());
        montant("0", e.ecart());
    }

    @Test
    void s13DesFraisNonComptabilisesTombentJustePuisSePassentDepuisLeReleve() {
        passerEncaissement("2026-01-05", "10000.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "9900.00");
        pointer(ligneReleve(releve, "2026-01-06", "VIR", "0", "10000.00"), ligneBanque("2026-01-05"), "10000.00");
        Long frais = ligneReleve(releve, "2026-01-30", "COMMISSION", "100.00", "0");

        EtatRapprochementDTO e = rapprochement.etat(banque, FIN_JANVIER);
        montant("100.00", e.debitsReleveNonPointes());
        montant("9900.00", e.soldeComptableRectifie());
        montant("0", e.ecart());

        // Écriture et pointage dans la même transaction, sans détection par mots-clés
        PointageDTO p = rapprochement.comptabiliser(frais, new ComptabiliserReleveRequest("61473", new BigDecimal("9.09"), new BigDecimal("10.00")));

        montant("100.00", p.montant());
        solde("61473", "90.91");
        solde("34552", "9.09");
        solde("5141", "9900.00");
        EtatRapprochementDTO apres = rapprochement.etat(banque, FIN_JANVIER);
        montant("9900.00", apres.soldeComptable());
        assertTrue(apres.relevesNonPointes().isEmpty());
        montant("0", apres.ecart());
        assertEquals("COMPTA_POINTAGE_EXCESSIF", assertThrows(CommonException.class, () -> rapprochement.comptabiliser(frais, new ComptabiliserReleveRequest("61473", null, null))).getErrorCode());
    }

    @Test
    void desFraisDatesApresLeReleveParLaChronologieNeFaussentPasLEtatPasse() {
        passerEncaissement("2026-01-05", "10000.00");
        passerEncaissement("2026-02-10", "500.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "9900.00");
        pointer(ligneReleve(releve, "2026-01-06", "VIR", "0", "10000.00"), ligneBanque("2026-01-05"), "10000.00");
        Long frais = ligneReleve(releve, "2026-01-30", "COMMISSION", "100.00", "0");

        PointageDTO p = rapprochement.comptabiliser(frais, new ComptabiliserReleveRequest("61473", null, null));

        // Le journal de banque a déjà une écriture de février : celle-ci passe au 1er février.
        assertEquals(LocalDate.parse("2026-02-01"), p.datePointage());
        EtatRapprochementDTO fin = rapprochement.etat(banque, FIN_JANVIER);
        montant("10000.00", fin.soldeComptable());
        montant("100.00", fin.debitsReleveNonPointes());
        montant("0", fin.ecart());
    }

    @Test
    void leSoldeNeLitJamaisCeluiDuCompteFinancier() {
        passerEncaissement("2026-01-05", "100.00");
        assertEquals(0, new BigDecimal("5000.00").compareTo(jdbc.queryForObject("SELECT solde_actuel FROM comptes_financiers WHERE id = ?", BigDecimal.class, banque)));

        montant("100.00", rapprochement.etat(banque, FIN_JANVIER).soldeComptable());
    }

    @Test
    void lEtatSeLitAUneDatePassee() {
        passerEncaissement("2026-01-05", "10000.00");
        passerReglement("2026-01-20", "2000.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "8000.00");
        Long l1 = ligneReleve(releve, "2026-01-06", "VIR", "0", "10000.00");
        Long l2 = ligneReleve(releve, "2026-01-22", "CHQ", "2000.00", "0");
        pointerLe(l1, ligneBanque("2026-01-05"), "10000.00", "2026-01-07");
        pointerLe(l2, ligneBanque("2026-01-20"), "2000.00", "2026-01-25");

        EtatRapprochementDTO auDix = rapprochement.etat(banque, LocalDate.parse("2026-01-10"));

        montant("10000.00", auDix.soldeComptable());
        montant("10000.00", auDix.soldeReleve());
        montant("0", auDix.ecart());
        assertTrue(auDix.ecrituresNonPointees().isEmpty(), "le chèque du 20 n'existe pas encore le 10");

        EtatRapprochementDTO auVingt = rapprochement.etat(banque, LocalDate.parse("2026-01-21"));
        montant("8000.00", auVingt.soldeComptable());
        montant("2000.00", auVingt.ecrituresCreditNonPointees(), "non pointé à cette date, même pointé depuis");
        montant("0", auVingt.ecart());
    }

    @Test
    void unChequeDeDecembreResteEnSuspensEnJanvierEtLANouveauNEntrePas() {
        passerEncaissement("2026-01-05", "10000.00");
        passerReglement("2026-12-20", "2000.00");
        fermerEtCloturer();
        ExerciceDTO suivant = exercices.lister().stream().filter(e -> "2027".equals(e.code())).findFirst().orElseThrow();
        LocalDate janvier = LocalDate.parse("2027-01-31");

        EtatRapprochementDTO e = rapprochement.etat(banque, janvier);

        montant("8000.00", e.soldeComptable());
        assertEquals(2, e.ecrituresNonPointees().size(), "les opérations de 2026 non pointées restent en suspens, l'à-nouveau n'y est pas");
        assertTrue(e.ecrituresNonPointees().stream().allMatch(l -> l.date().getYear() == 2026));
        Long aNouveau = jdbc.queryForObject("SELECT id FROM compta_lignes WHERE livre_id = ? AND exercice_id = ? AND compte_numero = '5141' AND nature_ecriture = 'A_NOUVEAU'", Long.class, livre.livreId(), suivant.id());
        Long releve = releve(banque, "2027-01-01", "2027-01-31", "0", "100.00");
        Long ligne = ligneReleve(releve, "2027-01-05", "X", "0", "100.00");
        assertEquals("COMPTA_POINTAGE_A_NOUVEAU", assertThrows(CommonException.class, () -> rapprochement.pointer(new PointageRequest(ligne, aNouveau, new BigDecimal("100.00"), janvier, null))).getErrorCode());
        // La base refuse aussi, sans passer par le service
        assertThrows(DataAccessException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(s -> jdbc.update("INSERT INTO compta_pointages (livre_id, ligne_releve_id, ligne_id, montant, date_pointage, pointe_par) VALUES (?, ?, ?, 100, '2027-01-31', 'essai')", livre.livreId(), ligne, aNouveau)));
    }

    // --- S10

    @Test
    void s10UnDebitDeMilleDeuxCentsNeSePointePasSurDeuxReglements() {
        Long r1 = passerReglement("2026-01-10", "1200.00");
        Long r2 = passerReglement("2026-01-11", "1200.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "-1200.00");
        Long debit = ligneReleve(releve, "2026-01-12", "VIREMENT EMIS", "1200.00", "0");

        rapprochement.pointer(new PointageRequest(debit, r1, new BigDecimal("1200.00"), FIN_JANVIER, null));

        assertEquals("COMPTA_POINTAGE_EXCESSIF", assertThrows(CommonException.class, () -> rapprochement.pointer(new PointageRequest(debit, r2, new BigDecimal("1200.00"), FIN_JANVIER, null))).getErrorCode());
        DataAccessException refus = assertThrows(DataAccessException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(s -> jdbc.update("INSERT INTO compta_pointages (livre_id, ligne_releve_id, ligne_id, montant, date_pointage, pointe_par) VALUES (?, ?, ?, 1200, '2026-01-31', 'essai')", livre.livreId(), debit, r2)));
        assertTrue(refus.getMostSpecificCause().getMessage().contains("compta_pointage_excessif"), refus::getMessage);
    }

    @Test
    void unDebitDeNeufCentQuatreVingtDixNeufNeSeProposePasSurUnReglementDeMilleDeuxCents() {
        passerReglement("2026-01-10", "1200.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "-999.00");
        ligneReleve(releve, "2026-01-12", "VIREMENT", "999.00", "0");

        assertTrue(rapprochement.propositions(banque, FIN_JANVIER).isEmpty());
    }

    @Test
    void unePropositionRapprocheSurLeMontantEtLaDateEtAttendUneConfirmation() {
        passerEncaissement("2026-01-05", "10000.00");
        passerReglement("2026-01-10", "2000.00");
        Long ecartee = passerEncaissement("2026-01-02", "500.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "8000.00");
        Long credit = ligneReleve(releve, "2026-01-06", "VIR", "0", "10000.00");
        ligneReleve(releve, "2026-01-29", "HORS DELAI", "0", "500.00");

        List<PropositionPointageDTO> p = rapprochement.propositions(banque, FIN_JANVIER);

        assertEquals(1, p.size(), "ni le mauvais sens, ni au-delà de sept jours");
        assertEquals(credit, p.get(0).ligneReleveId());
        montant("10000.00", p.get(0).montant());
        assertEquals(1, p.get(0).ecartJours());
        assertEquals(0, compter("compta_pointages WHERE livre_id = " + livre.livreId()), "rien ne se pointe sans confirmation");
        assertTrue(ecartee > 0);
    }

    @Test
    void lesRefusDuPointageSontLisibles() {
        Long debitBanque = passerEncaissement("2026-01-05", "100.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "-100.00");
        Long debit = ligneReleve(releve, "2026-01-06", "X", "100.00", "0");
        Long credit = ligneReleve(releve, "2026-01-07", "Y", "0", "100.00");

        assertRefus("COMPTA_POINTAGE_SENS", () -> rapprochement.pointer(new PointageRequest(debit, debitBanque, new BigDecimal("100.00"), FIN_JANVIER, null)));
        assertRefus("COMPTA_POINTAGE_MONTANT", () -> rapprochement.pointer(new PointageRequest(credit, debitBanque, BigDecimal.ZERO, FIN_JANVIER, null)));
        assertRefus("COMPTA_POINTAGE_EXCESSIF", () -> rapprochement.pointer(new PointageRequest(credit, debitBanque, new BigDecimal("100.01"), FIN_JANVIER, null)));
        assertRefus("COMPTA_POINTAGE_DATE", () -> rapprochement.pointer(new PointageRequest(credit, debitBanque, new BigDecimal("100.00"), LocalDate.parse("2026-01-06"), null)));
        Long compteTiers = jdbc.queryForObject("SELECT id FROM compta_lignes WHERE livre_id = ? AND compte_numero = '3421' LIMIT 1", Long.class, livre.livreId());
        assertRefus("COMPTA_POINTAGE_COMPTE", () -> rapprochement.pointer(new PointageRequest(credit, compteTiers, new BigDecimal("1.00"), FIN_JANVIER, null)));
    }

    @Test
    void unPointagePartielLaisseLeReste() {
        Long e = passerEncaissement("2026-01-05", "100.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "60.00");
        Long c = ligneReleve(releve, "2026-01-06", "X", "0", "60.00");

        PointageDTO p = rapprochement.pointer(new PointageRequest(c, e, new BigDecimal("60.00"), FIN_JANVIER, null));

        EtatRapprochementDTO etat = rapprochement.etat(banque, FIN_JANVIER);
        montant("40.00", etat.ecrituresNonPointees().get(0).reste());
        montant("0", etat.ecart());
        rapprochement.depointer(p.id());
        assertEquals(1, rapprochement.etat(banque, FIN_JANVIER).relevesNonPointes().size());
    }

    // --- Validation

    @Test
    void unEtatValideTombeJusteEtFigeSesPointages() {
        Long e = passerEncaissement("2026-01-05", "100.00");
        assertEquals("COMPTA_RAPPROCHEMENT_SANS_RELEVE", assertThrows(CommonException.class, () -> rapprochement.valider(banque, new ValiderEtatRequest(FIN_JANVIER))).getErrorCode());
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "95.00");
        Long c = ligneReleve(releve, "2026-01-06", "X", "0", "90.00");
        assertEquals("COMPTA_RAPPROCHEMENT_ECART", assertThrows(CommonException.class, () -> rapprochement.valider(banque, new ValiderEtatRequest(FIN_JANVIER))).getErrorCode());
        commercial("UPDATE releves_bancaires SET solde_final = 100 WHERE id = ?", releve);
        commercial("UPDATE lignes_releves_bancaires SET credit = 100 WHERE id = ?", c);
        PointageDTO p = rapprochement.pointer(new PointageRequest(c, e, new BigDecimal("100.00"), FIN_JANVIER, null));

        var valide = rapprochement.valider(banque, new ValiderEtatRequest(FIN_JANVIER));

        montant("100.00", valide.soldeRectifie());
        assertTrue(rapprochement.etat(banque, FIN_JANVIER).valide());
        assertEquals(FIN_JANVIER, rapprochement.comptes().stream().filter(x -> x.id().equals(banque)).findFirst().orElseThrow().dernierEtat());
        assertEquals("COMPTA_RAPPROCHEMENT_VALIDE", assertThrows(CommonException.class, () -> rapprochement.depointer(p.id())).getErrorCode());
        assertEquals("COMPTA_RAPPROCHEMENT_VALIDE", assertThrows(CommonException.class, () -> rapprochement.valider(banque, new ValiderEtatRequest(FIN_JANVIER))).getErrorCode());
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO compta_rapprochements (livre_id, compte_numero, compte_financier_id, date_arrete, solde_comptable, solde_releve, solde_comptable_rectifie, solde_releve_rectifie, contenu, valide_par)
                VALUES (?, '5141', ?, '2026-02-28', 1, 1, 1, 2, '{}', 'essai')
                """, livre.livreId(), banque), "ck_compta_rap_egalite");
    }

    @Test
    void deuxBanquesSurLeMemeCompteNeSeValidentPas() {
        Long autre = compteFinancier("BQ2-" + pdv, "Banque B", "0");
        passerEncaissement("2026-01-05", "100.00");
        releve(banque, "2026-01-01", "2026-01-31", "0", "0");

        var c = rapprochement.comptes().stream().filter(x -> x.id().equals(banque)).findFirst().orElseThrow();
        assertEquals(List.of("Banque B"), c.partageAvec());
        assertEquals("COMPTA_RAPPROCHEMENT_FUSION", assertThrows(CommonException.class, () -> rapprochement.valider(banque, new ValiderEtatRequest(FIN_JANVIER))).getErrorCode());

        jdbc.update("INSERT INTO compta_comptes (livre_id, numero, libelle, sens_normal, nature, imputable, origine) SELECT livre_id, '51412', 'Banque B', sens_normal, nature, TRUE, 'SOCIETE' FROM compta_comptes WHERE livre_id = ? AND numero = '5141' ON CONFLICT DO NOTHING", livre.livreId());
        jdbc.update("UPDATE compta_correspondances SET compte_numero = '51412' WHERE livre_id = ? AND type_cle = 'COMPTE_FINANCIER' AND cle = ?", livre.livreId(), String.valueOf(autre));
        assertTrue(rapprochement.comptes().stream().filter(x -> x.id().equals(banque)).findFirst().orElseThrow().partageAvec().isEmpty());
    }

    @Test
    void unAutreLivreNeVoitNiLeCompteNiLeReleve() {
        Long e = passerEncaissement("2026-01-05", "100.00");
        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "100.00");
        Long c = ligneReleve(releve, "2026-01-06", "X", "0", "100.00");
        SocieteDeTest autre = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(autre.pointDeVenteId());
        abonnements.modifier(autre.pointDeVenteId(), true);
        livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU), new IdentiteLivreDTO("Autre", "SARL", "99900002", "999000000000002", null, null, null, "Rabat", "Négoce", null, null, "GENERAL")));

        assertThrows(ResourceNotFoundException.class, () -> rapprochement.etat(banque, FIN_JANVIER));
        assertThrows(ResourceNotFoundException.class, () -> rapprochement.pointer(new PointageRequest(c, e, new BigDecimal("1.00"), FIN_JANVIER, null)));
    }

    // --- La TVA lit le relevé

    @Test
    void t22T23T24SExecutentQuandUnReleveExiste() {
        tva.enregistrerParametres(2026, new TvaParametresRequest(true, "ENCAISSEMENT", "MENSUELLE", new BigDecimal("100"), false, 1, BigDecimal.ZERO));
        TvaDeclarationDTO vide = tva.creer(new TvaCreationRequest(2026, 1));
        assertEquals("NON_EFFECTUE", controle(vide.id(), "T23"), "sans relevé, non effectué et jamais réussi");
        assertTrue(vide.controles().nonEffectues() >= 3);

        Long releve = releve(banque, "2026-01-01", "2026-01-31", "0", "-100.00");
        Long frais = ligneReleve(releve, "2026-01-30", "COMMISSION", "110.00", "0");
        ligneReleve(releve, "2026-01-31", "REMISE", "0", "10.00");
        TvaDeclarationDTO avec = tva.calculer(vide.id());

        assertEquals("ALERTE", controle(avec.id(), "T23"), "un débit ni pointé ni classé");
        assertEquals("ALERTE", controle(avec.id(), "T24"), "un crédit de 10 au relevé, aucun encaissement déclaré");
        assertEquals(null, controle(avec.id(), "T22"), "aucun règlement, aucun débit pointé");
        assertTrue(tva.pointage(avec.id()).stream().anyMatch(l -> l.ligneReleveId().equals(frais) && l.classement() == null));

        // Classer le frais lève T23, et la ligne entre au relevé des déductions à sa rubrique
        tva.classer(frais, new ClassementRequest("RUBRIQUE", "FRAIS_BANCAIRES", "Commission"));
        TvaDeclarationDTO classee = tva.calculer(vide.id());
        assertEquals(null, controle(classee.id(), "T23"));
        List<TvaDeductionDTO> deductions = tva.deductions(classee.id());
        assertEquals(1, deductions.size());
        assertEquals("LIGNE_RELEVE", deductions.get(0).sourceType());
        assertEquals("142", deductions.get(0).ligneCode());
        assertEquals(3, deductions.get(0).mpId());
        montant("10.00", deductions.get(0).tva());

        tva.classer(frais, new ClassementRequest("AUCUN", null, null));
        assertTrue(tva.deductions(tva.calculer(vide.id()).id()).isEmpty());
    }

    // --- Outils

    private String controle(Long declaration, String code) {
        return tva.listeControles(declaration).stream().filter(c -> code.equals(c.code())).map(c -> c.gravite()).findFirst().orElse(null);
    }

    private static BigDecimal m(String v) {
        return new BigDecimal(v);
    }

    private static void montant(String attendu, BigDecimal valeur) {
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    private static void montant(String attendu, BigDecimal valeur, String message) {
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), message + " : attendu " + attendu + ", obtenu " + valeur);
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

    private Long ligneBanque(String date) {
        return jdbc.queryForObject("SELECT id FROM compta_lignes WHERE livre_id = ? AND compte_numero = '5141' AND date_ecriture = ?::date LIMIT 1", Long.class, livre.livreId(), date);
    }

    private void pointer(Long ligneReleve, Long ligne, String montant) {
        rapprochement.pointer(new PointageRequest(ligneReleve, ligne, new BigDecimal(montant), FIN_JANVIER, null));
    }

    private void pointerLe(Long ligneReleve, Long ligne, String montant, String date) {
        rapprochement.pointer(new PointageRequest(ligneReleve, ligne, new BigDecimal(montant), LocalDate.parse(date), null));
    }

    private void fermerEtCloturer() {
        Long exercice = livre.exerciceCourantId();
        for (PeriodeDTO p : exercices.periodes(exercice)) {
            if ("COURANTE".equals(p.typePeriode()) && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
        inventaires.enregistrer(exercice, new InventaireRequest(null, true, List.of()));
        inventaires.valider(exercice);
        cloture.arreter(exercice, new ArreterRequest("Pas d'IS.", null));
        cloture.cloturer(exercice, new CloturerRequest("2026"));
    }

    private Long passerEncaissement(String date, String montant) {
        return passer(date, "5141", null, montant, "3421", client);
    }

    private Long passerReglement(String date, String montant) {
        return passer(date, "4411", fournisseur, montant, "5141", null);
    }

    /** Une écriture de banque, par le chemin normal. Rend la ligne de 5141. */
    private Long passer(String date, String compteDebit, Long tiersDebit, String montant, String compteCredit, Long tiersCredit) {
        ouvrirPeriode(LocalDate.parse(date));
        JournalDTO bq = journaux.parType(livre.livreId(), "BANQUE").orElseThrow();
        LocalDate jour = LocalDate.parse(date);
        BigDecimal m = new BigDecimal(montant);
        List<BrouillonLigneRequest> lignes = new ArrayList<>();
        lignes.add(new BrouillonLigneRequest(compteDebit, tiersDebit, m, BigDecimal.ZERO, null, null, null, null, null, null, null, null));
        lignes.add(new BrouillonLigneRequest(compteCredit, tiersCredit, BigDecimal.ZERO, m, null, null, null, null, null, null, null, null));
        var b = ecritures.creerBrouillon(new BrouillonRequest(bq.id(), jour, jour, "P-" + (numero++), "Mouvement", "COURANTE", null, false, lignes));
        var e = ecritures.validerBrouillon(b.id());
        return jdbc.queryForObject("SELECT id FROM compta_lignes WHERE livre_id = ? AND ecriture_id = ? AND compte_numero = '5141'", Long.class, livre.livreId(), e.id());
    }

    /** Ouvre la période de la date : les précédentes se clôturent, car le livre n'en garde que deux ouvertes. */
    private void ouvrirPeriode(LocalDate date) {
        for (PeriodeDTO p : exercices.periodes(livre.exerciceCourantId())) {
            if ("COURANTE".equals(p.typePeriode()) && p.au().isBefore(date.withDayOfMonth(1)) && !"CLOTUREE".equals(p.statut())) {
                periodes.cloturer(p.id());
            }
        }
    }

    private Long compteFinancier(String code, String nom, String solde) {
        return inserer("INSERT INTO comptes_financiers (code, nom, type, devise, solde_actuel, point_de_vente_id, actif) VALUES (?, ?, 'COMPTE_BANCAIRE', 'MAD', ?, ?, TRUE) RETURNING id", code, nom, new BigDecimal(solde), pdv);
    }

    private Long releve(Long compte, String du, String au, String initial, String fin) {
        return inserer("INSERT INTO releves_bancaires (compte_financier_id, reference_releve, date_debut, date_fin, solde_initial, solde_final, statut, point_de_vente_id) VALUES (?, ?, ?::date, ?::date, ?, ?, 'EN_COURS', ?) RETURNING id",
                compte, "REL-" + (numero++) + "-" + pdv, du, au, new BigDecimal(initial), new BigDecimal(fin), pdv);
    }

    private Long ligneReleve(Long releve, String date, String libelle, String debit, String credit) {
        return inserer("INSERT INTO lignes_releves_bancaires (releve_bancaire_id, date_operation, libelle, debit, credit, statut_rapprochement, point_de_vente_id) VALUES (?, ?::date, ?, ?, ?, 'NON_RAPPROCHE', ?) RETURNING id",
                releve, date, libelle, new BigDecimal(debit), new BigDecimal(credit), pdv);
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
