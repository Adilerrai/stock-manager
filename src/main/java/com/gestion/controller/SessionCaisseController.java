package com.gestion.controller;

import com.gestion.persistent.model.SessionCaisse;
import com.gestion.service.SessionCaisseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/caisses")
@CrossOrigin(origins = "*")
public class SessionCaisseController {

    private final SessionCaisseService sessionCaisseService;

    public SessionCaisseController(SessionCaisseService sessionCaisseService) {
        this.sessionCaisseService = sessionCaisseService;
    }

    @PostMapping("/ouvrir")
    public ResponseEntity<SessionCaisse> ouvrirSession(@RequestParam Long userId,
                                                        @RequestParam(required = false) BigDecimal fondDeCaisseInitial,
                                                        @RequestParam(required = false) String notes) {
        SessionCaisse session = sessionCaisseService.ouvrirSession(userId, fondDeCaisseInitial, notes);
        return new ResponseEntity<>(session, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<SessionCaisse> cloturerSession(@PathVariable Long id,
                                                         @RequestParam(required = false) BigDecimal montantReel,
                                                         @RequestParam(required = false) String notes) {
        SessionCaisse cloturee = sessionCaisseService.cloturerSession(id, montantReel, notes);
        return ResponseEntity.ok(cloturee);
    }

    @GetMapping("/active")
    public ResponseEntity<SessionCaisse> getSessionActive(@RequestParam Long userId) {
        return sessionCaisseService.getSessionActive(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionCaisse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionCaisseService.getSessionById(id));
    }

    @GetMapping
    public ResponseEntity<List<SessionCaisse>> getAllSessions() {
        return ResponseEntity.ok(sessionCaisseService.getAllSessions());
    }
}
