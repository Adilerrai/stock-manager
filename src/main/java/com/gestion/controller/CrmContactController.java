package com.gestion.controller;

import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmActiviteDTO;
import com.gestion.persistent.dto.CrmContactDTO;
import com.gestion.persistent.model.CrmContact;
import com.gestion.service.CrmActiviteService;
import com.gestion.service.CrmContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/contacts")
@CrossOrigin(origins = "*")
public class CrmContactController {

    private final CrmContactService contactService;
    private final CrmActiviteService activiteService;
    private final CrmMapper crmMapper;

    public CrmContactController(CrmContactService contactService,
                                CrmActiviteService activiteService,
                                CrmMapper crmMapper) {
        this.contactService = contactService;
        this.activiteService = activiteService;
        this.crmMapper = crmMapper;
    }

    @PostMapping
    public ResponseEntity<CrmContactDTO> creer(@RequestBody CrmContactDTO dto) {
        CrmContact entity = crmMapper.toEntity(dto);
        CrmContact saved = contactService.creerContact(entity);
        return new ResponseEntity<>(crmMapper.toDto(saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrmContactDTO> modifier(@PathVariable Long id, @RequestBody CrmContactDTO dto) {
        CrmContact entity = crmMapper.toEntity(dto);
        CrmContact updated = contactService.modifierContact(id, entity);
        return ResponseEntity.ok(crmMapper.toDto(updated));
    }

    @GetMapping
    public ResponseEntity<List<CrmContactDTO>> getAll() {
        List<CrmContactDTO> dtos = contactService.getAllContacts().stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmContactDTO> getById(@PathVariable Long id) {
        CrmContact entity = contactService.getContactById(id);
        return ResponseEntity.ok(crmMapper.toDto(entity));
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<CrmContactDTO>> getByEntreprise(@PathVariable Long entrepriseId) {
        List<CrmContactDTO> dtos = contactService.getContactsByEntreprise(entrepriseId).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CrmContactDTO>> search(@RequestParam String q) {
        List<CrmContactDTO> dtos = contactService.searchContacts(q).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/doublons")
    public ResponseEntity<List<CrmContactDTO>> detecterDoublons(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telephone) {
        List<CrmContactDTO> dtos = contactService.detecterDoublons(email, telephone).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/activites")
    public ResponseEntity<List<CrmActiviteDTO>> getActivites(@PathVariable Long id) {
        List<CrmActiviteDTO> dtos = activiteService.getHistoriqueContact(id).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
