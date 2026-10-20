package com.gestion.controller;

import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmActiviteDTO;
import com.gestion.persistent.dto.CrmEntrepriseDTO;
import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.CrmEntreprise;
import com.gestion.repository.CrmContactRepository;
import com.gestion.repository.CrmOpportuniteRepository;
import com.gestion.repository.CrmVisiteRepository;
import com.gestion.service.CrmActiviteService;
import com.gestion.service.CrmEntrepriseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/entreprises")
@CrossOrigin(origins = "*")
public class CrmEntrepriseController {

    private final CrmEntrepriseService entrepriseService;
    private final CrmActiviteService activiteService;
    private final CrmContactRepository contactRepository;
    private final CrmOpportuniteRepository opportuniteRepository;
    private final CrmVisiteRepository visiteRepository;
    private final CrmMapper crmMapper;

    public CrmEntrepriseController(CrmEntrepriseService entrepriseService,
                                   CrmActiviteService activiteService,
                                   CrmContactRepository contactRepository,
                                   CrmOpportuniteRepository opportuniteRepository,
                                   CrmVisiteRepository visiteRepository,
                                   CrmMapper crmMapper) {
        this.entrepriseService = entrepriseService;
        this.activiteService = activiteService;
        this.contactRepository = contactRepository;
        this.opportuniteRepository = opportuniteRepository;
        this.visiteRepository = visiteRepository;
        this.crmMapper = crmMapper;
    }

    @PostMapping
    public ResponseEntity<CrmEntrepriseDTO> creer(@RequestBody CrmEntrepriseDTO dto) {
        CrmEntreprise entity = crmMapper.toEntity(dto);
        CrmEntreprise saved = entrepriseService.creerEntreprise(entity);
        return new ResponseEntity<>(enrichirStats(crmMapper.toDto(saved)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrmEntrepriseDTO> modifier(@PathVariable Long id, @RequestBody CrmEntrepriseDTO dto) {
        CrmEntreprise entity = crmMapper.toEntity(dto);
        CrmEntreprise updated = entrepriseService.modifierEntreprise(id, entity);
        return ResponseEntity.ok(enrichirStats(crmMapper.toDto(updated)));
    }

    @GetMapping
    public ResponseEntity<List<CrmEntrepriseDTO>> getAll() {
        List<CrmEntrepriseDTO> dtos = entrepriseService.getAllEntreprises().stream()
                .map(crmMapper::toDto)
                .map(this::enrichirStats)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmEntrepriseDTO> getById(@PathVariable Long id) {
        CrmEntreprise entity = entrepriseService.getEntrepriseById(id);
        return ResponseEntity.ok(enrichirStats(crmMapper.toDto(entity)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CrmEntrepriseDTO>> search(@RequestParam String q) {
        List<CrmEntrepriseDTO> dtos = entrepriseService.searchEntreprises(q).stream()
                .map(crmMapper::toDto)
                .map(this::enrichirStats)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/doublons")
    public ResponseEntity<List<CrmEntrepriseDTO>> detecterDoublons(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String ice) {
        List<CrmEntrepriseDTO> dtos = entrepriseService.detecterDoublons(nom, ice).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/convertir-client")
    public ResponseEntity<Client> convertirClient(@PathVariable Long id) {
        Client client = entrepriseService.convertirEnClientErp(id);
        return ResponseEntity.ok(client);
    }

    @GetMapping("/{id}/activites")
    public ResponseEntity<List<CrmActiviteDTO>> getActivites(@PathVariable Long id) {
        List<CrmActiviteDTO> dtos = activiteService.getHistoriqueEntreprise(id).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private CrmEntrepriseDTO enrichirStats(CrmEntrepriseDTO dto) {
        if (dto != null && dto.getId() != null && dto.getPointDeVenteId() != null) {
            dto.setNombreContacts(contactRepository.countByPointDeVenteIdAndEntrepriseId(dto.getPointDeVenteId(), dto.getId()));
            dto.setNombreOpportunites(opportuniteRepository.countByPointDeVenteIdAndEntrepriseId(dto.getPointDeVenteId(), dto.getId()));
            dto.setNombreVisites(visiteRepository.countByPointDeVenteIdAndEntrepriseId(dto.getPointDeVenteId(), dto.getId()));
        }
        return dto;
    }
}
