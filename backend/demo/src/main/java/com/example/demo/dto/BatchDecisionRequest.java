package com.example.demo.dto;

import com.example.demo.entity.StatutAnomalie;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDecisionRequest {

    /** Liste des IDs d'anomalies a traiter */
    private List<Long> idsAnomalies;

    /** Nouveau statut a appliquer */
    private StatutAnomalie nouveauStatut;

    /** Commentaire obligatoire expliquant la decision */
    private String commentaire;
}
