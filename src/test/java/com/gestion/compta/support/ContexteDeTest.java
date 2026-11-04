package com.gestion.compta.support;

import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/** Ce que JwtAuthFilter pose pour une requête : l'utilisateur et l'en-tête de dossier. */
public final class ContexteDeTest {

    private ContexteDeTest() {
    }

    /** Un utilisateur sur son propre point de vente. */
    public static void surPointDeVente(long pointDeVenteId) {
        poser(pointDeVenteId, pointDeVenteId);
    }

    /** Un utilisateur de ce point de vente qui a basculé vers un dossier : l'en-tête diffère de son point de vente. */
    public static void surDossier(long pointDeVenteId, long societeId) {
        poser(pointDeVenteId, societeId);
    }

    public static void effacer() {
        TenantContext.clear();
        CurrentRequestContext.clear();
        SecurityContextHolder.clearContext();
    }

    private static void poser(long pointDeVenteId, long entete) {
        User utilisateur = new User();
        utilisateur.setEmail("comptable.test@exemple.invalid");
        utilisateur.setPointDeVenteId(pointDeVenteId);
        utilisateur.setTenantId(pointDeVenteId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(utilisateur, null, List.of()));
        TenantContext.setCurrentTenant(entete);
    }
}
