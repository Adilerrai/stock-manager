package com.acommon.service;

import com.acommon.persistant.dto.RoleCreateRequest;
import com.acommon.persistant.dto.RoleResponse;
import com.acommon.persistant.model.Habilitation;
import com.acommon.persistant.model.Role;
import com.acommon.repository.HabilitationRepository;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final HabilitationRepository habilitationRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository,
                       HabilitationRepository habilitationRepository,
                       UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.habilitationRepository = habilitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .filter(r -> !"ROLE_SUPERADMIN".equalsIgnoreCase(r.getNom()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rôle introuvable avec l'ID: " + id));
        if ("ROLE_SUPERADMIN".equalsIgnoreCase(role.getNom())) {
            throw new IllegalArgumentException("Rôle introuvable avec l'ID: " + id);
        }
        return mapToResponse(role);
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        String nom = request.getNom().trim().toUpperCase();
        if (!nom.startsWith("ROLE_")) {
            nom = "ROLE_" + nom;
        }
        if ("ROLE_SUPERADMIN".equalsIgnoreCase(nom)) {
            throw new IllegalArgumentException("Impossible de créer le rôle réservé ROLE_SUPERADMIN");
        }
        if (roleRepository.existsByNom(nom)) {
            throw new IllegalArgumentException("Un rôle avec le nom '" + nom + "' existe déjà");
        }

        Role role = new Role();
        role.setNom(nom);

        if (request.getHabilitations() != null && !request.getHabilitations().isEmpty()) {
            Set<Habilitation> habs = resolveHabilitations(request.getHabilitations());
            role.setHabilitations(habs);
        } else {
            role.setHabilitations(new HashSet<>());
        }

        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateHabilitations(Long roleId, List<String> habilitationNoms) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rôle introuvable: " + roleId));

        // Interdiction de modifier ROLE_SUPERADMIN
        if ("ROLE_SUPERADMIN".equals(role.getNom())) {
            throw new IllegalArgumentException("Le rôle SUPERADMIN ne peut pas être modifié");
        }

        Set<Habilitation> habilitations = resolveHabilitations(habilitationNoms);
        role.setHabilitations(habilitations);

        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rôle introuvable: " + id));

        // Roles système non supprimables
        List<String> systemRoles = List.of("ROLE_SUPERADMIN", "ROLE_ADMIN");
        if (systemRoles.contains(role.getNom())) {
            throw new IllegalArgumentException("Le rôle '" + role.getNom() + "' est un rôle système et ne peut pas être supprimé");
        }

        long usersWithRole = userRepository.countByRoleNom(role.getNom());
        if (usersWithRole > 0) {
            throw new IllegalArgumentException("Impossible de supprimer ce rôle : " + usersWithRole + " utilisateur(s) y sont assignés");
        }

        roleRepository.delete(role);
    }

    @Transactional
    public List<String> getAllHabilitationNoms() {
        // Garantit que le catalogue métier standard est toujours disponible en base et affiché dans l'UI AgenceWeb
        for (String perm : com.acommon.config.SuperAdminHabilitations.BUSINESS_PERMISSIONS) {
            if (habilitationRepository.findByNom(perm).isEmpty()) {
                Habilitation h = new Habilitation();
                h.setNom(perm);
                habilitationRepository.save(h);
            }
        }

        return habilitationRepository.findAll()
                .stream()
                .map(Habilitation::getNom)
                .sorted()
                .collect(Collectors.toList());
    }

    private Set<Habilitation> resolveHabilitations(List<String> noms) {
        if (noms == null || noms.isEmpty()) return new HashSet<>();
        Set<Habilitation> result = new HashSet<>();
        for (String nom : noms) {
            Habilitation hab = habilitationRepository.findByNom(nom)
                    .orElseGet(() -> {
                        Habilitation h = new Habilitation();
                        h.setNom(nom);
                        return habilitationRepository.save(h);
                    });
            result.add(hab);
        }
        return result;
    }

    private RoleResponse mapToResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setNom(role.getNom());
        response.setHabilitations(
                role.getHabilitations() == null ? List.of() :
                role.getHabilitations().stream()
                        .map(Habilitation::getNom)
                        .sorted()
                        .collect(Collectors.toList())
        );
        response.setNombreUtilisateurs(userRepository.countByRoleNom(role.getNom()));
        return response;
    }
}
