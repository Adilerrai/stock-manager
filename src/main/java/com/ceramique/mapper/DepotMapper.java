package com.ceramique.mapper;

import com.ceramique.persistent.dto.DepotDTO;
import com.ceramique.persistent.model.Depot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepotMapper {

    @Mapping(target = "pointDeVenteId", source = "pointDeVente.id")
    DepotDTO toDto(Depot depot);

    @Mapping(target = "pointDeVente", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    Depot toEntity(DepotDTO depotDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pointDeVente", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    void updateEntityFromDto(DepotDTO depotDTO, @MappingTarget Depot depot);
}