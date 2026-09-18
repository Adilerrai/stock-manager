package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CalculIsDTO;
import com.gestion.persistent.model.DeclarationTva;
import com.gestion.persistent.model.Societe;
import com.gestion.repository.DeclarationTvaRepository;
import com.gestion.repository.SocieteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class EdiFiscalService {

    private final DeclarationTvaRepository declarationTvaRepository;
    private final SocieteRepository societeRepository;
    private final FiscalEngineService fiscalEngineService;

    public EdiFiscalService(DeclarationTvaRepository declarationTvaRepository,
                            SocieteRepository societeRepository,
                            FiscalEngineService fiscalEngineService) {
        this.declarationTvaRepository = declarationTvaRepository;
        this.societeRepository = societeRepository;
        this.fiscalEngineService = fiscalEngineService;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // EXPORT EDI XML SIMPL-TVA (DGI MAROC)
    // =========================================================================

    public byte[] genererXmlSimplTva(Long declarationId) {
        Long tenantId = getTenantId();

        DeclarationTva decl = declarationTvaRepository.findByIdAndPointDeVenteId(declarationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Déclaration TVA introuvable ID: " + declarationId));

        Societe societe = societeRepository.findById(tenantId)
                .orElseGet(() -> societeRepository.findByTenantIdAndIsParDefautTrue(tenantId)
                        .orElseGet(Societe::new));

        String ifFiscal = societe.getIdentifiantFiscal() != null ? societe.getIdentifiantFiscal() : "";
        String ice = societe.getIce() != null ? societe.getIce() : "";
        String raisonSociale = societe.getRaisonSociale() != null ? escapeXml(societe.getRaisonSociale()) : "Société " + tenantId;

        String[] parts = decl.getPeriode().split("-");
        String annee = parts.length > 0 ? parts[0] : String.valueOf(LocalDate.now().getYear());
        String moisOuTrimestre = parts.length > 1 ? parts[1] : "1";

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<DeclarationTVA xmlns=\"http://simpl.portail.dgi.gov.ma/tva\">\n");
        
        // En-tête société
        xml.append("  <Entete>\n");
        xml.append("    <IdentifiantFiscal>").append(ifFiscal).append("</IdentifiantFiscal>\n");
        xml.append("    <ICE>").append(ice).append("</ICE>\n");
        xml.append("    <RaisonSociale>").append(raisonSociale).append("</RaisonSociale>\n");
        xml.append("    <Annee>").append(annee).append("</Annee>\n");
        xml.append("    <Periode>").append(moisOuTrimestre).append("</Periode>\n");
        xml.append("    <Regime>").append(decl.getRegime().name()).append("</Regime>\n");
        xml.append("    <Statut>").append(decl.getStatut().name()).append("</Statut>\n");
        xml.append("    <DateGeneration>").append(LocalDate.now()).append("</DateGeneration>\n");
        xml.append("  </Entete>\n");

        // Base & TVA Collectée
        xml.append("  <OperationsImposables>\n");
        xml.append("    <ChiffreAffairesTaxableHT>").append(formaterMontant(decl.getTotalVentesHT())).append("</ChiffreAffairesTaxableHT>\n");
        xml.append("    <TvaFacturee>").append(formaterMontant(decl.getTvaCollectee())).append("</TvaFacturee>\n");
        xml.append("  </OperationsImposables>\n");

        // Déductions & Prorata
        xml.append("  <Deductions>\n");
        xml.append("    <AchatsHT>").append(formaterMontant(decl.getTotalAchatsHT())).append("</AchatsHT>\n");
        xml.append("    <TvaDeductibleCharges>").append(formaterMontant(decl.getTvaDeductibleCharges())).append("</TvaDeductibleCharges>\n");
        xml.append("    <TvaDeductibleImmobilisations>").append(formaterMontant(decl.getTvaDeductibleImmo())).append("</TvaDeductibleImmobilisations>\n");
        xml.append("    <ProrataDeductionPourcentage>").append(formaterMontant(decl.getProrata())).append("</ProrataDeductionPourcentage>\n");
        xml.append("    <TotalTvaDeductible>").append(formaterMontant(decl.getTvaDeductibleTotal())).append("</TotalTvaDeductible>\n");
        xml.append("    <CreditTvaPrecedentMois>").append(formaterMontant(decl.getCreditTvaAnterieur())).append("</CreditTvaPrecedentMois>\n");
        xml.append("  </Deductions>\n");

        // Résultat de la liquidation
        xml.append("  <ResultatLiquidation>\n");
        xml.append("    <TvaDueAPayer>").append(formaterMontant(decl.getTvaAPayer())).append("</TvaDueAPayer>\n");
        xml.append("    <CreditTvaReportable>").append(formaterMontant(decl.getCreditTvaReportable())).append("</CreditTvaReportable>\n");
        xml.append("  </ResultatLiquidation>\n");

        xml.append("</DeclarationTVA>\n");

        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // EXPORT EDI XML SIMPL-IS (DGI MAROC)
    // =========================================================================

    public byte[] genererXmlSimplIs(int annee, BigDecimal reintegrations, BigDecimal deductions) {
        Long tenantId = getTenantId();

        Societe societe = societeRepository.findById(tenantId)
                .orElseGet(() -> societeRepository.findByTenantIdAndIsParDefautTrue(tenantId)
                        .orElseGet(Societe::new));

        CalculIsDTO isDto = fiscalEngineService.calculerIs(annee, reintegrations, deductions);

        String ifFiscal = societe.getIdentifiantFiscal() != null ? societe.getIdentifiantFiscal() : "";
        String ice = societe.getIce() != null ? societe.getIce() : "";
        String raisonSociale = societe.getRaisonSociale() != null ? escapeXml(societe.getRaisonSociale()) : "Société " + tenantId;

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<DeclarationIS xmlns=\"http://simpl.portail.dgi.gov.ma/is\">\n");

        // En-tête
        xml.append("  <Entete>\n");
        xml.append("    <IdentifiantFiscal>").append(ifFiscal).append("</IdentifiantFiscal>\n");
        xml.append("    <ICE>").append(ice).append("</ICE>\n");
        xml.append("    <RaisonSociale>").append(raisonSociale).append("</RaisonSociale>\n");
        xml.append("    <Exercice>").append(annee).append("</Exercice>\n");
        xml.append("    <DateDepotLegal>").append(annee + 1).append("-03-31</DateDepotLegal>\n");
        xml.append("    <DateGeneration>").append(LocalDate.now()).append("</DateGeneration>\n");
        xml.append("  </Entete>\n");

        // Résultat Fiscal
        xml.append("  <PassageFiscal>\n");
        xml.append("    <ResultatComptableAvantImpot>").append(formaterMontant(isDto.getResultatComptableAvantImpot())).append("</ResultatComptableAvantImpot>\n");
        xml.append("    <ReintegrationsFiscales>").append(formaterMontant(isDto.getReintegrationsFiscales())).append("</ReintegrationsFiscales>\n");
        xml.append("    <DeductionsFiscales>").append(formaterMontant(isDto.getDeductionsFiscales())).append("</DeductionsFiscales>\n");
        xml.append("    <ResultatFiscalNet>").append(formaterMontant(isDto.getResultatFiscal())).append("</ResultatFiscalNet>\n");
        xml.append("  </PassageFiscal>\n");

        // Liquidation de l'Impôt
        xml.append("  <LiquidationImpot>\n");
        xml.append("    <BaseCotisationMinimale>").append(formaterMontant(isDto.getBaseCotisationMinimale())).append("</BaseCotisationMinimale>\n");
        xml.append("    <MontantCotisationMinimale>").append(formaterMontant(isDto.getCotisationMinimaleRetenue())).append("</MontantCotisationMinimale>\n");
        xml.append("    <MontantISBaremeProgressif>").append(formaterMontant(isDto.getIsCalculeBareme())).append("</MontantISBaremeProgressif>\n");
        xml.append("    <ImpotExigibleRetenu>").append(formaterMontant(isDto.getImpotExigible())).append("</ImpotExigibleRetenu>\n");
        xml.append("    <NatureImpot>").append(isDto.getNatureImpotRetenu()).append("</NatureImpot>\n");
        xml.append("    <TotalAcomptesPayesCompte3453>").append(formaterMontant(isDto.getAcomptesVerses())).append("</TotalAcomptesPayesCompte3453>\n");
        xml.append("    <ReliquatISAPayer>").append(formaterMontant(isDto.getReliquatAPayer())).append("</ReliquatISAPayer>\n");
        xml.append("    <ExcedentISAReporter>").append(formaterMontant(isDto.getExcedentVersement())).append("</ExcedentISAReporter>\n");
        xml.append("  </LiquidationImpot>\n");

        xml.append("</DeclarationIS>\n");

        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formaterMontant(BigDecimal m) {
        return (m != null ? m : BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
