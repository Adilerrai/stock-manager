package com.ceramique.repository;

import com.ceramique.persistent.dto.CommandeSearchCriteria;
import com.ceramique.persistent.model.Commande;
import java.util.List;

public interface CommandeRepositoryCustom {
    List<Commande> findByCriteria(CommandeSearchCriteria criteria, Long pointDeVenteId);
}