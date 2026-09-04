package com.gestion.repository;

import com.gestion.persistent.dto.PaiementSearchCriteria;
import com.gestion.persistent.model.Paiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaiementRepositoryCustom {
    Page<Paiement> findByCriteria(PaiementSearchCriteria criteria, Pageable pageable);
}
