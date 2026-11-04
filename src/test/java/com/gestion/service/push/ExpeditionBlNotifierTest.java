package com.gestion.service.push;

import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionBlNotifierTest {

    private final List<Object> publies = new ArrayList<>();
    private final ApplicationEventPublisher publieur = publies::add;
    private final ExpeditionBlNotifier notifier = new ExpeditionBlNotifier(publieur);

    @AfterEach
    void effacer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unPremierPassageAExpedieNotifieAvecLeTenantDuBl() {
        connecter(7L);
        notifier.signaler(bl(StatutLivraison.EXPEDIE, 7L), StatutLivraison.EN_ATTENTE);

        assertEquals(List.of(new BonLivraisonExpedieEvent(42L, "BL-2026-0012", 7L)), publies);
    }

    @ParameterizedTest
    @EnumSource(value = StatutLivraison.class, names = {"EXPEDIE", "EN_LIVRAISON", "PARTIELLE", "CACHETE", "LIVREE"})
    void unBlDejaPartiNeNotifiePasDeNouveau(StatutLivraison precedent) {
        connecter(7L);
        notifier.signaler(bl(StatutLivraison.EXPEDIE, 7L), precedent);

        assertTrue(publies.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = StatutLivraison.class, names = {"CACHETE", "LIVREE", "ANNULEE"})
    void unAutreStatutFinalNeNotifiePas(StatutLivraison statut) {
        connecter(7L);
        notifier.signaler(bl(statut, 7L), StatutLivraison.EN_ATTENTE);

        assertTrue(publies.isEmpty());
    }

    @Test
    void unBlDUnAutreTenantNeNotifiePersonne() {
        connecter(7L);
        notifier.signaler(bl(StatutLivraison.EXPEDIE, 8L), StatutLivraison.EN_ATTENTE);

        assertTrue(publies.isEmpty());
    }

    @Test
    void sansUtilisateurConnecteRienNePart() {
        notifier.signaler(bl(StatutLivraison.EXPEDIE, 7L), StatutLivraison.EN_ATTENTE);

        assertTrue(publies.isEmpty());
    }

    private static void connecter(Long tenant) {
        User acteur = new User();
        acteur.setTenantId(tenant);
        acteur.setPointDeVenteId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acteur, null, List.of()));
    }

    private static BonLivraisonClient bl(StatutLivraison statut, Long tenant) {
        BonLivraisonClient bl = new BonLivraisonClient();
        bl.setId(42L);
        bl.setNumeroBl("BL-2026-0012");
        bl.setStatut(statut);
        bl.setPointDeVenteId(tenant);
        return bl;
    }
}
