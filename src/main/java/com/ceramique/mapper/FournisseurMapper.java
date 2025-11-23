package com.ceramique.mapper;

import com.ceramique.persistent.dto.FournisseurDTO;
import com.ceramique.persistent.model.Fournisseur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FournisseurMapper {

    FournisseurDTO toDto(Fournisseur fournisseur);

    @Mapping(target = "dateCreation", ignore = true)
    Fournisseur toEntity(FournisseurDTO fournisseurDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    void updateEntityFromDto(FournisseurDTO fournisseurDTO, @MappingTarget Fournisseur fournisseur);
}