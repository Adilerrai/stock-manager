package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.gestion.persistent.dto.LivraisonRetardDTO;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.*;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.CommandeClientRepository;
import com.gestion.repository.CommandeRepository;
import com.gestion.repository.LivraisonRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LivraisonRetardService {

    private final CommandeRepository commandeRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final LivraisonRepository livraisonRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public LivraisonRetardService(CommandeRepository commandeRepository,
                                  CommandeClientRepository commandeClientRepository,
                                  LivraisonRepository livraisonRepository,
                                  BonLivraisonClientRepository bonLivraisonClientRepository,
                                  PointDeVenteRepository pointDeVenteRepository) {
        this.commandeRepository = commandeRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.livraisonRepository = livraisonRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    private Long resolveTenant(Long pdvId) {
        if (pdvId != null) return pdvId;
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    private String getNomPointDeVente(Long pdvId) {
        if (pdvId == null) return "Principal";
        return pointDeVenteRepository.findById(pdvId)
                .map(p -> p.getNom() != null ? p.getNom() : ("PDV #" + p.getId()))
                .orElse("Point de Vente #" + pdvId);
    }

    // =========================================================================
    // 1. FOURNISSEURS (ACHATS)
    // =========================================================================

    public List<LivraisonRetardDTO> getRetardsFournisseurs(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<Commande> commandes = commandeRepository.findByPointDeVenteId(pdvId);
        LocalDateTime now = LocalDateTime.now();

        List<LivraisonRetardDTO> retards = new ArrayList<>();

        for (Commande c : commandes) {
            if (c.getDateLivraisonPrevue() == null) continue;
            if (c.getStatut() == StatutCommande.ANNULEE || c.getStatut() == StatutCommande.BROUILLON) continue;
            if (c.getStatut() == StatutCommande.LIVREE || c.getStatutLivraison() == StatutLivraison.LIVREE) continue;

            if (c.getDateLivraisonPrevue().isBefore(now)) {
                LivraisonRetardDTO dto = new LivraisonRetardDTO();
                dto.setId(c.getId());
                dto.setType("FOURNISSEUR");
                dto.setNumeroCommande(c.getNumeroCommande() != null ? c.getNumeroCommande() : ("CMD #" + c.getId()));

                // BL liés
                List<Livraison> livs = livraisonRepository.findByCommande_Id(c.getId());
                if (livs != null && !livs.isEmpty()) {
                    dto.setNumeroBl(livs.stream().map(Livraison::getNumeroLivraison).filter(Objects::nonNull).collect(Collectors.joining(", ")));
                } else {
                    dto.setNumeroBl("-");
                }

                if (c.getFournisseur() != null) {
                    dto.setNomTiers(c.getFournisseur().getNom());
                    dto.setTelephoneTiers(c.getFournisseur().getTelephone());
                    dto.setEmailTiers(c.getFournisseur().getEmail());
                } else {
                    dto.setNomTiers("Fournisseur non spécifié");
                }

                dto.setDateCommande(c.getDateCommande());
                dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());

                long jours = ChronoUnit.DAYS.between(c.getDateLivraisonPrevue().toLocalDate(), LocalDate.now());
                if (jours <= 0) jours = 1;
                dto.setJoursRetard(jours);

                boolean estPartielle = c.getStatut() == StatutCommande.PARTIELLE || c.getStatutLivraison() == StatutLivraison.PARTIELLE;
                dto.setStatut(estPartielle ? "Partiellement livrée" : "En attente de réception");
                dto.setStatutCode(estPartielle ? "PARTIELLE" : "EN_ATTENTE");

                dto.setMontantTotal(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);

                // Articles et reliquats
                List<String> articles = new ArrayList<>();
                int totalArt = 0;
                int restantArt = 0;
                if (c.getLignesCommande() != null) {
                    for (LigneCommande lc : c.getLignesCommande()) {
                        totalArt++;
                        int qteCmd = lc.getQuantiteCommandee();
                        int qteLiv = lc.getQuantiteLivree();
                        int reste = qteCmd - qteLiv;
                        if (reste > 0) {
                            restantArt++;
                            String pNom = lc.getProduit() != null ? lc.getProduit().getNom() : "Article";
                            articles.add(pNom + " (" + reste + "/" + qteCmd + " restant)");
                        }
                    }
                }
                dto.setNombreArticlesTotal(totalArt);
                dto.setNombreArticlesRestants(restantArt);
                dto.setArticlesEnAttente(articles.isEmpty() ? "Aucun reliquat" : String.join(", ", articles));
                dto.setPointDeVenteId(c.getPointDeVenteId());

                retards.add(dto);
            }
        }

        retards.sort(Comparator.comparing(LivraisonRetardDTO::getJoursRetard, Comparator.reverseOrder()));
        return retards;
    }

    public byte[] genererExcelRetardsFournisseurs(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<LivraisonRetardDTO> retards = getRetardsFournisseurs(pdvId);
        String nomPdv = getNomPointDeVente(pdvId);

        return buildWorkbook(
                "LIVRAISONS FOURNISSEURS EN RETARD",
                "Fournisseur",
                retards,
                nomPdv,
                new byte[]{(byte) 30, (byte) 58, (byte) 138} // Navy #1E3A8A
        );
    }

    // =========================================================================
    // 2. CLIENTS (VENTES)
    // =========================================================================

    public List<LivraisonRetardDTO> getRetardsClients(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<CommandeClient> commandes = commandeClientRepository.findByPointDeVenteId(pdvId);
        LocalDateTime now = LocalDateTime.now();

        List<LivraisonRetardDTO> retards = new ArrayList<>();

        for (CommandeClient c : commandes) {
            if (c.getDateLivraisonPrevue() == null) continue;
            if (c.getStatut() == StatutCommandeClient.ANNULEE || c.getStatut() == StatutCommandeClient.BROUILLON) continue;
            if (c.getStatut() == StatutCommandeClient.LIVREE || c.getStatut() == StatutCommandeClient.FACTUREE) continue;

            if (c.getDateLivraisonPrevue().isBefore(now)) {
                LivraisonRetardDTO dto = new LivraisonRetardDTO();
                dto.setId(c.getId());
                dto.setType("CLIENT");
                dto.setNumeroCommande(c.getNumeroCommande() != null ? c.getNumeroCommande() : ("CMD #" + c.getId()));

                // BLs liés
                List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByCommandeClientIdAndPointDeVenteId(c.getId(), pdvId);
                if (bls != null && !bls.isEmpty()) {
                    dto.setNumeroBl(bls.stream().map(BonLivraisonClient::getNumeroBl).filter(Objects::nonNull).collect(Collectors.joining(", ")));
                } else {
                    dto.setNumeroBl("-");
                }

                if (c.getClient() != null) {
                    dto.setNomTiers(c.getClient().getNomComplet());
                    dto.setTelephoneTiers(c.getClient().getTelephone());
                    dto.setEmailTiers(c.getClient().getEmail());
                } else if (c.getClientNom() != null) {
                    dto.setNomTiers(c.getClientNom());
                    dto.setTelephoneTiers(c.getClientTelephone());
                    dto.setEmailTiers(c.getClientEmail());
                } else {
                    dto.setNomTiers("Client non spécifié");
                }

                dto.setDateCommande(c.getDateCommande());
                dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());

                long jours = ChronoUnit.DAYS.between(c.getDateLivraisonPrevue().toLocalDate(), LocalDate.now());
                if (jours <= 0) jours = 1;
                dto.setJoursRetard(jours);

                boolean estPartielle = c.getStatut() == StatutCommandeClient.LIVREE_PARTIELLE;
                dto.setStatut(estPartielle ? "Partiellement livrée" : "En attente d'expédition");
                dto.setStatutCode(estPartielle ? "LIVREE_PARTIELLE" : "EN_ATTENTE");

                dto.setMontantTotal(c.getTotalTTC() != null ? c.getTotalTTC() : BigDecimal.ZERO);

                // Articles et reliquats
                List<String> articles = new ArrayList<>();
                int totalArt = 0;
                int restantArt = 0;
                if (c.getLignesCommande() != null) {
                    for (LigneCommandeClient lc : c.getLignesCommande()) {
                        if (Boolean.TRUE.equals(lc.getAnnulee())) continue;
                        totalArt++;
                        BigDecimal qteCmd = lc.getQuantiteCommandee() != null ? lc.getQuantiteCommandee() : BigDecimal.ZERO;
                        BigDecimal rel = lc.getQuantiteReliquat() != null ? lc.getQuantiteReliquat() : lc.calculerReliquat();
                        if (rel != null && rel.compareTo(BigDecimal.ZERO) > 0) {
                            restantArt++;
                            String pNom = lc.getProduit() != null ? lc.getProduit().getNom() : "Article";
                            articles.add(pNom + " (" + rel + "/" + qteCmd + " restant)");
                        }
                    }
                }
                dto.setNombreArticlesTotal(totalArt);
                dto.setNombreArticlesRestants(restantArt);
                dto.setArticlesEnAttente(articles.isEmpty() ? "Aucun reliquat" : String.join(", ", articles));
                dto.setPointDeVenteId(c.getPointDeVenteId());

                retards.add(dto);
            }
        }

        retards.sort(Comparator.comparing(LivraisonRetardDTO::getJoursRetard, Comparator.reverseOrder()));
        return retards;
    }

    public byte[] genererExcelRetardsClients(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<LivraisonRetardDTO> retards = getRetardsClients(pdvId);
        String nomPdv = getNomPointDeVente(pdvId);

        return buildWorkbook(
                "LIVRAISONS CLIENTS EN RETARD",
                "Client",
                retards,
                nomPdv,
                new byte[]{(byte) 16, (byte) 120, (byte) 80} // Teal-Emerald #107850
        );
    }

    // =========================================================================
    // 3. MOTEUR DE GÉNÉRATION POI EXCEL
    // =========================================================================

    private byte[] buildWorkbook(String titreDoc, String tiersLabel, List<LivraisonRetardDTO> retards, String nomPdv, byte[] primaryRgb) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Livraisons en retard");
            sheet.setDisplayGridlines(true);

            CreationHelper createHelper = wb.getCreationHelper();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // --- STYLES ---
            // Titre Principal
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(new XSSFColor(primaryRgb, null));
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            // Sous-titre
            CellStyle subTitleStyle = wb.createCellStyle();
            Font subTitleFont = wb.createFont();
            subTitleFont.setFontName("Calibri");
            subTitleFont.setFontHeightInPoints((short) 10);
            subTitleFont.setItalic(true);
            subTitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subTitleStyle.setFont(subTitleFont);

            // Header Tableau
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(primaryRgb, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Cellule standard
            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setBorderColor(BorderSide.TOP, IndexedColors.GREY_25_PERCENT);
            cellStyle.setBorderColor(BorderSide.BOTTOM, IndexedColors.GREY_25_PERCENT);
            cellStyle.setBorderColor(BorderSide.LEFT, IndexedColors.GREY_25_PERCENT);
            cellStyle.setBorderColor(BorderSide.RIGHT, IndexedColors.GREY_25_PERCENT);

            // Cellule Date
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.cloneStyleFrom(cellStyle);
            dateStyle.setAlignment(HorizontalAlignment.CENTER);

            // Cellule Montant
            CellStyle amountStyle = wb.createCellStyle();
            amountStyle.cloneStyleFrom(cellStyle);
            amountStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00 \"MAD\""));
            amountStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Cellule Retard Critique (> 7j)
            CellStyle retardCritiqueStyle = wb.createCellStyle();
            retardCritiqueStyle.cloneStyleFrom(cellStyle);
            retardCritiqueStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 254, (byte) 226, (byte) 226}, null)); // Red-100
            retardCritiqueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font redFont = wb.createFont();
            redFont.setBold(true);
            redFont.setColor(new XSSFColor(new byte[]{(byte) 153, (byte) 27, (byte) 27}, null)); // Red-800
            retardCritiqueStyle.setFont(redFont);
            retardCritiqueStyle.setAlignment(HorizontalAlignment.CENTER);

            // Cellule Retard Modéré (<= 7j)
            CellStyle retardModereStyle = wb.createCellStyle();
            retardModereStyle.cloneStyleFrom(cellStyle);
            retardModereStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 254, (byte) 243, (byte) 199}, null)); // Amber-100
            retardModereStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font amberFont = wb.createFont();
            amberFont.setBold(true);
            amberFont.setColor(new XSSFColor(new byte[]{(byte) 146, (byte) 64, (byte) 14}, null)); // Amber-800
            retardModereStyle.setFont(amberFont);
            retardModereStyle.setAlignment(HorizontalAlignment.CENTER);

            // Cellule Total en bas
            CellStyle totalStyle = wb.createCellStyle();
            totalStyle.cloneStyleFrom(cellStyle);
            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 241, (byte) 245, (byte) 249}, null)); // Slate-100
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalStyle.setBorderTop(BorderStyle.MEDIUM);
            totalStyle.setBorderBottom(BorderStyle.DOUBLE);

            CellStyle totalAmountStyle = wb.createCellStyle();
            totalAmountStyle.cloneStyleFrom(totalStyle);
            totalAmountStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00 \"MAD\""));
            totalAmountStyle.setAlignment(HorizontalAlignment.RIGHT);

            // --- ÉCRITURE DES LIGNES ---
            int rowIdx = 1;

            // Ligne 1 : Titre
            Row rTitle = sheet.createRow(rowIdx++);
            Cell cTitle = rTitle.createCell(1);
            cTitle.setCellValue("📋 " + titreDoc);
            cTitle.setCellStyle(titleStyle);

            // Ligne 2 : Sous-titre / Métadonnées
            Row rMeta = sheet.createRow(rowIdx++);
            Cell cMeta = rMeta.createCell(1);
            cMeta.setCellValue("Point de Vente : " + nomPdv + "  |  Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            cMeta.setCellStyle(subTitleStyle);

            rowIdx++; // Ligne vide

            // Ligne 4 : KPIs / Résumé
            BigDecimal montantTotalCumule = retards.stream()
                    .map(LivraisonRetardDTO::getMontantTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double moyenneRetard = retards.stream()
                    .mapToLong(r -> r.getJoursRetard() != null ? r.getJoursRetard() : 0L)
                    .average().orElse(0.0);

            Row rKpi = sheet.createRow(rowIdx++);
            rKpi.createCell(1).setCellValue("Total Dossiers en retard : " + retards.size());
            rKpi.createCell(4).setCellValue("Montant Global Engagé : " + String.format("%.2f MAD", montantTotalCumule));
            rKpi.createCell(7).setCellValue("Retard Moyen : " + String.format("%.1f jours", moyenneRetard));

            rowIdx++; // Ligne vide

            // Ligne 6 : En-têtes de colonnes
            String[] headers = {
                    "N° Commande",
                    "N° Bon Livraison",
                    tiersLabel,
                    "Téléphone",
                    "Date Commande",
                    "Date Prévue",
                    "Retard (Jours)",
                    "État Livraison",
                    "Montant (MAD)",
                    "Articles & Reliquats Restants"
            };

            Row rHead = sheet.createRow(rowIdx++);
            rHead.setHeightInPoints(26);
            for (int col = 0; col < headers.length; col++) {
                Cell ch = rHead.createCell(col + 1);
                ch.setCellValue(headers[col]);
                ch.setCellStyle(headerStyle);
            }

            // Lignes de Données
            for (LivraisonRetardDTO r : retards) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                // 1. N° Commande
                Cell c1 = row.createCell(1);
                c1.setCellValue(r.getNumeroCommande() != null ? r.getNumeroCommande() : "-");
                c1.setCellStyle(cellStyle);

                // 2. N° BL
                Cell c2 = row.createCell(2);
                c2.setCellValue(r.getNumeroBl() != null ? r.getNumeroBl() : "-");
                c2.setCellStyle(cellStyle);

                // 3. Tiers (Client ou Fournisseur)
                Cell c3 = row.createCell(3);
                c3.setCellValue(r.getNomTiers() != null ? r.getNomTiers() : "-");
                c3.setCellStyle(cellStyle);

                // 4. Téléphone
                Cell c4 = row.createCell(4);
                c4.setCellValue(r.getTelephoneTiers() != null ? r.getTelephoneTiers() : "-");
                c4.setCellStyle(cellStyle);

                // 5. Date Commande
                Cell c5 = row.createCell(5);
                c5.setCellValue(r.getDateCommande() != null ? r.getDateCommande().format(dateFormatter) : "-");
                c5.setCellStyle(dateStyle);

                // 6. Date Prévue
                Cell c6 = row.createCell(6);
                c6.setCellValue(r.getDateLivraisonPrevue() != null ? r.getDateLivraisonPrevue().format(dateFormatter) : "-");
                c6.setCellStyle(dateStyle);

                // 7. Retard Jours
                Cell c7 = row.createCell(7);
                long jr = r.getJoursRetard() != null ? r.getJoursRetard() : 0;
                c7.setCellValue(jr + " j");
                c7.setCellStyle(jr > 7 ? retardCritiqueStyle : retardModereStyle);

                // 8. État
                Cell c8 = row.createCell(8);
                c8.setCellValue(r.getStatut() != null ? r.getStatut() : "-");
                c8.setCellStyle(cellStyle);

                // 9. Montant
                Cell c9 = row.createCell(9);
                c9.setCellValue(r.getMontantTotal() != null ? r.getMontantTotal().doubleValue() : 0.0);
                c9.setCellStyle(amountStyle);

                // 10. Reliquats
                Cell c10 = row.createCell(10);
                c10.setCellValue(r.getArticlesEnAttente() != null ? r.getArticlesEnAttente() : "-");
                c10.setCellStyle(cellStyle);
            }

            // Ligne de Totalisation
            Row rTotal = sheet.createRow(rowIdx++);
            rTotal.setHeightInPoints(24);
            for (int col = 1; col <= headers.length; col++) {
                Cell ct = rTotal.createCell(col);
                ct.setCellStyle(totalStyle);
            }
            Cell cTotLabel = rTotal.getCell(1);
            cTotLabel.setCellValue("TOTAL (" + retards.size() + " commandes en retard)");

            Cell cTotMontant = rTotal.getCell(9);
            cTotMontant.setCellValue(montantTotalCumule.doubleValue());
            cTotMontant.setCellStyle(totalAmountStyle);

            // Ajustement de la largeur des colonnes
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col + 1);
                int curWidth = sheet.getColumnWidth(col + 1);
                sheet.setColumnWidth(col + 1, Math.max(curWidth + 1200, 3800));
            }
            // Colonne reliquats un peu plus large
            sheet.setColumnWidth(10, 10000);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel des livraisons en retard", e);
        }
    }
}
