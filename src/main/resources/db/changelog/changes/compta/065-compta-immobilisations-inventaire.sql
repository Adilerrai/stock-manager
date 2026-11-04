-- Changeset 065 : registre des immobilisations et inventaire de clôture.
-- Le registre ne porte pas de montant cumulé recopié : le cumul se lit dans les dotations passées.

CREATE TABLE compta_immobilisations (
    id                     BIGSERIAL PRIMARY KEY,
    livre_id             BIGINT         NOT NULL,
    code                   VARCHAR(30)    NOT NULL,
    designation            VARCHAR(255)   NOT NULL,
    compte_immo         VARCHAR(12)         NOT NULL,
    compte_amort        VARCHAR(12),
    compte_dotation     VARCHAR(12),
    date_acquisition       DATE           NOT NULL,
    date_mise_en_service   DATE,
    valeur_brute           NUMERIC(15, 2) NOT NULL,
    cumul_reprise          NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tva_deduite            NUMERIC(15, 2) NOT NULL DEFAULT 0,
    prorata_initial        NUMERIC(5, 2)  NOT NULL DEFAULT 100,
    mode_amortissement     VARCHAR(16)    NOT NULL,
    duree_mois             INTEGER,
    coefficient_degressif  NUMERIC(4, 2),
    statut                 VARCHAR(10)    NOT NULL DEFAULT 'EN_SERVICE',
    tiers_id               BIGINT,
    source_type            VARCHAR(24),
    source_id              BIGINT,
    ecriture_acquisition_id BIGINT        NOT NULL,
    cree_le                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cree_par               VARCHAR(150)   NOT NULL,
    CONSTRAINT uk_compta_imm_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_imm_code UNIQUE (livre_id, code),
    CONSTRAINT fk_compta_imm_compte FOREIGN KEY (livre_id, compte_immo) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_imm_amort FOREIGN KEY (livre_id, compte_amort) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_imm_dotation FOREIGN KEY (livre_id, compte_dotation) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_imm_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    -- Une immobilisation sans écriture d'acquisition n'existe pas.
    CONSTRAINT fk_compta_imm_acquisition FOREIGN KEY (livre_id, ecriture_acquisition_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_imm_valeur CHECK (valeur_brute > 0 AND cumul_reprise >= 0 AND cumul_reprise <= valeur_brute),
    CONSTRAINT ck_compta_imm_mode CHECK (mode_amortissement IN ('LINEAIRE', 'DEGRESSIF', 'NON_AMORTISSABLE')),
    CONSTRAINT ck_compta_imm_duree CHECK (mode_amortissement = 'NON_AMORTISSABLE' OR (duree_mois > 0 AND compte_amort IS NOT NULL AND compte_dotation IS NOT NULL)),
    CONSTRAINT ck_compta_imm_statut CHECK (statut IN ('EN_SERVICE', 'SORTIE'))
);

CREATE TABLE compta_amortissements (
    id                BIGSERIAL PRIMARY KEY,
    livre_id        BIGINT         NOT NULL,
    immobilisation_id BIGINT         NOT NULL,
    exercice_id       BIGINT         NOT NULL,
    nature            VARCHAR(12)    NOT NULL,
    du                DATE           NOT NULL,
    au                DATE           NOT NULL,
    montant           NUMERIC(15, 2) NOT NULL,
    ecriture_id       BIGINT         NOT NULL,
    contrepassee      BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_compta_amo_immo FOREIGN KEY (livre_id, immobilisation_id) REFERENCES compta_immobilisations(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_amo_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_amo_ecriture FOREIGN KEY (livre_id, ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_amo_nature CHECK (nature IN ('ANNUELLE', 'COMPLEMENT')),
    CONSTRAINT ck_compta_amo_montant CHECK (montant > 0),
    CONSTRAINT ck_compta_amo_dates CHECK (au >= du)
);

CREATE TABLE compta_immobilisation_sorties (
    id                       BIGSERIAL      PRIMARY KEY,
    immobilisation_id        BIGINT         NOT NULL,
    livre_id               BIGINT         NOT NULL,
    nature                   VARCHAR(8)     NOT NULL,
    date_sortie              DATE           NOT NULL,
    prix_cession_ht          NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tva_cession              NUMERIC(15, 2) NOT NULL DEFAULT 0,
    amortissements_cumules   NUMERIC(15, 2) NOT NULL,
    valeur_nette             NUMERIC(15, 2) NOT NULL,
    plus_ou_moins_value      NUMERIC(15, 2) NOT NULL,
    reversement_tva          NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tiers_id                 BIGINT,
    ecriture_sortie_id       BIGINT         NOT NULL,
    ecriture_prix_id         BIGINT,
    annulee                  BOOLEAN        NOT NULL DEFAULT FALSE,
    motif_annulation         VARCHAR(500),
    cree_le                  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cree_par                 VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_sor_immo FOREIGN KEY (livre_id, immobilisation_id) REFERENCES compta_immobilisations(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_sor_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_sor_ecriture FOREIGN KEY (livre_id, ecriture_sortie_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_sor_prix FOREIGN KEY (livre_id, ecriture_prix_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_sor_nature CHECK (nature IN ('CESSION', 'RETRAIT')),
    CONSTRAINT ck_compta_sor_prix CHECK (nature = 'CESSION' OR prix_cession_ht = 0),
    CONSTRAINT ck_compta_sor_ecriture_prix CHECK ((prix_cession_ht > 0) = (ecriture_prix_id IS NOT NULL)),
    -- 7513 est passé : le résultat de cession se lit dans le grand livre et ici, à l'identique.
    CONSTRAINT ck_compta_sor_pmv CHECK (plus_ou_moins_value = prix_cession_ht - valeur_nette),
    CONSTRAINT ck_compta_sor_annulation CHECK (annulee = (motif_annulation IS NOT NULL))
);
-- Une seule sortie active : la double cession est impossible. Une sortie annulée par contre-passation reste lisible.
CREATE UNIQUE INDEX uq_compta_sor_active ON compta_immobilisation_sorties(immobilisation_id) WHERE NOT annulee;
-- Une seule dotation active par immobilisation, exercice et nature : double dotation impossible.
CREATE UNIQUE INDEX uq_compta_amo_active ON compta_amortissements(immobilisation_id, exercice_id, nature) WHERE NOT contrepassee;

-- Pas de dotation après la sortie, pas de cumul au-delà de la valeur brute.
CREATE FUNCTION compta_amortissements_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_brut    NUMERIC;
    v_reprise NUMERIC;
    v_cumul   NUMERIC;
BEGIN
    SELECT valeur_brute, cumul_reprise INTO v_brut, v_reprise FROM compta_immobilisations
     WHERE id = NEW.immobilisation_id AND livre_id = NEW.livre_id FOR UPDATE;
    IF EXISTS (SELECT 1 FROM compta_immobilisation_sorties WHERE immobilisation_id = NEW.immobilisation_id AND NOT annulee) THEN
        PERFORM compta_refus('compta_immo_sortie', format('dotation refusée, immobilisation %s sortie', NEW.immobilisation_id));
    END IF;
    SELECT COALESCE(sum(montant), 0) INTO v_cumul FROM compta_amortissements
     WHERE immobilisation_id = NEW.immobilisation_id AND NOT contrepassee;
    IF v_reprise + v_cumul + NEW.montant > v_brut THEN
        PERFORM compta_refus('compta_immo_suramortie', format('%s + %s + %s dépasse %s', v_reprise, v_cumul, NEW.montant, v_brut));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_amortissements_gardien BEFORE INSERT ON compta_amortissements
    FOR EACH ROW EXECUTE FUNCTION compta_amortissements_gardien();

-- Dotations et sorties : seule transition permise, l'annulation, et seulement si l'écriture a sa contre-passation.
CREATE FUNCTION compta_amortissements_annulation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR NOT (OLD.contrepassee = FALSE AND NEW.contrepassee = TRUE)
       OR (NEW.immobilisation_id, NEW.exercice_id, NEW.nature, NEW.montant, NEW.ecriture_id)
          IS DISTINCT FROM (OLD.immobilisation_id, OLD.exercice_id, OLD.nature, OLD.montant, OLD.ecriture_id)
       OR NOT EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.contrepassation_de_id = OLD.ecriture_id) THEN
        PERFORM compta_refus('compta_immuable', format('%s interdit sur %s', TG_OP, TG_TABLE_NAME));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_amortissements_annulation BEFORE UPDATE OR DELETE ON compta_amortissements
    FOR EACH ROW EXECUTE FUNCTION compta_amortissements_annulation();

CREATE FUNCTION compta_sorties_annulation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR NOT (OLD.annulee = FALSE AND NEW.annulee = TRUE)
       OR (NEW.immobilisation_id, NEW.date_sortie, NEW.prix_cession_ht, NEW.valeur_nette, NEW.ecriture_sortie_id)
          IS DISTINCT FROM (OLD.immobilisation_id, OLD.date_sortie, OLD.prix_cession_ht, OLD.valeur_nette, OLD.ecriture_sortie_id)
       OR NOT EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.contrepassation_de_id = OLD.ecriture_sortie_id) THEN
        PERFORM compta_refus('compta_immuable', format('%s interdit sur %s', TG_OP, TG_TABLE_NAME));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_sorties_annulation BEFORE UPDATE OR DELETE ON compta_immobilisation_sorties
    FOR EACH ROW EXECUTE FUNCTION compta_sorties_annulation();

-- Inventaire figé à la clôture, en quantités et en valeurs (CGI art. 145-II ; loi 9-88 art. 5).
CREATE TABLE compta_inventaires (
    id                   BIGSERIAL PRIMARY KEY,
    livre_id           BIGINT       NOT NULL,
    exercice_id          BIGINT       NOT NULL,
    statut               VARCHAR(10)  NOT NULL DEFAULT 'BROUILLON',
    methode_valorisation VARCHAR(30)  NOT NULL,
    aucun_stock          BOOLEAN      NOT NULL DEFAULT FALSE,
    ecriture_initial_id  BIGINT,
    ecriture_final_id    BIGINT,
    valide_le            TIMESTAMP,
    valide_par           VARCHAR(150),
    CONSTRAINT fk_compta_inv_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_inv_initial FOREIGN KEY (livre_id, ecriture_initial_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_inv_final FOREIGN KEY (livre_id, ecriture_final_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_inv UNIQUE (livre_id, exercice_id),
    CONSTRAINT uk_compta_inv_livre_id UNIQUE (livre_id, id),
    CONSTRAINT ck_compta_inv_statut CHECK (statut IN ('BROUILLON', 'VALIDE'))
);
CREATE TABLE compta_inventaire_lignes (
    id                BIGSERIAL PRIMARY KEY,
    livre_id        BIGINT         NOT NULL,
    inventaire_id     BIGINT         NOT NULL,
    compte_stock   VARCHAR(12)         NOT NULL,
    compte_variation VARCHAR(12)       NOT NULL,
    produit_id        BIGINT,
    designation       VARCHAR(255)   NOT NULL,
    quantite          NUMERIC(15, 3) NOT NULL,
    prix_unitaire     NUMERIC(15, 4) NOT NULL,
    valeur            NUMERIC(15, 2) NOT NULL,
    origine           VARCHAR(30),
    CONSTRAINT fk_compta_inl_inventaire FOREIGN KEY (livre_id, inventaire_id) REFERENCES compta_inventaires(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_compta_inl_stock FOREIGN KEY (livre_id, compte_stock) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_inl_variation FOREIGN KEY (livre_id, compte_variation) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_inl_valeur CHECK (quantite >= 0 AND prix_unitaire >= 0 AND valeur >= 0)
);
CREATE INDEX idx_compta_inl_inventaire ON compta_inventaire_lignes(inventaire_id);
