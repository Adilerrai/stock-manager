# 🗺️ Roadmap Commerciale Cible — Spécifications & Backlog

Ce document consigne l'ensemble des fonctionnalités du module **Commercial** planifiées pour les prochaines étapes, classées par Sprint opérationnel.

---

## 📊 Tableau de bord d'avancement

| Sprint | Domaine | Statut |
| :---: | :--- | :---: |
| **Sprint 1** | **Commandes, Tarifs, Reliquats & Réservations Stock** |  **TERMINÉ** |
| **Sprint 2** | **Crédit Commercial & Gestion du Risque Client** | ⏳ *À venir* |
| **Sprint 3** | **Logistique, Expéditions & Preuve de Livraison (POD)** | ⏳ *À venir* |
| **Sprint 4** | **CRM & Pilotage de la Force de Vente** | ⏳ *À venir* |
| **Sprint 5** | **Piste d'Audit Générale & Workflows de Validation** | ⏳ *À venir* |
| **Sprint 6** | **Analytics, Décisionnel & Outils de Productivité** | ⏳ *À venir* |

---

## 🛡️ SPRINT 2 : Crédit Commercial & Sécurisation Financière

### 1. Objectifs
Sécuriser les ventes contre le risque d'impayés sans alourdir les flux commerciaux et sans nécessiter de comptabilité générale.

### 2. Fonctionnalités détaillées
* **Plafond de crédit par client :**
  * Définition d'un montant maximal d'encours autorisé (`plafondCredit` en MAD) sur la fiche Client.
  * Délais de paiement contractuels : Comptant, 30j fin de mois, 60j, etc.
* **Calcul de l'Encours Commercial en temps réel :**
  $$\text{Encours Réel} = \sum \text{Factures non réglées} + \sum \text{BL livrés non encore facturés}$$
  $$\text{Encours Prévisionnel} = \text{Encours Réel} + \text{Montant TTC nouvelle commande}$$
* **Seuils d'alerte et blocage automatique :**
  * Alerte préventive dès $85\%$ ou $90\%$ d'utilisation du plafond.
  * Blocage automatique si $\text{Encours Prévisionnel} > \text{Plafond}$ $\rightarrow$ Commande mise en statut `EN_ATTENTE_VALIDATION_CREDIT`.
* **Validation exceptionnelle (Dérogation) :**
  * Pouvoir accordé uniquement au Superviseur / Direction Financière.
  * Déblocage avec saisie obligatoire d'un motif et enregistrement dans la piste d'audit.

### 3. Modèle de données cible
* Modifications sur `Client` : `plafond_credit`, `delai_paiement_jours`, `conditions_paiement`, `bloque_vente_manuel`.
* Table `derogation_credit` : `commande_id`, `valide_par_id`, `date_validation`, `motif`, `montant_depassement`.

---

## 🚚 SPRINT 3 : Logistique, Expéditions & Preuve de Livraison (POD)

### 1. Objectifs
Organiser les sorties d'entrepôt, l'acheminement physique et la traçabilité jusqu'à la remise au client.

### 2. Fonctionnalités détaillées
* **Référentiel Transporteurs & Véhicules :**
  * Fiches transporteurs, contacts, grilles tarifaires par zone/poids.
  * Modes d'expédition : Retrait dépôt, flotte interne, transporteur tiers, coursier express.
* **Tournées de livraison :**
  * Regroupement de plusieurs bons de livraison (BL) par secteur géographique ou itinéraire.
  * Affectation d'un chauffeur et d'un véhicule à une tournée.
  * Génération d'une feuille de route / bon de chargement récapitulatif.
* **Suivi de livraison & Preuve de Livraison (POD) :**
  * Suivi du statut du BL : `EN_PREPARATION` $\rightarrow$ `EN_ROUTE` $\rightarrow$ `LIVRE` / `ECHEC_LIVRAISON`.
  * Émargement numérique / signature sur écran smartphone/tablette ou upload de la photo du bon tamponné.
* **Livraison multi-dépôts :**
  * Capacité d'expédier les lignes d'une même commande client depuis des dépôts géographiques différents.

### 3. Modèle de données cible
* Table `transporteur` : `nom`, `telephone`, `tarif_base`, `contact`.
* Table `tournee_livraison` : `reference`, `date_tournee`, `chauffeur_id`, `vehicule_immatriculation`, `statut`.
* Ajouts sur `bon_livraison_client` : `tournee_id`, `transporteur_id`, `signature_pod_url`, `statut_livraison`.

---

## 👥 SPRINT 4 : CRM & Pilotage des Commerciaux

### 1. Objectifs
Gérer l'amont de la vente (prospection, opportunités) et motiver/mesurer les performances des vendeurs.

### 2. Fonctionnalités détaillées
* **Gestion des Prospects & Contacts :**
  * Fiches Prospects distinctes des clients officiels.
  * Contacts multiples par entreprise (Directeur, Acheteur, Comptable, Réceptionniste).
  * **Conversion en 1 clic :** Transformer un Prospect qualifié en Client officiel avec report de tous les historiques.
* **Pipeline Commercial (Vue Kanban) :**
  * Colonnes : *Nouveau contact* $\rightarrow$ *Qualification* $\rightarrow$ *Devis envoyé* $\rightarrow$ *Négociation* $\rightarrow$ *Gagné / Perdu*.
  * Saisie obligatoire du **motif de perte** en cas d'échec (*Prix concurrent, budget insuffisant, projet reporté*).
* **Activités & Relances :**
  * Journal des interactions : appels téléphoniques, e-mails, comptes-rendus de RDV.
  * Relances automatiques programmées avec alertes dans le tableau de bord.
* **Pilotage de l'équipe commerciale :**
  * Affectation de portefeuilles clients / secteurs par commercial.
  * Suivi des objectifs mensuels/annuels (Objectif CA vs CA Réalisé).
  * Calcul automatique des commissions selon des règles configurables (% sur CA facturé ou % sur marge brute).

### 3. Modèle de données cible
* Tables : `prospect`, `contact_prospect`, `opportunite_vente`, `activite_commerciale`.
* Table `commission_commerciale` : barèmes, périodes et calculs d'intéressement.

---

## 🛡️ SPRINT 5 : Piste d'Audit Générale & Workflows de Validation

### 1. Objectifs
Garantir la responsabilité, la conformité et la sécurité de toutes les opérations sensibles.

### 2. Fonctionnalités détaillées
* **Traçabilité totale des modifications (Audit Trail) :**
  * Enregistrement automatique : *Utilisateur, Date/Heure, Action, Entité, Ancienne valeur, Nouvelle valeur, Motif*.
* **Circuits de validation obligatoires :**
  * Vente sous le prix minimum (Prix plancher).
  * Dépassement du plafond de crédit client.
  * Remise commerciale supérieure au seuil autorisé du vendeur.
  * Annulation d'une commande confirmée ou d'un bon de livraison.
  * Émission d'un Avoir client.

---

## 📊 SPRINT 6 : Analytics, Reporting & Productivité

### 1. Objectifs
Donner une visibilité en temps réel sur la rentabilité commerciale et fluidifier le travail quotidien.

### 2. Fonctionnalités détaillées
* **Tableaux de bord des Ventes :**
  * CA et Marge brute par produit, par client, par famille et par commercial.
  * Panier moyen et fréquence d'achat.
  * Détection des clients dormants ou en risque de perte (*churn*).
  * Analyse Pareto / ABC (les 20% de clients/produits générant 80% du CA).
* **Outils pratiques & Productivité :**
  * Import / Export Excel massif (catalogues, tarifs, prospects).
  * Duplication de devis/commandes en 1 clic.
  * Partage multicanal direct (Email, PDF professionnel, lien WhatsApp).
