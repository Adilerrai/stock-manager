package com.gestion.mapper;

import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.model.CommandeClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneCommandeClientMapper.class})
public interface CommandeClientMapper {

    @Mapping(target = "lignesCommande", source = "lignesCommande")
    CommandeClientDTO toDto(CommandeClient commandeClient);

    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "lignesCommande", ignore = true)
    CommandeClient toEntity(CommandeClientDTO commandeClientDTO);
}
