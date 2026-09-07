package com.acommon.controller;

import com.acommon.persistant.dto.*;
import com.acommon.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/users", "/users", "/api/v1/users"})
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Création d'un nouvel utilisateur dans l'entreprise courante (ou assigné par le SuperAdmin).
     */
    @PostMapping({"", "/create"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Liste des utilisateurs.
     * Pour les administrateurs d'entreprise : liste les utilisateurs de leur entreprise.
     * Pour le SuperAdmin : liste tous les utilisateurs ou filtre par pointDeVenteId.
     */
    @GetMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(name = "pointDeVenteId", required = false) Long pointDeVenteId) {
        List<UserResponse> users = userService.getUsers(pointDeVenteId);
        return ResponseEntity.ok(users);
    }

    /**
     * Obtenir les informations d'un utilisateur par son ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Modification d'un utilisateur existant.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Activer ou désactiver (bloquer) un utilisateur.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<UserResponse> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.toggleUserStatus(id, enabled));
    }

    /**
     * Réinitialisation de mot de passe d'un utilisateur par son administrateur ou le superadmin.
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }

    /**
     * Changement de mot de passe personnel par l'utilisateur connecté.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Votre mot de passe a été modifié avec succès"));
    }

    /**
     * Suppression d'un utilisateur.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));
    }
}