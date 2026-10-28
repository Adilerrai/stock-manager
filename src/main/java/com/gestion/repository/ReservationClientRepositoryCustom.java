package com.gestion.repository;

import com.gestion.persistent.dto.ReservationClientSearchCriteria;
import com.gestion.persistent.model.ReservationClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationClientRepositoryCustom {
    Page<ReservationClient> findByCriteria(ReservationClientSearchCriteria criteria, Pageable pageable);
}

