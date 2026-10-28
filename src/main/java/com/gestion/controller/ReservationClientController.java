package com.gestion.controller;

import com.gestion.persistent.dto.DevisDTO;
import com.gestion.persistent.dto.ReservationClientDTO;
import com.gestion.persistent.dto.ReservationClientSearchCriteria;
import com.gestion.persistent.enums.StatutReservationClient;
import com.gestion.service.ReservationClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/reservations-client", "/api/reservations-client"})
@CrossOrigin(origins = "*")
public class ReservationClientController {

    private final ReservationClientService reservationClientService;

    public ReservationClientController(ReservationClientService reservationClientService) {
        this.reservationClientService = reservationClientService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationClientDTO>> getAllReservationsTenant() {
        return ResponseEntity.ok(reservationClientService.getReservationsDuTenant());
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ReservationClientDTO>> searchReservations(
            @RequestBody(required = false) ReservationClientSearchCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(reservationClientService.searchReservations(
                criteria != null ? criteria : new ReservationClientSearchCriteria(), pageable));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ReservationClientDTO>> getReservationsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(reservationClientService.getReservationsDuClient(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationClientDTO> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationClientService.getReservationById(id));
    }

    @PostMapping
    public ResponseEntity<ReservationClientDTO> creerReservation(
            @RequestBody ReservationClientDTO dto,
            @RequestParam(required = false) Long userId) {
        ReservationClientDTO created = reservationClientService.creerReservation(dto, null, userId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}/statut", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ReservationClientDTO> changerStatut(
            @PathVariable Long id,
            @RequestParam(required = false) StatutReservationClient statut,
            @RequestBody(required = false) Map<String, String> body) {
        StatutReservationClient resolvedStatut = statut;
        String notesTenant = null;
        if (body != null) {
            if (resolvedStatut == null && body.get("statut") != null) {
                resolvedStatut = StatutReservationClient.valueOf(body.get("statut").trim().toUpperCase());
            }
            notesTenant = body.get("notesTenant");
        }
        return ResponseEntity.ok(reservationClientService.changerStatut(id, resolvedStatut, notesTenant));
    }

    @PostMapping("/{id}/creer-devis")
    public ResponseEntity<DevisDTO> creerDevisDepuisReservation(
            @PathVariable Long id,
            @RequestBody(required = false) DevisDTO customDevisDto,
            @RequestParam(required = false) Long userId) {
        DevisDTO devis = reservationClientService.creerDevisDepuisReservation(id, customDevisDto, userId);
        return new ResponseEntity<>(devis, HttpStatus.CREATED);
    }
}
