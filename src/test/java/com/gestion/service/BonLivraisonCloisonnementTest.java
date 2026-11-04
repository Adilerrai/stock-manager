package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.repository.BonLivraisonClientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un BL d'une autre société existe en base, mais pas pour cet utilisateur : aucune action ne doit l'atteindre. */
class BonLivraisonCloisonnementTest {

    private static final long TENANT = 7L;
    private static final long BL_AUTRE_TENANT = 42L;

    private final BonLivraisonClientRepository depot = mock(BonLivraisonClientRepository.class);
    private final BonLivraisonClientService service = new BonLivraisonClientService(depot, null, null, null, null, null,
            null, null, null, null, null, null, null);

    @BeforeEach
    void preparer() {
        BonLivraisonClient autre = new BonLivraisonClient();
        autre.setId(BL_AUTRE_TENANT);
        autre.setPointDeVenteId(8L);
        autre.setStatut(StatutLivraison.LIVREE);
        when(depot.findByIdAndPointDeVenteId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(depot.findById(BL_AUTRE_TENANT)).thenReturn(Optional.of(autre));
        TenantContext.setCurrentTenant(TENANT);
    }

    @AfterEach
    void effacer() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void unUtilisateurNAtteintAucunBlDUneAutreSociete() {
        connecter("ROLE_ADMIN");
        List<Executable> actions = List.of(
                () -> service.expedierBL(BL_AUTRE_TENANT, null, null),
                () -> service.validerLivraisonParLivreur(BL_AUTRE_TENANT, null, null),
                () -> service.validerEtExpedierBL(BL_AUTRE_TENANT),
                () -> service.marquerCachete(BL_AUTRE_TENANT, null, null),
                () -> service.annulerBonLivraisonClient(BL_AUTRE_TENANT),
                () -> service.remettreEnBrouillon(BL_AUTRE_TENANT),
                () -> service.updateBonLivraisonClient(BL_AUTRE_TENANT, new BonLivraisonClientDTO()),
                () -> service.updateDateBl(BL_AUTRE_TENANT, Map.of("date", "2026-01-01")));

        for (Executable action : actions) {
            CommonException erreur = assertThrows(CommonException.class, action);
            assertEquals(HttpStatus.NOT_FOUND, erreur.getStatus());
        }
        verify(depot, never()).save(any());
    }

    @Test
    void leSuperadminRetrouveLeBlHorsDeSonTenant() {
        connecter("ROLE_SUPERADMIN");

        // Trouvé : c'est la règle métier qui répond, un BL livré ne se réexpédie pas
        CommonException erreur = assertThrows(CommonException.class, () -> service.expedierBL(BL_AUTRE_TENANT, null, null));

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatus());
    }

    private static void connecter(String nomRole) {
        Role role = new Role();
        role.setNom(nomRole);
        User utilisateur = new User();
        utilisateur.setRole(role);
        utilisateur.setTenantId(TENANT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(utilisateur, null, List.of()));
    }
}
