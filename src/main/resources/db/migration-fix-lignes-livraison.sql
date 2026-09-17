-- Migration : Suppression de la contrainte d'unicite sur lignes_livraison(produit_id)
-- Date: 2026-09-17

-- 1. Suppression directe de la contrainte generee par Hibernate
ALTER TABLE IF EXISTS lignes_livraison DROP CONSTRAINT IF EXISTS uk_bgip3ymlrc9lga65p5dy9jfbr;

-- 2. Suppression dynamique de toute contrainte unique residuelle sur produit_id
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT con.conname
        FROM pg_constraint con
        INNER JOIN pg_class rel ON rel.oid = con.conrelid
        INNER JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
        WHERE rel.relname = 'lignes_livraison'
          AND att.attname = 'produit_id'
          AND con.contype = 'u'
    ) LOOP
        EXECUTE 'ALTER TABLE lignes_livraison DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname) || ' CASCADE;';
    END LOOP;
END $$;

-- 3. Index non-unique pour les performances de jointure
CREATE INDEX IF NOT EXISTS idx_lignes_livraison_produit_id ON lignes_livraison(produit_id);
