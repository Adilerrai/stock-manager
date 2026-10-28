package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.dto.UserResponse;
import com.acommon.persistant.enums.TypeUtilisateur;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.ClientSearchCriteria;
import com.gestion.persistent.dto.CreerCompteClientRequest;
import com.gestion.persistent.enums.CategorieClient;
import com.gestion.persistent.model.Client;
import com.gestion.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PointDeVenteRepository pointDeVenteRepository,
                         PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<Client> searchClients(ClientSearchCriteria criteria, Pageable pageable) {
        return clientRepository.findByCriteria(criteria, pageable);
    }

    public Client creerClient(Client client) {
        Long tenantId = TenantContext.getCurrentTenant();
        client.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        client.setDateCreation(LocalDateTime.now());
        if (client.getNom() != null && client.getPrenom() != null) {
            client.setNomComplet(client.getPrenom() + " " + client.getNom());
        } else if (client.getNomComplet() == null && client.getNom() != null) {
            client.setNomComplet(client.getNom());
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

        // Rattachement sécurisé du commercial du même tenant
        Long commId = client.getCommercialId();
        if (commId != null) {
            userRepository.findById(commId).ifPresent(user -> {
                Long targetTenant = tenantId != null ? tenantId : 1L;
                if (targetTenant.equals(user.getTenantId()) || targetTenant.equals(user.getPointDeVenteId())) {
                    client.setCommercial(user);
                }
            });
        }

        return clientRepository.save(client);
    }

    public Client modifierClient(Long id, Client clientModifie) {
        Client client = getClientById(id);

        client.setNom(clientModifie.getNom());
        client.setPrenom(clientModifie.getPrenom());
        if (client.getNom() != null && client.getPrenom() != null) {
            client.setNomComplet(client.getPrenom() + " " + client.getNom());
        } else if (clientModifie.getNomComplet() != null) {
            client.setNomComplet(clientModifie.getNomComplet());
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

        Long commId = clientModifie.getCommercialId();
        if (commId == null && clientModifie.getCommercial() != null) {
            commId = clientModifie.getCommercial().getId();
        }
        if (commId != null) {
            Long tenantId = TenantContext.getCurrentTenant();
            Long targetTenant = tenantId != null ? tenantId : client.getPointDeVenteId();
            User commercial = userRepository.findById(commId).orElse(null);
            if (commercial != null && (targetTenant == null || targetTenant.equals(commercial.getTenantId()) || targetTenant.equals(commercial.getPointDeVenteId()))) {
                client.setCommercial(commercial);
            }
        } else {
            client.setCommercial(null);
        }

        client.setCreditAutorise(clientModifie.getCreditAutorise() != null ? clientModifie.getCreditAutorise() : BigDecimal.ZERO);
        if (clientModifie.getCreditUtilise() != null) {
            client.setCreditUtilise(clientModifie.getCreditUtilise());
        } else if (client.getCreditUtilise() == null) {
            client.setCreditUtilise(BigDecimal.ZERO);
        }
        client.setNotes(clientModifie.getNotes());

        return clientRepository.save(client);
    }

    /**
     * Crée et rattache un compte utilisateur (User) pour permettre au Client d'accéder à son Espace Client.
     * Si l'ICE du client n'est pas encore renseigné, il doit être fourni dans la requête (ou préalablement sur la fiche client).
     */
    public UserResponse creerCompteUtilisateurClient(Long clientId, CreerCompteClientRequest request) {
        Client client = getClientById(clientId);

        // Si l'ICE est transmis dans la requête, on met à jour le client
        if (request != null && request.getIce() != null && !request.getIce().trim().isEmpty()) {
            client.setIce(request.getIce().trim());
            clientRepository.save(client);
        }

        // Vérifier que l'ICE est bien renseigné sur la fiche client
        if (client.getIce() == null || client.getIce().trim().isEmpty()) {
            throw new CommonException(
                    "L'ICE du client doit être renseigné avant de pouvoir créer son compte utilisateur Espace Client.",
                    HttpStatus.BAD_REQUEST);
        }

        // Vérifier si un utilisateur est déjà rattaché à ce client
        Optional<User> existingClientUser = Optional.empty();
        if (client.getUserId() != null) {
            existingClientUser = userRepository.findById(client.getUserId());
        }
        if (existingClientUser.isEmpty()) {
            existingClientUser = userRepository.findFirstByClientId(client.getId());
        }
        if (existingClientUser.isPresent()) {
            throw new CommonException(
                    "Ce client dispose déjà d'un compte utilisateur (" + existingClientUser.get().getEmail() + ").",
                    HttpStatus.BAD_REQUEST);
        }

        String email = (request != null && request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail().trim()
                : (client.getEmail() != null && !client.getEmail().isBlank() ? client.getEmail().trim() : null);

        if (email == null || email.isBlank()) {
            throw new CommonException("L'adresse email est obligatoire pour créer le compte Espace Client.", HttpStatus.BAD_REQUEST);
        }

        String username = (request != null && request.getUsername() != null && !request.getUsername().isBlank())
                ? request.getUsername().trim()
                : email;

        String rawPassword = (request != null && request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : "Client@" + client.getIce().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException("Un utilisateur avec cet email existe déjà : " + email, HttpStatus.BAD_REQUEST);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new CommonException("Un utilisateur avec cet identifiant existe déjà : " + username, HttpStatus.BAD_REQUEST);
        }

        Role roleClient = roleRepository.findByNom("ROLE_CLIENT")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setNom("ROLE_CLIENT");
                    return roleRepository.save(r);
                });

        Long tenantId = client.getPointDeVenteId() != null
                ? client.getPointDeVenteId()
                : (TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : 1L);

        PointDeVente pdv = pointDeVenteRepository.findById(tenantId)
                .or(() -> pointDeVenteRepository.findByTenantId(tenantId))
                .orElse(null);

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        String nomComplet = (request != null && request.getNomComplet() != null && !request.getNomComplet().isBlank())
                ? request.getNomComplet().trim()
                : (client.getNomComplet() != null && !client.getNomComplet().isBlank() ? client.getNomComplet() : client.getNom());
        user.setNomComplet(nomComplet);
        user.setTelephone((request != null && request.getTelephone() != null && !request.getTelephone().isBlank())
                ? request.getTelephone().trim()
                : client.getTelephone());
        user.setRole(roleClient);
        user.setUserType(TypeUtilisateur.CLIENT);
        user.setClientId(client.getId());
        user.setTenantId(tenantId);
        user.setPointDeVente(pdv);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setMustChangePassword(true);

        User savedUser = userRepository.save(user);

        client.setUserId(savedUser.getId());
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            client.setEmail(email);
        }
        clientRepository.save(client);

        return mapUserToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getCompteUtilisateurClient(Long clientId) {
        Client client = getClientById(clientId);
        Optional<User> userOpt = Optional.empty();
        if (client.getUserId() != null) {
            userOpt = userRepository.findById(client.getUserId());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstByClientId(client.getId());
        }
        User user = userOpt.orElseThrow(() -> new CommonException("Aucun compte utilisateur n'est associé à ce client.", HttpStatus.NOT_FOUND));
        return mapUserToResponse(user);
    }

    private UserResponse mapUserToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setNomComplet(user.getNomComplet());
        response.setTelephone(user.getTelephone());
        response.setGenre(user.getGenre());
        response.setRole(user.getRole() != null ? user.getRole().getNom() : null);
        response.setTenantId(user.getTenantId());
        response.setPointDeVenteId(user.getPointDeVenteId());
        if (user.getPointDeVente() != null) {
            response.setNomPointDeVente(user.getPointDeVente().getNomPointDeVente());
        }
        response.setEnabled(user.isEnabled());
        response.setMustChangePassword(user.getMustChangePassword());
        response.setUserType(user.getUserType());
        response.setClientId(user.getClientId());
        return response;
    }

    public Client getClientById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByIdAndPointDeVenteId(id, tenantId)
                    .orElseGet(() -> clientRepository.findById(id)
                            .orElseThrow(() -> new com.acommon.exception.ResourceNotFoundException("Client", "id", id)));
        }
        return clientRepository.findById(id)
                .orElseThrow(() -> new com.acommon.exception.ResourceNotFoundException("Client", "id", id));
    }

    public List<Client> getAllClients() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByPointDeVenteId(tenantId);
        }
        return List.of();
    }

    public List<Client> getClientsActifs() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByActifAndPointDeVenteId(true, tenantId);
        }
        return List.of();
    }

    public List<Client> getClientsByCategorie(CategorieClient categorie) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByPointDeVenteIdAndCategorie(tenantId, categorie);
        }
        return List.of();
    }

    public List<Client> rechercherClients(String search) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.searchClients(search, tenantId);
        }
        return List.of();
    }

    public Client findByTelephone(String telephone) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return clientRepository.findByTelephoneAndPointDeVenteId(telephone, tenantId).orElse(null);
        }
        return null;
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
        return List.of();
    }
}
