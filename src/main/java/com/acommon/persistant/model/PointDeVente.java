package com.acommon.persistant.model;


import jakarta.persistence.*;


import java.util.Set;

@Entity
@Table(name = "point_de_vente")
public class PointDeVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nomPointDeVente;

    @Column(nullable = false)
    private String password; // Doit être hashé en production

    @Column(nullable = false, unique = true, updatable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "pointDeVente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<User> users;

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomPointDeVente() {
        return nomPointDeVente;
    }

    public void setNomPointDeVente(String nomPointDeVente) {
        this.nomPointDeVente = nomPointDeVente;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTenantId() {
        return tenantId;
    }
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}