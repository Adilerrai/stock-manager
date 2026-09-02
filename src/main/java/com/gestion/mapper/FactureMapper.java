package com.gestion.mapper;

import com.gestion.persistent.dto.FactureDTO;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {LigneFactureMapper.class})
public interface FactureMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", expression = "java(entity.getClient() != null ? entity.getClient().getNomComplet() : null)")
    @Mapping(target = "clientTelephone", source = "client.telephone")
    @Mapping(target = "venteId", source = "vente.id")
    @Mapping(target = "emiseParUserId", source = "emisePar.id")
    @Mapping(target = "emiseParNom", expression = "java(entity.getEmisePar() != null ? (entity.getEmisePar().getPrenom() + \" \" + entity.getEmisePar().getNom()) : null)")
    @Mapping(target = "lignes", source = "lignes")
    @Mapping(target = "bonLivraisonIds", expression = "java(mapBlIds(entity))")
    @Mapping(target = "bonLivraisonNumeros", expression = "java(mapBlNumeros(entity))")
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
}
