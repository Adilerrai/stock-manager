package com.acommon.service;


import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.dto.UserCreationRequest;
import com.acommon.persistant.dto.UserResponse;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PointDeVenteRepository pointDeVenteRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasAuthority('ROLE_POINT_DE_VENTE_MANAGER')")
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        // Vérifier unicité email dans ce point de vente
        if (userRepository.findByEmailAndPointDeVente_Id(request.getEmail(), pointDeVente.getId()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà dans ce point de vente");
        }

        // Vérifier unicité username dans ce point de vente
        if (userRepository.findByUsernameAndPointDeVente_Id(request.getUsername(), pointDeVente.getId()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec ce nom d'utilisateur existe déjà dans ce point de vente");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNomComplet(request.getNomComplet());
        user.setPointDeVente(pointDeVente);
        // ... autres champs

        return mapToResponse(userRepository.save(user));
    }

    public List<UserResponse> getCurrentPointDeVenteUsers() {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        List<User> users = userRepository.findByPointDeVente_Id(pointDeVente.getId());
        return users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNomComplet(user.getNomComplet());
        response.setTelephone(user.getTelephone());
        response.setGenre(user.getGenre());
        response.setRole(user.getRole().getNom());
        response.setPointDeVenteId(user.getPointDeVente().getId());
        return response;
    }
}