# 📊 ROADMAP GLOBALE ERP : FINANCES, COMPTABILITÉ, LOGISTIQUE & EXTENSIONS MÉTIER
> **Projet** : ERP Point de Vente, Gestion de Stock, Achats & Chantiers  
> **Stack Technique** : Backend Spring Boot 3 (Java) + Frontend Next.js 16 (App Router / TypeScript / Tailwind CSS)  
> **Moteur d'Impression** : JasperReports 6 / OpenPDF  
> **Conformité & Cadre Légal** : Normes marocaines (PCGM, Déclarations TVA SIMPL, Retenues à la source RAS, DGI, Export FEC)

---

## 📑 TABLE DES MATIÈRES
1. [Vue d'Ensemble & Synergie avec l'Existant](#1-vue-densemble--synergie-avec-lexistant)
2. [🎯 Plan Directeur d'Implémentation par Ordre de Priorité (P0 à P3)](#2--plan-directeur-dimplémentation-par-ordre-de-priorité-p0-à-p3)
   - [Matrice Décisionnelle (Impact vs Effort)](#matrice-décisionnelle-impact-vs-effort)
   - [🔥 Priorité P0 : Bloquant & Exploitation Quotidienne Immédiate](#-priorité-p0--bloquant--exploitation-quotidienne-immédiate)
   - [⚡ Priorité P1 : Cœur de Gestion Financière, Caisse POS & Marges](#-priorité-p1--cœur-de-gestion-financière-caisse-pos--marges)
   - [⚖️ Priorité P2 : Conformité Légale, Traçabilité & Contrôle Interne](#️-priorité-p2--conformité-légale-traçabilité--contrôle-interne)
   - [🚀 Priorité P3 : Gouvernance Avancée, IA & Confort](#-priorité-p3--gouvernance-avancée-ia--confort)
3. [Spécifications Techniques : Modules Finances & Comptabilité](#3-spécifications-techniques--modules-finances--comptabilité)
   - [Module 1 : Comptabilité Générale & IA / OCR](#module-1--comptabilité-générale--ia--ocr)
   - [Module 2 : Gestion des Immobilisations](#module-2--gestion-des-immobilisations)
   - [Module 3 : Comptabilité Analytique & Chantiers](#module-3--comptabilité-analytique--chantiers)
   - [Module 4 : Gestion Budgétaire](#module-4--gestion-budgétaire)
   - [Module 5 : Révision Comptable & Audit](#module-5--révision-comptable--audit)
   - [Module 6 : Recouvrement & Impayés](#module-6--recouvrement--impayés)
4. [Spécifications Techniques : 7 Extensions Métier & Chantiers ERP Additionnels](#4-spécifications-techniques--7-extensions-métier--chantiers-erp-additionnels)
   - [4.1 Édition de Documents PDF (Rapports Jasper)](#41-édition-de-documents-pdf-rapports-jasper)
   - [4.2 Achats & Fournisseurs Avancés (Avoirs, Cycle de Vie, 3-Way Matching)](#42-achats--fournisseurs-avancés-avoirs-cycle-de-vie-3-way-matching)
   - [4.3 Ventes, Caisse POS & CRM (Acomptes, Multi-Modes, Fidélité, Paliers, Signature)](#43-ventes-caisse-pos--crm-acomptes-multi-modes-fidélité-paliers-signature)
   - [4.4 Stocks & Entrepôts (PUMP/FIFO, Emplacements, Sérialisation, Réappro)](#44-stocks--entrepôts-pumpfifo-emplacements-sérialisation-réappro)
   - [4.5 Trésorerie, Rapprochement & Banque (OFX, Virements, Petite Caisse)](#45-trésorerie-rapprochement--banque-ofx-virements-petite-caisse)
   - [4.6 Comptabilité Légale & Déclarations (FEC, Clôture/À-Nouveaux, Bilan/CPC, Lettrage)](#46-comptabilité-légale--déclarations-fec-clôtureà-nouveaux-bilancpc-lettrage)
   - [4.7 Communication & Système (Mailer SMTP, Mot de passe oublié, 2FA, Audit Trail)](#47-communication--système-mailer-smtp-mot-de-passe-oublié-2fa-audit-trail)
5. [Architecture des Données (Entités & Modèle Relationnel)](#5-architecture-des-données-entités--modèle-relationnel)
6. [Architecture Frontend (Next.js Routes & Composants)](#6-architecture-frontend-nextjs-routes--composants)
7. [Roadmap d'Implémentation Échelonnée (8 Phases)](#7-roadmap-dimplémentation-échelonnée-8-phases)
8. [Checklist Complète de Suivi & Recette](#8-checklist-complète-de-suivi--recette)

---

## 1. VUE D'ENSEMBLE & SYNERGIE AVEC L'EXISTANT

L'ERP dispose déjà d'un socle transactionnel opérationnel :
- **Clients & Fournisseurs** (avec gestion de crédits, catégorisations et recherche avancée multi-critères).
- **Ventes / POS & Factures** (calculs HT, TVA, TTC, échéances, remise, marge).
- **Paiements & Trésorerie de base** (Espèces, Chèques, Effets de commerce, Virements).
- **Chantiers & Commandes Clients / Fournisseurs**.
- **Gestion de Stock** (Dépôts, Bons d'Entrée/Sortie, Transferts, Ajustements, Lots/DLUO).
- **Moteur d'Impression Jasper** (7 documents PDF opérationnels : Devis, Facture Vente, Ticket POS, BL, Bon Commande Fournisseur, etc.).

```
┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
│   Ventes / POS / BL     │ ───► │  Moteur Écritures Auto  │ ───► │  Grand Livre / Balance  │
└─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
             │                                │                                │
             ▼                                ▼                                ▼
┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
│    Achats & Stocks      │ ───► │   Valorisation PUMP     │ ───► │  Bilan & CPC (Officiel) │
└─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
             │                                │                                │
             ▼                                ▼                                ▼
┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
│  Trésorerie & Banque    │ ───► │ Rapprochement Bancaire  │ ───► │ Audit Trail & Sécurité  │
└─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
```

---

## 2. 🎯 PLAN DIRECTEUR D'IMPLÉMENTATION PAR ORDRE DE PRIORITÉ (P0 À P3)

Pour garantir des gains rapides en production tout en sécurisant l'intégrité comptable et logistique, les fonctionnalités sont classées selon la méthode MoSCoW / matrice de criticité :

### Matrice Décisionnelle (Impact vs Effort)

| Niveau de Priorité | Définition & Règle Métier | Risque si non fait | Horizon Cible |
| :--- | :--- | :--- | :--- |
| **🔥 P0 - Critique** | **Bloquant Opérationnel Immédiat** : Documents physiques indispensables sur le terrain et cycle d'achats. | Blocage magasiniers/chauffeurs, factures achats orphelines, impossibilité de décharger. | **Sprint 1 (Immédiat)** |
| **⚡ P1 - Élevé** | **Cœur Financier, Caisse POS & Marges** : Traçabilité des règlements, encaissement réel et PUMP. | Marge fausse, caisse bloquée sur paiements panachés, perte de contrôle trésorerie. | **Sprint 2** |
| **⚖️ P2 - Moyen** | **Conformité Fiscale, Traçabilité & Lettrage** : Respect des normes marocaines (FEC, CPC, TVA) et contrôle. | Redressement fiscal, écarts factures/règlements non lettrés, litiges fournisseurs non arbitrés. | **Sprint 3** |
| **🚀 P3 - Avancé** | **Pilotage Stratégique, IA & Automatisation** : Reconnaissance OCR, 2FA, budgets, prédictions. | Confort réduit, saisie manuelle résiduelle, sécurité standard (login/pass). | **Sprint 4** |

---

### 🔥 PRIORITÉ P0 : BLOQUANT & EXPLOITATION QUOTIDIENNE IMMÉDIATE
> *Ces fonctionnalités doivent être livrées en premier car elles bloquent l'utilisation quotidienne sur le terrain (entrepôts, chauffeurs, achats).*

#### 1. Documents PDF Jasper Essentiels Manquants
* **P0.1 - Bordereau de Transfert Inter-Dépôts PDF** :
  - *Pourquoi P0* : Sans décharge signée par le chauffeur et le magasinier, tout déplacement de stock entre entrepôts présente un risque élevé de vol ou de litige d'inventaire.
  - *Composants* : Template `transfert_stock.jrxml` + endpoint `GET /api/impressions/transfert-stock/{id}/pdf`.
* **P0.2 - Fiche de Préparation d'Expédition (Picking List) PDF** :
  - *Pourquoi P0* : Le préparateur de commande ne peut pas préparer les colis sans listing physique clair des articles triés par dépôt.
  - *Composants* : Template `bon_preparation.jrxml` + endpoint `GET /api/impressions/bon-preparation/{id}/pdf`.
* **P0.3 - Reçu d'Encaissement / Quittance de Paiement (A5 / Ticket 80mm)** :
  - *Pourquoi P0* : Tout acompte ou chèque remis par un client hors caisse POS nécessite la remise immédiate d'un reçu officiel pour décharge juridique.
  - *Composants* : Template `recu_paiement.jrxml` + endpoint `GET /api/impressions/paiements/{id}/recu-pdf`.
* **P0.4 - Fiche de Comptage d'Inventaire à Blanc PDF** :
  - *Pourquoi P0* : Indispensable pour mener physiquement les campagnes d'inventaire en entrepôt.
  - *Composants* : Template `fiche_comptage_inventaire.jrxml` + endpoint `GET /api/impressions/inventaire/{id}/fiche-comptage-pdf`.

#### 2. Cycle de Vie Complet de la Facture d'Achat
* **P0.5 - Workflow & Modification `FactureAchat`** :
  - *Pourquoi P0* : `FactureAchatController` ne permet aujourd'hui que la création brute. Impossible de modifier un brouillon, d'annuler une erreur ou de solder une facture payée.
  - *Actions* : Endpoints `PUT /api/factures-achats/{id}`, `POST /valider`, `POST /annuler`. Statuts synchronisés (`BROUILLON`, `VALIDEE`, `PARTIELLEMENT_PAYEE`, `SOLDEE`, `ANNULEE`).

#### 3. Avoirs Fournisseurs (`AvoirFournisseur`)
* **P0.6 - Gestion des Retours & Avoirs Fournisseurs** :
  - *Pourquoi P0* : Les retours de marchandises défectueuses aux fournisseurs ne peuvent actuellement pas être enregistrés dans l'ERP.
  - *Actions* : Entité `AvoirFournisseur`, contrôleur, déduction sur la dette fournisseur ou remboursement, sortie de stock inverse.

#### 4. Nettoyage des Rapports Orphelins
* **P0.7 - Harmonisation des Templates Jasper** :
  - *Pourquoi P0* : `commande.jrxml` et `lignes_commande_subreport.jrxml` encombrent le classpath sans être fonctionnels. Raccordement ou suppression contrôlée.

---

### ⚡ PRIORITÉ P1 : CŒUR DE GESTION FINANCIÈRE, CAISSE POS & MARGES
> *Sécurise la rentabilité financière, la fluidité des encaissements en boutique et la valorisation réelle des actifs en stock.*

#### 1. Caisse POS & Encaissements
* **P1.1 - Paiement Fractionné Multi-Modes en Caisse POS** :
  - *Besoin* : Permettre sur une même vente de combiner 200 DH en espèces + 300 DH par carte bancaire + bon d'achat, avec rendu de monnaie sur les espèces.
  - *Actions* : Entité `LigneReglementVente`, mise à jour du composant de caisse Next.js et de l'endpoint d'encaissement.
* **P1.2 - Factures d'Acompte & Déduction sur Facture Finale** :
  - *Besoin* : Émettre une facture légale sur encaissement d'acompte (ex: 30%) et l'imputer automatiquement en déduction sur la facture de solde.
  - *Actions* : Génération facture d'acompte, ligne d'imputation négative sur la facture de solde avec report de TVA.

#### 2. Logistique & Valorisation des Marges
* **P1.3 - Valorisation Comptable PUMP (Prix Unitaire Moyen Pondéré)** :
  - *Besoin* : Mettre à jour automatiquement le coût moyen unitaire à chaque bon de réception achat pour connaître la marge commerciale réelle sur chaque vente.
  - *Actions* : Service de recalcul PUMP à la validation de la réception, valorisation du stock temps réel.

#### 3. Trésorerie & Relations Clients
* **P1.4 - Bordereau de Remise Bancaire de Chèques / Traites PDF** :
  - *Besoin* : Générer en 1 clic le document de remise à signer pour la banque listant tous les chèques/effets déposés avec totalisation.
* **P1.5 - Relevé de Compte Client (Extrait de Compte) PDF & Relances** :
  - *Besoin* : Document chronologique (débits factures, crédits paiements, solde dû) à envoyer aux clients en retard de paiement.
* **P1.6 - Envoi Direct de Documents par Email (Service SMTP / Mailer)** :
  - *Besoin* : Bouton "Envoyer par Email" sur Devis, Facture Vente et Bon de Commande avec PDF attaché automatiquement.

---

### ⚖️ PRIORITÉ P2 : CONFORMITÉ LÉGALE, TRAÇABILITÉ & CONTRÔLE INTERNE
> *Met l'entreprise en conformité avec les obligations fiscales marocaines (DGI) et renforce le contrôle de gestion.*

#### 1. Comptabilité Générale & Fiscalité Marocaine
* **P2.1 - Export FEC (Fichier des Écritures Comptables)** :
  - *Besoin* : Fichier normé légal obligatoire en cas de contrôle fiscal DGI (18 colonnes normalisées).
* **P2.2 - Clôture d'Exercice & Report à Nouveau (À-Nouveaux)** :
  - *Besoin* : Verrouillage de l'exercice N, calcul du résultat net et génération automatique des écritures d'ouverture au 01/01/N+1.
* **P2.3 - États Financiers Officiels (Bilan Actif/Passif & CPC)** :
  - *Besoin* : Génération automatique des tableaux de synthèse conformes au Code Général de Normalisation Comptable (CGNC).
* **P2.4 - Lettrage Comptable (Factures ⟷ Règlements)** :
  - *Besoin* : Rapprochement lettré (`AA`, `AB`...) pour apurer les comptes tiers 3421 (Clients) et 4411 (Fournisseurs).

#### 2. Contrôle des Achats & Traçabilité Entrepôt
* **P2.5 - Blocage Automatique 3-Way Matching (Achats)** :
  - *Besoin* : Détection automatique des écarts Commande vs Réception vs Facture avec mise en statut `BLOQUEE_LITIGE` et approbation managériale.
* **P2.6 - Emplacements Physiques dans les Dépôts** :
  - *Besoin* : Modélisation Allée / Travée / Étagère / Casier pour guider les magasiniers.
* **P2.7 - Gestion des Numéros de Série Unitaires (Sérialisation)** :
  - *Besoin* : Traçabilité unitaire des produits sérialisés de l'achat à la vente pour le SAV et les garanties.
* **P2.8 - Planches d'Étiquettes Code-Barres & Prix (PDF)** :
  - *Besoin* : Impression de planches d'étiquettes de rayonnage (EAN-13, Code 128).

#### 3. Fidélisation & CRM Ventes
* **P2.9 - Moteur de Conversion des Points de Fidélité** :
  - *Besoin* : Transformation des points accumulés en bons d'achat ou remises directes au passage en caisse.
* **P2.10 - Grille de Tarification Dégressive par Paliers** :
  - *Besoin* : Remises automatiques par volume (ex: 10 à 49 unités = -5%, $\ge 50$ = -10%).
* **P2.11 - Signature Électronique Émargée sur Bon de Livraison (BL)** :
  - *Besoin* : Capture tactile de signature client sur tablette/smartphone avec incrustation sur le PDF.

---

### 🚀 PRIORITÉ P3 : GOUVERNANCE AVANCÉE, IA & CONFORT
> *Modules à haute valeur ajoutée technologique, d'automatisation prédictive et de renforcement de la sécurité.*

* **P3.1 - Pipeline OCR & Reconnaissance IA des Factures d'Achat** :
  - Extraction automatique par IA des mentions clés (ICE, montants, TVA) avec validation split-screen.
* **P3.2 - Rapprochement Bancaire Automatique Multi-Formats (OFX / CSV / MT940)** :
  - Importation de relevés bancaires et moteur de pointage automatique basé sur date et montant.
* **P3.3 - Double Facteur d'Authentification (2FA / TOTP) & Réinitialisation Mot de Passe** :
  - Sécurisation des accès par Google Authenticator et réinitialisation autonome par lien email sécurisé.
* **P3.4 - Piste d'Audit Complète (Audit Trail / CDC)** :
  - Traçabilité exhaustive de toutes les créations/modifications/suppressions (qui, quand, quoi, anciennes et nouvelles valeurs JSON, IP).
* **P3.5 - Suggestion Automatique de Réapprovisionnement (VMJ)** :
  - Algorithme calculant les propositions de commandes fournisseurs basées sur la vente moyenne journalière et les délais fournisseurs.
* **P3.6 - Petite Caisse (Menues Dépenses)** :
  - Circuit d'avances de caisse et justification des menues dépenses.
* **P3.7 - Gestion des Immobilisations & Amortissements Marocains** :
  - Tableaux d'amortissement linéaire et dégressif marocain, dotations automatiques.
* **P3.8 - Comptabilité Analytique & Rentabilité par Chantier** :
  - Ventilation par axes et rentabilité nette par projet/chantier.
* **P3.9 - Gestion Budgétaire & Dossier de Révision Comptable (7 Cycles PCGM)** :
  - Comparatif budget vs réalisé et visas d'audit à 3 niveaux avant clôture.

---

## 3. SPÉCIFICATIONS TECHNIQUES : MODULES FINANCES & COMPTABILITÉ

### MODULE 1 : COMPTABILITÉ GÉNÉRALE & IA / OCR
#### Spécificités Marocaines
- **PCGM (Plan Comptable Général Marocain)** : Classes 1 à 8 avec arborescence hiérarchique personnalisable.
- **TVA SIMPL** : Taux légaux (20%, 14%, 10%, 7%), gestion des encaissements / débits.
- **Retenues à la source (RAS)** : Gestion des attestations RAS clients et déclarations fournisseurs.
- **États financiers de synthèse** : Bilan, CPC, ESG, Tableau de Financement.

#### Pipeline OCR & IA Documentaire
1. **Dépôt** : Upload drag-and-drop de factures PDF, scans ou photos de tickets.
2. **Extraction IA** : Identification Fournisseur, N° ICE, Date, N° Facture, Total HT, TVA, Total TTC.
3. **Génération d'écritures proposées** :
   - **Débit 6111** (Achats de marchandises) : Montant HT
   - **Débit 3455** (État - TVA récupérable) : Montant TVA
   - **Crédit 4411** (Fournisseurs - Compte Tiers) : Montant TTC
4. **Validation IHM Split-Screen** : Document original visualisable en vis-à-vis de l'écriture modifiable.

---

### MODULE 2 : GESTION DES IMMOBILISATIONS
- **Fichier des actifs** : Biens corporels/incorporels, date d'acquisition, date de mise en service, valeur d'acquisition, compte d'imputation classe 2.
- **Modes d'amortissement légaux** :
  - **Linéaire** : Prorata temporis ($Taux = 100 / Dur\acute{e}e$).
  - **Dégressif marocain** : Application des coefficients fiscaux officiels (1.5 pour 3-4 ans, 2.0 pour 5-6 ans, 3.0 au-delà de 6 ans).
- **Automatisations comptables** : Échéancier pluriannuel, dotations d'inventaire automatiques (`619x` / `28xx`), cessions avec calcul de la VNA (`6513` / `7513`).

---

### MODULE 3 : COMPTABILITÉ ANALYTIQUE & CHANTIERS
- **Axes analytiques** : Axe 1 (Chantiers / Projets), Axe 2 (Agences / Magasins), Axe 3 (Pôles d'activité).
- **Ventilation des charges et produits** : Imputation directe à la ligne de facture ou selon clés de répartition.
- **Reporting analytique** : Compte de résultat dégagé par chantier (Marge brute, marge nette, coûts réels vs devis).

---

### MODULE 4 : GESTION BUDGÉTAIRE
- **Définition budgétaire** : Par exercice, par compte comptable et par section analytique (ventilation sur 12 mois).
- **Comparatif Temps Réel Budget vs Réalisé** : Agrégation dynamique des écritures validées, calcul automatique des écarts.
- **Tableau de bord de pilotage** : Jauges de surconsommation budgétaire (seuils 80%, 100%, dépassements critiques).

---

### MODULE 5 : RÉVISION COMPTABLE & AUDIT
- **Dossier structuré en 7 cycles PCGM** :
  1. Trésorerie & Dettes financières
  2. Immobilisations
  3. Achats & Fournisseurs
  4. Ventes & Clients
  5. Personnel & Organismes sociaux
  6. État & Fiscalité
  7. Stocks & En-cours
- **Diligences & Justificatifs** : Checklist d'audit et attachement de pièces justificatives numérisées.
- **Workflow de visa à 3 niveaux** : Collaborateur $\rightarrow$ Réviseur $\rightarrow$ Expert-comptable avant clôture définitive.

---

### MODULE 6 : RECOUVREMENT & IMPAYÉS
- **Détection des impayés** : Surveillance automatique des échéances de paiement échues.
- **Balance âgée client** : Découpage par ancienneté (0-30j, 31-60j, 61-90j, >90j).
- **Scénarios de relances** : Relance courtoise (Email/SMS) $\rightarrow$ Relance ferme avec extrait de compte $\rightarrow$ Mise en demeure formelle PDF.
- **Dossiers contentieux & Promesses de paiement** : Plan d'apurement, promesses datées, historique d'audit.

---

## 4. SPÉCIFICATIONS TECHNIQUES : 7 EXTENSIONS MÉTIER & CHANTIERS ERP ADDITIONNELS

### 4.1 Édition de Documents PDF (Rapports Jasper)
Le moteur JasperReports actuel gère déjà 7 documents, mais plusieurs documents stratégiques manquent ou nécessitent une finalisation :

1. ❌ **Fiche de Préparation d'Expédition (Picking List)** :
   - **Objectif** : Fournir aux préparateurs d'entrepôt une feuille de route optimisée pour préparer les commandes clients.
   - **Données** : N° Bon de Préparation, Référence Commande/Client, Liste des articles triés par emplacement physique (Allée/Casier), Quantités commandées, Quantités préparées (case à cocher manuelle), Zone de signature préparateur et contrôleur.
   - **Branchement Backend** : Contrôleur existant `BonPreparationController` $\rightarrow$ nouvel endpoint `GET /api/impressions/bon-preparation/{id}/pdf`.
2. ❌ **Bordereau de Transfert Inter-Dépôts** :
   - **Objectif** : Servir de bon de décharge officiel et de lettre de voiture interne lors du déplacement physique de stock entre deux entrepôts.
   - **Données** : N° Transfert, Dépôt Source, Dépôt Cible, Chauffeur / Transporteur, Véhicule, Détail des articles et lots, Date d'expédition, Date de réception, Double visa (Magasinier expéditeur / Magasinier destinataire).
   - **Branchement Backend** : `TransfertStockController` $\rightarrow$ `GET /api/impressions/transfert-stock/{id}/pdf`.
3. ❌ **Fiche de Comptage d'Inventaire à Blanc** :
   - **Objectif** : Support papier d'audit pour les équipes chargées du comptage physique annuel ou tournant.
   - **Modes** : Mode aveugle (sans stock théorique affiché, pour forcer un comptage neutre) ou mode assisté (avec stock théorique).
   - **Données** : Dépôt, Date d'inventaire, Catégorie/Famille, Référence, Emplacement, Colonnes : "Comptage Réel", "Écart constaté", "Signature contrôleur".
   - **Branchement Backend** : `InventaireController` $\rightarrow$ `GET /api/impressions/inventaire/{id}/fiche-comptage-pdf`.
4. ❌ **Reçu d'Encaissement / Quittance de Paiement** :
   - **Objectif** : Délivrer immédiatement une quittance légale de paiement au client lors d'un règlement hors caisse POS (ex: acompte à la commande, chèque déposé, virement reçu).
   - **Formats** : Format A5 ou format ticket thermique 80mm.
   - **Données** : N° Reçu, Client, Montant reçu en lettres et chiffres, Mode de paiement (espèces, chèque n°/banque, virement), Facture(s) rattachée(s), Solde restant dû, Cachet/Signature caissier.
   - **Branchement Backend** : `PaiementController` $\rightarrow$ `GET /api/impressions/paiements/{id}/recu-pdf`.
5. ❌ **Bordereau de Remise Bancaire de Chèques / Traites** :
   - **Objectif** : Document officiel récapitulatif à joindre au dépôt physique des chèques ou effets à la banque.
   - **Données** : N° Bordereau, Compte bancaire récepteur (RIB / Banque), Nombre total de valeurs, Total général remis, Tableau détaillé (N° Chèque/Effet, Banque tirée, Émetteur / Client, Date d'échéance, Montant).
   - **Branchement Backend** : `ChequeEffetService` $\rightarrow$ `GET /api/impressions/remise-bancaire/{id}/pdf`.
6. ❌ **Relevé de Compte Client (Extrait de Compte Recouvrement)** :
   - **Objectif** : Document récapitulatif à adresser au client en support des relances de paiement.
   - **Données** : Période sélectionnée, Solde initial, Chronologie Débits (Factures) et Crédits (Paiements, Avoirs), Solde progressif, Détail des factures échues impayées, Coordonnées bancaires pour virement.
   - **Branchement Backend** : `ClientController` $\rightarrow$ `GET /api/impressions/clients/{id}/releve-compte-pdf`.
7. ❌ **Facture d'Achat Fournisseur PDF** :
   - **Objectif** : Génération / archivage interne au format normalisé PDF pour les factures d'achats enregistrées dans le système.
   - **Branchement Backend** : `FactureAchatController` $\rightarrow$ `GET /api/impressions/facture-achat/{id}/pdf`.
8. ❌ **Planches d'Étiquettes Code-Barres & Prix** :
   - **Objectif** : Générer une planche PDF imprimable (format Avery standard A4 ou rouleau thermique Zebra) d'étiquettes avec Code EAN-13 / Code 128, prix TTC et désignation produit pour étiquetage de rayonnage.
9. ⚠️ **Raccordement & Nettoyage des Templates Orphelins** :
   - Raccordement ou suppression contrôlée de `commande.jrxml` (remplacé par `commande_client.jrxml` et `commande_fournisseur.jrxml`).
   - Raccordement du sous-rapport `lignes_commande_subreport.jrxml` ou harmonisation avec les `JRBeanCollectionDataSource` autonomes.

---

### 4.2 Achats & Fournisseurs Avancés
1. ❌ **Avoirs Fournisseurs (`AvoirFournisseur`)** :
   - **Besoin** : Traiter les retours de marchandises défectueuses aux fournisseurs ou les remises commerciales obtenues a posteriori.
   - **Entités & Modèle** : `AvoirFournisseur` (n° pièce, date, fournisseur, montant HT, TVA, TTC, motif de retour, statut).
   - **Impacts** :
     - Sortie de stock ou neutralisation du mouvement d'entrée.
     - Déduction automatique sur les factures d'achat futures ou enregistrement d'un remboursement financier.
     - Génération d'écritures comptables inverses (Débit 4411 / Crédit 6119 & 3455).
2. ❌ **Cycle de Vie Complet de la Facture d'Achat** :
   - Passage des statuts : `BROUILLON` $\rightarrow$ `VALIDEE` $\rightarrow$ `PARTIELLEMENT_PAYEE` $\rightarrow$ `SOLDEE` / `ANNULEE`.
   - Verrouillage immuable de la facture après validation.
   - Mécanisme d'extourne / annulation formelle en cas d'erreur de saisie.
   - Endpoints manquants : `PUT /api/factures-achats/{id}`, `POST /api/factures-achats/{id}/valider`, `POST /api/factures-achats/{id}/annuler`.
3. ❌ **Blocage Automatique sur Litige de Rapprochement (3-Way Matching)** :
   - **Contrôle Triangulaire** : Bon de Commande Achat (PO) $\longleftrightarrow$ Bon de Réception Magasin (GRN) $\longleftrightarrow$ Facture Fournisseur.
   - **Tolérances paramétrables** : Tolérance de quantité ($\pm 0\%$) et tolérance de prix unitaire ($\pm 1\%$).
   - **Blocage & Approbation** : En cas d'écart non justifié, mise en statut `BLOQUEE_LITIGE` avec interdiction de règlement jusqu'à validation hiérarchique d'un Responsable Achats.

---

### 4.3 Ventes, Caisse POS & CRM
1. ❌ **Factures d'Acompte & Situations de Travaux** :
   - Émission d'une facture d'acompte officielle (ex: 30% à la signature du devis/commande) avec TVA collectée légale.
   - Imputation automatique de l'acompte déjà réglé en déduction de la facture finale de solde (ligne négative "Acompte déduit n° FA-XXX").
   - Gestion des factures de situation d'avancement pour le BTP / Chantiers.
2. ❌ **Paiement Multi-Modes Fractionné en Caisse POS** :
   - Permettre à un client de régler un ticket de vente avec plusieurs moyens combinés (ex: 400 DH en espèces + 250 DH par carte bancaire + 50 DH en bon d'achat fidélité).
   - Calcul du rendu de monnaie en temps réel sur la part espèces.
   - Enregistrement de chaque tranche dans la table `paiements` rattachée au ticket de vente.
3. ❌ **Moteur de Fidélité & Conversion de Points** :
   - Consommation des points accumulés dans l'entité `Client`.
   - Barème paramétrable : Ex. 100 points = 10 DH de réduction.
   - Émission d'un avoir / bon d'achat ou application d'une remise directe sur le ticket de caisse avec mise à jour du solde de points client.
4. ❌ **Tarification Dynamique Dégressive par Quantité (Paliers)** :
   - Grille de remises quantitatives par article ou famille (ex: de 1 à 9 unités = prix normal ; de 10 à 49 unités = -5% ; $\ge$ 50 unités = -10%).
   - Application automatique du meilleur prix lors de la saisie d'un devis, d'une commande ou d'une vente en caisse.
5. ❌ **Signature Électronique Émargée sur Bon de Livraison (BL)** :
   - Composant tactile de signature numérique (canvas web compatible tablette/smartphone).
   - Horodatage et stockage de la signature en base de données.
   - Incrustation automatique de la signature émargée sur le PDF du Bon de Livraison pour preuve juridique de livraison.

---

### 4.4 Stocks & Entrepôts
1. ❌ **Valorisation Comptable des Stocks (PUMP & FIFO)** :
   - Calcul automatique du **Prix Unitaire Moyen Pondéré (PUMP)** recalculé à chaque nouvelle entrée en stock :
     $$PUMP_{nouveau} = \frac{(Stock_{actuel} \times PUMP_{precedent}) + (Qte_{entree} \times Prix_{achat})}{(Stock_{actuel} + Qte_{entree})}$$
   - Valorisation instantanée de l'inventaire en stock pour le bilan.
   - Calcul de la marge commerciale réelle et précise ligne par ligne à la vente.
2. ❌ **Emplacements Physiques dans les Dépôts** :
   - Découpage fin d'un dépôt (`Depot`) en emplacements précis : Allée, Travée, Étagère, Casier / Bac (`EmplacementStock`).
   - Affectation des articles à un emplacement par défaut.
   - Guidage de l'opérateur de préparation selon le parcours de ramassage optimal.
3. ❌ **Gestion des Numéros de Série Unitaires (Sérialisation)** :
   - Traçabilité individuelle de chaque unité produite ou vendue (matériel IT, outillage, pièces sensibles).
   - Suivi du cycle de vie unitaire : Réception $\rightarrow$ Stockage $\rightarrow$ Vente $\rightarrow$ SAV / Garantie.
4. ❌ **Suggestion Automatique de Réapprovisionnement** :
   - Moteur de calcul des besoins en approvisionnement :
     $$Besoin = (Vente\ Moyenne\ Journali\grave{e}re \times D\acute{e}lai\ Fournisseur) + Stock\ S\acute{e}curit\acute{e} - Stock\ Actuel$$
   - Génération en 1 clic des propositions de bons de commande fournisseurs.

---

### 4.5 Trésorerie, Rapprochement & Banque
1. ❌ **Rapprochement Bancaire Automatique Multi-Formats** :
   - Import électronique des relevés bancaires aux formats normalisés : **CSV bancaire**, **OFX**, **MT940**, **CAMT.053**.
   - Moteur de pointage intelligent (Matching automatique) : rapprochement des lignes de relevé avec les règlements et factures selon date ($\pm 3$ jours), montant exact et libellé/référence.
2. ❌ **Fichiers de Virement Électroniques (SEPA / Normes Bancaires Marocaines)** :
   - Génération de fichiers d'ordre de virement de masse (XML ISO 20022 pain.001 ou format CFONB/interbancaire marocain).
   - Traitement des paiements groupés des factures fournisseurs et notes de frais en un seul téléversement bancaire.
3. ❌ **Caisse de Menues Dépenses (Petite Caisse)** :
   - Circuit autonome pour les dépenses courantes d'exploitation (fournitures, frais de déplacement, coursiers).
   - Enregistrement des avances de fonds, saisie des justificatifs de dépenses avec justificatif numérisé, validation par le responsable et régularisation périodique.

---

### 4.6 Comptabilité Légale & Déclarations
1. ❌ **Export FEC (Fichier des Écritures Comptables)** :
   - Génération du fichier plat normé (tabulé ou pipe-delimited) des écritures comptables, exigé lors des contrôles fiscaux de la DGI.
   - Validation stricte du format (18 colonnes légales : JournalCode, JournalLib, EcritureNum, EcritureDate, CompteNum, CompteLib, CompAuxNum, CompAuxLib, PieceRef, PieceDate, EcritureLib, Debit, Credit, EcritureLet, DateLet, ValidDate, Montantdevise, Idevise).
2. ❌ **Clôture d'Exercice Comptable & Report à Nouveau (À-Nouveaux)** :
   - Procédure de clôture annuelle avec verrouillage irréversible de l'exercice N.
   - Calcul automatique du résultat net (bénéfice au compte 1191 ou perte au compte 1199).
   - Génération automatique des écritures d'À-Nouveaux au 1er jour de l'exercice N+1 (soldes des comptes de bilan classes 1 à 5).
3. ❌ **Génération Officielle du Bilan & Compte de Résultat (CPC)** :
   - Compilation automatique des masses du bilan (Actif immobilisé, Actif circulant, Trésorerie-Actif / Financement permanent, Passif circulant, Trésorerie-Passif).
   - Calcul des niveaux de marge du CPC : Résultat d'exploitation, Résultat financier, Résultat courant, Résultat non courant, Impôt sur les sociétés (IS) et Résultat net.
4. ❌ **Lettrage Comptable** :
   - Affectation d'un code de lettrage alphabétique unique (ex: `AA`, `AB`, `AAA`) associant une facture client/fournisseur et ses écritures de règlement.
   - Filtrage instantané des écritures non lettrées pour identifier les créances et dettes résiduelles.

---

### 4.7 Communication, Sécurité & Système
1. ❌ **Envoi Direct de Documents par Email (Service SMTP / Mailer)** :
   - Envoi en 1 clic de devis, commandes, bons de livraison ou factures directement depuis l'application au client ou fournisseur.
   - Génération et attachement automatique du document PDF généré par JasperReports.
   - Modèles d'emails HTML personnalisables avec variables dynamiques (`{{nomClient}}`, `{{montantTTC}}`, `{{dateEcheance}}`).
   - Journal d'historique des emails envoyés (statut : envoyé, en attente, erreur).
2. ❌ **Réinitialisation Autonome de Mot de Passe ("Mot de passe oublié")** :
   - Endpoint public `POST /api/auth/forgot-password` générant un jeton unique à durée de vie limitée (15 minutes).
   - Email contenant le lien sécurisé vers la page Next.js `auth/reset-password`.
   - Endpoint `POST /api/auth/reset-password` vérifiant le hash du token et mettant à jour le mot de passe hashé en BCrypt.
3. ❌ **Double Facteur d'Authentification (2FA / OTP)** :
   - Option d'activation 2FA TOTP (Google Authenticator, Microsoft Authenticator) par utilisateur.
   - Génération de clé secrète avec QR Code lors de l'activation.
   - Vérification du code OTP à 6 chiffres lors de la connexion après le mot de passe.
4. ❌ **Piste d'Audit Complète (Audit Trail / CDC)** :
   - Traçabilité et historisation automatique de toutes les actions sensibles (création, modification, suppression).
   - Enregistrement : Date/Heure, Utilisateur, Adresse IP, Entité modifiée, Identifiant, Anciennes valeurs (JSON), Nouvelles valeurs (JSON).

---

## 5. ARCHITECTURE DES DONNÉES (ENTITÉS & MODÈLE RELATIONNEL)

### Packages Java Backend proposés dans `stock-manager` :
```
com.gestion/
├── comptabilite/        # Plan comptable, Journaux, Écritures, Lettrage, FEC, Bilan, CPC
├── immobilisation/      # Immobilisations, Amortissements linéaires & dégressifs, Cessions
├── analytique/          # Axes analytiques, Sections, Clés de répartition
├── budget/              # Enveloppes budgétaires, Suivi budget vs réalisé
├── revision/            # Cycles d'audit, Diligences, Pièces justificatives, Visas
├── recouvrement/        # Dossiers recouvrement, Relances, Promesses de paiement
├── achats/              # Avoirs fournisseurs, 3-way matching, Workflow validation
├── stock/               # Emplacements physiques, Sérialisation, PUMP, Suggestion réappro
├── pos/                 # Paiement fractionné multi-modes, Fidélité points
├── impression/          # Services JasperReports, Contrôleur d'impressions PDF
├── notification/        # Service Mailer SMTP, Templates d'emails
└── audit/               # Intercepteur d'audit trail, Entités AuditLog
```

### Schéma des Entités Clés :

```java
// --- 1. Avoirs Fournisseurs & 3-Way Matching ---
@Entity AvoirFournisseur {
    Long id;
    String numeroAvoir;
    LocalDate dateAvoir;
    @ManyToOne Fournisseur fournisseur;
    @ManyToOne FactureAchat factureAchatOrigine;
    BigDecimal totalHT, totalTVA, totalTTC;
    @Enumerated StatutAvoir statut; // BROUILLON, VALIDE, DEDUIT, REMBOURSE
    String motif;
    @OneToMany List<LigneAvoirFournisseur> lignes;
}

@Entity LitigeRapprochementAchat {
    Long id;
    @ManyToOne FactureAchat factureAchat;
    @ManyToOne BonCommande bonCommande;
    @ManyToOne BonReception bonReception;
    BigDecimal ecartQuantite, ecartPrix;
    Boolean bloquePourReglement;
    String commentaireLitige;
    LocalDateTime dateResolution;
    @ManyToOne Utilisateur resoluPar;
}

// --- 2. Emplacements Physiques & Sérialisation Stock ---
@Entity EmplacementStock {
    Long id;
    @ManyToOne Depot depot;
    String code;         // Ex: "A-02-E3-C1"
    String allee;        // Allée A
    String travee;       // Travée 02
    String etagere;      // Étagère 3
    String casier;       // Casier 1
    Boolean actif;
}

@Entity NumeroSerieProduit {
    Long id;
    @ManyToOne Produit produit;
    String numeroSerie;  // Numéro unique
    @ManyToOne EmplacementStock emplacement;
    @Enumerated StatutSerie statut; // EN_STOCK, VENDU, EN_REPARATION, RETOURNE
    LocalDate dateEntree;
    LocalDate dateGarantieFin;
    @ManyToOne BonLivraisonClient bonLivraison;
}

// --- 3. Caisse POS Multi-Modes & Fidélité ---
@Entity LigneReglementVente {
    Long id;
    @ManyToOne Vente vente;
    @Enumerated ModePaiement modePaiement; // ESPECES, CARTE, CHEQUE, BON_ACHAT
    BigDecimal montant;
    String referencePaiement; // N° transaction CB ou N° chèque
}

@Entity HistoriqueFidelite {
    Long id;
    @ManyToOne Client client;
    Integer points;      // Positif si cumul, négatif si utilisation
    @Enumerated TypeMouvementFidelite type; // ACHAT, UTILISATION_REMISE, EXPIRATION
    BigDecimal montantEquivalantRemise;
    LocalDateTime dateOperation;
}

// --- 4. Comptabilité Générale & Lettrage ---
@Entity CompteComptable {
    Long id;
    String numeroCompte; // Ex: 61110000
    String libelle;
    Integer classe;       // 1 à 8
    Boolean actif;
}

@Entity JournalComptable {
    Long id;
    String code;         // VT, AC, BQ, CA, OD
    String libelle;
}

@Entity EcritureComptable {
    Long id;
    String numeroPiece;
    LocalDate dateEcriture;
    String libelle;
    Boolean validee;     // Immuable une fois validée
    @ManyToOne JournalComptable journal;
    @OneToMany List<LigneEcriture> lignes;
}

@Entity LigneEcriture {
    Long id;
    @ManyToOne CompteComptable compte;
    BigDecimal debit;
    BigDecimal credit;
    String referencePiece;
    String lettrage;     // Ex: "AA" pour les rapprochements
    @ManyToOne SectionAnalytique sectionAnalytique;
}

// --- 5. Piste d'Audit & Journalisation ---
@Entity AuditLog {
    Long id;
    String username;
    String ipAddress;
    String action;       // CREATE, UPDATE, DELETE, VALIDATE
    String entityName;   // Facture, Client, EcritureComptable
    Long entityId;
    LocalDateTime timestamp;
    @Column(columnDefinition = "TEXT") String ancienneValeurJson;
    @Column(columnDefinition = "TEXT") String nouvelleValeurJson;
}
```

---

## 6. ARCHITECTURE FRONTEND (NEXT.JS ROUTES & COMPOSANTS)

```
agenceweb/app/(dashboard)/
├── comptabilite/
│   ├── plan-comptable/page.tsx          # Gestion PCGM marocain
│   ├── ecritures/page.tsx               # Saisie journal, filtres & lettrage
│   ├── grand-livre/page.tsx             # Grand Livre interactif & filtres
│   ├── balance/page.tsx                 # Balance générale (débit/crédit/solde)
│   ├── etats-financiers/
│   │   ├── bilan/page.tsx               # Bilan Actif / Passif officiel
│   │   ├── cpc/page.tsx                 # Compte de Produits et Charges
│   │   └── fec-export/page.tsx          # Générateur & téléchargement du FEC
│   ├── cloture-exercice/page.tsx        # Assistant de clôture & génération des À-Nouveaux
│   ├── ocr-saisie-ia/page.tsx           # Scanner OCR factures fournisseurs
│   └── banque-rapprochement/page.tsx     # Import relevés (OFX/CSV) & matching
├── immobilisations/
│   ├── page.tsx                         # Fichier des actifs & échéanciers
│   └── cessions/page.tsx                # Sorties d'actifs & calcul VNA
├── analytique/
│   ├── axes/page.tsx                    # Configuration axes & sections
│   └── rentabilite-chantiers/page.tsx   # Analyse rentabilité chantiers
├── budget/
│   ├── page.tsx                         # Grille budgétaire mensuelle
│   └── dashboard/page.tsx               # Comparateur Réalisé vs Budget
├── revision/
│   ├── dossier/page.tsx                 # Audit par cycles & documents
│   └── visas/page.tsx                   # Workflow visas 3 niveaux
├── recouvrement/
│   ├── page.tsx                         # Balance âgée & tableau de bord
│   ├── relances/page.tsx                # Scénarios de relances & emails
│   └── promesses/page.tsx               # Échéanciers & promesses
├── achats/
│   ├── factures/
│   │   ├── [id]/page.tsx                # Consultation / Validation / Workflow
│   │   └── rapprochement-3way/page.tsx  # Dashboard écarts PO/BL/Facture
│   └── avoirs/page.tsx                  # Avoirs fournisseurs & déductions
├── stock/
│   ├── valorisation-pump/page.tsx       # Valorisation PUMP & marges
│   ├── emplacements/page.tsx            # Gestion des allées/étagères/casiers
│   ├── numeros-serie/page.tsx           # Traçabilité des numéros de série
│   ├── reapprovisionnement/page.tsx     # Propositions réappro (VMJ)
│   └── inventaire/
│       └── comptage-blanc/page.tsx      # Fiche de comptage physique
├── tresorerie/
│   ├── virements-groupes/page.tsx       # Fichiers de virement bancaire
│   ├── petite-caisse/page.tsx           # Menues dépenses & avances
│   └── remises-bancaires/page.tsx       # Bordereaux de remise de chèques
└── administration/
    ├── audit-trail/page.tsx             # Piste d'audit & CDC
    └── securite-2fa/page.tsx            # Configuration 2FA / TOTP
```

---

## 7. ROADMAP D'IMPLÉMENTATION ÉCHELONNÉE (8 PHASES)

### 🟢 Phase 1 : Fondations Comptables PCGM & Banque (Semaines 1-2)
- Initialisation de la base avec le PCGM marocain.
- Moteur d'écritures automatiques sur factures ventes, achats et règlements.
- Consultation Journal, Grand Livre, Balance.
- Lettrage comptable de base (factures $\longleftrightarrow$ règlements).

### 🟡 Phase 2 : Automatisation IA/OCR & Recouvrement (Semaines 3-4)
- Upload et extraction OCR/IA pour factures d'achat.
- Écran split-view de validation d'écritures en 1 clic.
- Module complet de recouvrement avec balance âgée et relances automatiques.
- Suivi des promesses de paiement.

### 🟠 Phase 3 : Documents PDF Manquants & Impression Jasper (Semaines 5-6)
- Fiche de préparation d'expédition (Picking list) depuis `BonPreparationController`.
- Bordereau de transfert inter-dépôts depuis `TransfertStockController`.
- Fiche de comptage d'inventaire à blanc depuis `InventaireController`.
- Quittance / Reçu d'encaissement de paiement (A5 et Ticket 80mm).
- Bordereau de remise bancaire de chèques/effets.
- Relevé de compte client pour le recouvrement.
- Facture d'achat PDF et planches d'étiquettes code-barres.
- Nettoyage et raccordement des templates orphelins (`commande.jrxml`, `lignes_commande_subreport.jrxml`).

### 🔵 Phase 4 : Achats Avancés, Avoirs & 3-Way Matching (Semaines 7-8)
- Entité `AvoirFournisseur` avec déduction sur factures ou remboursement.
- Cycle de vie complet de `FactureAchat` (validation, annulation/extourne, statuts de paiement synchronisés).
- Moteur de 3-Way Matching avec blocage automatique sur litige de quantité/prix.

### 🟣 Phase 5 : POS, Caisse Multi-Modes & CRM Ventes (Semaines 9-10)
- Factures d'acompte avec déduction automatique sur la facture finale de solde.
- Encaissement fractionné multi-modes sur le terminal POS (Espèces + CB + Bon).
- Moteur de fidélité : barème et conversion de points en bons de réduction.
- Tarification dynamique dégressive par quantité/paliers.
- Signature tactile électronique émargée sur Bon de Livraison.

### 🟤 Phase 6 : Stocks Avancés, PUMP & Sérialisation (Semaines 11-12)
- Valorisation continue PUMP recalculée à chaque réception fournisseur.
- Emplacements physiques des dépôts (Allée, Travée, Étagère, Casier).
- Gestion des numéros de série unitaires (sérialisation & suivi de garantie).
- Moteur de suggestion automatique de réapprovisionnement basé sur la VMJ.

### ⚫ Phase 7 : Trésorerie Avancée, Fiscalité Légale & FEC (Semaines 13-14)
- Rapprochement bancaire électronique (import OFX, CSV, MT940 avec matching automatique).
- Fichiers d'ordres de virement bancaire de masse.
- Gestion de la petite caisse (menues dépenses d'avance).
- Module d'export légal FEC (Fichier des Écritures Comptables).
- Clôture annuelle d'exercice avec génération automatique des À-Nouveaux.
- États de synthèse officiels : Bilan (Actif/Passif) et CPC.

### ⚪ Phase 8 : Immobilisations, Analytique, Communication & Sécurité (Semaines 15-16)
- Immobilisations (amortissements linéaires et dégressifs marocains, dotations, cessions).
- Comptabilité analytique et rentabilité par chantier.
- Service SMTP / Mailer pour l'envoi direct en 1 clic des documents PDF avec templates HTML.
- Workflow "Mot de passe oublié" sécurisé par token temporaire.
- Double facteur d'authentification 2FA / TOTP.
- Piste d'audit complète (Audit Trail / CDC) sur toutes les actions sensibles.

---

## 8. CHECKLIST COMPLÈTE DE SUIVI & RECETTE

### 1. Documents PDF & Impression (JasperReports)
- [ ] Création du template `bon_preparation.jrxml` et exposition dans `ImpressionController`.
- [ ] Création du template `transfert_stock.jrxml` (bordereau de transfert inter-dépôts).
- [ ] Création du template `fiche_comptage_inventaire.jrxml` (mode aveugle et assisté).
- [ ] Création du template `recu_paiement.jrxml` (format A5 et format ticket 80mm).
- [ ] Création du template `bordereau_remise_cheques.jrxml` pour la remise en banque.
- [ ] Création du template `releve_compte_client.jrxml` avec historique débit/crédit et solde.
- [ ] Création du template `facture_achat.jrxml` pour impression de la facture fournisseur.
- [ ] Endpoint d'impression de planches d'étiquettes code-barres (Avery / thermique).
- [ ] Raccordement ou suppression contrôlée de `commande.jrxml` et nettoyage du sous-rapport.
- [ ] Ajout des boutons d'impression correspondants dans l'IHM Next.js (`agenceweb`).

### 2. Achats & Fournisseurs Avancés
- [ ] Création de l'entité `AvoirFournisseur` et `LigneAvoirFournisseur`.
- [ ] Endpoints CRUD et validation formelle de l'avoir fournisseur.
- [ ] Logique de déduction d'un avoir sur les prochaines factures d'achat du même fournisseur.
- [ ] Cycle de vie complet de `FactureAchat` (brouillon, validée, soldée, annulée).
- [ ] Moteur de 3-Way Matching (rapprochement PO ⟷ Réception ⟷ Facture).
- [ ] Blocage automatique des paiements sur factures d'achat en litige.
- [ ] Workflow d'approbation des écarts de rapprochement par un responsable.

### 3. Ventes, Caisse POS & CRM
- [ ] Gestion des factures d'acompte et calcul de la TVA collectée sur acompte.
- [ ] Ligne d'imputation négative sur la facture de solde finale.
- [ ] Support du règlement fractionné multi-modes sur le point de vente POS.
- [ ] Enregistrement multi-lignes de paiement avec calcul de rendu de monnaie.
- [ ] Moteur de conversion des points de fidélité en bons d'achat ou remises directes.
- [ ] Table de tarification dégressive par quantité/paliers.
- [ ] Composant Next.js de signature électronique sur écran tactile pour Bon de Livraison.
- [ ] Incrustation de la signature émargée sur le PDF du Bon de Livraison.

### 4. Stocks & Entrepôts
- [ ] Calcul et mise à jour du PUMP lors de chaque réception de marchandise.
- [ ] Valorisation du stock en temps réel basée sur le PUMP.
- [ ] Modélisation de l'entité `EmplacementStock` (dépôt, allée, travée, étagère, casier).
- [ ] Entité `NumeroSerieProduit` et traçabilité unitaire de bout en bout (entrée, vente, garantie).
- [ ] Algorithme de calcul de la VMJ et suggestion automatique de commandes de réapprovisionnement.

### 5. Trésorerie, Rapprochement & Banque
- [ ] Parser de relevés bancaires multi-formats : CSV, OFX, MT940, CAMT.053.
- [ ] Moteur de pointage et matching automatique (date, montant, référence).
- [ ] Module de génération des ordres de virement bancaires électroniques groupés.
- [ ] Gestion de la caisse de menues dépenses (avances, justificatifs, remboursements).

### 6. Comptabilité Légale & Déclarations
- [ ] Modélisation du Plan Comptable Général Marocain (PCGM) et script d'initialisation.
- [ ] Journalisation automatique des écritures (Ventes, Achats, Encaissements, Décaissements).
- [ ] Lettrage comptable (attribution des codes de lettrage `AA`, `AB`...).
- [ ] Assistant de clôture annuelle d'exercice et génération automatique des À-Nouveaux.
- [ ] Génération de l'export FEC (Fichier des Écritures Comptables) conforme aux normes DGI.
- [ ] Génération des états de synthèse légaux : Bilan officiel (Actif/Passif) et CPC.

### 7. Communication, Sécurité & Système
- [ ] Configuration du service SMTP Spring Boot (`JavaMailSender`).
- [ ] Envoi direct en 1 clic de documents PDF par email aux tiers avec templates dynamiques.
- [ ] Workflow de réinitialisation de mot de passe par token temporaire envoyé par email.
- [ ] Intégration du Double Facteur d'Authentification (2FA / TOTP) avec scan de QR Code.
- [ ] Intercepteur d'audit trail et table `AuditLog` historisant toutes les modifications sensibles (CDC).
