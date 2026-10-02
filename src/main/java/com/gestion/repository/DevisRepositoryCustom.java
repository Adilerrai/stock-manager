package com.gestion.repository;

import com.gestion.persistent.dto.DevisSearchCriteria;
import com.gestion.persistent.model.Devis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DevisRepositoryCustom {
    Page<Devis> findByCriteria(DevisSearchCriteria criteria, Pageable pageable);
}
