-- Changeset 066 : rapprochement. Une ligne de relevé n'est pas une ligne d'écriture : le pointage les relie avec un montant.
-- Pas de clé étrangère vers lignes_releves_bancaires : supprimerReleve reste possible côté trésorerie, l'orphelin se signale.

CREATE TABLE compta_pointages (
    id               BIGSERIAL PRIMARY KEY,
    livre_id       BIGINT         NOT NULL,
    ligne_releve_id  BIGINT         NOT NULL,
    ligne_id         BIGINT         NOT NULL,
    montant          NUMERIC(15, 2) NOT NULL,
    date_pointage    DATE           NOT NULL,
    origine          VARCHAR(12)    NOT NULL DEFAULT 'MANUEL',
    pointe_le        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pointe_par       VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_poi_ligne FOREIGN KEY (livre_id, ligne_id) REFERENCES compta_lignes(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_poi UNIQUE (ligne_releve_id, ligne_id),
    CONSTRAINT ck_compta_poi_montant CHECK (montant > 0),
    CONSTRAINT ck_compta_poi_origine CHECK (origine IN ('MANUEL', 'PROPOSITION'))
);
CREATE INDEX idx_compta_poi_ligne ON compta_pointages(ligne_id);
CREATE INDEX idx_compta_poi_releve ON compta_pointages(livre_id, ligne_releve_id);

-- Une ligne d'écriture ne se pointe pas au-delà de son montant : un débit bancaire ne justifie pas deux factures.
CREATE FUNCTION compta_pointages_verifier() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_montant NUMERIC;
    v_pointe  NUMERIC;
    v_releve  NUMERIC;
    v_pointe_releve NUMERIC;
    v_nature  VARCHAR(12);
BEGIN
    SELECT debit + credit, nature_ecriture INTO v_montant, v_nature FROM compta_lignes WHERE id = NEW.ligne_id;
    -- Une ligne d'à-nouveau reprend un solde, pas une opération bancaire : elle n'entre jamais dans le suspens.
    IF v_nature = 'A_NOUVEAU' THEN
        PERFORM compta_refus('compta_pointage_a_nouveau', format('ligne %s', NEW.ligne_id));
    END IF;
    SELECT COALESCE(sum(montant), 0) INTO v_pointe FROM compta_pointages WHERE ligne_id = NEW.ligne_id;
    IF v_pointe > v_montant THEN
        PERFORM compta_refus('compta_pointage_excessif', format('ligne %s, %s pointés pour %s', NEW.ligne_id, v_pointe, v_montant));
    END IF;
    SELECT COALESCE(debit, 0) + COALESCE(credit, 0) INTO v_releve FROM lignes_releves_bancaires WHERE id = NEW.ligne_releve_id;
    SELECT COALESCE(sum(montant), 0) INTO v_pointe_releve FROM compta_pointages WHERE ligne_releve_id = NEW.ligne_releve_id;
    IF v_releve IS NOT NULL AND v_pointe_releve > v_releve THEN
        PERFORM compta_refus('compta_pointage_excessif', format('relevé %s, %s pointés pour %s', NEW.ligne_releve_id, v_pointe_releve, v_releve));
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER trg_compta_pointages_verifier AFTER INSERT OR UPDATE ON compta_pointages
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION compta_pointages_verifier();

-- État de rapprochement validé : photo à la date d'arrêté, un par compte et par date.
CREATE TABLE compta_rapprochements (
    id                   BIGSERIAL PRIMARY KEY,
    livre_id           BIGINT         NOT NULL,
    compte_numero            VARCHAR(12)         NOT NULL,
    compte_financier_id  BIGINT         NOT NULL,
    date_arrete          DATE           NOT NULL,
    solde_comptable      NUMERIC(15, 2) NOT NULL,
    solde_releve         NUMERIC(15, 2) NOT NULL,
    solde_comptable_rectifie NUMERIC(15, 2) NOT NULL,
    solde_releve_rectifie    NUMERIC(15, 2) NOT NULL,
    contenu              JSONB          NOT NULL,
    valide_le            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valide_par           VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_rap_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_rap UNIQUE (livre_id, compte_numero, date_arrete),
    -- Un état validé tombe juste : l'écart reste visible en brouillon, à l'écran.
    CONSTRAINT ck_compta_rap_egalite CHECK (solde_comptable_rectifie = solde_releve_rectifie)
);
