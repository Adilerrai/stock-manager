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
     * Catalogue exhaustif des 73 habilitations métier synchronisées avec la BDD et AgenceWeb.
     */
    public static final java.util.List<String> BUSINESS_PERMISSIONS = java.util.List.of(
            // Produits (4)
            "PRODUIT_READ", "PRODUIT_CREATE", "PRODUIT_UPDATE", "PRODUIT_DELETE",
            // Stocks (4)
            "STOCK_READ", "STOCK_CREATE", "STOCK_TRANSFERT", "STOCK_INVENTAIRE",
            // Ventes & Devis & Réservations (9)
            "VENTE_READ", "VENTE_CREATE", "VENTE_DELETE",
            "DEVIS_READ", "DEVIS_CREATE", "DEVIS_VALIDER", "DEVIS_DELETE",
            "RESERVATION_READ", "RESERVATION_MANAGE",
            // Livraisons & Expéditions (5)
            "LIVRAISON_READ", "LIVRAISON_CREATE", "LIVRAISON_EXPEDIER", "LIVRAISON_VALIDER", "LIVRAISON_ANNULER",
            // Réceptions Achats (2)
            "RECEPTION_READ", "RECEPTION_VALIDER",
            // Commandes (3)
            "COMMANDE_READ", "COMMANDE_CREATE", "COMMANDE_VALIDATE",
            // Achats Demandes DA & DDP (4)
            "ACHAT_DA_CREATE", "ACHAT_DA_VALIDER", "ACHAT_DDP_CREATE", "ACHAT_DDP_VALIDER",
            // Factures & Avoirs (6)
            "FACTURE_READ", "FACTURE_CREATE", "FACTURE_VALIDER", "FACTURE_ANNULER",
            "AVOIR_VENTE_CREATE", "AVOIR_ACHAT_CREATE",
            // Clients & Espace Client (5)
            "CLIENT_READ", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_COMPTE_CREATE",
            // Fournisseurs (4)
            "FOURNISSEUR_READ", "FOURNISSEUR_CREATE", "FOURNISSEUR_UPDATE", "FOURNISSEUR_DELETE",
            // Paiements & Caisse & Dépenses & Effets (8)
            "PAIEMENT_READ", "PAIEMENT_CREATE", "PAIEMENT_ANNULER", "PAIEMENT_DECISSEMENT",
            "CAISSE_SESSION_MANAGE", "EFFET_GERER", "DEPENSE_READ", "DEPENSE_CREATE",
            // Comptabilité & Fiscalité (5)
            "COMPTA_READ", "COMPTA_ECRITURE", "COMPTA_LETTRAGE", "COMPTA_CLOTURE", "FISCALITE_LIASSE",
            // Trésorerie (3)
            "TRESORERIE_READ", "TRESORERIE_MOUVEMENT", "TRESORERIE_RAPPROCHEMENT",
            // Rapports, Marges sensibles & Audit (4)
            "RAPPORT_READ", "RAPPORT_EXPORT", "VENTE_PRIX_ACHAT_VIEW", "AUDIT_LOG_READ",
            // Utilisateurs (4)
            "USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
            // Administration (3)
            "ADMIN_ENTREPRISE", "ADMIN_ROLES", "ADMIN_HABILITATIONS"
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
        set.add(new SimpleGrantedAuthority("ROLE_CAISSIER"));
        set.add(new SimpleGrantedAuthority("ROLE_COMMERCIAL"));
        set.add(new SimpleGrantedAuthority("ROLE_ASSISTANTE"));

        // Les 40 Habilitations exhaustives du catalogue
        for (String perm : BUSINESS_PERMISSIONS) {
            set.add(new SimpleGrantedAuthority(perm));
        }

        // Alias et rétrocompatibilité
        set.add(new SimpleGrantedAuthority(COMPTA_WRITE));
        set.add(new SimpleGrantedAuthority(CLOTURE_EXERCICE));
        set.add(new SimpleGrantedAuthority(TRESORERIE_GESTION));
        set.add(new SimpleGrantedAuthority(ACHAT_FACTURE_READ));
        set.add(new SimpleGrantedAuthority(ACHAT_FACTURE_CREATE));
        set.add(new SimpleGrantedAuthority(FOURNISSEUR_GESTION));
        set.add(new SimpleGrantedAuthority(CLIENT_GESTION));
        set.add(new SimpleGrantedAuthority(STOCK_AJUSTER));
        set.add(new SimpleGrantedAuthority(DASHBOARD_VOIR));
        set.add(new SimpleGrantedAuthority(RAPPORT_VOIR));
        set.add(new SimpleGrantedAuthority(ADMIN_GESTION));
        set.add(new SimpleGrantedAuthority(VENTE_VALIDER));
        set.add(new SimpleGrantedAuthority("STOCK_VOIR"));
        set.add(new SimpleGrantedAuthority("STOCK_GESTION"));
        set.add(new SimpleGrantedAuthority("COMPTABILITE_VOIR"));

        ALL_AUTHORITIES = Collections.unmodifiableSet(set);
    }
}
