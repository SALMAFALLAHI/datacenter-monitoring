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
public class MetriqueSeriePointDTO {
    private LocalDateTime dateCollecte;
    private Double cpuMoyen;
    private Double ramPctMoyen;
    private Double reseauMoyen;
    private Double diskPctMoyen;
    private int nombreEquipements;
}