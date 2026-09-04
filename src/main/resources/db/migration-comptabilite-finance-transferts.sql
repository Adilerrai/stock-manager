-- =========================================================================
-- MIGRATION SCRIPT: COMPTABILITÉ PCGM, PORTEFEUILLE CHÈQUES & EFFETS, 
--                   TRANSFERTS INTER-DÉPÔTS, SYSTÈME DE NOTIFICATIONS
-- =========================================================================

-- 1. MODULE COMPTABILITÉ GÉNÉRALE

CREATE TABLE IF NOT EXISTS comptes_comptables (
    id BIGSERIAL PRIMARY KEY,
    numero_compte VARCHAR(20) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    classe INT NOT NULL,
    sens_par_defaut VARCHAR(10) NOT NULL DEFAULT 'DEBIT',
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    point_de_vente_id BIGINT NOT NULL DEFAULT 1,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_compte_numero_tenant UNIQUE (numero_compte, point_de_vente_id)
);

CREATE TABLE IF NOT EXISTS journaux_comptables (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    type_journal VARCHAR(30) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    point_de_vente_id BIGINT NOT NULL DEFAULT 1,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_journal_code_tenant UNIQUE (code, point_de_vente_id)
);

CREATE TABLE IF NOT EXISTS ecritures_comptables (
    id BIGSERIAL PRIMARY KEY,
    numero_piece VARCHAR(50) NOT NULL,
    date_ecriture DATE NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    reference_piece VARCHAR(100),
    validee BOOLEAN NOT NULL DEFAULT FALSE,
    total_debit NUMERIC(15, 2) DEFAULT 0.00,
    total_credit NUMERIC(15, 2) DEFAULT 0.00,
    journal_id BIGINT NOT NULL REFERENCES journaux_comptables(id),
    point_de_vente_id BIGINT NOT NULL DEFAULT 1,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ecriture_piece_tenant UNIQUE (numero_piece, point_de_vente_id)
);

CREATE TABLE IF NOT EXISTS lignes_ecriture (
    id BIGSERIAL PRIMARY KEY,
    ecriture_id BIGINT NOT NULL REFERENCES ecritures_comptables(id) ON DELETE CASCADE,
    compte_id BIGINT NOT NULL REFERENCES comptes_comptables(id),
    debit NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    credit NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    libelle_ligne VARCHAR(255),
    reference_ligne VARCHAR(100),
    lettrage VARCHAR(10),
    point_de_vente_id BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_ligne_ecriture_compte ON lignes_ecriture(compte_id);
CREATE INDEX IF NOT EXISTS idx_ligne_ecriture_ecriture ON lignes_ecriture(ecriture_id);
CREATE INDEX IF NOT EXISTS idx_ecriture_date ON ecritures_comptables(date_ecriture);
CREATE INDEX IF NOT EXISTS idx_ecriture_ref ON ecritures_comptables(reference_piece);


-- 2. MODULE FINANCE - PORTEFEUILLE CHÈQUES & EFFETS DE COMMERCE

CREATE TABLE IF NOT EXISTS cheques_effets (
    id BIGSERIAL PRIMARY KEY,
    numero_piece VARCHAR(50) NOT NULL,
    type_effet VARCHAR(30) NOT NULL DEFAULT 'CHEQUE',
    sens VARCHAR(30) NOT NULL DEFAULT 'ENCAISSEMENT_CLIENT',
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_PORTEFEUILLE',
    montant NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    date_emission DATE DEFAULT CURRENT_DATE,
    date_echeance DATE,
    date_remise DATE,
    date_encaissement DATE,
    banque_emettrice VARCHAR(100),
    tireur VARCHAR(150),
    beneficiaire VARCHAR(150),
    compte_bancaire_depot VARCHAR(100),
    reference_paiement VARCHAR(100),
    client_id BIGINT REFERENCES clients(id),
    fournisseur_id BIGINT REFERENCES fournisseurs(id),
    bordereau_remise_id BIGINT REFERENCES bordereaux_remise(id),
    motif_rejet VARCHAR(255),
    notes TEXT,
    point_de_vente_id BIGINT NOT NULL DEFAULT 1,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cheque_echeance ON cheques_effets(date_echeance);
CREATE INDEX IF NOT EXISTS idx_cheque_statut ON cheques_effets(statut);
CREATE INDEX IF NOT EXISTS idx_cheque_sens ON cheques_effets(sens);


-- 3. MODULE STOCK - TRANSFERTS INTER-DÉPÔTS

CREATE TABLE IF NOT EXISTS transferts_stock (
    id BIGSERIAL PRIMARY KEY,
    numero_transfert VARCHAR(50) NOT NULL,
    depot_source_id BIGINT NOT NULL REFERENCES depots(id),
    depot_destination_id BIGINT NOT NULL REFERENCES depots(id),
    date_transfert DATE NOT NULL DEFAULT CURRENT_DATE,
    date_reception DATE,
    statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
    motif VARCHAR(255),
    cree_par_user_id BIGINT REFERENCES users(id),
    valide_par_user_id BIGINT REFERENCES users(id),
    point_de_vente_id BIGINT NOT NULL DEFAULT 1,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_transfert_numero_tenant UNIQUE (numero_transfert, point_de_vente_id)
);

CREATE TABLE IF NOT EXISTS lignes_transfert_stock (
    id BIGSERIAL PRIMARY KEY,
    transfert_id BIGINT NOT NULL REFERENCES transferts_stock(id) ON DELETE CASCADE,
    produit_id BIGINT NOT NULL REFERENCES produits(id),
    quantite NUMERIC(12, 2) NOT NULL DEFAULT 1.00,
    notes VARCHAR(255),
    point_de_vente_id BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_transfert_source ON transferts_stock(depot_source_id);
CREATE INDEX IF NOT EXISTS idx_transfert_destination ON transferts_stock(depot_destination_id);


-- 4. MODULE SYSTÈME CENTRALISÉ DE NOTIFICATIONS

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEME',
    severite VARCHAR(20) NOT NULL DEFAULT 'INFO',
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    lien_action VARCHAR(255),
    point_de_vente_id BIGINT NOT NULL DEFAULT 1,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_tenant_lu ON notifications(point_de_vente_id, lu);
CREATE INDEX IF NOT EXISTS idx_notification_date ON notifications(date_creation DESC);


-- 5. INITIALISATION DES JOURNAUX PAR DÉFAUT (Pour tenant 1 et tenant 2)

INSERT INTO journaux_comptables (code, libelle, type_journal, actif, point_de_vente_id)
VALUES 
    ('VT', 'Journal des Ventes', 'VENTES', true, 1),
    ('AC', 'Journal des Achats', 'ACHATS', true, 1),
    ('BQ', 'Journal de Banque', 'BANQUE', true, 1),
    ('CA', 'Journal de Caisse', 'CAISSE', true, 1),
    ('OD', 'Journal des Opérations Diverses', 'OPERATIONS_DIVERSES', true, 1)
ON CONFLICT (code, point_de_vente_id) DO NOTHING;

INSERT INTO journaux_comptables (code, libelle, type_journal, actif, point_de_vente_id)
VALUES 
    ('VT', 'Journal des Ventes', 'VENTES', true, 2),
    ('AC', 'Journal des Achats', 'ACHATS', true, 2),
    ('BQ', 'Journal de Banque', 'BANQUE', true, 2),
    ('CA', 'Journal de Caisse', 'CAISSE', true, 2),
    ('OD', 'Journal des Opérations Diverses', 'OPERATIONS_DIVERSES', true, 2)
ON CONFLICT (code, point_de_vente_id) DO NOTHING;


-- 6. INITIALISATION DU PLAN COMPTABLE GÉNÉRAL MAROCAIN / MAGHRÉBIN (PCGM)

INSERT INTO comptes_comptables (numero_compte, libelle, classe, sens_par_defaut, actif, point_de_vente_id)
VALUES
    -- Classe 1 : Financement permanent
    ('11110000', 'Capital social', 1, 'CREDIT', true, 1),
    ('11910000', 'Résultat net de l''exercice (Créditeur)', 1, 'CREDIT', true, 1),
    ('11990000', 'Résultat net de l''exercice (Débiteur)', 1, 'DEBIT', true, 1),
    ('14810000', 'Emprunts bancaires', 1, 'CREDIT', true, 1),

    -- Classe 2 : Actif immobilisé
    ('21110000', 'Frais de constitution', 2, 'DEBIT', true, 1),
    ('23320000', 'Matériel et outillage', 2, 'DEBIT', true, 1),
    ('23400000', 'Matériel de transport', 2, 'DEBIT', true, 1),
    ('23510000', 'Mobilier de bureau', 2, 'DEBIT', true, 1),
    ('23550000', 'Matériel informatique', 2, 'DEBIT', true, 1),
    ('28330000', 'Amortissements du matériel', 2, 'CREDIT', true, 1),

    -- Classe 3 : Actif circulant
    ('31110000', 'Marchandises en stock', 3, 'DEBIT', true, 1),
    ('31210000', 'Matières premières', 3, 'DEBIT', true, 1),
    ('34210000', 'Clients', 3, 'DEBIT', true, 1),
    ('34250000', 'Clients - Effets à recevoir', 3, 'DEBIT', true, 1),
    ('34550000', 'État - TVA récupérable', 3, 'DEBIT', true, 1),
    ('34551000', 'État - TVA récupérable sur charges', 3, 'DEBIT', true, 1),
    ('34552000', 'État - TVA récupérable sur immobilisations', 3, 'DEBIT', true, 1),

    -- Classe 4 : Passif circulant
    ('44110000', 'Fournisseurs', 4, 'CREDIT', true, 1),
    ('44150000', 'Fournisseurs - Effets à payer', 4, 'CREDIT', true, 1),
    ('44550000', 'État - TVA facturée / collectée', 4, 'CREDIT', true, 1),
    ('44560000', 'État - TVA due', 4, 'CREDIT', true, 1),
    ('44320000', 'Rémunérations dues au personnel', 4, 'CREDIT', true, 1),
    ('44410000', 'CNSS / Sécurité sociale', 4, 'CREDIT', true, 1),

    -- Classe 5 : Trésorerie
    ('51110000', 'Chèques à encaisser', 5, 'DEBIT', true, 1),
    ('51130000', 'Effets à l''encaissement', 5, 'DEBIT', true, 1),
    ('51410000', 'Banque', 5, 'DEBIT', true, 1),
    ('51610000', 'Caisse centrale', 5, 'DEBIT', true, 1),
    ('55200000', 'Crédits de trésorerie', 5, 'CREDIT', true, 1),

    -- Classe 6 : Charges
    ('61110000', 'Achats de marchandises', 6, 'DEBIT', true, 1),
    ('61210000', 'Achats de matières premières', 6, 'DEBIT', true, 1),
    ('61310000', 'Locations et charges locatives', 6, 'DEBIT', true, 1),
    ('61330000', 'Entretien et réparations', 6, 'DEBIT', true, 1),
    ('61410000', 'Transports', 6, 'DEBIT', true, 1),
    ('61450000', 'Frais postaux et télécoms', 6, 'DEBIT', true, 1),
    ('61470000', 'Services bancaires', 6, 'DEBIT', true, 1),
    ('61710000', 'Rémunérations du personnel', 6, 'DEBIT', true, 1),
    ('61740000', 'Charges sociales', 6, 'DEBIT', true, 1),
    ('61930000', 'Dotations aux amortissements', 6, 'DEBIT', true, 1),

    -- Classe 7 : Produits
    ('71110000', 'Ventes de marchandises', 7, 'CREDIT', true, 1),
    ('71210000', 'Ventes de biens produits', 7, 'CREDIT', true, 1),
    ('71240000', 'Prestations de services', 7, 'CREDIT', true, 1),
    ('71970000', 'Reprises d''exploitation', 7, 'CREDIT', true, 1)
ON CONFLICT (numero_compte, point_de_vente_id) DO NOTHING;
