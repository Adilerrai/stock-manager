package com.gestion.controller;

import com.gestion.service.EdiFiscalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/fiscalite/edi")
@CrossOrigin(origins = "*")
public class EdiFiscalController {

    private final EdiFiscalService ediService;

    public EdiFiscalController(EdiFiscalService ediService) {
        this.ediService = ediService;
    }

    @GetMapping(value = "/simpl-tva/{declarationId}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> telechargerSimplTvaXml(@PathVariable Long declarationId) {
        byte[] xmlData = ediService.genererXmlSimplTva(declarationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SIMPL_TVA_" + declarationId + ".xml\"")
                .body(xmlData);
    }

    @GetMapping(value = "/simpl-is/{annee}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> telechargerSimplIsXml(
            @PathVariable int annee,
            @RequestParam(required = false, defaultValue = "0") BigDecimal reintegrations,
            @RequestParam(required = false, defaultValue = "0") BigDecimal deductions) {

        byte[] xmlData = ediService.genererXmlSimplIs(annee, reintegrations, deductions);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SIMPL_IS_" + annee + ".xml\"")
                .body(xmlData);
    }

    @GetMapping(value = "/simpl-delais-paiement/{annee}/{trimestre}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> telechargerSimplDelaisPaiementXml(
            @PathVariable int annee,
            @PathVariable int trimestre) {

        byte[] xmlData = ediService.genererXmlSimplDelaisPaiement(annee, trimestre);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SIMPL_DELAIS_PAIEMENT_" + annee + "_T" + trimestre + ".xml\"")
                .body(xmlData);
    }

    @GetMapping(value = "/simpl-ir/{annee}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> telechargerSimplIrXml(@PathVariable int annee) {
        byte[] xmlData = ediService.genererXmlSimplIr(annee);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SIMPL_IR_ETAT_9421_" + annee + ".xml\"")
                .body(xmlData);
    }
}
