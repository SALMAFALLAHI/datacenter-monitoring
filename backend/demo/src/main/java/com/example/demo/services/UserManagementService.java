package com.example.demo.services;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserAccessRequest;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.Administrateur;
import com.example.demo.entity.CentreDeDonnees;
import com.example.demo.entity.DashboardModule;
import com.example.demo.entity.Role;
import com.example.demo.repository.AdministrateurRepository;
import com.example.demo.repository.CentreDeDonneesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AdministrateurRepository administrateurRepository;
    private final CentreDeDonneesRepository centreDeDonneesRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO createUser(CreateUserRequest request) {
        if (administrateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        Administrateur admin = getCurrentUser();

        // Définir les accès dashboard par défaut selon le rôle
        Set<DashboardModule> access = request.getDashboardAccess();
        if (request.getRole() == Role.ADMIN) {
            access = new HashSet<>(List.of(DashboardModule.values()));
        } else if (access == null || access.isEmpty()) {
            access = new HashSet<>();
            if (request.getRole() == Role.OPERATEUR) {
                access.add(DashboardModule.ANOMALIES);
                access.add(DashboardModule.EQUIPEMENTS);
                access.add(DashboardModule.METRIQUES);
            } else {
                access.add(DashboardModule.ANOMALIES);
                access.add(DashboardModule.RAPPORTS);
            }
        }

        // ===== Étape 1 : créer l'utilisateur SANS centres =====
        Administrateur newUser = new Administrateur();
        newUser.setNom(request.getNom());
        newUser.setPrenom(request.getPrenom());
        newUser.setEmail(request.getEmail());
        newUser.setMotDePasse(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(request.getRole().name());
        newUser.setDashboardAccess(access);
        newUser.setActif(request.isActif());
        newUser.setCreatedBy(admin);
        // NE PAS toucher à newUser.setCentres() !

        // ===== Étape 2 : sauvegarder pour obtenir l'ID =====
        Administrateur saved = administrateurRepository.save(newUser);

        // ===== Étape 3 : assigner les centres manuellement via leur repository =====
        if (request.getCentreIds() != null && !request.getCentreIds().isEmpty()) {
            List<CentreDeDonnees> centres = centreDeDonneesRepository.findAllById(request.getCentreIds());
            for (CentreDeDonnees centre : centres) {
                centre.setAdministrateur(saved);
            }
            centreDeDonneesRepository.saveAll(centres);
        }

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return administrateurRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO updateUserAccess(Long userId, UpdateUserAccessRequest request) {
        Administrateur user = administrateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (request.getDashboardAccess() != null) {
            user.setDashboardAccess(request.getDashboardAccess());
        }
        if (request.getActif() != null) {
            user.setActif(request.getActif());
        }

        return toResponseDTO(administrateurRepository.save(user));
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        Administrateur user = administrateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        user.setActif(!user.isActif());
        administrateurRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        Administrateur current = getCurrentUser();
        if (current.getIdAdmin().equals(userId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
        }
        administrateurRepository.deleteById(userId);
    }

    private UserResponseDTO toResponseDTO(Administrateur u) {
        return UserResponseDTO.builder()
                .idAdmin(u.getIdAdmin())
                .email(u.getEmail())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .fullName(u.getFullName())
                .role(u.getRole())
                .dashboardAccess(u.getDashboardAccess())
                .actif(u.isActif())
                .createdByName(u.getCreatedBy() != null ? u.getCreatedBy().getFullName() : "Système")
                .centreIds(u.getCentres() != null ? u.getCentres().stream().map(c -> c.getIdCentre()).toList() : List.of())
                .centreNoms(u.getCentres() != null ? u.getCentres().stream().map(CentreDeDonnees::getNom).toList() : List.of())
                .build();
    }

    private Administrateur getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return administrateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté non trouvé"));
    }
}