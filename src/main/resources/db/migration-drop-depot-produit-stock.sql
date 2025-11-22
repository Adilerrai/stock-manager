-- Migration suppression des colonnes depot_id / produit_id devenues obsolètes
-- Adapter le schéma aux nouvelles entités Java (Stock / StockQualite sans dépôt ni produit direct)
-- PostgreSQL

BEGIN;

-- STOCK_QUALITES
-- Supprimer anciennes contraintes composites si elles existent
ALTER TABLE stock_qualites DROP CONSTRAINT IF EXISTS stock_qualites_produit_id_depot_id_qualite_produit_key;
ALTER TABLE stock_qualites DROP CONSTRAINT IF EXISTS stock_qualites_produit_id_depot_id_key;
-- Supprimer clés étrangères éventuellement présentes
ALTER TABLE stock_qualites DROP CONSTRAINT IF EXISTS fk_stock_qualites_produit;
ALTER TABLE stock_qualites DROP CONSTRAINT IF EXISTS fk_stock_qualites_depot;
ALTER TABLE stock_qualites DROP CONSTRAINT IF EXISTS stock_qualites_produit_id_fkey;
ALTER TABLE stock_qualites DROP CONSTRAINT IF EXISTS stock_qualites_depot_id_fkey;

-- Supprimer colonnes non utilisées désormais
ALTER TABLE stock_qualites DROP COLUMN IF EXISTS produit_id CASCADE;
ALTER TABLE stock_qualites DROP COLUMN IF EXISTS depot_id CASCADE;

-- Ajouter contrainte d'unicité sur (stock_id, qualite_produit)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name='stock_qualites'
          AND constraint_type='UNIQUE'
          AND constraint_name='stock_qualites_stock_id_qualite_produit_key'
    ) THEN
        ALTER TABLE stock_qualites
        ADD CONSTRAINT stock_qualites_stock_id_qualite_produit_key UNIQUE (stock_id, qualite_produit);
    END IF;
END$$;

-- STOCKS
ALTER TABLE stocks DROP CONSTRAINT IF EXISTS stocks_produit_id_depot_id_key;
ALTER TABLE stocks DROP CONSTRAINT IF EXISTS stocks_depot_id_fkey;
ALTER TABLE stocks DROP COLUMN IF EXISTS depot_id CASCADE;

-- Unicité sur produit_id (un stock par produit)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name='stocks'
          AND constraint_type='UNIQUE'
          AND constraint_name='stocks_produit_id_key'
    ) THEN
        ALTER TABLE stocks ADD CONSTRAINT stocks_produit_id_key UNIQUE (produit_id);
    END IF;
END$$;

COMMIT;

-- Vérification rapide (à exécuter manuellement):
-- \d stocks
-- \d stock_qualites
-- SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name='stock_qualites';
-- SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name='stocks';

