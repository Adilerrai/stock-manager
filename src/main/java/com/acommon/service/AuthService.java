package com.acommon.service;

import com.acommon.persistant.dto.JwtAuthenticationResponse;
import com.acommon.persistant.dto.UserLoginRequest;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import com.acommon.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PointDeVenteRepository pointDeVenteRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public JwtAuthenticationResponse authenticateByUsername(UserLoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Identifiant (username ou email) manquant dans la requête");
        }

        final String searchIdentifier = request.getUsername().trim();

        // 1. Recherche de l'utilisateur par username ou par email
        User user = userRepository.findByUsername(searchIdentifier)
                .or(() -> userRepository.findByEmail(searchIdentifier))
                .orElseThrow(() -> new BadCredentialsException("Identifiant ou mot de passe incorrect"));

        // 2. Vérification si le compte utilisateur est actif
        if (!user.isEnabled()) {
            throw new DisabledException("Votre compte est désactivé. Veuillez contacter votre administrateur.");
        }

        // 3. Vérification du statut de l'entreprise (sauf pour le SuperAdmin plateforme)
        boolean isSuperAdmin = user.getRole() != null && "ROLE_SUPERADMIN".equals(user.getRole().getNom());
        if (!isSuperAdmin) {
            Long tenantToCheck = user.getTenantId() != null ? user.getTenantId() : user.getPointDeVenteId();
            if (tenantToCheck != null && tenantToCheck > 0) {
                pointDeVenteRepository.findByTenantId(tenantToCheck)
                        .or(() -> pointDeVenteRepository.findById(tenantToCheck))
                        .ifPresent(pdv -> {
                            if (Boolean.FALSE.equals(pdv.getActif())) {
                                throw new DisabledException("L'accès pour votre entreprise (" + pdv.getNomPointDeVente() + ") a été suspendu.");
                            }
                        });
            }
        }

        // 4. Validation des identifiants avec Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );

        User authenticatedUser = (User) authentication.getPrincipal();

        // 5. Détermination du tenantId et du nom du point de vente
        Long tenantId = authenticatedUser.getTenantId() != null
                ? authenticatedUser.getTenantId()
                : authenticatedUser.getPointDeVenteId();

        String nomPdv = null;
        if (authenticatedUser.getPointDeVente() != null) {
            nomPdv = authenticatedUser.getPointDeVente().getNomPointDeVente();
        } else if (authenticatedUser.getPointDeVenteId() != null) {
            nomPdv = pointDeVenteRepository.findById(authenticatedUser.getPointDeVenteId())
                    .map(com.acommon.persistant.model.PointDeVente::getNomPointDeVente)
                    .orElse(null);
        }

        // 6. Génération du token JWT avec le tenantId et le pointDeVenteId
        String token = jwtUtil.generateToken(authenticatedUser, tenantId, authenticatedUser.getPointDeVenteId());

        return JwtAuthenticationResponse.builder()
                .token(token)
                .id(authenticatedUser.getId())
                .email(authenticatedUser.getEmail())
                .nomComplet(authenticatedUser.getNomComplet())
                .telephone(authenticatedUser.getTelephone())
                .genre(authenticatedUser.getGenre())
                .username(authenticatedUser.getUsername())
                .role(authenticatedUser.getRole() != null ? authenticatedUser.getRole().getNom() : null)
                .tenantId(tenantId)
                .pointDeVenteId(authenticatedUser.getPointDeVenteId())
                .nomPointDeVente(nomPdv)
                .tokenType("Bearer")
                .build();
    }
}
