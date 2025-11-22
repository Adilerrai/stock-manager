package com.ceramique.mapper;

import com.ceramique.persistent.dto.LigneLivraisonDTO;
import com.ceramique.persistent.dto.LivraisonDTO;
import com.ceramique.persistent.model.LigneLivraison;
import com.ceramique.persistent.model.Livraison;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {LigneLivraisonMapper.class})
public interface LivraisonMapper {

    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "commandeNumero", source = "commande.numeroCommande")
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
