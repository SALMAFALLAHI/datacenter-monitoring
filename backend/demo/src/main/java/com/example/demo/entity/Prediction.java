package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPrediction;
    @Column(nullable = false)
    private float probabilite;
    @Column(nullable = false)
    private String niveauRisque ;
    @Column(nullable = false)
    private LocalDateTime datePrediction;

    @ManyToOne
    @JoinColumn(name = "idAnomalie")
    private Anomalie anomalie;

}