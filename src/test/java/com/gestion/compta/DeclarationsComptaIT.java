package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.AttestationRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpCalculRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpDeclarationDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpDepotRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpValidationRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheanceDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheanceRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheancesDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasLigneDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasMoisDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasRetenueRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresRequest;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.DelaisComptaService;
import com.gestion.service.compta.EcheanceComptaService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.RasComptaService;
import com.gestion.service.compta.TvaDeclarationService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DeclarationsComptaIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");
    private static final DpCalculRequest CA = new DpCalculRequest(new BigDecimal("60000000.00"));

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private DelaisComptaService delais;
    @Autowired private RasComptaService ras;
    @Autowired private EcheanceComptaService echeances;
    @Autowired private TvaDeclarationService tva;
    @Autowired private TiersRepository tiers;
    @Autowired private com.gestion.repository.compta.ExerciceRepository exercicesLus;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private LivreDTO livre;
    private Long pdv;
    private Long sebou;
    private int numero = 1;

    @BeforeEach
    void preparer() {
        pdv = SocieteDeTest.creer().pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        abonnements.modifier(pdv, true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
        sebou = fournisseur("Comptoir Sebou", null);
    }

    @AfterEach
    void effacer() {
        jdbc.update("DELETE FROM compta_jours_feries WHERE libelle = 'Essai'");
        ContexteDeTest.effacer();
    }

    private static BigDecimal m(String s) {
        return new BigDecimal(s);
    }

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private Long inserer(String sql, Object... parametres) {
        return new TransactionTemplate(transactions).execute(s -> {
            jdbc.execute("SET LOCAL session_replication_role = replica");
            return jdbc.queryForObject(sql, Long.class, parametres);
        });
    }

    private Long fournisseur(String nom, Integer delai) {
        return inserer("INSERT INTO fournisseurs (raison_social, pays, numero_identification_fiscale, delai_paiement_jours, point_de_vente_id) VALUES (?, 'Maroc', '12345678', ?, ?) RETURNING id", nom + " " + pdv, delai, pdv);
    }

    /** Taux de 20 %. */
    private Long facture(Long fournisseurId, String date, String ttc) {
        BigDecimal total = m(ttc);
        BigDecimal ht = total.divide(m("1.2"), 2, java.math.RoundingMode.HALF_UP);
        return inserer("INSERT INTO factures_achat (numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) VALUES (?, ?::timestamp, ?, ?, ?, ?, 'EN_ATTENTE', ?) RETURNING id",
                "FA-" + (numero++) + "-" + pdv, date, fournisseurId, ht, total.subtract(ht), total, pdv);
    }

    private Long reglement(Long factureId, String date, String montant) {
        return inserer("INSERT INTO reglements_fournisseur (numero_reglement, date_reglement, montant, mode_paiement, type_reglement, facture_achat_id, point_de_vente_id) VALUES (?, ?::timestamp, ?, 'VIREMENT', 'FACTURE', ?, ?) RETURNING id",
                "RF-" + (numero++) + "-" + pdv, date, m(montant), factureId, pdv);
    }

    private BigDecimal solde(String compte) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(debit - credit), 0) FROM compta_lignes WHERE livre_id = ? AND compte_numero = ?", BigDecimal.class, livre.livreId(), compte);
    }

    private void refuse(String code, Runnable action) {
        CommonException e = assertThrows(CommonException.class, action::run);
        assertEquals(code, e.getErrorCode());
    }

    // --- Délais de paiement

    /** NC 734 p. 11-12 : T3 2023 = 120 000 DH, T4 2023 = 299 000 DH. */
    @Test
    void lesTrimestresOfficiels() {
        Long premiere = facture(sebou, "2023-07-17", "4000000.00");
        Long seconde = facture(sebou, "2023-08-23", "6000000.00");
        reglement(premiere, "2023-11-10", "4000000.00");
        reglement(seconde, "2023-12-10", "6000000.00");
        DpFactureDTO fiche = delais.modifierFiche(seconde, new DpFactureRequest(null, null, 90, null, null, null));
        assertEquals(d("2023-11-20"), fiche.echeance());
        assertEquals(d("2023-09-14"), delais.fiche(premiere).echeance());

        DpDeclarationDTO t3 = delais.calculer(2023, 3, CA);
        assertEquals(1, t3.lignes().size());
        assertEquals(m("120000.00"), t3.totalAmende());
        assertEquals(m("4000000.00"), t3.totalNonPayees());
        assertEquals(1, t3.lignes().get(0).moisRetard());
        assertEquals(d("2023-10-31"), t3.dateLimite());

        DpDeclarationDTO t4 = delais.calculer(2023, 4, CA);
        assertEquals(2, t4.lignes().size());
        assertEquals(m("299000.00"), t4.totalAmende());
        assertEquals(m("10000000.00"), t4.totalHorsDelai());
        assertEquals(m("0.00"), t4.totalNonPayees());
        assertEquals(d("2024-01-31"), t4.dateLimite());
    }

    @Test
    void validerPasseLAmendeAuDirhamSuperieurEtRouvrirContrePasse() {
        facture(sebou, "2025-01-05", "1000.33");
        delais.calculer(2025, 2, CA);
        DpDeclarationDTO d = delais.lire(2025, 2);
        // Échue le 05/03/2025 : le premier mois de retard est hors du trimestre, restent trois mois à 0,85 %.
        assertEquals(m("25.51"), d.totalAmende());
        assertEquals(m("26.00"), d.versement());

        DpDeclarationDTO v = delais.valider(2025, 2, new DpValidationRequest(d("2026-01-31")));
        assertEquals("VALIDEE", v.statut());
        assertNotNull(v.ecritureAmendeId());
        assertEquals(m("26.00"), solde("65831"));
        assertEquals(m("-26.00"), solde("4458"));
        refuse("COMPTA_DP_VALIDEE", () -> delais.calculer(2025, 2, CA));
        refuse("COMPTA_DP_VALIDEE", () -> delais.valider(2025, 2, new DpValidationRequest(d("2026-01-31"))));

        DpDeclarationDTO r = delais.rouvrir(2025, 2, new MotifRequest("Délai convenu oublié"));
        assertEquals("BROUILLON", r.statut());
        assertNull(r.ecritureAmendeId());
        assertEquals(0, solde("65831").signum());

        delais.valider(2025, 2, new DpValidationRequest(d("2026-01-31")));
        DpDeclarationDTO deposee = delais.deposer(2025, 2, new DpDepotRequest(d("2026-01-31")));
        assertEquals("DEPOSEE", deposee.statut());
        refuse("COMPTA_DP_NON_VALIDEE", () -> delais.rouvrir(2025, 2, new MotifRequest("Trop tard")));
    }

    /** DP-09 : l'absence de factures en retard n'exempte pas de la déclaration. */
    @Test
    void declarationAZero() {
        DpDeclarationDTO d = delais.calculer(2026, 1, CA);
        assertTrue(d.lignes().isEmpty());
        DpDeclarationDTO v = delais.valider(2026, 1, new DpValidationRequest(d("2026-01-31")));
        assertEquals("VALIDEE", v.statut());
        assertNull(v.ecritureAmendeId());
        assertEquals("DEPOSEE", delais.deposer(2026, 1, new DpDepotRequest(d("2026-04-15"))).statut());
    }

    @Test
    void chiffreDAffairesAbsentOuSousLeSeuil() {
        refuse("COMPTA_DP_CA_ABSENT", () -> delais.calculer(2026, 1, new DpCalculRequest(null)));
        refuse("COMPTA_DP_HORS_CHAMP", () -> delais.calculer(2026, 1, new DpCalculRequest(m("2000000.00"))));
        refuse("COMPTA_DP_NON_CALCULEE", () -> delais.valider(2026, 1, new DpValidationRequest(d("2026-01-31"))));
        refuse("COMPTA_DP_TRIMESTRE", () -> delais.calculer(2026, 5, CA));
    }

    @Test
    void lApplicationSuitLeChiffreDAffaires() {
        facture(sebou, "2024-06-01", "20000.00");
        assertEquals(1, delais.calculer(2024, 4, new DpCalculRequest(m("60000000.00"))).lignes().size());
        assertEquals(1, delais.calculer(2024, 4, new DpCalculRequest(m("20000000.00"))).lignes().size());
        assertEquals(0, delais.calculer(2024, 4, new DpCalculRequest(m("5000000.00"))).lignes().size());
    }

    @Test
    void nonResidentEtPetitesFacturesExclus() {
        Long etranger = fournisseur("Hambourg GmbH", null);
        facture(etranger, "2024-07-01", "90000.00");
        facture(sebou, "2024-07-01", "10000.00");
        assertEquals(2 - 1, delais.calculer(2024, 4, CA).lignes().size());
        Long tiersId = tiers.parSource(livre.livreId(), "FOURNISSEUR", etranger).orElseThrow().id();
        jdbc.update("UPDATE compta_tiers SET non_resident = TRUE WHERE id = ?", tiersId);
        assertTrue(delais.calculer(2024, 4, CA).lignes().isEmpty());
    }

    @Test
    void leDelaiVientDeLaFactureDeLaFicheOuDeLaLoi() {
        Long avecDelai = fournisseur("Avec délai", 30);
        Long facture = facture(avecDelai, "2026-01-01", "1200.00");
        assertEquals(30, delais.fiche(facture).delaiRetenu());
        assertEquals(d("2026-01-30"), delais.fiche(facture).echeance());
        Long plafond = facture(fournisseur("Long délai", 200), "2026-01-01", "1200.00");
        assertEquals(120, delais.fiche(plafond).delaiRetenu());
        assertEquals(60, delais.fiche(facture(sebou, "2026-01-01", "1200.00")).delaiRetenu());
        assertEquals(90, delais.modifierFiche(facture, new DpFactureRequest(null, null, 90, null, null, null)).delaiRetenu());
        refuse("COMPTA_DP_DELAI", () -> delais.modifierFiche(facture, new DpFactureRequest(null, null, 121, null, null, null)));
        assertThrows(ResourceNotFoundException.class, () -> delais.fiche(99999999L));
    }

    /** Date de livraison : le délai part de la fin du mois de livraison si la facture est émise après. */
    @Test
    void factureEmiseApresLeMoisDeLivraison() {
        Long f = facture(sebou, "2026-03-10", "1200.00");
        assertEquals(d("2026-05-08"), delais.fiche(f).echeance());
        DpFactureDTO fiche = delais.modifierFiche(f, new DpFactureRequest(null, d("2026-01-15"), null, null, null, null));
        assertEquals(d("2026-03-31"), fiche.echeance());
    }

    @Test
    void leLitigeSuspendLAmendeEtLeJugementLaRattrape() {
        Long f = facture(sebou, "2025-01-05", "100000.00");
        delais.modifierFiche(f, new DpFactureRequest(null, null, null, m("100000.00"), d("2025-03-01"), null));
        DpDeclarationDTO suspendue = delais.calculer(2025, 3, CA);
        assertEquals(m("0.00"), suspendue.totalAmende());
        assertEquals(m("100000.00"), suspendue.totalLitiges());

        delais.modifierFiche(f, new DpFactureRequest(null, null, null, m("100000.00"), d("2025-03-01"), d("2025-12-15")));
        DpDeclarationDTO jugee = delais.calculer(2025, 4, CA);
        // Retard de mars à décembre 2025 : le taux de mars (2,25 %) et neuf mois à 0,85 %, toute la période suspendue.
        assertEquals(m("9900.00"), jugee.totalAmende());
        refuse("COMPTA_DP_LITIGE", () -> delais.modifierFiche(f, new DpFactureRequest(null, null, null, m("100.00"), null, null)));
    }

    @Test
    void reportAuPremierJourOuvrableUneFoisLaListeRemplie() {
        Long f = facture(sebou, "2026-03-10", "1200.00");
        assertEquals(d("2026-05-08"), delais.fiche(f).echeance());
        jdbc.update("INSERT INTO compta_jours_feries (jour, libelle) VALUES ('2026-05-08', 'Essai')");
        assertEquals(d("2026-05-09"), delais.fiche(f).echeance());
        assertEquals(1, delais.joursFeries().size());
    }

    // --- Retenue à la source

    private Long reglementDeService(Long fournisseurId, String date, String ttc, String nature) {
        Long f = facture(fournisseurId, date, ttc);
        Long r = reglement(f, date, ttc);
        ras.modifierNature(f, nature);
        return r;
    }

    private Long tiersDe(Long fournisseurId) {
        return tiers.parSource(livre.livreId(), "FOURNISSEUR", fournisseurId).orElseThrow().id();
    }

    private static RasLigneDTO ligne(RasMoisDTO mois, Long reglementId) {
        return mois.lignes().stream().filter(l -> reglementId.equals(l.reglementId())).findFirst().orElseThrow();
    }

    /** RAS-07, NC 735 p. 25-28 : dix lignes, 111 187,50 DH à verser. */
    @Test
    void lesDixLignesOfficielles() {
        Long a = fournisseur("A", null);
        Long b = fournisseur("B", null);
        Long c = fournisseur("C", null);
        Long e = fournisseur("D", null);
        List<Long> reglements = List.of(
                reglementDeService(a, "2026-01-05", "3500.00", "SERVICE"),
                reglementDeService(a, "2026-01-06", "30000.00", "SERVICE"),
                reglementDeService(a, "2026-01-07", "4500.00", "SERVICE"),
                reglementDeService(b, "2026-01-08", "3500.00", "SERVICE"),
                reglementDeService(b, "2026-01-09", "45000.00", "SERVICE"),
                reglementDeService(b, "2026-01-10", "4500.00", "SERVICE"),
                reglementDeService(c, "2026-01-11", "15000.00", "SERVICE"),
                reglementDeService(c, "2026-01-12", "600000.00", "BIEN_EQUIPEMENT"),
                reglementDeService(c, "2026-01-13", "30000.00", "MARCHANDISE"),
                reglementDeService(e, "2026-01-14", "300000.00", "TRAVAUX"));
        ras.mois(2026, 1);
        ras.modifierForme(tiersDe(a), true);
        ras.modifierForme(tiersDe(b), true);
        ras.modifierForme(tiersDe(c), false);
        ras.modifierForme(tiersDe(e), false);
        ras.ajouterAttestation(tiersDe(b), new AttestationRequest(d("2026-01-02"), "B-1", true));
        ras.ajouterAttestation(tiersDe(e), new AttestationRequest(d("2026-01-02"), "D-1", true));

        RasMoisDTO mois = ras.mois(2026, 1);
        String[] attendus = {"0.00", "5000.00", "0.00", "0.00", "5625.00", "562.50", "0.00", "100000.00", "0.00", "0.00"};
        BigDecimal attendu = m("0");
        for (int i = 0; i < attendus.length; i++) {
            RasLigneDTO l = ligne(mois, reglements.get(i));
            assertEquals(m(attendus[i]), l.montantRetenu(), "ligne " + (i + 1) + " : " + l.raison());
            assertEquals(m(attendus[i]).signum() > 0 ? "A_RETENIR" : "SANS_RETENUE", l.etat(), "ligne " + (i + 1));
            attendu = attendu.add(m(attendus[i]));
        }
        assertEquals(m("111187.50"), attendu);
        assertEquals(m("38000.00"), ligne(mois, reglements.get(2)).cumulServices());
        assertEquals(m("53000.00"), ligne(mois, reglements.get(5)).cumulServices());
        assertEquals("117-V-b", ligne(mois, reglements.get(1)).article());
        assertEquals("117-IV", ligne(mois, reglements.get(7)).article());

        RasMoisDTO apres = mois;
        for (RasLigneDTO l : mois.lignes()) {
            if ("A_RETENIR".equals(l.etat())) {
                apres = ras.retenir(l.reglementId(), new RasRetenueRequest(l.factureAchatId(), null));
            }
        }
        assertEquals(m("111187.50"), apres.totalRetenu());
        assertEquals(m("111188.00"), apres.versement());
        assertEquals(d("2026-02-28"), apres.dateLimite());
        assertEquals(m("-111187.50"), solde("4458"));
        assertEquals(m("111187.50"), solde("4411"));
        assertEquals(4, apres.lignes().stream().filter(l -> "RETENUE".equals(l.etat())).count());
        assertTrue(ras.bordereauXlsx(2026, 1).length > 0);

        RasLigneDTO faite = ligne(apres, reglements.get(1));
        refuse("COMPTA_RAS_DEJA", () -> ras.retenir(faite.reglementId(), new RasRetenueRequest(faite.factureAchatId(), null)));
    }

    @Test
    void rienNEstSuppose() {
        Long f = facture(sebou, "2026-01-10", "12000.00");
        Long r = reglement(f, "2026-01-10", "12000.00");
        RasLigneDTO sansNature = ligne(ras.mois(2026, 1), r);
        assertEquals("A_QUALIFIER", sansNature.etat());
        refuse("COMPTA_RAS_SANS_RETENUE", () -> ras.retenir(r, new RasRetenueRequest(f, null)));

        ras.modifierNature(f, "SERVICE");
        assertEquals("A_QUALIFIER", ligne(ras.mois(2026, 1), r).etat());

        ras.modifierForme(sansNature.tiersId(), true);
        RasLigneDTO physique = ligne(ras.mois(2026, 1), r);
        assertEquals("A_RETENIR", physique.etat());
        assertEquals(m("100.00"), physique.tauxRetenue().setScale(2));
        assertEquals("ABSENTE", physique.attestation());

        refuse("COMPTA_RAS_NATURE", () -> ras.modifierNature(f, "AUTRE"));
    }

    @Test
    void attestationPerimeeOuNonVerifiee() {
        Long f = facture(sebou, "2026-01-10", "12000.00");
        Long r = reglement(f, "2026-01-10", "12000.00");
        ras.modifierNature(f, "BIEN_EQUIPEMENT");
        Long t = ligne(ras.mois(2026, 1), r).tiersId();

        ras.ajouterAttestation(t, new AttestationRequest(d("2025-07-09"), "Vieille", true));
        RasLigneDTO perimee = ligne(ras.mois(2026, 1), r);
        assertEquals("PERIMEE", perimee.attestation());
        assertEquals("A_RETENIR", perimee.etat());

        ras.ajouterAttestation(t, new AttestationRequest(d("2025-12-01"), "Non vérifiée", false));
        assertEquals("NON_VERIFIEE", ligne(ras.mois(2026, 1), r).attestation());
        assertEquals("A_RETENIR", ligne(ras.mois(2026, 1), r).etat());

        ras.ajouterAttestation(t, new AttestationRequest(d("2025-12-01"), "Vérifiée", true));
        RasLigneDTO valide = ligne(ras.mois(2026, 1), r);
        assertEquals("VALIDE", valide.attestation());
        assertEquals("SANS_RETENUE", valide.etat());

        ras.ajouterAttestation(t, new AttestationRequest(d("2026-02-01"), "Postérieure", true));
        assertEquals("VALIDE", ligne(ras.mois(2026, 1), r).attestation());
        assertEquals(2 + 1, ras.ficheTiers(t).attestations().size());
        Long id = ras.ficheTiers(t).attestations().get(0).id();
        assertEquals(2, ras.retirerAttestation(t, id).attestations().size());
        assertThrows(ResourceNotFoundException.class, () -> ras.retirerAttestation(t, id));
    }

    /** R1, R2 : sur ce règlement, jamais sur la facture entière. */
    @Test
    void laRetenuePorteSurLeReglement() {
        Long f = facture(sebou, "2026-01-05", "120000.00");
        Long premier = reglement(f, "2026-01-10", "12000.00");
        ras.modifierNature(f, "BIEN_EQUIPEMENT");
        RasLigneDTO l = ligne(ras.mois(2026, 1), premier);
        assertEquals(m("2000.00"), l.montantRetenu());
        assertEquals(m("10000.00"), l.montantHt());
        assertEquals(m("20.00"), l.tauxTva());
    }

    /** Le commercial a enregistré le net payé : le montant avant retenue se saisit. */
    @Test
    void montantBrutQuandLeCommercialEnregistreLeNet() {
        Long f = facture(sebou, "2026-01-05", "30000.00");
        Long r = reglement(f, "2026-01-10", "25000.00");
        ras.modifierNature(f, "BIEN_EQUIPEMENT");
        RasMoisDTO apres = ras.retenir(r, new RasRetenueRequest(f, m("30000.00")));
        RasLigneDTO l = ligne(apres, r);
        assertEquals(m("5000.00"), l.montantRetenu());
        assertEquals(m("30000.00"), l.montantBrut());
        assertEquals(m("25000.00"), l.montantPaye());
        refuse("COMPTA_RAS_MONTANT", () -> ras.retenir(r, new RasRetenueRequest(f, m("-1.00"))));
    }

    @Test
    void annulerLaRetenueContrePasse() {
        Long f = facture(sebou, "2026-01-05", "12000.00");
        Long r = reglement(f, "2026-01-10", "12000.00");
        ras.modifierNature(f, "TRAVAUX");
        RasLigneDTO l = ligne(ras.retenir(r, new RasRetenueRequest(f, null)), r);
        assertEquals("RETENUE", l.etat());
        assertEquals(m("-2000.00"), solde("4458"));
        RasLigneDTO annulee = ligne(ras.annuler(l.operationId(), new MotifRequest("Attestation reçue")), r);
        assertEquals("A_RETENIR", annulee.etat());
        assertEquals(0, solde("4458").signum());
        assertEquals(0, solde("4411").signum());
    }

    @Test
    void reglementSansFacture() {
        Long r = reglement(null, "2026-01-10", "500.00");
        assertEquals("SANS_FACTURE", ligne(ras.mois(2026, 1), r).etat());
        assertThrows(ResourceNotFoundException.class, () -> ras.modifierNature(99999999L, "SERVICE"));
    }

    // --- Échéancier

    private static List<EcheanceDTO> deType(EcheancesDTO e, String type) {
        return e.lignes().stream().filter(l -> type.equals(l.type())).toList();
    }

    private static EcheanceDTO deLaPeriode(EcheancesDTO e, String type, String periode) {
        return deType(e, type).stream().filter(l -> periode.equals(l.periode())).findFirst().orElseThrow();
    }

    /** FI7 : le calendrier imposait douze TVA mensuelles. */
    @Test
    void laTvaSuitLaPeriodiciteReelle() {
        EcheancesDTO sans = echeances.generer(2026);
        assertTrue(deType(sans, "TVA").isEmpty());
        assertTrue(sans.avertissements().stream().anyMatch(a -> a.contains("Paramètres TVA")));

        tva.enregistrerParametres(2026, new TvaParametresRequest(true, "ENCAISSEMENT", "MENSUELLE", m("100"), false, 1, m("0")));
        EcheancesDTO mensuelle = echeances.generer(2026);
        assertEquals(12, deType(mensuelle, "TVA").size());
        assertEquals(d("2026-02-28"), deLaPeriode(mensuelle, "TVA", "2026-01").dateLimite());
        assertEquals("CGI art. 110, 155", deLaPeriode(mensuelle, "TVA", "2026-01").article());

        tva.enregistrerParametres(2026, new TvaParametresRequest(true, "ENCAISSEMENT", "TRIMESTRIELLE", m("100"), false, 1, m("0")));
        EcheancesDTO trimestrielle = echeances.generer(2026);
        assertEquals(4, deType(trimestrielle, "TVA").size());
        assertEquals(d("2026-04-30"), deLaPeriode(trimestrielle, "TVA", "2026-T1").dateLimite());
        assertEquals("CGI art. 111, 155", deLaPeriode(trimestrielle, "TVA", "2026-T1").article());
    }

    @Test
    void lesAutresObligations() {
        EcheancesDTO e = echeances.generer(2026);
        assertEquals(12, deType(e, "RAS_TVA").size());
        assertEquals(d("2026-02-28"), deLaPeriode(e, "RAS_TVA", "2026-01").dateLimite());
        assertTrue(deType(e, "IS_ACOMPTE").isEmpty());
        assertTrue(deType(e, "CM_MINIMUM").isEmpty());
        assertTrue(e.avertissements().stream().anyMatch(a -> a.contains("Premier exercice")));
        assertEquals(d("2027-03-31"), deLaPeriode(e, "IS_RESULTAT", "2026").dateLimite());
        assertEquals(d("2027-03-31"), deLaPeriode(e, "ETATS_SYNTHESE", "2026").dateLimite());
        assertEquals(d("2027-01-31"), deLaPeriode(e, "DELAIS_PAIEMENT", "2026-T4").dateLimite());
        assertEquals(0, e.joursFeries());
        assertTrue(e.avertissements().stream().anyMatch(a -> a.contains("jours fériés")));
    }

    /** Un exercice qui suit un autre a ses quatre acomptes. */
    @Test
    void lesAcomptesDUnExerciceSuivant() {
        exerciceSuivant();
        EcheancesDTO e = echeances.generer(2027);
        assertEquals(4, deType(e, "IS_ACOMPTE").size());
        assertEquals(d("2027-03-31"), deLaPeriode(e, "IS_ACOMPTE", "2027-AC1").dateLimite());
        assertEquals(d("2027-12-31"), deLaPeriode(e, "IS_ACOMPTE", "2027-AC4").dateLimite());
        assertEquals(d("2027-03-31"), deLaPeriode(e, "CM_MINIMUM", "2027").dateLimite());
    }

    private void exerciceSuivant() {
        exercicesLus.creer(livre.livreId(), "2027", d("2027-01-01"), d("2027-12-31"), livre.exerciceCourantId());
    }

    @Test
    void reportDesEcheancesParLaListeDesJoursFeries() {
        jdbc.update("INSERT INTO compta_jours_feries (jour, libelle) VALUES ('2027-03-31', 'Essai')");
        EcheancesDTO e = echeances.generer(2026);
        assertEquals(d("2027-04-01"), deLaPeriode(e, "IS_RESULTAT", "2026").dateLimite());
        assertEquals(1, e.joursFeries());
    }

    @Test
    void uneOptionNonExerceeTombeSansRetard() {
        EcheanceDTO option = deLaPeriode(echeances.generer(2024), "DELAIS_PAIEMENT", "2024");
        assertEquals(d("2025-03-31"), option.dateLimite());
        tva.enregistrerParametres(2025, new TvaParametresRequest(true, "ENCAISSEMENT", "MENSUELLE", m("100"), false, 1, m("0")));
        EcheanceDTO lue = deLaPeriode(echeances.generer(2025), "TVA_OPTION_MENSUELLE", "2025");
        assertEquals("SANS_OBJET", lue.statut());
        assertFalse(lue.enRetard());
    }

    @Test
    void uneEcheanceFaiteLeReste() {
        EcheancesDTO e = echeances.generer(2026);
        EcheanceDTO acompte = deLaPeriode(e, "ETATS_SYNTHESE", "2026");
        EcheanceDTO faite = echeances.marquer(acompte.id(), new EcheanceRequest("FAIT", d("2026-12-20"), "Quittance 12"));
        assertEquals("FAIT", faite.statut());
        assertFalse(faite.enRetard());
        EcheanceDTO relue = deLaPeriode(echeances.generer(2026), "ETATS_SYNTHESE", "2026");
        assertEquals("FAIT", relue.statut());
        assertEquals("Quittance 12", relue.reference());
        refuse("COMPTA_ECHEANCE_DATE", () -> echeances.marquer(acompte.id(), new EcheanceRequest("FAIT", null, null)));
        refuse("COMPTA_ECHEANCE_STATUT", () -> echeances.marquer(acompte.id(), new EcheanceRequest("AUTRE", null, null)));
        assertThrows(ResourceNotFoundException.class, () -> echeances.marquer(99999999L, new EcheanceRequest("FAIT", d("2026-12-20"), null)));
    }

    @Test
    void uneDeclarationDeposeeSeMarqueSeule() {
        delais.calculer(2026, 1, CA);
        delais.valider(2026, 1, new DpValidationRequest(d("2026-01-31")));
        delais.deposer(2026, 1, new DpDepotRequest(d("2026-04-15")));
        EcheanceDTO l = deLaPeriode(echeances.generer(2026), "DELAIS_PAIEMENT", "2026-T1");
        assertEquals("FAIT", l.statut());
        assertEquals(d("2026-04-15"), l.faitLe());
    }
}
