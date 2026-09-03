-- ==============================================================================
-- SCRIPT DE SEED : RÔLES ET HABILITATIONS ERP
-- ==============================================================================

BEGIN;

-- 1. RÔLES ERP
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

-- 2. HABILITATIONS
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
(13, 'ADMIN_GESTION'),
-- Nouvelles habilitations ERP
(14, 'VENTE_VALIDER'),
(15, 'FACTURE_VOIR'),
(16, 'FACTURE_CREER'),
(17, 'FACTURE_VALIDER'),
(18, 'FACTURE_ANNULER'),
(19, 'PRIX_ACHAT_VOIR'),
(20, 'PRIX_ACHAT_MODIFIER'),
(21, 'REMISE_VALIDER'),
(22, 'STOCK_LIVRER'),
(23, 'CAISSE_ENCAISSER'),
(24, 'PAIEMENT_ANNULER'),
(25, 'MARGES_VOIR'),
(26, 'DASHBOARD_VOIR'),
(27, 'ANOMALIES_VOIR'),
(28, 'COMPTABILITE_VOIR'),
(29, 'COMMERCIAL_VOIR')
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom;

-- 3. ASSOCIATIONS RÔLES <-> HABILITATIONS
CREATE TABLE IF NOT EXISTS roles_habilitations (
    role_id BIGINT NOT NULL,
    habilitation_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, habilitation_id)
);

-- ROLE_ADMIN : Toutes habilitations
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT 1, h.id FROM habilitations h
ON CONFLICT DO NOTHING;

-- ROLE_GESTIONNAIRE : Accès global de direction
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT 6, h.id FROM habilitations h
ON CONFLICT DO NOTHING;

-- ROLE_POINT_DE_VENTE_MANAGER
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10),
(2, 11), (2, 12), (2, 14), (2, 15), (2, 16), (2, 17), (2, 21), (2, 22), (2, 23),
(2, 25), (2, 26), (2, 27), (2, 28), (2, 29)
ON CONFLICT DO NOTHING;

-- ROLE_RESPONSABLE_COMMERCIAL : Ventes, Facturation, Clients, Remises, Dashboard, Commerciaux
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(7, 1), (7, 2), (7, 4), (7, 6), (7, 12), (7, 14), (7, 15), (7, 16), (7, 17),
(7, 21), (7, 25), (7, 26), (7, 27), (7, 29)
ON CONFLICT DO NOTHING;

-- ROLE_COMMERCIAL : Clients, Devis, Commandes/Ventes, ses stats
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(8, 1), (8, 2), (8, 4), (8, 6), (8, 15), (8, 16)
ON CONFLICT DO NOTHING;

-- ROLE_VENDEUR : Comptoir POS
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(4, 1), (4, 2), (4, 4), (4, 6), (4, 14), (4, 15), (4, 23)
ON CONFLICT DO NOTHING;

-- ROLE_CAISSIER : Encaissements, Caisse
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(3, 1), (3, 2), (3, 7), (3, 8), (3, 15), (3, 23)
ON CONFLICT DO NOTHING;

-- ROLE_MAGASINIER : Stock, Réceptions, Livraisons
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(5, 4), (5, 5), (5, 9), (5, 22)
ON CONFLICT DO NOTHING;

-- ROLE_COMPTABLE : Factures, Règlements, Marges, Dashboard financier
INSERT INTO roles_habilitations (role_id, habilitation_id) VALUES
(9, 2), (9, 11), (9, 12), (9, 15), (9, 17), (9, 19), (9, 25), (9, 26), (9, 27), (9, 28)
ON CONFLICT DO NOTHING;

COMMIT;
