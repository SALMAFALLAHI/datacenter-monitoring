package com.example.demo.services;

import com.example.demo.dto.MlStressRequestDto;
import com.example.demo.dto.MlStressResponseDto;
import com.example.demo.entity.Anomalie;
import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import com.example.demo.repository.AnomalieRepository;
import com.example.demo.repository.EquipementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class MlStressDetectionService {

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate;
    private final AnomalieRepository anomalieRepository;
    private final EquipementRepository equipementRepository;
    private final EmailService emailService;   // ← AJOUTE

    public MlStressDetectionService(
            AnomalieRepository anomalieRepository,
            EquipementRepository equipementRepository,
            EmailService emailService) {        // ← AJOUTE
        this.restTemplate = new RestTemplate();
        this.anomalieRepository = anomalieRepository;
        this.equipementRepository = equipementRepository;
        this.emailService = emailService;       // ← AJOUTE
    }

    // ========== API PUBLIQUE ==========

    /** Ancienne signature (sans email) — gardee pour compatibilite */
    public MlStressResponseDto detectStress(Metrique metrique) {
        return detectStress(metrique, null);
    }

    /** Nouvelle signature (avec email de l'utilisateur connecte) */
    public MlStressResponseDto detectStress(Metrique metrique, String emailUtilisateurConnecte) {
        if (metrique == null || metrique.getEquipement() == null) {
            return null;
        }

        Equipement eq = metrique.getEquipement();
        String machineId = eq.getAdresseIP();

        Double ramPct = metrique.getRam() != null ? metrique.getRam().doubleValue() : 0.0;
        Double usedGb = metrique.getUsedGb() != null ? metrique.getUsedGb().doubleValue() : 0.0;
        Double availableGb = metrique.getAvailableGb() != null ? metrique.getAvailableGb().doubleValue() : 0.0;
        Double swapPct = metrique.getSwapPct() != null ? metrique.getSwapPct().doubleValue() : 0.0;

        if (usedGb == 0.0 && ramPct > 0 && eq.getRamTotaleGb() != null) {
            usedGb = ramPct / 100.0 * eq.getRamTotaleGb();
            availableGb = eq.getRamTotaleGb() - usedGb;
        }

        MlStressResponseDto result = detectStressInternal(machineId, ramPct, usedGb, availableGb, swapPct);

        if (result != null && (result.getIsStress() == 1 || "CRITIQUE".equals(result.getAlertLevel()))) {
            createAnomalie(metrique, result, emailUtilisateurConnecte);   // ← passe l'email
        }

        return result;
    }

    public MlStressResponseDto detectStress(Double ramPct, Double usedGb, Double availableGb, Double swapPct, String machineId) {
        return detectStressInternal(
                machineId != null && !machineId.isBlank() ? machineId : "manual",
                ramPct != null ? ramPct : 0.0,
                usedGb != null ? usedGb : 0.0,
                availableGb != null ? availableGb : 0.0,
                swapPct != null ? swapPct : 0.0
        );
    }

    public void resetMachineHistory(String adresseIP) {
        try {
            restTemplate.postForObject(mlServiceUrl + "/reset/" + adresseIP, null, String.class);
        } catch (Exception e) {
            // ignore
        }
    }

    // ========== METHODES PRIVEES ==========

    private MlStressResponseDto detectStressInternal(String machineId, Double ramPct,
                                                      Double usedGb, Double availableGb, Double swapPct) {
        MlStressRequestDto request = new MlStressRequestDto(machineId, ramPct, usedGb, availableGb, swapPct);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MlStressRequestDto> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<MlStressResponseDto> response = restTemplate.postForEntity(
                    mlServiceUrl + "/predict", entity, MlStressResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            return fallbackDetection(machineId, ramPct, swapPct);
        }
    }

    private void createAnomalie(Metrique metrique, MlStressResponseDto result, String emailUtilisateurConnecte) {
        Anomalie anomalie = new Anomalie();
        anomalie.setMetrique(metrique);
        anomalie.setScore(result.getProba().floatValue());
        anomalie.setNiveau(result.getAlertLevel());
        anomalie.setDescription(result.getMessage() + " | RAM=" + result.getRamPct() + "% | Swap=" + result.getSwapPct() + "%");
        anomalie.setDateDetection(LocalDateTime.now());

        Anomalie savedAnomalie = anomalieRepository.save(anomalie);

        // ===== ENVOI EMAIL =====
        emailService.envoyerAlerteStress(savedAnomalie, metrique, metrique.getEquipement(), emailUtilisateurConnecte);
    }

    private MlStressResponseDto fallbackDetection(String machineId, Double ramPct, Double swapPct) {
        String level = "NORMAL";
        int isStress = 0;
        String msg = "Fallback (ML indisponible)";
        double proba = 0.0;

        if (ramPct > 90 || swapPct > 60) {
            level = "CRITIQUE"; isStress = 1; proba = 1.0;
        } else if (ramPct > 80 || swapPct > 50) {
            level = "HAUTE"; isStress = 1; proba = 0.95;
        } else if (ramPct > 70) {
            level = "SUSPECT"; proba = 0.4;
        }

        return new MlStressResponseDto(
                machineId, proba, isStress, level, ramPct, swapPct, msg,
                LocalDateTime.now().toString()
        );
    }
}