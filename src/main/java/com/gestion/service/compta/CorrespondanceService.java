package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.gestion.persistent.dto.compta.PasserelleDtos.CompteFinancierDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.CorrespondanceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.repository.compta.PasserelleRepository;
import com.gestion.repository.compta.SourcesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aucune entité commerciale ne porte de compte. Ces lignes sont une proposition à faire relire, et l'utilisateur peut les modifier. */
@Service
public class CorrespondanceService {

    private static final String[][] PAR_DEFAUT = {
            {"VENTE_TYPE_PRODUIT", "MARCHANDISE", "7111"}, {"VENTE_TYPE_PRODUIT", "SERVICE", "71243"},
            {"VENTE_TYPE_PRODUIT", "MARCHANDISE_ETRANGER", "7113"}, {"VENTE_TYPE_PRODUIT", "SERVICE_ETRANGER", "71253"},
            {"ACHAT_TYPE_PRODUIT", "MARCHANDISE", "6111"}, {"ACHAT_TYPE_PRODUIT", "SERVICE", "61263"},
            {"RRR_VENTE", "MARCHANDISE", "7119"}, {"RRR_VENTE", "SERVICE", "71294"},
            {"RRR_ACHAT", "MARCHANDISE", "6119"}, {"RRR_ACHAT", "SERVICE", "61296"},
            {"BANQUE_DEFAUT", "", "5141"}, {"CAISSE_DEFAUT", "", "51611"},
            {"CATEGORIE_DEPENSE", "SALAIRES", "61711"}, {"CATEGORIE_DEPENSE", "LOYER", "61312"}, {"CATEGORIE_DEPENSE", "ELECTRICITE_EAU", "61251"},
            {"CATEGORIE_DEPENSE", "TRANSPORT_CARBURANT", "61428"}, {"CATEGORIE_DEPENSE", "COMMUNICATION_INTERNET", "61455"},
            {"CATEGORIE_DEPENSE", "FOURNITURES_BUREAU", "61254"}, {"CATEGORIE_DEPENSE", "ENTRETIEN_MAINTENANCE", "61335"},
            {"CATEGORIE_DEPENSE", "FRAIS_BANCAIRES", "61473"}, {"CATEGORIE_DEPENSE", "IMPOTS_TAXES", "61678"},
            {"CATEGORIE_DEPENSE", "MARKETING_PUBLICITE", "61448"}, {"CATEGORIE_DEPENSE", "DROITS_DOUANE_IMPORT", "6111"},
            {"CATEGORIE_DEPENSE", "HONORAIRES_TRANSIT_IMPORT", "61365"}, {"CATEGORIE_DEPENSE", "FRET_TRANSPORT_INTERNATIONAL", "61425"},
            {"CATEGORIE_DEPENSE", "MAGASINAGE_PORT_IMPORT", "61318"}, {"CATEGORIE_DEPENSE", "ASSURANCE_TRANSPORT_IMPORT", "61348"},
            {"CATEGORIE_DEPENSE", "FRAIS_PORTNET_IMPORT", "61473"},
            {"TYPE_CHARGE", "TVA_IMPORT", "34552"}, {"TYPE_CHARGE", "DROITS_DOUANE", "6111"}, {"TYPE_CHARGE", "FRET_INTERNATIONAL", "61425"},
            {"TYPE_CHARGE", "TRANSPORT_ORIGINE", "61425"}, {"TYPE_CHARGE", "TRANSPORT_LOCAL", "61425"}, {"TYPE_CHARGE", "FRAIS_BANCAIRES", "61473"},
    };

    private final PasserelleRepository repository;
    private final SourcesRepository sources;
    private final CompteRepository comptes;
    private final LivreCourantService livreCourant;

    public CorrespondanceService(PasserelleRepository repository, SourcesRepository sources, CompteRepository comptes, LivreCourantService livreCourant) {
        this.repository = repository;
        this.sources = sources;
        this.comptes = comptes;
        this.livreCourant = livreCourant;
    }

    /** La première lecture pose les lignes par défaut. AUTRES_CHARGES n'en a volontairement pas : le plan n'a pas de compte fourre-tout. */
    @Transactional
    public Map<String, String> assurer(Long livreId) {
        List<CorrespondanceDTO> existantes = repository.correspondances(livreId);
        if (existantes.isEmpty()) {
            String auteur = livreCourant.auteur();
            for (String[] ligne : PAR_DEFAUT) {
                if (comptes.lire(livreId, ligne[2]).map(CompteDTO::imputable).orElse(false)) {
                    repository.ecrireCorrespondance(livreId, ligne[0], ligne[1], ligne[2], auteur);
                }
            }
            for (Map<String, Object> financier : sources.comptesFinanciers(livreId)) {
                String type = String.valueOf(financier.get("type"));
                repository.ecrireCorrespondance(livreId, "COMPTE_FINANCIER", String.valueOf(financier.get("id")), type.contains("CAISSE") ? "51611" : "5141", auteur);
            }
            existantes = repository.correspondances(livreId);
        }
        Map<String, String> table = new HashMap<>();
        for (CorrespondanceDTO c : existantes) {
            table.put(c.typeCle() + "|" + c.cle(), c.compte());
        }
        return table;
    }

    @Transactional
    public List<CorrespondanceDTO> lister() {
        Long livreId = livreCourant.exiger().livreId();
        assurer(livreId);
        return repository.correspondances(livreId);
    }

    /** Un compte vide retire la ligne : la pièce repasse en anomalie, ce qui vaut mieux qu'un compte inventé. */
    @Transactional
    public List<CorrespondanceDTO> modifier(List<CorrespondanceDTO> lignes) {
        Long livreId = livreCourant.exiger().livreId();
        assurer(livreId);
        String auteur = livreCourant.auteur();
        for (CorrespondanceDTO c : lignes) {
            if (c.compte() == null || c.compte().isBlank()) {
                repository.supprimerCorrespondance(livreId, c.typeCle(), c.cle());
                continue;
            }
            CompteDTO compte = comptes.lire(livreId, c.compte().trim()).orElseThrow(() -> new CommonException("Le compte " + c.compte() + " n'est pas au plan.",
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_COMPTE_INCONNU"));
            if (!compte.imputable()) {
                throw new CommonException("Le compte " + compte.numero() + " n'est pas imputable : choisissez un sous-compte.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_COMPTE_NON_IMPUTABLE");
            }
            repository.ecrireCorrespondance(livreId, c.typeCle(), c.cle(), compte.numero(), auteur);
        }
        return repository.correspondances(livreId);
    }

    @Transactional(readOnly = true)
    public List<CompteFinancierDTO> comptesFinanciers() {
        Long livreId = livreCourant.exiger().livreId();
        return sources.comptesFinanciers(livreId).stream()
                .map(m -> new CompteFinancierDTO(((Number) m.get("id")).longValue(), String.valueOf(m.get("code")), String.valueOf(m.get("nom")), String.valueOf(m.get("type")))).toList();
    }
}
