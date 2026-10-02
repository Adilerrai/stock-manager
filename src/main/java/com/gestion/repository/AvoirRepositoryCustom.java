package com.gestion.repository;

import com.gestion.persistent.dto.AvoirSearchCriteria;
import com.gestion.persistent.model.Avoir;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AvoirRepositoryCustom {
    Page<Avoir> findByCriteria(AvoirSearchCriteria criteria, Pageable pageable);
}
