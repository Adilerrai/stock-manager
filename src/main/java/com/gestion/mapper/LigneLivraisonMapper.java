package com.gestion.mapper;

import com.gestion.persistent.dto.LigneLivraisonDTO;
import com.gestion.persistent.model.LigneLivraison;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DepotMapper.class, ProduitMapper.class})
public interface LigneLivraisonMapper {


    @Mapping(target = "depotId", source = "depot.id")
    @Mapping(target = "depotNom", source = "depot.nom")
    LigneLivraisonDTO toDto(LigneLivraison ligneLivraison);


    LigneLivraison toEntity(LigneLivraisonDTO ligneLivraisonDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "livraison", ignore = true)
    void updateEntityFromDto(LigneLivraisonDTO ligneLivraisonDTO, @MappingTarget LigneLivraison ligneLivraison);
}
