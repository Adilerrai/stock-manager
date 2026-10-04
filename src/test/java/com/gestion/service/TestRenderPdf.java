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
            params.put("clientAdresse", "PORT CASABLANCA");
            params.put("clientTelephone", "05 22 20 30 40");
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
            params.put("clientAdresse", "PORT CASABLANCA");
            params.put("clientTelephone", "05 22 20 30 40");
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
            params.put("clientAdresse", "PORT CASABLANCA");
            params.put("clientTelephone", "05 22 20 30 40");
            params.put("clientIce", "001827461000039");
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
}
