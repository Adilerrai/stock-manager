-- =============================================================================
-- PLAN DE TEST INTEGRE COMMERCIAL & COMPTABILITE — EXERCICE 2025
-- Societe : STE ATLAS CERAM & DESIGN SARL
-- Periode : 01/01/2025 au 31/12/2025
-- Equilibre Comptable & Financier au centime pres (Ecart : 0.00 DH)
-- Total Bilan : 749 650.00 DH | Resultat Net : +137 150.00 DH
-- =============================================================================

BEGIN TRANSACTION;

-- =============================================================================
-- 1. STRUCTURE SOCIETAIRE & POINT DE VENTE
-- =============================================================================
INSERT INTO meres (id, nom, forme_juridique, ice, rc, identifiant_fiscal, patente, adresse, ville, telephone, email, actif, date_creation)
VALUES (0, 'Plateforme Globale (SuperAdmin)', 'SYSTEM', '000000000000000', '000000', '00000000', '00000000', 'Plateforme Cloud', 'Casablanca', '0000000000', 'admin@erp.com', TRUE, '2025-01-01')
ON CONFLICT (id) DO NOTHING;

INSERT INTO meres (id, nom, forme_juridique, ice, rc, identifiant_fiscal, patente, adresse, ville, telephone, email, actif, date_creation)
VALUES (1, 'STE ATLAS CERAM & DESIGN SARL', 'SARL', '002849102000045', '489201', '45892011', '34901820', '45 Boulevard Al Qods, Sidi Maârouf', 'Casablanca', '0522889900', 'contact@atlasceram.ma', TRUE, '2025-01-01')
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom, forme_juridique = EXCLUDED.forme_juridique, ice = EXCLUDED.ice, rc = EXCLUDED.rc, identifiant_fiscal = EXCLUDED.identifiant_fiscal, patente = EXCLUDED.patente, adresse = EXCLUDED.adresse, ville = EXCLUDED.ville;

INSERT INTO point_de_vente (id, mere_id, tenant_id, nom_point_de_vente, nom, adresse, telephone, email, actif, module_commercial_actif, module_comptabilite_actif, module_fiscalite_actif, date_creation)
VALUES (1, 1, 1, 'Dépôt Central Tit Mellil & Siège', 'STE ATLAS CERAM & DESIGN SARL', '45 Boulevard Al Qods, Sidi Maârouf, Casablanca', '0522889900', 'contact@atlasceram.ma', TRUE, TRUE, TRUE, TRUE, '2025-01-01')
ON CONFLICT (id) DO UPDATE SET nom_point_de_vente = EXCLUDED.nom_point_de_vente, nom = EXCLUDED.nom, adresse = EXCLUDED.adresse;

INSERT INTO roles (id, nom) VALUES (1, 'ROLE_SUPERADMIN'), (2, 'ROLE_ADMIN') ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, username, password, role_id, nom_complet, telephone, mere_id, tenant_id, point_de_vente_id, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES 
(1, 'superadmin@erp.com', 'superadmin', '$2a$10$UKxUQl0p59cpN0fLj7c0f.U5OMgv.wfqrqkvskSXoPaxoJ2HEMeFy', 1, 'Super Administrateur Plateforme', '+212 00 00 00 00', 0, 0, NULL, TRUE, TRUE, TRUE, TRUE),
(2, 'admin@atlasceram.ma', 'admin_atlas', '$2a$10$UKxUQl0p59cpN0fLj7c0f.U5OMgv.wfqrqkvskSXoPaxoJ2HEMeFy', 2, 'Adil Directoire - Atlas Ceram', '+212 522 88 99 00', 1, 1, 1, TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password, role_id = EXCLUDED.role_id, point_de_vente_id = EXCLUDED.point_de_vente_id;

INSERT INTO entreprise_profiles (id, point_de_vente_id, nom_entreprise, activite, adresse, ville, code_postal, telephone, email, registre_commerce, numero_identification_fiscale, numero_identification_statistique, article_imposition, compte_bancaire_rib, nom_banque, devise, vente_stock_negatif, date_mise_a_jour)
VALUES (1, 1, 'STE ATLAS CERAM & DESIGN SARL', 'Négoce et Vente de Céramique & Sanitaire', '45 Boulevard Al Qods, Sidi Maârouf', 'Casablanca', '20280', '0522889900', 'contact@atlasceram.ma', '489201', '45892011', '002849102000045', '34901820', '007780000123456789012345', 'Attijariwafa Bank', 'MAD', FALSE, '2025-01-01')
ON CONFLICT (id) DO UPDATE SET nom_entreprise = EXCLUDED.nom_entreprise, activite = EXCLUDED.activite, compte_bancaire_rib = EXCLUDED.compte_bancaire_rib, nom_banque = EXCLUDED.nom_banque;

INSERT INTO societes (id, mere_id, tenant_id, code, raison_sociale, forme_juridique, ice, rc, identifiant_fiscal, patente, adresse, ville, telephone, email, capital_social, regime_tva, periodicite_tva, exercice_en_cours, is_par_defaut, actif, date_creation)
VALUES (1, 1, 1, 'ATLAS', 'STE ATLAS CERAM & DESIGN SARL', 'SARL', '002849102000045', '489201', '45892011', '34901820', '45 Boulevard Al Qods, Sidi Maârouf', 'Casablanca', '0522889900', 'contact@atlasceram.ma', 500000.00, 'ENCAISSEMENT', 'TRIMESTRIELLE', 2025, TRUE, TRUE, '2025-01-01')
ON CONFLICT (id) DO UPDATE SET raison_sociale = EXCLUDED.raison_sociale, capital_social = EXCLUDED.capital_social;

-- =============================================================================
-- 2. DEPOT & CATEGORIES DE PRODUITS
-- =============================================================================
INSERT INTO depots (id, nom, description, adresse, actif, date_creation, point_de_vente_id)
VALUES (1, 'Dépôt Central Tit Mellil', 'Dépôt Principal de Stockage et Expédition (1 200 m²)', 'Zone Industrielle Tit Mellil, Casablanca', TRUE, '2025-01-01', 1)
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom, description = EXCLUDED.description, adresse = EXCLUDED.adresse;

INSERT INTO categories (id, code, nom, description, actif, date_creation, point_de_vente_id)
VALUES
(1, 'CARRELAGE', 'Carrelage & Grès', 'Carrelages sol et grès cérame', TRUE, '2025-01-01', 1),
(2, 'FAIENCE', 'Faïence & Revêtement Mural', 'Revêtements muraux et faïences décoratives', TRUE, '2025-01-01', 1),
(3, 'COLLES', 'Colles & Mortiers', 'Colles ciments, joints et additifs', TRUE, '2025-01-01', 1),
(4, 'OUTILLAGE', 'Outillage & Accessoires', 'Croisillons autonivelants et outillage carreleur', TRUE, '2025-01-01', 1)
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom, code = EXCLUDED.code;

-- =============================================================================
-- 3. LES 4 FOURNISSEURS
-- =============================================================================
INSERT INTO fournisseurs (id, raison_social, contact, telephone, email, adresse, ville, pays, ice, numero_registre_commerce, numero_identification_fiscale, patente, conditions_paiement, rib_bancaire, banque_nom, delai_paiement_jours, actif, date_creation, point_de_vente_id)
VALUES
(1, 'CERAMICA ATLAS INDUSTRIE SA', 'M. Tazi Karim', '0536601122', 'contact@ceramica-atlas.ma', 'Zone Industrielle Selouane', 'Nador', 'Maroc', '001598402000031', '39821', '33445566', '12345678', '30 jours fin de mois (Virement)', '022780000123456789012345', 'SGMB', 30, TRUE, '2025-01-05', 1),
(2, 'MAGHREB FAIENCE SARL', 'Mme. Amrani Nadia', '0522324455', 'commercial@maghrebfaience.ma', 'Zone Industrielle', 'Berrechid', 'Maroc', '001984215000088', '45812', '44556677', '23456789', '30 jours (Chèque à réception)', '011780000987654321098765', 'BMCE', 30, TRUE, '2025-01-05', 1),
(3, 'CHIMIE & MORTIERS DU NORD SARL', 'M. Berrada Omar', '0539398877', 'ventes@chimie-nord.ma', 'Tanger Free Zone', 'Tanger', 'Maroc', '002145896000072', '56981', '55667788', '34567890', '60 jours (Traite / Effet de commerce)', '007780000555666777888999', 'Attijariwafa Bank', 60, TRUE, '2025-01-05', 1),
(4, 'OUTILPRO DISTRIBUTION SA', 'M. Chraibi Reda', '0522667788', 'distribution@outilpro.ma', 'Bd Chefchaouni, Aïn Sebaâ', 'Casablanca', 'Maroc', '001874523000049', '67123', '66778899', '45678901', '30 jours (Virement partiel / comptant)', '013780000111222333444555', 'Banque Populaire', 30, TRUE, '2025-01-05', 1)
ON CONFLICT (id) DO UPDATE SET raison_social = EXCLUDED.raison_social, ice = EXCLUDED.ice, telephone = EXCLUDED.telephone, contact = EXCLUDED.contact;

-- =============================================================================
-- 4. LES 10 CLIENTS
-- =============================================================================
INSERT INTO clients (id, nom, prenom, nom_complet, telephone, email, adresse, ville, categorie, credit_autorise, credit_utilise, points_fidelite, ice, numero_registre_commerce, numero_identification_fiscale, delai_paiement_jours, actif, date_creation, point_de_vente_id)
VALUES
(1, 'Al Omrane', 'SA', 'PROMOTION IMMOBILIERE AL OMRANE SA', '0522998877', 'achats@alomrane.gov.ma', 'Avenue Annakhil, Hay Riad', 'Rabat', 'CHANTIER', 300000.00, 0.00, 150, '001245789000012', '129845', '11223344', 30, TRUE, '2025-01-10', 1),
(2, 'Bâtiment Moderne', 'SARL', 'BATIMENT MODERNE SARL', '0522441122', 'contact@batimod.ma', 'Bd Abdelmoumen', 'Casablanca', 'PROFESSIONNEL', 150000.00, 0.00, 80, '001654987000044', '134567', '22334455', 30, TRUE, '2025-01-10', 1),
(3, 'Travaux Atlantique', 'SARL', 'STE TRAVAUX ATLANTIQUE SARL', '0523312244', 'contact@travaux-atlantique.ma', 'Bd Mohammed V', 'El Jadida', 'ENTREPRISE', 120000.00, 0.00, 95, '001987321000055', '145678', '33445566', 30, TRUE, '2025-01-10', 1),
(4, 'Benani', 'Mehdi', 'CABINET ARCHITECTURE BENANI', '0522778899', 'm.benani@archibenani.ma', 'Boulevard d''Anfa', 'Casablanca', 'ARCHITECTE', 80000.00, 0.00, 60, '002365412000099', '156789', '44556677', 30, TRUE, '2025-01-10', 1),
(5, 'Design Intérieur', 'Studio', 'DESIGN INTERIEUR STUDIO SARL', '0524432211', 'studio@designinterieur.ma', 'Guéliz', 'Marrakech', 'ARCHITECTE', 60000.00, 0.00, 45, '002145789000063', '167890', '55667788', 30, TRUE, '2025-01-10', 1),
(6, 'El Fassi', 'Karim', 'ARTISAN POSEUR KARIM EL FASSI', '0661223344', 'karim.poseur@gmail.com', 'Derb Ghallef', 'Casablanca', 'PROFESSIONNEL', 40000.00, 0.00, 30, 'CIN: BE458921', '178901', '66778899', 0, TRUE, '2025-01-10', 1),
(7, 'Comptoir Finitions', 'Souss', 'COMPTOIR FINITIONS DU SOUSS SARL', '0528821133', 'souss.finition@menara.ma', 'Avenue Hassan II', 'Agadir', 'PROFESSIONNEL', 70000.00, 0.00, 70, '001784512000022', '189012', '77889900', 60, TRUE, '2025-01-10', 1),
(8, 'Tazi', 'Mohamed', 'MOHAMED TAZI', '0661998877', 'm.tazi@outlook.com', 'Villa 14, Californie', 'Casablanca', 'PARTICULIER', 20000.00, 0.00, 25, 'CIN: BK223401', '190123', '88990011', 0, TRUE, '2025-01-10', 1),
(9, 'El Alaoui', 'Fatima Zahra', 'FATIMA ZAHRA EL ALAOUI', '0663445566', 'fz.alaoui@yahoo.fr', 'Résidence Al Manar, Maarif', 'Casablanca', 'PARTICULIER', 0.00, 0.00, 15, 'CIN: A789456', '201234', '99001122', 0, TRUE, '2025-01-10', 1),
(10, 'Benjelloun', 'Yassine', 'YASSINE BENJELLOUN', '0661778899', 'y.benjelloun@gmail.com', 'Hay Riad', 'Rabat', 'PARTICULIER', 0.00, 10000.00, 20, 'CIN: C451278', '212345', '00112233', 0, TRUE, '2025-01-10', 1)
ON CONFLICT (id) DO UPDATE SET nom_complet = EXCLUDED.nom_complet, telephone = EXCLUDED.telephone, credit_utilise = EXCLUDED.credit_utilise;

-- =============================================================================
-- 5. LES 4 PRODUITS / ARTICLES
-- =============================================================================
INSERT INTO produits (id, reference, designation, description, prix_achat_ht, prix_vente_ht, prix_achat_ttc, prix_vente_ttc, prix_achat, prix_vente, unite_mesure_stock, groupe_article, categorie_id, stock_minimum, actif, date_creation, point_de_vente_id)
VALUES
(1, 'CAR-6060-GRIS', 'Grès Cérame 60x60 Anthracite', 'Grès cérame émaillé poli haute résistance 60x60 cm', 90.00, 150.00, 108.00, 180.00, 90.00, 150.00, 'M2', 'CARRELAGE_SOL', 1, 100.00, TRUE, '2025-01-01', 1),
(2, 'FAI-3060-BLNC', 'Faïence Murale 30x60 Blanc Brillant', 'Faïence murale pâte blanche rectifiée brillante 30x60 cm', 70.00, 120.00, 84.00, 144.00, 70.00, 120.00, 'M2', 'FAIENCE', 2, 80.00, TRUE, '2025-01-01', 1),
(3, 'COL-C2TE-25KG', 'Colle Ciment Haute Performance C2TE Sac 25kg', 'Mortier colle amélioré résistant au glissement et temps ouvert prolongé', 60.00, 95.00, 72.00, 114.00, 60.00, 95.00, 'SAC', 'COLLES', 3, 50.00, TRUE, '2025-01-01', 1),
(4, 'CROIS-AUTOL-KIT', 'Kit Croisillons Autonivelants 2mm', 'Kit complet 100 clips + 100 coins pour pose carrelage parfaite', 40.00, 75.00, 48.00, 90.00, 40.00, 75.00, 'PIECE', 'OUTILLAGE', 4, 30.00, TRUE, '2025-01-01', 1)
ON CONFLICT (id) DO UPDATE SET reference = EXCLUDED.reference, designation = EXCLUDED.designation, prix_achat_ht = EXCLUDED.prix_achat_ht, prix_vente_ht = EXCLUDED.prix_vente_ht;

-- =============================================================================
-- 6. STOCKS AU 31/12/2025 (VALORISATION CMP : 85 600.00 DH)
-- =============================================================================
INSERT INTO stocks (id, produit_id, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj)
VALUES
(1, 1, 280.00, 0.00, 100.00, '2025-12-31 18:00:00'),
(2, 2, 320.00, 0.00, 80.00, '2025-12-31 18:00:00'),
(3, 3, 440.00, 0.00, 50.00, '2025-12-31 18:00:00'),
(4, 4, 290.00, 0.00, 30.00, '2025-12-31 18:00:00')
ON CONFLICT (id) DO UPDATE SET quantite_disponible = EXCLUDED.quantite_disponible, quantite_reservee = EXCLUDED.quantite_reservee, derniere_maj = EXCLUDED.derniere_maj;

INSERT INTO mouvements_stock (id, produit_id, type_mouvement, quantite, quantite_avant, quantite_apres, reference_document, motif, date_mouvement, utilisateur, depot_id, point_de_vente_id)
VALUES
(1, 1, 'INVENTAIRE_ENTREE', 500.00, 0.00, 500.00, 'INV-2025-INIT', 'Stock initial au 01/01/2025', '2025-01-01 08:00:00', 'admin', 1, 1),
(2, 2, 'INVENTAIRE_ENTREE', 400.00, 0.00, 400.00, 'INV-2025-INIT', 'Stock initial au 01/01/2025', '2025-01-01 08:00:00', 'admin', 1, 1),
(3, 3, 'INVENTAIRE_ENTREE', 200.00, 0.00, 200.00, 'INV-2025-INIT', 'Stock initial au 01/01/2025', '2025-01-01 08:00:00', 'admin', 1, 1),
(4, 4, 'INVENTAIRE_ENTREE', 100.00, 0.00, 100.00, 'INV-2025-INIT', 'Stock initial au 01/01/2025', '2025-01-01 08:00:00', 'admin', 1, 1),
(5, 1, 'RECEPTION_ACHAT', 1000.00, 500.00, 1500.00, 'BLF-2025-001', 'Livraison F1 Ceramica Atlas', '2025-01-20 10:00:00', 'magasinier', 1, 1),
(6, 2, 'RECEPTION_ACHAT', 800.00, 400.00, 1200.00, 'BLF-2025-002', 'Livraison F2 Maghreb Faïence', '2025-03-15 11:00:00', 'magasinier', 1, 1),
(7, 3, 'RECEPTION_ACHAT', 600.00, 200.00, 800.00, 'BLF-2025-003', 'Livraison F3 Chimie & Mortiers', '2025-06-08 09:30:00', 'magasinier', 1, 1),
(8, 4, 'RECEPTION_ACHAT', 400.00, 100.00, 500.00, 'BLF-2025-004', 'Livraison F4 Outilpro Distribution', '2025-10-15 14:00:00', 'magasinier', 1, 1),
(9, 1, 'LIVRAISON_VENTE', 1220.00, 1500.00, 280.00, 'CUMUL-VENTES-2025', 'Cumul des sorties de ventes exercice 2025', '2025-12-31 17:00:00', 'vendeur', 1, 1),
(10, 2, 'LIVRAISON_VENTE', 880.00, 1200.00, 320.00, 'CUMUL-VENTES-2025', 'Cumul des sorties de ventes exercice 2025', '2025-12-31 17:00:00', 'vendeur', 1, 1),
(11, 3, 'LIVRAISON_VENTE', 360.00, 800.00, 440.00, 'CUMUL-VENTES-2025', 'Cumul des sorties de ventes exercice 2025', '2025-12-31 17:00:00', 'vendeur', 1, 1),
(12, 4, 'LIVRAISON_VENTE', 210.00, 500.00, 290.00, 'CUMUL-VENTES-2025', 'Cumul des sorties de ventes exercice 2025', '2025-12-31 17:00:00', 'vendeur', 1, 1)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 7. FACTURES D'ACHAT FOURNISSEURS & REGLEMENTS
-- =============================================================================
INSERT INTO factures_achat (id, numero_facture, date_facture, date_echeance, statut, montant_ht, montant_tva, montant_ttc, fournisseur_id, observations, point_de_vente_id)
VALUES
(1, 'FA-2025-001', '2025-01-15', '2025-02-15', 'PAYEE_TOTALEMENT', 90000.00, 18000.00, 108000.00, 1, 'Approvisionnement Carrelage Grès Q1', 1),
(2, 'FA-2025-002', '2025-03-10', '2025-04-10', 'PAYEE_TOTALEMENT', 56000.00, 11200.00, 67200.00, 2, 'Approvisionnement Faïence Blanche Q1', 1),
(3, 'FA-2025-003', '2025-06-05', '2025-08-05', 'PAYEE_TOTALEMENT', 36000.00, 7200.00, 43200.00, 3, 'Stock Colles & Mortiers C2TE Q2', 1),
(4, 'FA-2025-004', '2025-10-12', '2025-11-15', 'PAYEE_PARTIELLEMENT', 16000.00, 3200.00, 19200.00, 4, 'Outillage croisillons nivelants Q4', 1)
ON CONFLICT (id) DO UPDATE SET numero_facture = EXCLUDED.numero_facture, montant_ht = EXCLUDED.montant_ht, montant_ttc = EXCLUDED.montant_ttc, statut = EXCLUDED.statut;

INSERT INTO lignes_facture_achat (id, facture_achat_id, produit_id, quantite, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc)
VALUES
(1, 1, 1, 1000.00, 90.00, 20.00, 90000.00, 18000.00, 108000.00),
(2, 2, 2, 800.00, 70.00, 20.00, 56000.00, 11200.00, 67200.00),
(3, 3, 3, 600.00, 60.00, 20.00, 36000.00, 7200.00, 43200.00),
(4, 4, 4, 400.00, 40.00, 20.00, 16000.00, 3200.00, 19200.00)
ON CONFLICT (id) DO UPDATE SET quantite = EXCLUDED.quantite, prix_unitaire_ht = EXCLUDED.prix_unitaire_ht, montant_ttc = EXCLUDED.montant_ttc;

INSERT INTO reglements_fournisseur (id, numero_reglement, date_reglement, montant, mode_paiement, reference_paiement, facture_achat_id, point_de_vente_id)
VALUES
(1, 'REG-F-001', '2025-02-15', 108000.00, 'VIREMENT', 'VIR-ATLAS-01', 1, 1),
(2, 'REG-F-002', '2025-04-10', 67200.00, 'CHEQUE', 'CHQ-458712', 2, 1),
(3, 'REG-F-003', '2025-08-05', 43200.00, 'EFFET', 'TRAITE-TR-092', 3, 1),
(4, 'REG-F-004', '2025-11-15', 10000.00, 'VIREMENT', 'VIR-OUTIL-ACOMPTE', 4, 1)
ON CONFLICT (id) DO UPDATE SET montant = EXCLUDED.montant, mode_paiement = EXCLUDED.mode_paiement;

-- =============================================================================
-- 8. FACTURES DE VENTE CLIENTS & ENCAISSEMENTS
-- =============================================================================
INSERT INTO factures (id, numero_facture, date_facture, date_echeance, statut, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, montant_paye, montant_restant, client_id, emise_par_user_id, point_de_vente_id)
VALUES
(1, 'FAC-2025-001', '2025-01-25', '2025-02-25', 'PAYEE_TOTALEMENT', 69500.00, 13900.00, 83400.00, 0.00, 83400.00, 83400.00, 0.00, 1, 1, 1),
(2, 'FAC-2025-002', '2025-02-15', '2025-03-20', 'PAYEE_TOTALEMENT', 39750.00, 7950.00, 47700.00, 0.00, 47700.00, 47700.00, 0.00, 2, 1, 1),
(3, 'FAC-2025-003', '2025-03-10', '2025-04-10', 'PAYEE_TOTALEMENT', 45100.00, 9020.00, 54120.00, 0.00, 54120.00, 54120.00, 0.00, 3, 1, 1),
(4, 'FAC-2025-004', '2025-04-05', '2025-05-05', 'PAYEE_TOTALEMENT', 40500.00, 8100.00, 48600.00, 0.00, 48600.00, 48600.00, 0.00, 4, 1, 1),
(5, 'FAC-2025-005', '2025-05-20', '2025-06-20', 'PAYEE_TOTALEMENT', 27000.00, 5400.00, 32400.00, 0.00, 32400.00, 32400.00, 0.00, 5, 1, 1),
(6, 'FAC-2025-006', '2025-06-12', '2025-06-12', 'PAYEE_TOTALEMENT', 15900.00, 3180.00, 19080.00, 0.00, 19080.00, 19080.00, 0.00, 6, 1, 1),
(7, 'FAC-2025-007', '2025-07-18', '2025-09-18', 'PAYEE_TOTALEMENT', 48000.00, 9600.00, 57600.00, 0.00, 57600.00, 57600.00, 0.00, 7, 1, 1),
(8, 'FAC-2025-008', '2025-09-15', '2025-10-15', 'PAYEE_TOTALEMENT', 20300.00, 4060.00, 24360.00, 0.00, 24360.00, 24360.00, 0.00, 8, 1, 1),
(9, 'FAC-2025-009', '2025-11-10', '2025-11-10', 'PAYEE_TOTALEMENT', 11500.00, 2300.00, 13800.00, 0.00, 13800.00, 13800.00, 0.00, 9, 1, 1),
(10, 'FAC-2025-010', '2025-12-15', '2025-12-31', 'PAYEE_PARTIELLEMENT', 21000.00, 4200.00, 25200.00, 0.00, 25200.00, 15200.00, 10000.00, 10, 1, 1)
ON CONFLICT (id) DO UPDATE SET numero_facture = EXCLUDED.numero_facture, montant_ht = EXCLUDED.montant_ht, montant_final = EXCLUDED.montant_final, montant_paye = EXCLUDED.montant_paye, montant_restant = EXCLUDED.montant_restant, statut = EXCLUDED.statut;

INSERT INTO lignes_facture (id, facture_id, produit_id, designation, reference, quantite, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc)
VALUES
(1, 1, 1, 'Grès Cérame 60x60 Anthracite', 'CAR-6060-GRIS', 400.00, 150.00, 20.00, 60000.00, 12000.00, 72000.00),
(2, 1, 3, 'Colle Ciment Haute Performance C2TE Sac 25kg', 'COL-C2TE-25KG', 100.00, 95.00, 20.00, 9500.00, 1900.00, 11400.00),
(3, 2, 2, 'Faïence Murale 30x60 Blanc Brillant', 'FAI-3060-BLNC', 300.00, 120.00, 20.00, 36000.00, 7200.00, 43200.00),
(4, 2, 4, 'Kit Croisillons Autonivelants 2mm', 'CROIS-AUTOL-KIT', 50.00, 75.00, 20.00, 3750.00, 750.00, 4500.00),
(5, 3, 1, 'Grès Cérame 60x60 Anthracite', 'CAR-6060-GRIS', 250.00, 150.00, 20.00, 37500.00, 7500.00, 45000.00),
(6, 3, 3, 'Colle Ciment Haute Performance C2TE Sac 25kg', 'COL-C2TE-25KG', 80.00, 95.00, 20.00, 7600.00, 1520.00, 9120.00),
(7, 4, 1, 'Grès Cérame 60x60 Anthracite', 'CAR-6060-GRIS', 150.00, 150.00, 20.00, 22500.00, 4500.00, 27000.00),
(8, 4, 2, 'Faïence Murale 30x60 Blanc Brillant', 'FAI-3060-BLNC', 150.00, 120.00, 20.00, 18000.00, 3600.00, 21600.00),
(9, 5, 2, 'Faïence Murale 30x60 Blanc Brillant', 'FAI-3060-BLNC', 200.00, 120.00, 20.00, 24000.00, 4800.00, 28800.00),
(10, 5, 4, 'Kit Croisillons Autonivelants 2mm', 'CROIS-AUTOL-KIT', 40.00, 75.00, 20.00, 3000.00, 600.00, 3600.00),
(11, 6, 3, 'Colle Ciment Haute Performance C2TE Sac 25kg', 'COL-C2TE-25KG', 120.00, 95.00, 20.00, 11400.00, 2280.00, 13680.00),
(12, 6, 4, 'Kit Croisillons Autonivelants 2mm', 'CROIS-AUTOL-KIT', 60.00, 75.00, 20.00, 4500.00, 900.00, 5400.00),
(13, 7, 1, 'Grès Cérame 60x60 Anthracite', 'CAR-6060-GRIS', 200.00, 150.00, 20.00, 30000.00, 6000.00, 36000.00),
(14, 7, 2, 'Faïence Murale 30x60 Blanc Brillant', 'FAI-3060-BLNC', 150.00, 120.00, 20.00, 18000.00, 3600.00, 21600.00),
(15, 8, 1, 'Grès Cérame 60x60 Anthracite', 'CAR-6060-GRIS', 100.00, 150.00, 20.00, 15000.00, 3000.00, 18000.00),
(16, 8, 3, 'Colle Ciment Haute Performance C2TE Sac 25kg', 'COL-C2TE-25KG', 40.00, 95.00, 20.00, 3800.00, 760.00, 4560.00),
(17, 8, 4, 'Kit Croisillons Autonivelants 2mm', 'CROIS-AUTOL-KIT', 20.00, 75.00, 20.00, 1500.00, 300.00, 1800.00),
(18, 9, 2, 'Faïence Murale 30x60 Blanc Brillant', 'FAI-3060-BLNC', 80.00, 120.00, 20.00, 9600.00, 1920.00, 11520.00),
(19, 9, 3, 'Colle Ciment Haute Performance C2TE Sac 25kg', 'COL-C2TE-25KG', 20.00, 95.00, 20.00, 1900.00, 380.00, 2280.00),
(20, 10, 1, 'Grès Cérame 60x60 Anthracite', 'CAR-6060-GRIS', 120.00, 150.00, 20.00, 18000.00, 3600.00, 21600.00),
(21, 10, 4, 'Kit Croisillons Autonivelants 2mm', 'CROIS-AUTOL-KIT', 40.00, 75.00, 20.00, 3000.00, 600.00, 3600.00)
ON CONFLICT (id) DO UPDATE SET quantite = EXCLUDED.quantite, prix_unitaire_ht = EXCLUDED.prix_unitaire_ht, montant_ttc = EXCLUDED.montant_ttc;

INSERT INTO paiements (id, numero_paiement, date_paiement, montant, mode_paiement, reference_paiement, facture_id, client_id, encaisse_par_user_id, point_de_vente_id)
VALUES
(1, 'ENC-C-001', '2025-02-25', 83400.00, 'VIREMENT', 'VIR-ALOMRANE-88', 1, 1, 1, 1),
(2, 'ENC-C-002', '2025-03-20', 47700.00, 'CHEQUE', 'CHQ-883012', 2, 2, 1, 1),
(3, 'ENC-C-003', '2025-04-10', 54120.00, 'VIREMENT', 'VIR-ATLANTIQUE-03', 3, 3, 1, 1),
(4, 'ENC-C-004', '2025-05-05', 48600.00, 'VIREMENT', 'VIR-BENANI-04', 4, 4, 1, 1),
(5, 'ENC-C-005', '2025-06-20', 32400.00, 'CHEQUE', 'CHQ-554210', 5, 5, 1, 1),
(6, 'ENC-C-006', '2025-06-12', 19080.00, 'ESPECES', 'CASH-COMPTOIR-6', 6, 6, 1, 1),
(7, 'ENC-C-007', '2025-09-18', 57600.00, 'EFFET', 'TRAITE-SOUSS-77', 7, 7, 1, 1),
(8, 'ENC-C-008', '2025-10-15', 24360.00, 'CHEQUE', 'CHQ-109283', 8, 8, 1, 1),
(9, 'ENC-C-009', '2025-11-10', 13800.00, 'ESPECES', 'CASH-COMPTOIR-9', 9, 9, 1, 1),
(10, 'ENC-C-010', '2025-12-20', 15200.00, 'VIREMENT', 'VIR-BENJELLOUN-ACOMPTE', 10, 10, 1, 1)
ON CONFLICT (id) DO UPDATE SET montant = EXCLUDED.montant, reference_paiement = EXCLUDED.reference_paiement;

INSERT INTO paiement_affectations (id, paiement_id, facture_id, montant_affecte, date_affectation)
VALUES
(1, 1, 1, 83400.00, '2025-02-25 12:00:00'),
(2, 2, 2, 47700.00, '2025-03-20 12:00:00'),
(3, 3, 3, 54120.00, '2025-04-10 12:00:00'),
(4, 4, 4, 48600.00, '2025-05-05 12:00:00'),
(5, 5, 5, 32400.00, '2025-06-20 12:00:00'),
(6, 6, 6, 19080.00, '2025-06-12 12:00:00'),
(7, 7, 7, 57600.00, '2025-09-18 12:00:00'),
(8, 8, 8, 24360.00, '2025-10-15 12:00:00'),
(9, 9, 9, 13800.00, '2025-11-10 12:00:00'),
(10, 10, 10, 15200.00, '2025-12-20 12:00:00')
ON CONFLICT (id) DO UPDATE SET montant_affecte = EXCLUDED.montant_affecte;

-- =============================================================================
-- 9. TRESORERIE : COMPTES FINANCIERS & MOUVEMENTS
-- =============================================================================
INSERT INTO comptes_financiers (id, code, nom, type, numero_compte_rib, nom_banque, solde_actuel, devise, actif, date_creation, point_de_vente_id)
VALUES
(1, 'BQ-AWB', 'Attijariwafa Bank - Compte Courant', 'BANQUE', '007780000123456789012345', 'Attijariwafa Bank', 410170.00, 'MAD', TRUE, '2025-01-01', 1),
(2, 'CA-CENT', 'Caisse Centrale Siège', 'CAISSE', 'CAISSE-MAD-01', 'Caisse Centrale', 57880.00, 'MAD', TRUE, '2025-01-01', 1)
ON CONFLICT (id) DO UPDATE SET solde_actuel = EXCLUDED.solde_actuel;

-- =============================================================================
-- 10. COMPTABILITE : EXERCICE, JOURNAUX & PLAN DE COMPTES (PCGM)
-- =============================================================================
INSERT INTO exercices_comptables (id, code, libelle, date_debut, date_fin, statut, point_de_vente_id)
VALUES (1, 'EXO-2025', 'Exercice Comptable 2025', '2025-01-01', '2025-12-31', 'OUVERT', 1)
ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

INSERT INTO journaux_comptables (id, code, libelle, type_journal, actif, point_de_vente_id)
VALUES
(1, 'AN', 'Journal des À-Nouveaux', 'A_NOUVEAUX', TRUE, 1),
(2, 'AC', 'Journal des Achats', 'ACHATS', TRUE, 1),
(3, 'VE', 'Journal des Ventes', 'VENTES', TRUE, 1),
(4, 'BQ', 'Journal de Banque Attijariwafa', 'BANQUE', TRUE, 1),
(5, 'CA', 'Journal de Caisse Centrale', 'CAISSE', TRUE, 1),
(6, 'OD', 'Journal des Opérations Diverses', 'OPERATIONS_DIVERSES', TRUE, 1)
ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle, type_journal = EXCLUDED.type_journal;

INSERT INTO comptes_comptables (id, numero_compte, libelle, classe, sens_par_defaut, actif, point_de_vente_id)
VALUES
(1, '11110000', 'Capital social', 1, 'CREDIT', TRUE, 1),
(2, '11610000', 'Report à nouveau créditeur', 1, 'CREDIT', TRUE, 1),
(3, '11910000', 'Résultat net de l''exercice (bénéfice)', 1, 'CREDIT', TRUE, 1),
(4, '23320000', 'Matériel et outillage', 2, 'DEBIT', TRUE, 1),
(5, '23400000', 'Matériel de transport', 2, 'DEBIT', TRUE, 1),
(6, '31110000', 'Marchandises (Stock)', 3, 'DEBIT', TRUE, 1),
(7, '34210000', 'Clients (Compte collectif)', 3, 'DEBIT', TRUE, 1),
(8, '34210001', 'Client Al Omrane SA', 3, 'DEBIT', TRUE, 1),
(9, '34210002', 'Client Bâtiment Moderne SARL', 3, 'DEBIT', TRUE, 1),
(10, '34210003', 'Client Ste Travaux Atlantique SARL', 3, 'DEBIT', TRUE, 1),
(11, '34210004', 'Client Cabinet Architecture Benani', 3, 'DEBIT', TRUE, 1),
(12, '34210005', 'Client Design Intérieur Studio SARL', 3, 'DEBIT', TRUE, 1),
(13, '34210006', 'Client Artisan Karim El Fassi', 3, 'DEBIT', TRUE, 1),
(14, '34210007', 'Client Comptoir Finitions du Souss SARL', 3, 'DEBIT', TRUE, 1),
(15, '34210008', 'Client Mohamed Tazi', 3, 'DEBIT', TRUE, 1),
(16, '34210009', 'Cliente Fatima Zahra El Alaoui', 3, 'DEBIT', TRUE, 1),
(17, '34210010', 'Client Yassine Benjelloun', 3, 'DEBIT', TRUE, 1),
(18, '34552000', 'État TVA récupérable sur charges', 3, 'DEBIT', TRUE, 1),
(19, '44110000', 'Fournisseurs (Compte collectif)', 4, 'CREDIT', TRUE, 1),
(20, '44110001', 'Fournisseur Ceramica Atlas Industrie SA', 4, 'CREDIT', TRUE, 1),
(21, '44110002', 'Fournisseur Maghreb Faïence SARL', 4, 'CREDIT', TRUE, 1),
(22, '44110003', 'Fournisseur Chimie & Mortiers du Nord SARL', 4, 'CREDIT', TRUE, 1),
(23, '44110004', 'Fournisseur Outilpro Distribution SA', 4, 'CREDIT', TRUE, 1),
(24, '44550000', 'État TVA facturée', 4, 'CREDIT', TRUE, 1),
(25, '44560000', 'État TVA due', 4, 'CREDIT', TRUE, 1),
(26, '51410000', 'Banque Attijariwafa Bank', 5, 'DEBIT', TRUE, 1),
(27, '51610000', 'Caisse Centrale', 5, 'DEBIT', TRUE, 1),
(28, '61110000', 'Achats de marchandises', 6, 'DEBIT', TRUE, 1),
(29, '61140000', 'Variation des stocks de marchandises', 6, 'DEBIT', TRUE, 1),
(30, '71110000', 'Ventes de marchandises', 7, 'CREDIT', TRUE, 1)
ON CONFLICT (id) DO UPDATE SET numero_compte = EXCLUDED.numero_compte, libelle = EXCLUDED.libelle;

-- =============================================================================
-- 11. ECRITURES COMPTABLES ET LIGNES D'ECRITURES (BALANCE RIGORISTE)
-- =============================================================================
INSERT INTO ecritures_comptables (id, numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
VALUES
(1, 'AN-2025-001', '2025-01-01', 'Bilan d''ouverture exercice 2025', 'AN-INIT', 600000.00, 600000.00, TRUE, 1, 1),
(2, 'FA-2025-001', '2025-01-15', 'Achat P1 Ceramica Atlas SA', 'FA-2025-001', 108000.00, 108000.00, TRUE, 2, 1),
(3, 'FA-2025-002', '2025-03-10', 'Achat P2 Maghreb Faïence SARL', 'FA-2025-002', 67200.00, 67200.00, TRUE, 2, 1),
(4, 'FA-2025-003', '2025-06-05', 'Achat P3 Chimie & Mortiers SARL', 'FA-2025-003', 43200.00, 43200.00, TRUE, 2, 1),
(5, 'FA-2025-004', '2025-10-12', 'Achat P4 Outilpro Distribution SA', 'FA-2025-004', 19200.00, 19200.00, TRUE, 2, 1),
(6, 'FAC-2025-001', '2025-01-25', 'Vente P1 + P3 Client Al Omrane SA', 'FAC-2025-001', 83400.00, 83400.00, TRUE, 3, 1),
(7, 'FAC-2025-002', '2025-02-15', 'Vente P2 + P4 Bâtiment Moderne', 'FAC-2025-002', 47700.00, 47700.00, TRUE, 3, 1),
(8, 'FAC-2025-003', '2025-03-10', 'Vente P1 + P3 Travaux Atlantique', 'FAC-2025-003', 54120.00, 54120.00, TRUE, 3, 1),
(9, 'FAC-2025-004', '2025-04-05', 'Vente P1 + P2 Architecture Benani', 'FAC-2025-004', 48600.00, 48600.00, TRUE, 3, 1),
(10, 'FAC-2025-005', '2025-05-20', 'Vente P2 + P4 Design Intérieur Studio', 'FAC-2025-005', 32400.00, 32400.00, TRUE, 3, 1),
(11, 'FAC-2025-006', '2025-06-12', 'Vente P3 + P4 Artisan Karim', 'FAC-2025-006', 19080.00, 19080.00, TRUE, 3, 1),
(12, 'FAC-2025-007', '2025-07-18', 'Vente P1 + P2 Comptoir Souss', 'FAC-2025-007', 57600.00, 57600.00, TRUE, 3, 1),
(13, 'FAC-2025-008', '2025-09-15', 'Vente P1+P3+P4 Mohamed Tazi', 'FAC-2025-008', 24360.00, 24360.00, TRUE, 3, 1),
(14, 'FAC-2025-009', '2025-11-10', 'Vente P2 + P3 Fatima Zahra', 'FAC-2025-009', 13800.00, 13800.00, TRUE, 3, 1),
(15, 'FAC-2025-010', '2025-12-15', 'Vente P1 + P4 Yassine Benjelloun', 'FAC-2025-010', 25200.00, 25200.00, TRUE, 3, 1),
(16, 'BQ-REG-001', '2025-02-15', 'Règlement F1 Ceramica Atlas (VIR)', 'REG-F-001', 108000.00, 108000.00, TRUE, 4, 1),
(17, 'BQ-ENC-001', '2025-02-25', 'Encaissement C1 Al Omrane (VIR)', 'ENC-C-001', 83400.00, 83400.00, TRUE, 4, 1),
(18, 'BQ-ENC-002', '2025-03-20', 'Encaissement C2 Bâtiment Moderne (CHQ)', 'ENC-C-002', 47700.00, 47700.00, TRUE, 4, 1),
(19, 'BQ-REG-002', '2025-04-10', 'Règlement F2 Maghreb Faïence (CHQ)', 'REG-F-002', 67200.00, 67200.00, TRUE, 4, 1),
(20, 'BQ-ENC-003', '2025-04-10', 'Encaissement C3 Travaux Atlantique (VIR)', 'ENC-C-003', 54120.00, 54120.00, TRUE, 4, 1),
(21, 'BQ-TVA-T1', '2025-04-20', 'Paiement TVA due T1', 'TVA-T1', 1670.00, 1670.00, TRUE, 4, 1),
(22, 'BQ-ENC-004', '2025-05-05', 'Encaissement C4 Architecture Benani (VIR)', 'ENC-C-004', 48600.00, 48600.00, TRUE, 4, 1),
(23, 'BQ-ENC-005', '2025-06-20', 'Encaissement C5 Design Intérieur (CHQ)', 'ENC-C-005', 32400.00, 32400.00, TRUE, 4, 1),
(24, 'BQ-TVA-T2', '2025-07-20', 'Paiement TVA due T2', 'TVA-T2', 9480.00, 9480.00, TRUE, 4, 1),
(25, 'BQ-REG-003', '2025-08-05', 'Règlement F3 Chimie & Mortiers (EFFET)', 'REG-F-003', 43200.00, 43200.00, TRUE, 4, 1),
(26, 'BQ-ENC-007', '2025-09-18', 'Encaissement C7 Comptoir Souss (EFFET)', 'ENC-C-007', 57600.00, 57600.00, TRUE, 4, 1),
(27, 'BQ-ENC-008', '2025-10-15', 'Encaissement C8 Mohamed Tazi (CHQ)', 'ENC-C-008', 24360.00, 24360.00, TRUE, 4, 1),
(28, 'BQ-TVA-T3', '2025-10-20', 'Paiement TVA due T3', 'TVA-T3', 13660.00, 13660.00, TRUE, 4, 1),
(29, 'BQ-REG-004', '2025-11-15', 'Acompte F4 Outilpro (VIR)', 'REG-F-004', 10000.00, 10000.00, TRUE, 4, 1),
(30, 'BQ-ENC-010', '2025-12-20', 'Acompte C10 Benjelloun (VIR)', 'ENC-C-010', 15200.00, 15200.00, TRUE, 4, 1),
(31, 'CA-ENC-006', '2025-06-12', 'Encaissement C6 Karim (ESPECES)', 'ENC-C-006', 19080.00, 19080.00, TRUE, 5, 1),
(32, 'CA-ENC-009', '2025-11-10', 'Encaissement C9 Fatima Zahra (ESPECES)', 'ENC-C-009', 13800.00, 13800.00, TRUE, 5, 1),
(33, 'OD-TVA-T1', '2025-03-31', 'Liquidation TVA Trimestre 1', 'LIQ-TVA-T1', 30870.00, 30870.00, TRUE, 6, 1),
(34, 'OD-TVA-T2', '2025-06-30', 'Liquidation TVA Trimestre 2', 'LIQ-TVA-T2', 16680.00, 16680.00, TRUE, 6, 1),
(35, 'OD-TVA-T3', '2025-09-30', 'Liquidation TVA Trimestre 3', 'LIQ-TVA-T3', 13660.00, 13660.00, TRUE, 6, 1),
(36, 'OD-TVA-T4', '2025-12-31', 'Liquidation TVA Trimestre 4', 'LIQ-TVA-T4', 6500.00, 6500.00, TRUE, 6, 1),
(37, 'OD-INV-001', '2025-12-31', 'Annulation du stock initial au 01/01/2025', 'CLOT-INV-INIT', 89000.00, 89000.00, TRUE, 6, 1),
(38, 'OD-INV-002', '2025-12-31', 'Constatation du stock final au 31/12/2025', 'CLOT-INV-FIN', 85600.00, 85600.00, TRUE, 6, 1)
ON CONFLICT (id) DO UPDATE SET numero_piece = EXCLUDED.numero_piece, libelle = EXCLUDED.libelle, total_debit = EXCLUDED.total_debit, total_credit = EXCLUDED.total_credit;

INSERT INTO lignes_ecriture (id, ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id)
VALUES
(1, 1, 4, 120000.00, 0.00, 'Matériel et outillage', NULL, 1),
(2, 1, 5, 66000.00, 0.00, 'Matériel de transport', NULL, 1),
(3, 1, 6, 89000.00, 0.00, 'Stock initial marchandises', NULL, 1),
(4, 1, 26, 300000.00, 0.00, 'Solde initial Banque', NULL, 1),
(5, 1, 27, 25000.00, 0.00, 'Solde initial Caisse', NULL, 1),
(6, 1, 1, 0.00, 500000.00, 'Capital social', NULL, 1),
(7, 1, 2, 0.00, 100000.00, 'Report à nouveau créditeur', NULL, 1),
(8, 2, 28, 90000.00, 0.00, 'Achats marchandises P1', NULL, 1),
(9, 2, 18, 18000.00, 0.00, 'TVA récupérable 20%', NULL, 1),
(10, 2, 20, 0.00, 108000.00, 'Fournisseur Ceramica Atlas', 'FA', 1),
(11, 3, 28, 56000.00, 0.00, 'Achats marchandises P2', NULL, 1),
(12, 3, 18, 11200.00, 0.00, 'TVA récupérable 20%', NULL, 1),
(13, 3, 21, 0.00, 67200.00, 'Fournisseur Maghreb Faïence', 'FB', 1),
(14, 4, 28, 36000.00, 0.00, 'Achats marchandises P3', NULL, 1),
(15, 4, 18, 7200.00, 0.00, 'TVA récupérable 20%', NULL, 1),
(16, 4, 22, 0.00, 43200.00, 'Fournisseur Chimie & Mortiers', 'FC', 1),
(17, 5, 28, 16000.00, 0.00, 'Achats marchandises P4', NULL, 1),
(18, 5, 18, 3200.00, 0.00, 'TVA récupérable 20%', NULL, 1),
(19, 5, 23, 0.00, 19200.00, 'Fournisseur Outilpro Distribution', NULL, 1),
(20, 6, 8, 83400.00, 0.00, 'Client Al Omrane SA', 'CA', 1),
(21, 6, 30, 0.00, 69500.00, 'Ventes marchandises P1+P3', NULL, 1),
(22, 6, 24, 0.00, 13900.00, 'TVA facturée 20%', NULL, 1),
(23, 7, 9, 47700.00, 0.00, 'Client Bâtiment Moderne', 'CB', 1),
(24, 7, 30, 0.00, 39750.00, 'Ventes marchandises P2+P4', NULL, 1),
(25, 7, 24, 0.00, 7950.00, 'TVA facturée 20%', NULL, 1),
(26, 8, 10, 54120.00, 0.00, 'Client Travaux Atlantique', 'CC', 1),
(27, 8, 30, 0.00, 45100.00, 'Ventes marchandises P1+P3', NULL, 1),
(28, 8, 24, 0.00, 9020.00, 'TVA facturée 20%', NULL, 1),
(29, 9, 11, 48600.00, 0.00, 'Client Architecture Benani', 'CD', 1),
(30, 9, 30, 0.00, 40500.00, 'Ventes marchandises P1+P2', NULL, 1),
(31, 9, 24, 0.00, 8100.00, 'TVA facturée 20%', NULL, 1),
(32, 10, 12, 32400.00, 0.00, 'Client Design Intérieur Studio', 'CE', 1),
(33, 10, 30, 0.00, 27000.00, 'Ventes marchandises P2+P4', NULL, 1),
(34, 10, 24, 0.00, 5400.00, 'TVA facturée 20%', NULL, 1),
(35, 11, 13, 19080.00, 0.00, 'Client Artisan Karim', 'CF', 1),
(36, 11, 30, 0.00, 15900.00, 'Ventes marchandises P3+P4', NULL, 1),
(37, 11, 24, 0.00, 3180.00, 'TVA facturée 20%', NULL, 1),
(38, 12, 14, 57600.00, 0.00, 'Client Comptoir Souss', 'CG', 1),
(39, 12, 30, 0.00, 48000.00, 'Ventes marchandises P1+P2', NULL, 1),
(40, 12, 24, 0.00, 9600.00, 'TVA facturée 20%', NULL, 1),
(41, 13, 15, 24360.00, 0.00, 'Client Mohamed Tazi', 'CH', 1),
(42, 13, 30, 0.00, 20300.00, 'Ventes marchandises P1+P3+P4', NULL, 1),
(43, 13, 24, 0.00, 4060.00, 'TVA facturée 20%', NULL, 1),
(44, 14, 16, 13800.00, 0.00, 'Cliente Fatima Zahra', 'CI', 1),
(45, 14, 30, 0.00, 11500.00, 'Ventes marchandises P2+P3', NULL, 1),
(46, 14, 24, 0.00, 2300.00, 'TVA facturée 20%', NULL, 1),
(47, 15, 17, 25200.00, 0.00, 'Client Yassine Benjelloun', NULL, 1),
(48, 15, 30, 0.00, 21000.00, 'Ventes marchandises P1+P4', NULL, 1),
(49, 15, 24, 0.00, 4200.00, 'TVA facturée 20%', NULL, 1),
(50, 16, 20, 108000.00, 0.00, 'Règlement F1 Ceramica Atlas', 'FA', 1),
(51, 16, 26, 0.00, 108000.00, 'Virement bancaire émis', NULL, 1),
(52, 17, 26, 83400.00, 0.00, 'Virement reçu C1 Al Omrane', NULL, 1),
(53, 17, 8, 0.00, 83400.00, 'Règlement FAC-2025-001', 'CA', 1),
(54, 18, 26, 47700.00, 0.00, 'Chèque reçu C2 Bâtiment Moderne', NULL, 1),
(55, 18, 9, 0.00, 47700.00, 'Règlement FAC-2025-002', 'CB', 1),
(56, 19, 21, 67200.00, 0.00, 'Chèque émis F2 Maghreb Faïence', 'FB', 1),
(57, 19, 26, 0.00, 67200.00, 'Débit bancaire chèque 458712', NULL, 1),
(58, 20, 26, 54120.00, 0.00, 'Virement reçu C3 Travaux Atlantique', NULL, 1),
(59, 20, 10, 0.00, 54120.00, 'Règlement FAC-2025-003', 'CC', 1),
(60, 21, 25, 1670.00, 0.00, 'Paiement État TVA due T1', NULL, 1),
(61, 21, 26, 0.00, 1670.00, 'Virement fiscal DGI', NULL, 1),
(62, 22, 26, 48600.00, 0.00, 'Virement reçu C4 Architecture Benani', NULL, 1),
(63, 22, 11, 0.00, 48600.00, 'Règlement FAC-2025-004', 'CD', 1),
(64, 23, 26, 32400.00, 0.00, 'Chèque reçu C5 Design Intérieur', NULL, 1),
(65, 23, 12, 0.00, 32400.00, 'Règlement FAC-2025-005', 'CE', 1),
(66, 24, 25, 9480.00, 0.00, 'Paiement État TVA due T2', NULL, 1),
(67, 24, 26, 0.00, 9480.00, 'Virement fiscal DGI', NULL, 1),
(68, 25, 22, 43200.00, 0.00, 'Paiement Traite F3 Chimie & Mortiers', 'FC', 1),
(69, 25, 26, 0.00, 43200.00, 'Débit bancaire traite TR-092', NULL, 1),
(70, 26, 26, 57600.00, 0.00, 'Encaissement Traite C7 Comptoir Souss', NULL, 1),
(71, 26, 14, 0.00, 57600.00, 'Règlement FAC-2025-007', 'CG', 1),
(72, 27, 26, 24360.00, 0.00, 'Chèque reçu C8 Mohamed Tazi', NULL, 1),
(73, 27, 15, 0.00, 24360.00, 'Règlement FAC-2025-008', 'CH', 1),
(74, 28, 25, 13660.00, 0.00, 'Paiement État TVA due T3', NULL, 1),
(75, 28, 26, 0.00, 13660.00, 'Virement fiscal DGI', NULL, 1),
(76, 29, 23, 10000.00, 0.00, 'Acompte virement F4 Outilpro', NULL, 1),
(77, 29, 26, 0.00, 10000.00, 'Virement bancaire émis', NULL, 1),
(78, 30, 26, 15200.00, 0.00, 'Acompte virement C10 Benjelloun', NULL, 1),
(79, 30, 17, 0.00, 15200.00, 'Règlement partiel FAC-2025-010', NULL, 1),
(80, 31, 27, 19080.00, 0.00, 'Encaissement espèces C6 Artisan Karim', NULL, 1),
(81, 31, 13, 0.00, 19080.00, 'Règlement FAC-2025-006', 'CF', 1),
(82, 32, 27, 13800.00, 0.00, 'Encaissement espèces C9 Fatima Zahra', NULL, 1),
(83, 32, 16, 0.00, 13800.00, 'Règlement FAC-2025-009', 'CI', 1),
(84, 33, 24, 30870.00, 0.00, 'Solde TVA facturée T1', NULL, 1),
(85, 33, 18, 0.00, 29200.00, 'Solde TVA récupérable T1', NULL, 1),
(86, 33, 25, 0.00, 1670.00, 'Constatation TVA due T1', NULL, 1),
(87, 34, 24, 16680.00, 0.00, 'Solde TVA facturée T2', NULL, 1),
(88, 34, 18, 0.00, 7200.00, 'Solde TVA récupérable T2', NULL, 1),
(89, 34, 25, 0.00, 9480.00, 'Constatation TVA due T2', NULL, 1),
(90, 35, 24, 13660.00, 0.00, 'Solde TVA facturée T3', NULL, 1),
(91, 35, 25, 0.00, 13660.00, 'Constatation TVA due T3', NULL, 1),
(92, 36, 24, 6500.00, 0.00, 'Solde TVA facturée T4', NULL, 1),
(93, 36, 18, 0.00, 3200.00, 'Solde TVA récupérable T4', NULL, 1),
(94, 36, 25, 0.00, 3300.00, 'Constatation TVA due T4 (au passif)', NULL, 1),
(95, 37, 29, 89000.00, 0.00, 'Annulation du stock initial', NULL, 1),
(96, 37, 6, 0.00, 89000.00, 'Sortie comptable stock initial', NULL, 1),
(97, 38, 6, 85600.00, 0.00, 'Entrée comptable stock final', NULL, 1),
(98, 38, 29, 0.00, 85600.00, 'Constatation du stock final', NULL, 1)
ON CONFLICT (id) DO UPDATE SET debit = EXCLUDED.debit, credit = EXCLUDED.credit, lettrage = EXCLUDED.lettrage;

-- =============================================================================
-- 12. DECLARATIONS DE TVA TRIMESTRIELLES (EXERCICE 2025)
-- =============================================================================
INSERT INTO declarations_tva (id, periode, regime, statut, total_ventes_ht, total_achats_ht, tva_collectee, tva_deductible_charges, tva_deductible_total, tva_a_payer, credit_tva_reportable, notes, point_de_vente_id)
VALUES
(1, '2025-T1', 'ENCAISSEMENT', 'VALIDEE', 154350.00, 146000.00, 30870.00, 29200.00, 29200.00, 1670.00, 0.00, 'Déclaration TVA T1 2025 payée le 20/04/2025', 1),
(2, '2025-T2', 'ENCAISSEMENT', 'VALIDEE', 83400.00, 36000.00, 16680.00, 7200.00, 7200.00, 9480.00, 0.00, 'Déclaration TVA T2 2025 payée le 20/07/2025', 1),
(3, '2025-T3', 'ENCAISSEMENT', 'VALIDEE', 68300.00, 0.00, 13660.00, 0.00, 0.00, 13660.00, 0.00, 'Déclaration TVA T3 2025 payée le 20/10/2025', 1),
(4, '2025-T4', 'ENCAISSEMENT', 'EN_ATTENTE_PAIEMENT', 32500.00, 16000.00, 6500.00, 3200.00, 3200.00, 3300.00, 0.00, 'Déclaration TVA T4 2025 à régler au 20/01/2026', 1)
ON CONFLICT (id) DO UPDATE SET total_ventes_ht = EXCLUDED.total_ventes_ht, tva_a_payer = EXCLUDED.tva_a_payer, statut = EXCLUDED.statut;

-- =============================================================================
-- 13. SYNCHRONISATION DES SEQUENCES POSTGRESQL
-- =============================================================================
SELECT setval('meres_id_seq', (SELECT COALESCE(MAX(id), 1) FROM meres));
SELECT setval('point_de_vente_id_seq', (SELECT COALESCE(MAX(id), 1) FROM point_de_vente));
SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM roles));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('entreprise_profiles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM entreprise_profiles));
SELECT setval('societes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM societes));
SELECT setval('depots_id_seq', (SELECT COALESCE(MAX(id), 1) FROM depots));
SELECT setval('categories_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categories));
SELECT setval('fournisseurs_id_seq', (SELECT COALESCE(MAX(id), 1) FROM fournisseurs));
SELECT setval('clients_id_seq', (SELECT COALESCE(MAX(id), 1) FROM clients));
SELECT setval('produits_id_seq', (SELECT COALESCE(MAX(id), 1) FROM produits));
SELECT setval('stocks_id_seq', (SELECT COALESCE(MAX(id), 1) FROM stocks));
SELECT setval('mouvements_stock_id_seq', (SELECT COALESCE(MAX(id), 1) FROM mouvements_stock));
SELECT setval('factures_achat_id_seq', (SELECT COALESCE(MAX(id), 1) FROM factures_achat));
SELECT setval('lignes_facture_achat_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lignes_facture_achat));
SELECT setval('reglements_fournisseur_id_seq', (SELECT COALESCE(MAX(id), 1) FROM reglements_fournisseur));
SELECT setval('factures_id_seq', (SELECT COALESCE(MAX(id), 1) FROM factures));
SELECT setval('lignes_facture_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lignes_facture));
SELECT setval('paiements_id_seq', (SELECT COALESCE(MAX(id), 1) FROM paiements));
SELECT setval('paiement_affectations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM paiement_affectations));
SELECT setval('comptes_financiers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM comptes_financiers));
SELECT setval('exercices_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM exercices_comptables));
SELECT setval('journaux_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM journaux_comptables));
SELECT setval('comptes_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM comptes_comptables));
SELECT setval('ecritures_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ecritures_comptables));
SELECT setval('lignes_ecriture_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lignes_ecriture));
SELECT setval('declarations_tva_id_seq', (SELECT COALESCE(MAX(id), 1) FROM declarations_tva));
SELECT setval('banques_id_seq', (SELECT COALESCE(MAX(id), 1) FROM banques));

COMMIT;

-- =============================================================================
-- RECAPITULATIF DE VALIDATION COMPTABLE ET COMMERCIALE 2025 :
-- 1. Total Chiffre d'Affaires HT (7111)    :   338 550.00 DH
-- 2. Total Achats de Marchandises HT (6111) :   198 000.00 DH
-- 3. Variation Stock (6114 - Déstockage)   :    +3 400.00 DH
-- 4. Coût d''Achat Marchandises Vendues (CAMV:   201 400.00 DH
-- 5. Résultat d''Exploitation / Net (1191)  :  +137 150.00 DH (Bénéfice net)
-- 6. Cumul Débit / Crédit Balance Générale  : 2 135 640.00 DH (Équilibre strict)
-- 7. Total Bilan Actif / Passif             :   749 650.00 DH (Écart = 0.00 DH)
-- 8. Trésorerie Finale Réelle (5141 + 5161) :   468 050.00 DH
-- =============================================================================
