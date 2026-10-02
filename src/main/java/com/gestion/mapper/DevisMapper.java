package com.gestion.mapper;

import com.gestion.persistent.dto.DevisDTO;
import com.gestion.persistent.model.Devis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneDevisMapper.class})
public interface DevisMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", expression = "java(entity.getClient() != null ? (entity.getClient().getNomComplet() != null ? entity.getClient().getNomComplet() : entity.getClient().getNom()) : null)")
    @Mapping(target = "clientTelephone", source = "client.telephone")
    @Mapping(target = "creeParId", source = "creePar.id")
    @Mapping(target = "creeParNom", expression = "java(entity.getCreePar() != null ? (entity.getCreePar().getNomComplet() != null ? entity.getCreePar().getNomComplet() : entity.getCreePar().getUsername()) : null)")
    @Mapping(target = "lignes", source = "lignes")
    DevisDTO toDto(Devis entity);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "creePar", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    Devis toEntity(DevisDTO dto);
}
