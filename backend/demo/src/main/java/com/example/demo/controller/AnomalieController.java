package com.example.demo.controller;

import com.example.demo.dto.AnomalieDecisionRequest;
import com.example.demo.dto.AnomalieGroupeDTO;
import com.example.demo.dto.AnomalieResponseDTO;
import com.example.demo.dto.BatchDecisionRequest;
import com.example.demo.entity.StatutAnomalie;
import com.example.demo.services.AnomalieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalieController {

    private final AnomalieService anomalieService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR', 'ROLE_OBSERVATEUR')")
    public ResponseEntity<List<AnomalieGroupeDTO>> getAnomalies(
            @RequestParam Long centreId,
            @RequestParam(required = false) StatutAnomalie statut,
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(anomalieService.getAnomaliesGroupes(centreId, statut, user.getUsername()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR', 'ROLE_OBSERVATEUR')")
    public ResponseEntity<Map<String, Long>> getStats(
            @RequestParam Long centreId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(anomalieService.getStatsParStatut(centreId, user.getUsername()));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR')")
    public ResponseEntity<Void> prendreDecision(
            @PathVariable Long id,
            @RequestBody AnomalieDecisionRequest request,
            @AuthenticationPrincipal UserDetails user) {

        anomalieService.prendreDecision(id, request, user.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR')")
    public ResponseEntity<Void> traiterBatch(
            @RequestBody BatchDecisionRequest request,
            @AuthenticationPrincipal UserDetails user) {

        anomalieService.traiterBatch(request, user.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/non-traitees")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR', 'ROLE_OBSERVATEUR')")
    public ResponseEntity<List<AnomalieResponseDTO>> listerNonTraitees(
            @RequestParam(required = false) Long centreId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(anomalieService.listerAnomaliesNonTraitees(centreId, user.getUsername()));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR')")
    public ResponseEntity<AnomalieResponseDTO> changerStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {

        StatutAnomalie statut = StatutAnomalie.valueOf(body.get("statut"));
        String commentaire = body.getOrDefault("commentaire", null);
        return ResponseEntity.ok(anomalieService.changerStatut(id, statut, commentaire, user.getUsername()));
    }
}