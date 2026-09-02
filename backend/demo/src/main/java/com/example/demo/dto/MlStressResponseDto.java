package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MlStressResponseDto {
    private String idequipement;
    private Double proba;
    private Integer isStress;
    private String alertLevel;   // CRITIQUE, HAUTE, STRESS, SUSPECT, NORMAL, INIT
    private Double ramPct;
    private Double swapPct;
    private String message;
    private String timestamp;
}
