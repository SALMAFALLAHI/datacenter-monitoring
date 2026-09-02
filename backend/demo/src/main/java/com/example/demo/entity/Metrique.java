package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import java.util.List;
import jakarta.persistence.CascadeType;



@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Metrique {
    @Id
    @GeneratedValue
    private Long idMetrique;
    @Column(nullable = false)
    private Float cpu;
    private Float ram;
    private Double ramPct;
    private Double usedGb;
    private Double availableGb;
    private Double swapPct;
    private Float disque;
    private Float temperature;
    private Float reseau;
    @Column(nullable = false)
    private LocalDateTime dateCollecte;

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeInfrastructure source  = TypeInfrastructure.PHYSIQUE;

    @ManyToOne
    @JoinColumn(name = "idEquipement")
    private Equipement equipement;

    @OneToMany(mappedBy = "metrique", cascade = CascadeType.ALL)
    private List<Anomalie> anomalies = new ArrayList<>();

}
