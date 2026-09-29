package com.gestion.controller;

import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.TypeOperationRas;
import com.gestion.service.RasTvaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion de la Retenue à la Source (RAS) sur TVA,
 * Loyers et Honoraires (CGI Maroc - Art. 117-VI, 89-I et 157).
 */
@RestController
@RequestMapping("/api/fiscalite/ras-tva")
@CrossOrigin(origins = "*")
public class RasTvaController {

    private final RasTvaService rasTvaService;

    public RasTvaController(RasTvaService rasTvaService) {
        this.rasTvaService = rasTvaService;
    }

    /**
     * Simulation / calcul automatique de la retenue à la source selon le barème légal du CGI.
     */
    @PostMapping("/calculer")
    public ResponseEntity<RasCalculResultDTO> calculerRas(@RequestBody RasCalculRequest req) {
        return ResponseEntity.ok(rasTvaService.calculerRas(req));
    }

    /**
     * Génération de l'attestation officielle de retenue à la source pour un règlement fournisseur donné.
     */
    @PostMapping("/attestations/generer-depuis-reglement/{reglementId}")
    public ResponseEntity<AttestationRasDTO> genererAttestation(
            @PathVariable Long reglementId,
            @RequestParam(required = false) TypeOperationRas typeOperation,
            @RequestParam(required = false, defaultValue = "true") Boolean attestationPresente) {
        return ResponseEntity.ok(rasTvaService.genererAttestationDepuisReglement(reglementId, typeOperation, attestationPresente));
    }

    /**
     * Document officiel imprimable au format A4 (HTML) de l'attestation de retenue à la source.
     */
    @GetMapping(value = "/attestations/{reglementId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> imprimerAttestationHtml(
            @PathVariable Long reglementId,
            @RequestParam(required = false) TypeOperationRas typeOperation,
            @RequestParam(required = false, defaultValue = "true") Boolean attestationPresente) {
        String html = rasTvaService.exporterAttestationHtml(reglementId, typeOperation, attestationPresente);
        return ResponseEntity.ok(html);
    }

    /**
     * Bordereau récapitulatif périodique de RAS TVA (Montants déclarables en Ligne 138 de la déclaration SIMPL-TVA).
     */
    @GetMapping("/declaration")
    public ResponseEntity<DeclarationRasPeriodeDTO> getDeclaration(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) Integer trimestre) {
        return ResponseEntity.ok(rasTvaService.getDeclarationPeriode(annee, mois, trimestre));
    }

    /**
     * Export CSV officiel du bordereau périodique des retenues à la source.
     */
    @GetMapping("/declaration/export/csv")
    public ResponseEntity<byte[]> exporterDeclarationCsv(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre) {
        byte[] csv = rasTvaService.exporterDeclarationCsv(annee, trimestre);
        String filename = "declaration_ras_tva_" + (annee != null ? annee : "current") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    /**
     * Génération automatique de l'écriture comptable équilibrée de règlement scindé :
     * Débit 4411 (TTC) / Crédit 5141 (Net payé) / Crédit 4458 (TVA retenue à la source).
     */
    @PostMapping("/comptabiliser")
    public ResponseEntity<EcritureComptableDTO> comptabiliser(@RequestBody ComptabiliserRasRequest req) {
        return ResponseEntity.ok(rasTvaService.comptabiliserReglementAvecRas(req));
    }
}
