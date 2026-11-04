package com.gestion.service.compta;

import com.gestion.persistent.dto.compta.TvaDtos.TvaDeclarationDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEncaissementDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaLigneDTO;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Les fichiers de la TVA : le relevé des déductions au format du cahier EDI, et les exports de travail.
 * L'application ne se connecte pas à SIMPL-TVA et ne se dit jamais officielle : ces documents ne portent aucun titre de ce genre.
 */
@Component
public class TvaFichiers {

    private static final String CRLF = "\r\n";

    /** @param contenu le XML, avec son BOM */
    public record Releve(String nomXml, byte[] contenu) {
    }

    /** BOM UTF-8, pas de prologue, pas de namespace, tabulations et CRLF comme l'exemple du cahier. Le prorata s'omet à 100 %. Un StAX échapperait les CR. */
    public Releve releve(String identifiantFiscal, int annee, int periode, int regime, int version, List<TvaDeductionDTO> operations) {
        StringBuilder x = new StringBuilder("\uFEFF<DeclarationReleveDeduction>");
        feuille(x, 1, "identifiantFiscal", identifiantFiscal == null ? "" : identifiantFiscal);
        feuille(x, 1, "annee", String.valueOf(annee));
        feuille(x, 1, "periode", String.valueOf(periode));
        feuille(x, 1, "regime", String.valueOf(regime));
        ouvrir(x, 1, "releveDeductions");
        for (TvaDeductionDTO o : operations) {
            ouvrir(x, 2, "rd");
            feuille(x, 3, "ord", String.valueOf(o.ordre()));
            feuille(x, 3, "num", o.numeroFacture());
            feuille(x, 3, "des", o.designation());
            feuille(x, 3, "mht", montant(o.ht()));
            feuille(x, 3, "tva", montant(o.tva()));
            feuille(x, 3, "ttc", montant(o.ttc()));
            ouvrir(x, 3, "refF");
            feuille(x, 4, "if", o.fournisseurIf() == null ? "" : o.fournisseurIf());
            feuille(x, 4, "nom", o.fournisseurNom());
            feuille(x, 4, "ice", o.fournisseurIce() == null ? "" : o.fournisseurIce());
            fermer(x, 3, "refF");
            feuille(x, 3, "tx", montant(o.taux()));
            if (o.prorata().compareTo(new BigDecimal("100")) != 0) {
                feuille(x, 3, "prorata", o.prorata().stripTrailingZeros().toPlainString());
            }
            ouvrir(x, 3, "mp");
            feuille(x, 4, "id", String.valueOf(o.mpId()));
            fermer(x, 3, "mp");
            feuille(x, 3, "dpai", o.datePaiement().toString());
            feuille(x, 3, "dfac", o.dateFacture().toString());
            fermer(x, 2, "rd");
        }
        fermer(x, 1, "releveDeductions");
        fermer(x, 0, "DeclarationReleveDeduction");
        return new Releve(String.format("RD_%s_%d_%02d_v%d.xml", identifiantFiscal == null ? "SANSIF" : identifiantFiscal, annee, periode, version), x.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void ouvrir(StringBuilder x, int niveau, String nom) {
        x.append(CRLF).append("\t".repeat(niveau)).append('<').append(nom).append('>');
    }

    private static void fermer(StringBuilder x, int niveau, String nom) {
        x.append(CRLF).append("\t".repeat(niveau)).append("</").append(nom).append('>');
    }

    private static void feuille(StringBuilder x, int niveau, String nom, String valeur) {
        ouvrir(x, niveau, nom);
        x.append(echapper(valeur)).append("</").append(nom).append('>');
    }

    /** Les caractères de contrôle n'existent pas en XML 1.0 : un nom mal saisi ne doit pas casser tout le fichier. */
    private static String echapper(String valeur) {
        StringBuilder sortie = new StringBuilder(valeur.length());
        valeur.codePoints().forEach(c -> {
            switch (c) {
                case '&' -> sortie.append("&amp;");
                case '<' -> sortie.append("&lt;");
                case '>' -> sortie.append("&gt;");
                default -> {
                    if (c >= 0x20 && c != 0xFFFE && c != 0xFFFF) {
                        sortie.appendCodePoint(c);
                    } else if (c == '\t' || c == '\n' || c == '\r') {
                        sortie.append(' ');
                    }
                }
            }
        });
        return sortie.toString();
    }

    private static String montant(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Contre notre schéma interne, non officiel : il détecte nos régressions, il ne dit pas que la DGI acceptera. */
    public void verifier(byte[] xml) {
        try {
            var fabrique = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrique.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrique.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var schema = fabrique.newSchema(new StreamSource(new ClassPathResource("compta/tva_releve_interne.xsd.xml").getInputStream()));
            schema.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (SAXException e) {
            throw new IllegalStateException("Le relevé produit ne respecte pas le format attendu : " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] zip(Releve releve) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(sortie, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(releve.nomXml()));
            zip.write(releve.contenu());
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    public String empreinte(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- Exports de travail

    /** La fiche de saisie : les lignes du formulaire dans son ordre, pour recopier dans le portail. */
    public byte[] fichePdf(TvaDeclarationDTO d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font police = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
        Font fort = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            document.add(new Paragraph("Fiche de saisie TVA : " + periode(d), titre));
            document.add(new Paragraph(d.societe() + "  IF " + texte(d.identifiantFiscal()) + "  ICE " + texte(d.ice()) + "  Fait générateur " + d.faitGenerateur().toLowerCase() + "  Formulaire " + d.modeleFormulaire() + "  Version " + d.version() + "  État " + d.statut(), police));
            document.add(new Paragraph("Document de travail, à recopier dans le portail de la DGI. Ce n'est pas la déclaration.", police));
            document.add(new Paragraph(" ", police));
            PdfPTable table = new PdfPTable(new float[]{0.7f, 5.3f, 0.8f, 1.6f, 1.6f});
            table.setWidthPercentage(100);
            for (String t : List.of("Ligne", "Libellé", "Taux", "Base ou TVA facturée", "Taxe")) {
                table.addCell(cellule(t, fort, Element.ALIGN_LEFT));
            }
            for (TvaLigneDTO l : d.lignes()) {
                table.addCell(cellule(l.code(), police, Element.ALIGN_LEFT));
                table.addCell(cellule(l.libelle() + (l.motifEcart() == null ? "" : " (corrigée : " + l.motifEcart() + ")"), police, Element.ALIGN_LEFT));
                table.addCell(cellule(l.taux() == null ? "" : l.taux() + " %", police, Element.ALIGN_RIGHT));
                table.addCell(cellule(l.baseRetenue() == null ? "" : MontantTexte.fr(l.baseRetenue()), police, Element.ALIGN_RIGHT));
                table.addCell(cellule(l.taxeRetenue() == null ? "" : MontantTexte.fr(l.taxeRetenue()), police, Element.ALIGN_RIGHT));
            }
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("La fiche n'a pas pu s'écrire : " + e.getMessage(), e);
        }
        return sortie.toByteArray();
    }

    private static PdfPCell cellule(String texte, Font police, int alignement) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, police));
        cellule.setHorizontalAlignment(alignement);
        cellule.setBorder(Rectangle.BOTTOM);
        cellule.setBorderWidth(0.25f);
        cellule.setPadding(2.2f);
        return cellule;
    }

    private static String texte(String v) {
        return v == null ? "absent" : v;
    }

    private static String periode(TvaDeclarationDTO d) {
        return ("TRIMESTRIELLE".equals(d.periodicite()) ? "trimestre " : "mois ") + d.numeroPeriode() + " de " + d.annee();
    }

    public byte[] releveXlsx(TvaDeclarationDTO d, List<TvaDeductionDTO> operations) {
        return classeur(c -> {
            Sheet f = c.createSheet("Relevé des déductions");
            ligne(f, 0, "Ordre", "N° facture", "Date facture", "Fournisseur", "IF", "ICE", "Désignation", "Rubrique", "Ligne", "HT", "Taux", "TVA", "TTC", "Prorata", "TVA déductible", "Mode", "Référence de paiement", "Date de paiement");
            int n = 1;
            for (TvaDeductionDTO o : operations) {
                ligne(f, n++, o.ordre(), o.numeroFacture(), o.dateFacture(), o.fournisseurNom(), o.fournisseurIf(), o.fournisseurIce(), o.designation(), o.rubriqueCode(), o.ligneCode(), o.ht(), o.taux(), o.tva(), o.ttc(), o.prorata(), o.tvaDeductible(), o.mpId(), o.referencePaiement(), o.datePaiement());
            }
        });
    }

    public byte[] couvertureXlsx(TvaDeclarationDTO d) {
        return classeur(c -> {
            Sheet f = c.createSheet("Couverture");
            ligne(f, 0, "Ligne", "Section", "Libellé", "Taux", "Base calculée", "Taxe calculée", "Base retenue", "Taxe retenue", "Motif");
            int n = 1;
            for (TvaLigneDTO l : d.lignes()) {
                ligne(f, n++, l.code(), l.section(), l.libelle(), l.taux(), l.baseCalculee(), l.taxeCalculee(), l.baseRetenue(), l.taxeRetenue(), l.motifEcart());
            }
        });
    }

    public byte[] detailXlsx(TvaDeclarationDTO d, List<TvaEncaissementDTO> encaissements, List<TvaDeductionDTO> deductions) {
        return classeur(c -> {
            Sheet e = c.createSheet("Encaissements");
            ligne(e, 0, "Date", "Origine", "Référence", "Facture", "Tiers", "Mode", "Taux", "TTC", "HT", "TVA", "Traitement", "Ligne");
            int n = 1;
            for (TvaEncaissementDTO o : encaissements) {
                ligne(e, n++, o.date(), o.sourceType(), o.reference(), o.factureNumero(), o.tiersNom(), o.mode(), o.taux(), o.ttc(), o.ht(), o.tva(), o.traitement(), o.ligneCode());
            }
            Sheet r = c.createSheet("Récupération");
            ligne(r, 0, "Date de paiement", "Rubrique", "Ligne", "Facture", "Fournisseur", "HT", "Taux", "TVA", "Prorata", "TVA déductible", "Compte");
            n = 1;
            for (TvaDeductionDTO o : deductions) {
                ligne(r, n++, o.datePaiement(), o.rubriqueCode(), o.ligneCode(), o.numeroFacture(), o.fournisseurNom(), o.ht(), o.taux(), o.tva(), o.prorata(), o.tvaDeductible(), o.compteTva());
            }
        });
    }

    interface Remplissage {
        void remplir(XSSFWorkbook classeur);
    }

    static byte[] classeur(Remplissage remplissage) {
        try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            remplissage.remplir(classeur);
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void ligne(Sheet feuille, int numero, Object... valeurs) {
        Row r = feuille.createRow(numero);
        for (int i = 0; i < valeurs.length; i++) {
            Object v = valeurs[i];
            if (v instanceof BigDecimal b) {
                r.createCell(i).setCellValue(b.doubleValue());
            } else if (v instanceof Number x) {
                r.createCell(i).setCellValue(x.doubleValue());
            } else if (v != null) {
                r.createCell(i).setCellValue(v.toString());
            }
        }
    }
}
