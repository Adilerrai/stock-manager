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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodificationService {

    private static final Logger log = LoggerFactory.getLogger(CodificationService.class);

    private final CodificationDocumentRepository repository;
    private final com.gestion.repository.DepotRepository depotRepository;
    private final com.gestion.repository.NumeroReserveRepository numeroReserveRepository;

    public CodificationService(CodificationDocumentRepository repository,
                               @org.springframework.context.annotation.Lazy com.gestion.repository.DepotRepository depotRepository,
                               com.gestion.repository.NumeroReserveRepository numeroReserveRepository) {
        this.repository = repository;
        this.depotRepository = depotRepository;
        this.numeroReserveRepository = numeroReserveRepository;
    }

    public CodificationService(CodificationDocumentRepository repository,
                               com.gestion.repository.DepotRepository depotRepository) {
        this(repository, depotRepository, null);
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
            if (typeDoc == TypeDocumentCodification.BL_CLIENT || typeDoc == TypeDocumentCodification.BL_FOURNISSEUR) {
                entity.setTypeReinitialisation(TypeReinitialisationCodification.CONTINUE);
            } else {
                entity.setTypeReinitialisation(TypeReinitialisationCodification.ANNUELLE);
            }
            entity.setActif(true);
            entity.setDateDerniereMaj(LocalDateTime.now());
            repository.save(entity);
        }
    }

    public String getCodeDepot(Long depotId, Long tenantId) {
        if (depotId != null && depotRepository != null) {
            return depotRepository.findById(depotId)
                    .map(com.gestion.persistent.model.Depot::getCodeOrDefault)
                    .orElse("DEP");
        }
        if (depotRepository != null) {
            Long effectiveTenant = (tenantId != null) ? tenantId : getCurrentTenantId();
            List<com.gestion.persistent.model.Depot> depots = depotRepository.findByPointDeVenteIdAndActifTrue(effectiveTenant);
            if (!depots.isEmpty()) {
                return depots.get(0).getCodeOrDefault();
            }
        }
        return "TAC";
    }

    @Transactional
    public String genererNumero(TypeDocumentCodification typeDoc) {
        return genererNumero(typeDoc, getCurrentTenantId(), (String) null);
    }

    @Transactional
    public String genererNumero(TypeDocumentCodification typeDoc, Long pointDeVenteId) {
        return genererNumero(typeDoc, pointDeVenteId, (String) null);
    }

    @Transactional
    public String genererNumero(TypeDocumentCodification typeDoc, Long pointDeVenteId, Long depotId) {
        String codeDepot = getCodeDepot(depotId, pointDeVenteId);
        return genererNumero(typeDoc, pointDeVenteId, codeDepot);
    }

    @Transactional
    public String genererNumero(TypeDocumentCodification typeDoc, Long pointDeVenteId, String codeDepot) {
        Long tenantId = (pointDeVenteId != null) ? pointDeVenteId : getCurrentTenantId();

        // 1. Vérifier si un numéro recyclé / réservé est disponible dans la file
        if (numeroReserveRepository != null) {
            java.util.Optional<com.gestion.persistent.model.NumeroReserve> reserveOpt = numeroReserveRepository
                    .findFirstByPointDeVenteIdAndTypeDocumentAndStatutOrderByValeurSequenceAsc(
                            tenantId, typeDoc, com.gestion.persistent.enums.StatutNumeroReserve.DISPONIBLE);

            if (reserveOpt.isPresent()) {
                com.gestion.persistent.model.NumeroReserve reserve = reserveOpt.get();
                reserve.setStatut(com.gestion.persistent.enums.StatutNumeroReserve.UTILISE);
                reserve.setDateUtilisation(LocalDateTime.now());
                numeroReserveRepository.save(reserve);
                log.info("Numéro réservé/recyclé utilisé pour tenant {} - type {}: {}", tenantId, typeDoc, reserve.getNumero());
                return reserve.getNumero();
            }
        }

        LocalDate now = LocalDate.now();
        int anneeCourante = now.getYear();
        int moisCourant = now.getMonthValue();

        // Verrou pessimiste thread-safe
        CodificationDocument codif = repository.findByPointDeVenteIdAndTypeDocumentForUpdate(tenantId, typeDoc)
                .orElseGet(() -> {
                    CodificationDocument nouveau = new CodificationDocument(tenantId, typeDoc);
                    if (typeDoc == TypeDocumentCodification.BL_CLIENT || typeDoc == TypeDocumentCodification.BL_FOURNISSEUR) {
                        nouveau.setTypeReinitialisation(TypeReinitialisationCodification.CONTINUE);
                    }
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

        String effectiveDepotCode = (codeDepot != null && !codeDepot.isBlank())
                ? codeDepot.trim().toUpperCase()
                : getCodeDepot(null, tenantId);

        String code = formaterCode(
                codif.getModeleFormat(),
                codif.getPrefixe(),
                effectiveDepotCode,
                anneeCourante,
                moisCourant,
                nouveauNumero,
                codif.getLongueurSequence()
        );

        log.info("Codification générée pour tenant {} - type {} (dépôt {}): {}", tenantId, typeDoc, effectiveDepotCode, code);
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

        String codeDepot = (dto.getCodeDepot() != null && !dto.getCodeDepot().isBlank())
                ? dto.getCodeDepot().trim().toUpperCase()
                : getCodeDepot(dto.getDepotId(), getCurrentTenantId());

        return formaterCode(modele, prefixe, codeDepot, now.getYear(), now.getMonthValue(), num, longueur);
    }

    public String formaterCode(String modele, String prefixe, int annee, int mois, long numero, int longueurSequence) {
        return formaterCode(modele, prefixe, null, annee, mois, numero, longueurSequence);
    }

    public String formaterCode(String modele, String prefixe, String codeDepot, int annee, int mois, long numero, int longueurSequence) {
        String paddedNum = String.format("%0" + longueurSequence + "d", numero);
        String resultat = (modele != null && !modele.isBlank()) ? modele : "{PREFIX}-{AAAA}-{NUM}";

        String depotVal = (codeDepot != null && !codeDepot.isBlank()) ? codeDepot.trim().toUpperCase() : "";

        // Tokens pour le dépôt
        resultat = resultat.replaceAll("(?i)\\{(depot|code_depot|codedepot|depot_code)\\}", depotVal);
        resultat = resultat.replaceAll("(?i)\\{prefix\\}", prefixe != null ? prefixe : "");
        resultat = resultat.replaceAll("(?i)\\{(aaaa|yyyy)\\}", String.valueOf(annee));
        resultat = resultat.replaceAll("(?i)\\{(aa|yy)\\}", String.format("%02d", annee % 100));
        resultat = resultat.replaceAll("(?i)\\{mm\\}", String.format("%02d", mois));
        resultat = resultat.replaceAll("(?i)\\{(num|n)\\}", paddedNum);

        // Nettoyage de double slash si le dépôt était vide
        if (depotVal.isEmpty()) {
            resultat = resultat.replaceAll("^/+", "").replaceAll("//+", "/");
        }

        // Fallback de sécurité si le format n'inclut pas le numéro
        if (!resultat.contains(paddedNum)) {
            resultat = resultat + "-" + paddedNum;
        }
        return resultat;
    }

    private synchronized void garantirInitialisation(Long tenantId) {
        for (TypeDocumentCodification typeDoc : TypeDocumentCodification.values()) {
            if (!repository.existsByPointDeVenteIdAndTypeDocument(tenantId, typeDoc)) {
                CodificationDocument nouveau = new CodificationDocument(tenantId, typeDoc);
                if (typeDoc == TypeDocumentCodification.BL_CLIENT || typeDoc == TypeDocumentCodification.BL_FOURNISSEUR) {
                    nouveau.setTypeReinitialisation(TypeReinitialisationCodification.CONTINUE);
                }
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

        String sampleDepot = getCodeDepot(null, entity.getPointDeVenteId());
        dto.setCodeDepot(sampleDepot);

        LocalDate now = LocalDate.now();
        int annee = entity.getAnneeCourante() != null ? entity.getAnneeCourante() : now.getYear();
        int mois = entity.getMoisCourant() != null ? entity.getMoisCourant() : now.getMonthValue();
        dto.setApercu(formaterCode(
                entity.getModeleFormat(),
                entity.getPrefixe(),
                sampleDepot,
                annee,
                mois,
                dto.getProchainNumero(),
                entity.getLongueurSequence()
        ));
        return dto;
    }

    @Transactional
    public void libererNumero(TypeDocumentCodification typeDoc, Long pointDeVenteId, String numero, Long docId, String motif) {
        if (numero == null || numero.isBlank()) return;
        Long tenantId = (pointDeVenteId != null) ? pointDeVenteId : getCurrentTenantId();

        // Extraire la séquence numérique si possible
        Long sequenceExtraite = extraireSequence(numero);

        // Vérifier si ce numéro correspondait au dernier numéro généré sur la codification
        java.util.Optional<CodificationDocument> codifOpt = repository.findByPointDeVenteIdAndTypeDocumentForUpdate(tenantId, typeDoc);
        if (codifOpt.isPresent()) {
            CodificationDocument codif = codifOpt.get();
            if (sequenceExtraite != null && sequenceExtraite.equals(codif.getDernierNumero())) {
                // Le compteur principal recule pour revenir à la normale
                codif.setDernierNumero(Math.max(0L, codif.getDernierNumero() - 1));
                codif.setDateDerniereMaj(LocalDateTime.now());
                repository.save(codif);
                log.info("Recul direct du compteur pour tenant {} - type {}: nouveau dernier_numero = {}",
                        tenantId, typeDoc, codif.getDernierNumero());
                return;
            }
        }

        // Sinon, ajouter ce numéro libéré dans la table des numéros réservés/recyclés
        if (numeroReserveRepository != null && !numeroReserveRepository.existsByPointDeVenteIdAndTypeDocumentAndNumeroAndStatut(
                tenantId, typeDoc, numero, com.gestion.persistent.enums.StatutNumeroReserve.DISPONIBLE)) {
            com.gestion.persistent.model.NumeroReserve reserve = new com.gestion.persistent.model.NumeroReserve();
            reserve.setPointDeVenteId(tenantId);
            reserve.setTypeDocument(typeDoc);
            reserve.setNumero(numero);
            reserve.setValeurSequence(sequenceExtraite);
            reserve.setStatut(com.gestion.persistent.enums.StatutNumeroReserve.DISPONIBLE);
            reserve.setDocumentOrigineId(docId);
            reserve.setDocumentOrigineType(typeDoc.name());
            reserve.setMotifLiberation(motif != null ? motif : "Annulation de document");
            reserve.setDateReservation(LocalDateTime.now());
            numeroReserveRepository.save(reserve);
            log.info("Numéro placé dans la réserve pour tenant {} - type {}: {}", tenantId, typeDoc, numero);
        }
    }

    public List<com.gestion.persistent.dto.NumeroReserveDTO> getNumerosReservesDisponibles(TypeDocumentCodification typeDoc, Long pointDeVenteId) {
        if (numeroReserveRepository == null) return Collections.emptyList();
        Long tenantId = (pointDeVenteId != null) ? pointDeVenteId : getCurrentTenantId();
        List<com.gestion.persistent.model.NumeroReserve> reserves = numeroReserveRepository
                .findByPointDeVenteIdAndTypeDocumentAndStatutOrderByValeurSequenceAsc(
                        tenantId, typeDoc, com.gestion.persistent.enums.StatutNumeroReserve.DISPONIBLE);

        return reserves.stream().map(r -> {
            com.gestion.persistent.dto.NumeroReserveDTO dto = new com.gestion.persistent.dto.NumeroReserveDTO();
            dto.setId(r.getId());
            dto.setPointDeVenteId(r.getPointDeVenteId());
            dto.setTypeDocument(r.getTypeDocument());
            dto.setNumero(r.getNumero());
            dto.setValeurSequence(r.getValeurSequence());
            dto.setStatut(r.getStatut());
            dto.setMotifLiberation(r.getMotifLiberation());
            dto.setDocumentOrigineId(r.getDocumentOrigineId());
            dto.setDocumentOrigineType(r.getDocumentOrigineType());
            dto.setDateReservation(r.getDateReservation());
            dto.setDateUtilisation(r.getDateUtilisation());
            dto.setDocumentUtilisationId(r.getDocumentUtilisationId());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void marquerNumeroReserveUtilise(String numero, TypeDocumentCodification typeDoc, Long pointDeVenteId, Long docId) {
        if (numero == null || numeroReserveRepository == null) return;
        Long tenantId = (pointDeVenteId != null) ? pointDeVenteId : getCurrentTenantId();
        numeroReserveRepository.findByPointDeVenteIdAndTypeDocumentAndNumeroAndStatut(
                tenantId, typeDoc, numero, com.gestion.persistent.enums.StatutNumeroReserve.DISPONIBLE)
                .ifPresent(r -> {
                    r.setStatut(com.gestion.persistent.enums.StatutNumeroReserve.UTILISE);
                    r.setDateUtilisation(LocalDateTime.now());
                    r.setDocumentUtilisationId(docId);
                    numeroReserveRepository.save(r);
                });
    }

    private Long extraireSequence(String numero) {
        if (numero == null) return null;
        try {
            // Chercher le dernier bloc de chiffres
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)$").matcher(numero.trim());
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        } catch (Exception ignored) {}
        return null;
    }
}
