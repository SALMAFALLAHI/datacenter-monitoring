package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import jakarta.persistence.CascadeType;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class CentreDeDonnees {
    @Id
    @GeneratedValue
    private Long idCentre;

    @Column(nullable = false)
    private String nom;
    @Column(nullable = false)
    private String localisation;
    private Double latitude;
    private Double longitude;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeInfrastructure type = TypeInfrastructure.PHYSIQUE;

    @Enumerated(EnumType.STRING)
    private Fournisseur fournisseur; 
    
    private String region;     
    
    @Column(name = "api_endpoint")
    private String apiEndpoint;       

    @ManyToOne
    @JoinColumn(name = "administrateur_id")
    private Administrateur administrateur;


    @OneToMany(mappedBy = "centreDeDonnees", cascade = CascadeType.ALL)
    private List<Equipement> equipements;
}
