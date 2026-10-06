package com.gestion.mapper;

import com.gestion.persistent.dto.StockDTO;
import com.gestion.persistent.model.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitNom", source = "produit.designation")
    @Mapping(target = "produitReference", source = "produit.reference")
    @Mapping(target = "produitCodeBarre", source = "produit.codeBarre")
    @Mapping(target = "produitDescription", source = "produit.description")
    @Mapping(target = "categorieId", source = "produit.categorie.id")
    @Mapping(target = "categorieNom", source = "produit.categorie.nom")
    @Mapping(target = "prixVente", source = "produit.prixVenteHt")
    @Mapping(target = "prixVenteTtc", source = "produit.prixVenteTtc")
    @Mapping(target = "prixAchatHt", source = "produit.prixAchatHt")
    @Mapping(target = "uniteMesure", source = "produit.uniteMesureStock")
    @Mapping(target = "statutStock", ignore = true)
    @Mapping(target = "stocksQualite", source = "stocksQualite")
    StockDTO toDto(Stock stock);

    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "stocksQualite", ignore = true)
    Stock toEntity(StockDTO stockDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntityFromDto(StockDTO stockDTO, @MappingTarget Stock stock);
}
