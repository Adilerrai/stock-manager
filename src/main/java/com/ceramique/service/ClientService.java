package com.ceramique.service;

import com.acommon.persistant.model.TenantContext;
import com.ceramique.persistent.enums.CategorieClient;
import com.ceramique.persistent.model.Client;
import com.ceramique.repository.ClientRepository;
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
        client.setCreditAutorise(clientModifie.getCreditAutorise());
        client.setNotes(clientModifie.getNotes());

        return clientRepository.save(client);
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
    }

    public List<Client> getAllClients() {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return clientRepository.findByPointDeVenteId(pointDeVenteId);
    }

    public List<Client> getClientsActifs() {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return clientRepository.findByPointDeVenteIdAndActif(pointDeVenteId, true);
    }

    public List<Client> getClientsByCategorie(CategorieClient categorie) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return clientRepository.findByPointDeVenteIdAndCategorie(pointDeVenteId, categorie);
    }

    public List<Client> rechercherClients(String search) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return clientRepository.searchClients(pointDeVenteId, search);
    }

    public Client findByTelephone(String telephone) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return clientRepository.findByTelephoneAndPointDeVenteId(telephone, pointDeVenteId)
                .orElse(null);
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
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return clientRepository.findClientsAvecDepassementCredit(pointDeVenteId);
    }
}

