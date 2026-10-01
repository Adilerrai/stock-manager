package com.gestion.service;

import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CodificationDocumentDTO;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.enums.TypeReinitialisationCodification;
import com.gestion.persistent.model.CodificationDocument;
import com.gestion.repository.CodificationDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodificationService {

    private static final Logger log = LoggerFactory.getLogger(CodificationService.class);

    private final CodificationDocumentRepository repository;

    public CodificationService(CodificationDocumentRepository repository) {
        this.repository = repository;
    }

    public Long getCurrentTenantId() {
        Long tenant = CurrentRequestContext.getSocieteId();
        if (tenant == null) {
            tenant = TenantContext.getCurrentTenant();
        }
        return (tenant != null) ? tenant : 1L;
    }

    @Transactional
    public List<CodificationDocumentDTO> getCodificationsCurrentTenant() {
        Long tenantId = getCurrentTenantId();
        garantirInitialisation(tenantId);
        return repository.findByPointDeVenteIdOrderByTypeDocumentAsc(tenantId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CodificationDocumentDTO updateCodification(TypeDocumentCodification typeDoc, CodificationDocumentDTO dto) {
        Long tenantId = getCurrentTenantId();
        CodificationDocument entity = repository.findByPointDeVenteIdAndTypeDocument(tenantId, typeDoc)
                .orElseGet(() -> new CodificationDocument(tenantId, typeDoc));

        if (dto.getPrefixe() != null && !dto.getPrefixe().isBlank()) {
            entity.setPrefixe(dto.getPrefixe().trim().toUpperCase());
        }
        if (dto.getModeleFormat() != null && !dto.getModeleFormat().isBlank()) {
            entity.setModeleFormat(dto.getModeleFormat().trim());
        }
        if (dto.getLongueurSequence() != null && dto.getLongueurSequence() >= 1 && dto.getLongueurSequence() <= 10) {
            entity.setLongueurSequence(dto.getLongueurSequence());
        }
        if (dto.getTypeReinitialisation() != null) {
            entity.setTypeReinitialisation(dto.getTypeReinitialisation());
        }
        if (dto.getActif() != null) {
            entity.setActif(dto.getActif());
        }
        if (dto.getProchainNumero() != null && dto.getProchainNumero() >= 1) {
            entity.setDernierNumero(dto.getProchainNumero() - 1);
        }

        entity.setDateDerniereMaj(LocalDateTime.now());
        CodificationDocument saved = repository.save(entity);
        return toDTO(saved);
    }

    @Transactional
    public void reinitialiserDefautsCurrentTenant() {
        Long tenantId = getCurrentTenantId();
        for (TypeDocumentCodification typeDoc : TypeDocumentCodification.values()) {
            CodificationDocument entity = repository.findByPointDeVenteIdAndTypeDocument(tenantId, typeDoc)
                    .orElseGet(() -> new CodificationDocument(tenantId, typeDoc));

            entity.setPrefixe(typeDoc.getPrefixeDefaut());
            entity.setModeleFormat(typeDoc.getFormatDefaut());
            entity.setLongueurSequence(typeDoc.getLongueurSequenceDefaut());
            entity.setTypeReinitialisation(TypeReinitialisationCodification.ANNUELLE);
            entity.setActif(true);
            entity.setDateDerniereMaj(LocalDateTime.now());
            repository.save(entity);
        }
    }

    @Transactional
    public String genererNumero(TypeDocumentCodification typeDoc) {
        return genererNumero(typeDoc, getCurrentTenantId());
    }

    @Transactional
    public String genererNumero(TypeDocumentCodification typeDoc, Long pointDeVenteId) {
        Long tenantId = (pointDeVenteId != null) ? pointDeVenteId : getCurrentTenantId();
        LocalDate now = LocalDate.now();
        int anneeCourante = now.getYear();
        int moisCourant = now.getMonthValue();

        // Verrou pessimiste thread-safe
        CodificationDocument codif = repository.findByPointDeVenteIdAndTypeDocumentForUpdate(tenantId, typeDoc)
                .orElseGet(() -> {
                    CodificationDocument nouveau = new CodificationDocument(tenantId, typeDoc);
                    return repository.save(nouveau);
                });

        // Gestion de la réinitialisation
        if (codif.getTypeReinitialisation() == TypeReinitialisationCodification.ANNUELLE) {
            if (codif.getAnneeCourante() == null || codif.getAnneeCourante() != anneeCourante) {
                codif.setAnneeCourante(anneeCourante);
                codif.setDernierNumero(0L);
            }
        } else if (codif.getTypeReinitialisation() == TypeReinitialisationCodification.MENSUELLE) {
            if (codif.getAnneeCourante() == null || codif.getAnneeCourante() != anneeCourante
                    || codif.getMoisCourant() == null || codif.getMoisCourant() != moisCourant) {
                codif.setAnneeCourante(anneeCourante);
                codif.setMoisCourant(moisCourant);
                codif.setDernierNumero(0L);
            }
        }

        long nouveauNumero = codif.getDernierNumero() + 1;
        codif.setDernierNumero(nouveauNumero);
        codif.setAnneeCourante(anneeCourante);
        codif.setMoisCourant(moisCourant);
        codif.setDateDerniereMaj(LocalDateTime.now());
        repository.save(codif);

        String code = formaterCode(
                codif.getModeleFormat(),
                codif.getPrefixe(),
                anneeCourante,
                moisCourant,
                nouveauNumero,
                codif.getLongueurSequence()
        );

        log.info("Codification générée pour tenant {} - type {}: {}", tenantId, typeDoc, code);
        return code;
    }

    public String genererApercu(CodificationDocumentDTO dto) {
        if (dto == null) return "";
        LocalDate now = LocalDate.now();
        String prefixe = (dto.getPrefixe() != null && !dto.getPrefixe().isBlank())
                ? dto.getPrefixe().trim().toUpperCase()
                : "DOC";
        String modele = (dto.getModeleFormat() != null && !dto.getModeleFormat().isBlank())
                ? dto.getModeleFormat().trim()
                : "{PREFIX}-{AAAA}-{NUM}";
        int longueur = (dto.getLongueurSequence() != null && dto.getLongueurSequence() > 0)
                ? dto.getLongueurSequence()
                : 3;
        long num = (dto.getProchainNumero() != null && dto.getProchainNumero() > 0)
                ? dto.getProchainNumero()
                : 1L;

        return formaterCode(modele, prefixe, now.getYear(), now.getMonthValue(), num, longueur);
    }

    public String formaterCode(String modele, String prefixe, int annee, int mois, long numero, int longueurSequence) {
        String paddedNum = String.format("%0" + longueurSequence + "d", numero);
        String resultat = (modele != null && !modele.isBlank()) ? modele : "{PREFIX}-{AAAA}-{NUM}";

        resultat = resultat.replaceAll("(?i)\\{prefix\\}", prefixe != null ? prefixe : "");
        resultat = resultat.replaceAll("(?i)\\{(aaaa|yyyy)\\}", String.valueOf(annee));
        resultat = resultat.replaceAll("(?i)\\{(aa|yy)\\}", String.format("%02d", annee % 100));
        resultat = resultat.replaceAll("(?i)\\{mm\\}", String.format("%02d", mois));
        resultat = resultat.replaceAll("(?i)\\{(num|n)\\}", paddedNum);

        // Fallback de sécurité si le format n'inclut pas le numéro ou le préfixe
        if (!resultat.contains(paddedNum)) {
            resultat = resultat + "-" + paddedNum;
        }
        return resultat;
    }

    private synchronized void garantirInitialisation(Long tenantId) {
        for (TypeDocumentCodification typeDoc : TypeDocumentCodification.values()) {
            if (!repository.existsByPointDeVenteIdAndTypeDocument(tenantId, typeDoc)) {
                CodificationDocument nouveau = new CodificationDocument(tenantId, typeDoc);
                repository.save(nouveau);
            }
        }
    }

    private CodificationDocumentDTO toDTO(CodificationDocument entity) {
        CodificationDocumentDTO dto = new CodificationDocumentDTO();
        dto.setId(entity.getId());
        dto.setTypeDocument(entity.getTypeDocument());
        dto.setLibelleDocument(entity.getTypeDocument().getLibelle());
        dto.setPrefixe(entity.getPrefixe());
        dto.setModeleFormat(entity.getModeleFormat());
        dto.setLongueurSequence(entity.getLongueurSequence());
        dto.setDernierNumero(entity.getDernierNumero());
        dto.setProchainNumero(entity.getDernierNumero() + 1);
        dto.setTypeReinitialisation(entity.getTypeReinitialisation());
        dto.setActif(entity.getActif());

        LocalDate now = LocalDate.now();
        int annee = entity.getAnneeCourante() != null ? entity.getAnneeCourante() : now.getYear();
        int mois = entity.getMoisCourant() != null ? entity.getMoisCourant() : now.getMonthValue();
        dto.setApercu(formaterCode(
                entity.getModeleFormat(),
                entity.getPrefixe(),
                annee,
                mois,
                dto.getProchainNumero(),
                entity.getLongueurSequence()
        ));
        return dto;
    }
}
