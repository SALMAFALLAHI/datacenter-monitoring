package com.example.demo.dto;

import com.example.demo.entity.StatutAnomalie;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalieDecisionRequest {

    private StatutAnomalie nouveauStatut;

    /** Commentaire obligatoire expliquant la decision */
    private String commentaire;
}
