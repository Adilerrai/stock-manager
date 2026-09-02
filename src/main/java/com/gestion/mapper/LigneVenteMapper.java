package com.gestion.mapper;

import com.gestion.persistent.dto.LigneVenteDTO;
import com.gestion.persistent.model.LigneVente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneVenteMapper {

    @Mapping(target = "produitId", source = "produit.id")
    LigneVenteDTO toDto(LigneVente entity);

    @Mapping(target = "vente", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneVente toEntity(LigneVenteDTO dto);
}
