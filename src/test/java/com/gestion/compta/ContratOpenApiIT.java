package com.gestion.compta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.persistent.dto.compta.ConsultationDtos;
import com.gestion.persistent.dto.compta.EcritureDtos;
import com.gestion.persistent.dto.compta.LivreDtos;
import com.gestion.persistent.dto.compta.MontantJson;
import com.gestion.persistent.dto.compta.ReferentielDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le contrat est versionné : toute modification de l'API comptable apparaît dans le diff.
 * Pour l'accepter : mvn -Dgroups=compta -Dtest=ContratOpenApiIT -Dcontrat.maj=true test
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContratOpenApiIT extends AbstractComptaIT {

    private static final Path INSTANTANE = Path.of("src/main/resources/compta/openapi-compta.json");
    private static final List<Class<?>> DTO = List.of(LivreDtos.class, ReferentielDtos.class, EcritureDtos.class, ConsultationDtos.class, com.gestion.persistent.dto.compta.EtatDtos.class, com.gestion.persistent.dto.compta.ClotureDtos.class, com.gestion.persistent.dto.compta.PasserelleDtos.class, com.gestion.persistent.dto.compta.TvaDtos.class, com.gestion.persistent.dto.compta.ImmobilisationDtos.class, com.gestion.persistent.dto.compta.RapprochementDtos.class);

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void leContratEgaleLInstantaneVersionne() throws Exception {
        String actuel = normaliser(document("/v3/api-docs/compta"));
        if (Boolean.getBoolean("contrat.maj")) {
            Files.writeString(INSTANTANE, actuel, StandardCharsets.UTF_8);
        }
        assertTrue(Files.exists(INSTANTANE), "Instantané absent : relancer avec -Dcontrat.maj=true");
        assertEquals(Files.readString(INSTANTANE, StandardCharsets.UTF_8), actuel,
                "Le contrat a changé : relire le diff et relancer avec -Dcontrat.maj=true");
    }

    @Test
    void leGroupeNeContientQueLesRoutesDuModuleEtToutEstRequis() throws Exception {
        ObjectMapper json = new ObjectMapper();
        ObjectNode racine = (ObjectNode) json.readTree(document("/v3/api-docs/compta"));

        racine.withObject("/paths").fieldNames().forEachRemaining(
                chemin -> assertTrue(chemin.startsWith("/api/v1/compta"), chemin));
        racine.withObject("/components/schemas").fields().forEachRemaining(schema -> {
            if (schema.getValue().has("properties")) {
                assertEquals(schema.getValue().get("properties").size(), schema.getValue().get("required").size(), schema.getKey());
            }
        });
    }

    @Test
    void leDocumentCompletRestePublieEtContientAussiLeCommercial() throws Exception {
        ObjectNode racine = (ObjectNode) new ObjectMapper().readTree(document("/v3/api-docs/tout"));

        assertTrue(racine.withObject("/paths").has("/api/factures"));
        assertTrue(racine.withObject("/paths").has("/api/v1/compta/livre"));
    }

    @Test
    void unMontantSortEnChaineEtLeContratLeDit() throws Exception {
        String json = mapper.writeValueAsString(new ConsultationDtos.TotauxDTO(new BigDecimal("1234567890123.10"), new BigDecimal("0.00")));
        assertEquals("{\"debit\":\"1234567890123.10\",\"credit\":\"0.00\"}", json);
        ObjectNode racine = (ObjectNode) new ObjectMapper().readTree(document("/v3/api-docs/compta"));
        assertEquals("string", racine.withObject("/components/schemas/TotauxDTO/properties/debit").get("type").asText());
    }

    @Test
    void chaqueMontantDuContratEstUneChaine() {
        List<String> fautes = new ArrayList<>();
        for (Class<?> conteneur : DTO) {
            for (Class<?> dto : conteneur.getDeclaredClasses()) {
                for (Field champ : dto.getDeclaredFields()) {
                    if (BigDecimal.class.equals(champ.getType()) && !champ.isAnnotationPresent(MontantJson.class)) {
                        fautes.add(dto.getSimpleName() + "." + champ.getName());
                    }
                    boolean carteAdmise = "LigneEtatDTO".equals(dto.getSimpleName()) && "valeurs".equals(champ.getName());
                    if (Object.class.equals(champ.getType()) || (Map.class.isAssignableFrom(champ.getType()) && !carteAdmise)) {
                        fautes.add(dto.getSimpleName() + "." + champ.getName() + " : type non typé");
                    }
                }
            }
        }
        assertEquals(List.of(), fautes);
    }

    private String document(String chemin) throws Exception {
        return mvc.perform(get(chemin)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    /** Clés triées et sans l'adresse du serveur : le fichier ne varie qu'avec le contrat. */
    private static String normaliser(String document) throws Exception {
        ObjectMapper json = new ObjectMapper()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        @SuppressWarnings("unchecked")
        Map<String, Object> racine = new TreeMap<>(json.readValue(document, Map.class));
        racine.remove("servers");
        return json.writeValueAsString(racine).replace("\r\n", "\n") + "\n";
    }
}
