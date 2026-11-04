package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.LivreDtos.PeriodeOuverteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CloturePeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ControlePeriodeDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.repository.compta.PeriodeRepository.LignePeriode;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PeriodeService {

    private static final int PERIODES_OUVERTES_MAX = 2;

    private final PeriodeRepository periodes;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public PeriodeService(PeriodeRepository periodes, LivreCourantService livreCourant, AuditService audit) {
        this.periodes = periodes;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ControlePeriodeDTO controler(Long periodeId) {
        Long livreId = livreCourant.exiger().livreId();
        LignePeriode periode = exiger(livreId, periodeId);
        return new ControlePeriodeDTO(periodeId, blocages(livreId, periode),
                periodes.nbBrouillons(livreId, periode.du(), periode.au()), null);
    }

    @Transactional
    public CloturePeriodeDTO cloturer(Long periodeId) {
        Long livreId = livreCourant.exiger().livreId();
        LignePeriode periode = exiger(livreId, periodeId);
        List<String> blocages = blocages(livreId, periode);
        if (!blocages.isEmpty()) {
            throw new CommonException(blocages.get(0), HttpStatus.CONFLICT, "COMPTA_PERIODE_NON_CLOTURABLE");
        }
        periodes.cloturer(livreId, periodeId, livreCourant.auteur());
        audit.logAction(ActionAudit.CLOTURE, "compta_periodes", periodeId, "Période " + periode.du() + " au " + periode.au());
        Optional<PeriodeOuverteDTO> ouverte = ouvrirSuivantes(livreId);
        periodes.ouvrirClotureSiPossible(livreId, periode.exerciceId());
        boolean exerciceRequis = ouverte.isEmpty() && periodes.nbCourantesOuvertes(livreId) == 0;
        return new CloturePeriodeDTO(periodeId, "CLOTUREE", ouverte.orElse(null), exerciceRequis);
    }

    /** Le livre garde deux périodes courantes ouvertes tant qu'il y en a à venir. Renvoie la dernière ouverte par cet appel. */
    public Optional<PeriodeOuverteDTO> ouvrirSuivantes(Long livreId) {
        Optional<PeriodeOuverteDTO> derniere = Optional.empty();
        while (periodes.nbCourantesOuvertes(livreId) < PERIODES_OUVERTES_MAX) {
            Optional<PeriodeOuverteDTO> prochaine = periodes.prochaineAVenir(livreId);
            if (prochaine.isEmpty()) {
                break;
            }
            periodes.ouvrir(livreId, prochaine.get().id());
            derniere = prochaine;
        }
        return derniere;
    }

    private List<String> blocages(Long livreId, LignePeriode periode) {
        List<String> blocages = new ArrayList<>();
        if (!"COURANTE".equals(periode.typePeriode())) {
            blocages.add("Seule une période courante se ferme ici");
            return blocages;
        }
        if (!"OUVERTE".equals(periode.statut())) {
            blocages.add("La période est " + ("CLOTUREE".equals(periode.statut()) ? "déjà clôturée" : "à venir"));
            return blocages;
        }
        if (periodes.anterieureNonCloturee(livreId, periode.du())) {
            blocages.add("Une période antérieure reste ouverte");
        }
        long brouillons = periodes.nbBrouillons(livreId, periode.du(), periode.au());
        if (brouillons > 0) {
            blocages.add(brouillons + " brouillon(s) daté(s) de la période : validez-les ou supprimez-les");
        }
        return blocages;
    }

    private LignePeriode exiger(Long livreId, Long id) {
        return periodes.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Période", "id", id));
    }
}
