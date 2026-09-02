package com.example.demo.controller;

import com.example.demo.dto.MlStressResponseDto;
import com.example.demo.entity.Metrique;
import com.example.demo.services.MlStressDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint pour la detection de stress RAM via IA.
 * Peut etre appele manuellement ou automatiquement apres chaque collecte de metrique.
 */
@RestController
@RequestMapping("/api/stress-detection")
public class StressDetectionController {

    private final MlStressDetectionService mlService;

    public StressDetectionController(MlStressDetectionService mlService) {
        this.mlService = mlService;
    }

    /**
     * POST /api/stress-detection/check
     * Test manuel avec les valeurs brutes.
     */
    @PostMapping("/check")
    public ResponseEntity<MlStressResponseDto> checkStress(@RequestBody StressCheckRequest request) {
        MlStressResponseDto result = mlService.detectStress(
                request.getRamPct(),
                request.getUsedGb(),
                request.getAvailableGb(),
                request.getSwapPct(),
                request.getMachineId()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/stress-detection/reset/{adresseIP}
     * Vide l'historique ML d'une machine.
     */
    @PostMapping("/reset/{adresseIP}")
    public ResponseEntity<String> resetHistory(@PathVariable String adresseIP) {
        mlService.resetMachineHistory(adresseIP);
        return ResponseEntity.ok("Historique ML reinitialise pour " + adresseIP);
    }

    // DTO interne
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StressCheckRequest {
        private String machineId;
        private Double ramPct;
        private Double usedGb;
        private Double availableGb;
        private Double swapPct;
    }
}
