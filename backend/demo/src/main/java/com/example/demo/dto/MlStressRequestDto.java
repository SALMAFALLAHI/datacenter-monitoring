package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MlStressRequestDto {
    private String idequipement;      // = adresseIP de l'equipement
    private Double ramPct;
    private Double usedGb;
    private Double availableGb;
    private Double swapPct;
}
