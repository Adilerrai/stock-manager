package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ClientSearchCriteria;
import com.gestion.persistent.enums.CategorieClient;
import com.gestion.persistent.model.Client;
import com.gestion.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Page<Client> searchClients(ClientSearchCriteria criteria, Pageable pageable) {
        return clientRepository.findByCriteria(criteria, pageable);
    }

    public Client creerClient(Client client) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (client.getPointDeVenteId() == null) {
            client.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        }
        client.setDateCreation(LocalDateTime.now());
        if (client.getNom() != null && client.getPrenom() != null) {
            client.setNomComplet(client.getPrenom() + " " + client.getNom());
        }
        if (client.getCreditAutorise() == null) {
            client.setCreditAutorise(BigDecimal.ZERO);
        }
        if (client.getCreditUtilise() == null) {
            client.setCreditUtilise(BigDecimal.ZERO);
        }
        if (client.getRemiseDefaut() == null) {
            client.setRemiseDefaut(BigDecimal.ZERO);
        }
        if (client.getPointsFidelite() == null) {
            client.setPointsFidelite(0);
        }
        if (client.getDelaiPaiementJours() == null) {
            client.setDelaiPaiementJours(30);
        }
        return clientRepository.save(client);
    }

    public Client modifierClient(Long id, Client clientModifie) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        client.setNom(clientModifie.getNom());
        client.setPrenom(clientModifie.getPrenom());
        if (client.getNom() != null && client.getPrenom() != null) {
            client.setNomComplet(client.getPrenom() + " " + client.getNom());
        }
        client.setTelephone(clientModifie.getTelephone());
        client.setEmail(clientModifie.getEmail());
        client.setAdresse(clientModifie.getAdresse());
        client.setVille(clientModifie.getVille());
        client.setCodePostal(clientModifie.getCodePostal());
        client.setCategorie(clientModifie.getCategorie());
        client.setNumeroRegistreCommerce(clientModifie.getNumeroRegistreCommerce());
        client.setNumeroIdentificationFiscale(clientModifie.getNumeroIdentificationFiscale());
        client.setIce(clientModifie.getIce());
        client.setTarif(clientModifie.getTarif());
        client.setDelaiPaiementJours(clientModifie.getDelaiPaiementJours() != null ? clientModifie.getDelaiPaiementJours() : 30);
        client.setRemiseDefaut(clientModifie.getRemiseDefaut() != null ? clientModifie.getRemiseDefaut() : BigDecimal.ZERO);
        client.setCommercial(clientModifie.getCommercial());
        client.setCreditAutorise(clientModifie.getCreditAutorise() != null ? clientModifie.getCreditAutorise() : BigDecimal.ZERO);
        if (clientModifie.getCreditUtilise() != null) {
            client.setCreditUtilise(clientModifie.getCreditUtilise());
        } else if (client.getCreditUtilise() == null) {
            client.setCreditUtilise(BigDecimal.ZERO);
        }
        client.setNotes(clientModifie.getNotes());

        return clientRepository.save(client);
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
    }

    public List<Client> getAllClients() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByPointDeVenteId(tenantId);
        }
        return clientRepository.findAll();
    }

    public List<Client> getClientsActifs() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByActifAndPointDeVenteId(true, tenantId);
        }
        return clientRepository.findByActif(true);
    }

    public List<Client> getClientsByCategorie(CategorieClient categorie) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByPointDeVenteIdAndCategorie(tenantId, categorie);
        }
        return clientRepository.findByCategorie(categorie);
    }

    public List<Client> rechercherClients(String search) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.searchClients(search, tenantId);
        }
        return clientRepository.searchClients(search);
    }

    public Client findByTelephone(String telephone) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByTelephoneAndPointDeVenteId(telephone, tenantId).orElse(null);
        }
        return clientRepository.findByTelephone(telephone).orElse(null);
    }

    public void desactiverClient(Long id) {
        Client client = getClientById(id);
        client.setActif(false);
        clientRepository.save(client);
    }

    public void activerClient(Long id) {
        Client client = getClientById(id);
        client.setActif(true);
        clientRepository.save(client);
    }

    public void augmenterCreditUtilise(Long clientId, BigDecimal montant) {
        Client client = getClientById(clientId);
        BigDecimal utilise = client.getCreditUtilise() != null ? client.getCreditUtilise() : BigDecimal.ZERO;
        BigDecimal mt = montant != null ? montant : BigDecimal.ZERO;
        client.setCreditUtilise(utilise.add(mt));
        clientRepository.save(client);
    }

    public void diminuerCreditUtilise(Long clientId, BigDecimal montant) {
        Client client = getClientById(clientId);
        BigDecimal utilise = client.getCreditUtilise() != null ? client.getCreditUtilise() : BigDecimal.ZERO;
        BigDecimal mt = montant != null ? montant : BigDecimal.ZERO;
        BigDecimal nouveauCredit = utilise.subtract(mt);
        if (nouveauCredit.compareTo(BigDecimal.ZERO) < 0) {
            nouveauCredit = BigDecimal.ZERO;
        }
        client.setCreditUtilise(nouveauCredit);
        clientRepository.save(client);
    }

    public void ajouterPointsFidelite(Long clientId, Integer points) {
        Client client = getClientById(clientId);
        int current = client.getPointsFidelite() != null ? client.getPointsFidelite() : 0;
        int pts = points != null ? points : 0;
        client.setPointsFidelite(current + pts);
        clientRepository.save(client);
    }

    public void mettreAJourDerniereVisite(Long clientId) {
        Client client = getClientById(clientId);
        client.setDateDerniereVisite(LocalDateTime.now());
        clientRepository.save(client);
    }

    public List<Client> getClientsAvecDepassementCredit() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findClientsAvecDepassementCredit(tenantId);
        }
        return clientRepository.findClientsAvecDepassementCredit();
    }
}


