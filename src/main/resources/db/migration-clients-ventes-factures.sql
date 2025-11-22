-- Script SQL pour créer les tables manquantes pour le système de point de vente céramique

-- Table des clients
CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255),
    nom_complet VARCHAR(255),
    telephone VARCHAR(50),
    email VARCHAR(255),
    adresse TEXT,
    ville VARCHAR(100),
    code_postal VARCHAR(20),
    categorie VARCHAR(50) DEFAULT 'PARTICULIER',
    numero_registre_commerce VARCHAR(100),
    numero_identification_fiscale VARCHAR(100),
    credit_autorise DECIMAL(15,2) DEFAULT 0,
    credit_utilise DECIMAL(15,2) DEFAULT 0,
    points_fidelite INTEGER DEFAULT 0,
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_derniere_visite TIMESTAMP,
    notes TEXT,
    point_de_vente_id BIGINT NOT NULL,
    CONSTRAINT fk_client_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id)
);

-- Table des ventes
CREATE TABLE IF NOT EXISTS ventes (
    id BIGSERIAL PRIMARY KEY,
    numero_ticket VARCHAR(100) UNIQUE NOT NULL,
    date_vente TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id BIGINT,
    vendeur_id BIGINT NOT NULL,
    montant_ht DECIMAL(15,2) DEFAULT 0,
    montant_tva DECIMAL(15,2) DEFAULT 0,
    montant_ttc DECIMAL(15,2) DEFAULT 0,
    remise_globale DECIMAL(15,2) DEFAULT 0,
    montant_final DECIMAL(15,2) DEFAULT 0,
    statut VARCHAR(50) DEFAULT 'EN_COURS',
    montant_paye DECIMAL(15,2) DEFAULT 0,
    montant_restant DECIMAL(15,2) DEFAULT 0,
    point_de_vente_id BIGINT NOT NULL,
    notes TEXT,
    date_annulation TIMESTAMP,
    motif_annulation TEXT,
    annule_par_user_id BIGINT,
    CONSTRAINT fk_vente_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_vente_vendeur FOREIGN KEY (vendeur_id) REFERENCES users(id),
    CONSTRAINT fk_vente_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id),
    CONSTRAINT fk_vente_annule_par FOREIGN KEY (annule_par_user_id) REFERENCES users(id)
);

-- Table des lignes de vente
CREATE TABLE IF NOT EXISTS lignes_vente (
    id BIGSERIAL PRIMARY KEY,
    vente_id BIGINT NOT NULL,
    produit_id BIGINT NOT NULL,
    designation VARCHAR(255),
    reference VARCHAR(100),
    quantite DECIMAL(15,3) NOT NULL,
    surface_m2 DECIMAL(15,2),
    prix_unitaire_ht DECIMAL(15,2) NOT NULL,
    taux_tva DECIMAL(5,2) DEFAULT 19.00,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    remise_pourcentage DECIMAL(5,2) DEFAULT 0,
    remise_montant DECIMAL(15,2) DEFAULT 0,
    notes TEXT,
    CONSTRAINT fk_ligne_vente FOREIGN KEY (vente_id) REFERENCES ventes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_vente_produit FOREIGN KEY (produit_id) REFERENCES produits(id)
);

-- Table des factures
CREATE TABLE IF NOT EXISTS factures (
    id BIGSERIAL PRIMARY KEY,
    numero_facture VARCHAR(100) UNIQUE NOT NULL,
    date_facture DATE NOT NULL,
    date_echeance DATE,
    client_id BIGINT NOT NULL,
    vente_id BIGINT,
    emise_par_user_id BIGINT NOT NULL,
    montant_ht DECIMAL(15,2) DEFAULT 0,
    montant_tva DECIMAL(15,2) DEFAULT 0,
    montant_ttc DECIMAL(15,2) DEFAULT 0,
    remise_globale DECIMAL(15,2) DEFAULT 0,
    montant_final DECIMAL(15,2) DEFAULT 0,
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE',
    montant_paye DECIMAL(15,2) DEFAULT 0,
    montant_restant DECIMAL(15,2) DEFAULT 0,
    point_de_vente_id BIGINT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    conditions_paiement TEXT,
    annulee BOOLEAN DEFAULT FALSE,
    date_annulation TIMESTAMP,
    motif_annulation TEXT,
    annulee_par_user_id BIGINT,
    chemin_pdf VARCHAR(500),
    CONSTRAINT fk_facture_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_facture_vente FOREIGN KEY (vente_id) REFERENCES ventes(id),
    CONSTRAINT fk_facture_emise_par FOREIGN KEY (emise_par_user_id) REFERENCES users(id),
    CONSTRAINT fk_facture_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id),
    CONSTRAINT fk_facture_annulee_par FOREIGN KEY (annulee_par_user_id) REFERENCES users(id)
);

-- Table des lignes de facture
CREATE TABLE IF NOT EXISTS lignes_facture (
    id BIGSERIAL PRIMARY KEY,
    facture_id BIGINT NOT NULL,
    produit_id BIGINT NOT NULL,
    designation VARCHAR(255),
    reference VARCHAR(100),
    quantite DECIMAL(15,3) NOT NULL,
    surface_m2 DECIMAL(15,2),
    prix_unitaire_ht DECIMAL(15,2) NOT NULL,
    taux_tva DECIMAL(5,2) DEFAULT 19.00,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    remise_pourcentage DECIMAL(5,2) DEFAULT 0,
    remise_montant DECIMAL(15,2) DEFAULT 0,
    notes TEXT,
    CONSTRAINT fk_ligne_facture FOREIGN KEY (facture_id) REFERENCES factures(id) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_facture_produit FOREIGN KEY (produit_id) REFERENCES produits(id)
);

-- Table des paiements
CREATE TABLE IF NOT EXISTS paiements (
    id BIGSERIAL PRIMARY KEY,
    numero_paiement VARCHAR(100) UNIQUE NOT NULL,
    date_paiement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vente_id BIGINT,
    facture_id BIGINT,
    client_id BIGINT,
    montant DECIMAL(15,2) NOT NULL,
    mode_paiement VARCHAR(50) NOT NULL,
    reference_paiement VARCHAR(255),
    nom_banque VARCHAR(255),
    numero_cheque VARCHAR(100),
    date_echeance TIMESTAMP,
    encaisse_par_user_id BIGINT NOT NULL,
    point_de_vente_id BIGINT NOT NULL,
    notes TEXT,
    annule BOOLEAN DEFAULT FALSE,
    date_annulation TIMESTAMP,
    motif_annulation TEXT,
    annule_par_user_id BIGINT,
    CONSTRAINT fk_paiement_vente FOREIGN KEY (vente_id) REFERENCES ventes(id),
    CONSTRAINT fk_paiement_facture FOREIGN KEY (facture_id) REFERENCES factures(id),
    CONSTRAINT fk_paiement_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_paiement_encaisse_par FOREIGN KEY (encaisse_par_user_id) REFERENCES users(id),
    CONSTRAINT fk_paiement_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id),
    CONSTRAINT fk_paiement_annule_par FOREIGN KEY (annule_par_user_id) REFERENCES users(id)
);

-- Table des chantiers
CREATE TABLE IF NOT EXISTS chantiers (
    id BIGSERIAL PRIMARY KEY,
    numero_chantier VARCHAR(100) UNIQUE NOT NULL,
    nom_chantier VARCHAR(255) NOT NULL,
    client_id BIGINT NOT NULL,
    adresse TEXT,
    ville VARCHAR(100),
    code_postal VARCHAR(20),
    date_debut DATE,
    date_fin_prevue DATE,
    date_fin_reelle DATE,
    statut VARCHAR(20) DEFAULT 'EN_COURS',
    nom_responsable VARCHAR(255),
    telephone_responsable VARCHAR(50),
    point_de_vente_id BIGINT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_chantier_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_chantier_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id)
);

-- Ajout de colonnes manquantes à la table produits si elle existe déjà
ALTER TABLE produits ADD COLUMN IF NOT EXISTS code_barre VARCHAR(100);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS couleur VARCHAR(100);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS texture VARCHAR(100);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS finition VARCHAR(100);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS origine VARCHAR(100);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS serie VARCHAR(100);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS surface_par_boite_m2 DECIMAL(10,4);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS quantite_par_boite INTEGER;
ALTER TABLE produits ADD COLUMN IF NOT EXISTS categorie_article VARCHAR(50);

-- Ajout de colonnes manquantes à la table commandes_client si elle existe
ALTER TABLE commandes_client ADD COLUMN IF NOT EXISTS client_id BIGINT;
ALTER TABLE commandes_client ADD COLUMN IF NOT EXISTS chantier_id BIGINT;
ALTER TABLE commandes_client ADD CONSTRAINT IF NOT EXISTS fk_commande_client_client
    FOREIGN KEY (client_id) REFERENCES clients(id);
ALTER TABLE commandes_client ADD CONSTRAINT IF NOT EXISTS fk_commande_client_chantier
    FOREIGN KEY (chantier_id) REFERENCES chantiers(id);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_clients_pdv ON clients(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_clients_telephone ON clients(telephone);
CREATE INDEX IF NOT EXISTS idx_clients_email ON clients(email);
CREATE INDEX IF NOT EXISTS idx_clients_categorie ON clients(categorie);

CREATE INDEX IF NOT EXISTS idx_ventes_pdv ON ventes(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_ventes_client ON ventes(client_id);
CREATE INDEX IF NOT EXISTS idx_ventes_vendeur ON ventes(vendeur_id);
CREATE INDEX IF NOT EXISTS idx_ventes_date ON ventes(date_vente);
CREATE INDEX IF NOT EXISTS idx_ventes_statut ON ventes(statut);

CREATE INDEX IF NOT EXISTS idx_factures_pdv ON factures(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_factures_client ON factures(client_id);
CREATE INDEX IF NOT EXISTS idx_factures_date ON factures(date_facture);
CREATE INDEX IF NOT EXISTS idx_factures_statut ON factures(statut);

CREATE INDEX IF NOT EXISTS idx_paiements_pdv ON paiements(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_paiements_vente ON paiements(vente_id);
CREATE INDEX IF NOT EXISTS idx_paiements_facture ON paiements(facture_id);
CREATE INDEX IF NOT EXISTS idx_paiements_date ON paiements(date_paiement);

CREATE INDEX IF NOT EXISTS idx_chantiers_pdv ON chantiers(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_chantiers_client ON chantiers(client_id);

CREATE INDEX IF NOT EXISTS idx_produits_code_barre ON produits(code_barre);
CREATE INDEX IF NOT EXISTS idx_produits_categorie ON produits(categorie_article);

