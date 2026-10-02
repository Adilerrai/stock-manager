package com.gestion.ai.tools;

import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.repository.CommandeClientRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class VenteAiTools {

    private final CommandeClientRepository commandeClientRepository;

    public VenteAiTools(CommandeClientRepository commandeClientRepository) {
        this.commandeClientRepository = commandeClientRepository;
    }

    /**
     * Suivi des commandes clients en cours (non livrées ou en attente)
     */
    public List<Map<String, Object>> suiviCommandesEnCours(Long tenantId) {
        List<CommandeClient> commandes = commandeClientRepository.findByPointDeVenteId(tenantId);
        List<Map<String, Object>> resultats = new ArrayList<>();

        for (CommandeClient cmd : commandes) {
            StatutCommandeClient st = cmd.getStatut();
            // On retient les commandes non terminées
            if (st != StatutCommandeClient.LIVREE && st != StatutCommandeClient.ANNULEE) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("numeroCommande", cmd.getNumeroCommande());
                item.put("clientNom", cmd.getClientNom() != null ? cmd.getClientNom()
                        : (cmd.getClient() != null ? cmd.getClient().getNomComplet() : "Client"));
                item.put("dateCommande", cmd.getDateCommande());
                item.put("dateLivraisonPrevue", cmd.getDateLivraisonPrevue());
                item.put("statut", st != null ? st.name() : "INCONNU");
                item.put("montantTTC", cmd.getMontantTTC());
                resultats.add(item);
            }
        }
        return resultats;
    }

    /**
     * Dernières commandes enregistrées
     */
    public List<Map<String, Object>> dernieresCommandes(Long tenantId) {
        List<CommandeClient> commandes = commandeClientRepository.findByPointDeVenteId(tenantId);
        commandes.sort((a, b) -> {
            if (a.getDateCommande() == null || b.getDateCommande() == null) return 0;
            return b.getDateCommande().compareTo(a.getDateCommande());
        });

        List<Map<String, Object>> resultats = new ArrayList<>();
        int limit = Math.min(commandes.size(), 10);
        for (int i = 0; i < limit; i++) {
            CommandeClient cmd = commandes.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numeroCommande", cmd.getNumeroCommande());
            item.put("clientNom", cmd.getClientNom() != null ? cmd.getClientNom()
                    : (cmd.getClient() != null ? cmd.getClient().getNomComplet() : "Client"));
            item.put("dateCommande", cmd.getDateCommande());
            item.put("statut", cmd.getStatut() != null ? cmd.getStatut().name() : "-");
            item.put("montantTTC", cmd.getMontantTTC());
            resultats.add(item);
        }
        return resultats;
    }
}
