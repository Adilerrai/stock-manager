package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.enums.CategorieClient;
import com.gestion.persistent.model.Client;
import com.gestion.repository.ClientRepository;
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

    public Client creerClient(Client client) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (client.getPointDeVenteId() == null) {
            client.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        }
        client.setDateCreation(LocalDateTime.now());
        if (client.getNom() != null && client.getPrenom() != null) {
            client.setNomComplet(client.getPrenom() + " " + client.getNom());
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
        client.setDelaiPaiementJours(clientModifie.getDelaiPaiementJours());
        client.setRemiseDefaut(clientModifie.getRemiseDefaut());
        client.setCommercial(clientModifie.getCommercial());
        client.setCreditAutorise(clientModifie.getCreditAutorise());
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
        client.setCreditUtilise(client.getCreditUtilise().add(montant));
        clientRepository.save(client);
    }

    public void diminuerCreditUtilise(Long clientId, BigDecimal montant) {
        Client client = getClientById(clientId);
        BigDecimal nouveauCredit = client.getCreditUtilise().subtract(montant);
        if (nouveauCredit.compareTo(BigDecimal.ZERO) < 0) {
            nouveauCredit = BigDecimal.ZERO;
        }
        client.setCreditUtilise(nouveauCredit);
        clientRepository.save(client);
    }

    public void ajouterPointsFidelite(Long clientId, Integer points) {
        Client client = getClientById(clientId);
        client.setPointsFidelite(client.getPointsFidelite() + points);
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


