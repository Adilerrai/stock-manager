package com.gestion.persistent.dto;

import com.gestion.persistent.enums.Incoterm;
import com.gestion.persistent.enums.TypeAchat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SimulationCoutRevientRequest {

    private TypeAchat typeAchat = TypeAchat.INTERNATIONAL;
    private Incoterm incoterm = Incoterm.FOB;
    private String devise = "USD";
    private BigDecimal tauxChange = BigDecimal.TEN;
    private List<LigneCommandeDTO> lignes = new ArrayList<>();
    private List<ChargeAchatDTO> charges = new ArrayList<>();

    public SimulationCoutRevientRequest() {}

    public TypeAchat getTypeAchat() { return typeAchat; }
    public void setTypeAchat(TypeAchat typeAchat) { this.typeAchat = typeAchat; }

    public Incoterm getIncoterm() { return incoterm; }
    public void setIncoterm(Incoterm incoterm) { this.incoterm = incoterm; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public BigDecimal getTauxChange() { return tauxChange; }
    public void setTauxChange(BigDecimal tauxChange) { this.tauxChange = tauxChange; }

    public List<LigneCommandeDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneCommandeDTO> lignes) { this.lignes = lignes; }

    public List<ChargeAchatDTO> getCharges() { return charges; }
    public void setCharges(List<ChargeAchatDTO> charges) { this.charges = charges; }
}
