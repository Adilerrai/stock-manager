package com.ceramique.mapper;

import com.ceramique.persistent.dto.LigneCommandeDTO;
import com.ceramique.persistent.model.LigneCommande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LigneCommandeMapper {

    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitDescription", source = "produit.description")
    @Mapping(target = "produitReference", source = "produit.reference")
    @Mapping(target = "qualiteProduit", source = "qualiteProduit")
    LigneCommandeDTO toDto(LigneCommande ligneCommande);

    @Mapping(target = "commande", ignore = true)
    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "qualiteProduit", source = "qualiteProduit")
    LigneCommande toEntity(LigneCommandeDTO ligneCommandeDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commande", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntityFromDto(LigneCommandeDTO ligneCommandeDTO, @MappingTarget LigneCommande ligneCommande);
}