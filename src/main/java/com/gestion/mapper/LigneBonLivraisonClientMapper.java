package com.gestion.mapper;

import com.gestion.persistent.dto.LigneBonLivraisonClientDTO;
import com.gestion.persistent.model.LigneBonLivraisonClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneBonLivraisonClientMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitReference", source = "produit.reference")
    @Mapping(target = "produitDesignation", source = "produit.nom")
    @Mapping(target = "depotId", source = "depot.id")
    @Mapping(target = "depotNom", source = "depot.nom")
    @Mapping(target = "lotId", source = "lot.id")
    @Mapping(target = "numeroLot", source = "lot.numeroLot")
    LigneBonLivraisonClientDTO toDto(LigneBonLivraisonClient entity);

    @Mapping(target = "bonLivraisonClient", ignore = true)
    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "depot", ignore = true)
    @Mapping(target = "lot", ignore = true)
    LigneBonLivraisonClient toEntity(LigneBonLivraisonClientDTO dto);
}
