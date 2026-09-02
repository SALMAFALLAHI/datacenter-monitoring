package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import com.example.demo.entity.SystemeExploitation;
import com.example.demo.entity.TypeEquipement;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipementResponseDTO {
    private Long idEquipement;
    private String nom;
    private TypeEquipement type;
    private SystemeExploitation systeme;
    private String etat;
    private String adresseIP;
    private LocalDate dateAjout;
    private Long idCentre;
    private String nomCentre;
    private Float ramTotaleGb;
}