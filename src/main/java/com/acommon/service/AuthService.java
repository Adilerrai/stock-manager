package com.acommon.service;



import com.acommon.persistant.dto.JwtAuthenticationResponse;
import com.acommon.persistant.dto.UserLoginRequest;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.acommon.config.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }


    public JwtAuthenticationResponse authenticateByUsername(UserLoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                throw new RuntimeException("Username manquant dans la requête");
            }
            
            System.out.println("🔍 Tentative de connexion avec username: " + request.getUsername());

            // Vérifier si l'utilisateur existe
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        System.err.println("❌ Utilisateur '" + request.getUsername() + "' non trouvé dans la base");
                        // Essayer de lister tous les utilisateurs pour debug
                        userRepository.findAll().forEach(u ->
                            System.out.println("   👤 User trouvé: username=" + u.getUsername() + ", email=" + u.getEmail())
                        );
                        return new RuntimeException("Utilisateur '" + request.getUsername() + "' non trouvé");
                    });

            System.out.println("✅ Utilisateur trouvé: " + user.getUsername() + " (email: " + user.getEmail() + ")");


            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
            );
            
            User authenticatedUser = (User) authentication.getPrincipal();

            String token = jwtUtil.generateToken(authenticatedUser, authenticatedUser.getPointDeVenteId());

            return JwtAuthenticationResponse.builder()
                .token(token)
                .id(authenticatedUser.getId())
                .email(authenticatedUser.getEmail())
                .nomComplet(authenticatedUser.getNomComplet())
                .telephone(authenticatedUser.getTelephone())
                .genre(authenticatedUser.getGenre())
                .username(authenticatedUser.getUsername())
                .role(authenticatedUser.getRole().getNom())
                .pointDeVenteId(authenticatedUser.getPointDeVenteId())
                .tokenType("Bearer")
                .build();
        } catch (Exception e) {
            System.err.println("Erreur d'authentification par username: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'authentification: " + e.getMessage());
        }
    }

}
