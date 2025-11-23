package com.ceramique.mapper;

import com.ceramique.persistent.dto.CommandeDTO;
import com.ceramique.persistent.model.Commande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {LigneCommandeMapper.class})
public interface CommandeMapper {

    @Mapping(target = "fournisseurId", source = "fournisseur.id")
    @Mapping(target = "fournisseurNom", source = "fournisseur.raisonSociale")
    CommandeDTO toDto(Commande commande);

    @Mapping(target = "fournisseur", ignore = true)
    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "livraisons", ignore = true)
    Commande toEntity(CommandeDTO commandeDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numeroCommande", ignore = true)
    @Mapping(target = "fournisseur", ignore = true)
    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "livraisons", ignore = true)
    void updateEntityFromDto(CommandeDTO commandeDTO, @MappingTarget Commande commande);
}