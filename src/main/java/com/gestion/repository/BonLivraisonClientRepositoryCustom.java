package com.gestion.repository;

import com.gestion.persistent.dto.BonLivraisonClientSearchCriteria;
import com.gestion.persistent.model.BonLivraisonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BonLivraisonClientRepositoryCustom {
    Page<BonLivraisonClient> findByCriteria(BonLivraisonClientSearchCriteria criteria, Pageable pageable);
}
