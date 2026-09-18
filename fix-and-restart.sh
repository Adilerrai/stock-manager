#!/bin/bash
set -e

echo "=========================================================="
echo "  Script de demarrage / correction DB (lignes_livraison)"
echo "=========================================================="

# Execution directe dans le conteneur postgres si present
if docker ps -q --filter "name=pgsql.prod" | grep -q .; then
    echo ">> Execution du script SQL sur pgsql.prod..."
    docker exec -i pgsql.prod psql -U postgres -d pointvente_db << 'EOF'
ALTER TABLE IF EXISTS lignes_livraison DROP CONSTRAINT IF EXISTS uk_bgip3ymlrc9lga65p5dy9jfbr;
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
CREATE INDEX IF NOT EXISTS idx_lignes_livraison_produit_id ON lignes_livraison(produit_id);
EOF
    echo ">> Script SQL execute avec succes sur PostgreSQL."
fi

# Rebuild et redemarrage du conteneur web de l'API
if [ -f docker-compose.yml ]; then
    echo ">> Redemarrage du service pointvente-app-api..."
    docker compose -f docker-compose.yml up -d --build web
    echo ">> Service web redemarre avec succes !"
fi
