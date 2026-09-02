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

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Equipement {
    @Id
    @GeneratedValue
    private Long idEquipement;


    @Column(name = "cloud_instance_id")
    private String cloudInstanceId; 
    
    
    @Column(nullable = false)
    private String nom;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEquipement Type;
    @Column(nullable = false)
    private String etat;
    @Column(nullable = false)
    private LocalDate dateAjout;

    @Column(name = "ram_totale_gb")
    private Float ramTotaleGb = 4.0f;
    


    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", message = "Adresse IP invalide")
    private String adresseIP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SystemeExploitation Systeme;

    @ManyToOne
    @JoinColumn(name = "idCentre")
    private CentreDeDonnees centreDeDonnees;

    @OneToMany(mappedBy = "equipement", cascade = CascadeType.ALL)
    private List<Metrique> metriques;

    @OneToMany(mappedBy = "equipement", cascade = CascadeType.ALL)
    private List<Alerte> alertes = new ArrayList<>();

}
