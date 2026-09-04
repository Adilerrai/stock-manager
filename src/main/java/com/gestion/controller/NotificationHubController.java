package com.gestion.controller;

import com.gestion.persistent.dto.NotificationSummaryDTO;
import com.gestion.service.NotificationHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@Tag(name = "Notifications & Alertes", description = "Centre de notifications unifié de l'ERP (impayés, stock critique, échéances de paiement, commandes)")
public class NotificationHubController {

    private final NotificationHubService notificationHubService;

    public NotificationHubController(NotificationHubService notificationHubService) {
        this.notificationHubService = notificationHubService;
    }

    @GetMapping("/hub")
    @Operation(summary = "Obtenir la liste exhaustive de toutes les alertes métier actives avec leurs détails")
    public ResponseEntity<NotificationSummaryDTO> getHubAlertes() {
        return ResponseEntity.ok(notificationHubService.getNotificationSummary());
    }

    @GetMapping("/summary")
    @Operation(summary = "Obtenir le comptage des alertes par catégorie pour le badge de la barre supérieure")
    public ResponseEntity<NotificationSummaryDTO> getBadgeSummary() {
        return ResponseEntity.ok(notificationHubService.getNotificationSummary());
    }
}
