package com.gestion.ai.tools;

import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.Facture;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.FactureRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class FactureAiTools {

    private final FactureRepository factureRepository;
    private final ClientRepository clientRepository;

    public FactureAiTools(FactureRepository factureRepository, ClientRepository clientRepository) {
        this.factureRepository = factureRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Résumé global des créances impayées et échues pour le tenant
     */
    public Map<String, Object> totalCreancesImpayees(Long tenantId) {
        BigDecimal totalCreances = factureRepository.sumTotalCreancesByPointDeVenteId(tenantId);
        BigDecimal creancesEchues = factureRepository.sumCreancesEchuesByPointDeVenteId(LocalDate.now(), tenantId);
        Long nbFacturesImpayees = factureRepository.countFacturesImpayeesByPointDeVenteId(tenantId);

        Map<String, Object> recap = new LinkedHashMap<>();
        recap.put("totalCreancesRestantes", totalCreances != null ? totalCreances : BigDecimal.ZERO);
        recap.put("totalCreancesEchues", creancesEchues != null ? creancesEchues : BigDecimal.ZERO);
        recap.put("nombreFacturesImpayees", nbFacturesImpayees != null ? nbFacturesImpayees : 0L);
        return recap;
    }

    /**
     * Liste des factures impayées pour un client spécifique
     */
    public List<Map<String, Object>> facturesImpayeesClient(Long tenantId, String clientNom) {
        List<Client> matches = clientRepository.searchClients(clientNom != null ? clientNom.trim() : "", tenantId);
        if (matches.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> resultats = new ArrayList<>();
        for (Client c : matches) {
            List<Facture> factures = factureRepository.findFacturesImpayeesByClientIdAndPointDeVenteId(c.getId(), tenantId);
            for (Facture f : factures) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("numeroFacture", f.getNumeroFacture());
                item.put("clientNom", c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
                item.put("dateFacture", f.getDateFacture());
                item.put("dateEcheance", f.getDateEcheance());
                item.put("montantTTC", f.getMontantTTC());
                item.put("montantPaye", f.getMontantPaye());
                item.put("montantRestant", f.getMontantRestant());
                item.put("estEchue", f.getDateEcheance() != null && f.getDateEcheance().isBefore(LocalDate.now()));
                resultats.add(item);
            }
        }
        return resultats;
    }

    /**
     * Liste des factures en retard de paiement (échéance dépassée)
     */
    public List<Map<String, Object>> facturesEchues(Long tenantId) {
        List<Facture> factures = factureRepository.findFacturesEchuesByPointDeVenteId(LocalDate.now(), tenantId);
        List<Map<String, Object>> resultats = new ArrayList<>();
        int limit = Math.min(factures.size(), 15);

        for (int i = 0; i < limit; i++) {
            Facture f = factures.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numeroFacture", f.getNumeroFacture());
            item.put("clientNom", f.getClient() != null ? f.getClient().getNomComplet() : "Client");
            item.put("dateFacture", f.getDateFacture());
            item.put("dateEcheance", f.getDateEcheance());
            item.put("montantRestant", f.getMontantRestant());
            resultats.add(item);
        }
        return resultats;
    }
}
