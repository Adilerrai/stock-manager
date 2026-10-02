package com.gestion.ai.tools;

import com.gestion.persistent.model.Client;
import com.gestion.repository.ClientRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ClientAiTools {

    private final ClientRepository clientRepository;

    public ClientAiTools(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Map<String, Object>> rechercherClient(Long tenantId, String query) {
        return rechercherClient(tenantId, query, Set.of(tenantId));
    }

    public List<Map<String, Object>> rechercherClient(Long tenantId, String query, Set<Long> accessibleTenantIds) {
        String cleanQuery = cleanClientQuery(query);

        List<Client> clients = new ArrayList<>();
        if (cleanQuery.isEmpty()) {
            clients.addAll(clientRepository.findByPointDeVenteId(tenantId));
            if (clients.isEmpty() && accessibleTenantIds != null) {
                for (Long tId : accessibleTenantIds) {
                    if (!Objects.equals(tId, tenantId)) {
                        clients.addAll(clientRepository.findByPointDeVenteId(tId));
                    }
                }
            }
        } else {
            clients.addAll(clientRepository.searchClients(cleanQuery, tenantId));
            if (clients.isEmpty() && accessibleTenantIds != null) {
                for (Long tId : accessibleTenantIds) {
                    if (!Objects.equals(tId, tenantId)) {
                        clients.addAll(clientRepository.searchClients(cleanQuery, tId));
                    }
                }
            }
        }

        List<Map<String, Object>> resultats = new ArrayList<>();
        int limit = Math.min(clients.size(), 15);
        for (int i = 0; i < limit; i++) {
            Client c = clients.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("clientId", c.getId());
            item.put("nomComplet", c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
            item.put("telephone", c.getTelephone());
            item.put("email", c.getEmail());
            item.put("ice", c.getIce());
            item.put("categorie", c.getCategorie() != null ? c.getCategorie().name() : null);
            item.put("creditAutorise", c.getCreditAutorise() != null ? c.getCreditAutorise() : BigDecimal.ZERO);
            item.put("creditUtilise", c.getCreditUtilise() != null ? c.getCreditUtilise() : BigDecimal.ZERO);
            BigDecimal dispo = (c.getCreditAutorise() != null ? c.getCreditAutorise() : BigDecimal.ZERO)
                    .subtract(c.getCreditUtilise() != null ? c.getCreditUtilise() : BigDecimal.ZERO);
            item.put("creditRestant", dispo);
            item.put("depassementCredit", dispo.compareTo(BigDecimal.ZERO) < 0);
            resultats.add(item);
        }
        return resultats;
    }

    public List<Map<String, Object>> clientsDepassementCredit(Long tenantId) {
        return clientsDepassementCredit(tenantId, Set.of(tenantId));
    }

    public List<Map<String, Object>> clientsDepassementCredit(Long tenantId, Set<Long> accessibleTenantIds) {
        List<Client> depasses = new ArrayList<>(clientRepository.findClientsAvecDepassementCredit(tenantId));
        if (depasses.isEmpty() && accessibleTenantIds != null) {
            for (Long tId : accessibleTenantIds) {
                if (!Objects.equals(tId, tenantId)) {
                    depasses.addAll(clientRepository.findClientsAvecDepassementCredit(tId));
                }
            }
        }

        List<Map<String, Object>> resultats = new ArrayList<>();
        for (Client c : depasses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("clientId", c.getId());
            item.put("nomComplet", c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
            item.put("telephone", c.getTelephone());
            item.put("creditAutorise", c.getCreditAutorise());
            item.put("creditUtilise", c.getCreditUtilise());
            BigDecimal depassement = c.getCreditUtilise().subtract(c.getCreditAutorise());
            item.put("montantDepassement", depassement);
            resultats.add(item);
        }
        return resultats;
    }

    private String cleanClientQuery(String query) {
        if (query == null) return "";
        String s = normalize(query.trim());

        // Si l'utilisateur demande une liste globale (ex: "mes clients", "liste clients", "tous les clients")
        if (s.equals("mes clients") || s.equals("clients") || s.equals("liste clients")
                || s.equals("tous les clients") || s.equals("nos clients") || s.equals("les clients")) {
            return "";
        }

        String[] stopWords = {"le client", "les clients", "client", "clients", "fiche de", "fiche du", "fiche", "de", "du"};
        for (String sw : stopWords) {
            if (s.startsWith(sw + " ")) {
                s = s.substring(sw.length()).trim();
            }
        }
        return s.trim();
    }

    private String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").trim();
    }
}
