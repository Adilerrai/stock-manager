package com.gestion.repository;

import com.gestion.persistent.dto.ChequeEffetSearchCriteria;
import com.gestion.persistent.model.ChequeEffet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChequeEffetRepositoryCustom {
    Page<ChequeEffet> findByCriteria(ChequeEffetSearchCriteria criteria, Pageable pageable);
}
