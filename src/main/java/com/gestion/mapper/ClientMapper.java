package com.gestion.mapper;

import com.gestion.persistent.dto.ClientDTO;
import com.gestion.persistent.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "creditDisponible", expression = "java(client.getCreditDisponible())")
    @Mapping(target = "commercialId", source = "commercial.id")
    @Mapping(target = "commercialNom", expression = "java(client.getCommercial() != null ? (client.getCommercial().getNomComplet() != null ? client.getCommercial().getNomComplet() : client.getCommercial().getUsername()) : null)")
    ClientDTO toDto(Client client);

    @Mapping(target = "ventes", ignore = true)
    @Mapping(target = "factures", ignore = true)
    @Mapping(target = "commercial", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateDerniereVisite", ignore = true)
    Client toEntity(ClientDTO dto);

    @Mapping(target = "ventes", ignore = true)
    @Mapping(target = "factures", ignore = true)
    @Mapping(target = "commercial", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateDerniereVisite", ignore = true)
    void updateEntityFromDto(ClientDTO dto, @MappingTarget Client client);
}


