package com.ceramique.mapper;

import com.ceramique.persistent.dto.CommandeClientDTO;
import com.ceramique.persistent.model.CommandeClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneCommandeClientMapper.class})
public interface CommandeClientMapper {

    @Mapping(target = "lignesCommande", source = "lignesCommande")
    CommandeClientDTO toDto(CommandeClient commandeClient);

    @Mapping(target = "pointDeVente", ignore = true)
    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "lignesCommande", ignore = true)
    CommandeClient toEntity(CommandeClientDTO commandeClientDTO);
}