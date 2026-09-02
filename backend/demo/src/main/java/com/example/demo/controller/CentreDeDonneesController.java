package com.example.demo.controller;

import com.example.demo.dto.CentreDeDonneesRequestDTO;
import com.example.demo.dto.CentreDeDonneesResponseDTO;
import com.example.demo.services.CentreDeDonneesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centres")
@RequiredArgsConstructor
public class CentreDeDonneesController {

    private final CentreDeDonneesService centreService;

    /** CRÉER un centre — ADMIN uniquement */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CentreDeDonneesResponseDTO> creer(
            @Valid @RequestBody CentreDeDonneesRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centreService.creer(dto, authentication));
    }

    /** LISTER TOUS les centres — Admin (création user) + Opérateur (sélecteur) */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR', 'ROLE_OBSERVATEUR')")
    public ResponseEntity<List<CentreDeDonneesResponseDTO>> listerTous() {
        return ResponseEntity.ok(centreService.listerTous());
    }

    /** LISTER les centres de l'utilisateur CONNECTÉ — pour l'opérateur */
    @GetMapping("/mes-centres")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR', 'ROLE_OBSERVATEUR')")
    public ResponseEntity<List<CentreDeDonneesResponseDTO>> listerMesCentres(Authentication authentication) {
        return ResponseEntity.ok(centreService.listerPourUtilisateurConnecte(authentication));
    }

    /** VOIR un centre par ID — tous, mais le service vérifie l'accès */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATEUR', 'ROLE_OBSERVATEUR')")
    public ResponseEntity<CentreDeDonneesResponseDTO> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(centreService.trouverParId(id));
    }

    /** MODIFIER un centre — ADMIN uniquement */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CentreDeDonneesResponseDTO> modifier(
            @PathVariable Long id,
            @Valid @RequestBody CentreDeDonneesRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(centreService.modifier(id, dto, authentication));
    }

    /** SUPPRIMER un centre — ADMIN uniquement */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, Authentication authentication) {
        centreService.supprimer(id, authentication);
        return ResponseEntity.noContent().build();
    }
}