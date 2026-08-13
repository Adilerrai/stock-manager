package com.gestion.controller;

import com.gestion.mapper.CommandeMapper;
import com.gestion.persistent.dto.CommandeDTO;
import com.gestion.persistent.dto.CommandeSearchCriteria;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.model.Commande;
import com.gestion.service.CommandeService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/commandes", "/api/v1/commandes-fournisseur"})
public class CommandeController {

    private final CommandeService commandeService;
    private final CommandeMapper commandeMapper;
    private final com.gestion.service.LivraisonService livraisonService;

    public CommandeController(CommandeService commandeService, 
                             CommandeMapper commandeMapper,
                             com.gestion.service.LivraisonService livraisonService) {
        this.commandeService = commandeService;
        this.commandeMapper = commandeMapper;
        this.livraisonService = livraisonService;
    }

    @PostMapping("/create")
    public ResponseEntity<CommandeDTO> createCommande(@RequestBody CommandeDTO commandeDTO) {
        Commande commande = commandeService.createCommande(commandeDTO);
        return ResponseEntity.ok(commandeMapper.toDto(commande));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CommandeDTO>> getAllCommandes() {
        List<Commande> commandes = commandeService.getAllCommandes();
        List<CommandeDTO> commandeDTOs = commandes.stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commandeDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable("id") Long id) {
        Commande commande = commandeService.getCommandeById(id);
        return ResponseEntity.ok(commandeMapper.toDto(commande));
    }

    @PostMapping("/search")
    public ResponseEntity<List<CommandeDTO>> searchCommandes(@RequestBody CommandeSearchCriteria criteria, Pageable pageable) {
        List<Commande> results = commandeService.searchCommandes(criteria);
        List<CommandeDTO> dtoList = results.stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
        // Pagination manuelle si pageable fourni et taille inférieure à la liste
        if (pageable != null && pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), dtoList.size());
            if (start > dtoList.size()) {
                return ResponseEntity.ok(List.of());
            }
            List<CommandeDTO> pageSlice = dtoList.subList(start, end);
            return ResponseEntity.ok(pageSlice);
        }
        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/{id}/confirmer")
    public ResponseEntity<CommandeDTO> confirmerVente(@PathVariable Long id) {
        Commande commande = commandeService.updateStatutCommande(id, StatutCommande.PASSEE);
        return ResponseEntity.ok(commandeMapper.toDto(commande));
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<CommandeDTO> updateStatutCommande(@PathVariable("id") Long id, 
                                                           @RequestParam StatutCommande statut) {
        Commande commande = commandeService.updateStatutCommande(id, statut);
        return ResponseEntity.ok(commandeMapper.toDto(commande));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<CommandeDTO> annulerCommande(@PathVariable("id") Long id) {
        Commande commande = commandeService.annulerCommande(id);
        return ResponseEntity.ok(commandeMapper.toDto(commande));
    }

    @GetMapping(value = "/{id}/print", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> printCommande(@PathVariable("id") Long id) {
        try {
            byte[] pdfBytes = commandeService.generateCommandePdf(id);

            if (pdfBytes == null) {
                String msg = "PDF not found for id=" + id;
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(msg.getBytes(StandardCharsets.UTF_8));
            }

            if (pdfBytes.length == 0) {
                String msg = "PDF generation returned empty content for id=" + id;
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(msg.getBytes(StandardCharsets.UTF_8));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"commande_" + id + ".pdf\"");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            String err = "Erreur génération PDF: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(err.getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<CommandeDTO>> getCommandesByStatut(@PathVariable String statut) {
        try {
            StatutCommande s = StatutCommande.valueOf(statut.toUpperCase());
            List<Commande> commandes = commandeService.getCommandesByStatut(s);
            List<CommandeDTO> dtos = commandes.stream()
                    .map(commandeMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<CommandeDTO>> getCommandesByFournisseur(@PathVariable Long fournisseurId) {
        List<Commande> commandes = commandeService.getCommandesByFournisseur(fournisseurId);
        List<CommandeDTO> dtos = commandes.stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/statut/{newStatut}")
    public ResponseEntity<CommandeDTO> updateStatutPath(@PathVariable Long id, @PathVariable String newStatut) {
        try {
            StatutCommande s = StatutCommande.valueOf(newStatut.toUpperCase());
            Commande commande = commandeService.updateStatutCommande(id, s);
            return ResponseEntity.ok(commandeMapper.toDto(commande));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/convert-to-reception")
    public ResponseEntity<Long> convertToReception(@PathVariable Long id) {
        try {
            Long receptionId = livraisonService.creerLivraisonDepuisCommande(id);
            return ResponseEntity.ok(receptionId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}

