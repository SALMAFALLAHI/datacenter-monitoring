package com.example.demo.entity;

public enum AnomalieType {
    RAM("Memoire RAM"),
    CPU("Processeur"),
    DISK("Disque dur"),
    NETWORK("Reseau");

    private final String label;

    AnomalieType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}