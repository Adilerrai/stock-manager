package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.EtiquetteCodeBarreRequest;
import com.gestion.persistent.dto.ProduitCodeBarreDTO;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.ProduitRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BarcodeService {

    private static final Logger log = LoggerFactory.getLogger(BarcodeService.class);

    private final ProduitRepository produitRepository;

    public BarcodeService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    /**
     * Détermine le code effectif pour le code-barre (codeBarre si renseigné, sinon reference).
     */
    public String resolveCode(Produit produit) {
        if (produit == null) return "";
        if (produit.getCodeBarre() != null && !produit.getCodeBarre().trim().isEmpty()) {
            return produit.getCodeBarre().trim();
        }
        if (produit.getReference() != null && !produit.getReference().trim().isEmpty()) {
            return produit.getReference().trim();
        }
        return "PROD-" + produit.getId();
    }

    /**
     * Génère une image PNG du code-barre Code 128 avec le texte en dessous.
     */
    public byte[] generateBarcodePng(String code, int width, int height) {
        try {
            String safeCode = (code != null && !code.trim().isEmpty()) ? code.trim() : "000000";

            Barcode128 barcode = new Barcode128();
            barcode.setCodeType(Barcode128.CODE128);
            barcode.setCode(safeCode);

            java.awt.Image awtBars = barcode.createAwtImage(Color.BLACK, Color.WHITE);
            int barWidth = awtBars.getWidth(null);
            int barHeight = awtBars.getHeight(null);

            if (barWidth <= 0) barWidth = 200;
            if (barHeight <= 0) barHeight = 50;

            int finalWidth = Math.max(width, barWidth + 30);
            int textHeight = 20;
            int finalHeight = Math.max(height, barHeight + textHeight + 16);

            BufferedImage bufferedImage = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bufferedImage.createGraphics();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, finalWidth, finalHeight);

            int xBar = (finalWidth - barWidth) / 2;
            int yBar = 8;
            g2.drawImage(awtBars, xBar, yBar, null);

            g2.setColor(Color.BLACK);
            java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            int textWidth = fm.stringWidth(safeCode);
            int xText = (finalWidth - textWidth) / 2;
            int yText = yBar + barHeight + fm.getAscent() + 2;
            g2.drawString(safeCode, xText, yText);

            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération code-barre PNG pour '{}': {}", code, e.getMessage(), e);
            throw new RuntimeException("Impossible de générer le code-barre: " + e.getMessage(), e);
        }
    }

    /**
     * Génère l'image PNG en Data URL Base64 pour affichage HTML ou JSON.
     */
    public String generateBarcodeBase64(String code) {
        byte[] png = generateBarcodePng(code, 220, 70);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    /**
     * Récupère la liste des produits avec leurs codes-barres en Base64.
     */
    public List<ProduitCodeBarreDTO> getProduitsCodeBarres(List<Long> produitIds, Long categorieId) {
        List<Produit> produits = findTargetProduits(produitIds, categorieId);
        return produits.stream().map(p -> {
            String code = resolveCode(p);
            String b64 = generateBarcodeBase64(code);
            String catNom = p.getCategorie() != null ? p.getCategorie().getNom() : p.getCategorieArticle();
            BigDecimal prix = p.getPrixVenteTtc() != null ? p.getPrixVenteTtc() : p.getPrixVente();
            return new ProduitCodeBarreDTO(
                    p.getId(),
                    p.getReference(),
                    p.getDesignation() != null ? p.getDesignation() : p.getDescription(),
                    code,
                    b64,
                    prix,
                    catNom,
                    p.getActif()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Recherche les produits cibles pour le tenant courant.
     */
    public List<Produit> findTargetProduits(List<Long> produitIds, Long categorieId) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenant = (tenantId != null) ? tenantId : 1L;

        List<Produit> produits;
        if (produitIds != null && !produitIds.isEmpty()) {
            produits = produitRepository.findAllById(produitIds).stream()
                    .filter(p -> p.getPointDeVenteId() == null || p.getPointDeVenteId().equals(effectiveTenant))
                    .collect(Collectors.toList());
        } else if (categorieId != null) {
            produits = produitRepository.findByCategorieId(categorieId).stream()
                    .filter(p -> p.getPointDeVenteId() == null || p.getPointDeVenteId().equals(effectiveTenant))
                    .collect(Collectors.toList());
        } else {
            produits = produitRepository.findByPointDeVenteId(effectiveTenant);
            if (produits.isEmpty()) {
                produits = produitRepository.findAll();
            }
        }

        produits.sort((a, b) -> {
            String ra = a.getReference() != null ? a.getReference() : "";
            String rb = b.getReference() != null ? b.getReference() : "";
            return ra.compareToIgnoreCase(rb);
        });

        return produits;
    }

    /**
     * Génère le document PDF d'étiquettes de codes-barres (A4).
     */
    public byte[] genererEtiquettesPdf(EtiquetteCodeBarreRequest request) {
        List<Produit> produits = findTargetProduits(request.getProduitIds(), request.getCategorieId());
        if (produits.isEmpty()) {
            throw new RuntimeException("Aucun produit trouvé pour la génération des étiquettes.");
        }

        int cols = request.getColonnes();
        int copies = request.getExemplaires();
        boolean afficherPrix = request.getAfficherPrix();
        String modele = request.getModele();

        List<Produit> listeImpression = new ArrayList<>();
        for (Produit p : produits) {
            for (int i = 0; i < copies; i++) {
                listeImpression.add(p);
            }
        }

        Document document = new Document(PageSize.A4, 16, 16, 18, 18);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            if ("sticker".equalsIgnoreCase(modele)) {
                buildModeleSticker(document, writer, listeImpression, cols, afficherPrix);
            } else {
                // Par défaut : MODÈLE 02 - GRILLE INVENTAIRE (conforme à l'image demandée)
                buildModeleGrille(document, writer, listeImpression, cols, afficherPrix);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération PDF étiquettes: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de génération des étiquettes code-barre: " + e.getMessage(), e);
        }
    }

    /**
     * MODÈLE 02 : Étiquettes Grille Inventaire avec bandeau bleu pastel (conforme aux images fournies).
     */
    private void buildModeleGrille(Document document, PdfWriter writer, List<Produit> produits, int cols, boolean afficherPrix) throws Exception {
        PdfContentByte cb = writer.getDirectContent();

        // 1. Bandeau En-tête bleu nuit (INVENTAIRE | ETIQUETTES PRODUITS)
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(10.0f);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(new Color(0x0A, 0x28, 0x47));
        headerCell.setPadding(8.0f);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Font fontHeaderTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, Color.WHITE);
        Font fontHeaderSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(0xCF, 0xD8, 0xDC));

        Paragraph title = new Paragraph("INVENTAIRE | ETIQUETTES PRODUITS", fontHeaderTitle);
        Paragraph sub = new Paragraph("MODELE 02 • CODE 128 • IMPRESSION A4 A 100 %", fontHeaderSub);
        sub.setSpacingBefore(2.0f);

        headerCell.addElement(title);
        headerCell.addElement(sub);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        // 2. Grille de cartes
        float[] relativeWidths = (cols == 3)
                ? new float[]{31.5f, 2.7f, 31.5f, 2.7f, 31.6f}
                : new float[]{48.6f, 2.8f, 48.6f};

        PdfPTable table = new PdfPTable(relativeWidths);
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        Font fontBadgeCode = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.0f, new Color(0x0A, 0x3B, 0x75));
        Font fontDesignation = FontFactory.getFont(FontFactory.HELVETICA_BOLD, cols == 3 ? 7.5f : 8.5f, new Color(0x1F, 0x29, 0x37));
        Font fontBottomCode = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(0x37, 0x41, 0x51));
        Font fontPrix = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, new Color(0x0A, 0x3B, 0x75));

        int cellInRow = 0;

        for (Produit p : produits) {
            String code = resolveCode(p);
            String designation = p.getDesignation() != null && !p.getDesignation().trim().isEmpty()
                    ? p.getDesignation().trim()
                    : (p.getDescription() != null ? p.getDescription().trim() : p.getReference());

            // Carte interne (tableau à 2 lignes : bandeau bleu clair + corps)
            PdfPTable cardInnerTable = new PdfPTable(1);
            cardInnerTable.setWidthPercentage(100);

            // A. Bandeau supérieur bleu pastel avec le code
            PdfPCell badgeCell = new PdfPCell();
            badgeCell.setBackgroundColor(new Color(0xEB, 0xF3, 0xFA));
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setPaddingLeft(8.0f);
            badgeCell.setPaddingTop(5.0f);
            badgeCell.setPaddingBottom(5.0f);
            badgeCell.setFixedHeight(22.0f);

            Paragraph pBadge = new Paragraph(code, fontBadgeCode);
            pBadge.setAlignment(Element.ALIGN_LEFT);
            badgeCell.addElement(pBadge);
            cardInnerTable.addCell(badgeCell);

            // B. Corps de la carte
            PdfPCell bodyCell = new PdfPCell();
            bodyCell.setBorder(Rectangle.NO_BORDER);
            bodyCell.setPaddingLeft(8.0f);
            bodyCell.setPaddingRight(8.0f);
            bodyCell.setPaddingTop(8.0f);
            bodyCell.setPaddingBottom(8.0f);
            bodyCell.setBackgroundColor(Color.WHITE);

            // Désignation
            Paragraph pDesig = new Paragraph(designation.toUpperCase(), fontDesignation);
            pDesig.setLeading(10.5f);
            pDesig.setSpacingAfter(8.0f);
            bodyCell.addElement(pDesig);

            // Code-barre
            Barcode128 barcode = new Barcode128();
            barcode.setCodeType(Barcode128.CODE128);
            barcode.setCode(code);
            barcode.setFont(null);
            barcode.setBarHeight(32.0f);
            barcode.setX(cols == 3 ? 0.95f : 1.10f);

            Image barcodeImg = barcode.createImageWithBarcode(cb, Color.BLACK, Color.BLACK);
            barcodeImg.setAlignment(Element.ALIGN_CENTER);
            bodyCell.addElement(barcodeImg);

            // Code sous le code-barre
            Paragraph pCode = new Paragraph(code, fontBottomCode);
            pCode.setAlignment(Element.ALIGN_CENTER);
            pCode.setSpacingBefore(4.0f);
            bodyCell.addElement(pCode);

            // Prix optionnel
            if (afficherPrix) {
                BigDecimal prix = p.getPrixVenteTtc() != null ? p.getPrixVenteTtc() : p.getPrixVente();
                if (prix != null && prix.compareTo(BigDecimal.ZERO) > 0) {
                    Paragraph pPrix = new Paragraph(String.format(Locale.FRANCE, "%,.2f MAD", prix), fontPrix);
                    pPrix.setAlignment(Element.ALIGN_CENTER);
                    pPrix.setSpacingBefore(3.0f);
                    bodyCell.addElement(pPrix);
                }
            }

            cardInnerTable.addCell(bodyCell);

            // Envelopper cardInnerTable dans une cellule avec bordure
            PdfPCell wrapperCell = new PdfPCell();
            wrapperCell.setBorder(Rectangle.BOX);
            wrapperCell.setBorderWidth(0.75f);
            wrapperCell.setBorderColor(new Color(210, 218, 226));
            wrapperCell.setPadding(0);
            wrapperCell.addElement(cardInnerTable);

            table.addCell(wrapperCell);
            cellInRow++;

            if (cellInRow < cols) {
                PdfPCell spacer = new PdfPCell();
                spacer.setBorder(Rectangle.NO_BORDER);
                table.addCell(spacer);
            } else {
                cellInRow = 0;
                for (int i = 0; i < relativeWidths.length; i++) {
                    PdfPCell vSpacer = new PdfPCell();
                    vSpacer.setBorder(Rectangle.NO_BORDER);
                    vSpacer.setFixedHeight(10.0f);
                    table.addCell(vSpacer);
                }
            }
        }

        if (cellInRow > 0) {
            while (cellInRow < cols) {
                PdfPCell spacer = new PdfPCell();
                spacer.setBorder(Rectangle.NO_BORDER);
                table.addCell(spacer);

                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                table.addCell(empty);

                cellInRow++;
            }
        }

        document.add(table);
    }

    /**
     * MODÈLE 01 : Étiquette sticker épurée et centrée.
     */
    private void buildModeleSticker(Document document, PdfWriter writer, List<Produit> produits, int cols, boolean afficherPrix) throws Exception {
        PdfContentByte cb = writer.getDirectContent();

        float[] relativeWidths = (cols == 3)
                ? new float[]{31.5f, 2.7f, 31.5f, 2.7f, 31.6f}
                : new float[]{48.6f, 2.8f, 48.6f};

        PdfPTable table = new PdfPTable(relativeWidths);
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        Font fontDesignation = FontFactory.getFont(FontFactory.HELVETICA_BOLD, cols == 3 ? 8.0f : 9.0f, Color.BLACK);
        Font fontCode = FontFactory.getFont(FontFactory.HELVETICA_BOLD, cols == 3 ? 8.5f : 9.5f, Color.BLACK);
        Font fontPrix = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, new Color(0x0A, 0x3B, 0x75));

        int cellInRow = 0;

        for (Produit p : produits) {
            String code = resolveCode(p);
            String designation = p.getDesignation() != null && !p.getDesignation().trim().isEmpty()
                    ? p.getDesignation().trim()
                    : (p.getDescription() != null ? p.getDescription().trim() : p.getReference());

            PdfPCell cardCell = new PdfPCell();
            cardCell.setBorder(Rectangle.BOX);
            cardCell.setBorderWidth(0.75f);
            cardCell.setBorderColor(new Color(210, 215, 222));
            cardCell.setPadding(8.0f);
            cardCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cardCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cardCell.setBackgroundColor(Color.WHITE);

            Paragraph pDesig = new Paragraph(designation.toUpperCase(), fontDesignation);
            pDesig.setAlignment(Element.ALIGN_CENTER);
            pDesig.setLeading(cols == 3 ? 9.5f : 11.0f);
            pDesig.setSpacingAfter(6.0f);
            cardCell.addElement(pDesig);

            Barcode128 barcode = new Barcode128();
            barcode.setCodeType(Barcode128.CODE128);
            barcode.setCode(code);
            barcode.setFont(null);
            barcode.setBarHeight(cols == 3 ? 28.0f : 34.0f);
            barcode.setX(cols == 3 ? 0.95f : 1.10f);

            Image barcodeImg = barcode.createImageWithBarcode(cb, Color.BLACK, Color.BLACK);
            barcodeImg.setAlignment(Element.ALIGN_CENTER);
            cardCell.addElement(barcodeImg);

            Paragraph pCode = new Paragraph(code, fontCode);
            pCode.setAlignment(Element.ALIGN_CENTER);
            pCode.setSpacingBefore(4.0f);
            cardCell.addElement(pCode);

            if (afficherPrix) {
                BigDecimal prix = p.getPrixVenteTtc() != null ? p.getPrixVenteTtc() : p.getPrixVente();
                if (prix != null && prix.compareTo(BigDecimal.ZERO) > 0) {
                    Paragraph pPrix = new Paragraph(String.format(Locale.FRANCE, "%,.2f MAD", prix), fontPrix);
                    pPrix.setAlignment(Element.ALIGN_CENTER);
                    pPrix.setSpacingBefore(3.0f);
                    cardCell.addElement(pPrix);
                }
            }

            table.addCell(cardCell);
            cellInRow++;

            if (cellInRow < cols) {
                PdfPCell spacer = new PdfPCell();
                spacer.setBorder(Rectangle.NO_BORDER);
                table.addCell(spacer);
            } else {
                cellInRow = 0;
                for (int i = 0; i < relativeWidths.length; i++) {
                    PdfPCell vSpacer = new PdfPCell();
                    vSpacer.setBorder(Rectangle.NO_BORDER);
                    vSpacer.setFixedHeight(10.0f);
                    table.addCell(vSpacer);
                }
            }
        }

        if (cellInRow > 0) {
            while (cellInRow < cols) {
                PdfPCell spacer = new PdfPCell();
                spacer.setBorder(Rectangle.NO_BORDER);
                table.addCell(spacer);

                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                table.addCell(empty);

                cellInRow++;
            }
        }

        document.add(table);
    }
}
