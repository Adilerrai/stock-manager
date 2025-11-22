package com.ceramique.mapper;

import com.ceramique.persistent.dto.LigneCommandeClientDTO;
import com.ceramique.persistent.model.LigneCommandeClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneCommandeClientMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitReference", source = "produit.reference")
    LigneCommandeClientDTO toDto(LigneCommandeClient ligneCommandeClient);

    @Mapping(target = "commandeClient", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneCommandeClient toEntity(LigneCommandeClientDTO ligneCommandeClientDTO);
}