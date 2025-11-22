package com.acommon.persistant.model;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // ex: "ROLE_AVOCAT", "ROLE_ASSISTANT"

    @ManyToMany(fetch = FetchType.EAGER) // EAGER pour charger les permissions avec le rôle
    @JoinTable(
            name = "roles_habilitations",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "habilitation_id")
    )
    private Set<Habilitation> habilitations;

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Set<Habilitation> getHabilitations() {
        return habilitations;
    }

    public void setHabilitations(Set<Habilitation> habilitations) {
        this.habilitations = habilitations;
    }
}