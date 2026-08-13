package com.gestion.mapper;

import com.gestion.persistent.dto.MouvementStockDTO;
import com.gestion.persistent.model.MouvementStock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MouvementStockMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitLibelle", source = "produit.designation")
    @Mapping(target = "produitReference", source = "produit.reference")
    @Mapping(target = "depotId", source = "depot.id")
    @Mapping(target = "depotNom", source = "depot.nom")
    MouvementStockDTO toDto(MouvementStock mouvementStock);

    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "depot", ignore = true)
    @Mapping(target = "lot", ignore = true)
    MouvementStock toEntity(MouvementStockDTO mouvementStockDTO);
}

