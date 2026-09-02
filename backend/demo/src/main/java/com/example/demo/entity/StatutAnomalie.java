package com.example.demo.entity;

public enum StatutAnomalie {
    NON_TRAITEE,   // Détectée, personne ne s'en est occupé
    EN_COURS,      // Un technicien travaille dessus
    TRAITEE,       // Problème résolu / action corrective appliquée
    IGNOREE        // Faux positif ou anomalie connue acceptée
}