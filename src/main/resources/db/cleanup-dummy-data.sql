-- ==============================================================================
-- SCRIPT DE PURGE COMPLÈTE DES DUMMY DATA (DONNÉES FACTICES)
-- Conserve la structure des tables, les rôles et les permissions système.
-- ==============================================================================

BEGIN;

-- 1. Ventes, Devis, Factures, Commandes et Livraisons
TRUNCATE TABLE lignes_avoir_client CASCADE;
TRUNCATE TABLE avoirs_clients CASCADE;
TRUNCATE TABLE lignes_avoir CASCADE;
TRUNCATE TABLE avoirs CASCADE;
TRUNCATE TABLE lignes_bon_livraison_client CASCADE;
TRUNCATE TABLE bons_livraison_client CASCADE;
TRUNCATE TABLE lignes_bon_preparation CASCADE;
TRUNCATE TABLE bons_preparation CASCADE;
TRUNCATE TABLE lignes_devis CASCADE;
TRUNCATE TABLE devis CASCADE;
TRUNCATE TABLE lignes_commande_client CASCADE;
TRUNCATE TABLE commandes_client CASCADE;
TRUNCATE TABLE lignes_facture CASCADE;
TRUNCATE TABLE factures CASCADE;
TRUNCATE TABLE lignes_vente CASCADE;
TRUNCATE TABLE ventes CASCADE;
TRUNCATE TABLE paiements CASCADE;

-- 2. Achats et Fournisseurs
TRUNCATE TABLE lignes_livraison CASCADE;
TRUNCATE TABLE livraisons CASCADE;
TRUNCATE TABLE lignes_facture_achat CASCADE;
TRUNCATE TABLE factures_achat CASCADE;
TRUNCATE TABLE lignes_commande CASCADE;
TRUNCATE TABLE commandes CASCADE;
TRUNCATE TABLE reglements_fournisseur CASCADE;
TRUNCATE TABLE fournisseurs CASCADE;

-- 3. Clients et CRM
TRUNCATE TABLE relances_clients CASCADE;
TRUNCATE TABLE promesses_paiement CASCADE;
TRUNCATE TABLE objectifs_commerciaux CASCADE;
TRUNCATE TABLE clients CASCADE;

-- 4. Stocks, Produits et Dépôts
TRUNCATE TABLE lignes_transfert_stock CASCADE;
TRUNCATE TABLE transferts_stock CASCADE;
TRUNCATE TABLE lignes_inventaire CASCADE;
TRUNCATE TABLE inventaires CASCADE;
TRUNCATE TABLE mouvements_stock CASCADE;
TRUNCATE TABLE stock_qualites CASCADE;
TRUNCATE TABLE stocks CASCADE;
TRUNCATE TABLE lots CASCADE;
TRUNCATE TABLE produit_images CASCADE;
TRUNCATE TABLE variantes_produit CASCADE;
TRUNCATE TABLE unite_mesure_options CASCADE;
TRUNCATE TABLE produits CASCADE;
TRUNCATE TABLE categories CASCADE;
TRUNCATE TABLE depots CASCADE;

-- 5. Trésorerie, Finances et Comptabilité
TRUNCATE TABLE mouvements_tresorerie CASCADE;
TRUNCATE TABLE cheques_effets CASCADE;
TRUNCATE TABLE cheques_traites CASCADE;
TRUNCATE TABLE bordereaux_remise CASCADE;
TRUNCATE TABLE depenses CASCADE;
TRUNCATE TABLE sessions_caisse CASCADE;
TRUNCATE TABLE comptes_financiers CASCADE;
TRUNCATE TABLE lignes_ecriture CASCADE;
TRUNCATE TABLE ecritures_comptables CASCADE;
TRUNCATE TABLE journaux_comptables CASCADE;
TRUNCATE TABLE comptes_comptables CASCADE;

-- 6. Notifications et Logs
TRUNCATE TABLE notifications CASCADE;

-- 7. Utilisateurs et Profils d'Entreprises factices
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE entreprise_profiles CASCADE;
TRUNCATE TABLE point_de_vente CASCADE;
TRUNCATE TABLE entreprises CASCADE;

-- 8. Réinitialisation des séquences clés
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS point_de_vente_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS entreprise_profiles_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS depots_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS clients_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS fournisseurs_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS produits_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS stocks_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS ventes_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS factures_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS devis_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS paiements_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS categories_id_seq RESTART WITH 1;

COMMIT;
