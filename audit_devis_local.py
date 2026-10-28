import re

with open('src/main/resources/db/import_sit_transmission_master_tenant5_vendredi_16h45.sql', encoding='utf-8') as f:
    master = f.read()

prod_matches = re.findall(r"INSERT INTO produits \([^)]+\) VALUES \('([^']*)',\s*'([^']*)'", master)
print('Products in master SQL:', len(prod_matches))
refs = {r: d for r, d in prod_matches}
ordered_refs = [r for r, d in prod_matches]

with open('src/main/resources/db/import_sit_transmission_devis_tenant5.sql', encoding='utf-8') as f:
    devis_sql = f.read()

lines = [l for l in devis_sql.splitlines() if l.startswith('INSERT INTO lignes_devis')]
print('Total devis lines:', len(lines))

anomalies = []
for l in lines:
    m_dev = re.search(r"d\.numero_devis = '([^']*)'", l)
    m_code = re.search(r"reference = '([^']*)'", l)
    m_desc = re.search(r",\s*'([^']*)',\s*'NON_ENGAGE'", l)
    num_dev = m_dev.group(1) if m_dev else '?'
    code = m_code.group(1) if m_code else ''
    desc = m_desc.group(1) if m_desc else ''

    # Simulate PostgreSQL: WHERE (reference = code OR reference LIKE code%) LIMIT 1
    matched_ref = None
    if code:
        for r in ordered_refs:
            if r == code or r.startswith(code):
                matched_ref = r
                break
    if not matched_ref:
        # Fell back to first product in table!
        first_ref = ordered_refs[0] if ordered_refs else '0610000007-3'
        anomalies.append((num_dev, 'FALLBACK_DEFAUT', code, desc, first_ref, refs.get(first_ref, '')))
    elif matched_ref != code:
        anomalies.append((num_dev, 'INVERSION_VARIANTE', code, desc, matched_ref, refs.get(matched_ref, '')))

print(f"\nTotal lignes de devis avec produit erroné : {len(anomalies)}")
for a in anomalies:
    print(f"Devis {a[0]} | Type: {a[1]} | Odoo: [{a[2]}] {a[3]}  --->  Tadbeer a mis: [{a[4]}] {a[5]}")
