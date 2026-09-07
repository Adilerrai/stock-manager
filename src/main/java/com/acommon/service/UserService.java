package com.acommon.service;

import com.acommon.persistant.dto.*;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PointDeVenteRepository pointDeVenteRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        if (auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return userRepository.findByEmail(auth.getName())
                .or(() -> userRepository.findByUsername(auth.getName()))
                .orElse(null);
    }

    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean superAdmin = isSuperAdmin(auth);
        User currentUser = getCurrentUser();

        Long targetTenantId;
        PointDeVente targetPointDeVente = null;

        if (superAdmin) {
            // Pour le SuperAdmin : sélection libre du PointDeVente ou déduction du tenant
            if (request.getPointDeVenteId() != null) {
                targetPointDeVente = pointDeVenteRepository.findById(request.getPointDeVenteId())
                        .orElseThrow(() -> new IllegalArgumentException("Point de vente introuvable avec l'ID : " + request.getPointDeVenteId()));
                targetTenantId = targetPointDeVente.getTenantId();
            } else {
                Long tenant = TenantContext.getCurrentTenant();
                targetTenantId = (tenant != null) ? tenant : 0L;
            }
        } else {
            // Pour l'administrateur d'entreprise (ROLE_ADMIN, etc.) :
            // Le tenantId DOIT OBLIGATOIREMENT être récupéré depuis l'utilisateur connecté !
            if (currentUser != null && currentUser.getTenantId() != null) {
                targetTenantId = currentUser.getTenantId();
            } else {
                Long tenant = TenantContext.getCurrentTenant();
                if (tenant == null) {
                    throw new AccessDeniedException("Impossible d'identifier l'entreprise (tenant) de l'utilisateur connecté");
                }
                targetTenantId = tenant;
            }

            // Gestion de l'association PointDeVente :
            if (request.getPointDeVenteId() != null) {
                targetPointDeVente = pointDeVenteRepository.findById(request.getPointDeVenteId())
                        .orElseThrow(() -> new IllegalArgumentException("Point de vente introuvable avec l'ID : " + request.getPointDeVenteId()));
                // Vérification stricte que le point de vente appartient au tenant de l'entreprise
                if (!targetTenantId.equals(targetPointDeVente.getTenantId())) {
                    throw new AccessDeniedException("Le point de vente spécifié n'appartient pas à votre entreprise (tenant " + targetTenantId + ")");
                }
            } else {
                // Par défaut, affecter au point de vente de l'admin connecté ou au point de vente principal du tenant
                if (currentUser != null && currentUser.getPointDeVente() != null) {
                    targetPointDeVente = currentUser.getPointDeVente();
                } else {
                    targetPointDeVente = pointDeVenteRepository.findByTenantId(targetTenantId).orElse(null);
                }
            }
        }

        // Vérifier unicité email et username
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + request.getEmail());
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet identifiant existe déjà : " + request.getUsername());
        }

        // Vérifier le rôle
        String roleNom = request.getRole();
        if (!roleNom.startsWith("ROLE_")) {
            roleNom = "ROLE_" + roleNom;
        }

        // Un non-superadmin ne peut pas créer de SUPERADMIN
        if (!superAdmin && "ROLE_SUPERADMIN".equals(roleNom)) {
            throw new AccessDeniedException("Seul un SuperAdmin peut assigner le rôle ROLE_SUPERADMIN");
        }

        final String finalRoleNom = roleNom;
        Role role = roleRepository.findByNom(finalRoleNom)
                .orElseThrow(() -> new IllegalArgumentException("Rôle '" + finalRoleNom + "' introuvable"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNomComplet(request.getNomComplet());
        user.setTelephone(request.getTelephone());
        user.setGenre(request.getGenre());
        user.setRole(role);
        user.setTenantId(targetTenantId);
        user.setPointDeVente(targetPointDeVente);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        return mapToResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(Long pointDeVenteIdFilter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean superAdmin = isSuperAdmin(auth);

        List<User> users;
        if (superAdmin) {
            if (pointDeVenteIdFilter != null) {
                users = userRepository.findByPointDeVenteId(pointDeVenteIdFilter);
            } else {
                users = userRepository.findAll();
            }
        } else {
            User currentUser = getCurrentUser();
            Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                    ? currentUser.getTenantId()
                    : TenantContext.getCurrentTenant();

            if (currentTenant == null) {
                return List.of();
            }

            if (pointDeVenteIdFilter != null) {
                users = userRepository.findByTenantIdAndPointDeVenteId(currentTenant, pointDeVenteIdFilter);
            } else {
                users = userRepository.findByTenantId(currentTenant);
                if (users.isEmpty()) {
                    users = userRepository.findByPointDeVenteId(currentTenant);
                }
            }
        }

        return users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findAndValidateAccess(id);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findAndValidateAccess(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean superAdmin = isSuperAdmin(auth);

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Cet email est déjà utilisé par un autre utilisateur");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getUsername() != null && !request.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Cet identifiant est déjà utilisé");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getNomComplet() != null) {
            user.setNomComplet(request.getNomComplet());
        }
        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        if (request.getGenre() != null) {
            user.setGenre(request.getGenre());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getPointDeVenteId() != null) {
            PointDeVente pdv = pointDeVenteRepository.findById(request.getPointDeVenteId())
                    .orElseThrow(() -> new IllegalArgumentException("Point de vente introuvable : " + request.getPointDeVenteId()));
            if (!superAdmin) {
                User currentUser = getCurrentUser();
                Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                        ? currentUser.getTenantId()
                        : user.getTenantId();
                if (currentTenant != null && !currentTenant.equals(pdv.getTenantId())) {
                    throw new AccessDeniedException("Le point de vente spécifié n'appartient pas à votre entreprise");
                }
            }
            user.setPointDeVente(pdv);
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            String rawRole = request.getRole().trim();
            final String targetRoleNom = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
            if (!superAdmin && "ROLE_SUPERADMIN".equals(targetRoleNom)) {
                throw new AccessDeniedException("Seul un SuperAdmin peut assigner le rôle ROLE_SUPERADMIN");
            }
            Role role = roleRepository.findByNom(targetRoleNom)
                    .orElseThrow(() -> new IllegalArgumentException("Rôle '" + targetRoleNom + "' introuvable"));
            user.setRole(role);
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id, boolean enabled) {
        User user = findAndValidateAccess(id);
        String currentUsername = getCurrentUsername();

        if (user.getUsername().equals(currentUsername) || user.getEmail().equals(currentUsername)) {
            throw new IllegalArgumentException("Vous ne pouvez pas désactiver votre propre compte");
        }

        user.setEnabled(enabled);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = findAndValidateAccess(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String currentIdentifier, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(currentIdentifier)
                .or(() -> userRepository.findByEmail(currentIdentifier))
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + currentIdentifier));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findAndValidateAccess(id);
        String currentUsername = getCurrentUsername();

        if (user.getUsername().equals(currentUsername) || user.getEmail().equals(currentUsername)) {
            throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
        }

        if (user.getRole() != null && "ROLE_SUPERADMIN".equals(user.getRole().getNom())) {
            throw new IllegalArgumentException("Impossible de supprimer un compte SUPERADMIN");
        }

        userRepository.delete(user);
    }

    private User findAndValidateAccess(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur avec l'id " + id + " introuvable"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isSuperAdmin(auth)) {
            User currentUser = getCurrentUser();
            Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                    ? currentUser.getTenantId()
                    : TenantContext.getCurrentTenant();

            if (currentTenant != null) {
                boolean tenantMatches = currentTenant.equals(user.getTenantId())
                        || (user.getPointDeVente() != null && currentTenant.equals(user.getPointDeVente().getTenantId()))
                        || currentTenant.equals(user.getPointDeVenteId());

                if (!tenantMatches) {
                    throw new AccessDeniedException("Accès refusé : cet utilisateur appartient à une autre entreprise");
                }
            }
        }
        return user;
    }

    private UserResponse mapToResponse(User user) {
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
        return response;
    }
}