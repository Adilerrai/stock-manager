package com.gestion.compta;

import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.service.compta.FormulaireTva;
import com.gestion.service.compta.PieceSource.LigneSource;
import com.gestion.service.compta.TvaCalcul;
import com.gestion.service.compta.TvaCalcul.BaseTaxe;
import com.gestion.service.compta.TvaCalcul.LigneCalculee;
import com.gestion.service.compta.TvaCalcul.PartTaux;
import com.gestion.service.compta.TvaCalcul.Periode;
import com.gestion.service.compta.TvaCalcul.Tranche;
import com.gestion.service.compta.TvaFichiers;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("compta")
class TvaCalculTest {

    private static BigDecimal m(String v) {
        return new BigDecimal(v);
    }

    private static LigneSource ligne(String taux, String ht, String tva) {
        return new LigneSource(1L, "MARCHANDISE", null, m(taux), m(ht), m(tva), m(ht).add(m(tva)));
    }

    private static void egal(String attendu, BigDecimal valeur) {
        assertEquals(0, m(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    @Test
    void s7CentDixEncaissesSurUneFactureADixPourCentDonnentDix() {
        List<PartTaux> parts = TvaCalcul.ventilation(List.of(ligne("10", "100.00", "10.00")), BigDecimal.ZERO, "NET_TVA_FACTUREE");

        Tranche t = TvaCalcul.repartir(m("110.00"), parts).get(0);

        egal("10.00", t.tva());
        egal("100.00", t.ht());
        egal("110.00", t.ttc());
    }

    @Test
    void uneRemiseGlobaleLaisseLaTvaImprimeeEtSolde4455() {
        List<PartTaux> parts = TvaCalcul.ventilation(List.of(ligne("20", "1000.00", "200.00")), m("200.00"), "NET_TVA_FACTUREE");

        Tranche t = TvaCalcul.repartir(m("1000.00"), parts).get(0);

        egal("800.00", t.ht());
        egal("200.00", t.tva());
        egal("1000.00", t.ttc());
    }

    @Test
    void enModeReduitLaRemiseBaisseAussiLaTva() {
        List<PartTaux> parts = TvaCalcul.ventilation(List.of(ligne("20", "1000.00", "200.00")), m("120.00"), "NET_TVA_REDUITE");

        egal("900.00", parts.get(0).ht());
        egal("180.00", parts.get(0).tva());
    }

    @Test
    void uneVenteMultiTauxEncaisseePartiellementResteJusteAuCentime() {
        List<PartTaux> parts = TvaCalcul.ventilation(List.of(ligne("20", "100.00", "20.00"), ligne("10", "33.33", "3.33")), BigDecimal.ZERO, "NET_TVA_FACTUREE");

        List<Tranche> tranches = TvaCalcul.repartir(m("77.77"), parts);

        assertEquals(2, tranches.size());
        egal("77.77", tranches.stream().map(Tranche::ttc).reduce(BigDecimal.ZERO, BigDecimal::add));
        for (Tranche t : tranches) {
            egal(t.ttc().toPlainString(), t.ht().add(t.tva()));
        }
    }

    @Test
    void deuxTranchesFontExactementLaTvaDeLaFacture() {
        PartTaux part = new PartTaux(m("20.00"), m("83.33"), m("16.67"), m("100.00"));

        Tranche premiere = TvaCalcul.solder(TvaCalcul.repartir(m("50.00"), List.of(part)).get(0), part, BigDecimal.ZERO, BigDecimal.ZERO);
        Tranche seconde = TvaCalcul.solder(TvaCalcul.repartir(m("50.00"), List.of(part)).get(0), part, premiere.ttc(), premiere.tva());

        egal("16.67", premiere.tva().add(seconde.tva()));
        egal("100.00", premiere.ttc().add(seconde.ttc()));
        egal("50.00", seconde.ht().add(seconde.tva()));
    }

    @Test
    void leHorsTaxesSeCalculeDAbordCommeLeDitLaSpecification() {
        Tranche t = TvaCalcul.repartir(m("33.33"), List.of(new PartTaux(m("20.00"), m("100.00"), m("20.00"), m("120.00")))).get(0);

        egal("27.78", t.ht());
        egal("5.55", t.tva());
    }

    @Test
    void uneFactureEntreMarchandisesEtServicesSePartage() {
        Map<String, BigDecimal> parts = TvaCalcul.partager(m("100.00"), Map.of("080", m("2"), "081", m("1")));

        egal("66.67", parts.get("080"));
        egal("33.33", parts.get("081"));
    }

    private static Periode periode(Map<String, BigDecimal> horsTaxable, String ca, Map<String, BaseTaxe> ventes, Map<String, BaseTaxe> deductions) {
        return new Periode(m(ca), horsTaxable, ventes, deductions);
    }

    private static BigDecimal taxe(List<LigneCalculee> lignes, String code) {
        return lignes.stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError("ligne " + code)).taxe();
    }

    private static BigDecimal base(List<LigneCalculee> lignes, String code) {
        return lignes.stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow(() -> new AssertionError("ligne " + code)).base();
    }

    @Test
    void premierExempleDeLaLiquidationLaisseTrenteCinqAPayer() {
        List<LigneCalculee> l = TvaCalcul.lignes(periode(Map.of(), "500.00", Map.of("080", new BaseTaxe(m("500.00"), m("100.00"))),
                Map.of("146", new BaseTaxe(m("50.00"), m("50.00")), "163", new BaseTaxe(m("10.00"), m("10.00")))), m("5.00"), Map.of());

        egal("100.00", taxe(l, "132"));
        egal("60.00", taxe(l, "182"));
        egal("5.00", taxe(l, "170"));
        egal("65.00", taxe(l, "190"));
        egal("35.00", taxe(l, "200"));
        egal("0.00", taxe(l, "201"));
        egal("500.00", base(l, "060"));
    }

    @Test
    void secondExempleDeLaLiquidationReporteVingtCinq() {
        List<LigneCalculee> l = TvaCalcul.lignes(periode(Map.of(), "200.00", Map.of("080", new BaseTaxe(m("200.00"), m("40.00"))), Map.of("146", new BaseTaxe(m("60.00"), m("60.00")))), m("5.00"), Map.of());

        egal("0.00", taxe(l, "200"));
        egal("25.00", taxe(l, "201"));
        egal("20.00", taxe(l, "205"));
    }

    @Test
    void lesLignesSaisiesEntrentDansLesFormules() {
        Map<String, BaseTaxe> saisies = Map.of("120", new BaseTaxe(null, m("10.00")), "180", new BaseTaxe(null, m("4.00")), "186", new BaseTaxe(null, m("1.00")));

        List<LigneCalculee> l = TvaCalcul.lignes(periode(Map.of(), "100.00", Map.of("080", new BaseTaxe(m("100.00"), m("20.00"))), Map.of()), BigDecimal.ZERO, saisies);

        egal("30.00", taxe(l, "132"));
        egal("3.00", taxe(l, "190"));
        egal("27.00", taxe(l, "200"));
    }

    @Test
    void lesDatesLimitesSuiventLesArticles110Et111() {
        assertEquals(LocalDate.parse("2026-10-31"), TvaCalcul.dateLimite(TvaCalcul.finDePeriode(2026, "MENSUELLE", 9)));
        assertEquals(LocalDate.parse("2026-04-30"), TvaCalcul.dateLimite(TvaCalcul.finDePeriode(2026, "TRIMESTRIELLE", 1)));
        assertEquals(LocalDate.parse("2027-01-31"), TvaCalcul.dateLimite(TvaCalcul.finDePeriode(2026, "TRIMESTRIELLE", 4)));
        assertEquals(LocalDate.parse("2026-02-28"), TvaCalcul.finDePeriode(2026, "MENSUELLE", 2));
    }

    @Test
    void lesModesDePaiementSuiventLeCahier() {
        assertEquals(1, TvaCalcul.modePaiementDgi("ESPECES"));
        assertEquals(2, TvaCalcul.modePaiementDgi("CHEQUE"));
        assertEquals(4, TvaCalcul.modePaiementDgi("VIREMENT"));
        assertEquals(5, TvaCalcul.modePaiementDgi("LCN"));
        assertEquals(7, TvaCalcul.modePaiementDgi("CARTE_BANCAIRE"));
    }

    @Test
    void leFormulaireSeChargeEtVerifieSesRubriques() {
        FormulaireTva f = new FormulaireTva();

        assertEquals("ADC080B-26I", f.modele());
        assertEquals("080", f.ligneDeVente("MARCHANDISE", m("20")));
        assertEquals("081", f.ligneDeVente("SERVICE", m("20")));
        assertEquals("118", f.ligneDeVente("MARCHANDISE", m("10")));
        assertEquals("146", f.ligneDeDeduction("ACHATS_INTERIEUR", m("20")));
        assertEquals("150", f.ligneDeDeduction("ACHATS_INTERIEUR", m("10")));
        assertEquals("142", f.ligneDeDeduction("FRAIS_BANCAIRES", m("10")));
        assertEquals("163", f.ligneDeDeduction("IMMOBILISATION_INTERIEUR", m("20")));
        assertEquals("34551", f.rubrique("IMMOBILISATION_INTERIEUR").orElseThrow().compteTva());
        assertEquals("34552", f.rubrique("ACHATS_INTERIEUR").orElseThrow().compteTva());
        assertNull(f.ligneDeDeduction("FRAIS_BANCAIRES", m("20")), "une rubrique ne prend pas un taux que le formulaire n'a pas");
        assertEquals("030", f.ligneDeTraitement("EXONERE_91"));
    }

    // --- Le relevé

    private static TvaDeductionDTO operation(int ordre, String nom, String prorata) {
        return new TvaDeductionDTO((long) ordre, "REGLEMENT_FOURNISSEUR", 1L, ordre, "F-000" + ordre, LocalDate.parse("2026-09-01"), nom, "00000000", "000000000000000", "Designation du bien ou service",
                "ACHATS_INTERIEUR", "146", "34552", m("100.00"), m("20.00"), m("20.00"), m("120.00"), m(prorata), m("20.00"), 2, "CHQ 1", LocalDate.parse("2026-09-15"), null);
    }

    private static String xml(TvaFichiers.Releve r) {
        return new String(r.contenu(), 3, r.contenu().length - 3, StandardCharsets.UTF_8);
    }

    @Test
    void leReleveSuitLExempleDuCahierBaliseParBalise() {
        TvaFichiers.Releve r = new TvaFichiers().releve("00000000", 2026, 3, 2, 1, List.of(operation(1, "FOURNISSEUR EXEMPLE", "100.00")));

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, new byte[]{r.contenu()[0], r.contenu()[1], r.contenu()[2]}, "BOM UTF-8");
        String attendu = """
                <DeclarationReleveDeduction>
                	<identifiantFiscal>00000000</identifiantFiscal>
                	<annee>2026</annee>
                	<periode>3</periode>
                	<regime>2</regime>
                	<releveDeductions>
                		<rd>
                			<ord>1</ord>
                			<num>F-0001</num>
                			<des>Designation du bien ou service</des>
                			<mht>100.00</mht>
                			<tva>20.00</tva>
                			<ttc>120.00</ttc>
                			<refF>
                				<if>00000000</if>
                				<nom>FOURNISSEUR EXEMPLE</nom>
                				<ice>000000000000000</ice>
                			</refF>
                			<tx>20.00</tx>
                			<mp>
                				<id>2</id>
                			</mp>
                			<dpai>2026-09-15</dpai>
                			<dfac>2026-09-01</dfac>
                		</rd>
                	</releveDeductions>
                </DeclarationReleveDeduction>""".replace("\n", "\r\n");
        assertEquals(attendu, xml(r), "mêmes éléments, même ordre, sans prologue ni namespace, prorata omis à 100 %");
        assertEquals("RD_00000000_2026_03_v1.xml", r.nomXml());
        new TvaFichiers().verifier(r.contenu());
    }

    @Test
    void unProrataDeQuatreVingtPourCentApparaitASaPlace() {
        String x = xml(new TvaFichiers().releve("00000000", 2026, 9, 1, 1, List.of(operation(1, "A", "80.00"))));

        assertTrue(x.contains("<tx>20.00</tx>\r\n\t\t\t<prorata>80</prorata>\r\n\t\t\t<mp>"), x);
    }

    @Test
    void lesCaracteresSpeciauxRestentUnXmlBienForme() {
        TvaFichiers f = new TvaFichiers();
        TvaFichiers.Releve r = f.releve("00000000", 2026, 9, 1, 1, List.of(operation(1, "Sté A&B <Casa>", "100.00")));

        f.verifier(r.contenu());
        assertTrue(xml(r).contains("Sté A&amp;B &lt;Casa"), xml(r));
    }

    @Test
    void leZipContientUnSeulXmlLisible() throws Exception {
        TvaFichiers f = new TvaFichiers();
        TvaFichiers.Releve r = f.releve("00000000", 2026, 9, 1, 2, List.of(operation(1, "A", "100.00")));

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(f.zip(r)))) {
            ZipEntry entree = zip.getNextEntry();
            assertNotNull(entree);
            assertEquals("RD_00000000_2026_09_v2.xml", entree.getName());
            assertArrayEquals(r.contenu(), zip.readAllBytes());
            assertNull(zip.getNextEntry(), "une seule entrée");
        }
    }

    @Test
    void leSchemaInterneRefuseUnModeHorsDuCahier() {
        TvaFichiers f = new TvaFichiers();
        TvaDeductionDTO o = operation(1, "A", "100.00");
        TvaDeductionDTO faux = new TvaDeductionDTO(o.id(), o.sourceType(), o.sourceId(), o.ordre(), o.numeroFacture(), o.dateFacture(), o.fournisseurNom(), o.fournisseurIf(), o.fournisseurIce(), o.designation(), o.rubriqueCode(), o.ligneCode(), o.compteTva(),
                o.ht(), o.taux(), o.tva(), o.ttc(), o.prorata(), o.tvaDeductible(), 9, o.referencePaiement(), o.datePaiement(), null);

        assertThrows(IllegalStateException.class, () -> f.verifier(f.releve("00000000", 2026, 9, 1, 1, List.of(faux)).contenu()));
        assertFalse(xml(f.releve("00000000", 2026, 9, 1, 1, List.of(o))).startsWith("<?xml"), "pas de prologue");
    }
}
