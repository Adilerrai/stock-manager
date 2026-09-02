package com.gestion.mapper;

import com.gestion.persistent.dto.VenteDTO;
import com.gestion.persistent.model.Vente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneVenteMapper.class})
public interface VenteMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", expression = "java(entity.getClient() != null ? entity.getClient().getNomComplet() : null)")
    @Mapping(target = "clientTelephone", source = "client.telephone")
    @Mapping(target = "vendeurId", source = "vendeur.id")
    @Mapping(target = "vendeurNom", expression = "java(entity.getVendeur() != null ? (entity.getVendeur().getNomComplet() != null ? entity.getVendeur().getNomComplet() : entity.getVendeur().getUsername()) : null)")
    @Mapping(target = "lignes", source = "lignes")
    VenteDTO toDto(Vente entity);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "vendeur", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    @Mapping(target = "paiements", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "annulePar", ignore = true)
    Vente toEntity(VenteDTO dto);
}
