package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CentreDeDonneesResponseDTO {
    private Long idCentre;
    private String nom;
    private String localisation;
    private Long idAdministrateur;
    private String nomAdministrateur;
    private int nombreEquipements;
    private Double latitude;
    private Double longitude;
}
