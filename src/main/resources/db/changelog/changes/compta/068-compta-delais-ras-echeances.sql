-- Changeset 068 : délais de paiement, retenue à la source, échéancier fiscal.

-- Ce que la facture d'achat commerciale ne porte pas : date de livraison, délai convenu, litige (C. com. art. 78-2 à 78-4).
CREATE TABLE compta_dp_factures (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT         NOT NULL,
    facture_achat_id   BIGINT         NOT NULL,
    date_emission      DATE           NOT NULL,
    date_livraison     DATE,
    delai_convenu_jours INTEGER,
    litige_montant     NUMERIC(15, 2),
    litige_date        DATE,
    litige_jugement_date DATE,
    CONSTRAINT fk_compta_dpf_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_dpf UNIQUE (livre_id, facture_achat_id),
    CONSTRAINT ck_compta_dpf_delai CHECK (delai_convenu_jours IS NULL OR delai_convenu_jours BETWEEN 1 AND 120)
);

CREATE TABLE compta_dp_declarations (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT         NOT NULL,
    annee              INTEGER        NOT NULL,
    trimestre          SMALLINT       NOT NULL,
    chiffre_affaires   NUMERIC(15, 2) NOT NULL,
    total_non_payees   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_amende       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_litiges      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    statut             VARCHAR(10)    NOT NULL DEFAULT 'BROUILLON',
    date_limite        DATE           NOT NULL,
    valide_le          TIMESTAMP,
    valide_par         VARCHAR(150),
    depose_le          DATE,
    ecriture_amende_id BIGINT,
    CONSTRAINT fk_compta_dpd_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_dpd_ecriture FOREIGN KEY (livre_id, ecriture_amende_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_dpd_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_dpd UNIQUE (livre_id, annee, trimestre),
    CONSTRAINT ck_compta_dpd_trimestre CHECK (trimestre BETWEEN 1 AND 4),
    CONSTRAINT ck_compta_dpd_statut CHECK (statut IN ('BROUILLON', 'VALIDEE', 'DEPOSEE'))
);
CREATE TABLE compta_dp_lignes (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT         NOT NULL,
    declaration_id     BIGINT         NOT NULL,
    facture_achat_id   BIGINT         NOT NULL,
    numero_facture     VARCHAR(100)   NOT NULL,
    tiers_id           BIGINT         NOT NULL,
    montant_ttc        NUMERIC(15, 2) NOT NULL,
    date_emission      DATE           NOT NULL,
    date_echeance      DATE           NOT NULL,
    montant_non_paye   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    montant_hors_delai NUMERIC(15, 2) NOT NULL DEFAULT 0,
    date_paiement      DATE,
    mois_retard_periode INTEGER       NOT NULL,
    taux_applique      NUMERIC(6, 3)  NOT NULL,
    amende             NUMERIC(15, 2) NOT NULL,
    CONSTRAINT fk_compta_dpl_declaration FOREIGN KEY (livre_id, declaration_id) REFERENCES compta_dp_declarations(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_compta_dpl_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_dpl UNIQUE (declaration_id, facture_achat_id)
);

-- Attestation de régularité fiscale d'un fournisseur (CGI art. 117-IV).
CREATE TABLE compta_tiers_attestations (
    id            BIGSERIAL PRIMARY KEY,
    livre_id    BIGINT       NOT NULL,
    tiers_id      BIGINT       NOT NULL,
    date_delivrance DATE       NOT NULL,
    reference     VARCHAR(100),
    verifiee_sur_dgi BOOLEAN   NOT NULL DEFAULT FALSE,
    saisie_le     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saisie_par    VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_tia_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_tia UNIQUE (livre_id, tiers_id, date_delivrance)
);

CREATE TABLE compta_ras_operations (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT         NOT NULL,
    sens               VARCHAR(8)     NOT NULL,
    source_type        VARCHAR(24)    NOT NULL,
    source_id          BIGINT         NOT NULL,
    facture_id         BIGINT,
    tiers_id           BIGINT         NOT NULL,
    nature_operation   VARCHAR(20)    NOT NULL,
    date_paiement      DATE           NOT NULL,
    montant_ht         NUMERIC(15, 2) NOT NULL,
    taux_tva           NUMERIC(5, 2)  NOT NULL,
    montant_tva        NUMERIC(15, 2) NOT NULL,
    taux_retenue       NUMERIC(5, 2)  NOT NULL,
    montant_retenu     NUMERIC(15, 2) NOT NULL,
    cas_article        VARCHAR(12)    NOT NULL,
    ecriture_id        BIGINT,
    CONSTRAINT fk_compta_ras_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_ras_ecriture FOREIGN KEY (livre_id, ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_ras_sens CHECK (sens IN ('OPEREE', 'SUBIE')),
    CONSTRAINT ck_compta_ras_nature CHECK (nature_operation IN ('BIEN_EQUIPEMENT', 'TRAVAUX', 'SERVICE')),
    CONSTRAINT ck_compta_ras_taux CHECK (taux_retenue IN (75, 100))
);
CREATE UNIQUE INDEX uq_compta_ras ON compta_ras_operations(livre_id, source_type, source_id, COALESCE(facture_id, 0));

-- Échéancier : une date limite calculée par livre, avec sa source, jamais inférée de l'écran.
CREATE TABLE compta_echeances (
    id             BIGSERIAL PRIMARY KEY,
    livre_id     BIGINT       NOT NULL,
    type_echeance  VARCHAR(24)  NOT NULL,
    periode        VARCHAR(12)  NOT NULL,
    date_limite    DATE         NOT NULL,
    statut         VARCHAR(10)  NOT NULL DEFAULT 'A_FAIRE',
    article        VARCHAR(60)  NOT NULL,
    fait_le        DATE,
    reference      VARCHAR(100),
    CONSTRAINT fk_compta_ech_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_ech UNIQUE (livre_id, type_echeance, periode),
    CONSTRAINT ck_compta_ech_statut CHECK (statut IN ('A_FAIRE', 'FAIT', 'SANS_OBJET'))
);
CREATE INDEX idx_compta_ech_date ON compta_echeances(livre_id, statut, date_limite);

-- Jours fériés légaux : report au premier jour ouvrable (CGI art. 163-I).
CREATE TABLE compta_jours_feries (
    jour     DATE PRIMARY KEY,
    libelle  VARCHAR(120) NOT NULL
);
