package com.gestion.mapper;

import com.gestion.persistent.dto.FactureAchatDTO;
import com.gestion.persistent.model.FactureAchat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneFactureAchatMapper.class})
public interface FactureAchatMapper {

    @Mapping(target = "fournisseurId", source = "fournisseur.id")
    @Mapping(target = "fournisseurNom", source = "fournisseur.raisonSociale")
    @Mapping(target = "fournisseurTelephone", source = "fournisseur.telephone")
    @Mapping(target = "lignes", source = "lignes")
    FactureAchatDTO toDto(FactureAchat entity);

    @Mapping(target = "fournisseur", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    FactureAchat toEntity(FactureAchatDTO dto);
}
