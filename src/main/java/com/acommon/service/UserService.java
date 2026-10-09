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
                .anyMatch(a -> "ROLE_SUPERADMIN".equalsIgnoreCase(a.getAuthority())
                            || "SUPERADMIN".equalsIgnoreCase(a.getAuthority()));
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

    private PointDeVente getAdminPointDeVente(User currentUser) {
        if (currentUser == null) return null;
        if (currentUser.getPointDeVente() != null) {
            return currentUser.getPointDeVente();
        }
        if (currentUser.getPointDeVenteId() != null) {
            return pointDeVenteRepository.findById(currentUser.getPointDeVenteId()).orElse(null);
        }
        return null;
    }

    private Long getAdminPointDeVenteId(User currentUser) {
        if (currentUser == null) return null;
        if (currentUser.getPointDeVente() != null && currentUser.getPointDeVente().getId() != null) {
            return currentUser.getPointDeVente().getId();
        }
        if (currentUser.getPointDeVenteId() != null) {
            return currentUser.getPointDeVenteId();
        }
        return null;
    }

    private boolean isUserInAdminScope(User targetUser, Long adminPdvId, Long adminTenantId) {
        if (targetUser == null) return false;

        // Un administrateur ne peut JAMAIS administrer un SUPERADMIN
        if (targetUser.getRole() != null && "ROLE_SUPERADMIN".equalsIgnoreCase(targetUser.getRole().getNom())) {
            return false;
        }

        // Si l'administrateur est rattaché à un point de vente / agence spécifique :
        if (adminPdvId != null) {
            if (targetUser.getPointDeVente() != null && adminPdvId.equals(targetUser.getPointDeVente().getId())) {
                return true;
            }
            if (adminPdvId.equals(targetUser.getPointDeVenteId())) {
                return true;
            }
            if (targetUser.getPointDeVente() == null && adminPdvId.equals(targetUser.getTenantId())) {
                return true;
            }
            return false;
        }

        // Si l'administrateur n'est rattaché à aucun point de vente spécifique, repli sur le tenant
        if (adminTenantId != null) {
            if (adminTenantId.equals(targetUser.getTenantId())) {
                return true;
            }
            if (targetUser.getPointDeVente() != null && adminTenantId.equals(targetUser.getPointDeVente().getTenantId())) {
                return true;
            }
            if (adminTenantId.equals(targetUser.getPointDeVenteId())) {
                return true;
            }
        }

        return false;
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
                targetTenantId = (targetPointDeVente.getTenantId() != null && targetPointDeVente.getTenantId() > 0)
                        ? targetPointDeVente.getTenantId()
                        : targetPointDeVente.getId();
            } else {
                Long tenant = TenantContext.getCurrentTenant();
                targetTenantId = (tenant != null) ? tenant : 0L;
            }
        } else {
            // Pour l'administrateur point de vente : gestion STRICTEMENT limitée à son agence / point de vente
            PointDeVente adminPdv = getAdminPointDeVente(currentUser);
            Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                    ? currentUser.getTenantId()
                    : TenantContext.getCurrentTenant();

            if (adminPdv != null) {
                // Si l'admin tente de spécifier un autre point de vente, refuser
                if (request.getPointDeVenteId() != null && !request.getPointDeVenteId().equals(adminPdv.getId())) {
                    throw new AccessDeniedException("Accès refusé : vous ne pouvez créer des utilisateurs que pour votre propre agence / point de vente (ID " + adminPdv.getId() + ")");
                }
                targetPointDeVente = adminPdv;
                targetTenantId = (adminPdv.getTenantId() != null && adminPdv.getTenantId() > 0)
                        ? adminPdv.getTenantId()
                        : adminPdv.getId();
            } else if (currentTenant != null) {
                targetTenantId = currentTenant;
                if (request.getPointDeVenteId() != null) {
                    targetPointDeVente = pointDeVenteRepository.findById(request.getPointDeVenteId())
                            .orElseThrow(() -> new IllegalArgumentException("Point de vente introuvable avec l'ID : " + request.getPointDeVenteId()));
                    if (!targetTenantId.equals(targetPointDeVente.getTenantId()) && !targetTenantId.equals(targetPointDeVente.getId())) {
                        throw new AccessDeniedException("Le point de vente spécifié n'appartient pas à votre entreprise (tenant " + targetTenantId + ")");
                    }
                } else {
                    targetPointDeVente = pointDeVenteRepository.findByTenantId(targetTenantId).orElse(null);
                }
            } else {
                throw new AccessDeniedException("Impossible d'identifier l'agence ou l'entreprise de l'administrateur connecté");
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
        user.setMereId(targetPointDeVente != null && targetPointDeVente.getMereId() != null
                ? targetPointDeVente.getMereId()
                : targetTenantId);
        user.setPointDeVente(targetPointDeVente);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setMustChangePassword(true);

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
            Long adminPdvId = getAdminPointDeVenteId(currentUser);
            Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                    ? currentUser.getTenantId()
                    : TenantContext.getCurrentTenant();

            if (adminPdvId == null && currentTenant == null) {
                return List.of();
            }

            // Contrôle strict du filtre s'il est fourni
            if (pointDeVenteIdFilter != null) {
                if (adminPdvId != null && !pointDeVenteIdFilter.equals(adminPdvId)) {
                    throw new AccessDeniedException("Accès refusé : vous ne pouvez consulter que les utilisateurs de votre agence / point de vente");
                }
                if (adminPdvId == null && !pointDeVenteIdFilter.equals(currentTenant)) {
                    throw new AccessDeniedException("Accès refusé : vous ne pouvez consulter que les utilisateurs de votre entreprise");
                }
            }

            if (adminPdvId != null) {
                users = userRepository.findByPointDeVenteId(adminPdvId);
                if (users.isEmpty()) {
                    users = userRepository.findByTenantId(adminPdvId);
                }
            } else {
                users = userRepository.findByTenantOrPointDeVenteTenant(currentTenant);
                if (users.isEmpty()) {
                    users = userRepository.findByTenantId(currentTenant);
                }
                if (users.isEmpty()) {
                    users = userRepository.findByPointDeVenteId(currentTenant);
                }
            }

            // Filtrage de sécurité : exclure les SuperAdmins et s'assurer que l'utilisateur appartient au périmètre
            users = users.stream()
                    .filter(u -> isUserInAdminScope(u, adminPdvId, currentTenant))
                    .collect(Collectors.toList());
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
                Long adminPdvId = getAdminPointDeVenteId(currentUser);
                if (adminPdvId != null && !adminPdvId.equals(request.getPointDeVenteId())) {
                    throw new AccessDeniedException("Accès refusé : vous ne pouvez pas déplacer un utilisateur vers une autre agence / point de vente");
                }
                Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                        ? currentUser.getTenantId()
                        : user.getTenantId();
                if (currentTenant != null && !currentTenant.equals(pdv.getTenantId()) && !currentTenant.equals(pdv.getId())) {
                    throw new AccessDeniedException("Le point de vente spécifié n'appartient pas à votre entreprise");
                }
            }
            user.setPointDeVente(pdv);
            if (superAdmin) {
                user.setTenantId(pdv.getTenantId() != null && pdv.getTenantId() > 0 ? pdv.getTenantId() : pdv.getId());
                if (pdv.getMereId() != null) {
                    user.setMereId(pdv.getMereId());
                }
            }
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
        user.setMustChangePassword(true);
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
        user.setMustChangePassword(false);
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
            Long adminPdvId = getAdminPointDeVenteId(currentUser);
            Long currentTenant = (currentUser != null && currentUser.getTenantId() != null)
                    ? currentUser.getTenantId()
                    : TenantContext.getCurrentTenant();

            if (!isUserInAdminScope(user, adminPdvId, currentTenant)) {
                throw new AccessDeniedException("Accès refusé : vous n'avez pas l'autorisation d'accéder ou de modifier cet utilisateur");
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
        response.setMustChangePassword(user.getMustChangePassword());
        return response;
    }
}