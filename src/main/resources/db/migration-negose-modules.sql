-- Script SQL de migration pour ajouter les modules d'achat/vente avancés (Négoce)

-- 1. Table des factures d'achat fournisseurs
CREATE TABLE IF NOT EXISTS factures_achat (
    id BIGSERIAL PRIMARY KEY,
    numero_facture VARCHAR(100) UNIQUE NOT NULL,
    date_facture TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_echeance TIMESTAMP,
    fournisseur_id BIGINT NOT NULL,
    montant_ht DECIMAL(15,2) DEFAULT 0,
    montant_tva DECIMAL(15,2) DEFAULT 0,
    montant_ttc DECIMAL(15,2) DEFAULT 0,
    statut VARCHAR(50) DEFAULT 'IMPAYEE', -- IMPAYEE, PAYEE_PARTIELLEMENT, PAYEE, ANNULEE
    observations TEXT,
    point_de_vente_id BIGINT NOT NULL,
    CONSTRAINT fk_facture_achat_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id),
    CONSTRAINT fk_facture_achat_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id)
);

-- 2. Table des lignes de factures d'achat
CREATE TABLE IF NOT EXISTS lignes_facture_achat (
    id BIGSERIAL PRIMARY KEY,
    facture_achat_id BIGINT NOT NULL,
    produit_id BIGINT NOT NULL,
    quantite DECIMAL(15,3) NOT NULL,
    prix_unitaire_ht DECIMAL(15,2) NOT NULL,
    taux_tva DECIMAL(5,2) DEFAULT 19.00,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    CONSTRAINT fk_ligne_facture_achat FOREIGN KEY (facture_achat_id) REFERENCES factures_achat(id) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_facture_achat_produit FOREIGN KEY (produit_id) REFERENCES produits(id)
);

-- 3. Table des règlements fournisseurs (décaissements)
CREATE TABLE IF NOT EXISTS reglements_fournisseur (
    id BIGSERIAL PRIMARY KEY,
    numero_reglement VARCHAR(100) UNIQUE NOT NULL,
    date_reglement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    facture_achat_id BIGINT NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    mode_paiement VARCHAR(50) NOT NULL, -- ESPECES, VIREMENT, CHEQUE
    reference_paiement VARCHAR(255),
    notes TEXT,
    point_de_vente_id BIGINT NOT NULL,
    CONSTRAINT fk_reglement_fournisseur_facture FOREIGN KEY (facture_achat_id) REFERENCES factures_achat(id),
    CONSTRAINT fk_reglement_fournisseur_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id)
);

-- 4. Table des bons de livraison clients (expéditions)
CREATE TABLE IF NOT EXISTS bons_livraison_client (
    id BIGSERIAL PRIMARY KEY,
    numero_bl VARCHAR(100) UNIQUE NOT NULL,
    date_bl TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id BIGINT NOT NULL,
    commande_client_id BIGINT,
    statut VARCHAR(50) DEFAULT 'EN_PREPARATION', -- EN_PREPARATION, EXPEDIE, ANNULE
    montant_total DECIMAL(15,2) DEFAULT 0,
    observations TEXT,
    point_de_vente_id BIGINT NOT NULL,
    CONSTRAINT fk_bl_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_bl_commande_client FOREIGN KEY (commande_client_id) REFERENCES commandes_client(id),
    CONSTRAINT fk_bl_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id)
);

-- 5. Table des lignes de bons de livraison clients
CREATE TABLE IF NOT EXISTS lignes_bon_livraison_client (
    id BIGSERIAL PRIMARY KEY,
    bon_livraison_client_id BIGINT NOT NULL,
    produit_id BIGINT NOT NULL,
    quantite_livree DECIMAL(15,3) NOT NULL,
    depot_id BIGINT,
    lot_id BIGINT,
    prix_vente DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_ligne_bl FOREIGN KEY (bon_livraison_client_id) REFERENCES bons_livraison_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_bl_produit FOREIGN KEY (produit_id) REFERENCES produits(id),
    CONSTRAINT fk_ligne_bl_depot FOREIGN KEY (depot_id) REFERENCES depots(id),
    CONSTRAINT fk_ligne_bl_lot FOREIGN KEY (lot_id) REFERENCES lots(id)
);

-- Index pour optimiser les performances des nouveaux modules
CREATE INDEX IF NOT EXISTS idx_factures_achat_pdv ON factures_achat(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_factures_achat_fournisseur ON factures_achat(fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_reglements_fournisseur_pdv ON reglements_fournisseur(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_reglements_fournisseur_facture ON reglements_fournisseur(facture_achat_id);
CREATE INDEX IF NOT EXISTS idx_bl_client_pdv ON bons_livraison_client(point_de_vente_id);
CREATE INDEX IF NOT EXISTS idx_bl_client_commande ON bons_livraison_client(commande_client_id);
