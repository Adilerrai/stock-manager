package com.acommon.controller;

import com.acommon.persistant.dto.PointDeVenteRequest;
import com.acommon.persistant.dto.PointDeVenteResponse;
import com.acommon.service.PointDeVenteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/points-de-vente", "/points-de-vente", "/api/v1/points-de-vente"})
@CrossOrigin(origins = "*")
public class PointDeVenteController {

    private final PointDeVenteService pointDeVenteService;

    public PointDeVenteController(PointDeVenteService pointDeVenteService) {
        this.pointDeVenteService = pointDeVenteService;
    }

    /**
     * Liste des points de vente de l'entreprise (tenant) de l'utilisateur connecté.
     * Accessible par l'Admin d'entreprise, les managers ou le SuperAdmin.
     */
    @GetMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_VENDEUR', 'ROLE_CAISSIER', 'ROLE_MAGASINIER', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<PointDeVenteResponse>> getPointsDeVente() {
        return ResponseEntity.ok(pointDeVenteService.getPointsDeVenteByCurrentTenant());
    }

    /**
     * Détails d'un point de vente par ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<PointDeVenteResponse> getPointDeVenteById(@PathVariable Long id) {
        return ResponseEntity.ok(pointDeVenteService.getPointDeVenteById(id));
    }

    /**
     * Création d'un nouveau point de vente rattaché à l'entreprise (tenant) connectée.
     */
    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<PointDeVenteResponse> createPointDeVente(@Valid @RequestBody PointDeVenteRequest request) {
        PointDeVenteResponse response = pointDeVenteService.createPointDeVente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Modification d'un point de vente existant.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<PointDeVenteResponse> updatePointDeVente(
            @PathVariable Long id,
            @Valid @RequestBody PointDeVenteRequest request) {
        return ResponseEntity.ok(pointDeVenteService.updatePointDeVente(id, request));
    }
}
