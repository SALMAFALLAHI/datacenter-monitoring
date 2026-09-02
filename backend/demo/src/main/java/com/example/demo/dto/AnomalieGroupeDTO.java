package com.example.demo.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalieGroupeDTO {

    /** Identifiant unique du groupe : idEquipement|type|niveau */
    private String idGroupe;

    private Long idEquipement;
    private String nomEquipement;
    private String typeAnomalie;
    private String niveau;

    /** Score le plus eleve parmi les anomalies du groupe */
    private Double scoreMax;

    /** Date de la derniere detection dans le groupe */
    private LocalDateTime dateDerniereDetection;

    /** Nombre d'anomalies identiques dans ce groupe */
    private int nombreOccurrences;

    /** Tous les IDs d'anomalies pour traitement par lot */
    private List<Long> idsAnomalies;

    /** Statut du groupe : NON_TRAITEE, EN_COURS, TRAITEE, IGNOREE, ou MIXTE */
    private String statut;

    /** Historique des decisions (de l'anomalie la plus recente du groupe) */
    private List<DecisionDTO> decisions;

    /** ID du centre de donnees */
    private Long idCentre;
}
