package com.gestion.service.compta;

import com.gestion.persistent.dto.compta.EtatDtos.ColonneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.LigneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.MentionsEtatDTO;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/** Met en page un état déjà calculé. Ce qui est ici ne calcule rien : le PDF et l'Excel montrent les valeurs de l'écran. */
@Component
public class EtatExports {

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> LIGNES_FORTES = Set.of("total", "resultat", "rubrique", "rubrique_poste", "solde");

    public byte[] pdf(EtatDTO etat) {
        return pdf(List.of(etat));
    }

    public byte[] pdf(List<EtatDTO> etats) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            boolean premier = true;
            for (EtatDTO etat : etats) {
                if (!premier) {
                    document.newPage();
                }
                premier = false;
                pdfEtat(document, etat);
            }
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF non produit : " + etats.get(0).code(), e);
        }
        return sortie.toByteArray();
    }

    private static void pdfEtat(Document document, EtatDTO etat) throws DocumentException {
        Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FontFactory.defaultEncoding, 12);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, FontFactory.defaultEncoding, 8);
        Font fort = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FontFactory.defaultEncoding, 8);
        document.add(new Paragraph(etat.mentions().typeEtat(), titre));
        document.add(new Paragraph(mentions(etat.mentions()), normal));
        if (etat.ouvertureProvisoire() || etat.nbBrouillons() > 0) {
            document.add(new Paragraph(provisoire(etat), fort));
        }
        document.add(new Paragraph(" ", normal));

        float[] largeurs = new float[etat.colonnes().size() + 1];
        largeurs[0] = 44;
        for (int i = 1; i < largeurs.length; i++) {
            largeurs[i] = 56f / (largeurs.length - 1);
        }
        PdfPTable table = new PdfPTable(largeurs);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.addCell(cellule("", fort, Element.ALIGN_LEFT, true));
        for (ColonneEtatDTO colonne : etat.colonnes()) {
            table.addCell(cellule(colonne.libelle(), fort, Element.ALIGN_RIGHT, true));
        }
        for (LigneEtatDTO ligne : etat.lignes()) {
            Font police = LIGNES_FORTES.contains(ligne.type()) ? fort : normal;
            boolean grise = "total".equals(ligne.type()) || "resultat".equals(ligne.type());
            table.addCell(cellule(ligne.libelle(), police, Element.ALIGN_LEFT, grise));
            for (ColonneEtatDTO colonne : etat.colonnes()) {
                table.addCell(cellule(texteMontant(ligne.valeurs().get(colonne.code())), police, Element.ALIGN_RIGHT, grise));
            }
        }
        document.add(table);
    }

    private static PdfPCell cellule(String texte, Font police, int alignement, boolean grise) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, police));
        cellule.setHorizontalAlignment(alignement);
        cellule.setBorder(Rectangle.BOTTOM);
        cellule.setBorderWidth(0.25f);
        cellule.setPadding(2.2f);
        if (grise) {
            cellule.setGrayFill(0.93f);
        }
        return cellule;
    }

    public byte[] xlsx(EtatDTO etat) {
        return xlsx(List.of(etat));
    }

    public byte[] xlsx(List<EtatDTO> etats) {
        try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            for (EtatDTO etat : etats) {
                feuille(classeur, etat);
            }
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void feuille(XSSFWorkbook classeur, EtatDTO etat) {
        {
            Sheet feuille = classeur.createSheet(etat.code());
            XSSFFont gras = classeur.createFont();
            gras.setBold(true);
            CellStyle styleFort = classeur.createCellStyle();
            styleFort.setFont(gras);
            CellStyle styleEntete = classeur.createCellStyle();
            styleEntete.setFont(gras);
            styleEntete.setBorderBottom(BorderStyle.THIN);
            styleEntete.setAlignment(HorizontalAlignment.RIGHT);
            styleEntete.setWrapText(true);
            short format = classeur.createDataFormat().getFormat("#,##0.00");
            CellStyle styleMontant = classeur.createCellStyle();
            styleMontant.setDataFormat(format);
            CellStyle styleMontantFort = classeur.createCellStyle();
            styleMontantFort.setDataFormat(format);
            styleMontantFort.setFont(gras);

            int rang = 0;
            feuille.createRow(rang++).createCell(0).setCellValue(etat.mentions().typeEtat());
            feuille.getRow(0).getCell(0).setCellStyle(styleFort);
            feuille.createRow(rang++).createCell(0).setCellValue(mentions(etat.mentions()));
            if (etat.ouvertureProvisoire() || etat.nbBrouillons() > 0) {
                feuille.createRow(rang++).createCell(0).setCellValue(provisoire(etat));
            }
            rang++;
            Row entete = feuille.createRow(rang++);
            List<ColonneEtatDTO> colonnes = etat.colonnes();
            for (int i = 0; i < colonnes.size(); i++) {
                Cell c = entete.createCell(i + 1);
                c.setCellValue(colonnes.get(i).libelle());
                c.setCellStyle(styleEntete);
            }
            for (LigneEtatDTO ligne : etat.lignes()) {
                boolean forte = LIGNES_FORTES.contains(ligne.type());
                Row r = feuille.createRow(rang++);
                Cell libelle = r.createCell(0);
                libelle.setCellValue(ligne.libelle());
                if (forte) {
                    libelle.setCellStyle(styleFort);
                }
                for (int i = 0; i < colonnes.size(); i++) {
                    String valeur = ligne.valeurs().get(colonnes.get(i).code());
                    if (valeur != null) {
                        Cell c = r.createCell(i + 1);
                        c.setCellValue(new BigDecimal(valeur).doubleValue());
                        c.setCellStyle(forte ? styleMontantFort : styleMontant);
                    }
                }
            }
            feuille.setColumnWidth(0, 60 * 256);
            for (int i = 0; i < colonnes.size(); i++) {
                feuille.setColumnWidth(i + 1, 18 * 256);
            }
        }
    }

    private static String mentions(MentionsEtatDTO m) {
        return m.entite() + " - Exercice " + m.exercice() + " - " + m.periode() + " - Produit le " + HORODATAGE.format(m.produitLe()) + " - " + m.logiciel();
    }

    private static String provisoire(EtatDTO etat) {
        StringBuilder texte = new StringBuilder("Situation provisoire");
        if (etat.ouvertureProvisoire()) {
            texte.append(" : ouverture calculée");
        }
        if (etat.nbBrouillons() > 0) {
            texte.append(etat.ouvertureProvisoire() ? ", " : " : ").append(etat.nbBrouillons()).append(" brouillard(s) hors état");
        }
        return texte.toString();
    }

    /** Une colonne sans valeur reste vide ; zéro s'écrit comme à l'écran, par un tiret. */
    private static String texteMontant(String valeur) {
        if (valeur == null) {
            return "";
        }
        BigDecimal montant = new BigDecimal(valeur);
        return montant.signum() == 0 ? "-" : MontantTexte.fr(montant);
    }
}
