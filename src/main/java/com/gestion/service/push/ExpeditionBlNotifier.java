package com.gestion.service.push;

import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/** La règle unique de tous les chemins qui expédient un BL : qu'est-ce qui mérite une notification ? */
@Component
public class ExpeditionBlNotifier {

    /** Un BL déjà parti qui repasse à EXPEDIE n'est pas une nouvelle expédition. */
    private static final Set<StatutLivraison> DEJA_PARTI = EnumSet.of(StatutLivraison.EXPEDIE, StatutLivraison.EN_LIVRAISON,
            StatutLivraison.PARTIELLE, StatutLivraison.CACHETE, StatutLivraison.LIVREE);

    private final ApplicationEventPublisher evenements;

    public ExpeditionBlNotifier(ApplicationEventPublisher evenements) {
        this.evenements = evenements;
    }

    public void signaler(BonLivraisonClient bl, StatutLivraison statutPrecedent) {
        if (bl.getStatut() == StatutLivraison.EXPEDIE) {
            publier(bl, statutPrecedent);
        }
    }

    /**
     * L'écran du livreur appelle « BL Expédié » une action qui met le BL en CACHETE, pas en EXPEDIE :
     * le statut ne suffit donc pas ici. Seule exception, un BL annulé, que la déclaration ne modifie pas.
     */
    public void signalerDeclarationLivreur(BonLivraisonClient bl, StatutLivraison statutPrecedent) {
        if (bl.getStatut() != StatutLivraison.ANNULEE) {
            publier(bl, statutPrecedent);
        }
    }

    private void publier(BonLivraisonClient bl, StatutLivraison statutPrecedent) {
        if (DEJA_PARTI.contains(statutPrecedent) || !blDuTenantDeLActeur(bl)) {
            return;
        }
        evenements.publishEvent(new BonLivraisonExpedieEvent(bl.getId(), bl.getNumeroBl(), bl.getPointDeVenteId()));
    }

    /**
     * Les ids de societes et de point_de_vente peuvent se recouvrir : un BL saisi dans un dossier de cabinet
     * porterait l'id d'un autre tenant. Sans rattachement direct à l'acteur, on ne notifie pas.
     */
    private static boolean blDuTenantDeLActeur(BonLivraisonClient bl) {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null || !(authentification.getPrincipal() instanceof User acteur)) {
            return false;
        }
        Long tenantDuBl = bl.getPointDeVenteId();
        return tenantDuBl != null && (tenantDuBl.equals(acteur.getPointDeVenteId()) || tenantDuBl.equals(acteur.getTenantId()));
    }
}
