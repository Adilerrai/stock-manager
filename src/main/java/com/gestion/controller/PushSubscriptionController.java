package com.gestion.controller;

import com.acommon.persistant.model.User;
import com.gestion.persistent.dto.PushSubscriptionRequest;
import com.gestion.service.push.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** L'utilisateur vient toujours du token : ni le corps ni l'en-tête de tenant ne choisissent le propriétaire. */
@RestController
@RequestMapping("/api/push")
public class PushSubscriptionController {

    private final PushNotificationService notifications;

    public PushSubscriptionController(PushNotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/configuration")
    public ResponseEntity<Map<String, Object>> configuration() {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("actif", notifications.actif());
        reponse.put("clePublique", notifications.clePublique());
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/abonnements")
    public ResponseEntity<Void> abonner(@AuthenticationPrincipal User utilisateur, @Valid @RequestBody PushSubscriptionRequest demande) {
        notifications.abonner(utilisateur.getId(), demande.endpoint(), demande.p256dh(), demande.auth());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/abonnements/retrait")
    public ResponseEntity<Void> desabonner(@AuthenticationPrincipal User utilisateur, @Valid @RequestBody PushSubscriptionRequest.Retrait demande) {
        notifications.desabonner(utilisateur.getId(), demande.endpoint());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Integer>> tester(@AuthenticationPrincipal User utilisateur) {
        return ResponseEntity.ok(Map.of("envoyes", notifications.envoyerTest(utilisateur.getId())));
    }
}
