package com.gestion.repository;

import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.model.JournalComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalComptableRepository extends JpaRepository<JournalComptable, Long> {

    List<JournalComptable> findByPointDeVenteIdOrderByCodeAsc(Long pointDeVenteId);

    Optional<JournalComptable> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);

    Optional<JournalComptable> findByTypeJournalAndPointDeVenteId(TypeJournal typeJournal, Long pointDeVenteId);
}
