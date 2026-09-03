-- ==============================================================================
-- SCRIPT DE REMPLISSAGE COMPLET MULTI-TENANT (SEED DATA)
-- Deux Entreprises 100% Étanches & Isolées :
--   1. Entreprise Adil (Point de Vente 1 / Tenant 1) - SARL Adil Céramique & Négoce (Alger)
--   2. Entreprise Marouane (Point de Vente 2 / Tenant 2) - SARL Marouane Filtration & Auto (Oran)
-- Hash de tous les mots de passe : $2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO
-- ==============================================================================

BEGIN;

-- ==============================================================================
-- 0. VÉRIFICATION DDL : ASSURER L'EXISTENCE DES COLONNES MULTI-TENANT
-- ==============================================================================
ALTER TABLE depots ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE fournisseurs ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE ventes ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE factures ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE paiements ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE commandes ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE depenses ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE comptes_financiers ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE objectifs_commerciaux ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE relances_clients ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;
ALTER TABLE promesses_paiement ADD COLUMN IF NOT EXISTS point_de_vente_id BIGINT DEFAULT 1;

-- ==============================================================================
-- 1. POINTS DE VENTE (TENANTS)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS point_de_vente (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE,
    nom_point_de_vente VARCHAR(255) NOT NULL,
    nom VARCHAR(255),
    adresse TEXT,
    telephone VARCHAR(50),
    email VARCHAR(255),
    password VARCHAR(255),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actif BOOLEAN DEFAULT TRUE
);

INSERT INTO point_de_vente (id, tenant_id, nom_point_de_vente, nom, adresse, telephone, email, password, date_creation, actif) VALUES
(
    1, 1,
    'Entreprise Adil - Céramique & Négoce',
    'Entreprise Adil - Céramique & Négoce',
    'Zone Industrielle Oued Smar, Hangar 4, Alger',
    '023 85 10 20',
    'contact@adil-ceramique.dz',
    '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO',
    NOW(), true
),
(
    2, 2,
    'Entreprise Marouane - Filtration & Pièces Auto',
    'Entreprise Marouane - Filtration & Pièces Auto',
    'Boulevard des Martyrs, Es Senia, Oran',
    '041 55 40 30',
    'contact@marouane-auto.dz',
    '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO',
    NOW(), true
)
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    nom_point_de_vente = EXCLUDED.nom_point_de_vente,
    nom = EXCLUDED.nom,
    adresse = EXCLUDED.adresse,
    telephone = EXCLUDED.telephone,
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    actif = true;

-- ==============================================================================
-- 2. PROFILS D'ENTREPRISES LÉGAUX
-- ==============================================================================
CREATE TABLE IF NOT EXISTS entreprise_profiles (
    id BIGSERIAL PRIMARY KEY,
    nom_entreprise VARCHAR(255) NOT NULL,
    activite VARCHAR(255),
    adresse TEXT,
    ville VARCHAR(100),
    telephone VARCHAR(50),
    email VARCHAR(255),
    registre_commerce VARCHAR(100),
    numero_identification_fiscale VARCHAR(100),
    numero_identification_statistique VARCHAR(100),
    article_imposition VARCHAR(100),
    compte_bancaire_rib VARCHAR(100),
    nom_banque VARCHAR(100),
    devise VARCHAR(10) DEFAULT 'DZD',
    point_de_vente_id BIGINT UNIQUE NOT NULL,
    date_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO entreprise_profiles (id, nom_entreprise, activite, adresse, ville, telephone, email, registre_commerce, numero_identification_fiscale, numero_identification_statistique, article_imposition, compte_bancaire_rib, nom_banque, devise, point_de_vente_id, date_mise_a_jour) VALUES
(1, 'SARL ADIL CÉRAMIQUE & NÉGOCE', 'Importation et distribution céramique, faïence et sanitaire', 'Zone Industrielle Oued Smar Hangar 4', 'Alger', '023 85 10 20', 'contact@adil-ceramique.dz', '16/00-0987654B19', '001916012345678', '19850101002233', '1622334455', '00200015015220003344', 'BNA Banque', 'DZD', 1, NOW()),
(2, 'SARL MAROUANE FILTRATION & AUTO', 'Distribution pièces automobiles, filtration et freinage', 'Boulevard des Martyrs, Es Senia', 'Oran', '041 55 40 30', 'contact@marouane-auto.dz', '31/00-1122334A21', '002231098765432', '20010202004455', '3155667788', '00400031031440008899', 'BEA Banque', 'DZD', 2, NOW())
ON CONFLICT (id) DO UPDATE SET
    nom_entreprise = EXCLUDED.nom_entreprise,
    adresse = EXCLUDED.adresse,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 3. RÔLES ET HABILITATIONS ERP
-- ==============================================================================
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO roles (id, nom) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_POINT_DE_VENTE_MANAGER'),
(3, 'ROLE_CAISSIER'),
(4, 'ROLE_VENDEUR'),
(5, 'ROLE_MAGASINIER'),
(6, 'ROLE_GESTIONNAIRE'),
(7, 'ROLE_RESPONSABLE_COMMERCIAL'),
(8, 'ROLE_COMMERCIAL'),
(9, 'ROLE_COMPTABLE')
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom;

-- ==============================================================================
-- 4. UTILISATEURS (PASSWORD COMMUN: admin123 via bcrypt)
-- ==============================================================================
-- Entreprise 1 : Adil (point_de_vente_id = 1)
-- Entreprise 2 : Marouane (point_de_vente_id = 2)
INSERT INTO users (id, email, username, password, nom_complet, telephone, genre, role_id, point_de_vente_id, account_non_expired, account_non_locked, credentials_non_expired, enabled) VALUES
-- Utilisateurs Entreprise Adil
(1, 'admin@adil.dz', 'admin_adil', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Adil Directeur Général', '0550110001', 0, 1, 1, true, true, true, true),
(2, 'manager@adil.dz', 'manager_adil', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Sofiane Manager Adil', '0550110002', 0, 2, 1, true, true, true, true),
(3, 'commercial@adil.dz', 'commercial_adil', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Karim Commercial Adil', '0550110003', 0, 8, 1, true, true, true, true),
(4, 'caissier@adil.dz', 'caissier_adil', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Amine Caissier Adil', '0550110004', 0, 3, 1, true, true, true, true),
(5, 'magasinier@adil.dz', 'magasinier_adil', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Hamza Magasinier Adil', '0550110005', 0, 5, 1, true, true, true, true),

-- Utilisateurs Entreprise Marouane
(10, 'admin@marouane.dz', 'admin_marouane', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Marouane Directeur Auto', '0550220001', 0, 1, 2, true, true, true, true),
(11, 'manager@marouane.dz', 'manager_marouane', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Yacine Manager Marouane', '0550220002', 0, 2, 2, true, true, true, true),
(12, 'commercial@marouane.dz', 'commercial_marouane', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Tarek Commercial Marouane', '0550220003', 0, 8, 2, true, true, true, true),
(13, 'caissier@marouane.dz', 'caissier_marouane', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Bilal Caissier Marouane', '0550220004', 0, 3, 2, true, true, true, true),
(14, 'magasinier@marouane.dz', 'magasinier_marouane', '$2a$12$b4dRtlX6JtNSXG.i4EwWA.8BTGSe19WIIe1Kvh5oazKlBegdFk1hK', 'Zaki Magasinier Marouane', '0550220005', 0, 5, 2, true, true, true, true)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    nom_complet = EXCLUDED.nom_complet,
    role_id = EXCLUDED.role_id,
    point_de_vente_id = EXCLUDED.point_de_vente_id,
    enabled = true;

-- ==============================================================================
-- 5. DÉPÔTS PAR ENTREPRISE
-- ==============================================================================
INSERT INTO depots (id, nom, description, adresse, point_de_vente_id, date_creation, actif) VALUES
-- Dépôts Adil (1)
(1, 'Showroom Central Adil', 'Salle d''exposition céramique et carrelage - Vente comptoir', '15 Rue de la Liberté, Oued Smar, Alger', 1, NOW(), true),
(2, 'Hangar Dépôt Oued Smar Adil', 'Zone de stockage palettes lourdes, faïence et colles', 'Zone Industrielle Oued Smar Hangar 4, Alger', 1, NOW(), true),

-- Dépôts Marouane (2)
(3, 'Magasin Pièces Détachées Marouane', 'Boutique comptoir filtration et pièces d''usure', 'Boulevard des Martyrs, Es Senia, Oran', 2, NOW(), true),
(4, 'Hangar Logistique Filtres Marouane', 'Centre de distribution régionale filtres et freinage', 'Zone d''activité Es Senia, Oran', 2, NOW(), true)
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    point_de_vente_id = EXCLUDED.point_de_vente_id,
    actif = true;

-- ==============================================================================
-- 6. CATÉGORIES HIÉRARCHIQUES RÉCURSIVES
-- ==============================================================================
-- Entreprise Adil (Céramique & Matériaux de pose)
INSERT INTO categories (id, nom, code, description, couleur, icone, parent_id, point_de_vente_id, actif, date_creation) VALUES
-- Racines Adil
(1, 'Revêtements Sols & Murs', 'REV_ADIL', 'Carrelages et faïences', '#3b82f6', 'Layers', NULL, 1, true, NOW()),
(2, 'Matériaux de Pose & Chimie', 'MAT_ADIL', 'Colles, mortiers et profilés', '#10b981', 'Package', NULL, 1, true, NOW()),
-- Sous-catégories Adil
(11, 'Carrelage Sol Intérieur', 'SOL_INT', 'Grès cérame poli et émaillé pour sol intérieur', '#3b82f6', 'Square', 1, 1, true, NOW()),
(12, 'Faïence Murale & Décor', 'FAI_MUR', 'Revêtement mural cuisine et salle de bain', '#60a5fa', 'Grid', 1, 1, true, NOW()),
(21, 'Mortiers-Colles Céramique', 'COL_CER', 'Colles en poudre haute performance C2TE', '#10b981', 'Box', 2, 1, true, NOW()),

-- Entreprise Marouane (Filtration Automobile Récursive & Freinage)
-- EXACTEMENT le use-case du client : Filtration -> Filtres à Huile -> Références
(100, 'Filtration', 'FILT_MAR', 'Systèmes complets de filtration automobile', '#f59e0b', 'Filter', NULL, 2, true, NOW()),
(200, 'Freinage & Sécurité', 'FREIN_MAR', 'Éléments de friction et disques', '#ef4444', 'Shield', NULL, 2, true, NOW()),
-- Sous-catégories Marouane
(101, 'Filtres à Huile', 'FILT_HUILE', 'Filtres à huile moteur vissés et cartouches', '#f59e0b', 'Droplet', 100, 2, true, NOW()),
(102, 'Filtres à Air Moteur', 'FILT_AIR', 'Filtres à air admission moteur thermique', '#fbbf24', 'Wind', 100, 2, true, NOW()),
(103, 'Filtres à Carburant Diesel', 'FILT_DIESEL', 'Filtres à gazole et séparateurs d''eau', '#d97706', 'Fuel', 100, 2, true, NOW()),
(201, 'Plaquettes de Frein', 'PLAQ_FREIN', 'Plaquettes avant et arrière homologuées', '#ef4444', 'Disc', 200, 2, true, NOW()),
(202, 'Disques de Frein', 'DISQ_FREIN', 'Disques ventilés et pleins', '#dc2626', 'Circle', 200, 2, true, NOW())
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    parent_id = EXCLUDED.parent_id,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 7. PRODUITS PAR ENTREPRISE
-- ==============================================================================
INSERT INTO produits (id, code_barre, reference, designation, description, categorie_id, prix_achat_ht, prix_vente_ttc, prix_achat, prix_vente, stock_minimum, point_de_vente_id, actif, date_creation) VALUES
-- Produits Entreprise Adil (Céramique)
(1, '613000000001', 'CAR-SOL-6060', 'Grès Cérame Émaillé Calacatta 60x60 (m²)', 'Carrelage sol intérieur haute résistance', 11, 1400.00, 2300.00, 1400.00, 1800.00, 20.0, 1, true, NOW()),
(2, '613000000002', 'FAI-MUR-3060', 'Faïence Murale Metro Blanc Brillant 30x60 (m²)', 'Faïence murale format métro pour salle de bain', 12, 1100.00, 1850.00, 1100.00, 1500.00, 15.0, 1, true, NOW()),
(3, '613000000003', 'COL-HP-25KG', 'Mortier Colle C2TE Sac 25kg Haute Adhérence', 'Colle carrelage haute performance pour grands formats', 21, 850.00, 1350.00, 850.00, 1100.00, 50.0, 1, true, NOW()),

-- Produits Entreprise Marouane (Filtration & Auto)
(101, '3286011409320', 'PUR-LS932', 'Filtre à Huile Purflux LS932 (Renault / Dacia / Nissan)', 'Filtre à huile pour moteurs essence/diesel Renault', 101, 650.00, 1150.00, 650.00, 900.00, 15.0, 2, true, NOW()),
(102, '4011558712750', 'MANN-W71275', 'Filtre à Huile Mann Filter W712/75 (Volkswagen / Seat / Audi)', 'Filtre à huile vissable pour groupe VAG 1.4/1.6 TSI', 101, 850.00, 1450.00, 850.00, 1100.00, 10.0, 2, true, NOW()),
(103, '3165143300300', 'BOS-C30', 'Filtre à Air Moteur Bosch F026400030 (Peugeot / Citroën)', 'Filtre à air moteur pour moteurs PSA THP', 102, 950.00, 1600.00, 950.00, 1250.00, 10.0, 2, true, NOW()),
(104, '5012759492400', 'DEL-HDF924', 'Filtre Carburant Gazole Delphi HDF924 (DCI / HDI)', 'Filtre gasoil avec séparateur eau pour diesel modernes', 103, 1800.00, 2900.00, 1800.00, 2200.00, 8.0, 2, true, NOW()),
(201, '8020584085000', 'BREM-P85020', 'Jeu Plaquettes Avant Brembo P85020 (Golf 7 / Leon)', 'Plaquettes frein avant sportives pour VAG/SEAT', 201, 3200.00, 5100.00, 3200.00, 4000.00, 5.0, 2, true, NOW()),
(202, '4044197114000', 'FER-DDF114', 'Jeu Disques Ventilés Ferodo DDF114 280mm', 'Disques de frein ventilés haute performance', 202, 6500.00, 9800.00, 6500.00, 7800.00, 3.0, 2, true, NOW())
ON CONFLICT (id) DO UPDATE SET
    designation = EXCLUDED.designation,
    reference = EXCLUDED.reference,
    categorie_id = EXCLUDED.categorie_id,
    point_de_vente_id = EXCLUDED.point_de_vente_id,
    prix_achat_ht = EXCLUDED.prix_achat_ht,
    prix_vente_ttc = EXCLUDED.prix_vente_ttc;

-- ==============================================================================
-- 8. STOCKS ET QUALITÉS DE STOCK PAR ENTREPRISE
-- ==============================================================================
-- Stocks (un enregistrement par produit)
INSERT INTO stocks (id, produit_id) VALUES
(1, 1), (2, 2), (3, 3),
(101, 101), (102, 102), (103, 103), (104, 104), (201, 201), (202, 202)
ON CONFLICT (produit_id) DO NOTHING;

-- StockQualites (quantités réelles par qualité de produit)
INSERT INTO stock_qualites (id, stock_id, produit_id, qualite_produit, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) VALUES
-- Adil
(1,  1,   1,   'PREMIERE_QUALITE', 350.0, 30.0, 20.0, NOW()),
(2,  2,   2,   'PREMIERE_QUALITE', 240.0, 20.0, 15.0, NOW()),
(3,  3,   3,   'PREMIERE_QUALITE', 500.0, 50.0, 50.0, NOW()),
-- Marouane
(101, 101, 101, 'PREMIERE_QUALITE', 85.0, 10.0, 15.0, NOW()),
(102, 102, 102, 'PREMIERE_QUALITE', 60.0,  5.0, 10.0, NOW()),
(103, 103, 103, 'PREMIERE_QUALITE', 45.0,  0.0, 10.0, NOW()),
(104, 104, 104, 'PREMIERE_QUALITE', 30.0,  4.0,  8.0, NOW()),
(201, 201, 201, 'PREMIERE_QUALITE', 25.0,  2.0,  5.0, NOW()),
(202, 202, 202, 'PREMIERE_QUALITE', 18.0,  0.0,  3.0, NOW())
ON CONFLICT (id) DO UPDATE SET
    quantite_disponible = EXCLUDED.quantite_disponible,
    quantite_reservee   = EXCLUDED.quantite_reservee;

-- ==============================================================================
-- 9. FOURNISSEURS PAR ENTREPRISE
-- ==============================================================================
INSERT INTO fournisseurs (id, raison_social, adresse, telephone, email, contact, point_de_vente_id, actif, date_creation) VALUES
-- Fournisseurs Adil
(1, 'Cévital Céramique Algérie', 'Zone Industrielle Oued Smar, Alger', '023 80 11 22', 'commercial@cevital-ceram.dz', 'M. Benali', 1, true, NOW()),
(2, 'Sika Algérie Chimiques', 'Zone Industrielle Rouiba, Alger', '023 85 44 55', 'commandes@sika.dz', 'Mme. Brahimi', 1, true, NOW()),

-- Fournisseurs Marouane
(10, 'Bosch Algérie Pièces Auto', 'Parc d''activité Es Senia, Oran', '041 33 22 11', 'distribution@bosch-algerie.dz', 'M. Mansour', 2, true, NOW()),
(11, 'Purflux & Sogefi Maghreb', 'Zone Logistique Port d''Oran', '041 44 55 66', 'contact@sogefigroup.dz', 'M. Larbi', 2, true, NOW()),
(12, 'Brembo Afrique Distribution', 'Boulevard ALN, Oran', '041 66 77 88', 'ventes@brembo-dz.com', 'M. Cherif', 2, true, NOW())
ON CONFLICT (id) DO UPDATE SET
    raison_social = EXCLUDED.raison_social,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 10. CLIENTS PAR ENTREPRISE (STRICTEMENT ÉTANCHES)
-- ==============================================================================
INSERT INTO clients (id, nom, prenom, nom_complet, telephone, email, adresse, ville, categorie, tarif, credit_autorise, credit_utilise, points_fidelite, commercial_user_id, point_de_vente_id, actif, date_creation) VALUES
-- Clients Entreprise Adil (1)
(1, 'El Bahia', 'BTPH SARL', 'BTPH SARL El Bahia', '0550100200', 'contact@elbahia-btp.dz', 'Cité des Asphodèles, Ben Aknoun', 'Alger', 'ENTREPRISE', 'GROSSISTE', 500000.00, 125000.00, 150, 3, 1, true, NOW()),
(2, 'Sahel', 'Promotion Immobilière', 'Promotion Immobilière Sahel', '0550300400', 'achats@sahel-immo.dz', 'Boulevard du 11 Décembre, El Biar', 'Alger', 'ENTREPRISE', 'GROSSISTE', 800000.00, 310000.00, 280, 3, 1, true, NOW()),
(3, 'Mourad', 'Karim', 'Karim Mourad', '0550500600', 'karim.mourad@gmail.com', '12 Rue Didouche Mourad', 'Alger', 'PARTICULIER', 'DETAIL', 50000.00, 0.00, 45, 3, 1, true, NOW()),

-- Clients Entreprise Marouane (2)
(10, 'Auto Express Oran', 'Garage SARL', 'Garage SARL Auto Express Oran', '0555112233', 'atelier@autoexpress-oran.dz', 'Rue Ben M''hidi', 'Oran', 'ENTREPRISE', 'GROSSISTE', 300000.00, 85000.00, 110, 12, 2, true, NOW()),
(11, 'Transport Ouest', 'SARL Logistique', 'SARL Logistique Transport Ouest', '0555445566', 'flotte@transport-ouest.dz', 'Zone Industrielle Es Senia', 'Oran', 'ENTREPRISE', 'GROSSISTE', 600000.00, 140000.00, 190, 12, 2, true, NOW()),
(12, 'Belkacem', 'Samir', 'Samir Belkacem (Taxi)', '0555778899', 'samir.taxi31@gmail.com', 'Cité Gambetta', 'Oran', 'PARTICULIER', 'DETAIL', 30000.00, 0.00, 25, 12, 2, true, NOW())
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    nom_complet = EXCLUDED.nom_complet,
    point_de_vente_id = EXCLUDED.point_de_vente_id,
    credit_utilise = EXCLUDED.credit_utilise;

-- ==============================================================================
-- 11. COMPTES FINANCIERS PAR ENTREPRISE
-- ==============================================================================
INSERT INTO comptes_financiers (id, code, nom, type, solde_actuel, devise, numero_compte_rib, nom_banque, actif, point_de_vente_id, date_creation) VALUES
-- Comptes Adil (1)
(1, 'CAISSE-ADIL', 'Caisse Principale Adil Oued Smar', 'CAISSE_PHYSIQUE', 95000.00, 'DZD', NULL, NULL, true, 1, NOW()),
(2, 'BNA-ADIL', 'Compte BNA Banque Adil Alger', 'COMPTE_BANCAIRE', 2850000.00, 'DZD', '00200015015220003344', 'BNA', true, 1, NOW()),

-- Comptes Marouane (2)
(3, 'CAISSE-MAR', 'Caisse Magasin Marouane Oran', 'CAISSE_PHYSIQUE', 145000.00, 'DZD', NULL, NULL, true, 2, NOW()),
(4, 'BEA-MAR', 'Compte BEA Banque Marouane Oran', 'COMPTE_BANCAIRE', 4200000.00, 'DZD', '00400031031440008899', 'BEA', true, 2, NOW())
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    solde_actuel = EXCLUDED.solde_actuel,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 12. VENTES COMPTOIR & FACTURES PAR ENTREPRISE
-- ==============================================================================
-- Vente 1 Adil
INSERT INTO ventes (id, numero_ticket, date_vente, client_id, vendeur_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, montant_paye, montant_restant, statut, point_de_vente_id) VALUES
(1, 'TK-ADIL-2026-001', NOW() - INTERVAL '2 days', 1, 3, 105000.00, 19950.00, 124950.00, 0.00, 124950.00, 124950.00, 0.00, 'VALIDEE', 1),
(2, 'TK-ADIL-2026-002', NOW() - INTERVAL '1 day', 2, 3, 260000.00, 49400.00, 309400.00, 9400.00, 300000.00, 175000.00, 125000.00, 'VALIDEE', 1),

-- Vente 2 Marouane
(10, 'TK-MAR-2026-001', NOW() - INTERVAL '2 days', 10, 12, 72000.00, 13680.00, 85680.00, 680.00, 85000.00, 85000.00, 0.00, 'VALIDEE', 2),
(11, 'TK-MAR-2026-002', NOW() - INTERVAL '1 day', 11, 12, 118000.00, 22420.00, 140420.00, 420.00, 140000.00, 55000.00, 85000.00, 'VALIDEE', 2)
ON CONFLICT (id) DO UPDATE SET
    numero_ticket = EXCLUDED.numero_ticket,
    montant_final = EXCLUDED.montant_final,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- Lignes de Vente
INSERT INTO lignes_vente (id, vente_id, produit_id, quantite, prix_unitaire_ht, taux_tva, montant_ht, montant_ttc) VALUES
(1,  1,   1,   50.0,  1932.77, 19.00,  96638.50, 115000.00),
(2,  2,   2,  100.0,  1554.62, 19.00, 155462.00, 185000.00),
(10, 10, 101,  40.0,   966.39, 19.00,  38655.60,  46000.00),
(11, 11, 201,  20.0,  4285.71, 19.00,  85714.20, 102000.00)
ON CONFLICT (id) DO NOTHING;

-- Factures Vente
INSERT INTO factures (id, numero_facture, date_facture, date_echeance, client_id, vente_id, emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, montant_paye, montant_restant, statut, point_de_vente_id) VALUES
(1,  'FAC-ADIL-2026-001', CURRENT_DATE - 2, CURRENT_DATE + 28,  1,  1,  3, 105000.00, 19950.00, 124950.00,    0.00, 124950.00, 124950.00,      0.00, 'PAYEE_TOTALEMENT',    1),
(2,  'FAC-ADIL-2026-002', CURRENT_DATE - 1, CURRENT_DATE + 29,  2,  2,  3, 260000.00, 49400.00, 309400.00, 9400.00, 300000.00, 175000.00, 125000.00, 'PAYEE_PARTIELLEMENT', 1),
(10, 'FAC-MAR-2026-001',  CURRENT_DATE - 2, CURRENT_DATE + 28, 10, 10, 12,  72000.00, 13680.00,  85680.00,  680.00,  85000.00,  85000.00,      0.00, 'PAYEE_TOTALEMENT',    2),
(11, 'FAC-MAR-2026-002',  CURRENT_DATE - 1, CURRENT_DATE + 29, 11, 11, 12, 118000.00, 22420.00, 140420.00,  420.00, 140000.00,  55000.00,  85000.00, 'PAYEE_PARTIELLEMENT', 2)
ON CONFLICT (id) DO UPDATE SET
    numero_facture = EXCLUDED.numero_facture,
    montant_restant = EXCLUDED.montant_restant,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- Paiements Vente
INSERT INTO paiements (id, numero_paiement, date_paiement, vente_id, facture_id, client_id, montant, mode_paiement, encaisse_par_user_id, point_de_vente_id) VALUES
(1,  'PAI-ADIL-001', NOW() - INTERVAL '2 days',  1,  1,  1, 124950.00, 'VIREMENT',  3, 1),
(2,  'PAI-ADIL-002', NOW() - INTERVAL '1 day',   2,  2,  2, 175000.00, 'CHEQUE',    3, 1),
(10, 'PAI-MAR-001',  NOW() - INTERVAL '2 days', 10, 10, 10,  85000.00, 'ESPECES',  13, 2),
(11, 'PAI-MAR-002',  NOW() - INTERVAL '1 day',  11, 11, 11,  55000.00, 'VIREMENT', 13, 2)
ON CONFLICT (id) DO UPDATE SET
    numero_paiement = EXCLUDED.numero_paiement,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 13. DÉPENSES D'EXPLOITATION PAR ENTREPRISE
-- ==============================================================================
INSERT INTO depenses (id, reference, designation, montant, date_depense, categorie, mode_paiement, beneficiaire, cree_par_user_id, point_de_vente_id, date_creation) VALUES
(1,  'DEP-ADIL-001', 'Loyer showroom et hangar Oued Smar - Mois en cours',     120000.00, CURRENT_DATE - 3, 'LOYER',               'VIREMENT', 'Bailleur Oued Smar',           1,  1, NOW()),
(2,  'DEP-ADIL-002', 'Facture Sonelgaz Hangar et Showroom',                      18500.00, CURRENT_DATE - 1, 'ELECTRICITE_EAU',     'ESPECES',  'Sonelgaz',                     2,  1, NOW()),
(10, 'DEP-MAR-001',  'Loyer magasin et dépôt Es Senia Oran',                     95000.00, CURRENT_DATE - 3, 'LOYER',               'VIREMENT', 'Bailleur Es Senia',            10, 2, NOW()),
(11, 'DEP-MAR-002',  'Frais transport livraison express pièces urgentes',         14000.00, CURRENT_DATE - 1, 'TRANSPORT_CARBURANT', 'ESPECES',  'Transporteur express',         11, 2, NOW())
ON CONFLICT (id) DO UPDATE SET
    designation = EXCLUDED.designation,
    montant = EXCLUDED.montant,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 14. OBJECTIFS COMMERCIAUX PAR ENTREPRISE
-- ==============================================================================
INSERT INTO objectifs_commerciaux (id, commercial_user_id, annee, mois, objectif_ca, objectif_marge, point_de_vente_id, date_creation) VALUES
(1, 3, 2026, 9, 2000000.00, 500000.00, 1, NOW()),
(2, 12, 2026, 9, 1500000.00, 450000.00, 2, NOW())
ON CONFLICT (id) DO UPDATE SET
    objectif_ca = EXCLUDED.objectif_ca,
    point_de_vente_id = EXCLUDED.point_de_vente_id;

-- ==============================================================================
-- 15. RELANCES ET PROMESSES DE RECOUVREMENT PAR ENTREPRISE
-- ==============================================================================
INSERT INTO relances_clients (id, client_id, facture_id, date_relance, canal, interlocuteur, commentaire, effectue_par_user_id, point_de_vente_id) VALUES
(1, 2,  2,  NOW() - INTERVAL '1 day', 'TELEPHONE',    'M. Gérant Promotion Sahel',        'Relance FAC-ADIL-2026-002 solde 125 000 DA. Promesse chèque sous 5 jours.', 3,  1),
(2, 11, 11, NOW() - INTERVAL '1 day', 'WHATSAPP_SMS', 'Chef atelier Transport Ouest',  'Rappel solde 85 000 DA après livraison plaquettes Brembo. Virement prévu.',   12, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO promesses_paiement (id, client_id, facture_id, date_promesse, date_echeance_promise, montant_promis, statut, enregistre_par_user_id, point_de_vente_id) VALUES
(1, 2,  2,  NOW() - INTERVAL '1 day', CURRENT_DATE + 5, 125000.00, 'EN_ATTENTE', 3,  1),
(2, 11, 11, NOW() - INTERVAL '1 day', CURRENT_DATE + 3,  85000.00, 'EN_ATTENTE', 12, 2)
ON CONFLICT (id) DO NOTHING;

-- Synchronisation des séquences PostgreSQL
SELECT setval(pg_get_serial_sequence('point_de_vente', 'id'), COALESCE(MAX(id), 1)) FROM point_de_vente;
SELECT setval(pg_get_serial_sequence('entreprise_profiles', 'id'), COALESCE(MAX(id), 1)) FROM entreprise_profiles;
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('depots', 'id'), COALESCE(MAX(id), 1)) FROM depots;
SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE(MAX(id), 1)) FROM categories;
SELECT setval(pg_get_serial_sequence('produits', 'id'), COALESCE(MAX(id), 1)) FROM produits;
SELECT setval(pg_get_serial_sequence('stocks', 'id'), COALESCE(MAX(id), 1)) FROM stocks;
SELECT setval(pg_get_serial_sequence('stock_qualites', 'id'), COALESCE(MAX(id), 1)) FROM stock_qualites;
SELECT setval(pg_get_serial_sequence('fournisseurs', 'id'), COALESCE(MAX(id), 1)) FROM fournisseurs;
SELECT setval(pg_get_serial_sequence('clients', 'id'), COALESCE(MAX(id), 1)) FROM clients;
SELECT setval(pg_get_serial_sequence('comptes_financiers', 'id'), COALESCE(MAX(id), 1)) FROM comptes_financiers;
SELECT setval(pg_get_serial_sequence('ventes', 'id'), COALESCE(MAX(id), 1)) FROM ventes;
SELECT setval(pg_get_serial_sequence('lignes_vente', 'id'), COALESCE(MAX(id), 1)) FROM lignes_vente;
SELECT setval(pg_get_serial_sequence('factures', 'id'), COALESCE(MAX(id), 1)) FROM factures;
SELECT setval(pg_get_serial_sequence('paiements', 'id'), COALESCE(MAX(id), 1)) FROM paiements;
SELECT setval(pg_get_serial_sequence('depenses', 'id'), COALESCE(MAX(id), 1)) FROM depenses;
SELECT setval(pg_get_serial_sequence('objectifs_commerciaux', 'id'), COALESCE(MAX(id), 1)) FROM objectifs_commerciaux;

COMMIT;
