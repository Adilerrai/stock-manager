package com.gestion.mapper;

import com.gestion.persistent.dto.EntrepriseProfileDTO;
import com.gestion.persistent.model.EntrepriseProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EntrepriseProfileMapper {

    @Mapping(target = "hasLogo", expression = "java(entity.hasLogo())")
    EntrepriseProfileDTO toDto(EntrepriseProfile entity);

    @Mapping(target = "logoData", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    @Mapping(target = "logoFileName", ignore = true)
    EntrepriseProfile toEntity(EntrepriseProfileDTO dto);
}
