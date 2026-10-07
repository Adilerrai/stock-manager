package com.gestion.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

public class TestRenderPdf {

    private Map<String, Object> createCommonParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("nomEntreprise", "TECHNOFIM GLOBAL");
        params.put("activiteEntreprise", "Négoce & Fournitures Générales");
        params.put("adresseEntreprise", "46, Bd Zerktouni Etg 2 Apt 6 - Casablanca");
        params.put("telephoneEntreprise", "05 22 32 93 74");
        params.put("telephoneSecondaire", "06 64 12 44 87");
        params.put("gsmEntreprise", "06 64 12 44 87");
        params.put("emailEntreprise", "technofimsarl@gmail.com");
        params.put("rcEntreprise", "727469");
        params.put("nifEntreprise", "72090385");
        params.put("patenteEntreprise", "34217516");
        params.put("iceEntreprise", "003949126000072");
        params.put("cnssEntreprise", "6916121");
        params.put("devise", "MAD");
        params.put("banqueEntreprise", "");
        params.put("ribEntreprise", "");
        return params;
    }

    private List<Map<String, Object>> createSampleItems() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[][] items = {
            {"P0007", "CABLE ELECTRIQUE 4X2,5 H05VV-F", "100.00", "26.50", "2650.00"},
            {"P0008", "RELAIS MINIAT ENFICHABLE PH2961105", "20.00", "82.00", "1640.00"},
            {"P0005", "CHIFFON", "100.00", "9.50", "950.00"},
            {"P0006", "TORCHE RECHARGEABLE", "3.00", "240.00", "720.00"},
            {"P0022", "Régulateur de pression à double manomètre pour bouteille d'oxygène", "1.00", "680.00", "680.00"},
            {"P0009", "Tige filetée galvanisée M33", "2.00", "4.20", "8.40"},
            {"P0010", "Écrou galvanisé M33", "4.00", "20.00", "80.00"},
            {"P0011", "Écrou galvanisé M32", "4.00", "7.50", "30.00"},
            {"P0012", "Baguette 3,15 Somati", "4.00", "140.00", "560.00"},
            {"P0013", "Ruban de Balisage", "3.00", "55.00", "165.00"},
            {"P0014", "Disque 125 x 1,6 x 22,23", "25.00", "12.00", "300.00"},
            {"P0015", "Vis TH HR CL 8.8 x 20", "100.00", "1.80", "180.00"},
            {"P0016", "Rondelle Cad M8", "100.00", "1.50", "150.00"},
            {"P0017", "Rondelle Cad M6", "100.00", "1.50", "150.00"},
            {"P0018", "Écrou hex Cad M8", "10.00", "1.80", "18.00"},
            {"P0019", "Clé à molette 18\" FACOM", "2.00", "580.00", "1160.00"},
            {"P0020", "Clé à molette 12\" FACOM", "1.00", "640.00", "640.00"},
            {"P0021", "Clé à Torx CR-V Pro", "1.00", "90.00", "90.00"},
            {"P0023", "SINTOFER", "10.00", "46.00", "460.00"},
            {"P0024", "SCOTCH NOIR ELECTRICIEN", "10.00", "10.50", "105.00"},
            {"P0025", "FORET M12 CYLINDRIQUE INOX", "6.00", "120.00", "720.00"},
            {"P0026", "COLLE REINZ", "4.00", "70.00", "280.00"},
            {"P0027", "ROBINET EN LAITON", "3.00", "65.00", "195.00"},
            {"P0028", "BOUCHON MALE 1/2", "5.00", "2.50", "12.50"},
            {"P0029", "ENTONNOIR", "1.00", "90.00", "90.00"},
            {"P0030", "VIS SPAX 5X15", "100.00", "1.50", "150.00"},
            {"P0031", "RONDELLE GALV 16X50", "50.00", "6.80", "340.00"},
            {"P0032", "DILUANT CELLULOSIQUE BIDON 5L", "1.00", "130.00", "130.00"},
            {"P0033", "POMPE DE PEINTURE 400ML", "2.00", "22.00", "44.00"}
        };

        for (String[] it : items) {
            Map<String, Object> row = new HashMap<>();
            // for commande_client & facture_client
            row.put("reference", it[0]);
            row.put("designation", it[1]);
            row.put("quantite", new BigDecimal(it[2]));
            row.put("prixUnitaire", new BigDecimal(it[3]));
            row.put("prixUnitaireHT", new BigDecimal(it[3]));
            row.put("tauxTVA", new BigDecimal("20.00"));
            row.put("montantHT", new BigDecimal(it[4]));
            row.put("montantTotal", new BigDecimal(it[4]));
            row.put("montantTTC", new BigDecimal(it[4]).multiply(new BigDecimal("1.20")));

            // for bon_livraison_client
            row.put("produitReference", it[0]);
            row.put("produitDesignation", it[1]);
            row.put("depotNom", "Dépôt Principal");
            row.put("quantiteLivree", new BigDecimal(it[2]));
            row.put("prixVente", new BigDecimal(it[3]));

            data.add(row);
        }
        return data;
    }

    @Test
    public void renderAllClientReports() throws Exception {
        List<Map<String, Object>> data = createSampleItems();

        // 1. Commande Client
        {
            InputStream is = getClass().getClassLoader().getResourceAsStream("reports/commande_client.jrxml");
            JasperReport jr = JasperCompileManager.compileReport(is);
            Map<String, Object> params = createCommonParams();
            params.put("numeroCommande", "CC-2026-004");
            params.put("dateCommande", "04/10/2026 00:00");
            params.put("clientNom", "STE MASS CEREALES");
            params.put("clientAdresse", "20, Rue Mostafa el Maani, CASABLANCA");
            params.put("clientTelephone", "");
            params.put("clientIce", "00152677600001");
            params.put("montantTotal", new BigDecimal("15237.48"));
            params.put("acompteVerse", BigDecimal.ZERO);
            params.put("soldeRestant", new BigDecimal("15237.48"));

            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(data));
            JasperExportManager.exportReportToPdfFile(jp, "uploads/test_commande_client_moderne.pdf");
            System.out.println("Generated uploads/test_commande_client_moderne.pdf");
        }

        // 2. Bon de Livraison Client
        {
            InputStream is = getClass().getClassLoader().getResourceAsStream("reports/bon_livraison_client.jrxml");
            JasperReport jr = JasperCompileManager.compileReport(is);
            Map<String, Object> params = createCommonParams();
            params.put("numeroBl", "BL-2026-004");
            params.put("dateBl", "04/10/2026 14:30");
            params.put("commandeReference", "CC-2026-004");
            params.put("depotNom", "Dépôt Principal");
            params.put("clientNom", "STE MASS CEREALES");
            params.put("clientAdresse", "20, Rue Mostafa el Maani, CASABLANCA");
            params.put("clientTelephone", "");
            params.put("clientIce", "00152677600001");
            params.put("montantTotal", new BigDecimal("15237.48"));

            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(data));
            JasperExportManager.exportReportToPdfFile(jp, "uploads/test_bon_livraison_client_moderne.pdf");
            System.out.println("Generated uploads/test_bon_livraison_client_moderne.pdf");
        }

        // 3. Facture Client
        {
            InputStream is = getClass().getClassLoader().getResourceAsStream("reports/facture_client.jrxml");
            JasperReport jr = JasperCompileManager.compileReport(is);
            Map<String, Object> params = createCommonParams();
            params.put("numeroFacture", "FA-2026-004");
            params.put("dateFacture", "04/10/2026");
            params.put("dateEcheance", "04/11/2026");
            params.put("modePaiement", "Virement Bancaire (30 jours)");
            params.put("bonLivraisonNumeros", "BL-2026-004");
            params.put("clientNom", "STE MASS CEREALES");
            params.put("clientAdresse", "20, Rue Mostafa el Maani, CASABLANCA");
            params.put("clientTelephone", "");
            params.put("clientIce", "00152677600001");
            params.put("montantHT", new BigDecimal("15237.48"));
            params.put("montantTVA", new BigDecimal("3047.50"));
            params.put("montantTTC", new BigDecimal("18284.98"));
            params.put("remiseGlobale", BigDecimal.ZERO);
            params.put("montantFinal", new BigDecimal("18284.98"));
            params.put("montantPaye", BigDecimal.ZERO);
            params.put("montantRestant", new BigDecimal("18284.98"));
            params.put("notes", "");

            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(data));
            JasperExportManager.exportReportToPdfFile(jp, "uploads/test_facture_client_moderne.pdf");
            System.out.println("Generated uploads/test_facture_client_moderne.pdf");
        }
    }

    @Test
    public void testCommandeFournisseurFixed25Lines() throws Exception {
        // Test with 3 items padded to 25 items -> exactly 1 page
        List<Map<String, Object>> threeItems = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>();
        r1.put("reference", "ART-001");
        r1.put("designation", "DISQUE DUR EXTERNE 1TO USB 3.0");
        r1.put("quantite", new BigDecimal("5"));
        r1.put("prixUnitaire", new BigDecimal("650.00"));
        r1.put("montantTotal", new BigDecimal("3250.00"));
        threeItems.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("reference", "ART-002");
        r2.put("designation", "CLAVIER SANS FIL LOGITECH K380");
        r2.put("quantite", new BigDecimal("10"));
        r2.put("prixUnitaire", new BigDecimal("350.00"));
        r2.put("montantTotal", new BigDecimal("3500.00"));
        threeItems.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("reference", "ART-003");
        r3.put("designation", "SOURIS OPTIQUE ERGONOMIQUE NOIRE");
        r3.put("quantite", new BigDecimal("10"));
        r3.put("prixUnitaire", new BigDecimal("180.00"));
        r3.put("montantTotal", new BigDecimal("1800.00"));
        threeItems.add(r3);

        // Pad to 25 lines
        int count = threeItems.size();
        for (int i = count; i < 25; i++) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("reference", "");
            empty.put("designation", "");
            empty.put("quantite", null);
            empty.put("prixUnitaire", null);
            empty.put("montantTotal", null);
            threeItems.add(empty);
        }

        InputStream is = getClass().getClassLoader().getResourceAsStream("reports/commande_fournisseur.jrxml");
        JasperReport jr = JasperCompileManager.compileReport(is);
        Map<String, Object> params = createCommonParams();
        params.put("numeroCommande", "CF-2026-001");
        params.put("dateCommande", "07/10/2026 10:00");
        params.put("dateLivraisonPrevue", "15/10/2026");
        params.put("statut", "VALIDÉE");
        params.put("fournisseurNom", "SOPHATEL MAROC SARL");
        params.put("fournisseurAdresse", "Zone Industrielle Ain Sebaa, Casablanca");
        params.put("fournisseurTelephone", "05 22 35 44 12");
        params.put("fournisseurIce", "001234567890001");
        params.put("montantHT", new BigDecimal("8550.00"));
        params.put("montantTVA", new BigDecimal("1710.00"));
        params.put("montantTTC", new BigDecimal("10260.00"));
        params.put("montantTotal", new BigDecimal("10260.00"));
        params.put("montantEnLettres", "DIX MILLE DEUX CENT SOIXANTE DIRHAMS");
        params.put("observations", "Livraison urgente demandée avant le 15 du mois.");

        JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(threeItems));
        JasperExportManager.exportReportToPdfFile(jp, "uploads/test_commande_fournisseur_25lignes_page1.pdf");
        java.awt.image.BufferedImage img = (java.awt.image.BufferedImage) JasperPrintManager.printPageToImage(jp, 0, 1.5f);
        javax.imageio.ImageIO.write(img, "PNG", new File("uploads/test_commande_fournisseur_page1.png"));
        System.out.println("Generated uploads/test_commande_fournisseur_page1.png");
        org.junit.jupiter.api.Assertions.assertEquals(1, jp.getPages().size(), "3 items padded to 25 must fit on exactly 1 page!");
    }

    @Test
    public void testCommandeFournisseur2Pages25LinesEach() throws Exception {
        // 29 items padded to 50 items -> exactly 2 pages (25 on page 1, 25 on page 2)
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 1; i <= 29; i++) {
            Map<String, Object> r = new HashMap<>();
            r.put("reference", "ART-" + String.format("%03d", i));
            r.put("designation", "ARTICLE TEST TECHNIQUE NUMÉRO " + i);
            r.put("quantite", new BigDecimal(i));
            r.put("prixUnitaire", new BigDecimal("100.00"));
            r.put("montantTotal", new BigDecimal(i * 100));
            items.add(r);
        }

        // Pad to multiple of 25 (50 items)
        int count = items.size();
        int rem = count % 25;
        int missing = (rem == 0) ? 0 : (25 - rem);
        for (int i = 0; i < missing; i++) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("reference", "");
            empty.put("designation", "");
            empty.put("quantite", null);
            empty.put("prixUnitaire", null);
            empty.put("montantTotal", null);
            items.add(empty);
        }
        org.junit.jupiter.api.Assertions.assertEquals(50, items.size());

        InputStream is = getClass().getClassLoader().getResourceAsStream("reports/commande_fournisseur.jrxml");
        JasperReport jr = JasperCompileManager.compileReport(is);
        Map<String, Object> params = createCommonParams();
        params.put("numeroCommande", "CF-2026-002");
        params.put("dateCommande", "07/10/2026 11:30");
        params.put("dateLivraisonPrevue", "20/10/2026");
        params.put("statut", "VALIDÉE");
        params.put("fournisseurNom", "SOPHATEL MAROC SARL");
        params.put("fournisseurAdresse", "Zone Industrielle Ain Sebaa, Casablanca");
        params.put("fournisseurTelephone", "05 22 35 44 12");
        params.put("fournisseurIce", "001234567890001");
        params.put("montantHT", new BigDecimal("43500.00"));
        params.put("montantTVA", new BigDecimal("8700.00"));
        params.put("montantTTC", new BigDecimal("52200.00"));
        params.put("montantTotal", new BigDecimal("52200.00"));
        params.put("montantEnLettres", "CINQUANTE-DEUX MILLE DEUX CENTS DIRHAMS");
        params.put("observations", "Livraison échelonnée en 2 fois.");

        JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(items));
        JasperExportManager.exportReportToPdfFile(jp, "uploads/test_commande_fournisseur_25lignes_page2.pdf");
        System.out.println("Generated uploads/test_commande_fournisseur_25lignes_page2.pdf with page count: " + jp.getPages().size());
        org.junit.jupiter.api.Assertions.assertEquals(2, jp.getPages().size(), "50 items (2x25) must produce exactly 2 pages!");
    }
}
