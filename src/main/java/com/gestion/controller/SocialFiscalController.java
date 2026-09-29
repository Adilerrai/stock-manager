package com.gestion.controller;

import com.gestion.persistent.dto.BordereauDamancomDTO;
import com.gestion.persistent.dto.DeclarationEtat9421DTO;
import com.gestion.persistent.dto.EcritureComptableDTO;
import com.gestion.persistent.dto.OdPaieGenerationRequest;
import com.gestion.service.SocialFiscalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/social")
@CrossOrigin(origins = "*")
public class SocialFiscalController {

    private final SocialFiscalService socialFiscalService;

    public SocialFiscalController(SocialFiscalService socialFiscalService) {
        this.socialFiscalService = socialFiscalService;
    }

    /**
     * Consultation de l'État 9421 (Déclaration Annuelle des Salaires & Rémunérations - Art. 79 CGI)
     */
    @GetMapping("/etat-9421")
    public ResponseEntity<DeclarationEtat9421DTO> getEtat9421(
            @RequestParam(required = false) Integer annee) {
        int an = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(socialFiscalService.calculerEtat9421(an));
    }

    /**
     * Télédéclaration DGI : Téléchargement XML certifié SIMPL-IR
     */
    @GetMapping(value = "/etat-9421/export/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> exporterSimplIrXml(
            @RequestParam(required = false) Integer annee) {
        int an = (annee != null) ? annee : LocalDate.now().getYear();
        byte[] xmlData = socialFiscalService.genererXmlSimplIr(an);
        String filename = "SIMPL_IR_ETAT_9421_" + an + ".xml";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(xmlData);
    }

    /**
     * Export CSV de l'État 9421 pour archivage et contrôle
     */
    @GetMapping(value = "/etat-9421/export/csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exporterEtat9421Csv(
            @RequestParam(required = false) Integer annee) {
        int an = (annee != null) ? annee : LocalDate.now().getYear();
        byte[] csvData = socialFiscalService.genererCsvEtat9421(an);
        String filename = "ETAT_9421_" + an + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvData);
    }

    /**
     * Consultation du Bordereau de Déclaration des Salaires CNSS (BDS Damancom)
     */
    @GetMapping("/damancom/bds")
    public ResponseEntity<BordereauDamancomDTO> getBordereauDamancom(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois) {
        int an = (annee != null) ? annee : LocalDate.now().getYear();
        int m = (mois != null) ? mois : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(socialFiscalService.genererBordereauBdsDamancom(an, m));
    }

    /**
     * Téléchargement du fichier texte normalisé BDS Damancom pour téléversement CNSS direct
     */
    @GetMapping(value = "/damancom/bds/export-txt", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<byte[]> exporterBdsDamancomTxt(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois) {
        int an = (annee != null) ? annee : LocalDate.now().getYear();
        int m = (mois != null) ? mois : LocalDate.now().getMonthValue();
        byte[] txtData = socialFiscalService.genererFichierTxtBdsDamancom(an, m);
        String filename = String.format("BDS_DAMANCOM_%04d_%02d.txt", an, m);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .body(txtData);
    }

    /**
     * Génération automatique de l'écriture comptable d'OD de Paie mensuelle (Journal OD ou PAIE)
     */
    @PostMapping("/od-paie/generer")
    public ResponseEntity<EcritureComptableDTO> genererOdPaie(@RequestBody OdPaieGenerationRequest request) {
        return new ResponseEntity<>(socialFiscalService.genererEcritureOdPaie(request), HttpStatus.CREATED);
    }
}
