package com.example.demo.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyseReseau {
    private String adresseIP;
    private String systemeDetecte;
    private String typeDetecte;
    private boolean joignable;
}