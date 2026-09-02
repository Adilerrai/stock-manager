package com.gestion.mapper;

import com.gestion.persistent.dto.CategorieDTO;
import com.gestion.persistent.model.Categorie;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategorieMapper {

    CategorieDTO toDto(Categorie categorie);

    Categorie toEntity(CategorieDTO categorieDTO);
}
