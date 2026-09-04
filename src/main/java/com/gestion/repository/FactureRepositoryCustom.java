package com.gestion.repository;

import com.gestion.persistent.dto.FactureSearchCriteria;
import com.gestion.persistent.model.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FactureRepositoryCustom {
    Page<Facture> findByCriteria(FactureSearchCriteria criteria, Pageable pageable);
}
