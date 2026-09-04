package com.gestion.controller;

import com.gestion.mapper.VenteMapper;
import com.gestion.persistent.dto.VenteDTO;
import com.gestion.persistent.dto.VenteSearchCriteria;
import com.gestion.persistent.model.Vente;
import com.gestion.service.VenteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/ventes")
@CrossOrigin(origins = "*")
public class VenteController {

    private final VenteService venteService;
    private final VenteMapper venteMapper;

    public VenteController(VenteService venteService, VenteMapper venteMapper) {
        this.venteService = venteService;
        this.venteMapper = venteMapper;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<VenteDTO>> searchVentes(@RequestBody VenteSearchCriteria criteria, Pageable pageable) {
        Page<Vente> page = venteService.searchVentes(criteria, pageable);
        return ResponseEntity.ok(page.map(venteMapper::toDto));
    }

    @PostMapping
    public ResponseEntity<VenteDTO> creerVente(@RequestBody VenteDTO dto, @RequestParam Long vendeurId) {
        VenteDTO nouvelleVente = venteService.creerVente(dto, vendeurId);
        return new ResponseEntity<>(nouvelleVente, HttpStatus.CREATED);
    }

    @PostMapping("/{venteId}/valider")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VENTE_VALIDER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_VENDEUR')")
    public ResponseEntity<VenteDTO> validerVente(@PathVariable Long venteId) {
        VenteDTO vente = venteService.validerVente(venteId);
        return ResponseEntity.ok(vente);
    }

    @PostMapping("/{venteId}/annuler")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VENTE_ANNULER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<VenteDTO> annulerVente(@PathVariable Long venteId,
                                                @RequestParam String motif,
                                                @RequestParam Long userId) {
        VenteDTO vente = venteService.annulerVente(venteId, motif, userId);
        return ResponseEntity.ok(vente);
    }

    @PatchMapping("/{venteId}/remise")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('REMISE_VALIDER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL')")
    public ResponseEntity<VenteDTO> appliquerRemise(@PathVariable Long venteId, @RequestParam BigDecimal remise) {
        VenteDTO vente = venteService.appliquerRemiseGlobale(venteId, remise);
        return ResponseEntity.ok(vente);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenteDTO> getVente(@PathVariable Long id) {
        VenteDTO vente = venteService.getVenteById(id);
        return ResponseEntity.ok(vente);
    }

    @GetMapping
    public ResponseEntity<List<VenteDTO>> getAllVentes() {
        List<VenteDTO> ventes = venteService.getAllVentes();
        return ResponseEntity.ok(ventes);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<VenteDTO>> getVentesByClient(@PathVariable Long clientId) {
        List<VenteDTO> ventes = venteService.getVentesByClient(clientId);
        return ResponseEntity.ok(ventes);
    }
}
