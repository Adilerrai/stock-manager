package com.gestion.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.renderers.DataRenderable;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QR code imprimé sur le BL client et la facture client : contenu décodable à l'identique
 * ("TADBEER:BL:<numeroBl>" / "TADBEER:FACTURE:<numeroFacture>") et gabarits Jasper qui le rendent sans erreur.
 */
class QrCodeDocumentTest {

    private static final float ZOOM_RENDU = 3f;

    // ---------- Contenu du QR ----------

    @Test
    void prefixesConformesAuFormatPartageAvecLeFrontend() {
        assertEquals("TADBEER:BL:", ImpressionService.QR_PREFIXE_BL);
        assertEquals("TADBEER:FACTURE:", ImpressionService.QR_PREFIXE_FACTURE);
        assertEquals("qrCode", ImpressionService.PARAM_QR_CODE);
    }

    @Test
    void qrDecodeExactementLeContenuBlEtFacture() throws Exception {
        // Numéros typiques + cas limites (minuscules, slash, accents -> mode octet UTF-8)
        List<String> numeros = List.of("BL-2026-004", "WH/OUT/00003", "FA-2026-0042", "fa-2026/0042", "BL-ÉTÉ-01");
        for (String numero : numeros) {
            for (String prefixe : List.of(ImpressionService.QR_PREFIXE_BL, ImpressionService.QR_PREFIXE_FACTURE)) {
                Map<String, Object> params = new HashMap<>();
                ImpressionService.ajouterQrCode(params, prefixe, numero);

                Object qr = params.get("qrCode");
                assertInstanceOf(BufferedImage.class, qr, "Image QR attendue pour " + prefixe + numero);
                BufferedImage image = (BufferedImage) qr;
                assertTrue(image.getWidth() >= 290 && image.getWidth() <= 310, "Taille ~300 px attendue : " + image.getWidth());
                assertEquals(image.getWidth(), image.getHeight());
                // Canal alpha requis pour que Jasper l'encode en PNG (sans perte) et non en JPEG
                assertTrue(image.getColorModel().hasAlpha());
                assertEquals(0xFFFFFFFF, image.getRGB(0, 0), "Coin en marge blanche opaque attendu");

                assertEquals(prefixe + numero, decoder(image));
            }
        }
    }

    @Test
    void numeroVideNeProduitPasDImage() {
        for (String numero : Arrays.asList(null, "", "   ")) {
            Map<String, Object> params = new HashMap<>();
            ImpressionService.ajouterQrCode(params, ImpressionService.QR_PREFIXE_BL, numero);
            ImpressionService.ajouterQrCode(params, ImpressionService.QR_PREFIXE_FACTURE, numero);
            assertFalse(params.containsKey("qrCode"), "Aucun QR attendu pour le numéro [" + numero + "]");
        }
    }

    // ---------- Gabarits Jasper ----------

    @Test
    void bonLivraisonClientAfficheUnQrLisible() throws Exception {
        String numero = "WH/OUT/00003";
        Map<String, Object> params = paramsCommuns();
        params.put("numeroBl", numero);
        params.put("dateBl", "22/10/2026 17:41");
        params.put("statut", "LIVREE");
        params.put("montantTotal", new BigDecimal("6578.40"));
        params.put("totalQuantite", new BigDecimal("3"));
        ImpressionService.ajouterQrCode(params, ImpressionService.QR_PREFIXE_BL, numero);

        verifierRapport("bon_livraison_client", params, "TADBEER:BL:" + numero);
    }

    @Test
    void factureClientAfficheUnQrLisible() throws Exception {
        String numero = "FA-2026-004";
        Map<String, Object> params = paramsCommuns();
        params.put("numeroFacture", numero);
        params.put("dateFacture", "04/10/2026");
        params.put("dateEcheance", "04/11/2026");
        params.put("modePaiement", "Virement bancaire");
        params.put("bonLivraisonNumeros", "BL-2026-004");
        params.put("montantHT", new BigDecimal("100.00"));
        params.put("montantTVA", new BigDecimal("20.00"));
        params.put("montantTTC", new BigDecimal("120.00"));
        params.put("remiseGlobale", BigDecimal.ZERO);
        params.put("montantFinal", new BigDecimal("120.00"));
        params.put("montantPaye", BigDecimal.ZERO);
        params.put("montantRestant", new BigDecimal("120.00"));
        params.put("montantEnLettres", "CENT VINGT DIRHAMS");
        ImpressionService.ajouterQrCode(params, ImpressionService.QR_PREFIXE_FACTURE, numero);

        verifierRapport("facture_client", params, "TADBEER:FACTURE:" + numero);
    }

    @Test
    void rapportsPrecompilesDeclarentLeParametreQrCode() throws Exception {
        // ImpressionService charge d'abord le .jasper du classpath : il doit connaître "qrCode", sinon le QR est ignoré sans erreur
        for (String nom : List.of("bon_livraison_client", "facture_client")) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("reports/" + nom + ".jasper")) {
                assertNotNull(is, nom + ".jasper absent du classpath");
                JasperReport report = (JasperReport) JRLoader.loadObject(is);
                Optional<JRParameter> param = Arrays.stream(report.getParameters())
                        .filter(p -> "qrCode".equals(p.getName())).findFirst();
                assertTrue(param.isPresent(), nom + ".jasper périmé : paramètre qrCode absent");
                assertEquals(java.awt.Image.class, param.get().getValueClass());
            }
        }
    }

    /** Compile le .jrxml, remplit avec et sans QR, vérifie le PDF et relit le QR sur le rendu de la page 1. */
    private void verifierRapport(String nom, Map<String, Object> params, String contenuAttendu) throws Exception {
        JasperReport report;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("reports/" + nom + ".jrxml")) {
            assertNotNull(is, nom + ".jrxml introuvable");
            report = JasperCompileManager.compileReport(is);
        }

        // Avec QR
        JasperPrint print = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(lignes()));
        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertTrue(pdf.length > 1000, "PDF vide ou trop petit");
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));

        JRPrintImage qr = trouverQr(print).orElseThrow(() -> new AssertionError("Élément QR 64x64 absent de la page 1 de " + nom));
        // Zone libre du bandeau : entre le logo (x <= 210) et le bloc société (x >= 300), dans les 100 premiers points
        assertTrue(qr.getX() >= 200 && qr.getX() + qr.getWidth() <= 300, "QR hors de la zone libre : x=" + qr.getX());
        assertTrue(qr.getY() >= 0 && qr.getY() + qr.getHeight() <= 100, "QR hors du bandeau : y=" + qr.getY());
        System.out.println(nom + " : QR à x=" + qr.getX() + " y=" + qr.getY() + " " + qr.getWidth() + "x" + qr.getHeight() + " pt");

        // Image embarquée en PNG (sans perte)
        assertInstanceOf(DataRenderable.class, qr.getRenderer());
        byte[] donnees = ((DataRenderable) qr.getRenderer()).getData(DefaultJasperReportsContext.getInstance());
        assertArrayEquals(new byte[] {(byte) 0x89, 'P', 'N', 'G'}, Arrays.copyOf(donnees, 4), "Image QR non encodée en PNG");

        // Relecture du QR tel qu'imprimé (rendu de la page 1, recadré sur l'élément)
        BufferedImage page = (BufferedImage) JasperPrintManager.printPageToImage(print, 0, ZOOM_RENDU);
        int marge = 6;
        int x = Math.max(0, Math.round((qr.getX() - marge) * ZOOM_RENDU));
        int y = Math.max(0, Math.round((qr.getY() - marge) * ZOOM_RENDU));
        int cote = Math.round((qr.getWidth() + 2 * marge) * ZOOM_RENDU);
        BufferedImage recadre = page.getSubimage(x, y, Math.min(cote, page.getWidth() - x), Math.min(cote, page.getHeight() - y));
        assertEquals(contenuAttendu, decoder(recadre));

        // Sans QR : le rapport se remplit et l'élément n'est pas imprimé
        Map<String, Object> sansQr = new HashMap<>(params);
        sansQr.remove("qrCode");
        JasperPrint printSansQr = JasperFillManager.fillReport(report, sansQr, new JRBeanCollectionDataSource(lignes()));
        assertTrue(JasperExportManager.exportReportToPdf(printSansQr).length > 1000);
        assertTrue(trouverQr(printSansQr).isEmpty(), "Aucun QR attendu quand le paramètre est absent");
    }

    private static Optional<JRPrintImage> trouverQr(JasperPrint print) {
        assertFalse(print.getPages().isEmpty(), "Aucune page générée");
        for (JRPrintElement el : print.getPages().get(0).getElements()) {
            if (el instanceof JRPrintImage img && img.getWidth() == 64 && img.getHeight() == 64) {
                return Optional.of(img);
            }
        }
        return Optional.empty();
    }

    // ---------- Décodage zxing ----------

    /** Adaptateur BufferedImage -> LuminanceSource (BufferedImageLuminanceSource n'est que dans zxing:javase). */
    private static LuminanceSource sourceLuminance(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        return new RGBLuminanceSource(w, h, pixels);
    }

    private static String decoder(BufferedImage image) throws NotFoundException {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        return new MultiFormatReader()
                .decode(new BinaryBitmap(new HybridBinarizer(sourceLuminance(image))), hints)
                .getText();
    }

    // ---------- Données minimales ----------

    private static Map<String, Object> paramsCommuns() {
        Map<String, Object> params = new HashMap<>();
        params.put("nomEntreprise", "SOCIETE TEST");
        params.put("activiteEntreprise", "Négoce");
        params.put("adresseEntreprise", "1, Rue de Test - Casablanca");
        params.put("telephoneEntreprise", "05 22 00 00 00");
        params.put("iceEntreprise", "001234567890001");
        params.put("devise", "MAD");
        params.put("clientNom", "CLIENT TEST");
        params.put("clientAdresse", "Zone Industrielle, Casablanca");
        params.put("clientTelephone", "06 00 00 00 00");
        params.put("clientIce", "00152677600001");
        params.put("logoImage", null);
        return params;
    }

    private static List<Map<String, Object>> lignes() {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> l = new HashMap<>();
            BigDecimal qte = new BigDecimal(i);
            BigDecimal pu = new BigDecimal("10.00");
            BigDecimal ht = pu.multiply(qte);
            l.put("reference", "REF-" + i);
            l.put("produitReference", "REF-" + i);
            l.put("designation", "Article de test " + i);
            l.put("produitDesignation", "Article de test " + i);
            l.put("depotNom", "Dépôt Principal");
            l.put("quantite", qte);
            l.put("quantiteCommandee", qte);
            l.put("quantiteLivree", qte);
            l.put("prixVente", pu);
            l.put("prixUnitaire", pu);
            l.put("prixUnitaireHT", pu);
            l.put("tauxTVA", new BigDecimal("20.00"));
            l.put("montantLigne", ht);
            l.put("montantHT", ht);
            l.put("montantTotal", ht);
            l.put("montantTTC", ht.multiply(new BigDecimal("1.20")));
            lignes.add(l);
        }
        return lignes;
    }
}
