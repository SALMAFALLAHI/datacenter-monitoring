package com.example.demo.controller;

import com.example.demo.dto.MetriqueResponseDTO;
import com.example.demo.dto.MetriqueSeriePointDTO;
import com.example.demo.dto.MetriqueStatutResponseDTO;
import com.example.demo.entity.Metrique;
import com.example.demo.services.MetriqueService;
import com.example.demo.services.MlStressDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metriques")
@RequiredArgsConstructor
public class MetriqueController {

    private final MetriqueService metriqueService;
    private final MlStressDetectionService mlStressDetectionService;

    @GetMapping("/equipement/{idEquipement}")
    public ResponseEntity<List<MetriqueResponseDTO>> listerParEquipement(@PathVariable Long idEquipement) {
        return ResponseEntity.ok(metriqueService.listerParEquipement(idEquipement));
    }

    @GetMapping("/equipement/{idEquipement}/latest")
    public ResponseEntity<MetriqueResponseDTO> trouverDerniereParEquipement(@PathVariable Long idEquipement) {
        return ResponseEntity.ok(metriqueService.trouverDerniereParEquipement(idEquipement));
    }

    @GetMapping("/equipement/{idEquipement}/with-status")
    public ResponseEntity<List<MetriqueStatutResponseDTO>> listerParEquipementAvecStatut(@PathVariable Long idEquipement) {
        return ResponseEntity.ok(metriqueService.listerParEquipementAvecStatut(idEquipement));
    }

    @GetMapping("/equipement/{idEquipement}/latest-with-status")
    public ResponseEntity<MetriqueStatutResponseDTO> trouverDerniereParEquipementAvecStatut(@PathVariable Long idEquipement) {
        return ResponseEntity.ok(metriqueService.trouverDerniereParEquipementAvecStatut(idEquipement));
    }

    
    @PostMapping
    public ResponseEntity<Metrique> collecterMetrique(@RequestBody Metrique metrique) {
        Metrique saved = metriqueService.saveMetrique(metrique);

        // Appel au modele ML pour detection de stress RAM
        mlStressDetectionService.detectStress(saved);

        return ResponseEntity.ok(saved);
    }

   

    

    @GetMapping
    public ResponseEntity<List<MetriqueResponseDTO>> getMetriques(
            @RequestParam(required = false) Long centreId) {
                System.out.println(">>> centreId reçu = " + centreId);
        
        if (centreId != null) {
            return ResponseEntity.ok(metriqueService.findByCentre(centreId));
        }
        return ResponseEntity.ok(metriqueService.findAllLatest());
    }

    @GetMapping("/historique")
public ResponseEntity<List<MetriqueSeriePointDTO>> getHistorique(
        @RequestParam(required = false) Long centreId,
        @RequestParam(defaultValue = "2880") int minutes) {
    
    // Utilisez System.out.println au lieu de log.info
    System.out.println(">>> CONTROLEUR getHistorique atteint - centreId=" + centreId 
        + ", minutes=" + minutes 
        + ", auth=" + (SecurityContextHolder.getContext().getAuthentication() != null 
            ? SecurityContextHolder.getContext().getAuthentication().getName() 
            : "NULL"));
    
    return ResponseEntity.ok(metriqueService.getHistorique(centreId, minutes));
}
}