package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetriqueResponseDTO {
    private Long idMetrique;
    private Float cpu;
    private Float ram;
    private Double ramPct;
    private Double usedGb;
    private Double availableGb;
    private Double swapPct;
    private Float disque;
    private Float temperature;
    private Float reseau;
    private LocalDateTime dateCollecte;
    private Long idEquipement;
    private String nomEquipement;
    private String adresseIP;
    private String source;
}