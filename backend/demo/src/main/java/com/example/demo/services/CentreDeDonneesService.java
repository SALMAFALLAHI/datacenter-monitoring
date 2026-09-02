package com.example.demo.services;

import com.example.demo.dto.CentreDeDonneesRequestDTO;
import com.example.demo.dto.CentreDeDonneesResponseDTO;
import com.example.demo.entity.Administrateur;
import com.example.demo.entity.CentreDeDonnees;
import com.example.demo.repository.AdministrateurRepository;
import com.example.demo.repository.CentreDeDonneesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CentreDeDonneesService {

    private final CentreDeDonneesRepository centreRepository;
    private final AdministrateurRepository administrateurRepository;

    // ==================== MAPPING ====================

    private CentreDeDonneesResponseDTO toResponseDTO(CentreDeDonnees centre) {
        Long idAdmin = null;
        String nomAdmin = null;

        if (centre.getAdministrateur() != null) {
            idAdmin = centre.getAdministrateur().getIdAdmin();
            nomAdmin = centre.getAdministrateur().getNom();
        }

        int nombreEquipements = centre.getEquipements() == null ? 0 : centre.getEquipements().size();

        return new CentreDeDonneesResponseDTO(
                centre.getIdCentre(),
                centre.getNom(),
                centre.getLocalisation(),
                idAdmin,
                nomAdmin,
                nombreEquipements,
                centre.getLatitude(),
                centre.getLongitude()
        );
    }

    // ==================== UTILITAIRES ====================

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Utilisateur non authentifie");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String email) {
            return email;
        }

        throw new IllegalArgumentException("Impossible de determiner l'utilisateur connecte");
    }

    private Administrateur resolveAdministrateurConnecte(Authentication authentication) {
        String email = extractEmail(authentication);
        return administrateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Administrateur introuvable avec l'email : " + email));
    }

    private CentreDeDonnees findCentreOrThrow(Long id) {
        return centreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Centre de donnees introuvable avec l'id : " + id));
    }

    private void verifyOwnership(CentreDeDonnees centre, Administrateur adminConnecte) {
        if (centre.getAdministrateur() == null || centre.getAdministrateur().getIdAdmin() == null) {
            throw new AccessDeniedException("Ce centre n'a pas de proprietaire assigne");
        }

        if (!centre.getAdministrateur().getIdAdmin().equals(adminConnecte.getIdAdmin())) {
            throw new AccessDeniedException("Vous n'etes pas autorise a modifier ou supprimer ce centre");
        }
    }

    // ==================== CRUD ====================

    @Transactional
    public CentreDeDonneesResponseDTO creer(CentreDeDonneesRequestDTO dto, Authentication authentication) {
        Administrateur adminConnecte = resolveAdministrateurConnecte(authentication);

        CentreDeDonnees centre = new CentreDeDonnees();
        centre.setNom(dto.getNom().trim());
        centre.setLocalisation(dto.getLocalisation().trim());
        centre.setLatitude(dto.getLatitude());
        centre.setLongitude(dto.getLongitude());
        centre.setAdministrateur(adminConnecte);

        CentreDeDonnees saved = centreRepository.save(centre);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CentreDeDonneesResponseDTO> listerTous() {
        return centreRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CentreDeDonneesResponseDTO trouverParId(Long id) {
        CentreDeDonnees centre = findCentreOrThrow(id);
        return toResponseDTO(centre);
    }

    @Transactional
    public CentreDeDonneesResponseDTO modifier(Long id, CentreDeDonneesRequestDTO dto, Authentication authentication) {
        Administrateur adminConnecte = resolveAdministrateurConnecte(authentication);
        CentreDeDonnees centre = findCentreOrThrow(id);
        verifyOwnership(centre, adminConnecte);

        centre.setNom(dto.getNom().trim());
        centre.setLocalisation(dto.getLocalisation().trim());
        centre.setLatitude(dto.getLatitude());
        centre.setLongitude(dto.getLongitude());

        CentreDeDonnees saved = centreRepository.save(centre);
        return toResponseDTO(saved);
    }

    @Transactional
    public void supprimer(Long id, Authentication authentication) {
        Administrateur adminConnecte = resolveAdministrateurConnecte(authentication);
        CentreDeDonnees centre = findCentreOrThrow(id);
        verifyOwnership(centre, adminConnecte);
        centreRepository.delete(centre);
    }

    // ==================== LECTURE PAR UTILISATEUR ====================

    /**
     * Retourne les centres de l'utilisateur connecté.
     * Admin = tous les centres.
     * Opérateur/Observateur = seulement les centres qui lui sont assignés.
     */
    @Transactional(readOnly = true)
    public List<CentreDeDonneesResponseDTO> listerPourUtilisateurConnecte(Authentication authentication) {
        Administrateur user = resolveAdministrateurConnecte(authentication);

        if (user.isAdmin()) {
            return listerTous();  // Admin voit tous les centres
        }

        // Opérateur / Observateur : seulement les centres assignés
        return centreRepository.findByAdministrateur_IdAdmin(user.getIdAdmin()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * @deprecated Utilisez listerPourUtilisateurConnecte() à la place.
     * Gardé pour compatibilité.
     */
    @Transactional(readOnly = true)
    public List<CentreDeDonneesResponseDTO> listerPourAdminConnecte(Authentication authentication) {
        return listerPourUtilisateurConnecte(authentication);
    }
}