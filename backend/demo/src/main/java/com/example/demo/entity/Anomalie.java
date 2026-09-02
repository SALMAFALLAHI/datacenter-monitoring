package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OrderBy;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;    
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;

 

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class Anomalie {
    @Id
    @GeneratedValue
    private Long idAnomalie;

    private float score;
    private String niveau;
    private String description;
    private LocalDateTime dateDetection;
     @Enumerated(EnumType.STRING)
    @Column(name = "type_anomalie", length = 20)
    private AnomalieType type = AnomalieType .RAM;


    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutAnomalie statut = StatutAnomalie.NON_TRAITEE;
    private LocalDateTime dateStatut;
    @Column(length = 500)
    private String commentaireTraitement;






    @ManyToOne
    @JoinColumn(name = "id_metrique")
    private Metrique metrique;

    @OneToMany(mappedBy = "anomalie", cascade = CascadeType.ALL)
    private List<Prediction> predictions = new ArrayList<>();

    @OneToOne(mappedBy = "anomalie", cascade = CascadeType.ALL)
    private Alerte alerte;


    @OneToMany(mappedBy = "anomalie", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("dateDecision DESC")
    
    private List<Decision> decisions = new ArrayList<>();

    
}
