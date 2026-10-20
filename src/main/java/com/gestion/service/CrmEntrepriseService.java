package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.StatutEntreprise;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.CrmEntreprise;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.CrmEntrepriseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CrmEntrepriseService {

    private final CrmEntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CrmActiviteService activiteService;

    public CrmEntrepriseService(CrmEntrepriseRepository entrepriseRepository,
                                UserRepository userRepository,
                                ClientRepository clientRepository,
                                CrmActiviteService activiteService) {
        this.entrepriseRepository = entrepriseRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.activiteService = activiteService;
    }

    public CrmEntreprise creerEntreprise(CrmEntreprise entreprise) {
        Long tenantId = TenantContext.getCurrentTenant();
        entreprise.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        entreprise.setDateCreation(LocalDateTime.now());
        entreprise.setDateModification(LocalDateTime.now());

        if (entreprise.getCommercialResponsableId() != null) {
            userRepository.findById(entreprise.getCommercialResponsableId())
                    .ifPresent(entreprise::setCommercialResponsable);
        }

        if (entreprise.getClientId() != null) {
            clientRepository.findById(entreprise.getClientId())
                    .ifPresent(entreprise::setClient);
        }

        CrmEntreprise saved = entrepriseRepository.save(entreprise);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.NOTE,
                "Création de la fiche entreprise",
                "Entreprise " + saved.getNom() + " ajoutée au CRM avec le statut " + saved.getStatut(),
                saved, null, null, null, saved.getCommercialResponsable()
        );

        return saved;
    }

    public CrmEntreprise modifierEntreprise(Long id, CrmEntreprise details) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmEntreprise existing = entrepriseRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise non trouvée avec l'id : " + id));

        existing.setNom(details.getNom());
        existing.setSecteurActivite(details.getSecteurActivite());
        existing.setTailleEntreprise(details.getTailleEntreprise());
        existing.setAdresse(details.getAdresse());
        existing.setVille(details.getVille());
        existing.setCodePostal(details.getCodePostal());
        existing.setPays(details.getPays());
        existing.setTelephone(details.getTelephone());
        existing.setEmail(details.getEmail());
        existing.setSiteWeb(details.getSiteWeb());
        existing.setIce(details.getIce());
        existing.setStatut(details.getStatut());
        existing.setDateModification(LocalDateTime.now());

        if (details.getCommercialResponsableId() != null) {
            userRepository.findById(details.getCommercialResponsableId())
                    .ifPresent(existing::setCommercialResponsable);
        } else if (details.getCommercialResponsable() != null) {
            existing.setCommercialResponsable(details.getCommercialResponsable());
        }

        return entrepriseRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<CrmEntreprise> getAllEntreprises() {
        Long tenantId = TenantContext.getCurrentTenant();
        return entrepriseRepository.findByPointDeVenteIdOrderByNomAsc(tenantId != null ? tenantId : 1L);
    }

    @Transactional(readOnly = true)
    public CrmEntreprise getEntrepriseById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return entrepriseRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise non trouvée avec l'id : " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmEntreprise> searchEntreprises(String query) {
        Long tenantId = TenantContext.getCurrentTenant();
        return entrepriseRepository.searchEntreprises(query, tenantId != null ? tenantId : 1L);
    }

    @Transactional(readOnly = true)
    public List<CrmEntreprise> detecterDoublons(String nom, String ice) {
        Long tenantId = TenantContext.getCurrentTenant();
        return entrepriseRepository.findDoublonsPotentiels(nom, ice, tenantId != null ? tenantId : 1L);
    }

    public Client convertirEnClientErp(Long entrepriseId) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmEntreprise entreprise = getEntrepriseById(entrepriseId);

        if (entreprise.getClient() != null) {
            return entreprise.getClient(); // Déjà convertie
        }

        Client client = new Client();
        client.setNom(entreprise.getNom());
        client.setNomComplet(entreprise.getNom());
        client.setAdresse(entreprise.getAdresse());
        client.setVille(entreprise.getVille());
        client.setCodePostal(entreprise.getCodePostal());
        client.setTelephone(entreprise.getTelephone());
        client.setEmail(entreprise.getEmail());
        client.setIce(entreprise.getIce());
        client.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        if (entreprise.getCommercialResponsable() != null) {
            client.setCommercial(entreprise.getCommercialResponsable());
        }

        Client savedClient = clientRepository.save(client);

        entreprise.setClient(savedClient);
        entreprise.setStatut(StatutEntreprise.CLIENT);
        entreprise.setDateModification(LocalDateTime.now());
        entrepriseRepository.save(entreprise);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.NOTE,
                "Conversion en Client ERP officiel",
                "L'entreprise a été convertie en fiche client ERP (Code: " + savedClient.getId() + ")",
                entreprise, null, null, null, entreprise.getCommercialResponsable()
        );

        return savedClient;
    }
}
