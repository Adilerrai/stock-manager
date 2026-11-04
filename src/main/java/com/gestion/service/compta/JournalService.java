package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class JournalService {

    /** Le journal créé à l'activation pour chaque type : deux d'entre eux restent uniques par livre. */
    public static final List<String[]> JOURNAUX_DE_BASE = List.of(
            new String[]{"VT", "Ventes", "VENTES"},
            new String[]{"AC", "Achats", "ACHATS"},
            new String[]{"BQ", "Banque", "BANQUE"},
            new String[]{"CA", "Caisse", "CAISSE"},
            new String[]{"OD", "Opérations diverses", "OD"},
            new String[]{"AN", "À-nouveaux", "A_NOUVEAUX"},
            new String[]{"CL", "Clôture", "CLOTURE"});

    private static final Set<String> TYPES_AJOUTABLES = Set.of("BANQUE", "CAISSE");

    private final JournauxRepository journaux;
    private final CompteRepository comptes;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public JournalService(JournauxRepository journaux, CompteRepository comptes, LivreCourantService livreCourant, AuditService audit) {
        this.journaux = journaux;
        this.comptes = comptes;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<JournalDTO> lister() {
        return journaux.lister(livreCourant.exiger().livreId());
    }

    @Transactional
    public JournalDTO creer(JournalCreationRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        String code = demande.code().trim().toUpperCase();
        if (!TYPES_AJOUTABLES.contains(demande.typeJournal())) {
            throw new CommonException("Seul un journal de banque ou de caisse s'ajoute, pas " + demande.typeJournal(),
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_TYPE_REFUSE");
        }
        if (journaux.codeExiste(livreId, code)) {
            throw new CommonException("Le journal " + code + " existe déjà", HttpStatus.CONFLICT, "COMPTA_JOURNAL_EXISTANT");
        }
        CompteDTO tresorerie = comptes.lire(livreId, demande.compteTresorerie())
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "numero", demande.compteTresorerie()));
        Long id = journaux.creer(livreId, code, demande.libelle().trim(), demande.typeJournal(), tresorerie.numero());
        audit.logAction(ActionAudit.CREATION, "compta_journaux", id, code);
        return journaux.lire(livreId, id).orElseThrow();
    }
}
