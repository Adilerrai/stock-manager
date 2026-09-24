package com.acommon.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Catalogue centralisé des habilitations et rôles ERP.
 * Définit également le Pass Universel accordé automatiquement au rôle ROLE_SUPERADMIN.
 */
public final class SuperAdminHabilitations {

    private SuperAdminHabilitations() {}

    // 1. Habilitations Comptabilité & Finance
    public static final String COMPTA_READ = "COMPTA_READ";
    public static final String COMPTA_WRITE = "COMPTA_WRITE";
    public static final String CLOTURE_EXERCICE = "CLOTURE_EXERCICE";
    public static final String TRESORERIE_READ = "TRESORERIE_READ";
    public static final String TRESORERIE_GESTION = "TRESORERIE_GESTION";

    // 2. Habilitations Achats & Fournisseurs
    public static final String ACHAT_FACTURE_READ = "ACHAT_FACTURE_READ";
    public static final String ACHAT_FACTURE_CREATE = "ACHAT_FACTURE_CREATE";
    public static final String FOURNISSEUR_READ = "FOURNISSEUR_READ";
    public static final String FOURNISSEUR_GESTION = "FOURNISSEUR_GESTION";

    // 3. Habilitations Ventes & Clients
    public static final String VENTE_READ = "VENTE_READ";
    public static final String VENTE_VALIDER = "VENTE_VALIDER";
    public static final String CLIENT_READ = "CLIENT_READ";
    public static final String CLIENT_GESTION = "CLIENT_GESTION";

    // 4. Habilitations Stocks & Dépôts
    public static final String STOCK_READ = "STOCK_READ";
    public static final String STOCK_AJUSTER = "STOCK_AJUSTER";

    // 5. Pilotage, Rapports & Administration
    public static final String DASHBOARD_VOIR = "DASHBOARD_VOIR";
    public static final String RAPPORT_VOIR = "RAPPORT_VOIR";
    public static final String ADMIN_ROLES = "ADMIN_ROLES";
    public static final String ADMIN_HABILITATIONS = "ADMIN_HABILITATIONS";
    public static final String ADMIN_GESTION = "ADMIN_GESTION";

    // 6. Rôles applicatifs
    public static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_COMPTABLE = "ROLE_COMPTABLE";
    public static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE";
    public static final String ROLE_POINT_DE_VENTE_MANAGER = "ROLE_POINT_DE_VENTE_MANAGER";
    public static final String ROLE_RESPONSABLE_COMMERCIAL = "ROLE_RESPONSABLE_COMMERCIAL";
    public static final String ROLE_RESPONSABLE_ACHAT = "ROLE_RESPONSABLE_ACHAT";
    public static final String ROLE_MAGASINIER = "ROLE_MAGASINIER";
    public static final String ROLE_VENDEUR = "ROLE_VENDEUR";

    /**
     * Catalogue structuré des habilitations métier standard pour l'interface de gestion AgenceWeb.
     */
    public static final java.util.List<String> BUSINESS_PERMISSIONS = java.util.List.of(
            COMPTA_READ, COMPTA_WRITE, CLOTURE_EXERCICE,
            ACHAT_FACTURE_READ, ACHAT_FACTURE_CREATE,
            VENTE_READ, VENTE_VALIDER,
            STOCK_READ, STOCK_AJUSTER,
            TRESORERIE_READ, TRESORERIE_GESTION,
            FOURNISSEUR_READ, FOURNISSEUR_GESTION,
            CLIENT_READ, CLIENT_GESTION,
            DASHBOARD_VOIR, RAPPORT_VOIR,
            ADMIN_ROLES, ADMIN_HABILITATIONS, ADMIN_GESTION
    );

    /**
     * Ensemble exhaustif de toutes les autorités accordées automatiquement au SuperAdmin (Master Key).
     */
    public static final Set<GrantedAuthority> ALL_AUTHORITIES;

    static {
        Set<GrantedAuthority> set = new HashSet<>();

        // Tous les Rôles
        set.add(new SimpleGrantedAuthority(ROLE_SUPERADMIN));
        set.add(new SimpleGrantedAuthority(ROLE_ADMIN));
        set.add(new SimpleGrantedAuthority(ROLE_COMPTABLE));
        set.add(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
        set.add(new SimpleGrantedAuthority(ROLE_POINT_DE_VENTE_MANAGER));
        set.add(new SimpleGrantedAuthority(ROLE_RESPONSABLE_COMMERCIAL));
        set.add(new SimpleGrantedAuthority(ROLE_RESPONSABLE_ACHAT));
        set.add(new SimpleGrantedAuthority(ROLE_MAGASINIER));
        set.add(new SimpleGrantedAuthority(ROLE_VENDEUR));

        // Toutes les Habilitations Métier
        set.add(new SimpleGrantedAuthority(COMPTA_READ));
        set.add(new SimpleGrantedAuthority(COMPTA_WRITE));
        set.add(new SimpleGrantedAuthority(CLOTURE_EXERCICE));
        set.add(new SimpleGrantedAuthority(TRESORERIE_READ));
        set.add(new SimpleGrantedAuthority(TRESORERIE_GESTION));

        set.add(new SimpleGrantedAuthority(ACHAT_FACTURE_READ));
        set.add(new SimpleGrantedAuthority(ACHAT_FACTURE_CREATE));
        set.add(new SimpleGrantedAuthority(FOURNISSEUR_READ));
        set.add(new SimpleGrantedAuthority(FOURNISSEUR_GESTION));

        set.add(new SimpleGrantedAuthority(VENTE_READ));
        set.add(new SimpleGrantedAuthority(VENTE_VALIDER));
        set.add(new SimpleGrantedAuthority(CLIENT_READ));
        set.add(new SimpleGrantedAuthority(CLIENT_GESTION));

        set.add(new SimpleGrantedAuthority(STOCK_READ));
        set.add(new SimpleGrantedAuthority(STOCK_AJUSTER));

        set.add(new SimpleGrantedAuthority(DASHBOARD_VOIR));
        set.add(new SimpleGrantedAuthority(RAPPORT_VOIR));
        set.add(new SimpleGrantedAuthority(ADMIN_ROLES));
        set.add(new SimpleGrantedAuthority(ADMIN_HABILITATIONS));
        set.add(new SimpleGrantedAuthority(ADMIN_GESTION));

        // Anciennes habilitations pour rétrocompatibilité
        set.add(new SimpleGrantedAuthority("COMMANDE_READ"));
        set.add(new SimpleGrantedAuthority("COMMANDE_CREATE"));
        set.add(new SimpleGrantedAuthority("COMMANDE_VALIDATE"));
        set.add(new SimpleGrantedAuthority("FACTURE_READ"));
        set.add(new SimpleGrantedAuthority("FACTURE_CREATE"));
        set.add(new SimpleGrantedAuthority("FACTURE_VALIDER"));
        set.add(new SimpleGrantedAuthority("FACTURE_ANNULER"));
        set.add(new SimpleGrantedAuthority("STOCK_VOIR"));
        set.add(new SimpleGrantedAuthority("STOCK_GESTION"));
        set.add(new SimpleGrantedAuthority("COMPTABILITE_VOIR"));
        set.add(new SimpleGrantedAuthority("USER_READ"));

        ALL_AUTHORITIES = Collections.unmodifiableSet(set);
    }
}
