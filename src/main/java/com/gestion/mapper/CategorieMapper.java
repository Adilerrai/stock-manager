package com.gestion.mapper;

import com.gestion.persistent.dto.CategorieDTO;
import com.gestion.persistent.model.Categorie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategorieMapper {

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.nom", target = "parentNom")
    CategorieDTO toDto(Categorie categorie);

    @Mapping(source = "parentId", target = "parent.id")
    Categorie toEntity(CategorieDTO categorieDTO);
}
