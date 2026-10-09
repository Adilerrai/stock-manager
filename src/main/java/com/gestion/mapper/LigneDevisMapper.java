package com.gestion.mapper;

import com.gestion.persistent.dto.LigneDevisDTO;
import com.gestion.persistent.model.LigneDevis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneDevisMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitReference", source = "produit.reference")
    @Mapping(target = "produitDesignation", expression = "java(entity.getProduit() != null ? (entity.getProduit().getDesignation() != null ? entity.getProduit().getDesignation() : entity.getProduit().getNom()) : entity.getDescription())")
    @Mapping(target = "produit", ignore = true)
    LigneDevisDTO toDto(LigneDevis entity);

    @Mapping(target = "devis", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneDevis toEntity(LigneDevisDTO dto);
}
