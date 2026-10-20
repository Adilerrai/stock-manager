package com.gestion.controller;

import com.acommon.persistant.model.User;
import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmRapportVisiteDTO;
import com.gestion.persistent.dto.CrmTacheDTO;
import com.gestion.persistent.dto.CrmValiderRapportRequest;
import com.gestion.persistent.model.CrmRapportVisite;
import com.gestion.service.CrmRapportVisiteService;
import com.gestion.service.CrmTacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/rapports")
@CrossOrigin(origins = "*")
public class CrmRapportVisiteController {

    private final CrmRapportVisiteService rapportVisiteService;
    private final CrmTacheService tacheService;
    private final CrmMapper crmMapper;

    public CrmRapportVisiteController(CrmRapportVisiteService rapportVisiteService,
                                     CrmTacheService tacheService,
                                     CrmMapper crmMapper) {
        this.rapportVisiteService = rapportVisiteService;
        this.tacheService = tacheService;
        this.crmMapper = crmMapper;
    }

    @PostMapping("/visite/{visiteId}/brouillon")
    public ResponseEntity<CrmRapportVisiteDTO> enregistrerBrouillon(
            @PathVariable Long visiteId,
            @RequestBody CrmRapportVisiteDTO dto) {
        CrmRapportVisite entity = crmMapper.toEntity(dto);
        CrmRapportVisite saved = rapportVisiteService.enregistrerBrouillon(visiteId, entity);
        return new ResponseEntity<>(enrichirActions(crmMapper.toDto(saved)), HttpStatus.OK);
    }

    @PostMapping("/visite/{visiteId}/soumettre")
    public ResponseEntity<CrmRapportVisiteDTO> soumettre(
            @PathVariable Long visiteId,
            @RequestBody CrmRapportVisiteDTO dto) {
        CrmRapportVisite entity = crmMapper.toEntity(dto);
        CrmRapportVisite submitted = rapportVisiteService.soumettreRapport(visiteId, entity, dto.getActionsSuivi());
        return new ResponseEntity<>(enrichirActions(crmMapper.toDto(submitted)), HttpStatus.OK);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<CrmRapportVisiteDTO> valider(
            @PathVariable Long id,
            @RequestBody CrmValiderRapportRequest request,
            @AuthenticationPrincipal User userConnecte) {
        Long managerId = userConnecte != null ? userConnecte.getId() : 1L;
        CrmRapportVisite validated = rapportVisiteService.validerRapportParManager(
                id,
                managerId,
                request.isApprouve(),
                request.getMotifRejet()
        );
        return ResponseEntity.ok(enrichirActions(crmMapper.toDto(validated)));
    }

    @GetMapping("/visite/{visiteId}")
    public ResponseEntity<CrmRapportVisiteDTO> getByVisiteId(@PathVariable Long visiteId) {
        return rapportVisiteService.getRapportByVisiteId(visiteId)
                .map(crmMapper::toDto)
                .map(this::enrichirActions)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmRapportVisiteDTO> getById(@PathVariable Long id) {
        CrmRapportVisite entity = rapportVisiteService.getRapportById(id);
        return ResponseEntity.ok(enrichirActions(crmMapper.toDto(entity)));
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<CrmRapportVisiteDTO>> getEnAttenteValidation() {
        List<CrmRapportVisiteDTO> dtos = rapportVisiteService.getRapportsEnAttenteValidation().stream()
                .map(crmMapper::toDto)
                .map(this::enrichirActions)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private CrmRapportVisiteDTO enrichirActions(CrmRapportVisiteDTO dto) {
        if (dto != null && dto.getId() != null) {
            List<CrmTacheDTO> taches = tacheService.getTachesByRapport(dto.getId()).stream()
                    .map(crmMapper::toDto)
                    .collect(Collectors.toList());
            dto.setActionsSuivi(taches);
        }
        return dto;
    }
}
