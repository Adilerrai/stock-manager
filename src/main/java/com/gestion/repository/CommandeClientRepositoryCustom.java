package com.gestion.repository;

import com.gestion.persistent.dto.CommandeClientSearchCriteria;
import com.gestion.persistent.model.CommandeClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommandeClientRepositoryCustom {
    Page<CommandeClient> findByCriteria(CommandeClientSearchCriteria criteria, Pageable pageable);
}
