package com.ceramique.persistent.model;


import jakarta.persistence.*;

@Entity
@Table(name = "unite_mesure_options")
public class UniteMesureOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String value;
    private String label;

    public UniteMesureOption() {
    }

    public UniteMesureOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}