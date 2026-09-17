package com.acommon.service;

import com.acommon.persistant.dto.EntrepriseRegistrationRequest;
import com.acommon.persistant.dto.EntrepriseResponse;
import com.acommon.persistant.dto.EntrepriseUpdateRequest;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.model.Depot;
import com.gestion.persistent.model.EntrepriseProfile;
import com.gestion.repository.DepotRepository;
import com.gestion.repository.EntrepriseProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EntrepriseManagementService {

    private final PointDeVenteRepository pointDeVenteRepository;
    private final EntrepriseProfileRepository entrepriseProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepotRepository depotRepository;
    private final PasswordEncoder passwordEncoder;

    public EntrepriseManagementService(
            PointDeVenteRepository pointDeVenteRepository,
            EntrepriseProfileRepository entrepriseProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepotRepository depotRepository,
            PasswordEncoder passwordEncoder) {
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.entrepriseProfileRepository = entrepriseProfileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.depotRepository = depotRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EntrepriseResponse creerEntreprise(EntrepriseRegistrationRequest request) {
        // 1. Validation de l'unicité
        if (pointDeVenteRepository.existsByNomPointDeVente(request.getNomEntreprise())) {
            throw new IllegalArgumentException(
                    "Une entreprise avec le nom '" + request.getNomEntreprise() + "' existe déjà");
        }
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec l'email '" + request.getAdminEmail() + "' existe déjà");
        }
        if (userRepository.existsByUsername(request.getAdminUsername())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec l'identifiant '" + request.getAdminUsername() + "' existe déjà");
        }

        // 2. Calcul ou validation du nouveau tenantId unique
        Long newTenantId;
        if (request.getTenantId() != null) {
            if (pointDeVenteRepository.existsByTenantId(request.getTenantId())) {
                throw new IllegalArgumentException(
                        "Une entreprise avec le tenant ID '" + request.getTenantId() + "' existe déjà");
            }
            newTenantId = request.getTenantId();
        } else {
            Long maxTenant = pointDeVenteRepository.findMaxTenantId();
            newTenantId = (maxTenant != null && maxTenant > 0) ? maxTenant + 1 : 1L;
        }

        // 3. Création du PointDeVente (Tenant)
        PointDeVente pdv = new PointDeVente();
        pdv.setTenantId(newTenantId);
        pdv.setNomPointDeVente(request.getNomEntreprise());
        pdv.setNom(request.getNomEntreprise());
        pdv.setAdresse(request.getAdresse());
        pdv.setTelephone(request.getTelephone());
        pdv.setEmail(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail()
                : request.getAdminEmail());
        pdv.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        pdv.setActif(true);
        pdv.setDateCreation(LocalDateTime.now());
        PointDeVente savedPdv = pointDeVenteRepository.save(pdv);

        Long tenantPointDeVenteId = savedPdv.getId();

        // 4. Création du profil légal d'entreprise associé
        EntrepriseProfile profile = new EntrepriseProfile();
        profile.setPointDeVenteId(tenantPointDeVenteId);
        profile.setNomEntreprise(request.getNomEntreprise());
        profile.setActivite(request.getActivite());
        profile.setAdresse(request.getAdresse());
        profile.setVille(request.getVille());
        profile.setCodePostal(request.getCodePostal());
        profile.setTelephone(request.getTelephone());
        profile.setEmail(request.getEmail());
        profile.setSiteWeb(request.getSiteWeb());
        profile.setDevise(request.getDevise() != null && !request.getDevise().isBlank() ? request.getDevise() : "MAD");
        profile.setRegistreCommerce(request.getRegistreCommerce());
        profile.setNumeroIdentificationFiscale(request.getNumeroIdentificationFiscale());
        profile.setNumeroIdentificationStatistique(request.getNumeroIdentificationStatistique());
        profile.setArticleImposition(request.getArticleImposition());
        profile.setCompteBancaireRib(request.getCompteBancaireRib());
        profile.setNomBanque(request.getNomBanque());
        profile.setDateMiseAJour(LocalDateTime.now());
        EntrepriseProfile savedProfile = entrepriseProfileRepository.save(profile);

        // 5. Création d'un Dépôt par défaut pour l'entreprise
        Depot defaultDepot = new Depot();
        defaultDepot.setNom("Dépôt Principal");
        defaultDepot.setDescription("Dépôt principal par défaut de " + request.getNomEntreprise());
        defaultDepot.setAdresse(request.getAdresse() != null && !request.getAdresse().isBlank() ? request.getAdresse()
                : "Siège principal");
        defaultDepot.setPointDeVenteId(tenantPointDeVenteId);
        defaultDepot.setActif(true);
        defaultDepot.setDateCreation(LocalDateTime.now());
        depotRepository.save(defaultDepot);

        // 6. Création de l'unique compte administrateur initial pour cette entreprise
        Role adminRole = roleRepository.findByNom("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ROLE_ADMIN non trouvé"));

        User adminUser = new User();
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setUsername(request.getAdminUsername());
        adminUser.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        adminUser.setNomComplet(request.getAdminNomComplet());
        adminUser.setTelephone(request.getAdminTelephone());
        adminUser.setRole(adminRole);
        adminUser.setTenantId(newTenantId);
        adminUser.setPointDeVente(savedPdv);
        adminUser.setEnabled(true);
        adminUser.setAccountNonExpired(true);
        adminUser.setAccountNonLocked(true);
        adminUser.setCredentialsNonExpired(true);
        User savedAdmin = userRepository.save(adminUser);

        return mapToResponse(savedPdv, savedProfile, savedAdmin);
    }

    @Transactional(readOnly = true)
    public Page<EntrepriseResponse> listerEntreprises(String search, Boolean actif, Pageable pageable) {
        Page<PointDeVente> pdvPage;
        boolean hasSearch = (search != null && !search.isBlank());

        if (hasSearch && actif != null) {
            pdvPage = pointDeVenteRepository.searchPointsDeVenteByActif(search.trim(), actif, pageable);
        } else if (hasSearch) {
            pdvPage = pointDeVenteRepository.searchPointsDeVente(search.trim(), pageable);
        } else if (actif != null) {
            pdvPage = pointDeVenteRepository.findByActif(actif, pageable);
        } else {
            pdvPage = pointDeVenteRepository.findAll(pageable);
        }

        return pdvPage.map(pdv -> {
            Optional<EntrepriseProfile> profileOpt = entrepriseProfileRepository.findByPointDeVenteId(pdv.getId());
            Optional<User> adminOpt = userRepository.findFirstByTenantIdAndRoleNom(pdv.getTenantId(), "ROLE_ADMIN")
                    .or(() -> userRepository.findFirstByPointDeVenteIdAndRoleNom(pdv.getId(), "ROLE_ADMIN"));
            return mapToResponse(pdv, profileOpt.orElse(null), adminOpt.orElse(null));
        });
    }

    @Transactional(readOnly = true)
    public EntrepriseResponse getEntrepriseById(Long id) {
        PointDeVente pdv = pointDeVenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise avec l'id " + id + " non trouvée"));
        Optional<EntrepriseProfile> profileOpt = entrepriseProfileRepository.findByPointDeVenteId(pdv.getId());
        Optional<User> adminOpt = userRepository.findFirstByTenantIdAndRoleNom(pdv.getTenantId(), "ROLE_ADMIN")
                .or(() -> userRepository.findFirstByPointDeVenteIdAndRoleNom(pdv.getId(), "ROLE_ADMIN"));
        return mapToResponse(pdv, profileOpt.orElse(null), adminOpt.orElse(null));
    }

    @Transactional
    public EntrepriseResponse modifierEntreprise(Long id, EntrepriseUpdateRequest request) {
        PointDeVente pdv = pointDeVenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise avec l'id " + id + " non trouvée"));

        if (!pdv.getNomPointDeVente().equals(request.getNomEntreprise()) &&
                pointDeVenteRepository.existsByNomPointDeVente(request.getNomEntreprise())) {
            throw new IllegalArgumentException("Une entreprise avec ce nom existe déjà");
        }

        pdv.setNomPointDeVente(request.getNomEntreprise());
        pdv.setNom(request.getNomEntreprise());
        pdv.setAdresse(request.getAdresse());
        pdv.setTelephone(request.getTelephone());
        pdv.setEmail(request.getEmail());
        if (request.getActif() != null) {
            pdv.setActif(request.getActif());
        }
        PointDeVente updatedPdv = pointDeVenteRepository.save(pdv);

        // Mettre à jour le profil
        EntrepriseProfile profile = entrepriseProfileRepository.findByPointDeVenteId(pdv.getId())
                .orElseGet(() -> {
                    EntrepriseProfile newProfile = new EntrepriseProfile();
                    newProfile.setPointDeVenteId(pdv.getId());
                    return newProfile;
                });

        profile.setNomEntreprise(request.getNomEntreprise());
        profile.setActivite(request.getActivite());
        profile.setAdresse(request.getAdresse());
        profile.setVille(request.getVille());
        profile.setCodePostal(request.getCodePostal());
        profile.setTelephone(request.getTelephone());
        profile.setEmail(request.getEmail());
        profile.setSiteWeb(request.getSiteWeb());
        if (request.getDevise() != null)
            profile.setDevise(request.getDevise());
        profile.setRegistreCommerce(request.getRegistreCommerce());
        profile.setNumeroIdentificationFiscale(request.getNumeroIdentificationFiscale());
        profile.setNumeroIdentificationStatistique(request.getNumeroIdentificationStatistique());
        profile.setArticleImposition(request.getArticleImposition());
        profile.setCompteBancaireRib(request.getCompteBancaireRib());
        profile.setNomBanque(request.getNomBanque());
        profile.setDateMiseAJour(LocalDateTime.now());
        EntrepriseProfile savedProfile = entrepriseProfileRepository.save(profile);

        Optional<User> adminOpt = userRepository.findFirstByTenantIdAndRoleNom(pdv.getTenantId(), "ROLE_ADMIN")
                .or(() -> userRepository.findFirstByPointDeVenteIdAndRoleNom(pdv.getId(), "ROLE_ADMIN"));
        return mapToResponse(updatedPdv, savedProfile, adminOpt.orElse(null));
    }

    @Transactional
    public EntrepriseResponse toggleActif(Long id, boolean actif) {
        PointDeVente pdv = pointDeVenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise avec l'id " + id + " non trouvée"));
        pdv.setActif(actif);
        PointDeVente updated = pointDeVenteRepository.save(pdv);

        Optional<EntrepriseProfile> profileOpt = entrepriseProfileRepository.findByPointDeVenteId(pdv.getId());
        Optional<User> adminOpt = userRepository.findFirstByTenantIdAndRoleNom(pdv.getTenantId(), "ROLE_ADMIN")
                .or(() -> userRepository.findFirstByPointDeVenteIdAndRoleNom(pdv.getId(), "ROLE_ADMIN"));
        return mapToResponse(updated, profileOpt.orElse(null), adminOpt.orElse(null));
    }

    private EntrepriseResponse mapToResponse(PointDeVente pdv, EntrepriseProfile profile, User admin) {
        EntrepriseResponse res = new EntrepriseResponse();
        res.setId(pdv.getId());
        res.setTenantId(pdv.getTenantId());
        res.setNomEntreprise(pdv.getNomPointDeVente());
        res.setAdresse(pdv.getAdresse());
        res.setTelephone(pdv.getTelephone());
        res.setEmail(pdv.getEmail());
        res.setActif(pdv.getActif() != null ? pdv.getActif() : true);
        res.setDateCreation(pdv.getDateCreation());

        int userCount = userRepository.countByTenantId(pdv.getTenantId());
        if (userCount == 0 && pdv.getId() != null) {
            userCount = userRepository.countByPointDeVenteId(pdv.getId());
        }
        res.setNombreUtilisateurs(userCount);

        if (profile != null) {
            res.setActivite(profile.getActivite());
            res.setVille(profile.getVille());
            res.setCodePostal(profile.getCodePostal());
            res.setSiteWeb(profile.getSiteWeb());
            res.setDevise(profile.getDevise());
            res.setRegistreCommerce(profile.getRegistreCommerce());
            res.setNumeroIdentificationFiscale(profile.getNumeroIdentificationFiscale());
            res.setNumeroIdentificationStatistique(profile.getNumeroIdentificationStatistique());
            res.setArticleImposition(profile.getArticleImposition());
            res.setCompteBancaireRib(profile.getCompteBancaireRib());
            res.setNomBanque(profile.getNomBanque());
        }

        if (admin != null) {
            res.setAdminUserId(admin.getId());
            res.setAdminEmail(admin.getEmail());
            res.setAdminUsername(admin.getUsername());
            res.setAdminNomComplet(admin.getNomComplet());
        }

        return res;
    }
}
