package com.gestion.controller;

import com.gestion.persistent.model.SessionCaisse;
import com.gestion.service.SessionCaisseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrôleur désactivé : Le système fonctionne en mode SaaS B2B exclusif (Devis -> Commande -> BL -> Facture -> Paiement).
 * Les ventes comptoir / sessions de caisse physique sont masquées.
 */
// @RestController
// @RequestMapping("/api/v1/caisses")
// @CrossOrigin(origins = "*")
public class SessionCaisseController {

    private final SessionCaisseService sessionCaisseService;

    public SessionCaisseController(SessionCaisseService sessionCaisseService) {
        this.sessionCaisseService = sessionCaisseService;
    }

    public ResponseEntity<SessionCaisse> ouvrirSession(Long userId, BigDecimal fondDeCaisseInitial, String notes) {
        SessionCaisse session = sessionCaisseService.ouvrirSession(userId, fondDeCaisseInitial, notes);
        return new ResponseEntity<>(session, HttpStatus.CREATED);
    }

    public ResponseEntity<SessionCaisse> cloturerSession(Long id, BigDecimal montantReel, String notes) {
        SessionCaisse cloturee = sessionCaisseService.cloturerSession(id, montantReel, notes);
        return ResponseEntity.ok(cloturee);
    }

    public ResponseEntity<SessionCaisse> getSessionActive(Long userId) {
        return sessionCaisseService.getSessionActive(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    public ResponseEntity<SessionCaisse> getSessionById(Long id) {
        return ResponseEntity.ok(sessionCaisseService.getSessionById(id));
    }

    public ResponseEntity<List<SessionCaisse>> getAllSessions() {
        return ResponseEntity.ok(sessionCaisseService.getAllSessions());
    }
}
