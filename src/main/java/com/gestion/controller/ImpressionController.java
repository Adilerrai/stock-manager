package com.gestion.controller;

import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import com.gestion.service.ImpressionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/impressions")
@CrossOrigin(origins = "*")
public class ImpressionController {

    private final ImpressionService impressionService;
    private final FactureRepository factureRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final DevisRepository devisRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final CommandeRepository commandeRepository;
    private final AvoirRepository avoirRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final LivraisonRepository livraisonRepository;
    private final FactureAchatRepository factureAchatRepository;

    public ImpressionController(ImpressionService impressionService,
                                FactureRepository factureRepository,
                                BonLivraisonClientRepository bonLivraisonClientRepository,
                                DevisRepository devisRepository,
                                CommandeClientRepository commandeClientRepository,
                                CommandeRepository commandeRepository,
                                AvoirRepository avoirRepository,
                                ClientRepository clientRepository,
                                FournisseurRepository fournisseurRepository,
                                LivraisonRepository livraisonRepository,
                                FactureAchatRepository factureAchatRepository) {
        this.impressionService = impressionService;
        this.factureRepository = factureRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.devisRepository = devisRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.commandeRepository = commandeRepository;
        this.avoirRepository = avoirRepository;
        this.clientRepository = clientRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.livraisonRepository = livraisonRepository;
        this.factureAchatRepository = factureAchatRepository;
    }

    @GetMapping("/factures/{id}")
    public ResponseEntity<byte[]> imprimerFacture(@PathVariable Long id) {
        Facture f = factureRepository.findById(id).orElse(null);
        String rs = (f != null && f.getClient() != null) ? f.getClient().getNomComplet() : "";
        String code = (f != null && f.getNumeroFacture() != null) ? f.getNumeroFacture() : "FACT-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererFacturePdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/bons-livraison/{id}")
    public ResponseEntity<byte[]> imprimerBonLivraison(@PathVariable Long id) {
        BonLivraisonClient bl = bonLivraisonClientRepository.findById(id).orElse(null);
        String rs = (bl != null && bl.getClient() != null) ? bl.getClient().getNomComplet() : "";
        String code = (bl != null && bl.getNumeroBl() != null) ? bl.getNumeroBl() : "BL-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererBonLivraisonPdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/devis/{id}")
    public ResponseEntity<byte[]> imprimerDevis(@PathVariable Long id) {
        Devis devis = devisRepository.findById(id).orElse(null);
        String rs = (devis != null && devis.getClient() != null) ? devis.getClient().getNomComplet() : "";
        String code = (devis != null && devis.getNumeroDevis() != null) ? devis.getNumeroDevis() : "DEV-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererDevisPdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/commandes-client/{id}")
    public ResponseEntity<byte[]> imprimerCommandeClient(@PathVariable Long id) {
        CommandeClient cmd = commandeClientRepository.findById(id).orElse(null);
        String rs = (cmd != null && cmd.getClient() != null) ? cmd.getClient().getNomComplet() : "";
        String code = (cmd != null && cmd.getNumeroCommande() != null) ? cmd.getNumeroCommande() : "CMD-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererCommandeClientPdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/commandes-fournisseur/{id}")
    public ResponseEntity<byte[]> imprimerCommandeFournisseur(@PathVariable Long id) {
        Commande cmd = commandeRepository.findById(id).orElse(null);
        String rs = (cmd != null && cmd.getFournisseur() != null) ? cmd.getFournisseur().getNom() : "";
        String code = (cmd != null && cmd.getNumeroCommande() != null) ? cmd.getNumeroCommande() : "CMDFOUR-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererCommandeFournisseurPdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/bons-reception/{id}")
    public ResponseEntity<byte[]> imprimerBonReception(@PathVariable Long id) {
        Livraison liv = livraisonRepository.findById(id).orElse(null);
        String rs = (liv != null && liv.getCommande() != null && liv.getCommande().getFournisseur() != null)
                ? (liv.getCommande().getFournisseur().getRaisonSociale() != null ? liv.getCommande().getFournisseur().getRaisonSociale() : liv.getCommande().getFournisseur().getNom())
                : "";
        String code = (liv != null && liv.getNumeroLivraison() != null) ? liv.getNumeroLivraison() : "BR-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererBonReceptionPdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/factures-achat/{id}")
    public ResponseEntity<byte[]> imprimerFactureAchat(@PathVariable Long id) {
        FactureAchat f = factureAchatRepository.findById(id).orElse(null);
        String rs = (f != null && f.getFournisseur() != null)
                ? (f.getFournisseur().getRaisonSociale() != null ? f.getFournisseur().getRaisonSociale() : f.getFournisseur().getNom())
                : "";
        String code = (f != null && f.getNumeroFacture() != null) ? f.getNumeroFacture() : "FA-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererFactureAchatPdf(id);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/avoirs/{id}")
    public ResponseEntity<byte[]> imprimerAvoir(@PathVariable Long id) {
        Avoir avoir = avoirRepository.findById(id).orElse(null);
        String rs = (avoir != null && avoir.getClient() != null) ? avoir.getClient().getNomComplet() : "";
        String code = (avoir != null && avoir.getNumeroAvoir() != null) ? avoir.getNumeroAvoir() : "AVR-" + id;
        String fileName = buildPdfFileName(code, rs);

        byte[] pdf = impressionService.genererAvoirPdf(id);
        return createPdfResponse(pdf, fileName);
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
        Client client = clientRepository.findById(clientId).orElse(null);
        String rs = client != null ? client.getNomComplet() : "";
        String fileName = buildPdfFileName("RELEVE-" + clientId, rs);

        byte[] pdf = impressionService.genererReleveClientPdf(clientId, dateDebut, dateFin);
        return createPdfResponse(pdf, fileName);
    }

    @GetMapping("/releve-fournisseur/{fournisseurId}")
    public ResponseEntity<byte[]> imprimerReleveFournisseur(
            @PathVariable Long fournisseurId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateDebut,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFin) {
        Fournisseur f = fournisseurRepository.findById(fournisseurId).orElse(null);
        String rs = f != null ? (f.getRaisonSociale() != null ? f.getRaisonSociale() : f.getNom()) : "";
        String fileName = buildPdfFileName("RELEVE-FOUR-" + fournisseurId, rs);

        byte[] pdf = impressionService.genererReleveFournisseurPdf(fournisseurId, dateDebut, dateFin);
        return createPdfResponse(pdf, fileName);
    }

    public static String buildPdfFileName(String code, String raisonSociale) {
        String baseCode = (code != null && !code.isBlank()) ? code.trim() : "DOCUMENT";
        baseCode = baseCode.replaceAll("[^a-zA-Z0-9-_]", "_");

        if (raisonSociale == null || raisonSociale.isBlank()) {
            return baseCode + ".pdf";
        }

        String cleanRs = raisonSociale.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "_");

        return baseCode + "-" + cleanRs + ".pdf";
    }

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfData, String fileName) {
        String cleanAscii = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String encodedFilename = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + cleanAscii + "\"; filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("X-Filename", fileName)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdfData);
    }
}
