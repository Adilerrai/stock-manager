package com.gestion.mapper;

import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.model.BonLivraisonClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneBonLivraisonClientMapper.class})
public interface BonLivraisonClientMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", expression = "java(entity.getClient() != null ? entity.getClient().getNomComplet() : null)")
    @Mapping(target = "clientTelephone", source = "client.telephone")
    @Mapping(target = "commandeClientId", source = "commandeClient.id")
    @Mapping(target = "commandeClientNumero", source = "commandeClient.numeroCommande")
    @Mapping(target = "factureId", source = "facture.id")
    @Mapping(target = "factureNumero", source = "facture.numeroFacture")
    @Mapping(target = "facturé", expression = "java(entity.getFacture() != null)")
    @Mapping(target = "lignes", source = "lignes")
    BonLivraisonClientDTO toDto(BonLivraisonClient entity);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "commandeClient", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    BonLivraisonClient toEntity(BonLivraisonClientDTO dto);
}
