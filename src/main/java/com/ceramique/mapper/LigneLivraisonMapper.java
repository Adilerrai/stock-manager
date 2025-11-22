package com.ceramique.mapper;

import com.ceramique.persistent.dto.LigneLivraisonDTO;
import com.ceramique.persistent.model.LigneLivraison;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DepotMapper.class, ProduitMapper.class})
public interface LigneLivraisonMapper {


    LigneLivraisonDTO toDto(LigneLivraison ligneLivraison);


    LigneLivraison toEntity(LigneLivraisonDTO ligneLivraisonDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "livraison", ignore = true)
    void updateEntityFromDto(LigneLivraisonDTO ligneLivraisonDTO, @MappingTarget LigneLivraison ligneLivraison);
}