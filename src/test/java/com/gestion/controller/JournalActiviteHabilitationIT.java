package com.gestion.controller;

import com.acommon.config.SuperAdminHabilitations;
import com.gestion.compta.support.AbstractComptaIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le journal d'activité n'est visible que par l'admin de l'entreprise (et le superadmin)
 * ou par un rôle qui porte l'habilitation JOURNAL_ACTIVITE_READ.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class JournalActiviteHabilitationIT extends AbstractComptaIT {

    private static final String JOURNAL = "/api/dashboard/journal";

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(authorities = {"ROLE_VENDEUR", "VENTE_READ", "FACTURE_READ"})
    void refuseSansHabilitation() throws Exception {
        mvc.perform(get(JOURNAL)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_VENDEUR", "JOURNAL_ACTIVITE_READ"})
    void autoriseAvecHabilitation() throws Exception {
        mvc.perform(get(JOURNAL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void autoriseLAdminDEntreprise() throws Exception {
        mvc.perform(get(JOURNAL)).andExpect(status().isOk());
    }

    @Test
    void habilitationProposeeDansLeCatalogueDesRoles() {
        assertThat(SuperAdminHabilitations.BUSINESS_PERMISSIONS).contains("JOURNAL_ACTIVITE_READ");
    }
}
