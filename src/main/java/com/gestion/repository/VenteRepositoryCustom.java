package com.gestion.repository;

import com.gestion.persistent.dto.VenteSearchCriteria;
import com.gestion.persistent.model.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VenteRepositoryCustom {
    Page<Vente> findByCriteria(VenteSearchCriteria criteria, Pageable pageable);
}
