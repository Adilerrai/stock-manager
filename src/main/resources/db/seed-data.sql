-- ==============================================================================
-- SCRIPT DE REMPLISSAGE COMPLET DE LA BASE DE DONNÉES (SEED DATA)
-- Système: Point de Vente & Gestion de Stock Céramique (point-vente-saas)
-- Hash de tous les mots de passe: $2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO
-- ==============================================================================

BEGIN;

-- ==============================================================================
-- 1. POINT DE VENTE & MULTI-TENANT
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

INSERT INTO point_de_vente (id, tenant_id, nom_point_de_vente, nom, adresse, telephone, email, password, date_creation, actif)
VALUES (
    1,
    1,
    'Point de Vente Principal - Alger',
    'Point de Vente Principal - Alger',
    '15 Rue de la Liberté, Oued Smar, Alger',
    '021 23 45 67',
    'contact@pointvente-ceram.dz',
    '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO',
    NOW(),
    true
)
ON CONFLICT (id) DO UPDATE SET
    password = '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO',
    nom_point_de_vente = EXCLUDED.nom_point_de_vente,
    actif = true;

-- ==============================================================================
-- 2. RÔLES ET HABILITATIONS
-- ==============================================================================
INSERT INTO roles (id, nom) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_POINT_DE_VENTE_MANAGER'),
(3, 'ROLE_CAISSIER'),
(4, 'ROLE_VENDEUR'),
(5, 'ROLE_MAGASINIER')
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom;

-- Habilitations
INSERT INTO habilitations (id, nom) VALUES
(1, 'VENTE_CREATE'),
(2, 'VENTE_READ'),
(3, 'VENTE_ANNULER'),
(4, 'STOCK_VOIR'),
(5, 'STOCK_AJUSTER'),
(6, 'CLIENT_GESTION'),
(7, 'CAISSE_OUVRIR'),
(8, 'CAISSE_FERMER'),
(9, 'FOURNISSEUR_GESTION'),
(10, 'FACTURE_ACHAT_GESTION'),
(11, 'TRESORERIE_GESTION'),
(12, 'RAPPORT_VOIR'),
(13, 'ADMIN_GESTION')
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom;

-- Association rôles <-> habilitations
CREATE TABLE IF NOT EXISTS roles_habilitations (
    role_id BIGINT NOT NULL,
    habilitation_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, habilitation_id)
);

INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
-- ROLE_ADMIN a toutes les permissions
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13),
-- ROLE_POINT_DE_VENTE_MANAGER
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11), (2, 12),
-- ROLE_CAISSIER
(3, 1), (3, 2), (3, 7), (3, 8),
-- ROLE_VENDEUR
(4, 1), (4, 2), (4, 4), (4, 6),
-- ROLE_MAGASINIER
(5, 4), (5, 5), (5, 9)
ON CONFLICT DO NOTHING;

-- ==============================================================================
-- 3. UTILISATEURS (PASSWORD: $2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO)
-- ==============================================================================
INSERT INTO users (id, email, username, password, nom_complet, telephone, genre, role_id, account_non_expired, account_non_locked, credentials_non_expired, enabled) VALUES
(1, 'admin@magasin.dz', 'admin', '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO', 'Administrateur Principal', '0550112233', 0, 1, true, true, true, true),
(2, 'manager@magasin.dz', 'manager', '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO', 'Gérant Magasin', '0550223344', 0, 2, true, true, true, true),
(3, 'g1500@magasin.dz', 'G1500', '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO', 'Caissier G1500', '0550334455', 0, 3, true, true, true, true),
(4, 'vendeur@magasin.dz', 'vendeur', '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO', 'Karim Vendeur', '0550445566', 0, 4, true, true, true, true),
(5, 'magasinier@magasin.dz', 'magasinier', '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO', 'Omar Magasinier', '0550556677', 0, 5, true, true, true, true)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password = '$2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO',
    nom_complet = EXCLUDED.nom_complet,
    role_id = EXCLUDED.role_id,
    enabled = true;

-- ==============================================================================
-- 4. DÉPÔTS
-- ==============================================================================
INSERT INTO depots (id, nom, description, adresse, date_creation, actif) VALUES
(1, 'Dépôt Principal', 'Zone de stockage principale - Réception palettes et gros volume', 'Zone Industrielle Oued Smar, Hangar 4, Alger', NOW(), true),
(2, 'Dépôt Magasin Showroom', 'Stock tampon et pièces d''exposition disponibles immédiatement', '15 Rue de la Liberté, Oued Smar, Alger', NOW(), true)
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    adresse = EXCLUDED.adresse,
    actif = true;

-- ==============================================================================
-- 5. CATÉGORIES DE PRODUITS
-- ==============================================================================
INSERT INTO categories (id, nom, code, description, couleur, icone, actif, point_de_vente_id, date_creation) VALUES
(1, 'Carrelage Sol', 'SOL', 'Carrelages céramiques pour intérieur et extérieur', '#3b82f6', 'Layers', true, 1, NOW()),
(2, 'Faïence Murale', 'MUR', 'Faïences décoratives pour cuisine et salle de bain', '#10b981', 'Grid', true, 1, NOW()),
(3, 'Grès Cérame', 'GRES', 'Grès cérame émaillé et pleine masse grand format', '#f59e0b', 'Square', true, 1, NOW()),
(4, 'Sanitaire & Baignoires', 'SAN', 'Équipements sanitaires, cuvettes et robinetterie', '#8b5cf6', 'Bath', true, 1, NOW()),
(5, 'Colles & Mortiers', 'COL', 'Mortiers-colles, ciments-joints et primaires d''accrochage', '#ec4899', 'Package', true, 1, NOW()),
(6, 'Accessoires & Profilés', 'ACC', 'Croisillons autonivelants, plinthes et profilés alu/inox', '#6b7280', 'Wrench', true, 1, NOW())
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    code = EXCLUDED.code,
    couleur = EXCLUDED.couleur,
    actif = true;

-- ==============================================================================
-- 6. OPTIONS D'UNITÉ DE MESURE
-- ==============================================================================
INSERT INTO unite_mesure_options (id, value, label) VALUES
(1, 'M2', 'Mètre carré (m²)'),
(2, 'PIECE', 'Pièce'),
(3, 'KG', 'Kilogramme (kg)'),
(4, 'LITRE', 'Litre (L)'),
(5, 'METRE', 'Mètre linéaire (m)')
ON CONFLICT (id) DO UPDATE SET
    value = EXCLUDED.value,
    label = EXCLUDED.label;

-- ==============================================================================
-- 7. FOURNISSEURS
-- ==============================================================================
INSERT INTO fournisseurs (id, raison_social, adresse, telephone, email, contact, actif, date_creation) VALUES
(1, 'Céramiques El Badr', 'Zone Industrielle Rouiba, Alger', '021 44 55 66', 'contact@elbadr-ceram.dz', 'M. Badr', true, NOW()),
(2, 'Eurl España Import Ceramic', 'Zone Portuaire, Skikda', '038 88 99 00', 'import@espana-tile.com', 'M. Carlos Gomez', true, NOW()),
(3, 'Société Algérienne de Céramique (SAC)', 'Zone Industrielle Bordj Bou Arreridj', '035 77 88 99', 'ventes@sac-ceramique.dz', 'M. Bouzid', true, NOW()),
(4, 'Atlas Sanitaire & Bains', 'Zone Industrielle Oued Smar, Alger', '021 55 66 77', 'contact@atlas-sanitaire.dz', 'M. Mansouri', true, NOW())
ON CONFLICT (id) DO UPDATE SET
    raison_social = EXCLUDED.raison_social,
    telephone = EXCLUDED.telephone,
    email = EXCLUDED.email,
    contact = EXCLUDED.contact,
    actif = true;

-- Synchronisation de sécurité si la colonne nom existe
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fournisseurs' AND column_name='nom') THEN
        UPDATE fournisseurs SET nom = raison_social WHERE nom IS NULL OR nom = '';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fournisseurs' AND column_name='point_de_vente_id') THEN
        UPDATE fournisseurs SET point_de_vente_id = 1 WHERE point_de_vente_id IS NULL;
    END IF;
END $$;

-- ==============================================================================
-- 8. CLIENTS
-- ==============================================================================
INSERT INTO clients (id, nom, prenom, nom_complet, telephone, email, adresse, ville, code_postal, categorie, numero_registre_commerce, numero_identification_fiscale, credit_autorise, credit_utilise, points_fidelite, actif, date_creation, notes) VALUES
(1, 'Benali', 'Ahmed', 'Ahmed Benali', '0661 12 34 56', 'ahmed.benali@gmail.com', '12 Rue Didouche Mourad', 'Alger', '16000', 'PARTICULIER', NULL, NULL, 0.00, 0.00, 150, true, NOW() - INTERVAL '60 days', 'Client particulier régulier - Rénovation appartement'),
(2, 'Khelil', 'Karim', 'Karim Khelil', '0770 23 45 67', 'karim.khelil@yahoo.fr', '45 Boulevard des Martyrs', 'Oran', '31000', 'PARTICULIER', NULL, NULL, 0.00, 0.00, 80, true, NOW() - INTERVAL '45 days', 'Achat comptoir pour villa'),
(3, 'SARL Bâtiment Moderne', NULL, 'SARL Bâtiment Moderne', '021 66 77 88', 'contact@batimod.dz', 'Zone d''Activité Dar El Beida', 'Alger', '16100', 'PROFESSIONNEL', '16/00-1234567B19', '001916012345678', 600000.00, 120000.00, 520, true, NOW() - INTERVAL '90 days', 'Entreprise TCE - Paiement par chèque à 30 jours'),
(4, 'EURL Promobat Algérie', NULL, 'EURL Promobat Algérie', '023 44 55 66', 'direction@promobat.dz', 'Centre d''Affaires Bab Ezzouar', 'Alger', '16311', 'ENTREPRISE', '16/00-9876543B20', '002016098765432', 2000000.00, 450000.00, 1800, true, NOW() - INTERVAL '120 days', 'Promoteur immobilier - Projets résidentiels 80 logements'),
(5, 'Amrani', 'Sofiane', 'Sofiane Amrani (Architecte)', '0555 98 76 54', 'amrani.archi@gmail.com', '8 Rue Hassiba Ben Bouali', 'Alger', '16000', 'ARCHITECTE', '16/00-5544332A21', '002116055443322', 300000.00, 0.00, 310, true, NOW() - INTERVAL '30 days', 'Architecte d''intérieur prescripteur'),
(6, 'Meziane', 'Yacine', 'Yacine Meziane', '0662 33 44 55', 'yacine.meziane@gmail.com', '24 Avenue Colonel Amirouche', 'Blida', '09000', 'PARTICULIER', NULL, NULL, 50000.00, 0.00, 95, true, NOW() - INTERVAL '15 days', 'Client rénovation salle de bain')
ON CONFLICT (id) DO UPDATE SET
    nom = EXCLUDED.nom,
    nom_complet = EXCLUDED.nom_complet,
    telephone = EXCLUDED.telephone,
    credit_autorise = EXCLUDED.credit_autorise,
    credit_utilise = EXCLUDED.credit_utilise,
    actif = true;

-- Si la colonne point_de_vente_id existe dans clients, la synchroniser
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='clients' AND column_name='point_de_vente_id') THEN
        UPDATE clients SET point_de_vente_id = 1 WHERE point_de_vente_id IS NULL;
    END IF;
END $$;

-- ==============================================================================
-- 9. PRODUITS
-- ==============================================================================
INSERT INTO produits (
    id, reference, designation, description, actif, groupe_article, code_barre,
    categorie_id, categorie_article, attributes,
    prix_achat_ht, prix_achat_ttc, prix_vente_ht, prix_vente_ttc,
    prix_achat, prix_vente, unite_mesure_stock, point_de_vente_id, date_creation
) VALUES
(
    1,
    'CAR-60X60-BLC',
    'Carrelage Sol 60x60 Porcelaine Blanc Brillant',
    'Carrelage céramique premium haute brillance, résistant aux rayures pour séjour et hall',
    true,
    'REVETEMENT_SOL',
    '613000100101',
    1,
    'Carrelage Sol',
    '{"format": "60x60", "aspect": "Brillant", "epaisseur": "9mm", "matiere": "Porcelaine", "origine": "Espagne", "surface_boite_m2": 1.44, "pieces_boite": 4}'::jsonb,
    1400.00, 1666.00, 2200.00, 2618.00,
    1666.00, 2618.00, 'M2', 1, NOW()
),
(
    2,
    'CAR-60X60-GRS',
    'Carrelage Sol 60x60 Effet Béton Gris Mat Antidérapant',
    'Carrelage sol intérieur/extérieur effet béton contemporain, classe antidérapante R10',
    true,
    'REVETEMENT_SOL',
    '613000100102',
    1,
    'Carrelage Sol',
    '{"format": "60x60", "aspect": "Mat", "epaisseur": "9.5mm", "matiere": "Grès Cérame", "origine": "Italie", "surface_boite_m2": 1.44, "pieces_boite": 4}'::jsonb,
    1600.00, 1904.00, 2500.00, 2975.00,
    1904.00, 2975.00, 'M2', 1, NOW()
),
(
    3,
    'FAI-30X60-MAR',
    'Faïence Murale 30x60 Marbre Blanc Calacatta',
    'Revêtement mural rectifié effet marbre blanc veiné or et gris pour salle de bain',
    true,
    'REVETEMENT_MURAL',
    '613000100201',
    2,
    'Faïence Murale',
    '{"format": "30x60", "aspect": "Satiné", "epaisseur": "8mm", "matiere": "Pâte Blanche", "origine": "Espagne", "surface_boite_m2": 1.08, "pieces_boite": 6}'::jsonb,
    1100.00, 1309.00, 1750.00, 2082.50,
    1309.00, 2082.50, 'M2', 1, NOW()
),
(
    4,
    'FAI-30X60-BEI',
    'Faïence Murale 30x60 Travertin Beige Chaud',
    'Faïence murale chaleureuse aspect travertin naturel pour cuisine et salle de bain',
    true,
    'REVETEMENT_MURAL',
    '613000100202',
    2,
    'Faïence Murale',
    '{"format": "30x60", "aspect": "Mat", "epaisseur": "8mm", "matiere": "Pâte Rouge", "origine": "Algérie", "surface_boite_m2": 1.08, "pieces_boite": 6}'::jsonb,
    1050.00, 1249.50, 1700.00, 2023.00,
    1249.50, 2023.00, 'M2', 1, NOW()
),
(
    5,
    'GRES-80X80-POL',
    'Grès Cérame 80x80 Poli Miroir Noir Imperial',
    'Dalle grand format 80x80 poli miroir luxueux haute densité zéro porosité',
    true,
    'REVETEMENT_SOL',
    '613000100301',
    3,
    'Grès Cérame',
    '{"format": "80x80", "aspect": "Poli Miroir", "epaisseur": "10mm", "matiere": "Grès Cérame Pleine Masse", "origine": "Italie", "surface_boite_m2": 1.28, "pieces_boite": 2}'::jsonb,
    2400.00, 2856.00, 3800.00, 4522.00,
    2856.00, 4522.00, 'M2', 1, NOW()
),
(
    6,
    'MOS-30X30-BLE',
    'Mosaïque Céramique 30x30 Décor Bleu Piscine',
    'Trame de mosaïque sur filet nylon pour douche à l''italienne et hammam',
    true,
    'DECORATION',
    '613000100401',
    2,
    'Faïence Murale',
    '{"format": "30x30", "aspect": "Brillant", "epaisseur": "6mm", "matiere": "Céramique Émaillée", "origine": "Espagne", "surface_boite_m2": 0.90, "pieces_boite": 10}'::jsonb,
    1800.00, 2142.00, 2900.00, 3451.00,
    2142.00, 3451.00, 'PIECE', 1, NOW()
),
(
    7,
    'COL-CAR-25KG',
    'Mortier-Colle Carrelage Haute Adhérence C2TE Sac 25kg',
    'Colle déformable en poudre pour carrelage grand format et grès cérame intérieur/extérieur',
    true,
    'PRODUITS_POSE',
    '613000100501',
    5,
    'Colles & Mortiers',
    '{"poids_kg": 25, "norme": "C2TE", "consommation": "5kg/m2", "couleur": "Gris"}'::jsonb,
    650.00, 773.50, 950.00, 1130.50,
    773.50, 1130.50, 'KG', 1, NOW()
),
(
    8,
    'JNT-BLC-5KG',
    'Joint Carrelage Hydrofuge Blanc Seau 5kg',
    'Mortier de jointoiement fin hydrofuge anti-moisissures pour joints de 1 à 6 mm',
    true,
    'PRODUITS_POSE',
    '613000100502',
    5,
    'Colles & Mortiers',
    '{"poids_kg": 5, "norme": "CG2WA", "largeur_joint": "1-6mm", "couleur": "Blanc Pur"}'::jsonb,
    450.00, 535.50, 700.00, 833.00,
    535.50, 833.00, 'KG', 1, NOW()
)
ON CONFLICT (id) DO UPDATE SET
    designation = EXCLUDED.designation,
    description = EXCLUDED.description,
    prix_achat_ht = EXCLUDED.prix_achat_ht,
    prix_achat_ttc = EXCLUDED.prix_achat_ttc,
    prix_vente_ht = EXCLUDED.prix_vente_ht,
    prix_vente_ttc = EXCLUDED.prix_vente_ttc,
    prix_achat = EXCLUDED.prix_achat,
    prix_vente = EXCLUDED.prix_vente,
    attributes = EXCLUDED.attributes,
    actif = true;

-- ==============================================================================
-- 10. VARIANTES DE PRODUITS
-- ==============================================================================
INSERT INTO variantes_produit (id, produit_parent_id, sku, code_barre, nom_variante, taille, couleur, dimension, prix_achat, prix_vente, quantite_stock, actif, date_creation) VALUES
(1, 1, 'VAR-CAR60-BLC-BR', '6130002001', 'Blanc Brillant Extra', '60x60', 'Blanc', '60x60x0.9cm', 1666.00, 2618.00, 180.00, true, NOW()),
(2, 1, 'VAR-CAR60-BLC-MT', '6130002002', 'Blanc Satiné Doux', '60x60', 'Blanc Mat', '60x60x0.9cm', 1666.00, 2618.00, 120.00, true, NOW()),
(3, 2, 'VAR-CAR60-GRS-FON', '6130002003', 'Gris Anthracite Foncé', '60x60', 'Anthracite', '60x60x0.95cm', 1904.00, 2975.00, 140.00, true, NOW()),
(4, 2, 'VAR-CAR60-GRS-CLR', '6130002004', 'Gris Perle Clair', '60x60', 'Gris Clair', '60x60x0.95cm', 1904.00, 2975.00, 95.00, true, NOW()),
(5, 5, 'VAR-GRES80-NR-POL', '6130002005', 'Noir Absolu Poli Miroir', '80x80', 'Noir Miroir', '80x80x1.0cm', 2856.00, 4522.00, 110.00, true, NOW()),
(6, 5, 'VAR-GRES80-NR-SAT', '6130002006', 'Noir Satiné Dépoli', '80x80', 'Noir Satin', '80x80x1.0cm', 2856.00, 4522.00, 60.00, true, NOW())
ON CONFLICT (id) DO UPDATE SET
    sku = EXCLUDED.sku,
    nom_variante = EXCLUDED.nom_variante,
    quantite_stock = EXCLUDED.quantite_stock,
    actif = true;

-- ==============================================================================
-- 11. STOCKS
-- ==============================================================================
INSERT INTO stocks (id, produit_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 7),
(8, 8)
ON CONFLICT (id) DO UPDATE SET produit_id = EXCLUDED.produit_id;

-- ==============================================================================
-- 12. QUALITÉS DE STOCK (PREMIÈRE, DEUXIÈME, TROISIÈME QUALITÉ)
-- ==============================================================================
INSERT INTO stock_qualites (id, stock_id, produit_id, qualite_produit, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) VALUES
-- Produit 1 : Carrelage Sol 60x60 Blanc
(1, 1, 1, 'PREMIERE_QUALITE', 300.00, 25.00, 50.00, NOW()),
(2, 1, 1, 'DEUXIEME_QUALITE', 65.00, 0.00, 20.00, NOW()),

-- Produit 2 : Carrelage Sol 60x60 Gris
(3, 2, 2, 'PREMIERE_QUALITE', 235.00, 15.00, 40.00, NOW()),
(4, 2, 2, 'DEUXIEME_QUALITE', 40.00, 0.00, 15.00, NOW()),

-- Produit 3 : Faïence Murale Calacatta
(5, 3, 3, 'PREMIERE_QUALITE', 180.00, 30.00, 35.00, NOW()),
(6, 3, 3, 'DEUXIEME_QUALITE', 35.00, 0.00, 10.00, NOW()),

-- Produit 4 : Faïence Murale Travertin
(7, 4, 4, 'PREMIERE_QUALITE', 150.00, 10.00, 30.00, NOW()),
(8, 4, 4, 'DEUXIEME_QUALITE', 25.00, 0.00, 10.00, NOW()),

-- Produit 5 : Grès Cérame 80x80 Noir
(9, 5, 5, 'PREMIERE_QUALITE', 170.00, 20.00, 30.00, NOW()),
(10, 5, 5, 'DEUXIEME_QUALITE', 30.00, 0.00, 10.00, NOW()),

-- Produit 6 : Mosaïque Piscine
(11, 6, 6, 'PREMIERE_QUALITE', 95.00, 5.00, 20.00, NOW()),

-- Produit 7 : Colle 25kg
(12, 7, 7, 'PREMIERE_QUALITE', 250.00, 20.00, 50.00, NOW()),

-- Produit 8 : Joint 5kg
(13, 8, 8, 'PREMIERE_QUALITE', 120.00, 10.00, 25.00, NOW())
ON CONFLICT (id) DO UPDATE SET
    quantite_disponible = EXCLUDED.quantite_disponible,
    quantite_reservee = EXCLUDED.quantite_reservee,
    seuil_alerte = EXCLUDED.seuil_alerte,
    derniere_maj = NOW();

-- ==============================================================================
-- 13. LOTS FOURNISSEURS
-- ==============================================================================
INSERT INTO lots (
    id, numero_lot, produit_id, depot_id, qualite,
    quantite_initiale, quantite_disponible, quantite_reservee,
    date_fabrication, date_expiration, date_reception, prix_achat_unitaire,
    numero_livraison, fournisseur, observations, actif
) VALUES
(
    1, 'LOT-2026-001', 1, 1, 'PREMIERE_QUALITE',
    350.00, 300.00, 25.00,
    CURRENT_DATE - INTERVAL '40 days', NULL, NOW() - INTERVAL '30 days', 1400.00,
    'BL-EB-2026-101', 'Céramiques El Badr', 'Palettes sous film thermorétractable en parfait état', true
),
(
    2, 'LOT-2026-002', 1, 1, 'DEUXIEME_QUALITE',
    70.00, 65.00, 0.00,
    CURRENT_DATE - INTERVAL '40 days', NULL, NOW() - INTERVAL '30 days', 1100.00,
    'BL-EB-2026-101', 'Céramiques El Badr', 'Légères variations de teinte 2ème choix', true
),
(
    3, 'LOT-2026-003', 2, 1, 'PREMIERE_QUALITE',
    250.00, 235.00, 15.00,
    CURRENT_DATE - INTERVAL '35 days', NULL, NOW() - INTERVAL '25 days', 1600.00,
    'BL-ESP-2026-042', 'Eurl España Import Ceramic', 'Arrivage conteneur Valence', true
),
(
    4, 'LOT-2026-004', 3, 1, 'PREMIERE_QUALITE',
    210.00, 180.00, 30.00,
    CURRENT_DATE - INTERVAL '20 days', NULL, NOW() - INTERVAL '15 days', 1100.00,
    'BL-ESP-2026-048', 'Eurl España Import Ceramic', 'Calacatta 30x60 sélection Premium', true
),
(
    5, 'LOT-2026-005', 5, 2, 'PREMIERE_QUALITE',
    190.00, 170.00, 20.00,
    CURRENT_DATE - INTERVAL '15 days', NULL, NOW() - INTERVAL '10 days', 2400.00,
    'BL-SAC-2026-077', 'Société Algérienne de Céramique (SAC)', 'Poli miroir Série prestige', true
),
(
    6, 'LOT-2026-006', 7, 1, 'PREMIERE_QUALITE',
    300.00, 250.00, 20.00,
    CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '350 days', NOW() - INTERVAL '8 days', 650.00,
    'BL-COL-2026-015', 'Atlas Sanitaire & Bains', 'Colle grise C2TE sacs étanches', true
)
ON CONFLICT (id) DO UPDATE SET
    quantite_disponible = EXCLUDED.quantite_disponible,
    quantite_reservee = EXCLUDED.quantite_reservee,
    actif = true;

-- ==============================================================================
-- 14. MOUVEMENTS DE STOCK
-- ==============================================================================
INSERT INTO mouvements_stock (
    id, produit_id, depot_id, type_mouvement, quantite,
    quantite_avant, quantite_apres, reference_document, motif,
    date_mouvement, utilisateur, lot_id, qualite_produit, numero_lot_externe
) VALUES
(
    1, 1, 1, 'ENTREE_LIVRAISON', 350.00,
    0.00, 350.00, 'BL-EB-2026-101', 'Réception commande fournisseur Céramiques El Badr',
    NOW() - INTERVAL '30 days', 'magasinier', 1, 'PREMIERE_QUALITE', 'LOT-2026-001'
),
(
    2, 2, 1, 'ENTREE_LIVRAISON', 250.00,
    0.00, 250.00, 'BL-ESP-2026-042', 'Réception conteneur España Ceramic',
    NOW() - INTERVAL '25 days', 'magasinier', 3, 'PREMIERE_QUALITE', 'LOT-2026-003'
),
(
    3, 3, 1, 'ENTREE_LIVRAISON', 210.00,
    0.00, 210.00, 'BL-ESP-2026-048', 'Réception faïence Calacatta',
    NOW() - INTERVAL '15 days', 'magasinier', 4, 'PREMIERE_QUALITE', 'LOT-2026-004'
),
(
    4, 5, 2, 'ENTREE_LIVRAISON', 190.00,
    0.00, 190.00, 'BL-SAC-2026-077', 'Réception Grès Cérame SAC',
    NOW() - INTERVAL '10 days', 'magasinier', 5, 'PREMIERE_QUALITE', 'LOT-2026-005'
),
(
    5, 7, 1, 'ENTREE_LIVRAISON', 300.00,
    0.00, 300.00, 'BL-COL-2026-015', 'Réception sacs colle carrelage',
    NOW() - INTERVAL '8 days', 'magasinier', 6, 'PREMIERE_QUALITE', 'LOT-2026-006'
),
(
    6, 1, 1, 'SORTIE_VENTE', 25.00,
    350.00, 325.00, 'TKT-20260901-001', 'Vente comptoir client Ahmed Benali',
    NOW() - INTERVAL '1 day', 'G1500', 1, 'PREMIERE_QUALITE', 'LOT-2026-001'
),
(
    7, 7, 1, 'SORTIE_VENTE', 50.00,
    300.00, 250.00, 'TKT-20260901-002', 'Vente entreprise SARL Bâtiment Moderne',
    NOW() - INTERVAL '1 day', 'G1500', 6, 'PREMIERE_QUALITE', 'LOT-2026-006'
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 15. SESSIONS DE CAISSE (G1500)
-- ==============================================================================
INSERT INTO sessions_caisse (
    id, reference, date_ouverture, date_cloture, caissier_user_id,
    fond_de_caisse_initial, total_ventes, total_especes, total_carte, total_cheque, total_virement, total_credit,
    montant_theorique_cloture, montant_reel_cloture, ecart_caisse, statut, notes, point_de_vente_id
) VALUES
(
    1,
    'SESS-20260901-01',
    NOW() - INTERVAL '1 day' - INTERVAL '8 hours',
    NOW() - INTERVAL '1 day',
    3, -- G1500
    20000.00,
    145000.00,
    115000.00,
    30000.00,
    0.00,
    0.00,
    0.00,
    135000.00,
    135000.00,
    0.00,
    'CLOTUREE',
    'Session de la veille clôturée sans écart',
    1
),
(
    2,
    'SESS-20260902-01',
    NOW() - INTERVAL '3 hours',
    NULL,
    3, -- G1500
    20000.00,
    65450.00,
    65450.00,
    0.00,
    0.00,
    0.00,
    0.00,
    85450.00,
    85450.00,
    0.00,
    'OUVERTE',
    'Session de caisse active du jour',
    1
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut,
    total_ventes = EXCLUDED.total_ventes,
    montant_reel_cloture = EXCLUDED.montant_reel_cloture;

-- ==============================================================================
-- 16. VENTES & LIGNES DE VENTE
-- ==============================================================================
INSERT INTO ventes (
    id, numero_ticket, date_vente, client_id, vendeur_id,
    montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
    statut, montant_paye, montant_restant, notes
) VALUES
(
    1,
    'TKT-20260901-001',
    NOW() - INTERVAL '1 day',
    1, -- Ahmed Benali
    4, -- Karim Vendeur
    55000.00, 10450.00, 65450.00, 0.00, 65450.00,
    'VALIDEE', 65450.00, 0.00, 'Achat carrelage sol séjour'
),
(
    2,
    'TKT-20260901-002',
    NOW() - INTERVAL '1 day',
    3, -- SARL Bâtiment Moderne
    4, -- Karim Vendeur
    100000.00, 19000.00, 119000.00, 0.00, 119000.00,
    'VALIDEE', 119000.00, 0.00, 'Fourniture chantier Dar El Beida'
),
(
    3,
    'TKT-20260902-001',
    NOW() - INTERVAL '2 hours',
    2, -- Karim Khelil
    4, -- Karim Vendeur
    35000.00, 6650.00, 41650.00, 0.00, 41650.00,
    'EN_COURS', 41650.00, 0.00, 'Commande faïence Calacatta'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut,
    montant_final = EXCLUDED.montant_final;

INSERT INTO lignes_vente (
    id, vente_id, produit_id, designation, reference, quantite,
    surface_m2, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
    remise_pourcentage, remise_montant, notes
) VALUES
(
    1, 1, 1,
    'Carrelage Sol 60x60 Porcelaine Blanc Brillant', 'CAR-60X60-BLC', 25.000,
    25.00, 2200.00, 19.00, 55000.00, 10450.00, 65450.00,
    0.00, 0.00, '25 m² pour grand salon'
),
(
    2, 2, 2,
    'Carrelage Sol 60x60 Effet Béton Gris Mat Antidérapant', 'CAR-60X60-GRS', 20.000,
    20.00, 2500.00, 19.00, 50000.00, 9500.00, 59500.00,
    0.00, 0.00, 'Sol magasin'
),
(
    3, 2, 7,
    'Mortier-Colle Carrelage Haute Adhérence C2TE Sac 25kg', 'COL-CAR-25KG', 50.000,
    NULL, 950.00, 19.00, 47500.00, 9025.00, 56525.00,
    0.00, 0.00, '50 sacs pour pose'
),
(
    4, 3, 3,
    'Faïence Murale 30x60 Marbre Blanc Calacatta', 'FAI-30X60-MAR', 20.000,
    20.00, 1750.00, 19.00, 35000.00, 6650.00, 41650.00,
    0.00, 0.00, 'Revêtement mural salle d''eau'
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 17. PAIEMENTS CLIENTS
-- ==============================================================================
INSERT INTO paiements (
    id, numero_paiement, date_paiement, vente_id, facture_id, client_id,
    montant, mode_paiement, reference_paiement, nom_banque, numero_cheque,
    encaisse_par_user_id, notes, annule
) VALUES
(
    1, 'PAI-20260901-001', NOW() - INTERVAL '1 day', 1, NULL, 1,
    65450.00, 'ESPECES', 'RECU-ESP-001', NULL, NULL,
    3, 'Règlement intégral en espèces au comptoir', false
),
(
    2, 'PAI-20260901-002', NOW() - INTERVAL '1 day', 2, NULL, 3,
    119000.00, 'CHEQUE', 'CHQ-BNA-88912', 'Banque Nationale d''Algérie (BNA)', '0088912',
    3, 'Chèque remis à l''encaissement', false
),
(
    3, 'PAI-20260902-001', NOW() - INTERVAL '2 hours', 3, NULL, 2,
    41650.00, 'ESPECES', 'RECU-ESP-002', NULL, NULL,
    3, 'Paiement comptant caisse', false
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 18. FACTURES CLIENTS
-- ==============================================================================
INSERT INTO factures (
    id, numero_facture, date_facture, date_echeance, client_id, vente_id,
    emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
    statut, montant_paye, montant_restant, notes
) VALUES
(
    1,
    'FAC-2026-0001',
    CURRENT_DATE - INTERVAL '1 day',
    CURRENT_DATE + INTERVAL '29 days',
    3, -- SARL Bâtiment Moderne
    2,
    2, -- Gérant
    100000.00, 19000.00, 119000.00, 0.00, 119000.00,
    'PAYEE_TOTALEMENT', 119000.00, 0.00,
    'Facture officielle acquittée par chèque'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut,
    montant_final = EXCLUDED.montant_final;

INSERT INTO lignes_facture (
    id, facture_id, produit_id, designation, reference, quantite,
    surface_m2, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
    remise_pourcentage, remise_montant
) VALUES
(
    1, 1, 2,
    'Carrelage Sol 60x60 Effet Béton Gris Mat Antidérapant', 'CAR-60X60-GRS', 20.000,
    20.00, 2500.00, 19.00, 50000.00, 9500.00, 59500.00,
    0.00, 0.00
),
(
    2, 1, 7,
    'Mortier-Colle Carrelage Haute Adhérence C2TE Sac 25kg', 'COL-CAR-25KG', 50.000,
    NULL, 950.00, 19.00, 47500.00, 9025.00, 56525.00,
    0.00, 0.00
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 19. FACTURES D'ACHAT FOURNISSEURS & RÈGLEMENTS (NÉGOCE)
-- ==============================================================================
INSERT INTO factures_achat (
    id, numero_facture, date_facture, date_echeance, fournisseur_id,
    montant_ht, montant_tva, montant_ttc, statut, observations, point_de_vente_id
) VALUES
(
    1,
    'FAC-ACH-2026-001',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '5 days',
    1, -- Céramiques El Badr
    490000.00, 93100.00, 583100.00,
    'PAYEE_TOTALEMENT',
    'Facture approvisionnement palettes 60x60 Blanc Brillant',
    1
),
(
    2,
    'FAC-ACH-2026-002',
    NOW() - INTERVAL '15 days',
    NOW() + INTERVAL '15 days',
    2, -- Eurl España Import Ceramic
    231000.00, 43890.00, 274890.00,
    'EN_ATTENTE',
    'Arrivage faïences 30x60 Calacatta Espagne',
    1
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut,
    montant_ttc = EXCLUDED.montant_ttc;

INSERT INTO lignes_facture_achat (
    id, facture_achat_id, produit_id, quantite,
    prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc
) VALUES
(1, 1, 1, 350.000, 1400.00, 19.00, 490000.00, 93100.00, 583100.00),
(2, 2, 3, 210.000, 1100.00, 19.00, 231000.00, 43890.00, 274890.00)
ON CONFLICT (id) DO NOTHING;

-- Règlements fournisseurs (décaissements)
INSERT INTO reglements_fournisseur (
    id, numero_reglement, date_reglement, facture_achat_id,
    montant, mode_paiement, reference_paiement, notes, point_de_vente_id
) VALUES
(
    1,
    'REG-FRS-2026-001',
    NOW() - INTERVAL '10 days',
    1,
    583100.00,
    'VIREMENT',
    'VIR-BEA-2026-0981',
    'Virement bancaire Banque Extérieure d''Algérie (BEA)',
    1
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 20. DEVIS CLIENTS
-- ==============================================================================
INSERT INTO devis (
    id, numero_devis, date_devis, date_validite, client_id, cree_par_user_id,
    montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
    statut, notes, conditions_paiement, point_de_vente_id, date_creation
) VALUES
(
    1,
    'DEV-2026-0001',
    CURRENT_DATE - INTERVAL '5 days',
    CURRENT_DATE + INTERVAL '25 days',
    4, -- EURL Promobat Algérie
    4, -- Vendeur Karim
    380000.00, 72200.00, 452200.00, 0.00, 452200.00,
    'ENVOYE',
    'Devis fourniture carrelage pour 8 halls d''immeubles',
    'Acompte de 30% à la commande, solde à la livraison',
    1,
    NOW() - INTERVAL '5 days'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut,
    montant_final = EXCLUDED.montant_final;

INSERT INTO lignes_devis (
    id, devis_id, produit_id, quantite,
    prix_unitaire_ht, taux_tva, taux_remise, montant_ht, montant_tva, montant_ttc, description
) VALUES
(
    1, 1, 5, 100.000,
    3800.00, 19.00, 0.00, 380000.00, 72200.00, 452200.00,
    'Grès Cérame 80x80 Poli Miroir Noir Imperial'
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 21. COMMANDES FOURNISSEURS & COMMANDES CLIENTS
-- ==============================================================================
INSERT INTO commandes (
    id, numero_commande, fournisseur_id, statut, statut_livraison,
    date_commande, date_livraison_prevue, date_livraison_reelle, montant_total, observations
) VALUES
(
    1,
    'CMD-FRS-2026-001',
    1, -- Céramiques El Badr
    'VALIDEE',
    'LIVREE',
    NOW() - INTERVAL '35 days',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '30 days',
    583100.00,
    'Commande d''approvisionnement initiale livrée avec succès'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut;

INSERT INTO lignes_commande (
    id, commande_id, produit_id, quantite_commandee, quantite_livree, prix_unitaire, montant_ligne, qualite_produit
) VALUES
(1, 1, 1, 350, 350, 1666.00, 583100.00, 'PREMIERE_QUALITE')
ON CONFLICT (id) DO NOTHING;

-- Commandes client
INSERT INTO commandes_client (
    id, numero_commande, client_id, client_nom, client_telephone, client_email,
    adresse_livraison, statut, date_commande, date_livraison_prevue,
    montant_ht, montant_ttc, taux_tva, observations
) VALUES
(
    1,
    'CMD-CLI-2026-001',
    4, -- Promobat
    'EURL Promobat Algérie',
    '023 44 55 66',
    'direction@promobat.dz',
    'Chantier Résidence Les Jasmins, Bab Ezzouar',
    'CONFIRMEE',
    NOW() - INTERVAL '3 days',
    NOW() + INTERVAL '7 days',
    380000.00, 452200.00, 19.00,
    'Livraison par camion plateau avec déchargement grue'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut;

INSERT INTO lignes_commande_client (
    id, commande_client_id, produit_id, quantite, prix_unitaire, montant_ligne, observations
) VALUES
(1, 1, 5, 100.000, 4522.00, 452200.00, 'Livraison par camion plateau')
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 22. BONS DE LIVRAISON CLIENTS
-- ==============================================================================
INSERT INTO bons_livraison_client (
    id, numero_bl, date_bl, client_id, commande_client_id, statut, montant_total, observations, point_de_vente_id
) VALUES
(
    1,
    'BL-CLI-2026-001',
    NOW() - INTERVAL '1 day',
    3, -- SARL Bâtiment Moderne
    NULL,
    'LIVREE',
    119000.00,
    'Livraison directe sur chantier Dar El Beida',
    1
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut;

INSERT INTO lignes_bon_livraison_client (
    id, bon_livraison_client_id, produit_id, quantite_livree, depot_id, lot_id, prix_vente
) VALUES
(1, 1, 2, 20.000, 1, 3, 2975.00),
(2, 1, 7, 50.000, 1, 6, 1130.50)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 23. AVOIRS CLIENTS / FOURNISSEURS
-- ==============================================================================
INSERT INTO avoirs (
    id, numero_avoir, type_avoir, client_id, cree_par_user_id,
    date_avoir, montant_ht, montant_tva, montant_ttc,
    statut, motif, notes, point_de_vente_id, date_creation
) VALUES
(
    1,
    'AVR-2026-0001',
    'CLIENT',
    6, -- Yacine Meziane
    2, -- Manager
    CURRENT_DATE - INTERVAL '2 days',
    4400.00, 836.00, 5236.00,
    'VALIDE',
    'Retour 2 cartons carrelage suite surplus sur chantier',
    'Avoir déductible sur prochain achat de colle et joints',
    1,
    NOW() - INTERVAL '2 days'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut;

INSERT INTO lignes_avoir (
    id, avoir_id, produit_id, quantite, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc, remettre_en_stock, motif
) VALUES
(1, 1, 1, 2.000, 2200.00, 19.00, 4400.00, 836.00, 5236.00, true, 'Surplus chantier')
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 24. INVENTAIRES
-- ==============================================================================
INSERT INTO inventaires (
    id, reference, date_inventaire, depot_id, responsable_user_id,
    statut, total_ecart_positif, total_ecart_negatif, valeur_totale_ecart,
    notes, point_de_vente_id, date_creation, date_validation
) VALUES
(
    1,
    'INV-2026-08',
    CURRENT_DATE - INTERVAL '7 days',
    1,
    5, -- Magasinier Omar
    'VALIDE',
    0.00, 0.00, 0.00,
    'Inventaire physique mensuel de clôture d''août - Stock conforme',
    1,
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '6 days'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut;

INSERT INTO lignes_inventaire (
    id, inventaire_id, produit_id, qualite, quantite_theorique, quantite_reelle, ecart, prix_unitaire, valeur_ecart
) VALUES
(1, 1, 1, 'PREMIERE_QUALITE', 300.000, 300.000, 0.000, 2200.00, 0.00),
(2, 1, 2, 'PREMIERE_QUALITE', 235.000, 235.000, 0.000, 2500.00, 0.00)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 25. BORDEREAUX DE REMISE DE CHÈQUES (TRÉSORERIE)
-- ==============================================================================
INSERT INTO bordereaux_remise (
    id, numero_bordereau, date_remise, nom_banque, compte_bancaire,
    type_valeur, montant_total, nombre_valeurs, statut, notes, point_de_vente_id, date_creation
) VALUES
(
    1,
    'BRM-20260901-01',
    CURRENT_DATE - INTERVAL '1 day',
    'Banque Nationale d''Algérie (BNA)',
    '00100 01234567890 45',
    'CHEQUE',
    119000.00,
    1,
    'REMIS_EN_BANQUE',
    'Remise chèque client SARL Bâtiment Moderne',
    1,
    NOW() - INTERVAL '1 day'
)
ON CONFLICT (id) DO UPDATE SET
    statut = EXCLUDED.statut;

-- ==============================================================================
-- 25. PROFIL ENTREPRISE (MULTI-TENANT SAAS / IMPRESSIONS)
-- ==============================================================================
INSERT INTO entreprise_profiles (
    id, point_de_vente_id, nom_entreprise, activite, adresse, ville, code_postal,
    telephone, telephone_secondaire, email, site_web, registre_commerce,
    numero_identification_fiscale, numero_identification_statistique, article_imposition,
    compte_bancaire_rib, nom_banque, pied_page, devise, date_mise_a_jour
) VALUES
(
    1, 1, 'SARL COMPTOIR DU CARRELAGE & MATÉRIAUX',
    'Importation & Distribution Carrelages, Faïences et Matériaux de Finition',
    'Zone Industrielle Oued Smar, Lot N° 45', 'Alger', '16200',
    '021 50 12 34', '0550 12 34 56', 'contact@comptoir-carrelage.dz', 'www.comptoir-carrelage.dz',
    '16/00-0987654B16', '001609876543210', '0016098765432', '16098765432',
    '002 00012 1234567890 55', 'Banque Nationale d''Algérie (BNA)',
    'Garantie légale selon la réglementation en vigueur. Marchandise livrée sous réserve de propriété jusqu''au complet paiement. Merci pour votre confiance !',
    'DZD', NOW()
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 26. MISE À JOUR DE TOUTES LES SÉQUENCES POSTGRESQL (Auto-Increment)
-- ==============================================================================
DO $$
DECLARE
    r RECORD;
    max_id BIGINT;
    seq_name TEXT;
BEGIN
    FOR r IN (
        SELECT table_name
        FROM information_schema.columns
        WHERE table_schema = 'public' AND column_name = 'id'
    ) LOOP
        seq_name := pg_get_serial_sequence(r.table_name, 'id');
        IF seq_name IS NOT NULL THEN
            EXECUTE format('SELECT COALESCE(MAX(id), 0) + 1 FROM %I', r.table_name) INTO max_id;
            EXECUTE format('SELECT setval(%L, %s, false)', seq_name, max_id);
        END IF;
    END LOOP;
END $$;

COMMIT;

-- Résumé final
SELECT '🎉 Base de données remplie avec succès avec tous les mots de passe configurés à $2a$12$x9Cn5nX6pbMfDczHBnmzjOHu5v6Ddx/pqoSbsXrNunFjXxq/tckMO' AS resultat;
