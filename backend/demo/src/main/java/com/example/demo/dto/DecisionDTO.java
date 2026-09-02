package com.example.demo.dto;

import com.example.demo.entity.StatutAnomalie;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionDTO {
    private Long idDecision;
    private String auteur;
    private LocalDateTime dateDecision;
    private StatutAnomalie ancienStatut;
    private StatutAnomalie nouveauStatut;
    private String commentaire;
}
