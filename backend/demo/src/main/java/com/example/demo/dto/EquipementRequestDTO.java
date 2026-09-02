package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipementRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    // ✅ Accepte vide pour auto-détection
    @Pattern(
        regexp = "^(?i)(serveur|routeur|switch|firewall|nas|imprimante|workstation|)$",
        message = "Type invalide. Valeurs : SERVEUR, ROUTEUR, SWITCH, FIREWALL, NAS, IMPRIMANTE, WORKSTATION ou vide"
    )
    private String type;

    // ✅ Accepte vide pour auto-détection
    @Pattern(
        regexp = "^(?i)(windows|linux|ubuntu|debian|centos|redhat|macos|ios|android|)$",
        message = "Système invalide. Valeurs : WINDOWS, LINUX, UBUNTU, DEBIAN, CENTOS, REDHAT, MACOS, IOS, ANDROID ou vide"
    )
    private String systeme;

    @NotBlank(message = "L'adresse IP est obligatoire")
    @Pattern(
        regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
        message = "Adresse IP invalide"
    )
    private String adresseIP;

    @NotNull(message = "L'id du centre est obligatoire")
    private Long idCentre;

    private Float ramTotaleGb = 4.0f;  // RAM totale en GB, defaut 4GB
}