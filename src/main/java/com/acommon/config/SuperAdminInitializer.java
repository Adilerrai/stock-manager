package com.acommon.config;

import com.acommon.persistant.model.Habilitation;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.User;
import com.acommon.repository.HabilitationRepository;
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

import java.util.*;
import java.util.stream.Collectors;

// @Component désactivé : toute l'initialisation des données et DDL est déléguée à db.sql
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final HabilitationRepository habilitationRepository;
    private final com.gestion.service.BanqueService banqueService;

    public SuperAdminInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            HabilitationRepository habilitationRepository,
            com.gestion.service.BanqueService banqueService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.habilitationRepository = habilitationRepository;
        this.banqueService = banqueService;
    }

    @Override
    public void run(String... args) {
        // Désactivé : toutes les données sont gérées par db.sql pour éviter tout verrouillage ou conflit au boot.
        log.info("ℹ️ Initialisation applicative au démarrage gérée intégralement via db.sql.");
    }

    private void cleanupConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE IF EXISTS lignes_livraison DROP CONSTRAINT IF EXISTS uk_bgip3ymlrc9lga65p5dy9jfbr");
            log.info("Nettoyage contrainte uk_bgip3ymlrc9lga65p5dy9jfbr sur lignes_livraison reussi.");
        } catch (Exception e) {
            log.warn("Erreur nettoyage contrainte lignes_livraison: {}", e.getMessage());
        }
    }

    private void ensureMultiTenantColumnsExist() {
        try {
            jdbcTemplate.execute("ALTER TABLE commandes_client ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;");
            jdbcTemplate.execute("ALTER TABLE mouvements_stock ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE livraisons ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;");
            jdbcTemplate.execute("ALTER TABLE entreprise_profiles ADD COLUMN IF NOT EXISTS vente_stock_negatif BOOLEAN DEFAULT FALSE;");
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

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION DES HABILITATIONS METIER
    // ─────────────────────────────────────────────────────────────────────────

    private Habilitation ensureHabilitation(String nom) {
        return habilitationRepository.findByNom(nom).orElseGet(() -> {
            Habilitation h = new Habilitation();
            h.setNom(nom);
            return habilitationRepository.save(h);
        });
    }

    private void initHabilitations() {
        List<String> allHabilitations = Arrays.asList(
            "PRODUIT_READ", "PRODUIT_CREATE", "PRODUIT_UPDATE", "PRODUIT_DELETE",
            "STOCK_READ", "STOCK_CREATE", "STOCK_TRANSFERT",
            "VENTE_READ", "VENTE_CREATE", "VENTE_DELETE",
            "COMMANDE_READ", "COMMANDE_CREATE", "COMMANDE_VALIDATE",
            "FACTURE_READ", "FACTURE_CREATE", "FACTURE_VALIDER", "FACTURE_ANNULER",
            "CLIENT_READ", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE",
            "FOURNISSEUR_READ", "FOURNISSEUR_CREATE", "FOURNISSEUR_UPDATE",
            "PAIEMENT_READ", "PAIEMENT_CREATE",
            "COMPTA_READ", "COMPTA_ECRITURE", "COMPTA_CLOTURE",
            "TRESORERIE_READ", "TRESORERIE_MOUVEMENT",
            "RAPPORT_READ", "RAPPORT_EXPORT",
            "USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
            "ADMIN_ENTREPRISE", "ADMIN_ROLES", "ADMIN_HABILITATIONS"
        );
        for (String nom : allHabilitations) {
            ensureHabilitation(nom);
        }
        log.info("🔑 {} habilitations metier initialisees.", allHabilitations.size());
    }

    private void assignHabilitationsToRoles() {
        Map<String, List<String>> roleHabilitations = new LinkedHashMap<>();

        roleHabilitations.put("ROLE_ADMIN", Arrays.asList(
            "PRODUIT_READ", "PRODUIT_CREATE", "PRODUIT_UPDATE", "PRODUIT_DELETE",
            "STOCK_READ", "STOCK_CREATE", "STOCK_TRANSFERT",
            "VENTE_READ", "VENTE_CREATE", "VENTE_DELETE",
            "COMMANDE_READ", "COMMANDE_CREATE", "COMMANDE_VALIDATE",
            "FACTURE_READ", "FACTURE_CREATE", "FACTURE_VALIDER", "FACTURE_ANNULER",
            "CLIENT_READ", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE",
            "FOURNISSEUR_READ", "FOURNISSEUR_CREATE", "FOURNISSEUR_UPDATE",
            "PAIEMENT_READ", "PAIEMENT_CREATE",
            "COMPTA_READ", "COMPTA_ECRITURE",
            "TRESORERIE_READ", "TRESORERIE_MOUVEMENT",
            "RAPPORT_READ", "RAPPORT_EXPORT",
            "USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
            "ADMIN_ENTREPRISE", "ADMIN_ROLES", "ADMIN_HABILITATIONS"
        ));
        roleHabilitations.put("ROLE_POINT_DE_VENTE_MANAGER", Arrays.asList(
            "PRODUIT_READ", "PRODUIT_CREATE", "PRODUIT_UPDATE",
            "STOCK_READ", "STOCK_CREATE", "STOCK_TRANSFERT",
            "VENTE_READ", "VENTE_CREATE",
            "COMMANDE_READ", "COMMANDE_CREATE", "COMMANDE_VALIDATE",
            "FACTURE_READ", "FACTURE_CREATE", "FACTURE_VALIDER",
            "CLIENT_READ", "CLIENT_CREATE", "CLIENT_UPDATE",
            "FOURNISSEUR_READ", "PAIEMENT_READ", "PAIEMENT_CREATE",
            "TRESORERIE_READ", "RAPPORT_READ",
            "USER_READ", "USER_CREATE", "USER_UPDATE"
        ));
        roleHabilitations.put("ROLE_COMMERCIAL", Arrays.asList(
            "VENTE_READ", "VENTE_CREATE",
            "COMMANDE_READ", "COMMANDE_CREATE", "COMMANDE_VALIDATE",
            "FACTURE_READ", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_UPDATE",
            "PRODUIT_READ", "RAPPORT_READ"
        ));
        roleHabilitations.put("ROLE_RESPONSABLE_COMMERCIAL", Arrays.asList(
            "VENTE_READ", "VENTE_CREATE", "VENTE_DELETE",
            "COMMANDE_READ", "COMMANDE_CREATE", "COMMANDE_VALIDATE",
            "FACTURE_READ", "FACTURE_CREATE",
            "CLIENT_READ", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE",
            "PRODUIT_READ", "PAIEMENT_READ", "RAPPORT_READ", "RAPPORT_EXPORT"
        ));
        roleHabilitations.put("ROLE_COMPTABLE", Arrays.asList(
            "COMPTA_READ", "COMPTA_ECRITURE", "COMPTA_CLOTURE",
            "TRESORERIE_READ", "TRESORERIE_MOUVEMENT",
            "FACTURE_READ", "FACTURE_CREATE", "FACTURE_VALIDER", "FACTURE_ANNULER",
            "PAIEMENT_READ", "PAIEMENT_CREATE", "RAPPORT_READ", "RAPPORT_EXPORT"
        ));
        roleHabilitations.put("ROLE_CAISSIER", Arrays.asList(
            "VENTE_CREATE", "PAIEMENT_READ", "PAIEMENT_CREATE",
            "CLIENT_READ", "FACTURE_READ"
        ));
        roleHabilitations.put("ROLE_VENDEUR", Arrays.asList(
            "VENTE_READ", "VENTE_CREATE", "CLIENT_READ", "PRODUIT_READ", "FACTURE_READ"
        ));
        roleHabilitations.put("ROLE_MAGASINIER", Arrays.asList(
            "STOCK_READ", "STOCK_CREATE", "STOCK_TRANSFERT",
            "PRODUIT_READ", "COMMANDE_READ", "FOURNISSEUR_READ"
        ));
        roleHabilitations.put("ROLE_GESTIONNAIRE", Arrays.asList(
            "PRODUIT_READ", "PRODUIT_CREATE", "PRODUIT_UPDATE", "PRODUIT_DELETE",
            "STOCK_READ", "STOCK_CREATE", "STOCK_TRANSFERT",
            "FOURNISSEUR_READ", "FOURNISSEUR_CREATE", "FOURNISSEUR_UPDATE",
            "COMMANDE_READ", "COMMANDE_CREATE", "RAPPORT_READ"
        ));

        for (Map.Entry<String, List<String>> entry : roleHabilitations.entrySet()) {
            roleRepository.findByNom(entry.getKey()).ifPresent(role -> {
                if (role.getHabilitations() == null || role.getHabilitations().isEmpty()) {
                    Set<Habilitation> habs = entry.getValue().stream()
                            .map(this::ensureHabilitation)
                            .collect(Collectors.toSet());
                    role.setHabilitations(habs);
                    roleRepository.save(role);
                    log.info("   🛡️ {} habilitations -> {}", habs.size(), entry.getKey());
                }
            });
        }
        log.info("🛡️ Assignation des habilitations terminée.");
    }
}
