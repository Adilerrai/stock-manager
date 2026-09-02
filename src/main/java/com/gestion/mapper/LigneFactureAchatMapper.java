package com.gestion.mapper;

import com.gestion.persistent.dto.LigneFactureAchatDTO;
import com.gestion.persistent.model.LigneFactureAchat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneFactureAchatMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitNom", source = "produit.nom")
    @Mapping(target = "produitReference", source = "produit.reference")
    LigneFactureAchatDTO toDto(LigneFactureAchat entity);

    @Mapping(target = "factureAchat", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneFactureAchat toEntity(LigneFactureAchatDTO dto);
}
