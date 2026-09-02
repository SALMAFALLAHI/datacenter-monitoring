package com.example.demo.controller;

import com.example.demo.services.DecouverteReseauService;
import com.example.demo.services.DecouverteReseauService.ResultatScan;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decouverte")
@RequiredArgsConstructor
public class DecouverteController {

    private final DecouverteReseauService decouverteService;

    /**
     * Scanner sans ajouter (juste voir ce qui existe)
     */
    @GetMapping("/scanner")
    public ResponseEntity<ResultatScan> scanner(
            @RequestParam String debut,
            @RequestParam String fin) {

        ResultatScan resultat = decouverteService.scannerPlage(debut, fin);
        return ResponseEntity.ok(resultat);
    }

    /**
     * Scanner ET ajouter automatiquement
     */
    @PostMapping("/scanner-ajouter")
    public ResponseEntity<ResultatScan> scannerEtAjouter(
            @RequestParam String debut,
            @RequestParam String fin,
            @RequestParam Long idCentre) {

        ResultatScan resultat = decouverteService.scannerEtAjouter(debut, fin, idCentre);
        return ResponseEntity.ok(resultat);
    }

    /**
     * Tester une seule IP
     */
    @GetMapping("/tester/{ip}")
    public ResponseEntity<DecouverteReseauService.EquipementDetecte> testerIP(@PathVariable String ip) {
        var resultat = decouverteService.scannerPlage(ip, ip);
        if (resultat.getDetectes().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resultat.getDetectes().get(0));
    }
}