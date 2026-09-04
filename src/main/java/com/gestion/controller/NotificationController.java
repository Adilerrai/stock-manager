package com.gestion.controller;

import com.gestion.persistent.dto.NotificationDTO;
import com.gestion.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") Boolean nonLuesUniquement) {
        return ResponseEntity.ok(notificationService.getNotifications(nonLuesUniquement));
    }

    @GetMapping("/count-non-lues")
    public ResponseEntity<Map<String, Long>> countNonLues() {
        return ResponseEntity.ok(Map.of("nonLues", notificationService.countNonLues()));
    }

    @PatchMapping("/{id}/lire")
    public ResponseEntity<NotificationDTO> marquerCommeLu(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.marquerCommeLu(id));
    }

    @PatchMapping("/marquer-toutes-lues")
    public ResponseEntity<Void> marquerToutesCommeLues() {
        notificationService.marquerToutesCommeLues();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerNotification(@PathVariable Long id) {
        notificationService.supprimerNotification(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generer-alertes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<Map<String, Object>> genererAlertes() {
        int count = notificationService.genererAlertesAutomatiques();
        return ResponseEntity.ok(Map.of("alertesGenerees", count));
    }
}
