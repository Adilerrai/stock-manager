package com.gestion.controller;

import com.gestion.persistent.dto.*;
import com.gestion.service.EspaceClientPortalService;
import com.gestion.service.ImpressionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST dédié au Portail "Espace Client".
 * Permet au client connecté (TypeUtilisateur.CLIENT) de :
 * - Consulter son Accueil / Tableau de bord
 * - Parcourir le Catalogue Produit du Tenant (SANS PRIX et SANS DISPONIBILITÉ/STOCK) avec SearchCriteria
 * - Créer et suivre ses Réservations d'articles (Panier de réservation SANS PRIX NI STOCK) avec SearchCriteria
 * - Consulter ses Devis avec SearchCriteria et donner son accord (Accepter / Refuser)
 * - Consulter ses Bons de Commande avec SearchCriteria
 * - Consulter ses Bons de Livraison (BL) et suivre leur statut (EN_ATTENTE, EXPEDIE, LIVREE) avec SearchCriteria
 */
@RestController
@RequestMapping({"/api/espace-client", "/api/v1/espace-client"})
@CrossOrigin(origins = "*")
public class EspaceClientPortalController {

    private final EspaceClientPortalService espaceClientPortalService;
    private final ImpressionService impressionService;

    public EspaceClientPortalController(EspaceClientPortalService espaceClientPortalService,
                                        ImpressionService impressionService) {
        this.espaceClientPortalService = espaceClientPortalService;
        this.impressionService = impressionService;
    }

    // =========================================================================
    // 1. ACCUEIL / DASHBOARD ESPACE CLIENT
    // =========================================================================

    @GetMapping({"/accueil", "/dashboard"})
    public ResponseEntity<EspaceClientDashboardDTO> getDashboard(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getDashboard(clientId));
    }

    // =========================================================================
    // 2. CATALOGUE PRODUITS (SANS PRIX, SANS STOCK / DISPONIBILITÉ)
    // =========================================================================

    @GetMapping("/catalogue")
    public ResponseEntity<List<CatalogueProduitClientDTO>> getCatalogue(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getCatalogueProduits(search, categorieId, categorie, clientId));
    }

    @PostMapping("/catalogue/search")
    public ResponseEntity<Page<CatalogueProduitClientDTO>> searchCatalogue(
            @RequestBody(required = false) ProduitSearchCriteria criteria,
            Pageable pageable,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.searchCatalogueProduits(criteria, pageable, clientId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategorieDTO>> getCategoriesCatalogue(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getCategoriesCatalogue(clientId));
    }

    // =========================================================================
    // 3. RÉSERVATIONS D'ARTICLES (PANIER DE RÉSERVATION SANS PRIX NI STOCK)
    // =========================================================================

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationClientDTO>> getMesReservations(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMesReservations(clientId));
    }

    @PostMapping("/reservations/search")
    public ResponseEntity<Page<ReservationClientDTO>> searchMesReservations(
            @RequestBody(required = false) ReservationClientSearchCriteria criteria,
            Pageable pageable,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.searchMesReservations(criteria, pageable, clientId));
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<ReservationClientDTO> getMaReservationById(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMaReservationById(id, clientId));
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationClientDTO> creerMaReservation(
            @RequestBody ReservationClientDTO dto,
            @RequestParam(required = false) Long clientId) {
        ReservationClientDTO created = espaceClientPortalService.creerMaReservation(dto, clientId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/reservations/{id}/annuler")
    public ResponseEntity<ReservationClientDTO> annulerMaReservation(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.annulerMaReservation(id, clientId));
    }

    // =========================================================================
    // 4. DEVIS & ACCORD CLIENT (ACCEPTER / REFUSER)
    // =========================================================================

    @GetMapping("/devis")
    public ResponseEntity<List<DevisDTO>> getMesDevis(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMesDevis(clientId));
    }

    @PostMapping("/devis/search")
    public ResponseEntity<Page<DevisDTO>> searchMesDevis(
            @RequestBody(required = false) DevisSearchCriteria criteria,
            Pageable pageable,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.searchMesDevis(criteria, pageable, clientId));
    }

    @GetMapping("/devis/{id}")
    public ResponseEntity<DevisDTO> getMonDevisById(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMonDevisById(id, clientId));
    }

    @GetMapping("/devis/{id}/pdf")
    public ResponseEntity<byte[]> getMonDevisPdf(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        espaceClientPortalService.getMonDevisById(id, clientId);
        byte[] pdf = impressionService.genererDevisPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"devis-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/devis/{id}/accepter")
    public ResponseEntity<DevisDTO> accepterMonDevis(
            @PathVariable Long id,
            @RequestParam(required = false) String numeroAchatClient,
            @RequestParam(required = false) Long clientId,
            @RequestBody(required = false) Map<String, String> body) {
        String resolvedBcClient = numeroAchatClient;
        String commentaire = null;
        if (body != null) {
            if (resolvedBcClient == null) {
                resolvedBcClient = body.getOrDefault("numeroAchatClient", body.get("bcClient"));
            }
            commentaire = body.getOrDefault("commentaire", body.get("notes"));
        }
        return ResponseEntity.ok(espaceClientPortalService.accepterMonDevis(id, resolvedBcClient, commentaire, clientId));
    }

    @PostMapping("/devis/{id}/refuser")
    public ResponseEntity<DevisDTO> refuserMonDevis(
            @PathVariable Long id,
            @RequestParam(required = false) String motifRefus,
            @RequestParam(required = false) Long clientId,
            @RequestBody(required = false) Map<String, String> body) {
        String resolvedMotif = motifRefus;
        if (resolvedMotif == null && body != null) {
            resolvedMotif = body.getOrDefault("motifRefus", body.getOrDefault("motif", body.get("commentaire")));
        }
        return ResponseEntity.ok(espaceClientPortalService.refuserMonDevis(id, resolvedMotif, clientId));
    }

    // =========================================================================
    // 5. BONS DE COMMANDE DU CLIENT
    // =========================================================================

    @GetMapping("/commandes")
    public ResponseEntity<List<CommandeClientDTO>> getMesCommandes(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMesCommandes(clientId));
    }

    @PostMapping("/commandes/search")
    public ResponseEntity<Page<CommandeClientDTO>> searchMesCommandes(
            @RequestBody(required = false) CommandeClientSearchCriteria criteria,
            Pageable pageable,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.searchMesCommandes(criteria, pageable, clientId));
    }

    @GetMapping("/commandes/{id}")
    public ResponseEntity<CommandeClientDTO> getMaCommandeById(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMaCommandeById(id, clientId));
    }

    @GetMapping("/commandes/{id}/pdf")
    public ResponseEntity<byte[]> getMaCommandePdf(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        espaceClientPortalService.getMaCommandeById(id, clientId);
        byte[] pdf = impressionService.genererCommandeClientPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"commande-" + id + ".pdf\"")
                .body(pdf);
    }

    // =========================================================================
    // 6. BONS DE LIVRAISON (BL) DU CLIENT (STATUT EXPEDIE / LIVREE)
    // =========================================================================

    @GetMapping("/bons-livraison")
    public ResponseEntity<List<BonLivraisonClientDTO>> getMesBonsLivraison(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMesBonsLivraison(clientId));
    }

    @PostMapping("/bons-livraison/search")
    public ResponseEntity<Page<BonLivraisonClientDTO>> searchMesBonsLivraison(
            @RequestBody(required = false) BonLivraisonClientSearchCriteria criteria,
            Pageable pageable,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.searchMesBonsLivraison(criteria, pageable, clientId));
    }

    @GetMapping("/bons-livraison/{id}")
    public ResponseEntity<BonLivraisonClientDTO> getMonBonLivraisonById(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(espaceClientPortalService.getMonBonLivraisonById(id, clientId));
    }

    @GetMapping("/bons-livraison/{id}/pdf")
    public ResponseEntity<byte[]> getMonBonLivraisonPdf(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean avecPrix,
            @RequestParam(required = false) Long clientId) {
        espaceClientPortalService.getMonBonLivraisonById(id, clientId);
        boolean showPrices = avecPrix == null || avecPrix;
        byte[] pdf = impressionService.genererBonLivraisonPdf(id, showPrices);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bon-livraison-" + id + ".pdf\"")
                .body(pdf);
    }
}

