package com.acommon.controller;

import com.acommon.persistant.dto.EntrepriseRegistrationRequest;
import com.acommon.persistant.dto.EntrepriseResponse;
import com.acommon.persistant.dto.EntrepriseUpdateRequest;
import com.acommon.service.EntrepriseManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/admin/entreprises", "/admin/entreprises", "/api/v1/admin/entreprises"})
@CrossOrigin(origins = "*")
public class EntrepriseManagementController {

    private final EntrepriseManagementService entrepriseManagementService;

    public EntrepriseManagementController(EntrepriseManagementService entrepriseManagementService) {
        this.entrepriseManagementService = entrepriseManagementService;
    }

    /**
     * Création d'une nouvelle entreprise avec son administrateur dédié et son dépôt par défaut.
     * Accessible par le SUPERADMIN ou en inscription.
     */
    @PostMapping({"", "/register"})
    public ResponseEntity<EntrepriseResponse> creerEntreprise(
            @Valid @RequestBody EntrepriseRegistrationRequest request) {
        EntrepriseResponse response = entrepriseManagementService.creerEntreprise(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Liste toutes les entreprises inscrites (filtrage par recherche et statut actif/inactif).
     * Réservé au SUPERADMIN.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMIN')")
    public ResponseEntity<Page<EntrepriseResponse>> listerEntreprises(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EntrepriseResponse> result = entrepriseManagementService.listerEntreprises(search, actif, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Détails d'une entreprise par son ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMIN')")
    public ResponseEntity<EntrepriseResponse> getEntrepriseById(@PathVariable Long id) {
        return ResponseEntity.ok(entrepriseManagementService.getEntrepriseById(id));
    }

    /**
     * Modification des coordonnées et profil de l'entreprise.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMIN')")
    public ResponseEntity<EntrepriseResponse> modifierEntreprise(
            @PathVariable Long id,
            @Valid @RequestBody EntrepriseUpdateRequest request) {
        return ResponseEntity.ok(entrepriseManagementService.modifierEntreprise(id, request));
    }

    /**
     * Activation / Suspension de l'accès de l'entreprise.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMIN')")
    public ResponseEntity<EntrepriseResponse> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean actif) {
        return ResponseEntity.ok(entrepriseManagementService.toggleActif(id, actif));
    }
}
