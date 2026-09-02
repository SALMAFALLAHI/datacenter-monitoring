package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnomalieResponseDTO {
    private Long idAnomalie;
    @JsonProperty("typeAnomalie")
    private String type;
    private String niveau;
    private Float score;
    private String description;
    private LocalDateTime dateDetection;
    private String statut;
    private LocalDateTime dateStatut;   
    private String commentaireTraitement;
    private Long idMetrique;
    private Long idEquipement;
    private String nomEquipement;
    private String adresseIP;
}
