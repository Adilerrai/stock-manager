package com.gestion.repository;

import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.CodificationDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodificationDocumentRepository extends JpaRepository<CodificationDocument, Long> {

    List<CodificationDocument> findByPointDeVenteIdOrderByTypeDocumentAsc(Long pointDeVenteId);

    Optional<CodificationDocument> findByPointDeVenteIdAndTypeDocument(Long pointDeVenteId, TypeDocumentCodification typeDocument);

    boolean existsByPointDeVenteIdAndTypeDocument(Long pointDeVenteId, TypeDocumentCodification typeDocument);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CodificationDocument c WHERE c.pointDeVenteId = :pointDeVenteId AND c.typeDocument = :typeDocument")
    Optional<CodificationDocument> findByPointDeVenteIdAndTypeDocumentForUpdate(
            @Param("pointDeVenteId") Long pointDeVenteId,
            @Param("typeDocument") TypeDocumentCodification typeDocument
    );
}
