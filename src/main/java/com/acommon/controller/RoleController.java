package com.acommon.controller;

import com.acommon.persistant.dto.RoleCreateRequest;
import com.acommon.persistant.dto.RoleResponse;
import com.acommon.persistant.dto.RoleUpdateHabilitationsRequest;
import com.acommon.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/roles", "/roles"})
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Liste tous les rôles avec leurs habilitations.
     * Accessible aux admins et superadmins.
     */
    @GetMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * Détail d'un rôle.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    /**
     * Créer un nouveau rôle personnalisé.
     * Accessible au SUPERADMIN et aux ADMIN tenant (pour leurs propres besoins).
     */
    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    /**
     * Modifier les habilitations assignées à un rôle.
     * Accessible au SUPERADMIN et aux ADMIN tenant.
     */
    @PutMapping("/{id}/habilitations")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<RoleResponse> updateHabilitations(
            @PathVariable Long id,
            @RequestBody RoleUpdateHabilitationsRequest request) {
        return ResponseEntity.ok(roleService.updateHabilitations(id, request.getHabilitations()));
    }

    /**
     * Supprimer un rôle (interdit si des utilisateurs y sont assignés ou si c'est un rôle système).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("message", "Rôle supprimé avec succès"));
    }

    /**
     * Liste toutes les habilitations disponibles dans le système.
     */
    @GetMapping("/habilitations")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<List<String>> getAllHabilitations() {
        return ResponseEntity.ok(roleService.getAllHabilitationNoms());
    }
}
