package com.gestion.mapper;

import com.gestion.persistent.dto.LigneLivraisonDTO;
import com.gestion.persistent.dto.LivraisonDTO;
import com.gestion.persistent.model.LigneLivraison;
import com.gestion.persistent.model.Livraison;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {LigneLivraisonMapper.class, DepotMapper.class})
public interface LivraisonMapper {

    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "commandeNumero", source = "commande.numeroCommande")
    @Mapping(target = "depot", expression = "java(livraison.getLignesLivraison() != null && !livraison.getLignesLivraison().isEmpty() && livraison.getLignesLivraison().get(0).getDepot() != null ? depotMapper.toDto(livraison.getLignesLivraison().get(0).getDepot()) : null)")
    @Mapping(target = "depotId", expression = "java(livraison.getLignesLivraison() != null && !livraison.getLignesLivraison().isEmpty() && livraison.getLignesLivraison().get(0).getDepot() != null ? livraison.getLignesLivraison().get(0).getDepot().getId() : null)")
    @Mapping(target = "depotNom", expression = "java(livraison.getLignesLivraison() != null && !livraison.getLignesLivraison().isEmpty() && livraison.getLignesLivraison().get(0).getDepot() != null ? livraison.getLignesLivraison().get(0).getDepot().getNom() : null)")
    LivraisonDTO toDto(Livraison livraison);

    @Mapping(target = "commande.id", source = "commandeId")
    @Mapping(target = "dateLivraison", ignore = true)
    Livraison toEntity(LivraisonDTO livraisonDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numeroLivraison", ignore = true)
    @Mapping(target = "commande", ignore = true)
    @Mapping(target = "dateLivraison", ignore = true)
    void updateEntityFromDto(LivraisonDTO livraisonDTO, @MappingTarget Livraison livraison);
}

