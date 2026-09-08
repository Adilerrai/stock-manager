package com.acommon.config;

import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.User;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final com.gestion.service.BanqueService banqueService;

    public SuperAdminInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            com.gestion.service.BanqueService banqueService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureMultiTenantColumnsExist();
        syncSequences();
        initRoles();
        syncSequences();
        initSuperAdmin();
        initBanques();
    }

    private void ensureMultiTenantColumnsExist() {
        try {
            jdbcTemplate.execute("ALTER TABLE commandes_client ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;");
            jdbcTemplate.execute("ALTER TABLE mouvements_stock ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE livraisons ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;");
            log.info("🛡️ Colonnes multi-tenant vérifiées/créées en base avec succès.");
        } catch (Exception e) {
            log.warn("Vérification colonnes multi-tenant : {}", e.getMessage());
        }
    }

    private void initBanques() {
        try {
            banqueService.initialiserBanquesStandardsSiVide(1L);
            log.info("🏦 Banques de référence vérifiées/initialisées avec succès.");
        } catch (Exception e) {
            log.debug("Initialisation banques : {}", e.getMessage());
        }
    }

    private void syncSequences() {
        try {
            jdbcTemplate.execute("SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM roles))");
            jdbcTemplate.execute("SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users))");
            jdbcTemplate.execute("SELECT setval('point_de_vente_id_seq', (SELECT COALESCE(MAX(id), 1) FROM point_de_vente))");
        } catch (Exception e) {
            log.debug("Synchronisation des séquences : {}", e.getMessage());
        }
    }

    private void initRoles() {
        List<String> requiredRoles = Arrays.asList(
                "ROLE_SUPERADMIN",
                "ROLE_ADMIN",
                "ROLE_POINT_DE_VENTE_MANAGER",
                "ROLE_CAISSIER",
                "ROLE_VENDEUR",
                "ROLE_MAGASINIER",
                "ROLE_GESTIONNAIRE",
                "ROLE_RESPONSABLE_COMMERCIAL",
                "ROLE_COMMERCIAL",
                "ROLE_COMPTABLE"
        );

        for (String roleNom : requiredRoles) {
            if (!roleRepository.existsByNom(roleNom)) {
                Role role = new Role();
                role.setNom(roleNom);
                roleRepository.save(role);
                log.info("🛡️ Rôle initialisé avec succès : {}", roleNom);
            }
        }
    }

    private void initSuperAdmin() {
        boolean superAdminExists = userRepository.existsByRoleNom("ROLE_SUPERADMIN")
                || userRepository.existsByUsername("superadmin")
                || userRepository.existsByEmail("superadmin@erp.com");

        if (!superAdminExists) {
            Role superAdminRole = roleRepository.findByNom("ROLE_SUPERADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setNom("ROLE_SUPERADMIN");
                        return roleRepository.save(r);
                    });

            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("superadmin@erp.com");
            superAdmin.setPassword(passwordEncoder.encode("SuperAdmin@2026!"));
            superAdmin.setNomComplet("Super Administrateur Plateforme");
            superAdmin.setTelephone("+213 00 00 00 00");
            superAdmin.setRole(superAdminRole);
            superAdmin.setTenantId(0L); // 0L pour le tenant global plateforme
            superAdmin.setPointDeVente(null); // Pas de point de vente assigné pour le superadmin global
            superAdmin.setEnabled(true);
            superAdmin.setAccountNonExpired(true);
            superAdmin.setAccountNonLocked(true);
            superAdmin.setCredentialsNonExpired(true);

            userRepository.save(superAdmin);

            log.info("\n" +
                    "====================================================================\n" +
                    "🚀 COMPTE SUPERADMIN CRÉÉ AVEC SUCCÈS AU DÉMARRAGE :\n" +
                    "   👤 Identifiant (Username) : superadmin\n" +
                    "   📧 Email                 : superadmin@erp.com\n" +
                    "   🔑 Mot de passe          : SuperAdmin@2026!\n" +
                    "   🛡️ Rôle                  : ROLE_SUPERADMIN\n" +
                    "====================================================================");
        } else {
            log.info("✅ Compte SUPERADMIN déjà présent et prêt.");
        }
    }
}
