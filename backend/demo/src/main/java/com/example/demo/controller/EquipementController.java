package com.example.demo.controller;

import com.example.demo.dto.EquipementRequestDTO;
import com.example.demo.dto.EquipementResponseDTO;
import com.example.demo.dto.ScanPlageRequestDTO;
import com.example.demo.dto.ScanPlageResultDTO;
import com.example.demo.entity.AnalyseReseau;
import com.example.demo.services.EquipementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // À restreindre en production !
public class EquipementController {

    private final EquipementService equipementService;

    @GetMapping
    public ResponseEntity<List<EquipementResponseDTO>> listerTous() {
        return ResponseEntity.ok(equipementService.listerTous());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipementResponseDTO> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(equipementService.trouverParId(id));
    }

    @GetMapping("/centre/{idCentre}")
    public ResponseEntity<List<EquipementResponseDTO>> listerParCentre(@PathVariable Long idCentre) {
        return ResponseEntity.ok(equipementService.listerParCentre(idCentre));
    }

    @PostMapping
    public ResponseEntity<EquipementResponseDTO> ajouter(@RequestBody EquipementRequestDTO dto) {
        return ResponseEntity.ok(equipementService.ajouterEquipement(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipementResponseDTO> modifier(@PathVariable Long id, @RequestBody EquipementRequestDTO dto) {
        return ResponseEntity.ok(equipementService.modifier(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        equipementService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analyser")
    public ResponseEntity<AnalyseReseau> analyser(@RequestParam String adresseIP) {
        return ResponseEntity.ok(equipementService.analyserEquipement(adresseIP));
    }

    @PostMapping("/scanner-plage")
    public ResponseEntity<List<ScanPlageResultDTO>> scannerPlage(@RequestBody ScanPlageRequestDTO dto) {
        return ResponseEntity.ok(equipementService.scannerPlage(dto));
    }
}