package com.gestion.mapper;

import com.gestion.persistent.dto.LigneFactureDTO;
import com.gestion.persistent.model.LigneFacture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneFactureMapper {

    @Mapping(target = "produitId", source = "produit.id")
    LigneFactureDTO toDto(LigneFacture entity);

    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneFacture toEntity(LigneFactureDTO dto);
}
