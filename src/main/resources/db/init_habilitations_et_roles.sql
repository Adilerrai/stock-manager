-- =============================================================================
-- ERP TADBEER / STOCK-MANAGER : RÉFÉRENTIEL COMPLET DES RÔLES & HABILITATIONS
-- =============================================================================
-- Ce script est 100% idempotent : peut être exécuté plusieurs fois sans conflit.
-- Compatible PostgreSQL (avec ON CONFLICT).

-- -----------------------------------------------------------------------------
-- 1. Index Uniques pour garantir l'intégrité et l'idempotence
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uk_roles_nom ON roles(nom);
CREATE UNIQUE INDEX IF NOT EXISTS uk_habilitations_nom ON habilitations(nom);

-- -----------------------------------------------------------------------------
-- 2. Insertion des 10 Rôles Standards ERP
-- -----------------------------------------------------------------------------
INSERT INTO roles (nom) VALUES
('ROLE_SUPERADMIN'),
('ROLE_ADMIN'),
('ROLE_POINT_DE_VENTE_MANAGER'),
('ROLE_RESPONSABLE_COMMERCIAL'),
('ROLE_RESPONSABLE_ACHAT'),
('ROLE_COMMERCIAL'),
('ROLE_VENDEUR'),
('ROLE_CAISSIER'),
('ROLE_MAGASINIER'),
('ROLE_COMPTABLE'),
('ROLE_CLIENT')
ON CONFLICT (nom) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3. Insertion du Catalogue Exhaustif des Habilitations Métier
-- -----------------------------------------------------------------------------
INSERT INTO habilitations (nom) VALUES
-- 3.1 Clients & Tiers
('CLIENT_READ'),
('CLIENT_CREATE'),
('CLIENT_UPDATE'),
('CLIENT_DELETE'),
('CLIENT_COMPTE_CREATE'),

-- 3.2 Ventes, Devis, Commandes & BL
('DEVIS_READ'),
('DEVIS_CREATE'),
('COMMANDE_READ'),
('COMMANDE_CREATE'),
('COMMANDE_VALIDATE'),
('COMMANDE_CLIENT_READ'),
('COMMANDE_CLIENT_CREATE'),
('COMMANDE_CLIENT_VALIDER'),
('BL_CLIENT_READ'),
('BL_CLIENT_CREATE'),
('VENTE_READ'),
('VENTE_CREATE'),
('VENTE_DELETE'),

-- 3.3 Facturation & Avoirs Ventes
('FACTURE_READ'),
('FACTURE_CREATE'),
('FACTURE_VALIDER'),
('FACTURE_ANNULER'),
('FACTURE_VENTE_READ'),
('FACTURE_VENTE_CREATE'),
('FACTURE_VENTE_VALIDER'),
('AVOIR_VENTE_CREATE'),

-- 3.4 Fournisseurs & Approvisionnements
('FOURNISSEUR_READ'),
('FOURNISSEUR_CREATE'),
('FOURNISSEUR_UPDATE'),
('DA_CREATE'),
('DA_APPROUVER'),
('DDP_GESTION'),
('COMMANDE_FOURNISSEUR_CREATE'),
('RECEPTION_FOURNISSEUR_VALIDER'),

-- 3.5 Facturation Achats & Avoirs Fournisseurs
('FACTURE_ACHAT_READ'),
('FACTURE_ACHAT_CREATE'),
('AVOIR_ACHAT_CREATE'),

-- 3.6 Catalogue Produits & Stocks
('PRODUIT_READ'),
('PRODUIT_CREATE'),
('PRODUIT_UPDATE'),
('PRODUIT_DELETE'),
('COUT_REVIENT_READ'),
('STOCK_READ'),
('STOCK_CREATE'),
('STOCK_TRANSFERT'),
('STOCK_INVENTAIRE'),
('STOCK_MOUVEMENT_CREATE'),

-- 3.7 Caisse, Règlements & Trésorerie
('SESSION_CAISSE_GERER'),
('PAIEMENT_READ'),
('PAIEMENT_CREATE'),
('ENCAISSEMENT_CREATE'),
('DECAISSEMENT_CREATE'),
('CHEQUE_EFFET_GERER'),
('TRESORERIE_READ'),
('TRESORERIE_MOUVEMENT'),
('TRESORERIE_RAPPROCHEMENT'),

-- 3.8 Comptabilité & Fiscalité
('COMPTA_READ'),
('COMPTA_WRITE'),
('COMPTA_ECRITURE'),
('COMPTA_LETTRAGE'),
('COMPTA_CLOTURE'),
('CLOTURE_EXERCICE'),
('FISCALITE_LIASSE'),

-- 3.9 Rapports, Marges & Pilotage (BI)
('DASHBOARD_VOIR'),
('RAPPORT_READ'),
('RAPPORT_VOIR'),
('RAPPORT_EXPORT'),
('MARGE_READ'),

-- 3.10 Gestion Équipe & Administration Système
('USER_READ'),
('USER_CREATE'),
('USER_UPDATE'),
('USER_DELETE'),
('USER_MANAGE'),
('ADMIN_ENTREPRISE'),
('ADMIN_ROLES'),
('ADMIN_HABILITATIONS'),
('ROLE_MANAGE'),
('ENTREPRISE_CONFIG'),
('AUDIT_LOG_READ'),

-- 3.11 Espace Client Portail
('ESPACE_CLIENT_ACCESS')
ON CONFLICT (nom) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4. Assignation des Habilitations aux Rôles (Matrice Métier)
-- -----------------------------------------------------------------------------

-- 4.1 ROLE_ADMIN : Reçoit TOUTES les habilitations métier de son entreprise
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_ADMIN'
  AND h.nom != 'ESPACE_CLIENT_ACCESS'
ON CONFLICT DO NOTHING;

-- 4.2 ROLE_POINT_DE_VENTE_MANAGER : Gestionnaire d'agence / Responsable de boutique
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_POINT_DE_VENTE_MANAGER'
  AND h.nom IN (
    'CLIENT_READ', 'CLIENT_CREATE', 'CLIENT_UPDATE', 'CLIENT_COMPTE_CREATE',
    'DEVIS_READ', 'DEVIS_CREATE',
    'COMMANDE_READ', 'COMMANDE_CREATE', 'COMMANDE_VALIDATE',
    'COMMANDE_CLIENT_READ', 'COMMANDE_CLIENT_CREATE', 'COMMANDE_CLIENT_VALIDER',
    'BL_CLIENT_READ', 'BL_CLIENT_CREATE',
    'VENTE_READ', 'VENTE_CREATE',
    'FACTURE_READ', 'FACTURE_CREATE', 'FACTURE_VALIDER', 'FACTURE_VENTE_READ', 'FACTURE_VENTE_CREATE', 'FACTURE_VENTE_VALIDER',
    'AVOIR_VENTE_CREATE',
    'FOURNISSEUR_READ',
    'PRODUIT_READ', 'PRODUIT_CREATE', 'PRODUIT_UPDATE',
    'STOCK_READ', 'STOCK_CREATE', 'STOCK_TRANSFERT', 'STOCK_INVENTAIRE', 'STOCK_MOUVEMENT_CREATE',
    'SESSION_CAISSE_GERER', 'PAIEMENT_READ', 'PAIEMENT_CREATE', 'ENCAISSEMENT_CREATE', 'CHEQUE_EFFET_GERER',
    'TRESORERIE_READ',
    'DASHBOARD_VOIR', 'RAPPORT_READ', 'RAPPORT_VOIR', 'RAPPORT_EXPORT',
    'USER_READ', 'AUDIT_LOG_READ'
  )
ON CONFLICT DO NOTHING;

-- 4.3 ROLE_RESPONSABLE_COMMERCIAL
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_RESPONSABLE_COMMERCIAL'
  AND h.nom IN (
    'CLIENT_READ', 'CLIENT_CREATE', 'CLIENT_UPDATE', 'CLIENT_COMPTE_CREATE',
    'DEVIS_READ', 'DEVIS_CREATE',
    'COMMANDE_READ', 'COMMANDE_CREATE', 'COMMANDE_VALIDATE',
    'COMMANDE_CLIENT_READ', 'COMMANDE_CLIENT_CREATE', 'COMMANDE_CLIENT_VALIDER',
    'BL_CLIENT_READ',
    'VENTE_READ', 'VENTE_CREATE',
    'FACTURE_READ', 'FACTURE_CREATE', 'FACTURE_VENTE_READ', 'FACTURE_VENTE_CREATE',
    'AVOIR_VENTE_CREATE',
    'PRODUIT_READ',
    'STOCK_READ',
    'PAIEMENT_READ',
    'DASHBOARD_VOIR', 'RAPPORT_READ', 'RAPPORT_VOIR', 'RAPPORT_EXPORT', 'MARGE_READ'
  )
ON CONFLICT DO NOTHING;

-- 4.4 ROLE_COMMERCIAL & ROLE_VENDEUR (Vente comptoir et terrain)
-- Note : Accès au devis, commandes, BL et création de fiches clients, SANS accès aux coûts de revient ni suppression.
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom IN ('ROLE_COMMERCIAL', 'ROLE_VENDEUR')
  AND h.nom IN (
    'CLIENT_READ', 'CLIENT_CREATE',
    'DEVIS_READ', 'DEVIS_CREATE',
    'COMMANDE_READ', 'COMMANDE_CREATE',
    'COMMANDE_CLIENT_READ', 'COMMANDE_CLIENT_CREATE',
    'BL_CLIENT_READ', 'BL_CLIENT_CREATE',
    'VENTE_READ', 'VENTE_CREATE',
    'FACTURE_READ', 'FACTURE_VENTE_READ',
    'PRODUIT_READ',
    'STOCK_READ',
    'PAIEMENT_READ', 'PAIEMENT_CREATE', 'ENCAISSEMENT_CREATE',
    'DASHBOARD_VOIR'
  )
ON CONFLICT DO NOTHING;

-- 4.5 ROLE_CAISSIER (Encaissement et sessions de caisse)
-- Note : Interdit de créer des fiches clients ou modifier des prix.
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_CAISSIER'
  AND h.nom IN (
    'CLIENT_READ',
    'VENTE_READ', 'VENTE_CREATE',
    'COMMANDE_READ',
    'FACTURE_READ', 'FACTURE_VENTE_READ',
    'PRODUIT_READ',
    'STOCK_READ',
    'SESSION_CAISSE_GERER',
    'PAIEMENT_READ', 'PAIEMENT_CREATE', 'ENCAISSEMENT_CREATE',
    'CHEQUE_EFFET_GERER',
    'DASHBOARD_VOIR'
  )
ON CONFLICT DO NOTHING;

-- 4.6 ROLE_RESPONSABLE_ACHAT
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_RESPONSABLE_ACHAT'
  AND h.nom IN (
    'FOURNISSEUR_READ', 'FOURNISSEUR_CREATE', 'FOURNISSEUR_UPDATE',
    'DA_CREATE', 'DA_APPROUVER', 'DDP_GESTION',
    'COMMANDE_READ', 'COMMANDE_CREATE', 'COMMANDE_FOURNISSEUR_CREATE',
    'RECEPTION_FOURNISSEUR_VALIDER',
    'FACTURE_READ', 'FACTURE_CREATE', 'FACTURE_ACHAT_READ', 'FACTURE_ACHAT_CREATE',
    'AVOIR_ACHAT_CREATE',
    'PRODUIT_READ', 'PRODUIT_CREATE', 'PRODUIT_UPDATE', 'COUT_REVIENT_READ',
    'STOCK_READ', 'STOCK_CREATE',
    'PAIEMENT_READ',
    'DASHBOARD_VOIR', 'RAPPORT_READ', 'RAPPORT_VOIR', 'RAPPORT_EXPORT'
  )
ON CONFLICT DO NOTHING;

-- 4.7 ROLE_MAGASINIER (Logistique, Dépôts & Réceptions)
-- Note : Aucun accès aux factures ni aux prix.
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_MAGASINIER'
  AND h.nom IN (
    'PRODUIT_READ',
    'STOCK_READ', 'STOCK_CREATE', 'STOCK_TRANSFERT', 'STOCK_INVENTAIRE', 'STOCK_MOUVEMENT_CREATE',
    'COMMANDE_READ', 'COMMANDE_CLIENT_READ',
    'BL_CLIENT_READ', 'BL_CLIENT_CREATE',
    'RECEPTION_FOURNISSEUR_VALIDER',
    'DASHBOARD_VOIR'
  )
ON CONFLICT DO NOTHING;

-- 4.8 ROLE_COMPTABLE
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_COMPTABLE'
  AND h.nom IN (
    'CLIENT_READ', 'FOURNISSEUR_READ',
    'FACTURE_READ', 'FACTURE_CREATE', 'FACTURE_VALIDER', 'FACTURE_ANNULER',
    'FACTURE_VENTE_READ', 'FACTURE_VENTE_VALIDER',
    'FACTURE_ACHAT_READ', 'FACTURE_ACHAT_CREATE',
    'AVOIR_VENTE_CREATE', 'AVOIR_ACHAT_CREATE',
    'PAIEMENT_READ', 'PAIEMENT_CREATE', 'ENCAISSEMENT_CREATE', 'DECAISSEMENT_CREATE',
    'CHEQUE_EFFET_GERER',
    'TRESORERIE_READ', 'TRESORERIE_MOUVEMENT', 'TRESORERIE_RAPPROCHEMENT',
    'COMPTA_READ', 'COMPTA_WRITE', 'COMPTA_ECRITURE', 'COMPTA_LETTRAGE', 'COMPTA_CLOTURE', 'CLOTURE_EXERCICE',
    'FISCALITE_LIASSE',
    'PRODUIT_READ', 'STOCK_READ', 'COUT_REVIENT_READ', 'MARGE_READ',
    'DASHBOARD_VOIR', 'RAPPORT_READ', 'RAPPORT_VOIR', 'RAPPORT_EXPORT',
    'AUDIT_LOG_READ'
  )
ON CONFLICT DO NOTHING;

-- 4.9 ROLE_CLIENT (Portail Espace Client dédié)
INSERT INTO roles_habilitations (role_id, habilitation_id)
SELECT r.id, h.id
FROM roles r, habilitations h
WHERE r.nom = 'ROLE_CLIENT'
  AND h.nom = 'ESPACE_CLIENT_ACCESS'
ON CONFLICT DO NOTHING;

