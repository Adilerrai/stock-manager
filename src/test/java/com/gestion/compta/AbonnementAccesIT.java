package com.gestion.compta;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Qui active la comptabilité : l'admin du tenant pour ses dossiers, le superadmin pour tous, personne d'autre. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AbonnementAccesIT extends AbstractComptaIT {

    private static final String ROUTE = "/api/v1/compta/abonnements";

    @Autowired private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();
    private Long premier;
    private Long autre;

    @BeforeEach
    void preparer() {
        premier = SocieteDeTest.creer().pointDeVenteId();
        autre = SocieteDeTest.creer().pointDeVenteId();
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    private void connecter(Long pointDeVente, String role) {
        User utilisateur = new User();
        utilisateur.setEmail("essai@exemple.invalid");
        utilisateur.setPointDeVenteId(pointDeVente);
        utilisateur.setTenantId(pointDeVente);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(utilisateur, null, List.of(new SimpleGrantedAuthority(role))));
        TenantContext.setCurrentTenant(pointDeVente);
    }

    private MockHttpServletResponse basculer(Long pointDeVente, boolean actif) throws Exception {
        return mvc.perform(put(ROUTE + "/" + pointDeVente).contentType(MediaType.APPLICATION_JSON).content("{\"actif\":" + actif + "}")).andReturn().getResponse();
    }

    private List<Long> dossiers() throws Exception {
        MockHttpServletResponse r = mvc.perform(get(ROUTE)).andReturn().getResponse();
        assertEquals(200, r.getStatus());
        JsonNode liste = json.readTree(r.getContentAsString(StandardCharsets.UTF_8));
        return StreamSupport.stream(liste.spliterator(), false).map(n -> n.path("pointDeVenteId").asLong()).toList();
    }

    private boolean actif(Long pointDeVente) throws Exception {
        connecter(premier, "ROLE_SUPERADMIN");
        MockHttpServletResponse r = mvc.perform(get(ROUTE + "/" + pointDeVente)).andReturn().getResponse();
        assertEquals(200, r.getStatus());
        return json.readTree(r.getContentAsString(StandardCharsets.UTF_8)).path("actif").asBoolean();
    }

    @Test
    void lAdminDuTenantActiveSonDossierEtLuiSeul() throws Exception {
        connecter(premier, "ROLE_ADMIN");
        List<Long> vus = dossiers();
        assertTrue(vus.contains(premier));
        assertFalse(vus.contains(autre));

        assertEquals(200, basculer(premier, true).getStatus());
        assertEquals(404, basculer(autre, true).getStatus());
        assertEquals(404, mvc.perform(get(ROUTE + "/" + autre)).andReturn().getResponse().getStatus());

        MockHttpServletResponse livre = mvc.perform(get("/api/v1/compta/livre")).andReturn().getResponse();
        assertEquals("A_ACTIVER", json.readTree(livre.getContentAsString(StandardCharsets.UTF_8)).path("etat").asText());
        assertTrue(actif(premier));
        assertFalse(actif(autre));
    }

    @Test
    void leSuperadminVoitEtActiveTousLesDossiers() throws Exception {
        connecter(premier, "ROLE_SUPERADMIN");
        List<Long> vus = dossiers();
        assertTrue(vus.contains(premier) && vus.contains(autre));
        assertEquals(200, basculer(autre, true).getStatus());
        assertTrue(actif(autre));
        assertEquals(200, basculer(autre, false).getStatus());
        assertFalse(actif(autre));
    }

    @Test
    void lesAutresRolesSontRefuses() throws Exception {
        for (String role : List.of("ROLE_COMPTABLE", "ROLE_VENDEUR", "ROLE_GESTIONNAIRE")) {
            connecter(premier, role);
            assertEquals(403, mvc.perform(get(ROUTE)).andReturn().getResponse().getStatus(), role);
            assertEquals(403, basculer(premier, true).getStatus(), role);
        }
        assertFalse(actif(premier));
    }

    @Test
    void unDossierInconnuRepond404() throws Exception {
        connecter(premier, "ROLE_SUPERADMIN");
        assertEquals(404, basculer(99999999L, true).getStatus());
    }
}
