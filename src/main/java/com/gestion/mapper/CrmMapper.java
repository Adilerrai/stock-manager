package com.gestion.mapper;

import com.gestion.persistent.dto.*;
import com.gestion.persistent.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CrmMapper {

    // --- Entreprise ---
    public CrmEntrepriseDTO toDto(CrmEntreprise entity) {
        if (entity == null) return null;
        CrmEntrepriseDTO dto = new CrmEntrepriseDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        dto.setNom(entity.getNom());
        dto.setSecteurActivite(entity.getSecteurActivite());
        dto.setTailleEntreprise(entity.getTailleEntreprise());
        dto.setAdresse(entity.getAdresse());
        dto.setVille(entity.getVille());
        dto.setCodePostal(entity.getCodePostal());
        dto.setPays(entity.getPays());
        dto.setTelephone(entity.getTelephone());
        dto.setEmail(entity.getEmail());
        dto.setSiteWeb(entity.getSiteWeb());
        dto.setIce(entity.getIce());
        dto.setStatut(entity.getStatut());
        if (entity.getCommercialResponsable() != null) {
            dto.setCommercialResponsableId(entity.getCommercialResponsable().getId());
            dto.setCommercialResponsableNom(entity.getCommercialResponsable().getNomComplet() != null ?
                    entity.getCommercialResponsable().getNomComplet() : entity.getCommercialResponsable().getUsername());
        }
        if (entity.getClient() != null) {
            dto.setClientId(entity.getClient().getId());
        }
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        return dto;
    }

    public CrmEntreprise toEntity(CrmEntrepriseDTO dto) {
        if (dto == null) return null;
        CrmEntreprise entity = new CrmEntreprise();
        entity.setId(dto.getId());
        entity.setPointDeVenteId(dto.getPointDeVenteId());
        entity.setNom(dto.getNom());
        entity.setSecteurActivite(dto.getSecteurActivite());
        entity.setTailleEntreprise(dto.getTailleEntreprise());
        entity.setAdresse(dto.getAdresse());
        entity.setVille(dto.getVille());
        entity.setCodePostal(dto.getCodePostal());
        entity.setPays(dto.getPays() != null ? dto.getPays() : "Maroc");
        entity.setTelephone(dto.getTelephone());
        entity.setEmail(dto.getEmail());
        entity.setSiteWeb(dto.getSiteWeb());
        entity.setIce(dto.getIce());
        entity.setStatut(dto.getStatut());
        entity.setCommercialResponsableId(dto.getCommercialResponsableId());
        entity.setClientId(dto.getClientId());
        return entity;
    }

    // --- Contact ---
    public CrmContactDTO toDto(CrmContact entity) {
        if (entity == null) return null;
        CrmContactDTO dto = new CrmContactDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        if (entity.getEntreprise() != null) {
            dto.setEntrepriseId(entity.getEntreprise().getId());
            dto.setEntrepriseNom(entity.getEntreprise().getNom());
        }
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setNomComplet(entity.getNomComplet());
        dto.setFonction(entity.getFonction());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setMobile(entity.getMobile());
        dto.setSourceAcquisition(entity.getSourceAcquisition());
        dto.setStatut(entity.getStatut());
        if (entity.getCommercialResponsable() != null) {
            dto.setCommercialResponsableId(entity.getCommercialResponsable().getId());
            dto.setCommercialResponsableNom(entity.getCommercialResponsable().getNomComplet() != null ?
                    entity.getCommercialResponsable().getNomComplet() : entity.getCommercialResponsable().getUsername());
        }
        dto.setPrincipal(entity.getPrincipal());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        return dto;
    }

    public CrmContact toEntity(CrmContactDTO dto) {
        if (dto == null) return null;
        CrmContact entity = new CrmContact();
        entity.setId(dto.getId());
        entity.setPointDeVenteId(dto.getPointDeVenteId());
        entity.setEntrepriseId(dto.getEntrepriseId());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setNomComplet(dto.getNomComplet());
        entity.setFonction(dto.getFonction());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setMobile(dto.getMobile());
        entity.setSourceAcquisition(dto.getSourceAcquisition());
        entity.setStatut(dto.getStatut());
        entity.setCommercialResponsableId(dto.getCommercialResponsableId());
        entity.setPrincipal(dto.getPrincipal());
        return entity;
    }

    // --- Opportunité ---
    public CrmOpportuniteDTO toDto(CrmOpportunite entity) {
        if (entity == null) return null;
        CrmOpportuniteDTO dto = new CrmOpportuniteDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        dto.setTitre(entity.getTitre());
        if (entity.getEntreprise() != null) {
            dto.setEntrepriseId(entity.getEntreprise().getId());
            dto.setEntrepriseNom(entity.getEntreprise().getNom());
        }
        if (entity.getContact() != null) {
            dto.setContactId(entity.getContact().getId());
            dto.setContactNomComplet(entity.getContact().getNomComplet());
        }
        if (entity.getCommercial() != null) {
            dto.setCommercialId(entity.getCommercial().getId());
            dto.setCommercialNom(entity.getCommercial().getNomComplet() != null ?
                    entity.getCommercial().getNomComplet() : entity.getCommercial().getUsername());
        }
        dto.setMontantEstime(entity.getMontantEstime());
        dto.setProbabilite(entity.getProbabilite());
        dto.setDateCloturePrevue(entity.getDateCloturePrevue());
        dto.setEtape(entity.getEtape());
        dto.setMotifPerte(entity.getMotifPerte());
        dto.setNotes(entity.getNotes());
        if (entity.getDevis() != null) {
            dto.setDevisId(entity.getDevis().getId());
        }
        dto.setDateClotureEffective(entity.getDateClotureEffective());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        return dto;
    }

    public CrmOpportunite toEntity(CrmOpportuniteDTO dto) {
        if (dto == null) return null;
        CrmOpportunite entity = new CrmOpportunite();
        entity.setId(dto.getId());
        entity.setPointDeVenteId(dto.getPointDeVenteId());
        entity.setTitre(dto.getTitre());
        entity.setEntrepriseId(dto.getEntrepriseId());
        entity.setContactId(dto.getContactId());
        entity.setCommercialId(dto.getCommercialId());
        entity.setMontantEstime(dto.getMontantEstime());
        entity.setProbabilite(dto.getProbabilite());
        entity.setDateCloturePrevue(dto.getDateCloturePrevue());
        entity.setEtape(dto.getEtape());
        entity.setMotifPerte(dto.getMotifPerte());
        entity.setNotes(dto.getNotes());
        entity.setDevisId(dto.getDevisId());
        return entity;
    }

    // --- Visite ---
    public CrmVisiteDTO toDto(CrmVisite entity) {
        if (entity == null) return null;
        CrmVisiteDTO dto = new CrmVisiteDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        if (entity.getEntreprise() != null) {
            dto.setEntrepriseId(entity.getEntreprise().getId());
            dto.setEntrepriseNom(entity.getEntreprise().getNom());
        }
        if (entity.getContact() != null) {
            dto.setContactId(entity.getContact().getId());
            dto.setContactNomComplet(entity.getContact().getNomComplet());
        }
        if (entity.getOpportunite() != null) {
            dto.setOpportuniteId(entity.getOpportunite().getId());
            dto.setOpportuniteTitre(entity.getOpportunite().getTitre());
        }
        if (entity.getCommercial() != null) {
            dto.setCommercialId(entity.getCommercial().getId());
            dto.setCommercialNom(entity.getCommercial().getNomComplet() != null ?
                    entity.getCommercial().getNomComplet() : entity.getCommercial().getUsername());
        }
        dto.setDateVisite(entity.getDateVisite());
        dto.setLieu(entity.getLieu());
        dto.setTypeVisite(entity.getTypeVisite());
        dto.setObjectif(entity.getObjectif());
        dto.setStatut(entity.getStatut());
        dto.setMotifAnnulation(entity.getMotifAnnulation());
        dto.setDateCreation(entity.getDateCreation());
        return dto;
    }

    public CrmVisite toEntity(CrmVisiteDTO dto) {
        if (dto == null) return null;
        CrmVisite entity = new CrmVisite();
        entity.setId(dto.getId());
        entity.setPointDeVenteId(dto.getPointDeVenteId());
        entity.setEntrepriseId(dto.getEntrepriseId());
        entity.setContactId(dto.getContactId());
        entity.setOpportuniteId(dto.getOpportuniteId());
        entity.setCommercialId(dto.getCommercialId());
        entity.setDateVisite(dto.getDateVisite());
        entity.setLieu(dto.getLieu());
        entity.setTypeVisite(dto.getTypeVisite());
        entity.setObjectif(dto.getObjectif());
        entity.setStatut(dto.getStatut());
        entity.setMotifAnnulation(dto.getMotifAnnulation());
        return entity;
    }

    // --- Rapport de Visite ---
    public CrmRapportVisiteDTO toDto(CrmRapportVisite entity) {
        if (entity == null) return null;
        CrmRapportVisiteDTO dto = new CrmRapportVisiteDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        if (entity.getVisite() != null) {
            dto.setVisiteId(entity.getVisite().getId());
            dto.setVisiteLieu(entity.getVisite().getLieu());
            dto.setVisiteDate(entity.getVisite().getDateVisite());
            dto.setVisiteType(entity.getVisite().getTypeVisite() != null ? entity.getVisite().getTypeVisite().name() : null);
            if (entity.getVisite().getEntreprise() != null) {
                dto.setEntrepriseId(entity.getVisite().getEntreprise().getId());
                dto.setEntrepriseNom(entity.getVisite().getEntreprise().getNom());
            }
            if (entity.getVisite().getContact() != null) {
                dto.setContactId(entity.getVisite().getContact().getId());
                dto.setContactNom(entity.getVisite().getContact().getNomComplet());
            }
            if (entity.getVisite().getCommercial() != null) {
                dto.setCommercialId(entity.getVisite().getCommercial().getId());
                dto.setCommercialNom(entity.getVisite().getCommercial().getNomComplet() != null ?
                        entity.getVisite().getCommercial().getNomComplet() : entity.getVisite().getCommercial().getUsername());
            }
        }
        dto.setStatut(entity.getStatut());
        dto.setResume(entity.getResume());
        dto.setInterlocuteursPresents(entity.getInterlocuteursPresents());
        dto.setBesoinsIdentifies(entity.getBesoinsIdentifies());
        dto.setObjectionsSoulevees(entity.getObjectionsSoulevees());
        dto.setResultat(entity.getResultat());
        dto.setProchaineEtape(entity.getProchaineEtape());
        dto.setDureeMinutes(entity.getDureeMinutes());
        dto.setSoumisLe(entity.getSoumisLe());
        if (entity.getValidePar() != null) {
            dto.setValideParUserId(entity.getValidePar().getId());
            dto.setValideParNom(entity.getValidePar().getNomComplet() != null ?
                    entity.getValidePar().getNomComplet() : entity.getValidePar().getUsername());
        }
        dto.setValideLe(entity.getValideLe());
        dto.setMotifRejet(entity.getMotifRejet());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        dto.setVerrouille(entity.isVerrouille());
        return dto;
    }

    public CrmRapportVisite toEntity(CrmRapportVisiteDTO dto) {
        if (dto == null) return null;
        CrmRapportVisite entity = new CrmRapportVisite();
        entity.setId(dto.getId());
        entity.setPointDeVenteId(dto.getPointDeVenteId());
        entity.setVisiteId(dto.getVisiteId());
        entity.setStatut(dto.getStatut());
        entity.setResume(dto.getResume());
        entity.setInterlocuteursPresents(dto.getInterlocuteursPresents());
        entity.setBesoinsIdentifies(dto.getBesoinsIdentifies());
        entity.setObjectionsSoulevees(dto.getObjectionsSoulevees());
        entity.setResultat(dto.getResultat());
        entity.setProchaineEtape(dto.getProchaineEtape());
        entity.setDureeMinutes(dto.getDureeMinutes());
        return entity;
    }

    // --- Tâche ---
    public CrmTacheDTO toDto(CrmTache entity) {
        if (entity == null) return null;
        CrmTacheDTO dto = new CrmTacheDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        if (entity.getVisite() != null) dto.setVisiteId(entity.getVisite().getId());
        if (entity.getRapport() != null) dto.setRapportId(entity.getRapport().getId());
        if (entity.getOpportunite() != null) {
            dto.setOpportuniteId(entity.getOpportunite().getId());
            dto.setOpportuniteTitre(entity.getOpportunite().getTitre());
        }
        if (entity.getEntreprise() != null) {
            dto.setEntrepriseId(entity.getEntreprise().getId());
            dto.setEntrepriseNom(entity.getEntreprise().getNom());
        }
        if (entity.getContact() != null) {
            dto.setContactId(entity.getContact().getId());
            dto.setContactNom(entity.getContact().getNomComplet());
        }
        if (entity.getAssigneA() != null) {
            dto.setAssigneAUserId(entity.getAssigneA().getId());
            dto.setAssigneANom(entity.getAssigneA().getNomComplet() != null ?
                    entity.getAssigneA().getNomComplet() : entity.getAssigneA().getUsername());
        }
        if (entity.getCreePar() != null) {
            dto.setCreeParUserId(entity.getCreePar().getId());
            dto.setCreeParNom(entity.getCreePar().getNomComplet() != null ?
                    entity.getCreePar().getNomComplet() : entity.getCreePar().getUsername());
        }
        dto.setPriorite(entity.getPriorite());
        dto.setStatut(entity.getStatut());
        dto.setDateEcheance(entity.getDateEcheance());
        dto.setDateRealisation(entity.getDateRealisation());
        dto.setDateCreation(entity.getDateCreation());
        dto.setEnRetard(entity.getDateEcheance() != null &&
                entity.getDateEcheance().isBefore(LocalDate.now()) &&
                (entity.getStatut() == com.gestion.persistent.enums.StatutTache.A_FAIRE ||
                 entity.getStatut() == com.gestion.persistent.enums.StatutTache.EN_COURS));
        return dto;
    }

    public CrmTache toEntity(CrmTacheDTO dto) {
        if (dto == null) return null;
        CrmTache entity = new CrmTache();
        entity.setId(dto.getId());
        entity.setPointDeVenteId(dto.getPointDeVenteId());
        entity.setTitre(dto.getTitre());
        entity.setDescription(dto.getDescription());
        entity.setVisiteId(dto.getVisiteId());
        entity.setRapportId(dto.getRapportId());
        entity.setOpportuniteId(dto.getOpportuniteId());
        entity.setEntrepriseId(dto.getEntrepriseId());
        entity.setContactId(dto.getContactId());
        entity.setAssigneAUserId(dto.getAssigneAUserId());
        entity.setCreeParUserId(dto.getCreeParUserId());
        entity.setPriorite(dto.getPriorite());
        entity.setStatut(dto.getStatut());
        entity.setDateEcheance(dto.getDateEcheance());
        return entity;
    }

    // --- Activité ---
    public CrmActiviteDTO toDto(CrmActivite entity) {
        if (entity == null) return null;
        CrmActiviteDTO dto = new CrmActiviteDTO();
        dto.setId(entity.getId());
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        dto.setTypeActivite(entity.getTypeActivite());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        if (entity.getEntreprise() != null) {
            dto.setEntrepriseId(entity.getEntreprise().getId());
            dto.setEntrepriseNom(entity.getEntreprise().getNom());
        }
        if (entity.getContact() != null) {
            dto.setContactId(entity.getContact().getId());
            dto.setContactNom(entity.getContact().getNomComplet());
        }
        if (entity.getOpportunite() != null) {
            dto.setOpportuniteId(entity.getOpportunite().getId());
            dto.setOpportuniteTitre(entity.getOpportunite().getTitre());
        }
        if (entity.getVisite() != null) {
            dto.setVisiteId(entity.getVisite().getId());
        }
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserNom(entity.getUser().getNomComplet() != null ?
                    entity.getUser().getNomComplet() : entity.getUser().getUsername());
        }
        dto.setDateActivite(entity.getDateActivite());
        dto.setMetadataJson(entity.getMetadataJson());
        return dto;
    }
}
