package com.gestion.controller;

import com.gestion.persistent.dto.DashboardDelaisPaiementDTO;
import com.gestion.persistent.dto.DeclarationDelaisPaiementDTO;
import com.gestion.service.DelaisPaiementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/delais-paiement")
@CrossOrigin(origins = "*")
public class DelaisPaiementController {

    private final DelaisPaiementService delaisPaiementService;

    public DelaisPaiementController(DelaisPaiementService delaisPaiementService) {
        this.delaisPaiementService = delaisPaiementService;
    }

    /**
     * Consultation de la déclaration trimestrielle des Délais de Paiement (Loi 69-21)
     */
    @GetMapping("/declaration")
    public ResponseEntity<DeclarationDelaisPaiementDTO> getDeclarationTrimestrielle(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre) {

        int an = (annee != null) ? annee : LocalDate.now().getYear();
        int tri = (trimestre != null) ? trimestre : ((LocalDate.now().getMonthValue() - 1) / 3 + 1);

        return ResponseEntity.ok(delaisPaiementService.calculerDeclarationTrimestrielle(an, tri));
    }

    /**
     * Tableau de bord prédictif : alertes factures à risque 15 jours, retards et simulation amendes
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDelaisPaiementDTO> getDashboardPrevisionnel() {
        return ResponseEntity.ok(delaisPaiementService.getDashboardPrevisionnel());
    }

    /**
     * Téléversement DGI : Export XML normalisé SIMPL-Délais de Paiement (EDI DGI)
     */
    @GetMapping(value = "/export/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> exporterXmlSimpl(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre) {

        int an = (annee != null) ? annee : LocalDate.now().getYear();
        int tri = (trimestre != null) ? trimestre : ((LocalDate.now().getMonthValue() - 1) / 3 + 1);

        byte[] xmlData = delaisPaiementService.genererXmlSimplDelaisPaiement(an, tri);
        String filename = "SIMPL_DELAIS_PAIEMENT_" + an + "_T" + tri + ".xml";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(xmlData);
    }

    /**
     * Téléchargement CSV normalisé pour import direct portail DGI ou contrôle fiduciaire
     */
    @GetMapping(value = "/export/csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exporterCsvSimpl(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre) {

        int an = (annee != null) ? annee : LocalDate.now().getYear();
        int tri = (trimestre != null) ? trimestre : ((LocalDate.now().getMonthValue() - 1) / 3 + 1);

        byte[] csvData = delaisPaiementService.genererCsvSimplDelaisPaiement(an, tri);
        String filename = "SIMPL_DELAIS_PAIEMENT_" + an + "_T" + tri + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvData);
    }
}
