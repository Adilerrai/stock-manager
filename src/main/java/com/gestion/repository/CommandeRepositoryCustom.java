package com.gestion.repository;

import com.gestion.persistent.dto.CommandeSearchCriteria;
import com.gestion.persistent.model.Commande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CommandeRepositoryCustom {
    List<Commande> findByCriteria(CommandeSearchCriteria criteria);
    Page<Commande> findByCriteria(CommandeSearchCriteria criteria, Pageable pageable);
}
