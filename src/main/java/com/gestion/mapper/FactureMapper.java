package com.gestion.mapper;

import com.gestion.persistent.dto.FactureDTO;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {LigneFactureMapper.class})
public interface FactureMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", expression = "java(entity.getClient() != null ? entity.getClient().getNomComplet() : null)")
    @Mapping(target = "clientTelephone", source = "client.telephone")
    @Mapping(target = "venteId", source = "vente.id")
    @Mapping(target = "emiseParUserId", source = "emisePar.id")
    @Mapping(target = "emiseParNom", expression = "java(entity.getEmisePar() != null ? (entity.getEmisePar().getNomComplet() != null ? entity.getEmisePar().getNomComplet() : entity.getEmisePar().getUsername()) : null)")
    @Mapping(target = "lignes", source = "lignes")
    @Mapping(target = "bonLivraisonIds", expression = "java(mapBlIds(entity))")
    @Mapping(target = "bonLivraisonNumeros", expression = "java(mapBlNumeros(entity))")
    @Mapping(target = "commandeClientNumerosExternes", expression = "java(mapCommandeClientNumerosExternes(entity))")
    @Mapping(target = "numeroCommandeExterne", expression = "java(mapNumeroCommandeExterne(entity))")
    @Mapping(target = "bcClient", expression = "java(mapNumeroCommandeExterne(entity))")
    FactureDTO toDto(Facture entity);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "vente", ignore = true)
    @Mapping(target = "emisePar", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    @Mapping(target = "bonsLivraison", ignore = true)
    @Mapping(target = "paiements", ignore = true)
    @Mapping(target = "annuleePar", ignore = true)
    Facture toEntity(FactureDTO dto);

    default List<Long> mapBlIds(Facture entity) {
        if (entity == null || entity.getBonsLivraison() == null) {
            return Collections.emptyList();
        }
        return entity.getBonsLivraison().stream()
                .map(BonLivraisonClient::getId)
                .collect(Collectors.toList());
    }

    default List<String> mapBlNumeros(Facture entity) {
        if (entity == null || entity.getBonsLivraison() == null) {
            return Collections.emptyList();
        }
        return entity.getBonsLivraison().stream()
                .map(BonLivraisonClient::getNumeroBl)
                .collect(Collectors.toList());
    }

    default List<String> mapCommandeClientNumerosExternes(Facture entity) {
        if (entity == null || entity.getBonsLivraison() == null) {
            return Collections.emptyList();
        }
        Set<String> set = new LinkedHashSet<>();
        for (BonLivraisonClient bl : entity.getBonsLivraison()) {
            if (bl.getCommandeClient() != null && bl.getCommandeClient().getNumeroCommandeExterne() != null
                    && !bl.getCommandeClient().getNumeroCommandeExterne().trim().isEmpty()) {
                set.add(bl.getCommandeClient().getNumeroCommandeExterne().trim());
            }
            if (bl.getCommandes() != null) {
                for (CommandeClient c : bl.getCommandes()) {
                    if (c != null && c.getNumeroCommandeExterne() != null && !c.getNumeroCommandeExterne().trim().isEmpty()) {
                        set.add(c.getNumeroCommandeExterne().trim());
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    default String mapNumeroCommandeExterne(Facture entity) {
        List<String> list = mapCommandeClientNumerosExternes(entity);
        return list.isEmpty() ? null : String.join(", ", list);
    }
}
