package com.gestion.controller;

import com.gestion.persistent.dto.StatistiqueMotifRetourDTO;
import com.gestion.persistent.enums.TypeAvoir;
import com.gestion.persistent.model.Avoir;
import com.gestion.service.AvoirService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/avoirs", "/api/avoirs"})
@CrossOrigin(origins = "*")
public class AvoirController {

    private final AvoirService avoirService;

    public AvoirController(AvoirService avoirService) {
        this.avoirService = avoirService;
    }

    @PostMapping
    public ResponseEntity<Avoir> creerAvoir(@RequestBody Avoir avoir,
                                           @RequestParam(required = false) Long userId) {
        Avoir nouvelAvoir = avoirService.creerAvoir(avoir, userId);
        return new ResponseEntity<>(nouvelAvoir, HttpStatus.CREATED);
    }

    @PostMapping("/depuis-facture/{factureId}")
    public ResponseEntity<Avoir> creerAvoirDepuisFacture(@PathVariable Long factureId,
                                                         @RequestParam(required = false) String motif,
                                                         @RequestParam(required = false) Long userId) {
        Avoir avoir = avoirService.creerAvoirDepuisFacture(factureId, motif, userId);
        return new ResponseEntity<>(avoir, HttpStatus.CREATED);
    }

    @PostMapping("/depuis-facture-achat/{factureAchatId}")
    public ResponseEntity<Avoir> creerAvoirDepuisFactureAchat(@PathVariable Long factureAchatId,
                                                              @RequestParam(required = false) String motif,
                                                              @RequestParam(required = false) Long userId) {
        Avoir avoir = avoirService.creerAvoirDepuisFactureAchat(factureAchatId, motif, userId);
        return new ResponseEntity<>(avoir, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Avoir> validerAvoir(@PathVariable Long id,
                                              @RequestParam(required = false) Long depotId) {
        Avoir valide = avoirService.validerAvoir(id, depotId);
        return ResponseEntity.ok(valide);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Avoir> getAvoirById(@PathVariable Long id) {
        return ResponseEntity.ok(avoirService.getAvoirById(id));
    }

    @GetMapping
    public ResponseEntity<List<Avoir>> getAllAvoirs(@RequestParam(required = false) TypeAvoir type) {
        if (type != null) {
            return ResponseEntity.ok(avoirService.getAvoirsByType(type));
        }
        return ResponseEntity.ok(avoirService.getAllAvoirs());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Avoir>> getAvoirsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(avoirService.getAvoirsByClient(clientId));
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<Avoir>> getAvoirsByFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(avoirService.getAvoirsByFournisseur(fournisseurId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerAvoir(@PathVariable Long id) {
        avoirService.supprimerAvoir(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Analyse des causes de retours clients avec perte financière par motif :
     * (Produit défectueux, erreur magasinier, erreur commande, etc.)
     */
    @GetMapping("/statistiques-motifs")
    @PreAuthorize("hasAnyAuthority('DASHBOARD_VOIR', 'VENTE_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL')")
    public ResponseEntity<List<StatistiqueMotifRetourDTO>> getStatistiquesMotifs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(avoirService.getStatistiquesMotifsRetour(dateDebut, dateFin));
    }
}
