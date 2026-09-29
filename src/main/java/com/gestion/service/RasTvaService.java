package com.gestion.service;

import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.enums.TypeOperationRas;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier dédié à la gestion de la Retenue à la Source (RAS) sur TVA,
 * Loyers et Honoraires selon le Code Général des Impôts (CGI Maroc - Art. 117-VI, 89-I et 157).
 */
@Service
@Transactional
public class RasTvaService {

    private final FactureAchatRepository factureAchatRepository;
    private final ReglementFournisseurRepository reglementRepository;
    private final SocieteRepository societeRepository;
    private final FournisseurRepository fournisseurRepository;
    private final JournalComptableRepository journalRepository;
    private final CompteComptableRepository compteRepository;
    private final EcritureComptableRepository ecritureRepository;
    private final LigneEcritureRepository ligneRepository;
    private final ComptabiliteService comptabiliteService;

    public RasTvaService(FactureAchatRepository factureAchatRepository,
                         ReglementFournisseurRepository reglementRepository,
                         SocieteRepository societeRepository,
                         FournisseurRepository fournisseurRepository,
                         JournalComptableRepository journalRepository,
                         CompteComptableRepository compteRepository,
                         EcritureComptableRepository ecritureRepository,
                         LigneEcritureRepository ligneRepository,
                         ComptabiliteService comptabiliteService) {
        this.factureAchatRepository = factureAchatRepository;
        this.reglementRepository = reglementRepository;
        this.societeRepository = societeRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.journalRepository = journalRepository;
        this.compteRepository = compteRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneRepository = ligneRepository;
        this.comptabiliteService = comptabiliteService;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    // =========================================================================
    // 1. MOTEUR DE CALCUL AUTOMATIQUE DE LA RAS
    // =========================================================================

    public RasCalculResultDTO calculerRas(RasCalculRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("La requête de calcul est obligatoire.");
        }

        BigDecimal ht = req.getMontantHt();
        BigDecimal tauxTva = req.getTauxTva() != null ? req.getTauxTva() : new BigDecimal("20.0");
        TypeOperationRas typeOp = req.getTypeOperation() != null ? req.getTypeOperation() : TypeOperationRas.PRESTATIONS_SERVICES;
        boolean hasArf = req.getAttestationRegularitePresente();

        // Si la facture d'achat est spécifiée et le HT n'est pas fourni
        if (ht == null && req.getFactureAchatId() != null) {
            FactureAchat facture = factureAchatRepository.findByIdAndPointDeVenteId(req.getFactureAchatId(), getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Facture d'achat introuvable : " + req.getFactureAchatId()));
            ht = facture.getMontantHt() != null ? facture.getMontantHt() : BigDecimal.ZERO;
            if (facture.getMontantTva() != null && ht.compareTo(BigDecimal.ZERO) > 0) {
                tauxTva = facture.getMontantTva().divide(ht, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }
        }

        if (ht == null) {
            ht = BigDecimal.ZERO;
        }

        BigDecimal montantTva = ht.multiply(tauxTva).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal montantTtc = ht.add(montantTva);

        BigDecimal tauxRas = BigDecimal.ZERO;
        BigDecimal montantRas = BigDecimal.ZERO;
        String baseCalcul = "TVA";
        String justification = "";

        switch (typeOp) {
            case BIENS_EQUIPEMENT_TRAVAUX:
                if (hasArf) {
                    tauxRas = BigDecimal.ZERO;
                    montantRas = BigDecimal.ZERO;
                    justification = "Fournisseur avec Attestation de Régularité Fiscale (ARF < 6 mois) valide. Aucune retenue à la source applicable sur les biens d'équipement et travaux (Art. 117-VI CGI).";
                } else {
                    tauxRas = new BigDecimal("100.0");
                    montantRas = montantTva;
                    justification = "Défaut d'attestation de régularité fiscale : Retenue obligatoire de 100% de la TVA due sur biens d'équipement et travaux (Art. 117-VI CGI).";
                }
                break;

            case PRESTATIONS_SERVICES:
                if (hasArf) {
                    tauxRas = new BigDecimal("75.0");
                    montantRas = montantTva.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
                    justification = "Prestations de services assujetties (Art. 89-I CGI) avec ARF valide : Retenue légale de 75% de la TVA (Art. 117-VI CGI).";
                } else {
                    tauxRas = new BigDecimal("100.0");
                    montantRas = montantTva;
                    justification = "Prestations de services sans attestation de régularité fiscale : Retenue de 100% de la TVA due (Art. 117-VI CGI).";
                }
                break;

            case PRESTATAIRES_NON_RESIDENTS:
                tauxRas = new BigDecimal("100.0");
                montantRas = montantTva;
                justification = "Fournisseur étranger non-résident fiscal au Maroc : Retenue de 100% de la TVA due (Art. 117-VI CGI).";
                break;

            case LOYERS_COMMERCIAUX:
                baseCalcul = "HT";
                tauxRas = new BigDecimal("5.0");
                montantRas = ht.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
                justification = "Loi de Finances : Retenue à la source de 5% sur les loyers commerciaux versés à des personnes physiques.";
                break;

            case HONORAIRES_LIBERAUX:
                baseCalcul = "HT";
                tauxRas = new BigDecimal("10.0");
                montantRas = ht.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                justification = "Art. 157 CGI : Retenue à la source de 10% sur les honoraires versés à des personnes morales (ou 5% personnes physiques).";
                break;

            case AUTRE:
            default:
                tauxRas = new BigDecimal("100.0");
                montantRas = montantTva;
                justification = "Retenue à la source personnalisée.";
                break;
        }

        BigDecimal montantNetAPayer = montantTtc.subtract(montantRas);

        RasCalculResultDTO res = new RasCalculResultDTO();
        res.setMontantHt(ht);
        res.setTauxTva(tauxTva);
        res.setMontantTva(montantTva);
        res.setMontantTtc(montantTtc);
        res.setTypeOperation(typeOp);
        res.setAttestationRegularitePresente(hasArf);
        res.setTauxRas(tauxRas);
        res.setMontantRas(montantRas);
        res.setMontantNetAPayer(montantNetAPayer);
        res.setBaseCalcul(baseCalcul);
        res.setJustificationLegale(justification);
        res.setCompteTvaRetenue("44580000");
        res.setCompteFournisseur("44110000");
        res.setCompteTresorerie("51410000");

        return res;
    }

    // =========================================================================
    // 2. GÉNÉRATION DE L'ATTESTATION LÉGALE DE RETENUE À LA SOURCE
    // =========================================================================

    public AttestationRasDTO genererAttestationDepuisReglement(Long reglementId, TypeOperationRas typeOp, Boolean attestationPresente) {
        Long tenantId = getTenantId();
        ReglementFournisseur reglement = reglementRepository.findByIdAndPointDeVenteId(reglementId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Règlement fournisseur introuvable : " + reglementId));

        FactureAchat facture = reglement.getFactureAchat();
        if (facture == null) {
            throw new IllegalArgumentException("Le règlement " + reglementId + " n'est rattaché à aucune facture d'achat.");
        }

        Fournisseur fournisseur = facture.getFournisseur();
        Societe societe = societeRepository.findById(tenantId).orElse(null);

        // Simulation / calcul RAS
        TypeOperationRas op = typeOp != null ? typeOp : TypeOperationRas.PRESTATIONS_SERVICES;
        boolean hasArf = attestationPresente != null && attestationPresente;

        RasCalculRequest req = new RasCalculRequest(
                facture.getMontantHt(),
                facture.getMontantTva() != null && facture.getMontantHt() != null && facture.getMontantHt().compareTo(BigDecimal.ZERO) > 0
                        ? facture.getMontantTva().divide(facture.getMontantHt(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                        : new BigDecimal("20.0"),
                op,
                hasArf
        );
        req.setFactureAchatId(facture.getId());
        RasCalculResultDTO calc = calculerRas(req);

        AttestationRasDTO att = new AttestationRasDTO();
        att.setId(reglement.getId());
        int annee = reglement.getDateReglement() != null ? reglement.getDateReglement().getYear() : LocalDate.now().getYear();
        att.setNumeroAttestation("AT-RAS-" + annee + "-" + String.format("%04d", reglement.getId()));
        att.setDateAttestation(LocalDate.now());
        att.setExerciceAnnee(annee);
        att.setPeriodeMois(reglement.getDateReglement() != null ? reglement.getDateReglement().getMonthValue() : LocalDate.now().getMonthValue());
        att.setPeriodeTrimestre((att.getPeriodeMois() - 1) / 3 + 1);

        // Acheteur (Déclarant)
        if (societe != null) {
            att.setSocieteId(societe.getId());
            att.setSocieteRaisonSociale(societe.getRaisonSociale());
            att.setSocieteIce(societe.getIce());
            att.setSocieteIf(societe.getIdentifiantFiscal());
            att.setSocieteRc(societe.getRc());
            att.setSocieteAdresse(societe.getAdresse());
            att.setSocieteVille(societe.getVille());
        } else {
            att.setSocieteId(tenantId);
            att.setSocieteRaisonSociale("Société " + tenantId);
            att.setSocieteIce("000000000000000");
            att.setSocieteIf("00000000");
        }

        // Fournisseur (Bénéficiaire)
        if (fournisseur != null) {
            att.setFournisseurId(fournisseur.getId());
            att.setFournisseurNom(fournisseur.getRaisonSociale());
            att.setFournisseurIce(fournisseur.getIce());
            att.setFournisseurIf(fournisseur.getNumeroIdentificationFiscale());
            att.setFournisseurRc(fournisseur.getNumeroRegistreCommerce());
            att.setFournisseurAdresse(fournisseur.getAdresse());
            att.setFournisseurVille(fournisseur.getVille());
        }

        // Facture
        att.setFactureAchatId(facture.getId());
        att.setFactureNumero(facture.getNumeroFacture());
        att.setFactureDate(facture.getDateFacture() != null ? facture.getDateFacture().toLocalDate() : null);
        att.setMontantHt(calc.getMontantHt());
        att.setTauxTva(calc.getTauxTva());
        att.setMontantTva(calc.getMontantTva());
        att.setMontantTtc(calc.getMontantTtc());

        // Règlement & Retenue
        att.setReglementId(reglement.getId());
        att.setDatePaiement(reglement.getDateReglement() != null ? reglement.getDateReglement().toLocalDate() : LocalDate.now());
        att.setModePaiement(reglement.getModePaiement() != null ? reglement.getModePaiement().name() : "VIREMENT");
        att.setReferencePaiement(reglement.getReferencePaiement());
        att.setTypeOperation(op);
        att.setAttestationFiscaleFournisseurPresente(hasArf);
        att.setTauxRas(calc.getTauxRas());
        att.setMontantRas(calc.getMontantRas());
        att.setMontantNetPaye(calc.getMontantNetAPayer());
        att.setMentionLegale(calc.getJustificationLegale());
        att.setStatut("VALIDEE");

        return att;
    }

    // =========================================================================
    // 3. DÉCLARATION PÉRIODIQUE & BORDEREAU RAS (LIGNE 138 SIMPL-TVA)
    // =========================================================================

    public DeclarationRasPeriodeDTO getDeclarationPeriode(Integer anneeParam, Integer moisParam, Integer trimestreParam) {
        Long tenantId = getTenantId();
        int annee = anneeParam != null ? anneeParam : (CurrentRequestContext.getYear() != null ? CurrentRequestContext.getYear() : LocalDate.now().getYear());

        LocalDateTime start;
        LocalDateTime end;

        if (moisParam != null && moisParam >= 1 && moisParam <= 12) {
            LocalDate d1 = LocalDate.of(annee, moisParam, 1);
            start = d1.atStartOfDay();
            end = d1.plusMonths(1).minusDays(1).atTime(23, 59, 59);
        } else if (trimestreParam != null && trimestreParam >= 1 && trimestreParam <= 4) {
            int startMonth = (trimestreParam - 1) * 3 + 1;
            LocalDate d1 = LocalDate.of(annee, startMonth, 1);
            start = d1.atStartOfDay();
            end = d1.plusMonths(3).minusDays(1).atTime(23, 59, 59);
        } else {
            // Toute l'année
            start = LocalDate.of(annee, 1, 1).atStartOfDay();
            end = LocalDate.of(annee, 12, 31).atTime(23, 59, 59);
        }

        List<ReglementFournisseur> reglements = reglementRepository.findByPeriodeAndPointDeVenteId(start, end, tenantId);

        Societe societe = societeRepository.findById(tenantId).orElse(null);
        DeclarationRasPeriodeDTO declaration = new DeclarationRasPeriodeDTO();
        declaration.setAnnee(annee);
        declaration.setMois(moisParam);
        declaration.setTrimestre(trimestreParam);
        if (societe != null) {
            declaration.setSocieteRaisonSociale(societe.getRaisonSociale());
            declaration.setSocieteIce(societe.getIce());
            declaration.setSocieteIf(societe.getIdentifiantFiscal());
        } else {
            declaration.setSocieteRaisonSociale("Société " + tenantId);
        }

        BigDecimal sumHt = BigDecimal.ZERO;
        BigDecimal sumTva = BigDecimal.ZERO;
        BigDecimal sumRas = BigDecimal.ZERO;
        BigDecimal sumNet = BigDecimal.ZERO;
        List<AttestationRasDTO> list = new ArrayList<>();

        for (ReglementFournisseur r : reglements) {
            if (r.getFactureAchat() != null) {
                try {
                    // Par défaut prestations avec ARF (75%) ou selon notes
                    boolean hasArf = r.getNotes() == null || !r.getNotes().contains("SANS_ARF");
                    TypeOperationRas op = (r.getNotes() != null && r.getNotes().contains("BIENS"))
                            ? TypeOperationRas.BIENS_EQUIPEMENT_TRAVAUX
                            : TypeOperationRas.PRESTATIONS_SERVICES;

                    AttestationRasDTO att = genererAttestationDepuisReglement(r.getId(), op, hasArf);
                    list.add(att);

                    sumHt = sumHt.add(att.getMontantHt() != null ? att.getMontantHt() : BigDecimal.ZERO);
                    sumTva = sumTva.add(att.getMontantTva() != null ? att.getMontantTva() : BigDecimal.ZERO);
                    sumRas = sumRas.add(att.getMontantRas() != null ? att.getMontantRas() : BigDecimal.ZERO);
                    sumNet = sumNet.add(att.getMontantNetPaye() != null ? att.getMontantNetPaye() : BigDecimal.ZERO);
                } catch (Exception ignored) {
                }
            }
        }

        declaration.setAttestations(list);
        declaration.setNombreRetenues(list.size());
        declaration.setTotalHt(sumHt);
        declaration.setTotalTva(sumTva);
        declaration.setTotalRas(sumRas);
        declaration.setTotalNetPaye(sumNet);
        // Ligne 138 de la déclaration SIMPL-TVA
        declaration.setLigne138SimplTva(sumRas);

        return declaration;
    }

    // =========================================================================
    // 4. RENDU IMPRIMABLE HTML A4 DE L'ATTESTATION OFFICIELLE
    // =========================================================================

    public String exporterAttestationHtml(Long reglementId, TypeOperationRas typeOp, Boolean attestationPresente) {
        AttestationRasDTO att = genererAttestationDepuisReglement(reglementId, typeOp, attestationPresente);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String dateAttStr = att.getDateAttestation() != null ? att.getDateAttestation().format(fmt) : "";
        String dateFactStr = att.getFactureDate() != null ? att.getFactureDate().format(fmt) : "";
        String datePaiementStr = att.getDatePaiement() != null ? att.getDatePaiement().format(fmt) : "";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"fr\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>Attestation de Retenue à la Source - " + att.getNumeroAttestation() + "</title>\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 30px; color: #1e293b; background: #fff; }\n" +
                "    .header { border-bottom: 2px solid #0f172a; padding-bottom: 15px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: flex-start; }\n" +
                "    .title-box { text-align: center; margin: 25px 0; background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 8px; padding: 15px; }\n" +
                "    .title-box h1 { margin: 0 0 5px 0; font-size: 20px; color: #0f172a; text-transform: uppercase; }\n" +
                "    .title-box p { margin: 0; font-size: 13px; color: #64748b; font-weight: 500; }\n" +
                "    .parties { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 25px; }\n" +
                "    .party-card { border: 1px solid #e2e8f0; border-radius: 6px; padding: 14px; background: #fafafa; }\n" +
                "    .party-card h3 { margin: 0 0 10px 0; font-size: 14px; text-transform: uppercase; color: #0284c7; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; }\n" +
                "    .party-card p { margin: 3px 0; font-size: 12px; }\n" +
                "    table { width: 100%; border-collapse: collapse; margin-bottom: 25px; font-size: 13px; }\n" +
                "    th, td { border: 1px solid #cbd5e1; padding: 10px; text-align: left; }\n" +
                "    th { background: #f1f5f9; color: #334155; font-weight: 600; text-transform: uppercase; font-size: 11px; }\n" +
                "    .text-right { text-align: right; }\n" +
                "    .text-center { text-align: center; }\n" +
                "    .highlight-row { background: #eff6ff; font-weight: bold; color: #1e3a8a; }\n" +
                "    .legal-notice { font-size: 11px; color: #475569; background: #f8fafc; border-left: 4px solid #0284c7; padding: 10px; margin-bottom: 35px; }\n" +
                "    .signatures { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-top: 30px; page-break-inside: avoid; }\n" +
                "    .sig-box { border: 1px dashed #94a3b8; border-radius: 6px; height: 110px; padding: 10px; text-align: center; }\n" +
                "    .sig-box span { font-size: 11px; color: #64748b; font-weight: 600; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"header\">\n" +
                "    <div>\n" +
                "      <h2 style=\"margin:0; color:#0f172a;\">" + (att.getSocieteRaisonSociale() != null ? att.getSocieteRaisonSociale() : "SOCIÉTÉ") + "</h2>\n" +
                "      <p style=\"margin:3px 0; font-size:12px; color:#64748b;\">" + (att.getSocieteAdresse() != null ? att.getSocieteAdresse() : "") + " " + (att.getSocieteVille() != null ? att.getSocieteVille() : "") + "</p>\n" +
                "      <p style=\"margin:3px 0; font-size:12px; color:#64748b;\">IF : " + (att.getSocieteIf() != null ? att.getSocieteIf() : "-") + " | ICE : " + (att.getSocieteIce() != null ? att.getSocieteIce() : "-") + " | RC : " + (att.getSocieteRc() != null ? att.getSocieteRc() : "-") + "</p>\n" +
                "    </div>\n" +
                "    <div style=\"text-align:right;\">\n" +
                "      <p style=\"margin:0; font-size:12px; font-weight:bold; color:#0f172a;\">RÉFÉRENCE OFFICIELLE</p>\n" +
                "      <p style=\"margin:3px 0; font-size:14px; color:#0284c7; font-weight:bold;\">" + att.getNumeroAttestation() + "</p>\n" +
                "      <p style=\"margin:3px 0; font-size:12px; color:#64748b;\">Date : " + dateAttStr + "</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <div class=\"title-box\">\n" +
                "    <h1>ATTESTATION DE RETENUE À LA SOURCE (TVA)</h1>\n" +
                "    <p>Conforme aux dispositions de l'Article 117-VI du Code Général des Impôts (CGI Maroc)</p>\n" +
                "  </div>\n" +
                "\n" +
                "  <div class=\"parties\">\n" +
                "    <div class=\"party-card\">\n" +
                "      <h3>L'Acheteur / Le Déclarant (Client)</h3>\n" +
                "      <p><strong>Raison Sociale :</strong> " + (att.getSocieteRaisonSociale() != null ? att.getSocieteRaisonSociale() : "-") + "</p>\n" +
                "      <p><strong>ICE :</strong> " + (att.getSocieteIce() != null ? att.getSocieteIce() : "-") + "</p>\n" +
                "      <p><strong>Identifiant Fiscal (IF) :</strong> " + (att.getSocieteIf() != null ? att.getSocieteIf() : "-") + "</p>\n" +
                "      <p><strong>Adresse :</strong> " + (att.getSocieteAdresse() != null ? att.getSocieteAdresse() : "-") + "</p>\n" +
                "    </div>\n" +
                "    <div class=\"party-card\">\n" +
                "      <h3>Le Fournisseur / Bénéficiaire</h3>\n" +
                "      <p><strong>Raison Sociale :</strong> " + (att.getFournisseurNom() != null ? att.getFournisseurNom() : "-") + "</p>\n" +
                "      <p><strong>ICE :</strong> " + (att.getFournisseurIce() != null ? att.getFournisseurIce() : "-") + "</p>\n" +
                "      <p><strong>Identifiant Fiscal (IF) :</strong> " + (att.getFournisseurIf() != null ? att.getFournisseurIf() : "-") + "</p>\n" +
                "      <p><strong>Adresse :</strong> " + (att.getFournisseurAdresse() != null ? att.getFournisseurAdresse() : "-") + "</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <table>\n" +
                "    <thead>\n" +
                "      <tr>\n" +
                "        <th>Facture Réf / Date</th>\n" +
                "        <th>Nature Opération</th>\n" +
                "        <th class=\"text-right\">Montant HT</th>\n" +
                "        <th class=\"text-center\">Taux TVA</th>\n" +
                "        <th class=\"text-right\">Montant TVA</th>\n" +
                "        <th class=\"text-center\">Taux RAS</th>\n" +
                "        <th class=\"text-right\">TVA Retenue (RAS)</th>\n" +
                "        <th class=\"text-right\">Net Versé</th>\n" +
                "      </tr>\n" +
                "    </thead>\n" +
                "    <tbody>\n" +
                "      <tr>\n" +
                "        <td>" + (att.getFactureNumero() != null ? att.getFactureNumero() : "-") + "<br><small style=\"color:#64748b;\">" + dateFactStr + "</small></td>\n" +
                "        <td>" + (att.getTypeOperation() != null ? att.getTypeOperation().name() : "PRESTATION") + "</td>\n" +
                "        <td class=\"text-right\">" + String.format(Locale.FRANCE, "%,.2f", att.getMontantHt()) + " MAD</td>\n" +
                "        <td class=\"text-center\">" + att.getTauxTva() + " %</td>\n" +
                "        <td class=\"text-right\">" + String.format(Locale.FRANCE, "%,.2f", att.getMontantTva()) + " MAD</td>\n" +
                "        <td class=\"text-center\"><strong style=\"color:#b91c1c;\">" + att.getTauxRas() + " %</strong></td>\n" +
                "        <td class=\"text-right\"><strong style=\"color:#b91c1c;\">" + String.format(Locale.FRANCE, "%,.2f", att.getMontantRas()) + " MAD</strong></td>\n" +
                "        <td class=\"text-right\"><strong>" + String.format(Locale.FRANCE, "%,.2f", att.getMontantNetPaye()) + " MAD</strong></td>\n" +
                "      </tr>\n" +
                "      <tr class=\"highlight-row\">\n" +
                "        <td colspan=\"6\"><strong>MONTANT TOTAL VERSÉ AU TRÉSOR PUBLIC (LIGNE 138 SIMPL-TVA)</strong></td>\n" +
                "        <td class=\"text-right\"><strong>" + String.format(Locale.FRANCE, "%,.2f", att.getMontantRas()) + " MAD</strong></td>\n" +
                "        <td class=\"text-right\"><strong>" + String.format(Locale.FRANCE, "%,.2f", att.getMontantNetPaye()) + " MAD</strong></td>\n" +
                "      </tr>\n" +
                "    </tbody>\n" +
                "  </table>\n" +
                "\n" +
                "  <div class=\"legal-notice\">\n" +
                "    <strong>Justification légale et fiscale :</strong> " + (att.getMentionLegale() != null ? att.getMentionLegale() : "") + "<br>\n" +
                "    Règlement opéré le <strong>" + datePaiementStr + "</strong> par <strong>" + att.getModePaiement() + "</strong> " + (att.getReferencePaiement() != null ? "(Réf : " + att.getReferencePaiement() + ")" : "") + ".\n" +
                "    Le montant de la TVA retenue à la source est versé directement au Trésor par le déclarant au titre de la déclaration de TVA de la période correspondante.\n" +
                "  </div>\n" +
                "\n" +
                "  <div class=\"signatures\">\n" +
                "    <div class=\"sig-box\">\n" +
                "      <span>Cachet & Signature de l'Acheteur</span>\n" +
                "    </div>\n" +
                "    <div class=\"sig-box\">\n" +
                "      <span>Accusé de réception du Fournisseur</span>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    // =========================================================================
    // 5. EXPORT CSV DU BORDEREAU DE RETENUE À LA SOURCE
    // =========================================================================

    public byte[] exporterDeclarationCsv(Integer annee, Integer trimestre) {
        DeclarationRasPeriodeDTO decl = getDeclarationPeriode(annee, null, trimestre);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM UTF-8
        sb.append("NumeroAttestation;DateAttestation;FactureNumero;FactureDate;FournisseurNom;FournisseurICE;FournisseurIF;TypeOperation;MontantHT;TauxTVA;MontantTVA;TauxRAS;MontantRAS;MontantNetPaye\n");

        for (AttestationRasDTO a : decl.getAttestations()) {
            sb.append(escape(a.getNumeroAttestation())).append(";")
                    .append(a.getDateAttestation() != null ? a.getDateAttestation().toString() : "").append(";")
                    .append(escape(a.getFactureNumero())).append(";")
                    .append(a.getFactureDate() != null ? a.getFactureDate().toString() : "").append(";")
                    .append(escape(a.getFournisseurNom())).append(";")
                    .append(escape(a.getFournisseurIce())).append(";")
                    .append(escape(a.getFournisseurIf())).append(";")
                    .append(a.getTypeOperation() != null ? a.getTypeOperation().name() : "").append(";")
                    .append(a.getMontantHt()).append(";")
                    .append(a.getTauxTva()).append(";")
                    .append(a.getMontantTva()).append(";")
                    .append(a.getTauxRas()).append(";")
                    .append(a.getMontantRas()).append(";")
                    .append(a.getMontantNetPaye()).append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // =========================================================================
    // 6. COMPTABILISATION AUTOMATIQUE DU RÈGLEMENT AVEC SCISSION DE LA RAS
    // =========================================================================

    public EcritureComptableDTO comptabiliserReglementAvecRas(ComptabiliserRasRequest req) {
        if (req == null || req.getReglementId() == null) {
            throw new IllegalArgumentException("L'identifiant du règlement est obligatoire.");
        }

        Long tenantId = getTenantId();
        ReglementFournisseur reglement = reglementRepository.findByIdAndPointDeVenteId(req.getReglementId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Règlement introuvable : " + req.getReglementId()));

        FactureAchat facture = reglement.getFactureAchat();
        if (facture == null) {
            throw new IllegalArgumentException("Le règlement " + req.getReglementId() + " n'est rattaché à aucune facture.");
        }

        AttestationRasDTO att = genererAttestationDepuisReglement(req.getReglementId(), TypeOperationRas.PRESTATIONS_SERVICES, true);

        // Journal de trésorerie (BQ ou OD)
        String journalCode = req.getJournalCode() != null ? req.getJournalCode().trim().toUpperCase() : "BQ";
        JournalComptable journal = journalRepository.findByCodeAndPointDeVenteId(journalCode, tenantId)
                .orElseGet(() -> {
                    JournalComptable j = new JournalComptable();
                    j.setCode(journalCode);
                    j.setLibelle("Journal " + journalCode);
                    j.setTypeJournal("BQ".equalsIgnoreCase(journalCode) ? TypeJournal.BANQUE : TypeJournal.OPERATIONS_DIVERSES);
                    j.setActif(true);
                    j.setPointDeVenteId(tenantId);
                    return journalRepository.save(j);
                });

        CompteComptable compteFournisseur = resoudreOuCreerCompte("44110000", "Fournisseurs", 4, SensCompte.CREDIT, tenantId);
        String compteTrCode = req.getCompteTresorerie() != null ? req.getCompteTresorerie().trim() : "51410000";
        CompteComptable compteTresorerie = resoudreOuCreerCompte(compteTrCode, "Banque", 5, SensCompte.DEBIT, tenantId);
        String compteRasCode = req.getCompteRas() != null ? req.getCompteRas().trim() : "44580000";
        CompteComptable compteRas = resoudreOuCreerCompte(compteRasCode, "État, TVA retenue à la source", 4, SensCompte.CREDIT, tenantId);

        LocalDate dateEcriture = req.getDateEcriture() != null
                ? req.getDateEcriture()
                : (reglement.getDateReglement() != null ? reglement.getDateReglement().toLocalDate() : LocalDate.now());

        String libellePiece = req.getLibelle() != null
                ? req.getLibelle()
                : "Règlement Fact " + facture.getNumeroFacture() + " avec RAS TVA (" + att.getNumeroAttestation() + ")";

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journal);
        ecriture.setDateEcriture(dateEcriture);
        ecriture.setLibelle(libellePiece);
        ecriture.setNumeroPiece(reglement.getNumeroReglement() != null ? reglement.getNumeroReglement() : "REG-" + reglement.getId());
        ecriture.setReferencePiece(facture.getNumeroFacture());
        ecriture.setValidee(true);
        ecriture.setPointDeVenteId(tenantId);

        // Ligne 1 : Débit 4411 (Total facture ou montant réglé TTC)
        LigneEcriture l1 = new LigneEcriture();
        l1.setEcriture(ecriture);
        l1.setCompte(compteFournisseur);
        l1.setDebit(att.getMontantTtc());
        l1.setCredit(BigDecimal.ZERO);
        l1.setLibelleLigne(libellePiece);
        l1.setReferenceLigne(facture.getNumeroFacture());
        l1.setPointDeVenteId(tenantId);
        ecriture.getLignes().add(l1);

        // Ligne 2 : Crédit 5141 (Montant Net versé au fournisseur)
        LigneEcriture l2 = new LigneEcriture();
        l2.setEcriture(ecriture);
        l2.setCompte(compteTresorerie);
        l2.setDebit(BigDecimal.ZERO);
        l2.setCredit(att.getMontantNetPaye());
        l2.setLibelleLigne("Net versé fournisseur " + (att.getFournisseurNom() != null ? att.getFournisseurNom() : ""));
        l2.setReferenceLigne(reglement.getReferencePaiement());
        l2.setPointDeVenteId(tenantId);
        ecriture.getLignes().add(l2);

        // Ligne 3 : Crédit 4458 (Montant de la Retenue à la Source TVA à verser à l'État)
        if (att.getMontantRas() != null && att.getMontantRas().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcriture l3 = new LigneEcriture();
            l3.setEcriture(ecriture);
            l3.setCompte(compteRas);
            l3.setDebit(BigDecimal.ZERO);
            l3.setCredit(att.getMontantRas());
            l3.setLibelleLigne("RAS TVA " + att.getTauxRas() + "% à reverser Trésor (" + att.getNumeroAttestation() + ")");
            l3.setReferenceLigne(att.getNumeroAttestation());
            l3.setPointDeVenteId(tenantId);
            ecriture.getLignes().add(l3);
        }

        ecriture.setTotalDebit(att.getMontantTtc());
        ecriture.setTotalCredit(att.getMontantNetPaye().add(att.getMontantRas()));

        EcritureComptable saved = ecritureRepository.save(ecriture);
        return comptabiliteService.getEcritureById(saved.getId());
    }

    private CompteComptable resoudreOuCreerCompte(String num, String libelle, Integer classe, SensCompte sens, Long tenantId) {
        return compteRepository.findByNumeroCompteAndPointDeVenteId(num, tenantId)
                .orElseGet(() -> {
                    CompteComptable c = new CompteComptable();
                    c.setNumeroCompte(num);
                    c.setLibelle(libelle);
                    c.setClasse(classe != null ? classe : 4);
                    c.setSensParDefaut(sens != null ? sens : SensCompte.CREDIT);
                    c.setActif(true);
                    c.setPointDeVenteId(tenantId);
                    return compteRepository.save(c);
                });
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace(";", ",");
    }
}
