package com.gestion.compta;

import com.acommon.config.ComptaEntreeJsonConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.AttestationRasDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComptabiliserRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresRequest;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.ImmobilisationComptaService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.MoteurComptabilisation;
import com.gestion.service.compta.TvaDeclarationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Le contrat contre le réel, route par route, depuis le document versionné : chaque GET répond exactement les champs de son schéma, les montants en chaînes.
 * Sur un autre livre, chaque route qui porte un identifiant répond 404. Les corps refusent un champ inconnu, sans changer le commercial.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ContratReponsesIT extends AbstractComptaIT {

    private static final String RACINE = "/api/v1/compta";
    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");
    private static final Pattern PARAMETRE = Pattern.compile("\\{([a-zA-Z]+)}");

    @Autowired private MockMvc mvc;
    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private EcritureService ecritures;
    @Autowired private JournauxRepository journaux;
    @Autowired private TiersRepository tiers;
    @Autowired private TvaDeclarationService tva;
    @Autowired private ImmobilisationComptaService immobilisations;
    @Autowired private MoteurComptabilisation moteur;
    @Autowired private ComptaEntreeJsonConverter convertisseur;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private final ObjectMapper json = new ObjectMapper();
    private JsonNode contrat;
    private Long pdv;
    private Long pdvAutre;
    private LivreDTO livre;
    private Long ecriture;
    private Long brouillon;
    private Long client;
    private Long fournisseurTiers;
    private Long facture;
    private Long declaration;
    private Long fiche;
    private Long lot;
    private Long periode;
    private Long compteFinancier;

    @BeforeEach
    void preparer() throws Exception {
        contrat = json.readTree(Files.readString(Path.of("src/main/resources/compta/openapi-compta.json"), StandardCharsets.UTF_8));
        pdvAutre = SocieteDeTest.creer().pointDeVenteId();
        activer(pdvAutre);
        pdv = SocieteDeTest.creer().pointDeVenteId();
        livre = activer(pdv);
        ecriture = passer("2026-01-05", "BQ", "5141", null, "1111", null, "500000.00");
        client = tiers.creer(livre.livreId(), new TiersCreationRequest("CLIENT", "Droguerie", null, null, "3421", null, null, null));
        passer("2026-01-06", "VT", "3421", client, "7111", null, "1000.00");
        fournisseurTiers = tiers.creer(livre.livreId(), new TiersCreationRequest("FOURNISSEUR", "Sebou", null, null, "4411", null, null, null));
        JournalDTO od = journal("OD");
        BrouillonDTO b = ecritures.creerBrouillon(new BrouillonRequest(od.id(), DU.plusDays(9), DU.plusDays(9), "BR-1", "En attente", "COURANTE", null, false, List.of(
                new BrouillonLigneRequest("6111", null, new BigDecimal("10.00"), BigDecimal.ZERO, null, null, null, null, null, null, null, null),
                new BrouillonLigneRequest("5141", null, BigDecimal.ZERO, new BigDecimal("10.00"), null, null, null, null, null, null, null, null))));
        brouillon = b.id();
        tva.enregistrerParametres(2026, new TvaParametresRequest(true, "ENCAISSEMENT", "MENSUELLE", new BigDecimal("100"), false, 1, BigDecimal.ZERO));
        declaration = tva.creer(new TvaCreationRequest(2026, 1)).id();
        fiche = immobilisations.creer(new ImmobilisationRequest("Chariot", "23321", null, null, LocalDate.parse("2026-01-10"), null, new BigDecimal("1000.00"), null, null, "LINEAIRE", 60, null, "BANQUE", null, null, null, null, null)).id();
        Long fournisseur = inserer("INSERT INTO fournisseurs (raison_social, pays, point_de_vente_id) VALUES ('Sebou commercial', 'Maroc', ?) RETURNING id", pdv);
        facture = inserer("INSERT INTO factures_achat (numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) VALUES (?, '2026-01-12'::timestamp, ?, 100, 20, 120, 'EN_ATTENTE', ?) RETURNING id", "FA-" + pdv, fournisseur, pdv);
        lot = moteur.comptabiliser(new ComptabiliserRequest(List.of(new PieceDemandee("FACTURE_ACHAT", facture, "COMPTABILISATION", null, null)))).lotId();
        periode = jdbc.queryForObject("SELECT id FROM compta_periodes WHERE livre_id = ? AND numero = 1", Long.class, livre.livreId());
        compteFinancier = inserer("INSERT INTO comptes_financiers (code, nom, type, devise, solde_actuel, point_de_vente_id, actif) VALUES (?, 'Banque', 'COMPTE_BANCAIRE', 'MAD', 0, ?, TRUE) RETURNING id", "BQ-" + pdv, pdv);
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    private LivreDTO activer(Long pointDeVente) {
        ContexteDeTest.surPointDeVente(pointDeVente);
        abonnements.modifier(pointDeVente, true);
        return livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
    }

    private JournalDTO journal(String code) {
        return journaux.lister(livre.livreId()).stream().filter(x -> x.code().equals(code)).findFirst().orElseThrow();
    }

    private Long passer(String date, String code, String debit, Long tiersDebit, String credit, Long tiersCredit, String montant) {
        LocalDate jour = LocalDate.parse(date);
        BigDecimal m = new BigDecimal(montant);
        BrouillonDTO b = ecritures.creerBrouillon(new BrouillonRequest(journal(code).id(), jour, jour, "P-" + date + code, "Essai", "COURANTE", null, false, List.of(
                new BrouillonLigneRequest(debit, tiersDebit, m, BigDecimal.ZERO, null, null, null, null, null, null, null, null),
                new BrouillonLigneRequest(credit, tiersCredit, BigDecimal.ZERO, m, null, null, null, null, null, null, null, null))));
        return ecritures.validerBrouillon(b.id()).id();
    }

    private Long inserer(String sql, Object... parametres) {
        return new TransactionTemplate(transactions).execute(s -> {
            jdbc.execute("SET LOCAL session_replication_role = replica");
            return jdbc.queryForObject(sql, Long.class, parametres);
        });
    }

    // --- Adresses

    private String valeurDeChemin(String gabarit, String nom) {
        String route = gabarit.substring(RACINE.length());
        return switch (nom) {
            case "annee" -> "2026";
            case "trimestre", "mois" -> "1";
            case "numero" -> "5141";
            case "code" -> "BILAN_ACTIF";
            case "ligne" -> "ACT.514";
            case "tableau" -> "T3";
            case "type" -> "RELEVE";
            case "sourceType" -> "FACTURE_ACHAT";
            case "sourceId" -> String.valueOf(facture);
            case "compteFinancierId" -> String.valueOf(compteFinancier);
            case "pointDeVenteId" -> String.valueOf(pdv);
            case "id" -> String.valueOf(identifiant(route));
            default -> throw new IllegalStateException("Paramètre de chemin sans valeur d'essai : " + nom + " dans " + gabarit);
        };
    }

    private Long identifiant(String route) {
        Map<String, Long> prefixes = Map.of("/exercices/", livre.exerciceCourantId(), "/ecritures/", ecriture, "/brouillons/", brouillon, "/tiers/", client, "/ras/tiers/", fournisseurTiers, "/tva/declarations/", declaration,
                "/immobilisations/", fiche, "/delais/factures/", facture, "/passerelle/lots/", lot, "/periodes/", periode);
        return prefixes.entrySet().stream().filter(e -> route.startsWith(e.getKey())).findFirst().map(Map.Entry::getValue)
                .orElseThrow(() -> new IllegalStateException("Identifiant d'essai absent pour " + route));
    }

    private String valeurDeRequete(String gabarit, String nom) {
        return switch (nom) {
            case "annee" -> "2026";
            case "exerciceId" -> String.valueOf(livre.exerciceCourantId());
            case "du" -> "2026-01-01";
            case "au" -> "2026-01-31";
            case "colonne" -> "NET";
            case "tiersId" -> String.valueOf(client);
            case "compte" -> gabarit.contains("lettrage") || gabarit.contains("balance") ? "3421" : gabarit.contains("comptes-lies") ? "23321" : "5141";
            default -> throw new IllegalStateException("Paramètre de requête sans valeur d'essai : " + nom + " dans " + gabarit);
        };
    }

    private String adresse(String gabarit, JsonNode operation) {
        Matcher m = PARAMETRE.matcher(gabarit);
        StringBuilder url = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(url, valeurDeChemin(gabarit, m.group(1)));
        }
        m.appendTail(url);
        char separateur = '?';
        for (JsonNode p : operation.path("parameters")) {
            if ("query".equals(p.path("in").asText()) && p.path("required").asBoolean()) {
                url.append(separateur).append(p.path("name").asText()).append('=').append(valeurDeRequete(gabarit, p.path("name").asText()));
                separateur = '&';
            }
        }
        return url.toString();
    }

    private MockHttpServletResponse lire(String url) throws Exception {
        return mvc.perform(get(url).accept(MediaType.ALL)).andReturn().getResponse();
    }

    // --- Les réponses contre leur schéma

    /** Chaque route JSON, avec les données du livre : mêmes champs que le schéma, ni plus ni moins, et des chaînes pour les montants. */
    @Test
    void chaqueReponseAExactementLesChampsDeSonSchema() throws Exception {
        ContexteDeTest.surPointDeVente(pdv);
        List<String> fautes = new ArrayList<>();
        Set<String> vus = new TreeSet<>();
        int routes = 0;
        for (Iterator<Map.Entry<String, JsonNode>> i = contrat.path("paths").fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> route = i.next();
            JsonNode operation = route.getValue().path("get");
            JsonNode schema = operation.path("responses").path("200").path("content").path("application/json").path("schema");
            // Les abonnements se règlent côté administrateur : pour un utilisateur du dossier, un refus d'accès est la bonne réponse.
            if (operation.isMissingNode() || schema.isMissingNode() || route.getKey().startsWith(RACINE + "/abonnements")) {
                continue;
            }
            routes++;
            String url = adresse(route.getKey(), operation);
            MockHttpServletResponse reponse = lire(url);
            if (reponse.getStatus() != 200) {
                fautes.add(url + " : statut " + reponse.getStatus() + " " + reponse.getContentAsString(StandardCharsets.UTF_8));
                continue;
            }
            verifier(url, json.readTree(reponse.getContentAsString(StandardCharsets.UTF_8)), schema, fautes, vus);
        }
        assertTrue(fautes.isEmpty(), () -> fautes.size() + " faute(s) :\n" + String.join("\n", fautes));
        assertTrue(routes >= 60, "routes JSON lues : " + routes);
        assertTrue(vus.contains("EcritureDTO") && vus.contains("TvaDeclarationDTO") && vus.contains("ImmobilisationDTO") && vus.contains("EtatDTO"), "schémas vus : " + vus);
    }

    private void verifier(String ou, JsonNode valeur, JsonNode schemaBrut, List<String> fautes, Set<String> vus) {
        JsonNode schema = schemaBrut;
        if (schema.has("$ref")) {
            String nom = schema.path("$ref").asText().replace("#/components/schemas/", "");
            vus.add(nom);
            schema = contrat.path("components").path("schemas").path(nom);
        }
        if (valeur.isNull()) {
            if (!schema.path("nullable").asBoolean() && !schemaBrut.path("nullable").asBoolean()) {
                fautes.add(ou + " : null alors que le schéma ne l'admet pas");
            }
            return;
        }
        String type = schema.path("type").asText("object");
        switch (type) {
            case "array" -> {
                if (!valeur.isArray()) {
                    fautes.add(ou + " : tableau attendu");
                    return;
                }
                int n = 0;
                for (JsonNode element : valeur) {
                    if (n++ < 20) {
                        verifier(ou + "[]", element, schema.path("items"), fautes, vus);
                    }
                }
            }
            case "object" -> {
                if (!valeur.isObject()) {
                    fautes.add(ou + " : objet attendu");
                    return;
                }
                if (schema.has("properties")) {
                    Set<String> attendus = new TreeSet<>();
                    schema.path("properties").fieldNames().forEachRemaining(attendus::add);
                    Set<String> recus = new TreeSet<>();
                    valeur.fieldNames().forEachRemaining(recus::add);
                    if (!attendus.equals(recus)) {
                        Set<String> absents = new TreeSet<>(attendus);
                        absents.removeAll(recus);
                        Set<String> enTrop = new TreeSet<>(recus);
                        enTrop.removeAll(attendus);
                        fautes.add(ou + " : champs absents " + absents + ", en trop " + enTrop);
                    }
                    for (String champ : attendus) {
                        if (recus.contains(champ)) {
                            verifier(ou + "." + champ, valeur.path(champ), schema.path("properties").path(champ), fautes, vus);
                        }
                    }
                } else if (schema.has("additionalProperties")) {
                    valeur.fields().forEachRemaining(e -> verifier(ou + "." + e.getKey(), e.getValue(), schema(schemaBrut).path("additionalProperties"), fautes, vus));
                }
            }
            case "string" -> {
                if (!valeur.isTextual()) {
                    fautes.add(ou + " : chaîne attendue, reçu " + valeur);
                }
            }
            case "integer", "number" -> {
                if (!valeur.isNumber()) {
                    fautes.add(ou + " : nombre attendu, reçu " + valeur);
                }
            }
            case "boolean" -> {
                if (!valeur.isBoolean()) {
                    fautes.add(ou + " : booléen attendu, reçu " + valeur);
                }
            }
            default -> fautes.add(ou + " : type inconnu " + type);
        }
    }

    private JsonNode schema(JsonNode brut) {
        return brut.has("$ref") ? contrat.path("components").path("schemas").path(brut.path("$ref").asText().replace("#/components/schemas/", "")) : brut;
    }

    // --- Un autre livre

    /** Lot 1.1 : un identifiant d'un autre livre répond 404, sur chaque route qui en porte un. */
    @Test
    void lIdentifiantDUnAutreLivreRepond404SurChaqueRoute() throws Exception {
        ContexteDeTest.surPointDeVente(pdvAutre);
        List<String> fautes = new ArrayList<>();
        int routes = 0;
        for (Iterator<Map.Entry<String, JsonNode>> i = contrat.path("paths").fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> route = i.next();
            JsonNode operation = route.getValue().path("get");
            String gabarit = route.getKey();
            boolean porteUnIdentifiant = gabarit.contains("{id}") || gabarit.contains("{compteFinancierId}") || gabarit.contains("{sourceId}");
            if (operation.isMissingNode() || !porteUnIdentifiant) {
                continue;
            }
            routes++;
            String url = adresse(gabarit, operation);
            int statut = lire(url).getStatus();
            if (statut != 404) {
                fautes.add(url + " : statut " + statut);
            }
        }
        assertTrue(fautes.isEmpty(), () -> fautes.size() + " route(s) sans refus :\n" + String.join("\n", fautes));
        assertTrue(routes >= 30, "routes lues : " + routes);
    }

    // --- Les corps

    @Test
    void unCorpsDuModuleRefuseUnChampInconnu() throws Exception {
        ContexteDeTest.surPointDeVente(pdv);
        String valide = "{\"typeTiers\":\"CLIENT\",\"nom\":\"Essai\",\"ice\":null,\"identifiantFiscal\":null,\"compteCollectif\":\"3421\",\"entrepriseLiee\":null,\"organismePublic\":null,\"nonResident\":null";
        assertEquals(201, mvc.perform(post(RACINE + "/tiers").contentType(MediaType.APPLICATION_JSON).content(valide + "}")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(post(RACINE + "/tiers").contentType(MediaType.APPLICATION_JSON).content(valide + ",\"nomm\":\"Faute\"}")).andReturn().getResponse().getStatus());
    }

    @Test
    void leCommercialGardeSonIndulgence() {
        assertTrue(convertisseur.canRead(TiersCreationRequest.class, TiersCreationRequest.class, MediaType.APPLICATION_JSON));
        assertFalse(convertisseur.canRead(AttestationRasDTO.class, AttestationRasDTO.class, MediaType.APPLICATION_JSON));
        assertFalse(convertisseur.canWrite(TiersCreationRequest.class, MediaType.APPLICATION_JSON));
    }
}
