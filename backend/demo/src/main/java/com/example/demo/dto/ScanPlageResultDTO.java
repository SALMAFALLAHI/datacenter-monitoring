package com.example.demo.dto;

import lombok.Data;

@Data
public class ScanPlageResultDTO {
    private String adresseIP;
    private boolean joignable;
    private String typeDetecte;
    private String systemeDetecte;
}