package com.ceramique.mapper;

import com.ceramique.persistent.dto.StockDTO;
import com.ceramique.persistent.model.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitDescription", source = "produit.description")
    @Mapping(target = "stocksQualite", source = "stocksQualite")
    StockDTO toDto(Stock stock);

    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "stocksQualite", ignore = true)
    Stock toEntity(StockDTO stockDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntityFromDto(StockDTO stockDTO, @MappingTarget Stock stock);
}