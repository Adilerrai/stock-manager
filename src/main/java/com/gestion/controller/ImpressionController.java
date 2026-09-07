package com.gestion.controller;

import com.gestion.service.ImpressionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/impressions")
@CrossOrigin(origins = "*")
public class ImpressionController {

    private final ImpressionService impressionService;

    public ImpressionController(ImpressionService impressionService) {
        this.impressionService = impressionService;
    }

    @GetMapping("/factures/{id}")
    public ResponseEntity<byte[]> imprimerFacture(@PathVariable Long id) {
        byte[] pdf = impressionService.genererFacturePdf(id);
        return createPdfResponse(pdf, "facture-" + id + ".pdf");
    }

    @GetMapping("/bons-livraison/{id}")
    public ResponseEntity<byte[]> imprimerBonLivraison(@PathVariable Long id) {
        byte[] pdf = impressionService.genererBonLivraisonPdf(id);
        return createPdfResponse(pdf, "bon-livraison-" + id + ".pdf");
    }

    @GetMapping("/devis/{id}")
    public ResponseEntity<byte[]> imprimerDevis(@PathVariable Long id) {
        byte[] pdf = impressionService.genererDevisPdf(id);
        return createPdfResponse(pdf, "devis-" + id + ".pdf");
    }

    @GetMapping("/commandes-client/{id}")
    public ResponseEntity<byte[]> imprimerCommandeClient(@PathVariable Long id) {
        byte[] pdf = impressionService.genererCommandeClientPdf(id);
        return createPdfResponse(pdf, "commande-client-" + id + ".pdf");
    }

    @GetMapping("/commandes-fournisseur/{id}")
    public ResponseEntity<byte[]> imprimerCommandeFournisseur(@PathVariable Long id) {
        byte[] pdf = impressionService.genererCommandeFournisseurPdf(id);
        return createPdfResponse(pdf, "commande-fournisseur-" + id + ".pdf");
    }

    @GetMapping("/avoirs/{id}")
    public ResponseEntity<byte[]> imprimerAvoir(@PathVariable Long id) {
        byte[] pdf = impressionService.genererAvoirPdf(id);
        return createPdfResponse(pdf, "avoir-" + id + ".pdf");
    }

    @GetMapping("/ventes/{id}/ticket")
    public ResponseEntity<byte[]> imprimerTicketVente(@PathVariable Long id) {
        byte[] pdf = impressionService.genererTicketVentePdf(id);
        return createPdfResponse(pdf, "ticket-vente-" + id + ".pdf");
    }

    @GetMapping("/bordereaux-remise/{id}")
    public ResponseEntity<byte[]> imprimerBordereauRemise(@PathVariable Long id) {
        byte[] pdf = impressionService.genererBordereauRemisePdf(id);
        return createPdfResponse(pdf, "bordereau-remise-" + id + ".pdf");
    }

    @GetMapping("/releve-client/{clientId}")
    public ResponseEntity<byte[]> imprimerReleveClient(
            @PathVariable Long clientId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateDebut,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFin) {
        byte[] pdf = impressionService.genererReleveClientPdf(clientId, dateDebut, dateFin);
        return createPdfResponse(pdf, "releve-client-" + clientId + ".pdf");
    }

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfData, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdfData);
    }
}
