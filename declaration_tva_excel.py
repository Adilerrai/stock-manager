#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de génération des fichiers de Déclaration TVA basé sur un fichier Excel.
Conforme aux exigences DGI Maroc (Téléservice SIMPL-TVA & Relevé de déduction Art. 112 CGI).

Fonctionnalités :
1. Génération d'un fichier Excel modèle de test (--creer-modele).
2. Lecture et calcul automatique de la TVA (Collectée, Déductible, Prorata, Crédit de report).
3. Gestion des cas standards (20%, 14%, 10%, 7%) et des opérations exonérées / exemptes (0%).
4. Export du fichier XML officiel SIMPL-TVA (télédéclaration DGI).
5. Export du Relevé de Déduction officiel Excel (Article 112).
6. Export de la Synthèse de liquidation TVA Excel.
"""

import sys
import os
import argparse
from decimal import Decimal, ROUND_HALF_UP
from datetime import date, datetime
import xml.etree.ElementTree as ET
from xml.dom import minidom
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter


def to_dec(val, default="0.00"):
    """Convertit de façon sécurisée en Decimal avec 2 décimales."""
    if val is None or val == "" or str(val).strip() == "":
        return Decimal(default)
    try:
        cleaned = str(val).replace(",", ".").replace(" ", "").replace("MAD", "").strip()
        return Decimal(cleaned).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except Exception:
        return Decimal(default)


# ==============================================================================
# 1. CRÉATION D'UN FICHIER EXCEL MODÈLE DE TEST
# ==============================================================================

def creer_fichier_excel_test(chemin_fichier="test_donnees_tva.xlsx"):
    """Crée un fichier Excel d'exemple complet avec deux feuilles : Ventes et Achats."""
    wb = openpyxl.Workbook()
    # Supprimer la feuille par défaut
    wb.remove(wb.active)

    # Styles
    header_fill = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
    header_font = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    border_thin = Border(
        left=Side(style='thin', color='DDDDDD'),
        right=Side(style='thin', color='DDDDDD'),
        top=Side(style='thin', color='DDDDDD'),
        bottom=Side(style='thin', color='DDDDDD')
    )
    align_center = Alignment(horizontal="center", vertical="center")
    align_right = Alignment(horizontal="right", vertical="center")

    # --- FEUILLE PARAMÈTRES / SOCIÉTÉ ---
    ws_soc = wb.create_sheet(title="Societe")
    ws_soc.append(["PARAMÈTRE", "VALEUR"])
    ws_soc.append(["Raison Sociale", "STE TEST DEVELOPPEMENT SARL"])
    ws_soc.append(["Identifiant Fiscal (IF)", "40123456"])
    ws_soc.append(["ICE", "002345678000091"])
    ws_soc.append(["Taxe Professionnelle (Patente)", "12345678"])
    ws_soc.append(["Période (AAAA-MM)", "2024-03"])
    ws_soc.append(["Régime (ENCAISSEMENT ou DEBIT)", "ENCAISSEMENT"])
    ws_soc.append(["Prorata Déduction (%)", 100.0])
    ws_soc.append(["Crédit TVA Antérieur (MAD)", 5000.0])

    for row in ws_soc.iter_rows(min_row=1, max_row=9, min_col=1, max_col=2):
        for cell in row:
            cell.border = border_thin
            if cell.row == 1:
                cell.fill = header_fill
                cell.font = header_font
            elif cell.column == 1:
                cell.font = Font(bold=True)

    # --- FEUILLE VENTES (TVA COLLECTÉE) ---
    ws_v = wb.create_sheet(title="Ventes")
    headers_ventes = [
        "N° Facture", "Date Facture", "Client", "ICE Client", "Désignation",
        "Montant HT", "Taux TVA (%)", "Montant TVA", "Montant TTC",
        "Date Encaissement", "Mode Encaissement", "Statut TVA"
    ]
    ws_v.append(headers_ventes)

    lignes_ventes = [
        ["FAC-2024-001", "2024-03-02", "ALPHA TECH SARL", "001111111000011", "Prestation informatique", 50000.0, 20.0, 10000.0, 60000.0, "2024-03-05", "VIREMENT", "IMPOSABLE"],
        ["FAC-2024-002", "2024-03-08", "GLOBAL TRADING", "002222222000022", "Vente matériel réseau", 80000.0, 20.0, 16000.0, 96000.0, "2024-03-12", "CHEQUE", "IMPOSABLE"],
        ["FAC-2024-003", "2024-03-15", "SOCIETE EXPORT SA", "003333333000033", "Services d'ingénierie export", 70000.0, 0.0, 0.0, 70000.0, "2024-03-20", "VIREMENT", "EXONERE_ART92"],
        ["FAC-2024-004", "2024-03-22", "RESTAURATION PLUS", "004444444000044", "Maintenance POS", 25000.0, 10.0, 2500.0, 27500.0, "2024-03-25", "VIREMENT", "IMPOSABLE"],
        ["FAC-2024-005", "2024-03-28", "CLIENT NON ENCAISSE", "005555555000055", "Licence logicielle", 30000.0, 20.0, 6000.0, 36000.0, "", "", "NON_ENCAISSE"]
    ]
    for row in lignes_ventes:
        ws_v.append(row)

    # --- FEUILLE ACHATS (TVA DÉDUCTIBLE - ART. 112) ---
    ws_a = wb.create_sheet(title="Achats")
    headers_achats = [
        "N° Ordre", "N° Facture", "Date Facture", "Fournisseur", "IF Fournisseur", "ICE Fournisseur",
        "Désignation", "Type Achat", "Montant HT", "Taux TVA (%)", "Montant TVA", "Montant TTC",
        "Prorata (%)", "Date Paiement", "Mode Paiement", "Compte Charge/Immo"
    ]
    ws_a.append(headers_achats)

    lignes_achats = [
        [1, "FA-2024-101", "2024-03-01", "FOURNISSEUR SERVEURS", "11223344", "001888888000088", "Serveur baie datacenter", "IMMOBILISATION", 60000.0, 20.0, 12000.0, 72000.0, 100.0, "2024-03-03", "VIREMENT", "235500"],
        [2, "FA-2024-102", "2024-03-06", "MAROC TELECOM", "22334455", "001999999000099", "Abonnement fibre optique", 4000.0, 20.0, 800.0, 4800.0, 100.0, "2024-03-10", "PRELEVEMENT", "614550"],
        [3, "FA-2024-103", "2024-03-11", "PAPETERIE CENTRALE", "33445566", "001777777000077", "Fournitures administratives", 2500.0, 20.0, 500.0, 3000.0, 100.0, "2024-03-14", "CHEQUE", "612540"],
        [4, "FA-2024-104", "2024-03-18", "HEBERGEMENT CLOUD", "44556677", "001666666000066", "Cloud hosting dédié", 15000.0, 20.0, 3000.0, 18000.0, 100.0, "2024-03-21", "VIREMENT", "613670"],
        [5, "FA-2024-105", "2024-03-24", "RESTAURANT D'AFFAIRES", "55667788", "001555555000055", "Frais de réception repas", 3000.0, 10.0, 300.0, 3300.0, 100.0, "2024-03-26", "CARTE_BANCAIRE", "614400"],
        [6, "FA-2024-106", "2024-03-29", "ASSURANCE LOCALE", "66778899", "001444444000044", "Assurance multirisque", 8000.0, 0.0, 0.0, 8000.0, 100.0, "2024-03-30", "VIREMENT", "613400"]
    ]
    for row in lignes_achats:
        ws_a.append(row)

    # Styliser les tableaux
    for ws in [ws_v, ws_a]:
        for cell in ws[1]:
            cell.fill = header_fill
            cell.font = header_font
            cell.alignment = align_center
            cell.border = border_thin
        for row in ws.iter_rows(min_row=2, max_row=ws.max_row, min_col=1, max_col=ws.max_column):
            for cell in row:
                cell.border = border_thin
                if isinstance(cell.value, (int, float)):
                    cell.number_format = "#,##0.00"
                    cell.alignment = align_right

    # Ajustement largeur colonnes
    for ws in [ws_soc, ws_v, ws_a]:
        for col in ws.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws.column_dimensions[col_letter].width = max(max_len + 4, 12)

    wb.save(chemin_fichier)
    print(f"[+] Modèle Excel de test créé avec succès : {chemin_fichier}")
    return chemin_fichier


# ==============================================================================
# 2. LECTURE ET TRAITEMENT DU FICHIER EXCEL
# ==============================================================================

def traiter_fichier_excel(chemin_fichier, regime_override=None, periode_override=None, prorata_override=None):
    """Lit le classeur Excel et effectue le calcul complet de la TVA."""
    if not os.path.exists(chemin_fichier):
        raise FileNotFoundError(f"Le fichier Excel {chemin_fichier} est introuvable.")

    wb = openpyxl.load_workbook(chemin_fichier, data_only=True)

    # 1. Lecture des métadonnées société
    societe = {
        "raison_sociale": "ENTREPRISE TEST",
        "if": "12345678",
        "ice": "000000000000000",
        "periode": "2024-03",
        "regime": "ENCAISSEMENT",
        "prorata": Decimal("100.00"),
        "credit_anterieur": Decimal("0.00")
    }

    if "Societe" in wb.sheetnames:
        ws_soc = wb["Societe"]
        for row in ws_soc.iter_rows(min_row=2, values_only=True):
            if not row or not row[0]:
                continue
            cle = str(row[0]).strip().lower()
            val = row[1] if len(row) > 1 else ""
            if "raison" in cle:
                societe["raison_sociale"] = str(val or "")
            elif "identifiant" in cle or "if" in cle:
                societe["if"] = str(val or "")
            elif "ice" in cle:
                societe["ice"] = str(val or "")
            elif "période" in cle or "periode" in cle:
                societe["periode"] = str(val or "")
            elif "régime" in cle or "regime" in cle:
                societe["regime"] = str(val or "").upper()
            elif "prorata" in cle:
                societe["prorata"] = to_dec(val, "100.00")
            elif "crédit" in cle or "credit" in cle:
                societe["credit_anterieur"] = to_dec(val, "0.00")

    if regime_override:
        societe["regime"] = regime_override.upper()
    if periode_override:
        societe["periode"] = periode_override
    if prorata_override is not None:
        societe["prorata"] = to_dec(prorata_override, "100.00")

    regime = societe["regime"]

    # 2. Lecture des Ventes
    ventes_lignes = []
    total_ventes_ht = Decimal("0.00")
    total_ventes_exonerees_ht = Decimal("0.00")
    total_tva_collectee = Decimal("0.00")

    if "Ventes" in wb.sheetnames:
        ws_v = wb["Ventes"]
        headers = [str(c.value).strip().lower() if c.value else "" for c in ws_v[1]]
        
        # Trouver index des colonnes
        def idx(pattern):
            for i, h in enumerate(headers):
                if pattern in h:
                    return i
            return -1

        i_num = idx("facture")
        i_date = idx("date facture")
        i_client = idx("client")
        i_ht = idx("montant ht")
        i_taux = idx("taux")
        i_tva = idx("montant tva")
        i_ttc = idx("montant ttc")
        i_enc = idx("encaissement")
        i_statut = idx("statut")

        for row in ws_v.iter_rows(min_row=2, values_only=True):
            if not row or row[i_num] is None:
                continue

            num_fac = str(row[i_num])
            ht = to_dec(row[i_ht] if i_ht >= 0 else 0)
            taux = to_dec(row[i_taux] if i_taux >= 0 else 20)
            tva = to_dec(row[i_tva] if i_tva >= 0 else 0)
            ttc = to_dec(row[i_ttc] if i_ttc >= 0 else 0)
            date_enc = str(row[i_enc] if i_enc >= 0 and row[i_enc] else "").strip()
            statut = str(row[i_statut] if i_statut >= 0 and row[i_statut] else "").strip().upper()

            # Règle d'exigibilité :
            # En régime ENCAISSEMENT : exigible seulement si encaissé
            # En régime DÉBIT : exigible dès la facture
            exigible = True
            if regime == "ENCAISSEMENT":
                if not date_enc or "NON" in statut:
                    exigible = False

            if exigible:
                total_ventes_ht += ht
                if taux == Decimal("0.00") or "EXONERE" in statut or "EXEMPT" in statut:
                    total_ventes_exonerees_ht += ht
                else:
                    total_tva_collectee += tva

            ventes_lignes.append({
                "num": num_fac,
                "ht": ht,
                "taux": taux,
                "tva": tva,
                "ttc": ttc,
                "exigible": exigible,
                "statut": statut
            })

    # 3. Lecture des Achats (Déductions)
    achats_lignes = []
    total_achats_ht = Decimal("0.00")
    total_tva_deductible_charges = Decimal("0.00")
    total_tva_deductible_immo = Decimal("0.00")

    if "Achats" in wb.sheetnames:
        ws_a = wb["Achats"]
        headers_a = [str(c.value).strip().lower() if c.value else "" for c in ws_a[1]]

        def idx_a(pattern):
            for i, h in enumerate(headers_a):
                if pattern in h:
                    return i
            return -1

        i_ordre = idx_a("ordre")
        i_fac = idx_a("facture")
        i_date_fac = idx_a("date facture")
        i_fr = idx_a("fournisseur")
        i_if = idx_a("if")
        i_ice = idx_a("ice")
        i_desig = idx_a("désignation") if idx_a("désignation") >= 0 else idx_a("designation")
        i_type = idx_a("type")
        i_ht_a = idx_a("montant ht")
        i_taux_a = idx_a("taux")
        i_tva_a = idx_a("montant tva")
        i_ttc_a = idx_a("montant ttc")
        i_pro_a = idx_a("prorata")
        i_date_paie = idx_a("date paiement")
        i_mode = idx_a("mode")
        i_cpt = idx_a("compte")

        ordre_cpt = 1
        for row in ws_a.iter_rows(min_row=2, values_only=True):
            if not row or (row[i_fac] is None and row[i_fr] is None):
                continue

            num_fac = str(row[i_fac] if i_fac >= 0 else "")
            date_fac = str(row[i_date_fac] if i_date_fac >= 0 and row[i_date_fac] else "")
            nom_fr = str(row[i_fr] if i_fr >= 0 and row[i_fr] else "")
            if_fr = str(row[i_if] if i_if >= 0 and row[i_if] else "")
            ice_fr = str(row[i_ice] if i_ice >= 0 and row[i_ice] else "")
            desig = str(row[i_desig] if i_desig >= 0 and row[i_desig] else "")
            type_achat = str(row[i_type] if i_type >= 0 and row[i_type] else "").strip().upper()
            ht = to_dec(row[i_ht_a] if i_ht_a >= 0 else 0)
            taux = to_dec(row[i_taux_a] if i_taux_a >= 0 else 20)
            tva = to_dec(row[i_tva_a] if i_tva_a >= 0 else 0)
            ttc = to_dec(row[i_ttc_a] if i_ttc_a >= 0 else 0)
            pro_ligne = to_dec(row[i_pro_a] if i_pro_a >= 0 else societe["prorata"], "100.00")
            date_paie = str(row[i_date_paie] if i_date_paie >= 0 and row[i_date_paie] else "")
            mode_paie = str(row[i_mode] if i_mode >= 0 and row[i_mode] else "VIREMENT")
            cpt = str(row[i_cpt] if i_cpt >= 0 and row[i_cpt] else "")

            # Exigibilité déduction (Régime encaissement -> payé, régime débit -> déductible)
            deductible = True
            if regime == "ENCAISSEMENT" and not date_paie:
                deductible = False

            if deductible:
                total_achats_ht += ht
                if "IMMO" in type_achat or cpt.startswith("2"):
                    total_tva_deductible_immo += tva
                else:
                    total_tva_deductible_charges += tva

            achats_lignes.append({
                "num_ordre": ordre_cpt,
                "num_facture": num_fac,
                "date_facture": date_fac,
                "fournisseur": nom_fr,
                "if": if_fr,
                "ice": ice_fr,
                "designation": desig,
                "type": type_achat,
                "ht": ht,
                "taux": taux,
                "tva": tva,
                "ttc": ttc,
                "prorata": pro_ligne,
                "date_paiement": date_paie,
                "mode_paiement": mode_paie,
                "compte": cpt,
                "deductible": deductible
            })
            ordre_cpt += 1

    # 4. Calcul de la liquidation TVA
    prorata = societe["prorata"]
    ratio_prorata = (prorata / Decimal("100")).quantize(Decimal("0.0001"))
    tva_charges_prorata = (total_tva_deductible_charges * ratio_prorata).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    tva_immo_prorata = (total_tva_deductible_immo * ratio_prorata).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    total_tva_deductible = tva_charges_prorata + tva_immo_prorata

    credit_anterieur = societe["credit_anterieur"]
    total_deductions_global = total_tva_deductible + credit_anterieur

    if total_tva_collectee >= total_deductions_global:
        tva_a_payer = total_tva_collectee - total_deductions_global
        credit_reportable = Decimal("0.00")
    else:
        tva_a_payer = Decimal("0.00")
        credit_reportable = total_deductions_global - total_tva_collectee

    resultats = {
        "societe": societe,
        "ventes_ht": total_ventes_ht,
        "ventes_exonerees_ht": total_ventes_exonerees_ht,
        "tva_collectee": total_tva_collectee,
        "achats_ht": total_achats_ht,
        "tva_deductible_charges": tva_charges_prorata,
        "tva_deductible_immo": tva_immo_prorata,
        "total_tva_deductible": total_tva_deductible,
        "credit_anterieur": credit_anterieur,
        "tva_a_payer": tva_a_payer,
        "credit_reportable": credit_reportable,
        "achats_details": achats_lignes,
        "ventes_details": ventes_lignes
    }

    return resultats


# ==============================================================================
# 3. GÉNÉRATEUR XML SIMPL-TVA (DGI MAROC)
# ==============================================================================

def generer_xml_simpl_tva(res, fichier_sortie):
    """Génère le fichier XML au format EDI SIMPL-TVA homologué DGI Maroc."""
    soc = res["societe"]
    periode = soc["periode"]
    parts = periode.split("-")
    annee = parts[0] if len(parts) > 0 else str(date.today().year)
    mois_ou_trim = parts[1] if len(parts) > 1 else "1"

    root = ET.Element("DeclarationTVA", xmlns="http://simpl.portail.dgi.gov.ma/tva")

    # En-tête
    entete = ET.SubElement(root, "Entete")
    ET.SubElement(entete, "IdentifiantFiscal").text = str(soc.get("if", ""))
    ET.SubElement(entete, "ICE").text = str(soc.get("ice", ""))
    ET.SubElement(entete, "RaisonSociale").text = str(soc.get("raison_sociale", ""))
    ET.SubElement(entete, "Annee").text = str(annee)
    ET.SubElement(entete, "Periode").text = str(mois_ou_trim)
    ET.SubElement(entete, "Regime").text = str(soc.get("regime", "ENCAISSEMENT"))
    ET.SubElement(entete, "Statut").text = "BROUILLON"
    ET.SubElement(entete, "DateGeneration").text = str(date.today())

    # Opérations imposables
    ops = ET.SubElement(root, "OperationsImposables")
    ET.SubElement(ops, "ChiffreAffairesTotalHT").text = f"{res['ventes_ht']:.2f}"
    ET.SubElement(ops, "ChiffreAffairesExonereHT").text = f"{res['ventes_exonerees_ht']:.2f}"
    ET.SubElement(ops, "TvaFacturee").text = f"{res['tva_collectee']:.2f}"

    # Déductions
    ded = ET.SubElement(root, "Deductions")
    ET.SubElement(ded, "AchatsHT").text = f"{res['achats_ht']:.2f}"
    ET.SubElement(ded, "TvaDeductibleCharges").text = f"{res['tva_deductible_charges']:.2f}"
    ET.SubElement(ded, "TvaDeductibleImmobilisations").text = f"{res['tva_deductible_immo']:.2f}"
    ET.SubElement(ded, "ProrataDeductionPourcentage").text = f"{soc['prorata']:.2f}"
    ET.SubElement(ded, "TotalTvaDeductible").text = f"{res['total_tva_deductible']:.2f}"
    ET.SubElement(ded, "CreditTvaPrecedentMois").text = f"{res['credit_anterieur']:.2f}"

    # Résultat Liquidation
    liquidation = ET.SubElement(root, "ResultatLiquidation")
    ET.SubElement(liquidation, "TvaDueAPayer").text = f"{res['tva_a_payer']:.2f}"
    ET.SubElement(liquidation, "CreditTvaReportable").text = f"{res['credit_reportable']:.2f}"

    xml_bytes = minidom.parseString(ET.tostring(root, encoding="utf-8")).toprettyxml(indent="  ")
    with open(fichier_sortie, "w", encoding="utf-8") as f:
        f.write(xml_bytes)

    print(f"[+] XML SIMPL-TVA généré : {fichier_sortie}")


# ==============================================================================
# 4. GÉNÉRATEUR RELEVÉ DES DÉDUCTIONS EXCEL (ARTICLE 112 CGI MAROC)
# ==============================================================================

def generer_releve_deductions_excel(res, fichier_sortie):
    """Crée le fichier Excel du Relevé de Déduction officiel conforme Article 112 CGI."""
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Relevé Déduction Art.112"

    soc = res["societe"]

    # Styles
    title_font = Font(name="Calibri", size=14, bold=True, color="1F497D")
    header_fill = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
    header_font = Font(name="Calibri", size=10, bold=True, color="FFFFFF")
    total_fill = PatternFill(start_color="C5D9F1", end_color="C5D9F1", fill_type="solid")
    total_font = Font(name="Calibri", size=10, bold=True)
    border_cell = Border(
        left=Side(style='thin', color='CCCCCC'),
        right=Side(style='thin', color='CCCCCC'),
        top=Side(style='thin', color='CCCCCC'),
        bottom=Side(style='thin', color='CCCCCC')
    )

    # En-tête fiscal
    ws.append(["ROYAUME DU MAROC - DIRECTION GÉNÉRALE DES IMPÔTS"])
    ws.append(["RELEVÉ DES DÉDUCTIONS DE TVA (Article 112 du Code Général des Impôts)"])
    ws.append([])
    ws.append(["Raison Sociale :", soc.get("raison_sociale", ""), "", "Période :", soc.get("periode", "")])
    ws.append(["Identifiant Fiscal :", soc.get("if", ""), "", "Régime :", soc.get("regime", "")])
    ws.append(["ICE :", soc.get("ice", ""), "", "Prorata :", f"{soc.get('prorata')}%"])
    ws.append([])

    ws["A1"].font = Font(bold=True, size=11, color="555555")
    ws["A2"].font = title_font

    # Tableau des colonnes
    headers = [
        "N° Ordre", "N° Facture", "Date Facture", "Fournisseur", "IF Fournisseur", "ICE",
        "Désignation", "Type", "Montant HT", "Taux TVA (%)", "Montant TVA", "Montant TTC",
        "Prorata", "TVA Déductible", "Mode Paiement", "Date Paiement"
    ]
    ws.append(headers)
    table_header_row = ws.max_row

    for col_num, cell in enumerate(ws[table_header_row], 1):
        cell.fill = header_fill
        cell.font = header_font
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        cell.border = border_cell

    # Données
    start_row = ws.max_row + 1
    for row in res["achats_details"]:
        tva_deductible = (row["tva"] * (row["prorata"] / Decimal("100"))).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
        ws.append([
            row["num_ordre"],
            row["num_facture"],
            row["date_facture"],
            row["fournisseur"],
            row["if"],
            row["ice"],
            row["designation"],
            row["type"],
            float(row["ht"]),
            float(row["taux"]),
            float(row["tva"]),
            float(row["ttc"]),
            float(row["prorata"]),
            float(tva_deductible),
            row["mode_paiement"],
            row["date_paiement"]
        ])

    end_row = ws.max_row

    # Formater les cellules
    for r in range(start_row, end_row + 1):
        for c in range(1, len(headers) + 1):
            cell = ws.cell(row=r, column=c)
            cell.border = border_cell
            if c in [9, 10, 11, 12, 13, 14]:
                cell.number_format = "#,##0.00"
                cell.alignment = Alignment(horizontal="right")

    # Ligne des Totaux
    total_row = [
        "TOTAL", "", "", "", "", "", "", "",
        f"=SUM(I{start_row}:I{end_row})",
        "",
        f"=SUM(K{start_row}:K{end_row})",
        f"=SUM(L{start_row}:L{end_row})",
        "",
        f"=SUM(N{start_row}:N{end_row})",
        "", ""
    ]
    ws.append(total_row)
    tot_row_idx = ws.max_row
    for cell in ws[tot_row_idx]:
        cell.fill = total_fill
        cell.font = total_font
        cell.border = border_cell
        if cell.column in [9, 11, 12, 14]:
            cell.number_format = "#,##0.00"
            cell.alignment = Alignment(horizontal="right")

    # Largeurs colonnes
    for col in ws.columns:
        max_len = max(len(str(cell.value or '')) for cell in col)
        col_letter = get_column_letter(col[0].column)
        ws.column_dimensions[col_letter].width = max(max_len + 3, 11)

    wb.save(fichier_sortie)
    print(f"[+] Relevé de déduction Excel officiel (Art. 112) généré : {fichier_sortie}")


# ==============================================================================
# 5. GÉNÉRATEUR RAPPORT DE SYNTHÈSE LIQUIDATION EXCEL
# ==============================================================================

def generer_synthese_liquidation_excel(res, fichier_sortie):
    """Génère la feuille de synthèse récapitulative du calcul de la déclaration TVA."""
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Synthèse Déclaration TVA"

    soc = res["societe"]

    title_font = Font(name="Calibri", size=13, bold=True, color="1F497D")
    bold_font = Font(name="Calibri", size=11, bold=True)
    border_cell = Border(
        left=Side(style='thin', color='DDDDDD'), right=Side(style='thin', color='DDDDDD'),
        top=Side(style='thin', color='DDDDDD'), bottom=Side(style='thin', color='DDDDDD')
    )
    fill_section = PatternFill(start_color="D9E1F2", end_color="D9E1F2", fill_type="solid")
    fill_result = PatternFill(start_color="E2EFDA", end_color="E2EFDA", fill_type="solid")

    ws.append(["RAPPORT DE LIQUIDATION DE LA TVA"])
    ws.append([f"Période : {soc.get('periode')} | Régime : {soc.get('regime')} | Société : {soc.get('raison_sociale')}"])
    ws.append([])

    elements = [
        ("1. OPÉRATIONS IMPOSABLES (VENTES)", None),
        ("Chiffre d'affaires taxable HT", float(res["ventes_ht"])),
        ("Chiffre d'affaires exonéré HT (Art. 92/94 CGI)", float(res["ventes_exonerees_ht"])),
        ("TVA Collectée (Exigible)", float(res["tva_collectee"])),
        ("2. DÉDUCTIONS (ACHATS)", None),
        ("Total Achats HT", float(res["achats_ht"])),
        ("TVA Déductible sur Charges (après prorata)", float(res["tva_deductible_charges"])),
        ("TVA Déductible sur Immobilisations", float(res["tva_deductible_immo"])),
        ("Prorata de déduction appliqué", f"{soc.get('prorata')}%"),
        ("TOTAL TVA DÉDUCTIBLE DE LA PÉRIODE", float(res["total_tva_deductible"])),
        ("3. REPORT DE CRÉDIT ANTÉRIEUR", None),
        ("Crédit de TVA du mois précédent", float(res["credit_anterieur"])),
        ("TOTAL GÉNÉRAL DÉDUCTIBLE", float(res["total_tva_deductible"] + res["credit_anterieur"])),
        ("4. LIQUIDATION FISCALE", None),
        ("TVA NETTE DUE À PAYER", float(res["tva_a_payer"])),
        ("CRÉDIT DE TVA REPORTABLE SUR LE MOIS SUIVANT", float(res["credit_reportable"]))
    ]

    for label, val in elements:
        if val is None:
            ws.append([])
            row = ws.append([label, ""])
            curr = ws.max_row
            ws.cell(row=curr, column=1).fill = fill_section
            ws.cell(row=curr, column=1).font = bold_font
            ws.cell(row=curr, column=2).fill = fill_section
        else:
            ws.append([label, val])
            curr = ws.max_row
            ws.cell(row=curr, column=1).border = border_cell
            c2 = ws.cell(row=curr, column=2)
            c2.border = border_cell
            if isinstance(val, (int, float)):
                c2.number_format = "#,##0.00 MAD"
                c2.alignment = Alignment(horizontal="right")
            if "NETTE DUE" in label or "REPORTABLE" in label:
                ws.cell(row=curr, column=1).font = bold_font
                c2.font = bold_font
                ws.cell(row=curr, column=1).fill = fill_result
                c2.fill = fill_result

    ws["A1"].font = title_font
    ws.column_dimensions["A"].width = 50
    ws.column_dimensions["B"].width = 25

    wb.save(fichier_sortie)
    print(f"[+] Rapport de synthèse Excel généré : {fichier_sortie}")


# ==============================================================================
# 6. POINT D'ENTRÉE CLI
# ==============================================================================

def main():
    parser = argparse.ArgumentParser(description="Traitement et Génération des déclarations TVA depuis un fichier Excel")
    parser.add_argument("--creer-modele", action="store_true", help="Génère un fichier Excel modèle de test avec des données complètes")
    parser.add_argument("--input", default="test_donnees_tva.xlsx", help="Chemin du fichier Excel d'entrée (par défaut: test_donnees_tva.xlsx)")
    parser.add_argument("--periode", default=None, help="Forcer la période (ex: 2024-03)")
    parser.add_argument("--regime", choices=["ENCAISSEMENT", "DEBIT"], default=None, help="Forcer le régime TVA")
    parser.add_argument("--prorata", type=float, default=None, help="Forcer le prorata en %")
    parser.add_argument("--output-dir", default="declarations_tva_output", help="Dossier de sortie des fichiers générés")

    args = parser.parse_args()

    if args.creer_modele or not os.path.exists(args.input):
        print("[*] Création du fichier Excel modèle d'exemple...")
        creer_fichier_excel_test(args.input)
        if args.creer_modele:
            print("[✓] Modèle prêt. Vous pouvez maintenant modifier ses valeurs ou lancer le traitement avec:")
            print(f"    python declaration_tva_excel.py --input {args.input}")
            return

    os.makedirs(args.output_dir, exist_ok=True)

    print(f"[*] Lecture et traitement du fichier Excel : {args.input}...")
    res = traiter_fichier_excel(
        args.input,
        regime_override=args.regime,
        periode_override=args.periode,
        prorata_override=args.prorata
    )

    periode = res["societe"]["periode"]

    # Affichage en console
    print("\n" + "=" * 65)
    print(f" BILAN DE DÉCLARATION TVA ({periode}) - {res['societe']['raison_sociale']}")
    print("=" * 65)
    print(f" 1. Ventes taxables HT       : {res['ventes_ht']:>14} MAD")
    print(f" 2. Ventes exonérées HT      : {res['ventes_exonerees_ht']:>14} MAD")
    print(f" 3. TVA Collectée            : {res['tva_collectee']:>14} MAD")
    print(f" 4. Achats HT                : {res['achats_ht']:>14} MAD")
    print(f" 5. TVA Déductible (Charges) : {res['tva_deductible_charges']:>14} MAD")
    print(f" 6. TVA Déductible (Immo)    : {res['tva_deductible_immo']:>14} MAD")
    print(f" 7. Total TVA Déductible     : {res['total_tva_deductible']:>14} MAD (Prorata: {res['societe']['prorata']}%)")
    print(f" 8. Crédit antérieur reporté : {res['credit_anterieur']:>14} MAD")
    print("-" * 65)
    if res['tva_a_payer'] > 0:
        print(f" 👉 TVA NETTE À PAYER        : {res['tva_a_payer']:>14} MAD")
    else:
        print(f" 👉 CRÉDIT DE TVA REPORTABLE : {res['credit_reportable']:>14} MAD")
    print("=" * 65 + "\n")

    # Génération des 3 fichiers de sortie
    xml_path = os.path.join(args.output_dir, f"SIMPL_TVA_{periode}.xml")
    releve_path = os.path.join(args.output_dir, f"Releve_Deductions_Art112_{periode}.xlsx")
    synthese_path = os.path.join(args.output_dir, f"Synthese_Declaration_TVA_{periode}.xlsx")

    generer_xml_simpl_tva(res, xml_path)
    generer_releve_deductions_excel(res, releve_path)
    generer_synthese_liquidation_excel(res, synthese_path)

    print("\n[✓] Tous les fichiers de déclaration ont été générés avec succès dans :", os.path.abspath(args.output_dir))


if __name__ == "__main__":
    main()
