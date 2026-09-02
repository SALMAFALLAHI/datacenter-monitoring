package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
public class Alerte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long idAlerte;
    @Column(nullable = false)
    private String message;
    @Column(nullable = false)
    private String criticite;
    @Column(nullable = false)
    private String etat;
    @Column(nullable = false)
    private LocalDateTime dateCreation;


     @ManyToOne
    @JoinColumn(name = "idEquipement")
    private Equipement equipement;

    @OneToOne
    @JoinColumn(name = "anomalie_idaz", unique = true)
    private Anomalie anomalie;





}