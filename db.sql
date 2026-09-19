-- =============================================================================
-- SIYANA ERP / POINT DE VENTE - POSTGRESQL DATABASE SCHEMA (db.sql)
-- Target Database : PostgreSQL 13+ (pointvente_db)
-- Models Covered  : All 75 JPA Entity Models + Join Tables (76 tables total)
-- Dialect         : org.hibernate.dialect.PostgreSQLDialect
-- Encoding        : UTF-8
-- Date Generated  : 2026-09-19
-- =============================================================================

-- Activer les extensions requises
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 0. NETTOYAGE PREALABLE (A decommenter pour reinitialiser la base)
-- =============================================================================
/*
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS documents_comptables CASCADE;
DROP TABLE IF EXISTS echeances_fiscales CASCADE;
DROP TABLE IF EXISTS declarations_tva CASCADE;
DROP TABLE IF EXISTS regles_fiscales_is CASCADE;
DROP TABLE IF EXISTS lignes_plan_amortissement CASCADE;
DROP TABLE IF EXISTS immobilisations CASCADE;
DROP TABLE IF EXISTS ventilations_analytiques CASCADE;
DROP TABLE IF EXISTS centres_analytiques CASCADE;
DROP TABLE IF EXISTS axes_analytiques CASCADE;
DROP TABLE IF EXISTS lignes_ecriture CASCADE;
DROP TABLE IF EXISTS ecritures_comptables CASCADE;
DROP TABLE IF EXISTS comptes_comptables CASCADE;
DROP TABLE IF EXISTS journaux_comptables CASCADE;
DROP TABLE IF EXISTS exercices_comptables CASCADE;
DROP TABLE IF EXISTS depenses CASCADE;
DROP TABLE IF EXISTS cheques_effets CASCADE;
DROP TABLE IF EXISTS bordereaux_remise CASCADE;
DROP TABLE IF EXISTS lignes_releves_bancaires CASCADE;
DROP TABLE IF EXISTS releves_bancaires CASCADE;
DROP TABLE IF EXISTS mouvements_tresorerie CASCADE;
DROP TABLE IF EXISTS comptes_financiers CASCADE;
DROP TABLE IF EXISTS banques CASCADE;
DROP TABLE IF EXISTS promesses_paiement CASCADE;
DROP TABLE IF EXISTS relances_clients CASCADE;
DROP TABLE IF EXISTS paiement_affectations CASCADE;
DROP TABLE IF EXISTS paiements CASCADE;
DROP TABLE IF EXISTS lignes_avoir CASCADE;
DROP TABLE IF EXISTS avoirs CASCADE;
DROP TABLE IF EXISTS lignes_facture CASCADE;
DROP TABLE IF EXISTS factures CASCADE;
DROP TABLE IF EXISTS lignes_vente CASCADE;
DROP TABLE IF EXISTS ventes CASCADE;
DROP TABLE IF EXISTS lignes_bon_livraison_client CASCADE;
DROP TABLE IF EXISTS bons_livraison_client CASCADE;
DROP TABLE IF EXISTS lignes_bon_preparation CASCADE;
DROP TABLE IF EXISTS bons_preparation CASCADE;
DROP TABLE IF EXISTS lignes_commande_client CASCADE;
DROP TABLE IF EXISTS commandes_client CASCADE;
DROP TABLE IF EXISTS lignes_devis CASCADE;
DROP TABLE IF EXISTS devis CASCADE;
DROP TABLE IF EXISTS sessions_caisse CASCADE;
DROP TABLE IF EXISTS objectifs_commerciaux CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS reglements_fournisseur CASCADE;
DROP TABLE IF EXISTS lignes_facture_achat CASCADE;
DROP TABLE IF EXISTS factures_achat CASCADE;
DROP TABLE IF EXISTS lignes_livraison CASCADE;
DROP TABLE IF EXISTS livraisons CASCADE;
DROP TABLE IF EXISTS lignes_commande CASCADE;
DROP TABLE IF EXISTS commandes CASCADE;
DROP TABLE IF EXISTS fournisseurs CASCADE;
DROP TABLE IF EXISTS lignes_inventaire CASCADE;
DROP TABLE IF EXISTS inventaires CASCADE;
DROP TABLE IF EXISTS lignes_transfert_stock CASCADE;
DROP TABLE IF EXISTS transferts_stock CASCADE;
DROP TABLE IF EXISTS mouvements_stock CASCADE;
DROP TABLE IF EXISTS lots CASCADE;
DROP TABLE IF EXISTS stock_qualites CASCADE;
DROP TABLE IF EXISTS stocks CASCADE;
DROP TABLE IF EXISTS depots CASCADE;
DROP TABLE IF EXISTS variantes_produit CASCADE;
DROP TABLE IF EXISTS produit_images CASCADE;
DROP TABLE IF EXISTS produits CASCADE;
DROP TABLE IF EXISTS banques CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS unite_mesure_options CASCADE;
DROP TABLE IF EXISTS entreprise_profiles CASCADE;
DROP TABLE IF EXISTS collaborateurs_societes CASCADE;
DROP TABLE IF EXISTS societes CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles_habilitations CASCADE;
DROP TABLE IF EXISTS habilitations CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS point_de_vente CASCADE;
DROP TABLE IF EXISTS meres CASCADE;
*/

-- =============================================================================
-- 1. AUTHENTIFICATION, MULTI-TENANT & STRUCTURE DE L'ENTREPRISE
-- =============================================================================

-- Table : meres (Holding & Societes Meres)
CREATE TABLE IF NOT EXISTS meres (
    id                             BIGSERIAL PRIMARY KEY,
    nom                            VARCHAR(200) NOT NULL,
    ice                            VARCHAR(30),
    rc                             VARCHAR(50),
    identifiant_fiscal             VARCHAR(50),
    patente                        VARCHAR(50),
    forme_juridique                VARCHAR(50),
    adresse                        TEXT,
    ville                          VARCHAR(100),
    code_postal                    VARCHAR(20),
    telephone                      VARCHAR(50),
    email                          VARCHAR(150),
    site_web                       VARCHAR(200),
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table : point_de_vente (Points de Vente & Magasins (Multi-Tenant))
CREATE TABLE IF NOT EXISTS point_de_vente (
    id                             BIGSERIAL PRIMARY KEY,
    mere_id                        BIGINT,
    tenant_id                      BIGINT DEFAULT 1 NOT NULL,
    nom_point_de_vente             VARCHAR(255) NOT NULL UNIQUE,
    nom                            VARCHAR(255),
    adresse                        TEXT,
    telephone                      VARCHAR(50),
    email                          VARCHAR(255),
    password                       VARCHAR(255),
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actif                          BOOLEAN DEFAULT TRUE,
    module_commercial_actif        BOOLEAN DEFAULT TRUE,
    module_comptabilite_actif      BOOLEAN DEFAULT TRUE,
    module_fiscalite_actif         BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_point_de_vente_mere FOREIGN KEY (mere_id) REFERENCES meres(id) ON DELETE SET NULL
);

-- Table : roles (Roles Utilisateur & Autorisations)
CREATE TABLE IF NOT EXISTS roles (
    id                             BIGSERIAL PRIMARY KEY,
    nom                            VARCHAR(255)
);

-- Table : habilitations (Habilitations & Permissions Fines)
CREATE TABLE IF NOT EXISTS habilitations (
    id                             BIGSERIAL PRIMARY KEY,
    nom                            VARCHAR(255)
);

-- Table : roles_habilitations (Table de Jointure Roles <-> Habilitations)
CREATE TABLE IF NOT EXISTS roles_habilitations (
    role_id                        BIGINT NOT NULL,
    habilitation_id                BIGINT NOT NULL,
    CONSTRAINT pk_roles_habilitations PRIMARY KEY (role_id, habilitation_id),
    CONSTRAINT fk_rh_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rh_habilitation FOREIGN KEY (habilitation_id) REFERENCES habilitations(id) ON DELETE CASCADE
);

-- Table : users (Comptes Utilisateurs & Collaborateurs)
CREATE TABLE IF NOT EXISTS users (
    id                             BIGSERIAL PRIMARY KEY,
    email                          VARCHAR(255) NOT NULL UNIQUE,
    username                       VARCHAR(255),
    password                       VARCHAR(255) NOT NULL,
    role_id                        BIGINT,
    nom_complet                    VARCHAR(255),
    telephone                      VARCHAR(255),
    genre                          VARCHAR(50),
    mere_id                        BIGINT,
    tenant_id                      BIGINT DEFAULT 1 NOT NULL,
    point_de_vente_id              BIGINT,
    account_non_expired            BOOLEAN DEFAULT TRUE,
    account_non_locked             BOOLEAN DEFAULT TRUE,
    credentials_non_expired        BOOLEAN DEFAULT TRUE,
    enabled                        BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_users_mere FOREIGN KEY (mere_id) REFERENCES meres(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE SET NULL
);

-- Table : societes (Societes & Dossiers Clients)
CREATE TABLE IF NOT EXISTS societes (
    id                             BIGSERIAL PRIMARY KEY,
    mere_id                        BIGINT,
    tenant_id                      BIGINT DEFAULT 1 NOT NULL,
    code                           VARCHAR(50) NOT NULL,
    raison_sociale                 VARCHAR(200) NOT NULL,
    forme_juridique                VARCHAR(50),
    ice                            VARCHAR(30),
    rc                             VARCHAR(50),
    identifiant_fiscal             VARCHAR(50),
    patente                        VARCHAR(50),
    cnss                           VARCHAR(50),
    capital_social                 NUMERIC(15, 2) DEFAULT 0.00,
    adresse                        TEXT,
    ville                          VARCHAR(100),
    code_postal                    VARCHAR(20),
    telephone                      VARCHAR(50),
    email                          VARCHAR(150),
    site_web                       VARCHAR(200),
    activite_principale            VARCHAR(255),
    regime_tva                     VARCHAR(50),
    periodicite_tva                VARCHAR(20),
    exercice_en_cours              INTEGER DEFAULT 2026,
    date_cloture_exercice          VARCHAR(20),
    responsable_dossier            VARCHAR(150),
    is_par_defaut                  BOOLEAN DEFAULT FALSE,
    actif                          BOOLEAN DEFAULT TRUE,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_societes_mere FOREIGN KEY (mere_id) REFERENCES meres(id) ON DELETE SET NULL
);

-- Table : collaborateurs_societes (Affectations Utilisateurs aux Societes)
CREATE TABLE IF NOT EXISTS collaborateurs_societes (
    id                             BIGSERIAL PRIMARY KEY,
    user_id                        BIGINT NOT NULL,
    societe_id                     BIGINT NOT NULL,
    role_dossier                   VARCHAR(50),
    date_affectation               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collaborateurs_societes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_collaborateurs_societes_societe FOREIGN KEY (societe_id) REFERENCES societes(id) ON DELETE CASCADE
);

-- Table : entreprise_profiles (Profil & Parametres de l'Entreprise)
CREATE TABLE IF NOT EXISTS entreprise_profiles (
    id                             BIGSERIAL PRIMARY KEY,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL UNIQUE,
    nom_entreprise                 VARCHAR(255) NOT NULL,
    activite                       VARCHAR(255),
    adresse                        TEXT,
    ville                          VARCHAR(255),
    code_postal                    VARCHAR(255),
    telephone                      VARCHAR(255),
    telephone_secondaire           VARCHAR(255),
    email                          VARCHAR(255),
    site_web                       VARCHAR(255),
    registre_commerce              VARCHAR(255),
    numero_identification_fiscale  VARCHAR(255),
    numero_identification_statistique VARCHAR(255),
    article_imposition             VARCHAR(255),
    compte_bancaire_rib            VARCHAR(255),
    nom_banque                     VARCHAR(255),
    logo_data                      BYTEA,
    logo_content_type              VARCHAR(255),
    logo_file_name                 VARCHAR(255),
    pied_page                      VARCHAR(1000),
    devise                         VARCHAR(255),
    vente_stock_negatif            BOOLEAN DEFAULT FALSE,
    date_mise_a_jour               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entreprise_profiles_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE CASCADE
);

-- =============================================================================
-- 2. REFERENTIEL & CONFIGURATION
-- =============================================================================

-- Table : unite_mesure_options (Unites de Mesure (KG, M, L, Piece, etc.))
CREATE TABLE IF NOT EXISTS unite_mesure_options (
    id                             BIGSERIAL PRIMARY KEY,
    value                          VARCHAR(255),
    label                          VARCHAR(255)
);

-- Table : categories (Categories & Familles de Produits)
CREATE TABLE IF NOT EXISTS categories (
    id                             BIGSERIAL PRIMARY KEY,
    nom                            VARCHAR(255) NOT NULL,
    code                           VARCHAR(255),
    description                    TEXT,
    couleur                        VARCHAR(255),
    icone                          VARCHAR(255),
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    parent_id                      BIGINT,
    point_de_vente_id              BIGINT,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_categories_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : banques (Banques de Reference)
CREATE TABLE IF NOT EXISTS banques (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(50) NOT NULL UNIQUE,
    nom                            VARCHAR(150) NOT NULL,
    nom_court                      VARCHAR(50),
    code_swift                     VARCHAR(20),
    logo                           VARCHAR(255),
    couleur                        VARCHAR(20),
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_banques_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- =============================================================================
-- 3. CATALOGUE ARTICLES & PRODUITS
-- =============================================================================

-- Table : produits (Articles, Produits & Services)
CREATE TABLE IF NOT EXISTS produits (
    id                             BIGSERIAL PRIMARY KEY,
    reference                      VARCHAR(255) NOT NULL UNIQUE,
    designation                    VARCHAR(255),
    description                    TEXT NOT NULL,
    actif                          BOOLEAN DEFAULT TRUE,
    groupe_article                 VARCHAR(255),
    code_barre                     VARCHAR(255),
    categorie_id                   BIGINT,
    categorie_article              VARCHAR(255),
    attributes                     JSONB DEFAULT '{}'::jsonb,
    prix_achat_ht                  NUMERIC(15, 2) DEFAULT 0.00,
    prix_achat_ttc                 NUMERIC(15, 2) DEFAULT 0.00,
    prix_vente_ht                  NUMERIC(15, 2) DEFAULT 0.00,
    prix_vente_ttc                 NUMERIC(15, 2) DEFAULT 0.00,
    image_id                       BIGINT,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    prix_achat                     NUMERIC(15, 2) DEFAULT 0.00,
    prix_vente                     NUMERIC(15, 2) DEFAULT 0.00,
    unite_mesure_stock             VARCHAR(50),
    stock_minimum                  NUMERIC(15, 2),
    point_de_vente_id              BIGINT,
    CONSTRAINT fk_produits_categorie FOREIGN KEY (categorie_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_produits_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : produit_images (Images & Photos des Produits)
CREATE TABLE IF NOT EXISTS produit_images (
    id                             BIGSERIAL PRIMARY KEY,
    produit_id                     BIGINT NOT NULL,
    file_name                      VARCHAR(255),
    image_data                     BYTEA,
    content_type                   VARCHAR(255),
    date_upload                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_produit_images_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE CASCADE
);

-- Table : variantes_produit (Declinaisons & Variantes Produits (Taille, Couleur))
CREATE TABLE IF NOT EXISTS variantes_produit (
    id                             BIGSERIAL PRIMARY KEY,
    produit_parent_id              BIGINT NOT NULL,
    sku                            VARCHAR(255) NOT NULL UNIQUE,
    code_barre                     VARCHAR(255),
    nom_variante                   VARCHAR(255) NOT NULL,
    taille                         VARCHAR(255),
    couleur                        VARCHAR(255),
    dimension                      VARCHAR(255),
    prix_vente                     NUMERIC(15, 2) DEFAULT 0.00,
    prix_achat                     NUMERIC(15, 2) DEFAULT 0.00,
    quantite_stock                 NUMERIC(12, 3) DEFAULT 0.000,
    actif                          BOOLEAN DEFAULT TRUE,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_variantes_produit_produit_parent FOREIGN KEY (produit_parent_id) REFERENCES produits(id) ON DELETE CASCADE
);

-- =============================================================================
-- 4. GESTION DES DEPOTS & DU STOCK
-- =============================================================================

-- Table : depots (Depots & Entrepots de Stockage)
CREATE TABLE IF NOT EXISTS depots (
    id                             BIGSERIAL PRIMARY KEY,
    nom                            VARCHAR(255) NOT NULL,
    description                    TEXT,
    adresse                        TEXT NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actif                          BOOLEAN DEFAULT TRUE,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_depots_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : stocks (Niveaux de Stock par Produit & Depot)
CREATE TABLE IF NOT EXISTS stocks (
    id                             BIGSERIAL PRIMARY KEY,
    produit_id                     BIGINT NOT NULL,
    quantite_disponible            NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    quantite_reservee              NUMERIC(12, 2) DEFAULT 0.00,
    seuil_alerte                   NUMERIC(12, 2),
    derniere_maj                   TIMESTAMP,
    CONSTRAINT fk_stocks_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE CASCADE
);

-- Table : stock_qualites (Repartition des Stocks par Qualite (1er choix, etc.))
CREATE TABLE IF NOT EXISTS stock_qualites (
    id                             BIGSERIAL PRIMARY KEY,
    qualite_produit                VARCHAR(50) NOT NULL,
    quantite_disponible            NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    quantite_reservee              NUMERIC(10, 2) DEFAULT 0.00,
    seuil_alerte                   NUMERIC(10, 2),
    derniere_maj                   TIMESTAMP,
    stock_id                       BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    CONSTRAINT fk_stock_qualites_stock FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_qualites_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE CASCADE
);

-- Table : lots (Lots & Numeros de Serie)
CREATE TABLE IF NOT EXISTS lots (
    id                             BIGSERIAL PRIMARY KEY,
    numero_lot                     VARCHAR(255) NOT NULL UNIQUE,
    produit_id                     BIGINT NOT NULL,
    depot_id                       BIGINT NOT NULL,
    qualite                        VARCHAR(50) NOT NULL,
    quantite_initiale              NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    quantite_disponible            NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    quantite_reservee              NUMERIC(10, 2) DEFAULT 0.00,
    date_fabrication               DATE,
    date_expiration                DATE,
    date_reception                 TIMESTAMP NOT NULL,
    prix_achat_unitaire            NUMERIC(10, 2) DEFAULT 0.00,
    numero_livraison               VARCHAR(255),
    fournisseur                    VARCHAR(255),
    observations                   TEXT,
    actif                          BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_lots_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE CASCADE,
    CONSTRAINT fk_lots_depot FOREIGN KEY (depot_id) REFERENCES depots(id) ON DELETE RESTRICT
);

-- Table : mouvements_stock (Mouvements de Stock (Entrees, Sorties, Ajustements))
CREATE TABLE IF NOT EXISTS mouvements_stock (
    id                             BIGSERIAL PRIMARY KEY,
    produit_id                     BIGINT NOT NULL,
    type_mouvement                 VARCHAR(50) NOT NULL,
    quantite                       NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    quantite_avant                 NUMERIC(10, 2) DEFAULT 0.00,
    quantite_apres                 NUMERIC(10, 2) DEFAULT 0.00,
    reference_document             VARCHAR(255),
    motif                          TEXT,
    date_mouvement                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    utilisateur                    VARCHAR(255),
    lot_id                         BIGINT,
    qualite_produit                VARCHAR(50),
    numero_lot_externe             VARCHAR(255),
    depot_id                       BIGINT NOT NULL,
    point_de_vente_id              BIGINT,
    CONSTRAINT fk_mouvements_stock_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mouvements_stock_lot FOREIGN KEY (lot_id) REFERENCES lots(id) ON DELETE SET NULL,
    CONSTRAINT fk_mouvements_stock_depot FOREIGN KEY (depot_id) REFERENCES depots(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mouvements_stock_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : transferts_stock (Entetes de Transferts Inter-Depots)
CREATE TABLE IF NOT EXISTS transferts_stock (
    id                             BIGSERIAL PRIMARY KEY,
    numero_transfert               VARCHAR(50) NOT NULL,
    depot_source_id                BIGINT NOT NULL,
    depot_destination_id           BIGINT NOT NULL,
    date_transfert                 DATE NOT NULL,
    date_expedition                TIMESTAMP,
    date_reception                 DATE,
    statut                         VARCHAR(50) NOT NULL,
    motif                          TEXT,
    notes                          TEXT,
    cree_par_user_id               BIGINT,
    valide_par_user_id             BIGINT,
    expedie_par_user_id            BIGINT,
    recu_par_user_id               BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transferts_stock_depot_source FOREIGN KEY (depot_source_id) REFERENCES depots(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transferts_stock_depot_destination FOREIGN KEY (depot_destination_id) REFERENCES depots(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transferts_stock_cree_par_user FOREIGN KEY (cree_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transferts_stock_valide_par_user FOREIGN KEY (valide_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transferts_stock_expedie_par_user FOREIGN KEY (expedie_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transferts_stock_recu_par_user FOREIGN KEY (recu_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transferts_stock_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_transfert_stock (Lignes de Details des Transferts de Stock)
CREATE TABLE IF NOT EXISTS lignes_transfert_stock (
    id                             BIGSERIAL PRIMARY KEY,
    transfert_id                   BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite_demandee              NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    quantite_expediee              NUMERIC(12, 2) DEFAULT 0.00,
    quantite_recue                 NUMERIC(12, 2) DEFAULT 0.00,
    notes                          TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_lignes_transfert_stock_transfert FOREIGN KEY (transfert_id) REFERENCES transferts_stock(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_transfert_stock_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT,
    CONSTRAINT fk_lignes_transfert_stock_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : inventaires (Sessions d'Inventaire Physique)
CREATE TABLE IF NOT EXISTS inventaires (
    id                             BIGSERIAL PRIMARY KEY,
    reference                      VARCHAR(255) NOT NULL UNIQUE,
    date_inventaire                DATE NOT NULL,
    depot_id                       BIGINT NOT NULL,
    responsable_user_id            BIGINT,
    statut                         VARCHAR(50) NOT NULL,
    total_ecart_positif            NUMERIC(15, 2) DEFAULT 0.00,
    total_ecart_negatif            NUMERIC(15, 2) DEFAULT 0.00,
    valeur_totale_ecart            NUMERIC(15, 2) DEFAULT 0.00,
    notes                          TEXT,
    point_de_vente_id              BIGINT,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_validation                TIMESTAMP,
    CONSTRAINT fk_inventaires_depot FOREIGN KEY (depot_id) REFERENCES depots(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventaires_responsable_user FOREIGN KEY (responsable_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventaires_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_inventaire (Lignes de Comptage d'Inventaire)
CREATE TABLE IF NOT EXISTS lignes_inventaire (
    id                             BIGSERIAL PRIMARY KEY,
    inventaire_id                  BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    qualite                        VARCHAR(50),
    quantite_theorique             NUMERIC(12, 3) DEFAULT 0.000 NOT NULL,
    quantite_reelle                NUMERIC(12, 3) DEFAULT 0.000,
    ecart                          NUMERIC(12, 3),
    prix_unitaire                  NUMERIC(15, 2) DEFAULT 0.00,
    valeur_ecart                   NUMERIC(15, 2),
    CONSTRAINT fk_lignes_inventaire_inventaire FOREIGN KEY (inventaire_id) REFERENCES inventaires(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_inventaire_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- =============================================================================
-- 5. ACHATS & FOURNISSEURS
-- =============================================================================

-- Table : fournisseurs (Fournisseurs & Prestataires)
CREATE TABLE IF NOT EXISTS fournisseurs (
    id                             BIGSERIAL PRIMARY KEY,
    raison_social                  VARCHAR(255) NOT NULL,
    adresse                        TEXT,
    telephone                      VARCHAR(255),
    email                          VARCHAR(255),
    contact                        VARCHAR(255),
    ice                            VARCHAR(255),
    numero_registre_commerce       VARCHAR(255),
    numero_identification_fiscale  VARCHAR(255),
    patente                        VARCHAR(255),
    rib_bancaire                   VARCHAR(255),
    banque_nom                     VARCHAR(255),
    delai_paiement_jours           INTEGER,
    ville                          VARCHAR(255),
    code_postal                    VARCHAR(255),
    pays                           VARCHAR(255),
    conditions_paiement            VARCHAR(255),
    actif                          BOOLEAN DEFAULT TRUE,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fournisseurs_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : commandes (Bons de Commande Fournisseur)
CREATE TABLE IF NOT EXISTS commandes (
    id                             BIGSERIAL PRIMARY KEY,
    numero_commande                VARCHAR(255) NOT NULL UNIQUE,
    fournisseur_id                 BIGINT NOT NULL,
    statut                         VARCHAR(50),
    statut_livraison               VARCHAR(50),
    date_commande                  TIMESTAMP,
    date_livraison_prevue          TIMESTAMP,
    date_livraison_reelle          TIMESTAMP,
    montant_total                  NUMERIC(10, 2) DEFAULT 0.00,
    observations                   TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_commandes_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_commandes_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_commande (Lignes de Bons de Commande Fournisseur)
CREATE TABLE IF NOT EXISTS lignes_commande (
    id                             BIGSERIAL PRIMARY KEY,
    commande_id                    BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite_commandee             INTEGER,
    quantite_livree                INTEGER,
    prix_unitaire                  NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    montant_ligne                  NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    qualite_produit                VARCHAR(50),
    CONSTRAINT fk_lignes_commande_commande FOREIGN KEY (commande_id) REFERENCES commandes(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_commande_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : livraisons (Bons de Reception / Livraisons Fournisseur)
CREATE TABLE IF NOT EXISTS livraisons (
    id                             BIGSERIAL PRIMARY KEY,
    numero_livraison               VARCHAR(255) NOT NULL UNIQUE,
    commande_id                    BIGINT,
    date_livraison                 TIMESTAMP,
    transporteur                   VARCHAR(255),
    montant_total                  NUMERIC(15, 2) DEFAULT 0.00,
    observations                   TEXT,
    statut                         VARCHAR(50),
    numero_suivi                   VARCHAR(255),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_livraisons_commande FOREIGN KEY (commande_id) REFERENCES commandes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_livraisons_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_livraison (Lignes de Reception Fournisseur)
CREATE TABLE IF NOT EXISTS lignes_livraison (
    id                             BIGSERIAL PRIMARY KEY,
    livraison_id                   BIGINT NOT NULL,
    depot_id                       BIGINT NOT NULL,
    quantite_livree                BIGINT,
    produit_id                     BIGINT NOT NULL,
    prix_produit                   NUMERIC(15, 2) DEFAULT 0.00,
    qualite_produit                VARCHAR(50),
    CONSTRAINT fk_lignes_livraison_livraison FOREIGN KEY (livraison_id) REFERENCES livraisons(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_livraison_depot FOREIGN KEY (depot_id) REFERENCES depots(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_livraison_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : factures_achat (Factures d'Achat Fournisseur)
CREATE TABLE IF NOT EXISTS factures_achat (
    id                             BIGSERIAL PRIMARY KEY,
    numero_facture                 VARCHAR(255) NOT NULL UNIQUE,
    date_facture                   TIMESTAMP NOT NULL,
    date_echeance                  TIMESTAMP,
    fournisseur_id                 BIGINT NOT NULL,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00,
    statut                         VARCHAR(50),
    observations                   TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_factures_achat_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_factures_achat_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_facture_achat (Lignes de Factures d'Achat)
CREATE TABLE IF NOT EXISTS lignes_facture_achat (
    id                             BIGSERIAL PRIMARY KEY,
    facture_achat_id               BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite                       NUMERIC(15, 3) DEFAULT 0.000 NOT NULL,
    prix_unitaire_ht               NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    taux_tva                       NUMERIC(5, 2) DEFAULT 0.00,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00,
    CONSTRAINT fk_lignes_facture_achat_facture_achat FOREIGN KEY (facture_achat_id) REFERENCES factures_achat(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_facture_achat_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : reglements_fournisseur (Reglements & Paiements Fournisseurs)
CREATE TABLE IF NOT EXISTS reglements_fournisseur (
    id                             BIGSERIAL PRIMARY KEY,
    numero_reglement               VARCHAR(255) NOT NULL UNIQUE,
    date_reglement                 TIMESTAMP NOT NULL,
    facture_achat_id               BIGINT NOT NULL,
    montant                        NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    mode_paiement                  VARCHAR(50) NOT NULL,
    reference_paiement             VARCHAR(255),
    notes                          TEXT,
    nom_banque                     VARCHAR(255),
    numero_cheque                  VARCHAR(255),
    date_echeance                  TIMESTAMP,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_reglements_fournisseur_facture_achat FOREIGN KEY (facture_achat_id) REFERENCES factures_achat(id) ON DELETE SET NULL,
    CONSTRAINT fk_reglements_fournisseur_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- =============================================================================
-- 6. VENTES, CLIENTS & COMMERCE
-- =============================================================================

-- Table : clients (Fichier Clients & Prospects)
CREATE TABLE IF NOT EXISTS clients (
    id                             BIGSERIAL PRIMARY KEY,
    nom                            VARCHAR(255) NOT NULL,
    prenom                         VARCHAR(255),
    nom_complet                    VARCHAR(255),
    telephone                      VARCHAR(255),
    email                          VARCHAR(255),
    adresse                        TEXT,
    ville                          VARCHAR(255),
    code_postal                    VARCHAR(255),
    categorie                      VARCHAR(50),
    numero_registre_commerce       VARCHAR(255),
    numero_identification_fiscale  VARCHAR(255),
    ice                            VARCHAR(255),
    tarif                          VARCHAR(50),
    delai_paiement_jours           INTEGER,
    remise_defaut                  NUMERIC(5, 2),
    commercial_user_id             BIGINT,
    credit_autorise                NUMERIC(15, 2) DEFAULT 0.00,
    credit_utilise                 NUMERIC(15, 2) DEFAULT 0.00,
    points_fidelite                INTEGER,
    actif                          BOOLEAN DEFAULT TRUE,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_derniere_visite           TIMESTAMP,
    notes                          TEXT,
    CONSTRAINT fk_clients_commercial_user FOREIGN KEY (commercial_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_clients_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : objectifs_commerciaux (Objectifs Commerciaux & Quotas)
CREATE TABLE IF NOT EXISTS objectifs_commerciaux (
    id                             BIGSERIAL PRIMARY KEY,
    commercial_user_id             BIGINT NOT NULL,
    annee                          INTEGER NOT NULL,
    mois                           INTEGER NOT NULL,
    objectif_ca                    NUMERIC(15, 2) NOT NULL,
    objectif_marge                 NUMERIC(15, 2),
    notes                          TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_objectifs_commerciaux_commercial_user FOREIGN KEY (commercial_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_objectifs_commerciaux_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : sessions_caisse (Sessions & Clotures de Caisse)
CREATE TABLE IF NOT EXISTS sessions_caisse (
    id                             BIGSERIAL PRIMARY KEY,
    reference                      VARCHAR(255) NOT NULL UNIQUE,
    date_ouverture                 TIMESTAMP NOT NULL,
    date_cloture                   TIMESTAMP,
    caissier_user_id               BIGINT NOT NULL,
    fond_de_caisse_initial         NUMERIC(15, 2) NOT NULL,
    total_ventes                   NUMERIC(15, 2) DEFAULT 0.00,
    total_especes                  NUMERIC(15, 2) DEFAULT 0.00,
    total_carte                    NUMERIC(15, 2) DEFAULT 0.00,
    total_cheque                   NUMERIC(15, 2) DEFAULT 0.00,
    total_virement                 NUMERIC(15, 2) DEFAULT 0.00,
    total_credit                   NUMERIC(15, 2) DEFAULT 0.00,
    montant_theorique_cloture      NUMERIC(15, 2) DEFAULT 0.00,
    montant_reel_cloture           NUMERIC(15, 2) DEFAULT 0.00,
    ecart_caisse                   NUMERIC(15, 2),
    statut                         VARCHAR(50) NOT NULL,
    notes                          TEXT,
    point_de_vente_id              BIGINT,
    CONSTRAINT fk_sessions_caisse_caissier_user FOREIGN KEY (caissier_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_sessions_caisse_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : devis (Devis & Propositions Commerciales)
CREATE TABLE IF NOT EXISTS devis (
    id                             BIGSERIAL PRIMARY KEY,
    numero_devis                   VARCHAR(255) NOT NULL UNIQUE,
    date_devis                     DATE NOT NULL,
    date_validite                  DATE,
    client_id                      BIGINT NOT NULL,
    cree_par_user_id               BIGINT,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    remise_globale                 NUMERIC(15, 2),
    montant_final                  NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    statut                         VARCHAR(50) NOT NULL,
    notes                          TEXT,
    conditions_paiement            VARCHAR(255),
    point_de_vente_id              BIGINT,
    commande_generee_id            BIGINT,
    facture_generee_id             BIGINT,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_devis_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_devis_cree_par_user FOREIGN KEY (cree_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_devis_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_devis (Lignes de Devis Commercial)
CREATE TABLE IF NOT EXISTS lignes_devis (
    id                             BIGSERIAL PRIMARY KEY,
    devis_id                       BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite                       NUMERIC(12, 3) DEFAULT 0.000 NOT NULL,
    prix_unitaire_ht               NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    taux_tva                       NUMERIC(5, 2) DEFAULT 0.00,
    taux_remise                    NUMERIC(5, 2) DEFAULT 0.00,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00,
    description                    TEXT,
    CONSTRAINT fk_lignes_devis_devis FOREIGN KEY (devis_id) REFERENCES devis(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_devis_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : commandes_client (Commandes Clients)
CREATE TABLE IF NOT EXISTS commandes_client (
    id                             BIGSERIAL PRIMARY KEY,
    numero_commande                VARCHAR(255) NOT NULL UNIQUE,
    client_id                      BIGINT,
    client_nom                     VARCHAR(255),
    client_telephone               VARCHAR(255),
    client_email                   VARCHAR(255),
    adresse_livraison              VARCHAR(255),
    statut                         VARCHAR(50),
    date_commande                  TIMESTAMP,
    date_livraison_prevue          TIMESTAMP,
    montant_ht                     NUMERIC(10, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(10, 2) DEFAULT 0.00,
    taux_tva                       NUMERIC(5, 2) DEFAULT 0.00,
    observations                   TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_commandes_client_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_commandes_client_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_commande_client (Lignes de Commandes Clients)
CREATE TABLE IF NOT EXISTS lignes_commande_client (
    id                             BIGSERIAL PRIMARY KEY,
    commande_client_id             BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite                       NUMERIC(10, 3) DEFAULT 0.000,
    prix_unitaire                  NUMERIC(10, 2) DEFAULT 0.00,
    montant_ligne                  NUMERIC(10, 2) DEFAULT 0.00,
    observations                   TEXT,
    CONSTRAINT fk_lignes_commande_client_commande_client FOREIGN KEY (commande_client_id) REFERENCES commandes_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_commande_client_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : bons_preparation (Bons de Preparation Magasin)
CREATE TABLE IF NOT EXISTS bons_preparation (
    id                             BIGSERIAL PRIMARY KEY,
    numero_preparation             VARCHAR(255) NOT NULL UNIQUE,
    commande_client_id             BIGINT NOT NULL,
    client_id                      BIGINT NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_preparation               TIMESTAMP,
    statut                         VARCHAR(50) NOT NULL,
    magasinier_user_id             BIGINT,
    notes                          TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_bons_preparation_commande_client FOREIGN KEY (commande_client_id) REFERENCES commandes_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_bons_preparation_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bons_preparation_magasinier_user FOREIGN KEY (magasinier_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bons_preparation_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_bon_preparation (Lignes de Preparation Magasin)
CREATE TABLE IF NOT EXISTS lignes_bon_preparation (
    id                             BIGSERIAL PRIMARY KEY,
    bon_preparation_id             BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite_commandee             NUMERIC(12, 3) DEFAULT 0.000 NOT NULL,
    quantite_preparee              NUMERIC(12, 3) DEFAULT 0.000 NOT NULL,
    emplacement_depot              VARCHAR(255),
    statut_ligne                   VARCHAR(255),
    CONSTRAINT fk_lignes_bon_preparation_bon_preparation FOREIGN KEY (bon_preparation_id) REFERENCES bons_preparation(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_bon_preparation_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : bons_livraison_client (Bons de Livraison Client)
CREATE TABLE IF NOT EXISTS bons_livraison_client (
    id                             BIGSERIAL PRIMARY KEY,
    numero_bl                      VARCHAR(255) NOT NULL UNIQUE,
    date_bl                        TIMESTAMP NOT NULL,
    client_id                      BIGINT NOT NULL,
    commande_client_id             BIGINT,
    statut                         VARCHAR(50),
    montant_total                  NUMERIC(15, 2) DEFAULT 0.00,
    observations                   TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    facture_id                     BIGINT,
    CONSTRAINT fk_bons_livraison_client_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bons_livraison_client_commande_client FOREIGN KEY (commande_client_id) REFERENCES commandes_client(id) ON DELETE SET NULL,
    CONSTRAINT fk_bons_livraison_client_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_bon_livraison_client (Lignes de Bons de Livraison Client)
CREATE TABLE IF NOT EXISTS lignes_bon_livraison_client (
    id                             BIGSERIAL PRIMARY KEY,
    bon_livraison_client_id        BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite_livree                NUMERIC(15, 3) DEFAULT 0.000 NOT NULL,
    depot_id                       BIGINT,
    lot_id                         BIGINT,
    prix_vente                     NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    CONSTRAINT fk_lignes_bon_livraison_client_bon_livraison_client FOREIGN KEY (bon_livraison_client_id) REFERENCES bons_livraison_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_bon_livraison_client_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT,
    CONSTRAINT fk_lignes_bon_livraison_client_depot FOREIGN KEY (depot_id) REFERENCES depots(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_bon_livraison_client_lot FOREIGN KEY (lot_id) REFERENCES lots(id) ON DELETE CASCADE
);

-- Table : ventes (Ventes Caisse & Comptoir)
CREATE TABLE IF NOT EXISTS ventes (
    id                             BIGSERIAL PRIMARY KEY,
    numero_ticket                  VARCHAR(255) NOT NULL UNIQUE,
    date_vente                     TIMESTAMP NOT NULL,
    client_id                      BIGINT,
    vendeur_id                     BIGINT NOT NULL,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    remise_globale                 NUMERIC(15, 2),
    montant_final                  NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    statut                         VARCHAR(50),
    montant_paye                   NUMERIC(15, 2) DEFAULT 0.00,
    montant_restant                NUMERIC(15, 2) DEFAULT 0.00,
    facture_id                     BIGINT,
    notes                          TEXT,
    date_annulation                TIMESTAMP,
    motif_annulation               VARCHAR(255),
    annule_par_user_id             BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_ventes_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ventes_vendeur FOREIGN KEY (vendeur_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ventes_annule_par_user FOREIGN KEY (annule_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ventes_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_vente (Lignes d'Articles Vendus en Caisse)
CREATE TABLE IF NOT EXISTS lignes_vente (
    id                             BIGSERIAL PRIMARY KEY,
    vente_id                       BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    designation                    VARCHAR(255),
    reference                      VARCHAR(255),
    quantite                       NUMERIC(15, 3) DEFAULT 0.000 NOT NULL,
    surface_m2                     NUMERIC(15, 2),
    prix_unitaire_ht               NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    taux_tva                       NUMERIC(5, 2) DEFAULT 0.00,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00,
    remise_pourcentage             NUMERIC(5, 2),
    remise_montant                 NUMERIC(15, 2) DEFAULT 0.00,
    notes                          TEXT,
    CONSTRAINT fk_lignes_vente_vente FOREIGN KEY (vente_id) REFERENCES ventes(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_vente_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : factures (Factures de Vente Client)
CREATE TABLE IF NOT EXISTS factures (
    id                             BIGSERIAL PRIMARY KEY,
    numero_facture                 VARCHAR(255) NOT NULL UNIQUE,
    date_facture                   DATE NOT NULL,
    date_echeance                  DATE,
    client_id                      BIGINT NOT NULL,
    vente_id                       BIGINT,
    emise_par_user_id              BIGINT NOT NULL,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    remise_globale                 NUMERIC(15, 2),
    montant_final                  NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    statut                         VARCHAR(50),
    montant_paye                   NUMERIC(15, 2) DEFAULT 0.00,
    montant_restant                NUMERIC(15, 2) DEFAULT 0.00,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes                          TEXT,
    conditions_paiement            VARCHAR(255),
    annulee                        BOOLEAN DEFAULT FALSE,
    date_annulation                TIMESTAMP,
    motif_annulation               VARCHAR(255),
    annulee_par_user_id            BIGINT,
    chemin_pdf                     VARCHAR(255),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_factures_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_factures_emise_par_user FOREIGN KEY (emise_par_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_factures_annulee_par_user FOREIGN KEY (annulee_par_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_factures_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_facture (Lignes de Factures Client)
CREATE TABLE IF NOT EXISTS lignes_facture (
    id                             BIGSERIAL PRIMARY KEY,
    facture_id                     BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    designation                    VARCHAR(255),
    reference                      VARCHAR(255),
    quantite                       NUMERIC(15, 3) DEFAULT 0.000 NOT NULL,
    surface_m2                     NUMERIC(15, 2),
    prix_unitaire_ht               NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    taux_tva                       NUMERIC(5, 2) DEFAULT 0.00,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00,
    remise_pourcentage             NUMERIC(5, 2),
    remise_montant                 NUMERIC(15, 2) DEFAULT 0.00,
    notes                          TEXT,
    CONSTRAINT fk_lignes_facture_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_facture_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : avoirs (Avoirs & Notes de Credit Client/Fournisseur)
CREATE TABLE IF NOT EXISTS avoirs (
    id                             BIGSERIAL PRIMARY KEY,
    numero_avoir                   VARCHAR(255) NOT NULL UNIQUE,
    type_avoir                     VARCHAR(50) NOT NULL,
    facture_origine_id             BIGINT,
    numero_facture_origine         VARCHAR(255),
    client_id                      BIGINT,
    fournisseur_id                 BIGINT,
    cree_par_user_id               BIGINT,
    date_avoir                     DATE NOT NULL,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    statut                         VARCHAR(50) NOT NULL,
    motif                          TEXT,
    notes                          TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avoirs_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_avoirs_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id) ON DELETE SET NULL,
    CONSTRAINT fk_avoirs_cree_par_user FOREIGN KEY (cree_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_avoirs_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_avoir (Lignes d'Avoirs)
CREATE TABLE IF NOT EXISTS lignes_avoir (
    id                             BIGSERIAL PRIMARY KEY,
    avoir_id                       BIGINT NOT NULL,
    produit_id                     BIGINT NOT NULL,
    quantite                       NUMERIC(12, 3) DEFAULT 0.000 NOT NULL,
    prix_unitaire_ht               NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    taux_tva                       NUMERIC(5, 2) DEFAULT 0.00,
    montant_ht                     NUMERIC(15, 2) DEFAULT 0.00,
    montant_tva                    NUMERIC(15, 2) DEFAULT 0.00,
    montant_ttc                    NUMERIC(15, 2) DEFAULT 0.00,
    remettre_en_stock              BOOLEAN DEFAULT FALSE,
    motif                          TEXT,
    CONSTRAINT fk_lignes_avoir_avoir FOREIGN KEY (avoir_id) REFERENCES avoirs(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_avoir_produit FOREIGN KEY (produit_id) REFERENCES produits(id) ON DELETE RESTRICT
);

-- Table : paiements (Encaissements & Paiements Clients)
CREATE TABLE IF NOT EXISTS paiements (
    id                             BIGSERIAL PRIMARY KEY,
    numero_paiement                VARCHAR(255) NOT NULL UNIQUE,
    date_paiement                  TIMESTAMP NOT NULL,
    vente_id                       BIGINT,
    facture_id                     BIGINT,
    client_id                      BIGINT,
    montant                        NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    mode_paiement                  VARCHAR(50) NOT NULL,
    reference_paiement             VARCHAR(255),
    nom_banque                     VARCHAR(255),
    numero_cheque                  VARCHAR(255),
    date_echeance                  TIMESTAMP,
    encaisse_par_user_id           BIGINT NOT NULL,
    notes                          TEXT,
    annule                         BOOLEAN DEFAULT FALSE,
    date_annulation                TIMESTAMP,
    motif_annulation               VARCHAR(255),
    annule_par_user_id             BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_paiements_vente FOREIGN KEY (vente_id) REFERENCES ventes(id) ON DELETE SET NULL,
    CONSTRAINT fk_paiements_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE SET NULL,
    CONSTRAINT fk_paiements_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_paiements_encaisse_par_user FOREIGN KEY (encaisse_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_paiements_annule_par_user FOREIGN KEY (annule_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_paiements_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : paiement_affectations (Affectations Multi-Factures des Paiements)
CREATE TABLE IF NOT EXISTS paiement_affectations (
    id                             BIGSERIAL PRIMARY KEY,
    paiement_id                    BIGINT NOT NULL,
    facture_id                     BIGINT NOT NULL,
    montant_affecte                NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    date_affectation               TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_paiement_affectations_paiement FOREIGN KEY (paiement_id) REFERENCES paiements(id) ON DELETE CASCADE,
    CONSTRAINT fk_paiement_affectations_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE CASCADE
);

-- Table : relances_clients (Historique des Relances d'Impayes Client)
CREATE TABLE IF NOT EXISTS relances_clients (
    id                             BIGSERIAL PRIMARY KEY,
    client_id                      BIGINT NOT NULL,
    facture_id                     BIGINT,
    date_relance                   TIMESTAMP NOT NULL,
    canal                          VARCHAR(50) NOT NULL,
    interlocuteur                  VARCHAR(255),
    commentaire                    TEXT NOT NULL,
    effectue_par_user_id           BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_relances_clients_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_relances_clients_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE SET NULL,
    CONSTRAINT fk_relances_clients_effectue_par_user FOREIGN KEY (effectue_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_relances_clients_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : promesses_paiement (Engagements & Promesses de Paiement Client)
CREATE TABLE IF NOT EXISTS promesses_paiement (
    id                             BIGSERIAL PRIMARY KEY,
    client_id                      BIGINT NOT NULL,
    facture_id                     BIGINT,
    date_promesse                  TIMESTAMP NOT NULL,
    date_echeance_promise          DATE NOT NULL,
    montant_promis                 NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    statut                         VARCHAR(50) NOT NULL,
    notes                          TEXT,
    enregistre_par_user_id         BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_promesses_paiement_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_promesses_paiement_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE SET NULL,
    CONSTRAINT fk_promesses_paiement_enregistre_par_user FOREIGN KEY (enregistre_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_promesses_paiement_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- =============================================================================
-- 7. TRESORERIE, BANQUE & CAISSE
-- =============================================================================

-- Table : banques (Établissements bancaires de référence)
CREATE TABLE IF NOT EXISTS banques (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(50) NOT NULL UNIQUE,
    nom                            VARCHAR(150) NOT NULL,
    nom_court                      VARCHAR(50),
    code_swift                     VARCHAR(20),
    logo                           VARCHAR(255),
    couleur                        VARCHAR(20),
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_banques_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : comptes_financiers (Comptes Bancaires, Caisses & Coffres)
CREATE TABLE IF NOT EXISTS comptes_financiers (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(255) NOT NULL UNIQUE,
    nom                            VARCHAR(255) NOT NULL,
    type                           VARCHAR(50) NOT NULL,
    solde_actuel                   NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    devise                         VARCHAR(10) NOT NULL,
    numero_compte_rib              VARCHAR(255),
    nom_banque                     VARCHAR(255),
    banque_id                      BIGINT,
    agence                         VARCHAR(150),
    code_agence                    VARCHAR(50),
    titulaire                      VARCHAR(150),
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comptes_financiers_banque FOREIGN KEY (banque_id) REFERENCES banques(id) ON DELETE RESTRICT,
    CONSTRAINT fk_comptes_financiers_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : mouvements_tresorerie (Mouvements & Virements de Tresorerie)
CREATE TABLE IF NOT EXISTS mouvements_tresorerie (
    id                             BIGSERIAL PRIMARY KEY,
    reference                      VARCHAR(255) NOT NULL UNIQUE,
    type_mouvement                 VARCHAR(50) NOT NULL,
    compte_source_id               BIGINT NOT NULL,
    compte_destination_id          BIGINT,
    montant                        NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    date_mouvement                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    motif                          TEXT NOT NULL,
    justificatif_reference         VARCHAR(255),
    effectue_par_user_id           BIGINT,
    solde_apres_source             NUMERIC(15, 2) DEFAULT 0.00,
    solde_apres_destination        NUMERIC(15, 2) DEFAULT 0.00,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_mouvements_tresorerie_compte_source FOREIGN KEY (compte_source_id) REFERENCES comptes_financiers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mouvements_tresorerie_compte_destination FOREIGN KEY (compte_destination_id) REFERENCES comptes_financiers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mouvements_tresorerie_effectue_par_user FOREIGN KEY (effectue_par_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_mouvements_tresorerie_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : releves_bancaires (Releves Bancaires & Rapprochements)
CREATE TABLE IF NOT EXISTS releves_bancaires (
    id                             BIGSERIAL PRIMARY KEY,
    reference_releve               VARCHAR(255) NOT NULL,
    compte_financier_id            BIGINT NOT NULL,
    date_debut                     DATE NOT NULL,
    date_fin                       DATE NOT NULL,
    solde_initial                  NUMERIC(15, 2) DEFAULT 0.00,
    solde_final                    NUMERIC(15, 2) DEFAULT 0.00,
    date_import                    TIMESTAMP,
    statut                         VARCHAR(30),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_releves_bancaires_compte_financier FOREIGN KEY (compte_financier_id) REFERENCES comptes_financiers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_releves_bancaires_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_releves_bancaires (Lignes de Releves Bancaires Rapprochees)
CREATE TABLE IF NOT EXISTS lignes_releves_bancaires (
    id                             BIGSERIAL PRIMARY KEY,
    releve_bancaire_id             BIGINT NOT NULL,
    date_operation                 DATE NOT NULL,
    date_valeur                    DATE,
    libelle                        VARCHAR(500) NOT NULL,
    debit                          NUMERIC(15, 2) DEFAULT 0.00,
    credit                         NUMERIC(15, 2) DEFAULT 0.00,
    reference                      VARCHAR(100),
    statut_rapprochement           VARCHAR(50) NOT NULL,
    mouvement_tresorerie_id        BIGINT,
    ligne_ecriture_id              BIGINT,
    date_rapprochement             TIMESTAMP,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_lignes_releves_bancaires_releve_bancaire FOREIGN KEY (releve_bancaire_id) REFERENCES releves_bancaires(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_releves_bancaires_mouvement_tresorerie FOREIGN KEY (mouvement_tresorerie_id) REFERENCES mouvements_tresorerie(id) ON DELETE SET NULL,
    CONSTRAINT fk_lignes_releves_bancaires_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : bordereaux_remise (Bordereaux de Remise de Cheques/Effets)
CREATE TABLE IF NOT EXISTS bordereaux_remise (
    id                             BIGSERIAL PRIMARY KEY,
    numero_bordereau               VARCHAR(255) NOT NULL UNIQUE,
    date_remise                    DATE NOT NULL,
    nom_banque                     VARCHAR(255) NOT NULL,
    compte_bancaire                VARCHAR(255),
    type_valeur                    VARCHAR(255) NOT NULL,
    montant_total                  NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    nombre_valeurs                 INTEGER,
    statut                         VARCHAR(50) NOT NULL,
    notes                          TEXT,
    point_de_vente_id              BIGINT,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bordereaux_remise_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : cheques_effets (Gestion du Portefeuille Cheques & Effets (LCN))
CREATE TABLE IF NOT EXISTS cheques_effets (
    id                             BIGSERIAL PRIMARY KEY,
    numero_piece                   VARCHAR(50) NOT NULL,
    type_effet                     VARCHAR(50) NOT NULL,
    sens                           VARCHAR(50) NOT NULL,
    statut                         VARCHAR(50) NOT NULL,
    montant                        NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    date_emission                  DATE,
    date_echeance                  DATE,
    date_remise                    DATE,
    date_encaissement              DATE,
    banque_emettrice               VARCHAR(255),
    tireur                         VARCHAR(255),
    beneficiaire                   VARCHAR(255),
    compte_bancaire_depot          VARCHAR(255),
    reference_paiement             VARCHAR(255),
    client_id                      BIGINT,
    fournisseur_id                 BIGINT,
    bordereau_remise_id            BIGINT,
    motif_rejet                    VARCHAR(255),
    notes                          TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cheques_effets_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cheques_effets_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cheques_effets_bordereau_remise FOREIGN KEY (bordereau_remise_id) REFERENCES bordereaux_remise(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cheques_effets_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : depenses (Depenses Directes & Menues Frais de Caisse)
CREATE TABLE IF NOT EXISTS depenses (
    id                             BIGSERIAL PRIMARY KEY,
    reference                      VARCHAR(255) NOT NULL UNIQUE,
    designation                    VARCHAR(255) NOT NULL,
    montant                        NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    date_depense                   DATE NOT NULL,
    categorie                      VARCHAR(50) NOT NULL,
    mode_paiement                  VARCHAR(50) NOT NULL,
    beneficiaire                   VARCHAR(255),
    numero_facture_justificatif    VARCHAR(255),
    notes                          TEXT,
    cree_par_user_id               BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_depenses_cree_par_user FOREIGN KEY (cree_par_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_depenses_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- =============================================================================
-- 8. COMPTABILITE GENERALE & ANALYTIQUE, FISCALITE
-- =============================================================================

-- Table : exercices_comptables (Exercices Comptables Annuels)
CREATE TABLE IF NOT EXISTS exercices_comptables (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(50) NOT NULL,
    libelle                        VARCHAR(150) NOT NULL,
    date_debut                     DATE NOT NULL,
    date_fin                       DATE NOT NULL,
    statut                         VARCHAR(20) NOT NULL,
    resultat_net                   NUMERIC(15, 2),
    date_cloture                   TIMESTAMP,
    cloture_par                    VARCHAR(100),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_exercices_comptables_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : journaux_comptables (Journaux Comptables (Ventes, Achats, Banque, Caisse, OD))
CREATE TABLE IF NOT EXISTS journaux_comptables (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(10) NOT NULL,
    libelle                        VARCHAR(255) NOT NULL,
    type_journal                   VARCHAR(50) NOT NULL,
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_journaux_comptables_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : comptes_comptables (Plan Comptable General (PCGM))
CREATE TABLE IF NOT EXISTS comptes_comptables (
    id                             BIGSERIAL PRIMARY KEY,
    numero_compte                  VARCHAR(20) NOT NULL,
    libelle                        VARCHAR(255) NOT NULL,
    classe                         INTEGER NOT NULL,
    sens_par_defaut                VARCHAR(50),
    actif                          BOOLEAN DEFAULT TRUE NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comptes_comptables_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : ecritures_comptables (Pieces & Ecritures Comptables)
CREATE TABLE IF NOT EXISTS ecritures_comptables (
    id                             BIGSERIAL PRIMARY KEY,
    numero_piece                   VARCHAR(50) NOT NULL,
    date_ecriture                  DATE NOT NULL,
    libelle                        VARCHAR(255) NOT NULL,
    reference_piece                VARCHAR(255),
    validee                        BOOLEAN DEFAULT FALSE NOT NULL,
    journal_id                     BIGINT NOT NULL,
    total_debit                    NUMERIC(15, 2) DEFAULT 0.00,
    total_credit                   NUMERIC(15, 2) DEFAULT 0.00,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ecritures_comptables_journal FOREIGN KEY (journal_id) REFERENCES journaux_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ecritures_comptables_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_ecriture (Lignes d'Ecritures Comptables (Debit / Credit))
CREATE TABLE IF NOT EXISTS lignes_ecriture (
    id                             BIGSERIAL PRIMARY KEY,
    ecriture_id                    BIGINT NOT NULL,
    compte_id                      BIGINT NOT NULL,
    debit                          NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    credit                         NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    libelle_ligne                  VARCHAR(255),
    reference_ligne                VARCHAR(255),
    lettrage                       VARCHAR(10),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_lignes_ecriture_ecriture FOREIGN KEY (ecriture_id) REFERENCES ecritures_comptables(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_ecriture_compte FOREIGN KEY (compte_id) REFERENCES comptes_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_lignes_ecriture_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : axes_analytiques (Axes Analytiques (Projet, Departement, Activite))
CREATE TABLE IF NOT EXISTS axes_analytiques (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(50) NOT NULL,
    libelle                        VARCHAR(150) NOT NULL,
    description                    TEXT,
    actif                          BOOLEAN DEFAULT TRUE,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_axes_analytiques_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : centres_analytiques (Centres de Couts / Sections Analytiques)
CREATE TABLE IF NOT EXISTS centres_analytiques (
    id                             BIGSERIAL PRIMARY KEY,
    axe_id                         BIGINT NOT NULL,
    code                           VARCHAR(50) NOT NULL,
    libelle                        VARCHAR(150) NOT NULL,
    type                           VARCHAR(50),
    responsable                    VARCHAR(150),
    actif                          BOOLEAN DEFAULT TRUE,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_centres_analytiques_axe FOREIGN KEY (axe_id) REFERENCES axes_analytiques(id) ON DELETE RESTRICT,
    CONSTRAINT fk_centres_analytiques_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : ventilations_analytiques (Ventilations Analytiques des Charges & Produits)
CREATE TABLE IF NOT EXISTS ventilations_analytiques (
    id                             BIGSERIAL PRIMARY KEY,
    ligne_ecriture_id              BIGINT NOT NULL,
    centre_analytique_id           BIGINT NOT NULL,
    pourcentage                    NUMERIC(6, 2) NOT NULL,
    montant                        NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ventilations_analytiques_ligne_ecriture FOREIGN KEY (ligne_ecriture_id) REFERENCES lignes_ecriture(id) ON DELETE CASCADE,
    CONSTRAINT fk_ventilations_analytiques_centre_analytique FOREIGN KEY (centre_analytique_id) REFERENCES centres_analytiques(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ventilations_analytiques_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : immobilisations (Registre des Immobilisations & Actifs)
CREATE TABLE IF NOT EXISTS immobilisations (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(50) NOT NULL,
    designation                    VARCHAR(255) NOT NULL,
    compte_immobilisation_id       BIGINT NOT NULL,
    compte_amortissement_id        BIGINT NOT NULL,
    compte_dotation_id             BIGINT NOT NULL,
    compte_produit_cession_id      BIGINT,
    compte_vna_id                  BIGINT,
    numero_facture                 VARCHAR(100),
    fournisseur_nom                VARCHAR(150),
    date_acquisition               DATE NOT NULL,
    date_mise_en_service           DATE NOT NULL,
    valeur_acquisition             NUMERIC(15, 2) NOT NULL,
    tva_deductible                 NUMERIC(15, 2),
    valeur_residuelle              NUMERIC(15, 2),
    duree_annees                   INTEGER NOT NULL,
    type_amortissement             VARCHAR(30) NOT NULL,
    taux_amortissement             NUMERIC(8, 4) DEFAULT 0.000,
    coefficient_degressif          NUMERIC(4, 2),
    statut                         VARCHAR(30) NOT NULL,
    date_cession                   DATE,
    prix_cession                   NUMERIC(15, 2) DEFAULT 0.00,
    cumul_amortissements           NUMERIC(15, 2),
    valeur_nette_comptable         NUMERIC(15, 2),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_immobilisations_compte_immobilisation FOREIGN KEY (compte_immobilisation_id) REFERENCES comptes_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_immobilisations_compte_amortissement FOREIGN KEY (compte_amortissement_id) REFERENCES comptes_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_immobilisations_compte_dotation FOREIGN KEY (compte_dotation_id) REFERENCES comptes_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_immobilisations_compte_produit_cession FOREIGN KEY (compte_produit_cession_id) REFERENCES comptes_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_immobilisations_compte_vna FOREIGN KEY (compte_vna_id) REFERENCES comptes_comptables(id) ON DELETE RESTRICT,
    CONSTRAINT fk_immobilisations_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : lignes_plan_amortissement (Tableau d'Amortissement des Immobilisations)
CREATE TABLE IF NOT EXISTS lignes_plan_amortissement (
    id                             BIGSERIAL PRIMARY KEY,
    immobilisation_id              BIGINT NOT NULL,
    annee                          INTEGER NOT NULL,
    mois_amortis                   INTEGER NOT NULL,
    base_calcul                    NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    taux_applique                  NUMERIC(8, 4) DEFAULT 0.000 NOT NULL,
    dotation                       NUMERIC(15, 2) NOT NULL,
    cumul_amortissement            NUMERIC(15, 2) NOT NULL,
    valeur_nette_fin               NUMERIC(15, 2) NOT NULL,
    mode_lineaire_bascule          BOOLEAN DEFAULT FALSE NOT NULL,
    comptabilisee                  BOOLEAN DEFAULT FALSE NOT NULL,
    date_comptabilisation          DATE,
    ecriture_id                    BIGINT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_lignes_plan_amortissement_immobilisation FOREIGN KEY (immobilisation_id) REFERENCES immobilisations(id) ON DELETE CASCADE,
    CONSTRAINT fk_lignes_plan_amortissement_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : regles_fiscales_is (Barem & Regles Fiscales IS)
CREATE TABLE IF NOT EXISTS regles_fiscales_is (
    id                             BIGSERIAL PRIMARY KEY,
    annee_fiscale                  INTEGER NOT NULL,
    seuil_tranche1                 NUMERIC(15, 2) NOT NULL,
    taux_tranche1                  NUMERIC(6, 4) DEFAULT 0.000 NOT NULL,
    seuil_tranche2                 NUMERIC(15, 2) NOT NULL,
    taux_tranche2                  NUMERIC(6, 4) DEFAULT 0.000 NOT NULL,
    taux_tranche3                  NUMERIC(6, 4) DEFAULT 0.000 NOT NULL,
    taux_cotisation_minimale       NUMERIC(6, 4) DEFAULT 0.000 NOT NULL,
    plancher_cotisation_minimale   NUMERIC(15, 2) NOT NULL,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_mise_a_jour               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_regles_fiscales_is_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : declarations_tva (Declarations Periodiques de TVA)
CREATE TABLE IF NOT EXISTS declarations_tva (
    id                             BIGSERIAL PRIMARY KEY,
    periode                        VARCHAR(20) NOT NULL,
    regime                         VARCHAR(50) NOT NULL,
    tva_collectee                  NUMERIC(15, 2),
    tva_deductible_charges         NUMERIC(15, 2),
    tva_deductible_immo            NUMERIC(15, 2),
    tva_deductible_total           NUMERIC(15, 2) DEFAULT 0.00,
    credit_tva_anterieur           NUMERIC(15, 2) DEFAULT 0.00,
    tva_a_payer                    NUMERIC(15, 2),
    credit_tva_reportable          NUMERIC(15, 2) DEFAULT 0.00,
    prorata                        NUMERIC(5, 2),
    total_ventes_ht                NUMERIC(15, 2) DEFAULT 0.00,
    total_achats_ht                NUMERIC(15, 2) DEFAULT 0.00,
    statut                         VARCHAR(50) NOT NULL,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_validation                TIMESTAMP,
    validee_par                    VARCHAR(255),
    notes                          TEXT,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_declarations_tva_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : echeances_fiscales (Calendrier des Echeances Fiscales)
CREATE TABLE IF NOT EXISTS echeances_fiscales (
    id                             BIGSERIAL PRIMARY KEY,
    societe_id                     BIGINT NOT NULL,
    tenant_id                      BIGINT DEFAULT 1 NOT NULL,
    type_echeance                  VARCHAR(50) NOT NULL,
    libelle                        VARCHAR(200) NOT NULL,
    date_echeance                  DATE NOT NULL,
    statut                         VARCHAR(30) NOT NULL,
    montant_estime                 NUMERIC(15, 2) DEFAULT 0.00,
    montant_paye                   NUMERIC(15, 2) DEFAULT 0.00,
    date_paiement                  DATE,
    reference_paiement             VARCHAR(100),
    commentaire                    TEXT,
    exercice                       INTEGER DEFAULT 2026,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_echeances_fiscales_societe FOREIGN KEY (societe_id) REFERENCES societes(id) ON DELETE RESTRICT
);

-- Table : documents_comptables (Pieces Justificatives & Documents Numerises)
CREATE TABLE IF NOT EXISTS documents_comptables (
    id                             BIGSERIAL PRIMARY KEY,
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    nom_original                   VARCHAR(255) NOT NULL,
    nom_stocke                     VARCHAR(255) NOT NULL UNIQUE,
    content_type                   VARCHAR(100),
    taille_octets                  BIGINT,
    chemin_stockage                VARCHAR(500),
    sha256_hash                    VARCHAR(64),
    type_piece                     VARCHAR(50),
    description                    TEXT,
    ecriture_id                    BIGINT,
    facture_achat_id               BIGINT,
    facture_vente_id               BIGINT,
    paiement_id                    BIGINT,
    uploaded_by                    VARCHAR(150),
    date_upload                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_comptables_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT,
    CONSTRAINT fk_documents_comptables_ecriture FOREIGN KEY (ecriture_id) REFERENCES ecritures_comptables(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_comptables_facture_achat FOREIGN KEY (facture_achat_id) REFERENCES factures_achat(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_comptables_facture_vente FOREIGN KEY (facture_vente_id) REFERENCES factures(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_comptables_paiement FOREIGN KEY (paiement_id) REFERENCES paiements(id) ON DELETE SET NULL
);

-- =============================================================================
-- 9. AUDIT & SYSTEME
-- =============================================================================

-- Table : notifications (Notifications Internes & Alertes Systeme)
CREATE TABLE IF NOT EXISTS notifications (
    id                             BIGSERIAL PRIMARY KEY,
    titre                          VARCHAR(255) NOT NULL,
    message                        TEXT NOT NULL,
    type                           VARCHAR(50) NOT NULL,
    severite                       VARCHAR(50) NOT NULL,
    lu                             BOOLEAN DEFAULT FALSE NOT NULL,
    lien_action                    VARCHAR(255),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    date_lecture                   TIMESTAMP,
    date_creation                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- Table : audit_log (Journal d'Audit & Tracabilite des Modifications)
CREATE TABLE IF NOT EXISTS audit_log (
    id                             BIGSERIAL PRIMARY KEY,
    entite                         VARCHAR(100) NOT NULL,
    entite_id                      BIGINT NOT NULL,
    action                         VARCHAR(50) NOT NULL,
    champ_modifie                  VARCHAR(100),
    ancienne_valeur                TEXT,
    nouvelle_valeur                TEXT,
    description                    TEXT,
    utilisateur                    VARCHAR(150) NOT NULL,
    date_action                    TIMESTAMP NOT NULL,
    adresse_ip                     VARCHAR(50),
    point_de_vente_id              BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_audit_log_point_de_vente FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT
);

-- =============================================================================
-- 10. CONTRAINTES DE CLES ETRANGERES CIRCULAIRES & CROISEES
-- =============================================================================
ALTER TABLE produits ADD CONSTRAINT fk_produits_image FOREIGN KEY (image_id) REFERENCES produit_images(id) ON DELETE SET NULL;
ALTER TABLE ventes ADD CONSTRAINT fk_ventes_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE SET NULL;
ALTER TABLE factures ADD CONSTRAINT fk_factures_vente FOREIGN KEY (vente_id) REFERENCES ventes(id) ON DELETE SET NULL;
ALTER TABLE devis ADD CONSTRAINT fk_devis_commande_generee FOREIGN KEY (commande_generee_id) REFERENCES commandes_client(id) ON DELETE SET NULL;
ALTER TABLE devis ADD CONSTRAINT fk_devis_facture_generee FOREIGN KEY (facture_generee_id) REFERENCES factures(id) ON DELETE SET NULL;
ALTER TABLE avoirs ADD CONSTRAINT fk_avoirs_facture_origine FOREIGN KEY (facture_origine_id) REFERENCES factures(id) ON DELETE SET NULL;
ALTER TABLE bons_livraison_client ADD CONSTRAINT fk_blc_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE SET NULL;
ALTER TABLE lignes_plan_amortissement ADD CONSTRAINT fk_lpa_ecriture FOREIGN KEY (ecriture_id) REFERENCES ecritures_comptables(id) ON DELETE SET NULL;
ALTER TABLE lignes_releves_bancaires ADD CONSTRAINT fk_lrb_ligne_ecriture FOREIGN KEY (ligne_ecriture_id) REFERENCES lignes_ecriture(id) ON DELETE SET NULL;

-- =============================================================================
-- 11. INDEX DE PERFORMANCE (MULTI-TENANCY, RECHERCHE & DATES)
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_users_pdv ON users(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_entreprise_profiles_pdv ON entreprise_profiles(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_categories_pdv ON categories(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_banques_pdv ON banques(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_produits_pdv ON produits(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_depots_pdv ON depots(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_pdv ON mouvements_stock(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_transferts_stock_pdv ON transferts_stock(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_transfert_stock_pdv ON lignes_transfert_stock(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_inventaires_pdv ON inventaires(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_fournisseurs_pdv ON fournisseurs(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_commandes_pdv ON commandes(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_livraisons_pdv ON livraisons(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_factures_achat_pdv ON factures_achat(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_reglements_fournisseur_pdv ON reglements_fournisseur(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_clients_pdv ON clients(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_objectifs_commerciaux_pdv ON objectifs_commerciaux(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_sessions_caisse_pdv ON sessions_caisse(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_devis_pdv ON devis(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_commandes_client_pdv ON commandes_client(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_bons_preparation_pdv ON bons_preparation(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_bons_livraison_client_pdv ON bons_livraison_client(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_ventes_pdv ON ventes(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_factures_pdv ON factures(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_avoirs_pdv ON avoirs(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_paiements_pdv ON paiements(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_relances_clients_pdv ON relances_clients(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_promesses_paiement_pdv ON promesses_paiement(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_comptes_financiers_pdv ON comptes_financiers(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_mouvements_tresorerie_pdv ON mouvements_tresorerie(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_releves_bancaires_pdv ON releves_bancaires(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_releves_bancaires_pdv ON lignes_releves_bancaires(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_bordereaux_remise_pdv ON bordereaux_remise(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_cheques_effets_pdv ON cheques_effets(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_depenses_pdv ON depenses(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_exercices_comptables_pdv ON exercices_comptables(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_journaux_comptables_pdv ON journaux_comptables(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_comptes_comptables_pdv ON comptes_comptables(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_ecritures_comptables_pdv ON ecritures_comptables(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_ecriture_pdv ON lignes_ecriture(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_axes_analytiques_pdv ON axes_analytiques(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_centres_analytiques_pdv ON centres_analytiques(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_ventilations_analytiques_pdv ON ventilations_analytiques(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_immobilisations_pdv ON immobilisations(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_plan_amortissement_pdv ON lignes_plan_amortissement(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_regles_fiscales_is_pdv ON regles_fiscales_is(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_declarations_tva_pdv ON declarations_tva(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_documents_comptables_pdv ON documents_comptables(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_notifications_pdv ON notifications(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_pdv ON audit_log(point_de_vente_id);

-- Index metier sur les cles de recherche et d'audit
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_produits_ref ON produits(reference);
CREATE INDEX IF NOT EXISTS idx_produits_code_barre ON produits(code_barre);
CREATE INDEX IF NOT EXISTS idx_clients_nom ON clients(nom);
CREATE INDEX IF NOT EXISTS idx_clients_telephone ON clients(telephone);
CREATE INDEX IF NOT EXISTS idx_fournisseurs_raison_social ON fournisseurs(raison_social);
CREATE INDEX IF NOT EXISTS idx_fournisseurs_telephone ON fournisseurs(telephone);
CREATE INDEX IF NOT EXISTS idx_ventes_date ON ventes(date_vente);
CREATE INDEX IF NOT EXISTS idx_ventes_ticket ON ventes(numero_ticket);
CREATE INDEX IF NOT EXISTS idx_factures_date ON factures(date_facture);
CREATE INDEX IF NOT EXISTS idx_factures_num ON factures(numero_facture);
CREATE INDEX IF NOT EXISTS idx_factures_statut ON factures(statut);
CREATE INDEX IF NOT EXISTS idx_factures_achat_date ON factures_achat(date_facture);
CREATE INDEX IF NOT EXISTS idx_factures_achat_num ON factures_achat(numero_facture);
CREATE INDEX IF NOT EXISTS idx_commandes_num ON commandes(numero_commande);
CREATE INDEX IF NOT EXISTS idx_commandes_client_num ON commandes_client(numero_commande);
CREATE INDEX IF NOT EXISTS idx_devis_num ON devis(numero_devis);
CREATE INDEX IF NOT EXISTS idx_livraisons_num ON livraisons(numero_livraison);
CREATE INDEX IF NOT EXISTS idx_blc_num ON bons_livraison_client(numero_bl);
CREATE INDEX IF NOT EXISTS idx_avoirs_num ON avoirs(numero_avoir);
CREATE INDEX IF NOT EXISTS idx_paiements_date ON paiements(date_paiement);
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_date ON mouvements_stock(date_mouvement);
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_prod ON mouvements_stock(produit_id);
CREATE INDEX IF NOT EXISTS idx_stocks_prod ON stocks(produit_id);
CREATE INDEX IF NOT EXISTS idx_ecritures_date ON ecritures_comptables(date_ecriture);
CREATE INDEX IF NOT EXISTS idx_ecritures_piece ON ecritures_comptables(numero_piece);
CREATE INDEX IF NOT EXISTS idx_lignes_ecriture_compte ON lignes_ecriture(compte_id);
CREATE INDEX IF NOT EXISTS idx_mouvements_treso_date ON mouvements_tresorerie(date_mouvement);
CREATE INDEX IF NOT EXISTS idx_cheques_effets_statut ON cheques_effets(statut);
CREATE INDEX IF NOT EXISTS idx_cheques_effets_echeance ON cheques_effets(date_echeance);
CREATE INDEX IF NOT EXISTS idx_audit_log_date ON audit_log(date_action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entite ON audit_log(entite, entite_id);

-- =============================================================================
-- 12. DONNEES INITIALES ESSENTIELLES (SEED DATA)
-- =============================================================================

-- 12.1 Societe Mere Principale & Plateforme Globale
INSERT INTO meres (id, nom, forme_juridique, actif, date_creation)
VALUES 
(0, 'Plateforme Globale (SuperAdmin)', 'SYSTEM', TRUE, CURRENT_TIMESTAMP),
(1, 'Holding Principale', 'SARL', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 12.2 Point de Vente par Defaut (Siege)
INSERT INTO point_de_vente (id, mere_id, tenant_id, nom_point_de_vente, nom, actif, module_commercial_actif, module_comptabilite_actif, module_fiscalite_actif, date_creation)
VALUES (1, 1, 1, 'Siège Principal', 'Siège Principal', TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 12.3 Roles Utilisateur
INSERT INTO roles (id, nom) VALUES
(1, 'ROLE_SUPERADMIN'),
(2, 'ROLE_ADMIN'),
(3, 'ROLE_POINT_DE_VENTE_MANAGER'),
(4, 'ROLE_CAISSIER'),
(5, 'ROLE_VENDEUR'),
(6, 'ROLE_MAGASINIER'),
(7, 'ROLE_GESTIONNAIRE'),
(8, 'ROLE_RESPONSABLE_COMMERCIAL'),
(9, 'ROLE_COMMERCIAL'),
(10, 'ROLE_COMPTABLE')
ON CONFLICT (id) DO NOTHING;

-- 12.4 Habilitations Metier
INSERT INTO habilitations (id, nom) VALUES
(1, 'PRODUIT_READ'),
(2, 'PRODUIT_CREATE'),
(3, 'PRODUIT_UPDATE'),
(4, 'PRODUIT_DELETE'),
(5, 'STOCK_READ'),
(6, 'STOCK_CREATE'),
(7, 'STOCK_TRANSFERT'),
(8, 'VENTE_READ'),
(9, 'VENTE_CREATE'),
(10, 'VENTE_DELETE'),
(11, 'COMMANDE_READ'),
(12, 'COMMANDE_CREATE'),
(13, 'COMMANDE_VALIDATE'),
(14, 'FACTURE_READ'),
(15, 'FACTURE_CREATE'),
(16, 'FACTURE_VALIDER'),
(17, 'FACTURE_ANNULER'),
(18, 'CLIENT_READ'),
(19, 'CLIENT_CREATE'),
(20, 'CLIENT_UPDATE'),
(21, 'CLIENT_DELETE'),
(22, 'FOURNISSEUR_READ'),
(23, 'FOURNISSEUR_CREATE'),
(24, 'FOURNISSEUR_UPDATE'),
(25, 'PAIEMENT_READ'),
(26, 'PAIEMENT_CREATE'),
(27, 'COMPTA_READ'),
(28, 'COMPTA_ECRITURE'),
(29, 'COMPTA_CLOTURE'),
(30, 'TRESORERIE_READ'),
(31, 'TRESORERIE_MOUVEMENT'),
(32, 'RAPPORT_READ'),
(33, 'RAPPORT_EXPORT'),
(34, 'USER_READ'),
(35, 'USER_CREATE'),
(36, 'USER_UPDATE'),
(37, 'USER_DELETE'),
(38, 'ADMIN_ENTREPRISE'),
(39, 'ADMIN_ROLES'),
(40, 'ADMIN_HABILITATIONS')
ON CONFLICT (id) DO NOTHING;

-- 12.5 Assignation des Habilitations au ROLE_ADMIN (Role ID 2)
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(2, 1),
(2, 2),
(2, 3),
(2, 4),
(2, 5),
(2, 6),
(2, 7),
(2, 8),
(2, 9),
(2, 10),
(2, 11),
(2, 12),
(2, 13),
(2, 14),
(2, 15),
(2, 16),
(2, 17),
(2, 18),
(2, 19),
(2, 20),
(2, 21),
(2, 22),
(2, 23),
(2, 24),
(2, 25),
(2, 26),
(2, 27),
(2, 28),
(2, 29),
(2, 30),
(2, 31),
(2, 32),
(2, 33),
(2, 34),
(2, 35),
(2, 36),
(2, 37),
(2, 38),
(2, 39),
(2, 40)
ON CONFLICT DO NOTHING;

-- 12.6 Compte Super Administrateur
-- Mot de passe par defaut: SuperAdmin@2026! (Bcrypt hash: $2a$10$UKxUQl0p59cpN0fLj7c0f.U5OMgv.wfqrqkvskSXoPaxoJ2HEMeFy)
INSERT INTO users (id, email, username, password, role_id, nom_complet, telephone, mere_id, tenant_id, point_de_vente_id, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES 
(1, 'superadmin@erp.com', 'superadmin', '$2a$10$UKxUQl0p59cpN0fLj7c0f.U5OMgv.wfqrqkvskSXoPaxoJ2HEMeFy', 1, 'Super Administrateur Plateforme', '+212 00 00 00 00', 0, 0, NULL, TRUE, TRUE, TRUE, TRUE),
(2, 'admin@atlasceram.ma', 'admin_atlas', '$2a$10$UKxUQl0p59cpN0fLj7c0f.U5OMgv.wfqrqkvskSXoPaxoJ2HEMeFy', 2, 'Adil Directoire - Atlas Ceram', '+212 522 88 99 00', 1, 1, 1, TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

-- 12.7 Banques marocaines de référence
INSERT INTO banques (id, code, nom, nom_court, couleur, actif, point_de_vente_id) VALUES
(1, 'AWB', 'Attijariwafa Bank', 'Attijariwafa', '#D97706', TRUE, 1),
(2, 'BCP', 'Banque Centrale Populaire', 'Chaabi', '#EA580C', TRUE, 1),
(3, 'BOA', 'Bank of Africa', 'BOA', '#2563EB', TRUE, 1),
(4, 'CIH', 'CIH Bank', 'CIH', '#0D9488', TRUE, 1),
(5, 'SGMB', 'Société Générale Maroc', 'SGMB', '#DC2626', TRUE, 1),
(6, 'BMCI', 'Banque Marocaine pour le Commerce et l''Industrie', 'BMCI', '#16A34A', TRUE, 1),
(7, 'CDM', 'Crédit du Maroc', 'CDM', '#0284C7', TRUE, 1),
(8, 'ALBARID', 'Al Barid Bank', 'Barid Bank', '#CA8A04', TRUE, 1)
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom, couleur = EXCLUDED.couleur;

-- 12.8 Synchronisation des sequences PostgreSQL
SELECT setval('meres_id_seq', (SELECT COALESCE(MAX(id), 1) FROM meres));
SELECT setval('point_de_vente_id_seq', (SELECT COALESCE(MAX(id), 1) FROM point_de_vente));
SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM roles));
SELECT setval('habilitations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM habilitations));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('banques_id_seq', (SELECT COALESCE(MAX(id), 1) FROM banques));

-- 12.9 Barème officiel IS et Cotisation Minimale (Loi de Finances Marocaine)
INSERT INTO regles_fiscales_is (annee_fiscale, point_de_vente_id, seuil_tranche1, seuil_tranche2, taux_tranche1, taux_tranche2, taux_tranche3, taux_cotisation_minimale, plancher_cotisation_minimale, date_mise_a_jour)
VALUES 
  (2023, 1, 300000.00, 1000000.00, 0.1000, 0.2000, 0.3100, 0.0050, 3000.00, NOW()),
  (2024, 1, 300000.00, 1000000.00, 0.1000, 0.2000, 0.3100, 0.0050, 3000.00, NOW()),
  (2025, 1, 300000.00, 1000000.00, 0.1000, 0.2000, 0.3100, 0.0050, 3000.00, NOW()),
  (2026, 1, 300000.00, 1000000.00, 0.1000, 0.2000, 0.3100, 0.0050, 3000.00, NOW()),
  (2027, 1, 300000.00, 1000000.00, 0.1000, 0.2000, 0.3100, 0.0050, 3000.00, NOW())
ON CONFLICT DO NOTHING;
SELECT setval('regles_fiscales_is_id_seq', (SELECT COALESCE(MAX(id), 1) FROM regles_fiscales_is));

-- =============================================================================
-- FIN DU SCRIPT db.sql
-- =============================================================================
