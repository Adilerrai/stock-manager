package com.gestion.persistent.model;

import com.gestion.persistent.enums.StatutNumeroReserve;
import com.gestion.persistent.enums.TypeDocumentCodification;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "numeros_reserves", indexes = {
    @Index(name = "idx_num_res_tenant_type_statut", columnList = "point_de_vente_id, type_document, statut"),
    @Index(name = "idx_num_res_numero", columnList = "numero")
})
public class NumeroReserve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 50)
    private TypeDocumentCodification typeDocument;

    @Column(name = "numero", nullable = false, length = 100)
    private String numero;

    @Column(name = "valeur_sequence")
    private Long valeurSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutNumeroReserve statut = StatutNumeroReserve.DISPONIBLE;

    @Column(name = "motif_liberation", length = 255)
    private String motifLiberation;

    @Column(name = "document_origine_id")
    private Long documentOrigineId;

    @Column(name = "document_origine_type", length = 50)
    private String documentOrigineType;

    @Column(name = "date_reservation", nullable = false)
    private LocalDateTime dateReservation = LocalDateTime.now();

    @Column(name = "date_utilisation")
    private LocalDateTime dateUtilisation;

    @Column(name = "document_utilisation_id")
    private Long documentUtilisationId;

    public NumeroReserve() {}

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
