package com.gestion.controller;

import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.FacturationBLRequest;
import com.gestion.persistent.dto.FactureDTO;
import com.gestion.service.FactureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.gestion.mapper.FactureMapper;
import com.gestion.persistent.dto.FactureSearchCriteria;
import com.gestion.persistent.model.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/factures")
@CrossOrigin(origins = "*")
public class FactureController {

    private final FactureService factureService;
    private final com.gestion.service.ImpressionService impressionService;
    private final FactureMapper factureMapper;

    public FactureController(FactureService factureService, 
                             com.gestion.service.ImpressionService impressionService,
                             FactureMapper factureMapper) {
        this.factureService = factureService;
        this.impressionService = impressionService;
        this.factureMapper = factureMapper;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<FactureDTO>> searchFactures(@RequestBody FactureSearchCriteria criteria, Pageable pageable) {
        Page<Facture> page = factureService.searchFactures(criteria, pageable);
        return ResponseEntity.ok(page.map(factureMapper::toDto));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getFacturePdf(@PathVariable Long id) {
        byte[] pdf = impressionService.genererFacturePdf(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture-" + id + ".pdf\"")
                .body(pdf);
    }

    /**
     * Crée une facture en regroupant manuellement les BLs sélectionnés par l'utilisateur
     */
    @PostMapping("/depuis-bl")
    @PreAuthorize("hasAnyAuthority('FACTURE_CREER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<FactureDTO> creerFactureDepuisBL(@RequestBody FacturationBLRequest request) {
        FactureDTO factureDTO = factureService.creerFactureDepuisBonsLivraison(request);
        return new ResponseEntity<>(factureDTO, HttpStatus.CREATED);
    }

    /**
     * Retourne la liste des BLs non encore facturés pour un client donné (ex: pour la facturation mensuelle)
     */
    @GetMapping("/bl-non-factures/{clientId}")
    public ResponseEntity<List<BonLivraisonClientDTO>> getBonsLivraisonNonFacturesByClient(@PathVariable Long clientId) {
        List<BonLivraisonClientDTO> bls = factureService.getBonsLivraisonNonFacturesByClient(clientId);
        return ResponseEntity.ok(bls);
    }

    /**
     * Retourne tous les BLs non encore facturés du point de vente
     */
    @GetMapping("/bl-non-factures")
    public ResponseEntity<List<BonLivraisonClientDTO>> getAllBonsLivraisonNonFactures() {
        List<BonLivraisonClientDTO> bls = factureService.getAllBonsLivraisonNonFactures();
        return ResponseEntity.ok(bls);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FACTURE_CREER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<FactureDTO> creerFacture(@RequestBody FactureDTO factureDTO, @RequestParam Long userId) {
        FactureDTO nouvelleFacture = factureService.creerFacture(factureDTO, userId);
        return new ResponseEntity<>(nouvelleFacture, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureDTO> getFacture(@PathVariable Long id) {
        FactureDTO facture = factureService.getFactureById(id);
        return ResponseEntity.ok(facture);
    }

    @GetMapping
    public ResponseEntity<List<FactureDTO>> getAllFactures() {
        List<FactureDTO> factures = factureService.getAllFactures();
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<FactureDTO>> getFacturesByClient(@PathVariable Long clientId) {
        List<FactureDTO> factures = factureService.getFacturesByClient(clientId);
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/impayees")
    public ResponseEntity<List<FactureDTO>> getFacturesImpayees() {
        List<FactureDTO> factures = factureService.getFacturesImpayees();
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/echues")
    public ResponseEntity<List<FactureDTO>> getFacturesEchues() {
        List<FactureDTO> factures = factureService.getFacturesEchues();
        return ResponseEntity.ok(factures);
    }

    @PostMapping("/{factureId}/valider")
    @PreAuthorize("hasAnyAuthority('FACTURE_VALIDER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<FactureDTO> validerFacture(@PathVariable Long factureId) {
        FactureDTO facture = factureService.validerFacture(factureId);
        return ResponseEntity.ok(facture);
    }

    @PostMapping("/{factureId}/annuler")
    @PreAuthorize("hasAnyAuthority('FACTURE_ANNULER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE')")
    public ResponseEntity<FactureDTO> annulerFacture(@PathVariable Long factureId,
                                                   @RequestParam String motif,
                                                   @RequestParam Long userId) {
        FactureDTO facture = factureService.annulerFacture(factureId, motif, userId);
        return ResponseEntity.ok(facture);
    }
}
