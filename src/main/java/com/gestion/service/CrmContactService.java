package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.CrmContact;
import com.gestion.repository.CrmContactRepository;
import com.gestion.repository.CrmEntrepriseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CrmContactService {

    private final CrmContactRepository contactRepository;
    private final CrmEntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final CrmActiviteService activiteService;

    public CrmContactService(CrmContactRepository contactRepository,
                             CrmEntrepriseRepository entrepriseRepository,
                             UserRepository userRepository,
                             CrmActiviteService activiteService) {
        this.contactRepository = contactRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.userRepository = userRepository;
        this.activiteService = activiteService;
    }

    public CrmContact creerContact(CrmContact contact) {
        Long tenantId = TenantContext.getCurrentTenant();
        contact.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        contact.setDateCreation(LocalDateTime.now());
        contact.setDateModification(LocalDateTime.now());

        if (contact.getEntrepriseId() != null) {
            entrepriseRepository.findByIdAndPointDeVenteId(contact.getEntrepriseId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(contact::setEntreprise);
        }

        if (contact.getCommercialResponsableId() != null) {
            userRepository.findById(contact.getCommercialResponsableId())
                    .ifPresent(contact::setCommercialResponsable);
        } else if (contact.getEntreprise() != null && contact.getEntreprise().getCommercialResponsable() != null) {
            // Hériter du commercial de l'entreprise si non spécifié
            contact.setCommercialResponsable(contact.getEntreprise().getCommercialResponsable());
        }

        CrmContact saved = contactRepository.save(contact);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.NOTE,
                "Création du contact",
                "Contact " + saved.getNomComplet() + " (" + (saved.getFonction() != null ? saved.getFonction() : "Sans fonction") + ") ajouté",
                saved.getEntreprise(), saved, null, null, saved.getCommercialResponsable()
        );

        return saved;
    }

    public CrmContact modifierContact(Long id, CrmContact details) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmContact existing = contactRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Contact non trouvé avec l'id : " + id));

        existing.setNom(details.getNom());
        existing.setPrenom(details.getPrenom());
        existing.setFonction(details.getFonction());
        existing.setEmail(details.getEmail());
        existing.setTelephone(details.getTelephone());
        existing.setMobile(details.getMobile());
        existing.setSourceAcquisition(details.getSourceAcquisition());
        existing.setStatut(details.getStatut());
        existing.setPrincipal(details.getPrincipal());
        existing.setDateModification(LocalDateTime.now());

        if (details.getEntrepriseId() != null) {
            entrepriseRepository.findByIdAndPointDeVenteId(details.getEntrepriseId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(existing::setEntreprise);
        }

        if (details.getCommercialResponsableId() != null) {
            userRepository.findById(details.getCommercialResponsableId())
                    .ifPresent(existing::setCommercialResponsable);
        }

        return contactRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<CrmContact> getAllContacts() {
        Long tenantId = TenantContext.getCurrentTenant();
        return contactRepository.findByPointDeVenteIdOrderByNomAsc(tenantId != null ? tenantId : 1L);
    }

    @Transactional(readOnly = true)
    public CrmContact getContactById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return contactRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Contact non trouvé avec l'id : " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmContact> getContactsByEntreprise(Long entrepriseId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return contactRepository.findByPointDeVenteIdAndEntrepriseId(tenantId != null ? tenantId : 1L, entrepriseId);
    }

    @Transactional(readOnly = true)
    public List<CrmContact> searchContacts(String query) {
        Long tenantId = TenantContext.getCurrentTenant();
        return contactRepository.searchContacts(query, tenantId != null ? tenantId : 1L);
    }

    @Transactional(readOnly = true)
    public List<CrmContact> detecterDoublons(String email, String telephone) {
        Long tenantId = TenantContext.getCurrentTenant();
        return contactRepository.findDoublonsPotentiels(email, telephone, tenantId != null ? tenantId : 1L);
    }
}
