package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.FournisseurDTO;
import com.gestion.persistent.dto.FournisseurSearchCriteria;
import com.gestion.persistent.model.Fournisseur;
import com.gestion.repository.FournisseurRepository;
import com.gestion.repository.LigneCommandeRepository;
import com.gestion.persistent.model.LigneCommande;
import com.gestion.persistent.dto.FournisseurProduitResumeDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    public FournisseurService(FournisseurRepository fournisseurRepository,
                              LigneCommandeRepository ligneCommandeRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new org.springframework.security.access.AccessDeniedException("Impossible d'identifier l'entreprise (tenant) courante");
        }
        return tenant;
    }

    @Transactional
    public Fournisseur createFournisseur(FournisseurDTO fournisseurDTO) {
        Long tenantId = getTenantId();
        if (fournisseurRepository.existsByRaisonSocialeAndPointDeVenteId(fournisseurDTO.getRaisonSociale(), tenantId)) {
            throw new IllegalArgumentException("Un fournisseur avec ce nom existe déjà");
        }

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        fournisseur.setAdresse(fournisseurDTO.getAdresse());
        fournisseur.setTelephone(fournisseurDTO.getTelephone());
        fournisseur.setEmail(fournisseurDTO.getEmail());
        fournisseur.setContact(fournisseurDTO.getContact());
        fournisseur.setIce(fournisseurDTO.getIce());
        fournisseur.setNumeroRegistreCommerce(fournisseurDTO.getNumeroRegistreCommerce());
        fournisseur.setNumeroIdentificationFiscale(fournisseurDTO.getNumeroIdentificationFiscale());
        fournisseur.setPatente(fournisseurDTO.getPatente());
        fournisseur.setRibBancaire(fournisseurDTO.getRibBancaire());
        fournisseur.setBanqueNom(fournisseurDTO.getBanqueNom());
        fournisseur.setDelaiPaiementJours(fournisseurDTO.getDelaiPaiementJours() != null ? fournisseurDTO.getDelaiPaiementJours() : 30);
        fournisseur.setVille(fournisseurDTO.getVille());
        fournisseur.setCodePostal(fournisseurDTO.getCodePostal());
        fournisseur.setPays(fournisseurDTO.getPays() != null ? fournisseurDTO.getPays() : "Maroc");
        fournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
        fournisseur.setActif(true);
        fournisseur.setPointDeVenteId(tenantId);

        if (fournisseurDTO.getTypeFournisseur() != null) {
            fournisseur.setTypeFournisseur(fournisseurDTO.getTypeFournisseur());
        }
        if (fournisseurDTO.getDevise() != null && !fournisseurDTO.getDevise().isBlank()) {
            fournisseur.setDevise(fournisseurDTO.getDevise());
        }

        return fournisseurRepository.save(fournisseur);
    }

    public List<Fournisseur> getAllFournisseursActifs() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return List.of();
        }
        return fournisseurRepository.findActiveByPointDeVenteIdOrderByNom(tenant);
    }

    public Fournisseur getFournisseurById(Long fournisseurId) {
        Long tenantId = getTenantId();
        return fournisseurRepository.findByIdAndPointDeVenteId(fournisseurId, tenantId)
                .orElseGet(() -> fournisseurRepository.findById(fournisseurId)
                        .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", fournisseurId)));
    }

    @Transactional
    public Fournisseur updateFournisseur(FournisseurDTO fournisseurDTO) {
        Fournisseur fournisseur = getFournisseurById(fournisseurDTO.getId());
        
        // Mise à jour des champs
        if (fournisseurDTO.getRaisonSociale() != null) {
            fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        }
        if (fournisseurDTO.getAdresse() != null) {
            fournisseur.setAdresse(fournisseurDTO.getAdresse());
        }
        if (fournisseurDTO.getTelephone() != null) {
            fournisseur.setTelephone(fournisseurDTO.getTelephone());
        }
        if (fournisseurDTO.getEmail() != null) {
            fournisseur.setEmail(fournisseurDTO.getEmail());
        }
        if (fournisseurDTO.getContact() != null) {
            fournisseur.setContact(fournisseurDTO.getContact());
        }
        if (fournisseurDTO.getIce() != null) {
            fournisseur.setIce(fournisseurDTO.getIce());
        }
        if (fournisseurDTO.getNumeroRegistreCommerce() != null) {
            fournisseur.setNumeroRegistreCommerce(fournisseurDTO.getNumeroRegistreCommerce());
        }
        if (fournisseurDTO.getNumeroIdentificationFiscale() != null) {
            fournisseur.setNumeroIdentificationFiscale(fournisseurDTO.getNumeroIdentificationFiscale());
        }
        if (fournisseurDTO.getPatente() != null) {
            fournisseur.setPatente(fournisseurDTO.getPatente());
        }
        if (fournisseurDTO.getRibBancaire() != null) {
            fournisseur.setRibBancaire(fournisseurDTO.getRibBancaire());
        }
        if (fournisseurDTO.getBanqueNom() != null) {
            fournisseur.setBanqueNom(fournisseurDTO.getBanqueNom());
        }
        if (fournisseurDTO.getDelaiPaiementJours() != null) {
            fournisseur.setDelaiPaiementJours(fournisseurDTO.getDelaiPaiementJours());
        }
        if (fournisseurDTO.getVille() != null) {
            fournisseur.setVille(fournisseurDTO.getVille());
        }
        if (fournisseurDTO.getCodePostal() != null) {
            fournisseur.setCodePostal(fournisseurDTO.getCodePostal());
        }
        if (fournisseurDTO.getPays() != null) {
            fournisseur.setPays(fournisseurDTO.getPays());
        }
        if (fournisseurDTO.getConditionsPaiement() != null) {
            fournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
        }
        if (fournisseurDTO.getActif() != null) {
            fournisseur.setActif(fournisseurDTO.getActif());
        }
        if (fournisseurDTO.getTypeFournisseur() != null) {
            fournisseur.setTypeFournisseur(fournisseurDTO.getTypeFournisseur());
        }
        if (fournisseurDTO.getDevise() != null) {
            fournisseur.setDevise(fournisseurDTO.getDevise());
        }

        return fournisseurRepository.save(fournisseur);
    }

    @Transactional
    public void deleteFournisseur(Long fournisseurId) {
        Fournisseur fournisseur = getFournisseurById(fournisseurId);
        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }

    public Page<Fournisseur> searchFournisseurs(FournisseurSearchCriteria criteria, Pageable pageable) {
        return fournisseurRepository.findByCriteria(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public List<FournisseurProduitResumeDTO> getEtatProduitsCommandes(Long fournisseurId, Long produitId, String search) {
        Long tenantId = getTenantId();
        List<LigneCommande> lignes;
        if (produitId != null) {
            lignes = ligneCommandeRepository.findByFournisseurAndProduitAndTenant(fournisseurId, produitId, tenantId);
        } else {
            lignes = ligneCommandeRepository.findByFournisseurAndTenant(fournisseurId, tenantId);
        }

        Map<Long, FournisseurProduitResumeDTO> resumeMap = new LinkedHashMap<>();

        for (LigneCommande lc : lignes) {
            if (lc.getProduit() == null) continue;
            Long pId = lc.getProduit().getId();

            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase();
                boolean matchRef = lc.getProduit().getReference() != null && lc.getProduit().getReference().toLowerCase().contains(term);
                boolean matchNom = lc.getProduit().getDesignation() != null && lc.getProduit().getDesignation().toLowerCase().contains(term);
                if (!matchRef && !matchNom) {
                    continue;
                }
            }

            FournisseurProduitResumeDTO dto = resumeMap.computeIfAbsent(pId, id -> {
                FournisseurProduitResumeDTO r = new FournisseurProduitResumeDTO();
                r.setProduitId(id);
                r.setReference(lc.getProduit().getReference());
                r.setDesignation(lc.getProduit().getDesignation());
                if (lc.getProduit().getCategorie() != null) {
                    r.setCategorie(lc.getProduit().getCategorie().getNom());
                }
                return r;
            });

            int qteCmd = lc.getQuantiteCommandee() != null ? lc.getQuantiteCommandee() : 0;
            int qteLiv = lc.getQuantiteLivree() != null ? lc.getQuantiteLivree() : 0;
            BigDecimal prixHT = lc.getPrixUnitaireHT() != null ? lc.getPrixUnitaireHT() : (lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO);
            BigDecimal mntHT = lc.getMontantHT() != null ? lc.getMontantHT() : prixHT.multiply(BigDecimal.valueOf(qteCmd));
            BigDecimal mntTTC = lc.getMontantTTC() != null ? lc.getMontantTTC() : mntHT.multiply(new BigDecimal("1.20"));

            dto.setQuantiteTotaleCommandee(dto.getQuantiteTotaleCommandee() + qteCmd);
            dto.setQuantiteTotaleLivree(dto.getQuantiteTotaleLivree() + qteLiv);
            dto.setMontantTotalHT(dto.getMontantTotalHT().add(mntHT));
            dto.setMontantTotalTTC(dto.getMontantTotalTTC().add(mntTTC));
            dto.setNombreCommandes(dto.getNombreCommandes() + 1);

            if (dto.getDernierPrixAchat().compareTo(BigDecimal.ZERO) == 0 && prixHT.compareTo(BigDecimal.ZERO) > 0) {
                dto.setDernierPrixAchat(prixHT);
            }
            if (dto.getDateDerniereCommande() == null && lc.getCommande() != null) {
                dto.setDateDerniereCommande(lc.getCommande().getDateCommande());
            }

            FournisseurProduitResumeDTO.CommandeReferenceResumeDTO cmdRef = new FournisseurProduitResumeDTO.CommandeReferenceResumeDTO();
            if (lc.getCommande() != null) {
                cmdRef.setCommandeId(lc.getCommande().getId());
                cmdRef.setNumeroCommande(lc.getCommande().getNumeroCommande());
                cmdRef.setDateCommande(lc.getCommande().getDateCommande());
                cmdRef.setStatut(lc.getCommande().getStatut() != null ? lc.getCommande().getStatut().name() : null);
            }
            cmdRef.setQuantiteCommandee(qteCmd);
            cmdRef.setQuantiteLivree(qteLiv);
            cmdRef.setPrixUnitaireHT(prixHT);
            cmdRef.setMontantHT(mntHT);
            dto.getCommandes().add(cmdRef);
        }

        for (FournisseurProduitResumeDTO r : resumeMap.values()) {
            if (r.getQuantiteTotaleCommandee() > 0 && r.getMontantTotalHT().compareTo(BigDecimal.ZERO) > 0) {
                r.setPrixMoyen(r.getMontantTotalHT().divide(BigDecimal.valueOf(r.getQuantiteTotaleCommandee()), 2, RoundingMode.HALF_UP));
            } else {
                r.setPrixMoyen(r.getDernierPrixAchat());
            }
        }

        return new ArrayList<>(resumeMap.values());
    }
    // =========================================================================
    // EXPORT EXCEL : ÉTAT DES PRODUITS COMMANDÉS PAR FOURNISSEUR
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] genererExcelEtatProduitsCommandes(Long fournisseurId, Long produitId, String search) {
        Fournisseur fournisseur = getFournisseurById(fournisseurId);
        List<FournisseurProduitResumeDTO> items = getEtatProduitsCommandes(fournisseurId, produitId, search);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Produits commandés");

            DataFormat dataFormat = workbook.createDataFormat();
            short integerFormat = dataFormat.getFormat("#,##0");
            short moneyFormat = dataFormat.getFormat("#,##0.00");

            // Fonts
            Font fontTitre = workbook.createFont();
            fontTitre.setFontHeightInPoints((short) 14);
            fontTitre.setBold(true);
            fontTitre.setColor(IndexedColors.ROYAL_BLUE.getIndex());

            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontHeightInPoints((short) 9);
            fontSubtitle.setItalic(true);
            fontSubtitle.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font fontHeader = workbook.createFont();
            fontHeader.setFontHeightInPoints((short) 10);
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());

            Font fontData = workbook.createFont();
            fontData.setFontHeightInPoints((short) 10);

            Font fontBold = workbook.createFont();
            fontBold.setFontHeightInPoints((short) 10);
            fontBold.setBold(true);

            // Styles
            CellStyle styleTitle = workbook.createCellStyle();
            styleTitle.setFont(fontTitre);
            styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleSub = workbook.createCellStyle();
            styleSub.setFont(fontSubtitle);
            styleSub.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleHeader = workbook.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);
            styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            styleHeader.setBorderTop(BorderStyle.MEDIUM);
            styleHeader.setBorderBottom(BorderStyle.MEDIUM);
            styleHeader.setBorderLeft(BorderStyle.THIN);
            styleHeader.setBorderRight(BorderStyle.THIN);

            CellStyle styleText = createBorderedStyle(workbook, fontData, HorizontalAlignment.LEFT);
            CellStyle styleCenter = createBorderedStyle(workbook, fontData, HorizontalAlignment.CENTER);
            CellStyle styleQty = createBorderedStyle(workbook, fontData, HorizontalAlignment.RIGHT);
            styleQty.setDataFormat(integerFormat);
            CellStyle styleMoney = createBorderedStyle(workbook, fontData, HorizontalAlignment.RIGHT);
            styleMoney.setDataFormat(moneyFormat);
            CellStyle styleMoneyBold = createBorderedStyle(workbook, fontBold, HorizontalAlignment.RIGHT);
            styleMoneyBold.setDataFormat(moneyFormat);

            CellStyle styleTotal = workbook.createCellStyle();
            styleTotal.setFont(fontBold);
            styleTotal.setBorderTop(BorderStyle.THIN);
            styleTotal.setBorderBottom(BorderStyle.DOUBLE);
            styleTotal.setBorderLeft(BorderStyle.THIN);
            styleTotal.setBorderRight(BorderStyle.THIN);
            styleTotal.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            styleTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleTotal.setAlignment(HorizontalAlignment.RIGHT);
            styleTotal.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleTotalLabel = workbook.createCellStyle();
            styleTotalLabel.cloneStyleFrom(styleTotal);
            styleTotalLabel.setAlignment(HorizontalAlignment.LEFT);

            CellStyle styleTotalQty = workbook.createCellStyle();
            styleTotalQty.cloneStyleFrom(styleTotal);
            styleTotalQty.setDataFormat(integerFormat);

            CellStyle styleTotalMoney = workbook.createCellStyle();
            styleTotalMoney.cloneStyleFrom(styleTotal);
            styleTotalMoney.setDataFormat(moneyFormat);

            String[] headers = {
                    "Référence", "Désignation", "Catégorie", "Nb commandes",
                    "Qté commandée", "Qté reçue", "Qté restante",
                    "Dernier prix HT", "Prix moyen HT", "Montant total HT", "Montant total TTC", "Dernière commande"
            };
            int lastCol = headers.length - 1;

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // 1. Titre
            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            rowTitle.setHeightInPoints(24f);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("État des produits commandés - " + (fournisseur.getNom() != null ? fournisseur.getNom() : ""));
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));

            // 2. Sous-titre
            Row rowMeta = sheet.createRow(rowIdx++);
            Cell cellMeta = rowMeta.createCell(0);
            String meta = "Fournisseur #" + fournisseur.getId()
                    + "   |   Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            if (search != null && !search.isBlank()) {
                meta += "   |   Filtre : \"" + search.trim() + "\"";
            }
            cellMeta.setCellValue(meta);
            cellMeta.setCellStyle(styleSub);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastCol));

            rowIdx++; // ligne vide

            // 3. En-têtes
            Row rowHeader = sheet.createRow(rowIdx++);
            rowHeader.setHeightInPoints(20f);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = rowHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }
            int firstDataRow = rowIdx;

            // 4. Données
            long totalCmd = 0, totalLiv = 0;
            BigDecimal totalHT = BigDecimal.ZERO, totalTTC = BigDecimal.ZERO;

            for (FournisseurProduitResumeDTO it : items) {
                Row row = sheet.createRow(rowIdx++);
                int qCmd = it.getQuantiteTotaleCommandee() != null ? it.getQuantiteTotaleCommandee() : 0;
                int qLiv = it.getQuantiteTotaleLivree() != null ? it.getQuantiteTotaleLivree() : 0;
                BigDecimal mHT = it.getMontantTotalHT() != null ? it.getMontantTotalHT() : BigDecimal.ZERO;
                BigDecimal mTTC = it.getMontantTotalTTC() != null ? it.getMontantTotalTTC() : BigDecimal.ZERO;

                int c = 0;
                Cell cRef = row.createCell(c++);
                cRef.setCellValue(it.getReference() != null ? it.getReference() : "-");
                cRef.setCellStyle(styleText);

                Cell cDes = row.createCell(c++);
                cDes.setCellValue(it.getDesignation() != null ? it.getDesignation() : "-");
                cDes.setCellStyle(styleText);

                Cell cCat = row.createCell(c++);
                cCat.setCellValue(it.getCategorie() != null ? it.getCategorie() : "-");
                cCat.setCellStyle(styleText);

                Cell cNb = row.createCell(c++);
                cNb.setCellValue(it.getNombreCommandes() != null ? it.getNombreCommandes() : 0);
                cNb.setCellStyle(styleCenter);

                Cell cQc = row.createCell(c++);
                cQc.setCellValue(qCmd);
                cQc.setCellStyle(styleQty);

                Cell cQl = row.createCell(c++);
                cQl.setCellValue(qLiv);
                cQl.setCellStyle(styleQty);

                Cell cQr = row.createCell(c++);
                cQr.setCellValue(Math.max(qCmd - qLiv, 0));
                cQr.setCellStyle(styleQty);

                Cell cDp = row.createCell(c++);
                cDp.setCellValue(it.getDernierPrixAchat() != null ? it.getDernierPrixAchat().doubleValue() : 0d);
                cDp.setCellStyle(styleMoney);

                Cell cPm = row.createCell(c++);
                cPm.setCellValue(it.getPrixMoyen() != null ? it.getPrixMoyen().doubleValue() : 0d);
                cPm.setCellStyle(styleMoney);

                Cell cHt = row.createCell(c++);
                cHt.setCellValue(mHT.doubleValue());
                cHt.setCellStyle(styleMoneyBold);

                Cell cTtc = row.createCell(c++);
                cTtc.setCellValue(mTTC.doubleValue());
                cTtc.setCellStyle(styleMoney);

                Cell cDate = row.createCell(c++);
                cDate.setCellValue(it.getDateDerniereCommande() != null ? it.getDateDerniereCommande().format(dtf) : "-");
                cDate.setCellStyle(styleCenter);

                totalCmd += qCmd;
                totalLiv += qLiv;
                totalHT = totalHT.add(mHT);
                totalTTC = totalTTC.add(mTTC);
            }

            // 5. Totaux
            Row rowTotal = sheet.createRow(rowIdx++);
            rowTotal.setHeightInPoints(18f);
            for (int i = 0; i <= lastCol; i++) {
                Cell cell = rowTotal.createCell(i);
                cell.setCellStyle(styleTotal);
            }
            rowTotal.getCell(0).setCellValue("TOTAL (" + items.size() + " article" + (items.size() > 1 ? "s" : "") + ")");
            rowTotal.getCell(0).setCellStyle(styleTotalLabel);
            sheet.addMergedRegion(new CellRangeAddress(rowTotal.getRowNum(), rowTotal.getRowNum(), 0, 3));
            rowTotal.getCell(4).setCellValue(totalCmd);
            rowTotal.getCell(4).setCellStyle(styleTotalQty);
            rowTotal.getCell(5).setCellValue(totalLiv);
            rowTotal.getCell(5).setCellStyle(styleTotalQty);
            rowTotal.getCell(6).setCellValue(Math.max(totalCmd - totalLiv, 0));
            rowTotal.getCell(6).setCellStyle(styleTotalQty);
            rowTotal.getCell(9).setCellValue(totalHT.doubleValue());
            rowTotal.getCell(9).setCellStyle(styleTotalMoney);
            rowTotal.getCell(10).setCellValue(totalTTC.doubleValue());
            rowTotal.getCell(10).setCellStyle(styleTotalMoney);

            // 6. Mise en page
            sheet.createFreezePane(0, firstDataRow);
            if (!items.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(firstDataRow - 1, rowIdx - 2, 0, lastCol));
            }
            int[] widths = {18, 45, 24, 13, 14, 12, 13, 16, 16, 18, 18, 18};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du fichier Excel des produits commandés", e);
        }
    }

    private CellStyle createBorderedStyle(Workbook workbook, Font font, HorizontalAlignment align) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }
}
