-- Changeset 061 : lettrage par tiers. Le lettrage vit à côté du journal : le journal reste en ajout seul.
-- Les clés composites rendent impossible de lettrer ensemble deux tiers ou deux comptes.

CREATE TABLE compta_compteurs_lettrage (
    livre_id      BIGINT NOT NULL,
    compte_numero       VARCHAR(12) NOT NULL,
    dernier_numero  BIGINT NOT NULL,
    CONSTRAINT pk_compta_cpt_lettrage PRIMARY KEY (livre_id, compte_numero),
    CONSTRAINT fk_compta_cpt_lettrage FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT
);

CREATE TABLE compta_lettrages (
    id             BIGSERIAL PRIMARY KEY,
    livre_id     BIGINT       NOT NULL,
    compte_numero      VARCHAR(12)       NOT NULL,
    tiers_id       BIGINT       NOT NULL,
    numero         BIGINT       NOT NULL,
    code           VARCHAR(12)  NOT NULL,
    statut         VARCHAR(8)   NOT NULL,
    origine        VARCHAR(16)  NOT NULL,
    date_lettrage  DATE         NOT NULL,
    cree_le        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cree_par       VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_let_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_let_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_let_cle UNIQUE (livre_id, id, compte_numero, tiers_id),
    CONSTRAINT uk_compta_let_numero UNIQUE (livre_id, compte_numero, numero),
    CONSTRAINT ck_compta_let_statut CHECK (statut IN ('TOTAL', 'PARTIEL')),
    CONSTRAINT ck_compta_let_origine CHECK (origine IN ('MANUEL', 'AFFECTATION', 'PROPOSITION', 'CONTREPASSATION', 'REJET'))
);

CREATE TABLE compta_lettrage_lignes (
    ligne_id     BIGINT PRIMARY KEY,
    lettrage_id  BIGINT NOT NULL,
    livre_id   BIGINT NOT NULL,
    compte_numero    VARCHAR(12) NOT NULL,
    tiers_id     BIGINT NOT NULL,
    CONSTRAINT fk_compta_lel_lettrage FOREIGN KEY (livre_id, lettrage_id, compte_numero, tiers_id)
        REFERENCES compta_lettrages(livre_id, id, compte_numero, tiers_id) ON DELETE CASCADE,
    CONSTRAINT fk_compta_lel_ligne FOREIGN KEY (livre_id, ligne_id, compte_numero, tiers_id)
        REFERENCES compta_lignes(livre_id, id, compte_numero, tiers_id) ON DELETE RESTRICT
);
CREATE INDEX idx_compta_lel_lettrage ON compta_lettrage_lignes(lettrage_id);

-- AA, AB ... ZZ, AAA, AAB : le numéro ne repart jamais et ne se réutilise pas.
CREATE FUNCTION compta_code_lettrage(p_numero BIGINT) RETURNS VARCHAR LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    v_n    BIGINT := p_numero + 25;   -- 1 donne AA
    v_code VARCHAR := '';
BEGIN
    WHILE v_n >= 0 LOOP
        v_code := chr(65 + (v_n % 26)::INTEGER) || v_code;
        v_n := v_n / 26 - 1;
    END LOOP;
    RETURN v_code;
END $$;

CREATE FUNCTION compta_lettrages_avant_insert() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO compta_compteurs_lettrage AS c (livre_id, compte_numero, dernier_numero)
    VALUES (NEW.livre_id, NEW.compte_numero, 1)
    ON CONFLICT (livre_id, compte_numero) DO UPDATE SET dernier_numero = c.dernier_numero + 1
    RETURNING dernier_numero INTO NEW.numero;
    NEW.code := compta_code_lettrage(NEW.numero);
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_lettrages_avant_insert BEFORE INSERT ON compta_lettrages
    FOR EACH ROW EXECUTE FUNCTION compta_lettrages_avant_insert();

-- Une ligne d'un exercice clos ne change plus d'état de lettrage : le FEC archivé reste reproductible.
CREATE FUNCTION compta_lettrage_lignes_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_ligne BIGINT := CASE WHEN TG_OP = 'DELETE' THEN OLD.ligne_id ELSE NEW.ligne_id END;
BEGIN
    IF EXISTS (SELECT 1 FROM compta_lignes l JOIN compta_exercices e ON e.id = l.exercice_id
                WHERE l.id = v_ligne AND e.statut = 'CLOTURE') THEN
        PERFORM compta_refus('compta_lettrage_exercice_clos', format('ligne %s', v_ligne));
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_lettrage_lignes_gardien BEFORE INSERT OR DELETE ON compta_lettrage_lignes
    FOR EACH ROW EXECUTE FUNCTION compta_lettrage_lignes_gardien();

-- Au commit : un lettrage TOTAL est équilibré au centime (aucune tolérance silencieuse), deux lignes au moins.
CREATE FUNCTION compta_lettrages_verifier(p_id BIGINT) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_statut VARCHAR(8);
    v_nb     INTEGER;
    v_solde  NUMERIC;
BEGIN
    SELECT statut INTO v_statut FROM compta_lettrages WHERE id = p_id;
    IF NOT FOUND THEN RETURN; END IF;
    SELECT count(*), COALESCE(sum(l.debit - l.credit), 0) INTO v_nb, v_solde
      FROM compta_lettrage_lignes x JOIN compta_lignes l ON l.id = x.ligne_id
     WHERE x.lettrage_id = p_id;
    IF v_nb < 2 THEN
        PERFORM compta_refus('compta_lettrage_vide', format('lettrage %s', p_id));
    END IF;
    IF v_statut = 'TOTAL' AND v_solde <> 0 THEN
        PERFORM compta_refus('compta_lettrage_desequilibre', format('lettrage %s, écart %s', p_id, v_solde));
    END IF;
END $$;
CREATE FUNCTION compta_lettrages_apres() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'compta_lettrages' THEN
        PERFORM compta_lettrages_verifier(NEW.id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM compta_lettrages_verifier(OLD.lettrage_id);
    ELSE
        PERFORM compta_lettrages_verifier(NEW.lettrage_id);
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER trg_compta_lettrages_apres AFTER INSERT OR UPDATE ON compta_lettrages
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION compta_lettrages_apres();
CREATE CONSTRAINT TRIGGER trg_compta_lettrage_lignes_apres AFTER INSERT OR DELETE ON compta_lettrage_lignes
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION compta_lettrages_apres();
