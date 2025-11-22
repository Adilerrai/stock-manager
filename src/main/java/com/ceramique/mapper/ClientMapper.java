package com.ceramique.mapper;

import com.ceramique.persistent.dto.ClientDTO;
import com.ceramique.persistent.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "pointDeVenteId", source = "pointDeVente.id")
    @Mapping(target = "creditDisponible", expression = "java(client.getCreditDisponible())")
    ClientDTO toDto(Client client);

    @Mapping(target = "pointDeVente", ignore = true)
    @Mapping(target = "ventes", ignore = true)
    @Mapping(target = "factures", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateDerniereVisite", ignore = true)
    Client toEntity(ClientDTO dto);

    @Mapping(target = "pointDeVente", ignore = true)
    @Mapping(target = "ventes", ignore = true)
    @Mapping(target = "factures", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateDerniereVisite", ignore = true)
    void updateEntityFromDto(ClientDTO dto, @MappingTarget Client client);
}

