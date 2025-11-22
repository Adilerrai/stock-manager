-- Suppression de la contrainte existante si elle existe
ALTER TABLE IF EXISTS stock_qualite DROP CONSTRAINT IF EXISTS uk_stock_qualite;

-- Suppression des tables si elles existent
DROP TABLE IF EXISTS stock_qualites CASCADE;
DROP TABLE IF EXISTS stock_qualite CASCADE;

-- Création de la table stock_qualites
CREATE TABLE stock_qualites (
    id BIGSERIAL PRIMARY KEY,
    produit_id BIGINT NOT NULL,
    depot_id BIGINT NOT NULL,
    qualite_produit VARCHAR(50) NOT NULL,
    quantite_disponible DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantite_reservee DECIMAL(10,2) DEFAULT 0.00,
    seuil_alerte DECIMAL(10,2),
    derniere_maj TIMESTAMP,
    CONSTRAINT fk_stock_qualite_produit FOREIGN KEY (produit_id) REFERENCES produits(id),
    CONSTRAINT fk_stock_qualite_depot FOREIGN KEY (depot_id) REFERENCES depots(id),
    CONSTRAINT uk_stock_qualite UNIQUE (produit_id, depot_id, qualite_produit)
);

-- Création d'un point de vente par défaut (seulement s'il n'existe pas)
INSERT INTO point_de_vente (nom_point_de_vente, password, tenant_id) 
SELECT 'Point de Vente Principal', '$2a$10$yfIHYCHVqyYQZMicjQMBWO.1Z7Zt2h9Mn9F5ocYzwWCnPvLeko3Aq', 1
WHERE NOT EXISTS (SELECT 1 FROM point_de_vente WHERE tenant_id = 1);

-- Insertion d'un seul dépôt (seulement s'il n'existe pas)
INSERT INTO depots (nom, description, adresse, point_de_vente_id, date_creation, actif) 
SELECT 'Dépôt Principal', 'Dépôt unique de stockage', 'Zone de stockage principale', 1, NOW(), true
WHERE NOT EXISTS (SELECT 1 FROM depots WHERE nom = 'Dépôt Principal' AND point_de_vente_id = 1);

-- Insertion de produits céramiques (seulement s'ils n'existent pas)
INSERT INTO produits (reference, description, prix_achat, prix_vente, unite_mesure_stock, point_de_vente_id, date_creation) 
SELECT * FROM (VALUES
    ('CERAM001', 'Carrelage céramique premium 60x60', 25.00, 45.00, 'M2', 1, NOW()),
    ('CERAM002', 'Faïence murale décorative 30x60', 15.00, 28.00, 'M2', 1, NOW()),
    ('CERAM003', 'Grès cérame antidérapant 45x45', 30.00, 55.00, 'M2', 1, NOW()),
    ('CERAM004', 'Mosaïque en céramique 30x30', 40.00, 75.00, 'M2', 1, NOW()),
    ('CERAM005', 'Carrelage extérieur résistant', 35.00, 65.00, 'M2', 1, NOW())
) AS v(reference, description, prix_achat, prix_vente, unite_mesure_stock, point_de_vente_id, date_creation)
WHERE NOT EXISTS (SELECT 1 FROM produits WHERE reference = v.reference);

-- Création des stocks principaux (seulement s'ils n'existent pas)
INSERT INTO stocks (produit_id, depot_id) 
SELECT p.id, d.id
FROM produits p, depots d 
WHERE p.point_de_vente_id = 1 AND d.point_de_vente_id = 1
AND NOT EXISTS (SELECT 1 FROM stocks WHERE produit_id = p.id AND depot_id = d.id);

-- Insertion des stocks par qualité
INSERT INTO stock_qualites (produit_id, depot_id, qualite_produit, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) 
SELECT p.id, d.id, 'PREMIERE_QUALITE', 100.00, 0.00, 20.00, NOW()
FROM produits p, depots d 
WHERE p.point_de_vente_id = 1 AND d.point_de_vente_id = 1
AND NOT EXISTS (SELECT 1 FROM stock_qualites WHERE produit_id = p.id AND depot_id = d.id AND qualite_produit = 'PREMIERE_QUALITE');

INSERT INTO stock_qualites (produit_id, depot_id, qualite_produit, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) 
SELECT p.id, d.id, 'DEUXIEME_QUALITE', 50.00, 0.00, 10.00, NOW()
FROM produits p, depots d 
WHERE p.point_de_vente_id = 1 AND d.point_de_vente_id = 1
AND NOT EXISTS (SELECT 1 FROM stock_qualites WHERE produit_id = p.id AND depot_id = d.id AND qualite_produit = 'DEUXIEME_QUALITE');

-- Insertion d'un rôle admin par défaut (seulement s'il n'existe pas)
INSERT INTO roles (nom) 
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nom = 'ROLE_ADMIN');

-- Insertion d'un utilisateur admin par défaut (seulement s'il n'existe pas)
INSERT INTO users (email, password, nom_complet, point_de_vente_id, role_id, account_non_expired, account_non_locked, credentials_non_expired, enabled) 
SELECT 'admin@ceramique.ma', '$2a$10$yfIHYCHVqyYQZMicjQMBWO.1Z7Zt2h9Mn9F5ocYzwWCnPvLeko3Aq', 'Administrateur Principal', 1, r.id, true, true, true, true
FROM roles r
WHERE r.nom = 'ROLE_ADMIN'
AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@ceramique.ma');