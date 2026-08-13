package com.gestion.repository;

import com.gestion.persistent.dto.CommandeSearchCriteria;
import com.gestion.persistent.model.Commande;
import java.util.List;

public interface CommandeRepositoryCustom {
    List<Commande> findByCriteria(CommandeSearchCriteria criteria);
}
