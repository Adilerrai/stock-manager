package com.gestion.mapper;

import com.gestion.persistent.dto.DepotDTO;
import com.gestion.persistent.model.Depot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepotMapper {

    DepotDTO toDto(Depot depot);

    @Mapping(target = "dateCreation", ignore = true)
    Depot toEntity(DepotDTO depotDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    void updateEntityFromDto(DepotDTO depotDTO, @MappingTarget Depot depot);
}
