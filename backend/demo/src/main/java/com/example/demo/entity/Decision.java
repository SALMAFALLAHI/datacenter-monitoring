package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_decision")
    private Long idDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_anomalie", nullable = false)
    private Anomalie anomalie;

    @Column(name = "auteur", nullable = false)
    private String auteur;

    @Column(name = "date_decision", nullable = false)
    private LocalDateTime dateDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", nullable = false)
    private StatutAnomalie ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false)
    private StatutAnomalie nouveauStatut;

    @Column(name = "commentaire", length = 2000)
    private String commentaire;

    @PrePersist
    public void prePersist() {
        if (this.dateDecision == null) {
            this.dateDecision = LocalDateTime.now();
        }
    }
}
