#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
SCRIPT MASTER UNIFIÉ D'IMPORTATION ODOO LOCAL -> ERP POINT DE VENTE (TENANT 5)
=============================================================================
Ce script remplace et unifie l'ensemble des anciens scripts :
  - generate_sql_from_odoo_local.py
  - generate_sql_achats_fournisseurs_only.py
  - generate_sql_ventes_clients_only.py

Il extrait TOUTES les données depuis Odoo 10 (SIT-NV) et génère UN SEUL
fichier SQL consolidé et transactionnel pour le Tenant #5 (SIT TRANSMISSION SARL) :
  1. Purge propre et sécurisée Tenant 5 (respect strict des clés étrangères)
  2. Point de vente & Dépôts
  3. Catégories d'articles (arborescence hiérarchique)
  4. Fournisseurs & Fournisseur par défaut
  5. Clients & CRM Entreprises
  6. Articles, Unités de mesure, Prix HT/TTC & Stocks initiaux
  7. Achats Fournisseurs :
     - Commandes d'achat & Lignes de commande
     - Bons de réception / Livraisons fournisseur & Lignes avec dépôts
     - Synchronisation des réceptions et statuts commandes d'achat
     - Factures d'achat & Lignes
     - Règlements fournisseurs & Lettrage avec factures d'achat
     - Recalculs des statuts de paiement des factures d'achat
  8. Ventes Clients :
     - Commandes clients & Lignes avec remises
     - Bons de livraison clients & Lignes avec dépôts et prix
     - Table de jonction BLs clients <-> Commandes
     - Recalculs des montants totaux des BLs clients
     - Factures de vente & Lignes avec remises et TVA
     - Liaison BLs clients <-> Factures clients
     - Avoirs clients & Lignes d'avoir
     - Règlements clients & Lettrage
     - Affectations multi-factures (paiement_affectations)
     - Recalculs soldes et statuts des factures de vente (montant_paye, restant, statut)
  9. Validation transactionnelle (COMMIT)
=============================================================================
"""

import http.cookiejar
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from datetime import datetime

# Encodage console Windows
sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')

# =============================================================================
# CONFIGURATION
# =============================================================================
BASE_URL = os.environ.get('ODOO_URL', 'http://192.168.1.200:8069')
ODOO_DB = os.environ.get('ODOO_DB', 'SIT-NV')
ODOO_LOGIN = os.environ.get('ODOO_LOGIN', 'm.zaydan@sit.ma')
ODOO_PASSWORD = os.environ.get('ODOO_PASSWORD', '$m@zaydan@23')

TENANT_ID = 5
TENANT_NAME = "SIT TRANSMISSION SARL"
DEFAULT_OUTPUT_SQL = "import_sit_tenant5_local.sql"

SESSION_COOKIE = None


# =============================================================================
# GESTION SESSION ODOO
# =============================================================================
def get_opener_and_cookie():
    global SESSION_COOKIE
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

    login_url = f"{BASE_URL}/web/login?db={ODOO_DB}"
    resp = opener.open(login_url)
    html = resp.read().decode('utf-8', errors='ignore')

    m = re.search(r'name="csrf_token" value="([^"]+)"', html)
    csrf = m.group(1) if m else ''

    data = urllib.parse.urlencode({
        'db': ODOO_DB,
        'login': ODOO_LOGIN,
        'password': ODOO_PASSWORD,
        'csrf_token': csrf
    }).encode()

    opener.open(login_url, data=data)
    for c in cj:
        if c.name == 'session_id':
            SESSION_COOKIE = c.value
            break

    if not SESSION_COOKIE:
        raise RuntimeError("Impossible de récupérer la session Odoo. Vérifiez les identifiants.")

    print(f"[OK] Authentifié sur {BASE_URL} (Base: {ODOO_DB}) -> session_id={SESSION_COOKIE[:12]}...")
    return SESSION_COOKIE


def odoo_search_read_all(model, fields, domain=None, order="id asc", batch_size=500):
    global SESSION_COOKIE
    if domain is None:
        domain = []

    if not SESSION_COOKIE:
        get_opener_and_cookie()

    all_records = []
    offset = 0

    while True:
        url = f"{BASE_URL}/web/dataset/search_read"
        payload = {
            "jsonrpc": "2.0",
            "method": "call",
            "params": {
                "model": model,
                "fields": fields,
                "domain": domain,
                "offset": offset,
                "limit": batch_size,
                "sort": order
            },
            "id": 1
        }
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Cookie': f'session_id={SESSION_COOKIE}',
            'Origin': BASE_URL,
            'Referer': f'{BASE_URL}/web',
        }
        req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers)
        try:
            with urllib.request.urlopen(req) as resp:
                res = json.loads(resp.read().decode('utf-8'))
                if "error" in res:
                    err_msg = str(res['error'].get('data', {}).get('message') or res['error'].get('message'))
                    if "Session expired" in err_msg:
                        print("\nSession expirée, ré-authentification...")
                        get_opener_and_cookie()
                        continue
                    print(f"\n[ERREUR Odoo {model}]: {err_msg}")
                    break
                records = res.get("result", {}).get("records", [])
                if not records:
                    break
                all_records.extend(records)
                offset += len(records)
                print(f"   -> {model}: {len(all_records)} enregistrements récupérés...", end='\r')
                if len(records) < batch_size:
                    break
        except Exception as e:
            print(f"\n[EXC Odoo {model}]: {e}")
            break

    print(f"   -> {model}: Total {len(all_records)} enregistrements récupérés.          ")
    return all_records


# =============================================================================
# UTILITAIRES DE CONVERSION ET SQL ESCAPE
# =============================================================================
def sql_escape(val, max_len=None):
    if val is None or val is False:
        return "NULL"
    s = str(val).strip()
    if s == "" or s.lower() == "none" or s.lower() == "false" or s == '-':
        return "NULL"
    if max_len and len(s) > max_len:
        s = s[:max_len]
    s = s.replace("'", "''")
    return f"'{s}'"


def num_val(val, default=0.0):
    if val is None:
        return default
    try:
        s = str(val).replace(',', '.').strip()
        if not s or s.lower() == 'none' or s.lower() == 'false' or s == '-':
            return default
        return float(s)
    except Exception:
        return default


def date_val(val):
    if val is None or val is False:
        return "CURRENT_TIMESTAMP"
    s = str(val).strip()
    if not s or s.lower() == 'none' or s == '-':
        return "CURRENT_TIMESTAMP"
    m = re.match(r'^(\d{4}-\d{2}-\d{2})[T ]?(\d{2}:\d{2}:\d{2})?', s)
    if m:
        d = m.group(1)
        t = m.group(2) if m.group(2) else "00:00:00"
        return f"'{d} {t}'"
    return "CURRENT_TIMESTAMP"


def map_unite(u_str):
    u = str(u_str or '').lower()
    if 'm²' in u or 'm2' in u:
        return 'M2'
    elif 'm' == u or 'mètre' in u or 'metre' in u:
        return 'METRE'
    elif 'kg' in u or 'kilo' in u:
        return 'KG'
    elif 'l' == u or 'litre' in u:
        return 'LITRE'
    elif 'carton' in u:
        return 'CARTON'
    elif 'palette' in u:
        return 'PALETTE'
    elif 'paquet' in u:
        return 'PAQUET'
    elif 'rouleau' in u:
        return 'ROULEAU'
    return 'PIECE'


def parse_code_nom(display_name, default_code=None):
    code = (default_code or "").strip()
    nom = (display_name or "").strip()
    if not code and "[" in nom and "]" in nom:
        try:
            code = nom.split("]")[0].replace("[", "").strip()
            nom = nom.split("]", 1)[1].strip()
        except Exception:
            pass
    return code, nom


def get_depot_subquery(doc_ref_or_name=None, location_str=None, tenant_id=5):
    combined = f"{doc_ref_or_name or ''} {location_str or ''}".upper().strip()
    if 'GARA' in combined or 'DEPG' in combined:
        target = 'GARA'
    elif 'TAC' in combined or 'DEPT' in combined or 'TACHAROUK' in combined:
        target = 'TAC'
    else:
        target = 'WH'
    return (
        f"COALESCE("
        f"(SELECT id FROM depots WHERE description = '{target}' AND point_de_vente_id = {tenant_id} LIMIT 1), "
        f"(SELECT id FROM depots WHERE nom ILIKE '%{target}%' AND point_de_vente_id = {tenant_id} LIMIT 1), "
        f"(SELECT id FROM depots WHERE point_de_vente_id = {tenant_id} LIMIT 1)"
        f")"
    )


# =============================================================================
# CLASSE DE STREAMING SQL (ÉCRITURE BUFFERISÉE À HAUTE PERFORMANCE)
# =============================================================================
class SqlStreamWriter:
    def __init__(self, filepath):
        self.filepath = filepath
        self.file = None
        self.lines_written = 0

    def __enter__(self):
        self.file = open(self.filepath, 'w', encoding='utf-8', buffering=128 * 1024)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.file:
            self.file.flush()
            self.file.close()

    def write_line(self, line=""):
        self.file.write(line + "\n")
        self.lines_written += 1

    def write_block(self, block_str):
        for line in block_str.strip().split("\n"):
            self.write_line(line)


# =============================================================================
# FONCTION PRINCIPALE DE GÉNÉRATION MASTER
# =============================================================================
def generate(output_file_name=None):
    start_time = time.time()
    if not output_file_name:
        output_file_name = DEFAULT_OUTPUT_SQL

    output_sql_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), output_file_name)

    print("=" * 80)
    print(f"GÉNÉRATION DU SCRIPT MASTER CONSOLIDÉ ODOO LOCAL -> TENANT #{TENANT_ID}")
    print(f"Société   : {TENANT_NAME}")
    print(f"Source    : {BASE_URL} (Base: {ODOO_DB})")
    print(f"Fichier   : {output_sql_path}")
    print("=" * 80)

    # Initialisation de la session Odoo
    get_opener_and_cookie()

    with SqlStreamWriter(output_sql_path) as writer:
        # En-tête transactionnel
        writer.write_line("\\set ON_ERROR_STOP on")
        writer.write_line("-- =============================================================================")
        writer.write_line(f"-- SCRIPT MASTER UNIQUE CONSOLIDÉ : {TENANT_NAME} (TENANT #{TENANT_ID})")
        writer.write_line(f"-- Source : Odoo Local ({BASE_URL}), Base: {ODOO_DB}")
        writer.write_line(f"-- Date de génération : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        writer.write_line("-- =============================================================================")
        writer.write_line("BEGIN;\n")

        # ---------------------------------------------------------------------
        # 1. PURGE CIBLÉE SUR LE TENANT 5
        # ---------------------------------------------------------------------
        print("\n[Étape 1/8] Génération de la purge ciblée Tenant #5...")
        writer.write_block(f"""
-- -----------------------------------------------------------------------------
-- 1. PURGE CIBLÉE TENANT #{TENANT_ID} (ORDRE RESPECTANT LES CONTRAINTES FK)
-- -----------------------------------------------------------------------------
DELETE FROM paiement_affectations WHERE paiement_id IN (SELECT id FROM paiements WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM paiements WHERE point_de_vente_id = {TENANT_ID} OR facture_id IN (SELECT id FROM factures WHERE point_de_vente_id = {TENANT_ID});

DELETE FROM lignes_facture WHERE facture_id IN (SELECT id FROM factures WHERE point_de_vente_id = {TENANT_ID}) 
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM factures WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM bons_livraison_client_commandes WHERE bon_livraison_id IN (SELECT id FROM bons_livraison_client WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM lignes_bon_livraison_client WHERE bon_livraison_client_id IN (SELECT id FROM bons_livraison_client WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM bons_livraison_client WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_commande_client WHERE commande_client_id IN (SELECT id FROM commandes_client WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM commandes_client WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_bon_preparation WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM bons_preparation WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_vente WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM ventes WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_devis WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM devis WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_avoir WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM avoirs WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM reglements_fournisseur WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM cheques_effets WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_facture_achat WHERE facture_achat_id IN (SELECT id FROM factures_achat WHERE point_de_vente_id = {TENANT_ID}) 
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM factures_achat WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_livraison WHERE livraison_id IN (SELECT id FROM livraisons WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM livraisons WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_commande WHERE commande_id IN (SELECT id FROM commandes WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM commandes WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_offre_fournisseur WHERE offre_fournisseur_id IN (SELECT id FROM offres_fournisseurs WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM offres_fournisseurs WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_demande_prix WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM demandes_prix WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_transfert_stock WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM transferts_stock WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_inventaire WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM inventaires WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM mouvements_stock WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lots WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM stock_qualites WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM stocks WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM produit_images WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});

DELETE FROM crm_activites WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_taches WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_rapports_visite WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_visites WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_opportunites WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_contacts WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_entreprises WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM clients WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM produits WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM categories WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM depots WHERE point_de_vente_id = {TENANT_ID};
""")

        # ---------------------------------------------------------------------
        # 2. POINT DE VENTE & DÉPÔTS
        # ---------------------------------------------------------------------
        print("\n[Étape 2/8] Point de Vente & Dépôts...")
        writer.write_block(f"""
-- -----------------------------------------------------------------------------
-- 2. POINT DE VENTE & DÉPÔTS (TENANT #{TENANT_ID})
-- -----------------------------------------------------------------------------
INSERT INTO point_de_vente (id, tenant_id, nom_point_de_vente, nom, actif, date_creation)
VALUES ({TENANT_ID}, {TENANT_ID}, '{TENANT_NAME}', '{TENANT_NAME}', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE 
SET nom_point_de_vente = EXCLUDED.nom_point_de_vente,
    nom = EXCLUDED.nom,
    tenant_id = EXCLUDED.tenant_id;

SELECT setval('point_de_vente_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM point_de_vente), {TENANT_ID}));

INSERT INTO depots (nom, description, adresse, point_de_vente_id, actif, date_creation)
VALUES 
  ('SIT TRANSMISSION', 'WH', 'Siège / Dépôt Central Casablanca', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP),
  ('DEP TACHAROUK', 'TAC', 'Magasin Tacharouk Casablanca', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP),
  ('DEPOT GARA', 'GARA', 'Dépôt Gara', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
""")

        # ---------------------------------------------------------------------
        # 3. CATÉGORIES D'ARTICLES
        # ---------------------------------------------------------------------
        print("\n[Étape 3/8] Catégories d'articles (product.category)...")
        raw_cats = odoo_search_read_all("product.category", ["name", "parent_id"])
        cat_id_to_code = {}
        for c in raw_cats:
            c_id = c["id"]
            cat_id_to_code[c_id] = f"CAT_{c_id}"

        writer.write_line("\n-- -----------------------------------------------------------------------------")
        writer.write_line(f"-- 3. CATÉGORIES D'ARTICLES (TENANT #{TENANT_ID})")
        writer.write_line("-- -----------------------------------------------------------------------------")

        for c in raw_cats:
            c_id = c["id"]
            c_name = str(c.get("name") or f"Catégorie #{c_id}").strip()
            e_nom = c_name[:255].replace("'", "''")
            c_code = cat_id_to_code[c_id]
            p_obj = c.get("parent_id")
            if p_obj and isinstance(p_obj, (list, tuple)):
                p_id = p_obj[0]
                p_code = cat_id_to_code.get(p_id)
                parent_subquery = f"(SELECT id FROM categories WHERE code = '{p_code}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)" if p_code else "NULL"
            else:
                parent_subquery = "NULL"

            writer.write_line(
                f"INSERT INTO categories (nom, code, description, parent_id, actif, point_de_vente_id, date_creation) "
                f"VALUES ('{e_nom}', '{c_code}', '{e_nom}', {parent_subquery}, TRUE, {TENANT_ID}, CURRENT_TIMESTAMP) "
                f"ON CONFLICT DO NOTHING;"
            )

        # ---------------------------------------------------------------------
        # 4. FOURNISSEURS
        # ---------------------------------------------------------------------
        print("\n[Étape 4/8] Fournisseurs (res.partner supplier=True)...")
        raw_suppliers = odoo_search_read_all("res.partner", [
            "name", "street", "street2", "city", "zip", "country_id", "email", "phone", "vat", "ref"
        ], domain=[["supplier", "=", True]])

        writer.write_block(f"""
-- -----------------------------------------------------------------------------
-- 4. FOURNISSEURS (TENANT #{TENANT_ID})
-- -----------------------------------------------------------------------------
INSERT INTO fournisseurs (raison_social, adresse, ville, pays, point_de_vente_id, actif, type_fournisseur, devise, date_creation) 
VALUES ('FOURNISSEUR DIVERS', 'Casablanca', 'Casablanca', 'Maroc', {TENANT_ID}, TRUE, 'NATIONAL', 'MAD', CURRENT_TIMESTAMP) 
ON CONFLICT DO NOTHING;
""")

        for s in raw_suppliers:
            s_name = str(s.get("name") or "Fournisseur").strip()
            e_nom = s_name[:255].replace("'", "''")
            adr = s.get("street") or ""
            if s.get("street2"):
                adr = f"{adr}, {s.get('street2')}" if adr else s.get("street2")
            e_adr = sql_escape(adr)
            e_vil = sql_escape(s.get("city") or "Casablanca", 100)
            e_cp = sql_escape(s.get("zip"), 20)
            c_obj = s.get("country_id")
            pays_nom = c_obj[1] if (c_obj and isinstance(c_obj, (list, tuple))) else "Maroc"
            e_pay = sql_escape(pays_nom, 100)
            e_ice = sql_escape(s.get("vat"), 50)
            e_em = sql_escape(s.get("email"), 150)
            e_tel = sql_escape(s.get("phone"), 50)
            is_inter = (pays_nom and pays_nom.lower() != 'maroc')
            type_frn = 'INTERNATIONAL' if is_inter else 'NATIONAL'
            devise = 'EUR' if is_inter else 'MAD'

            writer.write_line(
                f"INSERT INTO fournisseurs (raison_social, adresse, ville, code_postal, pays, ice, email, telephone, "
                f"point_de_vente_id, actif, type_fournisseur, devise, date_creation) "
                f"VALUES ('{e_nom}', {e_adr}, {e_vil}, {e_cp}, {e_pay}, {e_ice}, {e_em}, {e_tel}, "
                f"{TENANT_ID}, TRUE, '{type_frn}', '{devise}', CURRENT_TIMESTAMP);"
            )

        # ---------------------------------------------------------------------
        # 5. CLIENTS & CRM
        # ---------------------------------------------------------------------
        print("\n[Étape 5/8] Clients & CRM (res.partner customer=True)...")
        raw_customers = odoo_search_read_all("res.partner", [
            "name", "street", "street2", "city", "zip", "country_id", "email", "phone", "vat", "ref", "is_company"
        ], domain=[["customer", "=", True]])

        writer.write_block(f"""
-- -----------------------------------------------------------------------------
-- 5. CLIENTS & CRM ENTREPRISES (TENANT #{TENANT_ID})
-- -----------------------------------------------------------------------------
INSERT INTO clients (code_client, nom, nom_complet, point_de_vente_id, actif, date_creation) 
VALUES ('CLI-DIVERS', 'CLIENT DIVERS', 'CLIENT DIVERS', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP) 
ON CONFLICT DO NOTHING;
""")

        for idx, c in enumerate(raw_customers, start=1):
            c_id = c["id"]
            c_name = str(c.get("name") or f"Client #{c_id}").strip()
            e_nom = c_name[:200].replace("'", "''")
            ref_cli = str(c.get("ref") or f"CL-LOC-{c_id:04d}").strip()[:50].replace("'", "''")
            adr = c.get("street") or ""
            if c.get("street2"):
                adr = f"{adr}, {c.get('street2')}" if adr else c.get("street2")
            e_adr = sql_escape(adr)
            e_vil = sql_escape(c.get("city") or "Casablanca", 100)
            e_cp = sql_escape(c.get("zip"), 20)
            c_obj = c.get("country_id")
            pays_nom = c_obj[1] if (c_obj and isinstance(c_obj, (list, tuple))) else "Maroc"
            e_pay = sql_escape(pays_nom, 100)
            e_tel = sql_escape(c.get("phone"), 50)
            e_em = sql_escape(c.get("email"), 150)
            e_ice = sql_escape(c.get("vat"), 50)
            is_cie = c.get("is_company", False)
            cat_cli = "ENTREPRISE" if is_cie else "PROFESSIONNEL"

            writer.write_line(
                f"INSERT INTO clients (code_client, nom, nom_complet, adresse, ville, code_postal, pays, telephone, email, "
                f"ice, categorie, tarif, delai_paiement_jours, actif, point_de_vente_id, date_creation) "
                f"VALUES ('{ref_cli}', '{e_nom}', '{e_nom}', {e_adr}, {e_vil}, {e_cp}, {e_pay}, {e_tel}, {e_em}, "
                f"{e_ice}, '{cat_cli}', 'DETAIL', 30, TRUE, {TENANT_ID}, CURRENT_TIMESTAMP) "
                f"ON CONFLICT DO NOTHING;"
            )

            writer.write_line(
                f"INSERT INTO crm_entreprises (nom, point_de_vente_id, adresse, ville, code_postal, pays, telephone, email, "
                f"ice, statut, client_id, date_creation) "
                f"VALUES ('{e_nom}', {TENANT_ID}, {e_adr}, {e_vil}, {e_cp}, {e_pay}, {e_tel}, {e_em}, "
                f"{e_ice}, 'CLIENT', (SELECT id FROM clients WHERE code_client = '{ref_cli}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), CURRENT_TIMESTAMP) "
                f"ON CONFLICT DO NOTHING;"
            )

        # ---------------------------------------------------------------------
        # 6. PRODUITS & STOCKS
        # ---------------------------------------------------------------------
        print("\n[Étape 6/8] Produits & Stocks (product.product)...")
        raw_products = odoo_search_read_all("product.product", [
            "default_code", "name", "barcode", "list_price", "standard_price", "qty_available", "uom_id", "categ_id"
        ])

        writer.write_line("\n-- -----------------------------------------------------------------------------")
        writer.write_line(f"-- 6. ARTICLES & STOCKS (TENANT #{TENANT_ID})")
        writer.write_line("-- -----------------------------------------------------------------------------")

        prod_ref_counts = {}
        for idx, p in enumerate(raw_products, start=1):
            p_id = p["id"]
            c_code, c_nom = parse_code_nom(p.get("name"), p.get("default_code"))
            if not c_code:
                c_code = f"ART-LOC-{p_id:05d}"
            cnt = prod_ref_counts.get(c_code, 0)
            unique_ref = c_code if cnt == 0 else f"{c_code}_{cnt+1}"
            prod_ref_counts[c_code] = cnt + 1

            e_ref = unique_ref[:255].replace("'", "''")
            e_nom = (c_nom or f"Article #{p_id}")[:255].replace("'", "''")
            e_cb = sql_escape(p.get("barcode"), 255)
            pv = num_val(p.get("list_price"), 0.0)
            pa = num_val(p.get("standard_price"), 0.0)
            qte = max(0.0, num_val(p.get("qty_available"), 0.0))

            u_obj = p.get("uom_id")
            unite = map_unite(u_obj[1] if (u_obj and isinstance(u_obj, (list, tuple))) else "PIECE")

            cat_obj = p.get("categ_id")
            if cat_obj and isinstance(cat_obj, (list, tuple)):
                c_cat_code = cat_id_to_code.get(cat_obj[0])
                cat_subquery = f"(SELECT id FROM categories WHERE code = '{c_cat_code}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)" if c_cat_code else "NULL"
                cat_nom = sql_escape(cat_obj[1], 255)
            else:
                cat_subquery = "NULL"
                cat_nom = "NULL"

            writer.write_line(
                f"INSERT INTO produits (reference, designation, description, code_barre, categorie_id, categorie_article, "
                f"prix_achat_ht, prix_achat_ttc, prix_vente_ht, prix_vente_ttc, prix_achat, prix_vente, "
                f"unite_mesure_stock, stock_minimum, point_de_vente_id, actif, date_creation) "
                f"VALUES ('{e_ref}', '{e_nom}', '{e_nom}', {e_cb}, {cat_subquery}, {cat_nom}, "
                f"{pa:.2f}, {pa:.2f}, {pv:.2f}, {pv:.2f}, {pa:.2f}, {pv:.2f}, "
                f"'{unite}', 0.00, {TENANT_ID}, TRUE, CURRENT_TIMESTAMP);"
            )

            writer.write_line(
                f"INSERT INTO stocks (produit_id, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) "
                f"VALUES ((SELECT id FROM produits WHERE reference = '{e_ref}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"{qte:.2f}, 0.00, 0.00, CURRENT_TIMESTAMP);"
            )

        # ---------------------------------------------------------------------
        # 7. ACHATS FOURNISSEURS (Commandes, BLs, Factures, Règlements)
        # ---------------------------------------------------------------------
        print("\n[Étape 7/8] Achats Fournisseurs...")
        writer.write_line("\n-- -----------------------------------------------------------------------------")
        writer.write_line(f"-- 7. ACHATS FOURNISSEURS (TENANT #{TENANT_ID})")
        writer.write_line("-- -----------------------------------------------------------------------------")

        # 7.a Commandes Fournisseurs (purchase.order)
        print("   -> 7.a Extraction Commandes d'achat (purchase.order)...")
        raw_pos = odoo_search_read_all("purchase.order", [
            "name", "partner_id", "date_order", "date_planned", "amount_untaxed", "amount_total", "currency_id", "state", "invoice_status"
        ], domain=[["state", "in", ["purchase", "done"]]])

        po_id_to_name = {}
        for po in raw_pos:
            p_id = po["id"]
            po_name = str(po.get("name") or f"PO-{p_id}").strip()
            po_id_to_name[p_id] = po_name
            e_po = po_name[:100].replace("'", "''")
            dt_cmd = date_val(po.get("date_order"))
            dt_prev = date_val(po.get("date_planned"))
            ht = float(po.get("amount_untaxed") or 0.0)
            ttc = float(po.get("amount_total") or 0.0)
            tva = max(0.0, ttc - ht)
            app_tva = "TRUE" if tva > 0.01 else "FALSE"
            taux_tva = "20.00" if tva > 0.01 else "0.00"

            c_obj = po.get("currency_id")
            devise = c_obj[1] if (c_obj and isinstance(c_obj, (list, tuple))) else "MAD"

            p_obj = po.get("partner_id")
            frn_nom = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "FOURNISSEUR DIVERS"
            e_frn = frn_nom[:255].replace("'", "''")

            frn_sub = (
                f"COALESCE("
                f"(SELECT id FROM fournisseurs WHERE (raison_social = '{e_frn}' OR raison_social ILIKE '{e_frn}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM fournisseurs WHERE raison_social = 'FOURNISSEUR DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                f")"
            )

            writer.write_line(
                f"INSERT INTO commandes (numero_commande, fournisseur_id, statut, statut_livraison, "
                f"date_commande, date_livraison_prevue, montant_total, montant_ht, montant_tva, montant_ttc, "
                f"appliquer_tva, taux_tva, devise, point_de_vente_id) "
                f"VALUES ('{e_po}', {frn_sub}, "
                f"'VALIDEE', 'EN_ATTENTE', {dt_cmd}, {dt_prev}, {ttc:.2f}, {ht:.2f}, {tva:.2f}, {ttc:.2f}, "
                f"{app_tva}, {taux_tva}, '{devise}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

        # 7.b Lignes de Commande Fournisseur (purchase.order.line)
        print("   -> 7.b Extraction Lignes de Commande d'achat (purchase.order.line)...")
        raw_pols = odoo_search_read_all("purchase.order.line", [
            "order_id", "product_id", "name", "product_qty", "qty_received", "price_unit", "price_subtotal", "taxes_id"
        ], domain=[["order_id", "in", list(po_id_to_name.keys())]] if po_id_to_name else [["id", "=", 0]])

        for pol in raw_pols:
            o_obj = pol.get("order_id")
            if not o_obj:
                continue
            po_id = o_obj[0] if isinstance(o_obj, (list, tuple)) else o_obj
            po_num = po_id_to_name.get(po_id)
            if not po_num:
                continue
            e_cmd = po_num[:100].replace("'", "''")

            p_obj = pol.get("product_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else str(pol.get("name") or "")
            c_code, c_nom = parse_code_nom(p_name)
            e_code = c_code.replace("'", "''") if c_code else ""
            e_pnom = c_nom[:255].replace("'", "''")

            if e_code:
                prod_sub = (
                    f"COALESCE("
                    f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM produits WHERE designation ILIKE '%{e_pnom}%' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                    f")"
                )
            else:
                prod_sub = (
                    f"COALESCE("
                    f"(SELECT id FROM produits WHERE (designation = '{e_pnom}' OR designation ILIKE '{e_pnom}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                    f")"
                )

            q_cmd = int(round(float(pol.get("product_qty") or 1.0)))
            q_rec = int(round(float(pol.get("qty_received") or 0.0)))
            pu = float(pol.get("price_unit") or 0.0)
            st_ht = float(pol.get("price_subtotal") or (pu * q_cmd))
            tva_l = st_ht * 0.20
            st_ttc = st_ht + tva_l

            writer.write_line(
                f"INSERT INTO lignes_commande (commande_id, produit_id, quantite_commandee, quantite_livree, "
                f"prix_unitaire, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc, montant_ligne, qualite_produit) "
                f"VALUES ("
                f"(SELECT id FROM commandes WHERE numero_commande = '{e_cmd}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"{prod_sub}, "
                f"{q_cmd}, {q_rec}, {pu:.4f}, {pu:.4f}, 20.00, {st_ht:.2f}, {tva_l:.2f}, {st_ttc:.2f}, {st_ht:.2f}, NULL) "
                f"ON CONFLICT DO NOTHING;"
            )

        # 7.c Bons de Réception Fournisseurs (stock.picking incoming)
        print("   -> 7.c Extraction Réceptions Fournisseurs (stock.picking incoming)...")
        raw_in_pickings = odoo_search_read_all("stock.picking", [
            "name", "origin", "partner_id", "min_date", "date_done", "state", "location_dest_id"
        ], domain=[["picking_type_code", "=", "incoming"]])

        pick_in_ids = [p["id"] for p in raw_in_pickings]
        raw_in_moves = odoo_search_read_all("stock.move", [
            "picking_id", "product_id", "product_uom_qty", "price_unit", "state"
        ], domain=[["picking_id", "in", pick_in_ids]] if pick_in_ids else [["id", "=", 0]])

        moves_by_in_pick = {}
        for m in raw_in_moves:
            p_obj = m.get("picking_id")
            if p_obj:
                pid = p_obj[0] if isinstance(p_obj, (list, tuple)) else p_obj
                if pid not in moves_by_in_pick:
                    moves_by_in_pick[pid] = []
                moves_by_in_pick[pid].append(m)

        for p in raw_in_pickings:
            p_id = p["id"]
            bl_num = str(p.get("name") or f"BLI-{p_id}").strip()
            e_bl = bl_num[:100].replace("'", "''")
            dt_liv = date_val(p.get("date_done") or p.get("min_date"))
            st_pick = str(p.get("state") or "").lower()
            statut_bl = "LIVREE" if st_pick == "done" else "EN_ATTENTE"

            orig = str(p.get("origin") or "").strip()
            po_num = orig if orig.startswith("P") else None
            if po_num:
                po_sub = f"(SELECT id FROM commandes WHERE numero_commande = '{po_num.replace('\'', '\'\'')}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                po_sub = "NULL"

            loc_obj = p.get("location_dest_id")
            loc_str = loc_obj[1] if (loc_obj and isinstance(loc_obj, (list, tuple))) else ""
            depot_sub = get_depot_subquery(bl_num, loc_str, TENANT_ID)

            writer.write_line(
                f"INSERT INTO livraisons (numero_livraison, commande_id, date_livraison, statut, montant_total, observations, point_de_vente_id) "
                f"VALUES ('{e_bl}', {po_sub}, {dt_liv}, '{statut_bl}', 0.00, '{orig[:255].replace('\'', '\'\'')}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

            for m in moves_by_in_pick.get(p_id, []):
                prod_obj = m.get("product_id")
                p_name = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else ""
                c_code, c_nom = parse_code_nom(p_name)
                e_code = c_code.replace("'", "''") if c_code else ""
                e_pnom = c_nom[:255].replace("'", "''")

                if e_code:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE designation ILIKE '%{e_pnom}%' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )
                else:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (designation = '{e_pnom}' OR designation ILIKE '{e_pnom}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )

                q_liv = max(1, int(round(float(m.get("product_uom_qty") or 1.0))))
                pu = float(m.get("price_unit") or 0.0)

                writer.write_line(
                    f"INSERT INTO lignes_livraison (livraison_id, depot_id, produit_id, quantite_livree, prix_produit, qualite_produit) "
                    f"VALUES ("
                    f"(SELECT id FROM livraisons WHERE numero_livraison = '{e_bl}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"{depot_sub}, {prod_sub}, "
                    f"{q_liv}, {pu:.4f}, NULL);"
                )

        # 7.d Synchronisation des commandes d'achat
        writer.write_block(f"""
UPDATE lignes_commande lc
SET quantite_livree = COALESCE((
    SELECT SUM(ll.quantite_livree)
    FROM lignes_livraison ll
    JOIN livraisons l ON ll.livraison_id = l.id
    WHERE l.commande_id = lc.commande_id
      AND ll.produit_id = lc.produit_id
), 0)
WHERE lc.commande_id IN (SELECT id FROM commandes WHERE point_de_vente_id = {TENANT_ID});

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

        # 7.e Factures Fournisseurs (account.invoice in_invoice)
        print("   -> 7.e Extraction Factures Fournisseurs (account.invoice in_invoice)...")
        raw_in_invoices = odoo_search_read_all("account.invoice", [
            "number", "reference", "date_invoice", "date_due", "partner_id", "amount_untaxed", "amount_tax", "amount_total", "state"
        ], domain=[["type", "=", "in_invoice"]])

        inv_in_id_to_num = {}
        for inv in raw_in_invoices:
            inv_in_id_to_num[inv["id"]] = str(inv.get("number") or f"FA-LOC-{inv['id']}").strip()

        in_inv_ids = [inv["id"] for inv in raw_in_invoices]
        raw_in_inv_lines = odoo_search_read_all("account.invoice.line", [
            "invoice_id", "product_id", "name", "quantity", "price_unit", "discount", "price_subtotal"
        ], domain=[["invoice_id", "in", in_inv_ids]] if in_inv_ids else [["id", "=", 0]])

        lines_by_in_inv = {}
        for l in raw_in_inv_lines:
            inv_obj = l.get("invoice_id")
            if inv_obj:
                iid = inv_obj[0] if isinstance(inv_obj, (list, tuple)) else inv_obj
                if iid not in lines_by_in_inv:
                    lines_by_in_inv[iid] = []
                lines_by_in_inv[iid].append(l)

        for inv in raw_in_invoices:
            inv_id = inv["id"]
            num_fac = inv_in_id_to_num[inv_id]
            e_num = num_fac[:100].replace("'", "''")
            dt_fac = date_val(inv.get("date_invoice"))
            dt_ech = date_val(inv.get("date_due")) if inv.get("date_due") else dt_fac

            p_obj = inv.get("partner_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else None
            if p_name:
                frn_sub = (
                    f"COALESCE("
                    f"(SELECT id FROM fournisseurs WHERE (raison_social = '{p_name[:100].replace('\'', '\'\'')}' OR raison_social ILIKE '{p_name[:100].replace('\'', '\'\'')}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM fournisseurs WHERE raison_social = 'FOURNISSEUR DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                    f")"
                )
            else:
                frn_sub = f"(SELECT id FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"

            ht = float(inv.get("amount_untaxed") or 0.0)
            tva = float(inv.get("amount_tax") or 0.0)
            ttc = float(inv.get("amount_total") or 0.0)
            st_inv = str(inv.get("state") or "").lower()
            statut = "PAYEE_TOTALEMENT" if st_inv == "paid" else ("BROUILLON" if st_inv == "draft" else "VALIDEE")

            writer.write_line(
                f"INSERT INTO factures_achat (numero_facture, date_facture, date_echeance, fournisseur_id, "
                f"montant_ht, montant_tva, montant_ttc, statut, observations, point_de_vente_id) "
                f"VALUES ('{e_num}', {dt_fac}, {dt_ech}, {frn_sub}, "
                f"{ht:.2f}, {tva:.2f}, {ttc:.2f}, '{statut}', 'Facture Odoo Local {e_num}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

            for il in lines_by_in_inv.get(inv_id, []):
                prod_obj = il.get("product_id")
                p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(il.get("name") or "")
                c_code, c_nom = parse_code_nom(p_display)
                e_code = c_code.replace("'", "''") if c_code else ""
                e_pnom = c_nom[:255].replace("'", "''")

                if e_code:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE designation ILIKE '%{e_pnom}%' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )
                else:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (designation = '{e_pnom}' OR designation ILIKE '{e_pnom}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )

                q_l = float(il.get("quantity") or 1.0)
                pu_l = float(il.get("price_unit") or 0.0)
                ht_l = float(il.get("price_subtotal") or (pu_l * q_l))
                tva_l = ht_l * 0.20
                ttc_l = ht_l + tva_l

                writer.write_line(
                    f"INSERT INTO lignes_facture_achat (facture_achat_id, produit_id, quantite, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc) "
                    f"VALUES ((SELECT id FROM factures_achat WHERE numero_facture = '{e_num}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"{prod_sub}, "
                    f"{q_l:.3f}, {pu_l:.2f}, 20.00, {ht_l:.2f}, {tva_l:.2f}, {ttc_l:.2f});"
                )

        # 7.f Règlements Fournisseurs (account.payment outbound supplier)
        print("   -> 7.f Extraction Règlements Fournisseurs (account.payment outbound supplier)...")
        raw_supp_payments = odoo_search_read_all("account.payment", [
            "name", "payment_date", "amount", "partner_id", "communication", "invoice_ids"
        ], domain=[["payment_type", "=", "outbound"], ["partner_type", "=", "supplier"]])

        for p in raw_supp_payments:
            p_name = str(p.get("name") or f"REG-LOC-{p['id']}").strip()
            e_pname = p_name[:255].replace("'", "''")
            dt_pay = date_val(p.get("payment_date"))
            amount = float(p.get("amount") or 0.0)

            p_obj = p.get("partner_id")
            p_name_supp = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else ""

            reconciled_inv_ids = p.get("invoice_ids") or []
            first_inv_num = None
            for iid in reconciled_inv_ids:
                if iid in inv_in_id_to_num:
                    first_inv_num = inv_in_id_to_num[iid]
                    break

            if first_inv_num:
                e_finv = first_inv_num[:100].replace("'", "''")
                fac_achat_sub = f"(SELECT id FROM factures_achat WHERE numero_facture = '{e_finv}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                fac_achat_sub = (
                    f"COALESCE("
                    f"(SELECT id FROM factures_achat WHERE fournisseur_id IN (SELECT id FROM fournisseurs WHERE raison_social ILIKE '{p_name_supp[:100].replace('\'', '\'\'')}%' AND point_de_vente_id = {TENANT_ID}) AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM factures_achat WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                    f")"
                )

            memo = str(p.get("communication") or "").strip()
            e_ref = sql_escape(memo, 255)
            e_notes = f"Règlement Odoo {p_name} ({memo})"[:255].replace("'", "''")

            writer.write_line(
                f"INSERT INTO reglements_fournisseur (numero_reglement, date_reglement, facture_achat_id, montant, "
                f"mode_paiement, reference_paiement, notes, nom_banque, numero_cheque, date_echeance, point_de_vente_id) "
                f"VALUES ('{e_pname}', {dt_pay}, {fac_achat_sub}, {amount:.2f}, "
                f"'VIREMENT', {e_ref}, '{e_notes}', 'Banque', NULL, {dt_pay}, {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

        # 7.g Recalcul statuts factures d'achat
        writer.write_block(f"""
UPDATE factures_achat fa
SET statut = CASE
    WHEN COALESCE((SELECT SUM(rf.montant) FROM reglements_fournisseur rf WHERE rf.facture_achat_id = fa.id), 0.00) >= fa.montant_ttc AND fa.montant_ttc > 0 THEN 'PAYEE_TOTALEMENT'
    WHEN COALESCE((SELECT SUM(rf.montant) FROM reglements_fournisseur rf WHERE rf.facture_achat_id = fa.id), 0.00) > 0 THEN 'PAYEE_PARTIELLEMENT'
    ELSE fa.statut
END
WHERE fa.point_de_vente_id = {TENANT_ID};
""")

        # ---------------------------------------------------------------------
        # 8. VENTES CLIENTS (Commandes, BLs, Factures, Avoirs, Paiements)
        # ---------------------------------------------------------------------
        print("\n[Étape 8/8] Ventes Clients...")
        writer.write_line("\n-- -----------------------------------------------------------------------------")
        writer.write_line(f"-- 8. VENTES CLIENTS (TENANT #{TENANT_ID})")
        writer.write_line("-- -----------------------------------------------------------------------------")

        user_sub = "(SELECT id FROM users LIMIT 1)"

        # 8.a Commandes Clients (sale.order)
        print("   -> 8.a Extraction Commandes Clients (sale.order)...")
        raw_sos = odoo_search_read_all("sale.order", [
            "name", "partner_id", "date_order", "amount_untaxed", "amount_total", "state", "client_order_ref"
        ], domain=[["state", "in", ["sale", "done"]]])

        so_id_to_name = {}
        for so in raw_sos:
            s_id = so["id"]
            so_name = str(so.get("name") or f"SO-{s_id}").strip()
            so_id_to_name[s_id] = so_name
            e_so = so_name[:100].replace("'", "''")
            dt_so = date_val(so.get("date_order"))
            ht = float(so.get("amount_untaxed") or 0.0)
            ttc = float(so.get("amount_total") or 0.0)
            ref_ext = sql_escape(so.get("client_order_ref"), 100)

            p_obj = so.get("partner_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
            e_pname = p_name[:200].replace("'", "''")

            cli_sub = (
                f"COALESCE("
                f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE code_client = 'CLI-DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                f")"
            )

            writer.write_line(
                f"INSERT INTO commandes_client (numero_commande, numero_commande_externe, client_id, client_nom, "
                f"statut, date_commande, montant_ht, montant_ttc, taux_tva, observations, point_de_vente_id) "
                f"VALUES ('{e_so}', {ref_ext}, {cli_sub}, '{e_pname}', 'CONFIRMEE', {dt_so}, {ht:.2f}, {ttc:.2f}, 20.00, {ref_ext}, {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

        # 8.b Lignes de Commande Client (sale.order.line)
        print("   -> 8.b Extraction Lignes de Commande Client (sale.order.line)...")
        raw_sols = odoo_search_read_all("sale.order.line", [
            "order_id", "product_id", "name", "product_uom_qty", "qty_delivered", "price_unit", "discount", "price_subtotal"
        ], domain=[["order_id", "in", list(so_id_to_name.keys())]] if so_id_to_name else [["id", "=", 0]])

        for sol in raw_sols:
            o_obj = sol.get("order_id")
            if not o_obj:
                continue
            so_id = o_obj[0] if isinstance(o_obj, (list, tuple)) else o_obj
            so_num = so_id_to_name.get(so_id)
            if not so_num:
                continue
            e_so = so_num[:100].replace("'", "''")

            prod_obj = sol.get("product_id")
            p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(sol.get("name") or "")
            c_code, c_nom = parse_code_nom(p_display)
            e_code = c_code.replace("'", "''") if c_code else ""
            e_pnom = c_nom[:255].replace("'", "''")

            if e_code:
                prod_sub = (
                    f"COALESCE("
                    f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM produits WHERE designation ILIKE '%{e_pnom}%' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                    f")"
                )
            else:
                prod_sub = (
                    f"COALESCE("
                    f"(SELECT id FROM produits WHERE (designation = '{e_pnom}' OR designation ILIKE '{e_pnom}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                    f")"
                )

            qte = float(sol.get("product_uom_qty") or 1.0)
            qte_liv = float(sol.get("qty_delivered") or 0.0)
            qte_rel = max(0.0, qte - qte_liv)
            pu = float(sol.get("price_unit") or 0.0)
            remise_pct = float(sol.get("discount") or 0.0)
            remise_mt = round(pu * qte * remise_pct / 100.0, 2)
            mt_ligne = float(sol.get("price_subtotal") or ((pu * qte) - remise_mt))
            line_obs = str(sol.get("name") or "")[:255].replace("'", "''")

            writer.write_line(
                f"INSERT INTO lignes_commande_client (commande_client_id, produit_id, quantite, quantite_commandee, "
                f"quantite_livree, quantite_reliquat, prix_unitaire, remise_pourcentage, remise_montant, montant_ligne, annulee, observations) "
                f"SELECT cmd.id, {prod_sub}, "
                f"{qte:.3f}, {qte:.3f}, {qte_liv:.3f}, {qte_rel:.3f}, {pu:.2f}, {remise_pct:.2f}, {remise_mt:.2f}, {mt_ligne:.2f}, false, '{line_obs}' "
                f"FROM commandes_client cmd WHERE cmd.numero_commande = '{e_so}' AND cmd.point_de_vente_id = {TENANT_ID} LIMIT 1;"
            )

        # 8.c Bons de Livraison Clients (stock.picking outgoing)
        print("   -> 8.c Extraction Bons de Livraison Clients (stock.picking outgoing)...")
        raw_out_pickings = odoo_search_read_all("stock.picking", [
            "name", "origin", "partner_id", "min_date", "date_done", "state", "location_id"
        ], domain=[["picking_type_code", "=", "outgoing"]])

        pick_out_ids = [p["id"] for p in raw_out_pickings]
        raw_out_moves = odoo_search_read_all("stock.move", [
            "picking_id", "product_id", "product_uom_qty", "price_unit", "state"
        ], domain=[["picking_id", "in", pick_out_ids]] if pick_out_ids else [["id", "=", 0]])

        moves_by_out_pick = {}
        for m in raw_out_moves:
            p_obj = m.get("picking_id")
            if p_obj:
                pid = p_obj[0] if isinstance(p_obj, (list, tuple)) else p_obj
                if pid not in moves_by_out_pick:
                    moves_by_out_pick[pid] = []
                moves_by_out_pick[pid].append(m)

        for p in raw_out_pickings:
            p_id = p["id"]
            bl_num = str(p.get("name") or f"BLC-{p_id}").strip()
            e_bl = bl_num[:100].replace("'", "''")
            dt_bl = date_val(p.get("date_done") or p.get("min_date"))
            st_pick = str(p.get("state") or "").lower()
            statut_bl = "LIVREE" if st_pick == "done" else "EN_ATTENTE"

            orig = str(p.get("origin") or "").strip()
            so_num = orig.split()[0] if orig.startswith("S") else None
            if so_num:
                e_so = so_num[:100].replace("'", "''")
                cmd_sub = f"(SELECT id FROM commandes_client WHERE numero_commande = '{e_so}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                cmd_sub = "NULL"

            p_obj = p.get("partner_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
            e_pname = p_name[:200].replace("'", "''")
            cli_sub = (
                f"COALESCE("
                f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE code_client = 'CLI-DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                f")"
            )

            loc_obj = p.get("location_id")
            loc_str = loc_obj[1] if (loc_obj and isinstance(loc_obj, (list, tuple))) else ""
            depot_sub = get_depot_subquery(bl_num, loc_str, TENANT_ID)

            writer.write_line(
                f"INSERT INTO bons_livraison_client (numero_bl, date_bl, client_id, commande_client_id, statut, montant_total, observations, point_de_vente_id) "
                f"VALUES ('{e_bl}', {dt_bl}, {cli_sub}, {cmd_sub}, '{statut_bl}', 0.00, '{orig[:255].replace('\'', '\'\'')}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

            if so_num:
                writer.write_line(
                    f"INSERT INTO bons_livraison_client_commandes (bon_livraison_id, commande_id) "
                    f"SELECT (SELECT id FROM bons_livraison_client WHERE numero_bl = '{e_bl}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"{cmd_sub} "
                    f"WHERE {cmd_sub} IS NOT NULL "
                    f"ON CONFLICT DO NOTHING;"
                )

            for m in moves_by_out_pick.get(p_id, []):
                prod_obj = m.get("product_id")
                p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else ""
                c_code, c_nom = parse_code_nom(p_display)
                e_code = c_code.replace("'", "''") if c_code else ""
                e_pnom = c_nom[:255].replace("'", "''")

                if e_code:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE designation ILIKE '%{e_pnom}%' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )
                else:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (designation = '{e_pnom}' OR designation ILIKE '{e_pnom}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )

                q_liv = float(m.get("product_uom_qty") or 1.0)
                pu = float(m.get("price_unit") or 0.0)
                mt_mv = round(q_liv * pu, 2)

                writer.write_line(
                    f"INSERT INTO lignes_bon_livraison_client (bon_livraison_client_id, depot_id, produit_id, quantite_livree, "
                    f"prix_vente, prix_vente_brut, remise_pourcentage, remise_montant, montant_ligne) "
                    f"SELECT bl.id, {depot_sub}, {prod_sub}, "
                    f"{q_liv:.3f}, {pu:.2f}, {pu:.2f}, 0.00, 0.00, {mt_mv:.2f} "
                    f"FROM bons_livraison_client bl WHERE bl.numero_bl = '{e_bl}' AND bl.point_de_vente_id = {TENANT_ID} LIMIT 1;"
                )

        # 8.d Recalcul montants BLs clients
        writer.write_block(f"""
UPDATE bons_livraison_client bl 
SET montant_total = COALESCE((SELECT SUM(lbl.montant_ligne) FROM lignes_bon_livraison_client lbl WHERE lbl.bon_livraison_client_id = bl.id), 0.00) 
WHERE bl.point_de_vente_id = {TENANT_ID};
""")

        # 8.e Factures Clients (account.invoice out_invoice)
        print("   -> 8.e Extraction Factures Clients (account.invoice out_invoice)...")
        raw_out_invoices = odoo_search_read_all("account.invoice", [
            "number", "reference", "origin", "date_invoice", "date_due", "partner_id", "amount_untaxed", "amount_tax", "amount_total", "state"
        ], domain=[["type", "=", "out_invoice"]])

        inv_id_to_num = {}
        for inv in raw_out_invoices:
            inv_id_to_num[inv["id"]] = str(inv.get("number") or f"FC-LOC-{inv['id']}").strip()

        out_inv_ids = [inv["id"] for inv in raw_out_invoices]
        raw_out_inv_lines = odoo_search_read_all("account.invoice.line", [
            "invoice_id", "product_id", "name", "quantity", "price_unit", "discount", "price_subtotal"
        ], domain=[["invoice_id", "in", out_inv_ids]] if out_inv_ids else [["id", "=", 0]])

        lines_by_out_inv = {}
        for l in raw_out_inv_lines:
            inv_obj = l.get("invoice_id")
            if inv_obj:
                iid = inv_obj[0] if isinstance(inv_obj, (list, tuple)) else inv_obj
                if iid not in lines_by_out_inv:
                    lines_by_out_inv[iid] = []
                lines_by_out_inv[iid].append(l)

        for inv in raw_out_invoices:
            inv_id = inv["id"]
            num_fac = inv_id_to_num[inv_id]
            e_num = num_fac[:100].replace("'", "''")
            dt_fac = date_val(inv.get("date_invoice"))
            dt_ech = date_val(inv.get("date_due")) if inv.get("date_due") else dt_fac

            p_obj = inv.get("partner_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
            e_pname = p_name[:200].replace("'", "''")
            cli_sub = (
                f"COALESCE("
                f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE code_client = 'CLI-DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                f")"
            )

            ht = float(inv.get("amount_untaxed") or 0.0)
            ttc = float(inv.get("amount_total") or 0.0)
            tva = max(0.0, ttc - ht)
            st_inv = str(inv.get("state") or "").lower()
            statut = "PAYEE_TOTALEMENT" if st_inv == "paid" else ("BROUILLON" if st_inv == "draft" else "VALIDEE")
            orig = str(inv.get("origin") or "").strip()
            notes_fac = f"Origine: {orig}" if orig else ""

            writer.write_line(
                f"INSERT INTO factures (numero_facture, date_facture, date_echeance, client_id, emise_par_user_id, "
                f"montant_ht, montant_tva, montant_ttc, montant_final, statut, notes, point_de_vente_id) "
                f"VALUES ('{e_num}', ({dt_fac})::date, ({dt_ech})::date, {cli_sub}, "
                f"{user_sub}, {ht:.2f}, {tva:.2f}, {ttc:.2f}, {ttc:.2f}, '{statut}', '{notes_fac[:255].replace('\'', '\'\'')}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

            for il in lines_by_out_inv.get(inv_id, []):
                prod_obj = il.get("product_id")
                p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(il.get("name") or "")
                c_code, c_nom = parse_code_nom(p_display)
                e_code = c_code.replace("'", "''") if c_code else ""
                e_pnom = c_nom[:255].replace("'", "''")

                if e_code:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE designation ILIKE '%{e_pnom}%' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )
                else:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (designation = '{e_pnom}' OR designation ILIKE '{e_pnom}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )

                q_l = float(il.get("quantity") or 1.0)
                pu_l = float(il.get("price_unit") or 0.0)
                remise_pct = float(il.get("discount") or 0.0)
                ht_l = float(il.get("price_subtotal") or (pu_l * q_l))
                remise_mt = round(pu_l * q_l * remise_pct / 100.0, 2)
                tva_l = ht_l * 0.20
                ttc_l = ht_l + tva_l
                line_notes = str(il.get("name") or "")[:255].replace("'", "''")

                writer.write_line(
                    f"INSERT INTO lignes_facture (facture_id, produit_id, reference, designation, quantite, "
                    f"prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc, remise_pourcentage, remise_montant, notes) "
                    f"SELECT f.id, {prod_sub}, "
                    f"'{e_code}', '{e_pnom}', {q_l:.3f}, {pu_l:.2f}, 20.00, {ht_l:.2f}, {tva_l:.2f}, {ttc_l:.2f}, "
                    f"{remise_pct:.2f}, {remise_mt:.2f}, '{line_notes}' "
                    f"FROM factures f WHERE f.numero_facture = '{e_num}' AND f.point_de_vente_id = {TENANT_ID} LIMIT 1;"
                )

            # Liaison Facture <-> BL client
            if orig and orig.startswith("S"):
                so_name = orig.split()[0]
                writer.write_line(
                    f"UPDATE bons_livraison_client "
                    f"SET facture_id = (SELECT id FROM factures WHERE numero_facture = '{e_num}' AND point_de_vente_id = {TENANT_ID} LIMIT 1) "
                    f"WHERE commande_client_id IN (SELECT id FROM commandes_client WHERE numero_commande = '{so_name}' AND point_de_vente_id = {TENANT_ID}) "
                    f"  AND point_de_vente_id = {TENANT_ID};"
                )

        # 8.f Avoirs Clients (account.invoice out_refund)
        print("   -> 8.f Extraction Avoirs Clients (account.invoice out_refund)...")
        raw_refunds = odoo_search_read_all("account.invoice", [
            "number", "origin", "reference", "date_invoice", "partner_id", "amount_untaxed", "amount_tax", "amount_total", "state"
        ], domain=[["type", "=", "out_refund"]])

        ref_ids = [r["id"] for r in raw_refunds]
        raw_ref_lines = odoo_search_read_all("account.invoice.line", [
            "invoice_id", "product_id", "name", "quantity", "price_unit", "price_subtotal"
        ], domain=[["invoice_id", "in", ref_ids]] if ref_ids else [["id", "=", 0]])

        lines_by_ref = {}
        for l in raw_ref_lines:
            inv_obj = l.get("invoice_id")
            if inv_obj:
                iid = inv_obj[0] if isinstance(inv_obj, (list, tuple)) else inv_obj
                if iid not in lines_by_ref:
                    lines_by_ref[iid] = []
                lines_by_ref[iid].append(l)

        for r_inv in raw_refunds:
            r_id = r_inv["id"]
            num_avr = str(r_inv.get("number") or f"AVR-LOC-{r_id}").strip()
            e_navr = num_avr[:100].replace("'", "''")
            dt_avr = date_val(r_inv.get("date_invoice"))

            p_obj = r_inv.get("partner_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
            e_pname = p_name[:200].replace("'", "''")
            cli_sub = (
                f"COALESCE("
                f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                f")"
            )

            ht = float(r_inv.get("amount_untaxed") or 0.0)
            ttc = float(r_inv.get("amount_total") or 0.0)
            tva = max(0.0, ttc - ht)

            writer.write_line(
                f"INSERT INTO avoirs (numero_avoir, type_avoir, nature_avoir, facture_origine_id, numero_facture_origine, "
                f"client_id, cree_par_user_id, date_avoir, montant_ht, montant_tva, montant_ttc, statut, motif, notes, point_de_vente_id) "
                f"VALUES ('{e_navr}', 'CLIENT', 'RETOUR_MARCHANDISE', NULL, NULL, "
                f"{cli_sub}, {user_sub}, ({dt_avr})::date, {ht:.2f}, {tva:.2f}, {ttc:.2f}, 'VALIDE', 'Avoir client Odoo Local', 'Avoir {e_navr}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

            for rl in lines_by_ref.get(r_id, []):
                prod_obj = rl.get("product_id")
                p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(rl.get("name") or "")
                c_code, c_nom = parse_code_nom(p_display)
                e_code = c_code.replace("'", "''") if c_code else ""
                e_pnom = c_nom[:255].replace("'", "''")

                if e_code:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )
                else:
                    prod_sub = (
                        f"COALESCE("
                        f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                        f"(SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                        f")"
                    )

                q_l = float(rl.get("quantity") or 1.0)
                pu_l = float(rl.get("price_unit") or 0.0)
                ht_l = float(rl.get("price_subtotal") or (pu_l * q_l))
                tva_l = ht_l * 0.20
                ttc_l = ht_l + tva_l
                desig_l = str(rl.get("name") or e_pnom)[:255].replace("'", "''")

                writer.write_line(
                    f"INSERT INTO lignes_avoir (avoir_id, produit_id, designation, quantite, prix_unitaire_ht, "
                    f"taux_tva, montant_ht, montant_tva, montant_ttc, remettre_en_stock, motif_retour) "
                    f"SELECT a.id, {prod_sub}, "
                    f"'{desig_l}', {q_l:.3f}, {pu_l:.2f}, 20.00, {ht_l:.2f}, {tva_l:.2f}, {ttc_l:.2f}, true, 'AUTRE' "
                    f"FROM avoirs a WHERE a.numero_avoir = '{e_navr}' AND a.point_de_vente_id = {TENANT_ID} LIMIT 1;"
                )

        # 8.g Règlements Clients (account.payment inbound customer)
        print("   -> 8.g Extraction Règlements Clients (account.payment inbound customer)...")
        raw_payments = odoo_search_read_all("account.payment", [
            "name", "payment_date", "amount", "partner_id", "communication", "invoice_ids"
        ], domain=[["payment_type", "=", "inbound"], ["partner_type", "=", "customer"]])

        for p in raw_payments:
            p_name = str(p.get("name") or f"PAY-LOC-{p['id']}").strip()
            e_pname = p_name[:100].replace("'", "''")
            dt_pay = date_val(p.get("payment_date"))
            amount = float(p.get("amount") or 0.0)

            part_obj = p.get("partner_id")
            c_name = part_obj[1] if (part_obj and isinstance(part_obj, (list, tuple))) else "CLIENT DIVERS"
            e_cname = c_name[:200].replace("'", "''")
            cli_sub = (
                f"COALESCE("
                f"(SELECT id FROM clients WHERE (nom = '{e_cname}' OR nom ILIKE '{e_cname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE code_client = 'CLI-DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"
                f")"
            )

            reconciled_inv_ids = p.get("invoice_ids") or []
            first_inv_num = None
            for iid in reconciled_inv_ids:
                if iid in inv_id_to_num:
                    first_inv_num = inv_id_to_num[iid]
                    break

            if first_inv_num:
                e_finv = first_inv_num[:100].replace("'", "''")
                fac_sub = f"(SELECT id FROM factures WHERE numero_facture = '{e_finv}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                fac_sub = "NULL"

            memo = str(p.get("communication") or "").strip()
            e_memo = sql_escape(memo, 255)

            writer.write_line(
                f"INSERT INTO paiements (numero_paiement, date_paiement, client_id, facture_id, montant, "
                f"mode_paiement, reference_paiement, nom_banque, encaisse_par_user_id, notes, point_de_vente_id) "
                f"VALUES ('{e_pname}', {dt_pay}, {cli_sub}, "
                f"{fac_sub}, {amount:.2f}, 'VIREMENT', {e_memo}, 'Banque', {user_sub}, 'Paiement Odoo Local {e_pname}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )

            # Affectations multi-factures
            if reconciled_inv_ids:
                part_amount = amount / len(reconciled_inv_ids)
                for iid in reconciled_inv_ids:
                    inv_num = inv_id_to_num.get(iid)
                    if inv_num:
                        e_invn = inv_num[:100].replace("'", "''")
                        writer.write_line(
                            f"INSERT INTO paiement_affectations (paiement_id, facture_id, montant_affecte, date_affectation) "
                            f"SELECT "
                            f"  (SELECT id FROM paiements WHERE numero_paiement = '{e_pname}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                            f"  f.id, "
                            f"  {part_amount:.2f}, "
                            f"  {dt_pay} "
                            f"FROM factures f WHERE f.numero_facture = '{e_invn}' AND f.point_de_vente_id = {TENANT_ID} "
                            f"LIMIT 1;"
                        )

        # 8.h Recalculs finaux factures clients
        writer.write_block(f"""
UPDATE factures f
SET montant_paye = COALESCE((
    SELECT SUM(pa.montant_affecte)
    FROM paiement_affectations pa
    WHERE pa.facture_id = f.id
), 0.00)
WHERE f.point_de_vente_id = {TENANT_ID};

UPDATE factures f
SET montant_restant = GREATEST(0.00, f.montant_final - f.montant_paye),
    statut = CASE 
        WHEN f.montant_paye >= f.montant_final AND f.montant_final > 0 THEN 'PAYEE_TOTALEMENT'
        WHEN f.montant_paye > 0 THEN 'PAYEE_PARTIELLEMENT'
        ELSE f.statut
    END
WHERE f.point_de_vente_id = {TENANT_ID};

COMMIT;
""")

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(output_sql_path) / (1024 * 1024)

    print("\n" + "=" * 80)
    print("SUCCÈS ABSOLU ! SCRIPT SQL MASTER UNIQUE GÉNÉRÉ :")
    print(f"  Fichier SQL généré : {output_sql_path}")
    print(f"  Taille du fichier  : {file_size_mb:.2f} MB")
    print(f"  Lignes écrites     : {writer.lines_written}")
    print(f"  Durée totale       : {elapsed:.2f} secondes")
    print("=" * 80)


if __name__ == "__main__":
    out_file = sys.argv[1] if len(sys.argv) > 1 else None
    generate(out_file)
