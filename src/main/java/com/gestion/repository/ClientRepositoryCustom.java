package com.gestion.repository;

import com.gestion.persistent.dto.ClientSearchCriteria;
import com.gestion.persistent.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientRepositoryCustom {
    Page<Client> findByCriteria(ClientSearchCriteria criteria, Pageable pageable);
}
