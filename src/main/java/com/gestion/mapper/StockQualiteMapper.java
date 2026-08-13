package com.gestion.mapper;

import com.gestion.persistent.dto.StockQualiteDTO;
import com.gestion.persistent.model.StockQualite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StockQualiteMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitDescription", source = "produit.description")
    @Mapping(target = "produitNom", source = "produit.designation")
    StockQualiteDTO toDto(StockQualite stockQualite);

    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "derniereMaj", ignore = true)
    StockQualite toEntity(StockQualiteDTO stockQualiteDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "derniereMaj", ignore = true)
    void updateEntityFromDto(StockQualiteDTO stockQualiteDTO, @MappingTarget StockQualite stockQualite);
}
