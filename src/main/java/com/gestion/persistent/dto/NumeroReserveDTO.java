package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutNumeroReserve;
import com.gestion.persistent.enums.TypeDocumentCodification;
import java.time.LocalDateTime;

public class NumeroReserveDTO {
    private Long id;
    private Long pointDeVenteId;
    private TypeDocumentCodification typeDocument;
    private String numero;
    private Long valeurSequence;
    private StatutNumeroReserve statut;
    private String motifLiberation;
    private Long documentOrigineId;
    private String documentOrigineType;
    private LocalDateTime dateReservation;
    private LocalDateTime dateUtilisation;
    private Long documentUtilisationId;

    public NumeroReserveDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public TypeDocumentCodification getTypeDocument() { return typeDocument; }
    public void setTypeDocument(TypeDocumentCodification typeDocument) { this.typeDocument = typeDocument; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public Long getValeurSequence() { return valeurSequence; }
    public void setValeurSequence(Long valeurSequence) { this.valeurSequence = valeurSequence; }

    public StatutNumeroReserve getStatut() { return statut; }
    public void setStatut(StatutNumeroReserve statut) { this.statut = statut; }

    public String getMotifLiberation() { return motifLiberation; }
    public void setMotifLiberation(String motifLiberation) { this.motifLiberation = motifLiberation; }

    public Long getDocumentOrigineId() { return documentOrigineId; }
    public void setDocumentOrigineId(Long documentOrigineId) { this.documentOrigineId = documentOrigineId; }

    public String getDocumentOrigineType() { return documentOrigineType; }
    public void setDocumentOrigineType(String documentOrigineType) { this.documentOrigineType = documentOrigineType; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public LocalDateTime getDateUtilisation() { return dateUtilisation; }
    public void setDateUtilisation(LocalDateTime dateUtilisation) { this.dateUtilisation = dateUtilisation; }

    public Long getDocumentUtilisationId() { return documentUtilisationId; }
    public void setDocumentUtilisationId(Long documentUtilisationId) { this.documentUtilisationId = documentUtilisationId; }
}
