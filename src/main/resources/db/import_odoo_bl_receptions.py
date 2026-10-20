import json
import urllib.request
import ssl
import re
import os
import sys
from datetime import datetime

# Windows console encoding
sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')

BASE_URL = 'https://sit-transmission1.odoo.com'
COOKIE = 'frontend_lang=fr_FR; tz=Africa/Casablanca; cids=1; color_scheme=dark; session_id=5AozDPwL1wo4495It4BJgS6tpadMkaB-cVkRcScq6EklPgRe1KnFt5yO5yvEgc2da1_A-J3LVxszdneAq8GNXw'

HEADERS = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Chrome/155.0.0.0',
    'Cookie': COOKIE,
    'Origin': BASE_URL,
    'Referer': f'{BASE_URL}/odoo',
}

CTX = ssl.create_default_context()

def odoo_call(model, method, args, kwargs):
    url = f"{BASE_URL}/web/dataset/call_kw/{model}/{method}"
    payload = {
        "jsonrpc": "2.0",
        "method": "call",
        "params": {
            "model": model,
            "method": method,
            "args": args,
            "kwargs": kwargs
        }
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=HEADERS)
    with urllib.request.urlopen(req, context=CTX) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        if "error" in res:
            print(f"[ERREUR Odoo {model}.{method}]: {res['error']}")
            return []
        return res.get("result", {})

def sql_str(val, max_len=None):
    if val is None or val is False:
        return "NULL"
    s = str(val).strip()
    if not s or s.lower() == 'none' or s.lower() == 'false' or s == '-':
        return "NULL"
    if max_len and len(s) > max_len:
        s = s[:max_len]
    s = s.replace("'", "''")
    return f"'{s}'"

def sql_date(val):
    if not val or val is False:
        return "CURRENT_TIMESTAMP"
    s = str(val).strip()
    m = re.match(r'^(\d{4}-\d{2}-\d{2})[T ]?(\d{2}:\d{2}:\d{2})?', s)
    if m:
        d = m.group(1)
        t = m.group(2) if m.group(2) else "00:00:00"
        return f"'{d} {t}'"
    return "CURRENT_TIMESTAMP"

def parse_code_nom(display_name):
    code = ""
    nom = display_name or ""
    if "[" in nom and "]" in nom:
        try:
            code = nom.split("]")[0].replace("[", "").strip()
            nom = nom.split("]", 1)[1].strip()
        except:
            pass
    return code, nom

def map_statut_livraison(state):
    st = str(state or '').lower()
    if st == 'done':
        return 'LIVREE'
    elif st in ('assigned', 'confirmed'):
        return 'EN_ATTENTE'
    elif st == 'cancel':
        return 'ANNULEE'
    elif st == 'draft':
        return 'BROUILLON'
    return 'EN_ATTENTE'

def generate_bl_sql():
    print("=" * 65)
    print("EXTRACTION DES BLs & RÉCEPTIONS ODOO POUR LE TENANT #5")
    print("=" * 65)

    TENANT_ID = 5
    output_sql_path = os.path.join(
        os.path.dirname(__file__),
        "import_bl_receptions_tenant5.sql"
    )

    # 1. Fetch all incoming stock.picking
    print("1. Récupération des bons de réception (stock.picking incoming)...")
    pickings_data = odoo_call(
        "stock.picking", "web_search_read", [],
        {
            "specification": {
                "id": {},
                "name": {},
                "origin": {},
                "purchase_id": {"fields": {"display_name": {}}},
                "partner_id": {"fields": {"display_name": {}}},
                "date_done": {},
                "scheduled_date": {},
                "state": {},
                "picking_type_code": {}
            },
            "domain": [["picking_type_code", "=", "incoming"]],
            "limit": 5000,
            "order": "id asc"
        }
    )
    pickings = pickings_data.get("records", [])
    print(f"-> {len(pickings)} réceptions trouvées.")

    # 2. Fetch all incoming stock.move
    print("2. Récupération des lignes de réception (stock.move incoming)...")
    moves_data = odoo_call(
        "stock.move", "web_search_read", [],
        {
            "specification": {
                "id": {},
                "picking_id": {"fields": {"display_name": {}}},
                "product_id": {"fields": {"display_name": {}}},
                "quantity": {},
                "product_uom_qty": {},
                "price_unit": {},
                "state": {}
            },
            "domain": [["picking_type_id.code", "=", "incoming"]],
            "limit": 10000,
            "order": "id asc"
        }
    )
    moves = moves_data.get("records", [])
    print(f"-> {len(moves)} lignes d'articles trouvées.")

    # Group moves by picking_id
    moves_by_picking = {}
    for m in moves:
        p_obj = m.get("picking_id")
        if p_obj and isinstance(p_obj, dict):
            pid = p_obj.get("id")
            if pid not in moves_by_picking:
                moves_by_picking[pid] = []
            moves_by_picking[pid].append(m)

    # Build SQL
    sql_lines = []
    sql_lines.append("-- =============================================================================")
    sql_lines.append(f"-- SYNCHRONISATION BLs / RÉCEPTIONS & RATTACHEMENT COMMANDES -> TENANT #{TENANT_ID}")
    sql_lines.append(f"-- Généré le : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    sql_lines.append("-- =============================================================================\n")
    sql_lines.append("BEGIN;\n")

    # 1. Dépôt par défaut pour le Tenant #5
    sql_lines.append("-- 1. DÉPÔT PRINCIPAL DU TENANT #5")
    sql_lines.append(f"""
INSERT INTO depots (nom, adresse, point_de_vente_id, actif, date_creation)
VALUES ('Dépôt Principal', 'Casablanca', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
""")

    # 2. Nettoyage idempotent des réceptions précédentes pour le Tenant 5
    sql_lines.append(f"""
-- Nettoyage idempotent des lignes de livraisons et livraisons du tenant {TENANT_ID}
DELETE FROM lignes_livraison WHERE livraison_id IN (SELECT id FROM livraisons WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM livraisons WHERE point_de_vente_id = {TENANT_ID};
""")

    nb_rattachees = 0
    nb_directes = 0
    nb_lignes_total = 0

    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 2. INSERTION DES RÉCEPTIONS / BLs (LIVRAISONS) POUR LE TENANT #{TENANT_ID}")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    for p in pickings:
        p_id = p["id"]
        num_bl = str(p.get("name") or f"BL-{p_id}").strip()
        dt_liv = sql_date(p.get("date_done") or p.get("scheduled_date"))
        statut = map_statut_livraison(p.get("state"))

        # Résolution de la commande rattachée
        po_num = None
        if p.get("purchase_id") and isinstance(p.get("purchase_id"), dict):
            po_num = p["purchase_id"].get("display_name")
        elif p.get("origin") and str(p.get("origin")).strip().startswith("P"):
            # Ex: "P00220"
            m = re.search(r'P\d+', str(p.get("origin")))
            if m:
                po_num = m.group(0)

        if po_num:
            po_subquery = f"(SELECT id FROM commandes WHERE numero_commande = '{po_num.replace('\'', '\'\'')}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            nb_rattachees += 1
            obs = f"Réception rattachée à la commande {po_num}"
        else:
            po_subquery = "NULL"
            nb_directes += 1
            origin_desc = str(p.get("origin") or "Réception directe Odoo")
            obs = f"Réception directe ({origin_desc})"

        p_moves = moves_by_picking.get(p_id, [])
        montant_total = sum(float(m.get("quantity") or 0.0) * float(m.get("price_unit") or 0.0) for m in p_moves)

        e_bl = num_bl.replace("'", "''")
        e_obs = obs.replace("'", "''")

        sql_lines.append(
            f"INSERT INTO livraisons (numero_livraison, commande_id, date_livraison, statut, "
            f"montant_total, observations, point_de_vente_id) "
            f"VALUES ('{e_bl}', {po_subquery}, {dt_liv}, '{statut}', "
            f"{montant_total:.2f}, '{e_obs}', {TENANT_ID});"
        )

        # Insérer les lignes de livraison (stock.move)
        for m in p_moves:
            prod_obj = m.get("product_id") or {}
            prod_name = prod_obj.get("display_name") if isinstance(prod_obj, dict) else str(prod_obj)
            code_art, clean_nom = parse_code_nom(prod_name)

            qte = max(1, int(round(float(m.get("quantity") or m.get("product_uom_qty") or 1.0))))
            pu = float(m.get("price_unit") or 0.0)

            e_code = code_art.replace("'", "''") if code_art else ""
            e_pnom = clean_nom[:255].replace("'", "''")

            # Résolution produit : par référence ou désignation
            if e_code:
                prod_subquery = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                prod_subquery = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            depot_subquery = f"(SELECT id FROM depots WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"

            sql_lines.append(
                f"INSERT INTO lignes_livraison (livraison_id, depot_id, produit_id, quantite_livree, prix_produit, qualite_produit) "
                f"VALUES ("
                f"(SELECT id FROM livraisons WHERE numero_livraison = '{e_bl}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"{depot_subquery}, "
                f"COALESCE({prod_subquery}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"{qte}, {pu:.4f}, 'PREMIERE_QUALITE');"
            )
            nb_lignes_total += 1

    # 3. Synchronisation automatique des statuts de livraison des commandes
    sql_lines.append("\n-- -----------------------------------------------------------------------------")
    sql_lines.append("-- 3. MISE À JOUR DES STATUTS DE LIVRAISON DES COMMANDES ET QUANTITÉS REÇUES")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"""
-- Mettre à jour les quantités livrées sur les lignes de commande depuis les BLs
UPDATE lignes_commande lc
SET quantite_livree = COALESCE((
    SELECT SUM(ll.quantite_livree)
    FROM lignes_livraison ll
    JOIN livraisons l ON ll.livraison_id = l.id
    WHERE l.commande_id = lc.commande_id
      AND ll.produit_id = lc.produit_id
), 0)
WHERE lc.commande_id IN (SELECT id FROM commandes WHERE point_de_vente_id = {TENANT_ID});

-- Mettre à jour le statut_livraison sur la commande
UPDATE commandes c
SET statut_livraison = CASE
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande WHERE commande_id = c.id) >= (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande WHERE commande_id = c.id) AND (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande WHERE commande_id = c.id) > 0 THEN 'LIVREE'
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande WHERE commande_id = c.id) > 0 THEN 'PARTIELLE'
    ELSE 'EN_ATTENTE'
END,
date_livraison_reelle = CASE
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande WHERE commande_id = c.id) >= (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande WHERE commande_id = c.id) THEN COALESCE(c.date_livraison_reelle, CURRENT_TIMESTAMP)
    ELSE c.date_livraison_reelle
END
WHERE c.point_de_vente_id = {TENANT_ID};
""")

    sql_lines.append("\nCOMMIT;\n")

    print(f"Écriture du fichier SQL : {output_sql_path}...")
    with open(output_sql_path, "w", encoding="utf-8") as f_out:
        f_out.write("\n".join(sql_lines))

    file_size_mb = os.path.getsize(output_sql_path) / (1024 * 1024)
    print("=" * 65)
    print("RÉSUMÉ DE LA GÉNÉRATION :")
    print(f"  - Total BLs / Livraisons : {len(pickings)}")
    print(f"  - BLs rattachés à un bon de commande : {nb_rattachees}")
    print(f"  - BLs en réception directe : {nb_directes}")
    print(f"  - Total lignes d'articles reçus : {nb_lignes_total}")
    print(f"  - Fichier SQL généré : {output_sql_path} ({file_size_mb:.2f} MB, {len(sql_lines)} lignes)")
    print("=" * 65)

if __name__ == "__main__":
    generate_bl_sql()
