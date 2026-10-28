import json
import re

with open('odoo_all_sale_orders.json', 'r', encoding='utf-8') as f:
    orders = json.load(f)
orders_by_name = {o['name'].strip(): o for o in orders}

with open('src/main/resources/db/import_sit_transmission_master_tenant5_vendredi_16h45.sql', encoding='utf-8') as f:
    master = f.read()

prod_matches = re.findall(r"INSERT INTO produits \([^)]+\) VALUES \('([^']*)',\s*'([^']*)'", master)
existing_refs = {r: d for r, d in prod_matches}
ordered_refs = [r for r, d in prod_matches]
existing_desigs = {d.upper().strip(): r for r, d in prod_matches}

with open('src/main/resources/db/import_sit_transmission_devis_tenant5.sql', encoding='utf-8') as f:
    devis_sql = f.read()

lines = [l for l in devis_sql.splitlines() if l.startswith('INSERT INTO lignes_devis')]

def sql_str(s, max_len=255):
    if not s:
        return "''"
    s = str(s).strip()[:max_len].replace("'", "''")
    return f"'{s}'"

missing_prods = {}
specific_line_updates = []

for l in lines:
    m_dev = re.search(r"d\.numero_devis = '([^']*)'", l)
    m_code = re.search(r"reference = '([^']*)'", l)
    m_vals = re.search(r"\),\s*([0-9.]+),\s*([0-9.]+),\s*20\.00,\s*([0-9.]+),\s*([0-9.]+),\s*[0-9.]+,\s*[0-9.]+,\s*'((?:''|[^'])*)',\s*'NON_ENGAGE'", l)
    num_dev = m_dev.group(1) if m_dev else ''
    code = m_code.group(1) if m_code else ''
    qty = float(m_vals.group(1)) if m_vals else 0.0
    pu = float(m_vals.group(2)) if m_vals else 0.0
    desc = m_vals.group(5).replace("''", "'") if m_vals else ''

    matched_ref = None
    if code:
        for r in ordered_refs:
            if r == code or r.startswith(code):
                matched_ref = r
                break

    if not matched_ref or matched_ref != code:
        # Find exact line in live Odoo
        o = orders_by_name.get(num_dev)
        if not o:
            continue
        target_ol = None
        for ol in o.get('lignes', []):
            if code and (ol.get('product_code') or '').strip() == code:
                target_ol = ol
                break
        if not target_ol:
            for ol in o.get('lignes', []):
                if abs(float(ol.get('qty') or 0) - qty) < 0.01 and abs(float(ol.get('price_unit') or 0) - pu) < 0.01:
                    target_ol = ol
                    break
        if target_ol:
            real_code = (target_ol.get('product_code') or '').strip()
            real_name = (target_ol.get('product_name') or '').strip()
            if not real_code and real_name.startswith('[') and ']' in real_name:
                real_code = real_name.split(']')[0].replace('[', '').strip()
                real_name = real_name.split(']', 1)[1].strip()
            if real_code and real_code not in existing_refs:
                missing_prods[real_code] = (real_code, real_name or real_code, pu)
            elif not real_code and real_name:
                if real_name.upper().strip() not in existing_desigs:
                    gen_code = f"ODOO-{target_ol['product_id']}"
                    missing_prods[gen_code] = (gen_code, real_name, pu)
                    real_code = gen_code
                else:
                    real_code = existing_desigs[real_name.upper().strip()]

            # If description doesn't start with [real_code], add a targeted UPDATE
            if not desc.startswith(f"[{real_code}]"):
                specific_line_updates.append((num_dev, qty, pu, desc, real_code, real_name))

print(f"Missing products to insert: {len(missing_prods)}")
print(f"Specific lines without [CODE] in description: {len(specific_line_updates)}")

sql = []
sql.append("BEGIN;\n")
sql.append("-- 1. Créer les références produits manquantes dans le catalogue (Tenant 5)")
for code, (ref, desig, pu) in sorted(missing_prods.items()):
    e_ref = sql_str(ref, 100)
    e_des = sql_str(desig, 255)
    pu_ttc = round(pu * 1.20, 2)
    sql.append(
        f"INSERT INTO produits (reference, designation, description, categorie_id, prix_achat_ht, prix_achat_ttc, prix_vente_ht, prix_vente_ttc, prix_achat, prix_vente, unite_mesure_stock, stock_minimum, point_de_vente_id, actif, date_creation) "
        f"VALUES ({e_ref}, {e_des}, {e_des}, (SELECT id FROM categories WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1), {pu:.2f}, {pu_ttc:.2f}, {pu:.2f}, {pu_ttc:.2f}, {pu:.2f}, {pu:.2f}, 'PIECE', 0.00, 5, TRUE, NOW()) "
        f"ON CONFLICT (point_de_vente_id, reference) DO NOTHING;"
    )
    sql.append(
        f"INSERT INTO stocks (produit_id, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) "
        f"SELECT id, 0.00, 0.00, 0.00, NOW() FROM produits WHERE point_de_vente_id = 5 AND reference = {e_ref} "
        f"ON CONFLICT (produit_id) DO NOTHING;"
    )

sql.append("""
-- 2. Corriger automatiquement toutes les lignes de devis ayant [CODE] dans description (108 lignes : inversions de variantes + nouvelles références)
UPDATE lignes_devis ld
SET produit_id = p_exact.id
FROM devis d, produits p_exact
WHERE ld.devis_id = d.id
  AND d.point_de_vente_id = 5
  AND ld.description ~ '^\\[[^\\]]+\\]'
  AND p_exact.point_de_vente_id = 5
  AND p_exact.reference = SUBSTRING(ld.description FROM '^\\[([^\\]]+)\\]')
  AND ld.produit_id <> p_exact.id;
""")

sql.append("-- 3. Corriger les lignes de devis où le code Odoo n'était pas entre crochets dans description (44 lignes)")
for num_dev, qty, pu, desc, real_code, real_name in specific_line_updates:
    new_desc = f"[{real_code}] {real_name}"
    sql.append(
        f"UPDATE lignes_devis SET produit_id = (SELECT id FROM produits WHERE point_de_vente_id = 5 AND reference = {sql_str(real_code, 100)} LIMIT 1), "
        f"description = {sql_str(new_desc, 255)} "
        f"WHERE id = (SELECT ld.id FROM lignes_devis ld JOIN devis d ON d.id = ld.devis_id "
        f"WHERE d.point_de_vente_id = 5 AND d.numero_devis = {sql_str(num_dev, 50)} "
        f"AND ld.quantite = {qty:.3f} AND ld.prix_unitaire_ht = {pu:.2f} "
        f"AND COALESCE(ld.description, '') = {sql_str(desc, 255)} LIMIT 1);"
    )

sql.append("""
COMMIT;

-- 4. Vérification finale : doit retourner 0 ligne en anomalie !
SELECT 
    d.numero_devis,
    SUBSTRING(ld.description FROM '^\\[([^\\]]+)\\]') AS code_odoo,
    p.reference AS ref_tadbeer,
    p.designation,
    ld.quantite,
    ld.prix_unitaire_ht
FROM devis d
JOIN lignes_devis ld ON ld.devis_id = d.id
JOIN produits p ON p.id = ld.produit_id
WHERE d.point_de_vente_id = 5
  AND (
      (ld.description ~ '^\\[[^\\]]+\\]' AND SUBSTRING(ld.description FROM '^\\[([^\\]]+)\\]') <> p.reference)
      OR (p.reference = '0610000007-3' AND ld.description NOT ILIKE '%CHAINE S52%')
  );
""")

with open('fix_all_devis_tenant5.sql', 'w', encoding='utf-8') as f:
    f.write("\n".join(sql))

print(f"Total lines in fix_all_devis_tenant5.sql: {len(sql)}")
