package com.example.demo.services;

import com.example.demo.dto.AnomalieDecisionRequest;
import com.example.demo.dto.AnomalieGroupeDTO;
import com.example.demo.dto.AnomalieResponseDTO;
import com.example.demo.dto.BatchDecisionRequest;
import com.example.demo.dto.DecisionDTO;
import com.example.demo.entity.Administrateur;
import com.example.demo.entity.Anomalie;
import com.example.demo.entity.Decision;
import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import com.example.demo.entity.StatutAnomalie;
import com.example.demo.repository.AdministrateurRepository;
import com.example.demo.repository.AnomalieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalieService {

    private static final long GAP_SUCCESSIF_MINUTES = 10;

    private final AnomalieRepository anomalieRepository;
    private final AdministrateurRepository administrateurRepository;

    // ==================== SECURITE : VERIFICATION ACCES CENTRE ====================

    /**
     * Verifie que l'utilisateur connecte a acces au centre.
     * Admin = passe toujours.
     * Operateur/Observateur = verifie que le centre est dans sa liste.
     */
    private void verifierAccesCentre(Long centreId, String email) {
        if (centreId == null) return;

        Administrateur user = administrateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve : " + email));

        if (user.isAdmin()) return; // Admin = acces total

        boolean hasAccess = user.getCentres() != null && user.getCentres().stream()
                .anyMatch(c -> c.getIdCentre() != null && c.getIdCentre().equals(centreId));

        if (!hasAccess) {
            throw new AccessDeniedException("Acces refuse : vous n'avez pas les droits sur ce centre (ID: " + centreId + ")");
        }
    }

    /**
     * Verifie l'acces au centre d'une anomalie specifique (par son ID).
     */
    private void verifierAccesAnomalie(Long idAnomalie, String email) {
        Anomalie anomalie = anomalieRepository.findById(idAnomalie)
                .orElseThrow(() -> new RuntimeException("Anomalie non trouvee : " + idAnomalie));

        Long centreId = anomalie.getMetrique() != null
                && anomalie.getMetrique().getEquipement() != null
                && anomalie.getMetrique().getEquipement().getCentreDeDonnees() != null
                ? anomalie.getMetrique().getEquipement().getCentreDeDonnees().getIdCentre()
                : null;

        verifierAccesCentre(centreId, email);
    }

    // ==================== MAPPING DTO ====================

    private AnomalieResponseDTO toResponseDTO(Anomalie anomalie) {
        Metrique metrique = anomalie.getMetrique();
        Equipement equipement = metrique != null ? metrique.getEquipement() : null;

        return new AnomalieResponseDTO(
                anomalie.getIdAnomalie(),
                anomalie.getType() != null ? anomalie.getType().name() : null,
                anomalie.getNiveau(),
                anomalie.getScore(),
                anomalie.getDescription(),
                anomalie.getDateDetection(),
                anomalie.getStatut() != null ? anomalie.getStatut().name() : null,
                anomalie.getDateStatut(),
                anomalie.getCommentaireTraitement(),
                metrique != null ? metrique.getIdMetrique() : null,
                equipement != null ? equipement.getIdEquipement() : null,
                equipement != null ? equipement.getNom() : null,
                equipement != null ? equipement.getAdresseIP() : null
        );
    }

    private DecisionDTO toDecisionDTO(Decision d) {
        return DecisionDTO.builder()
                .idDecision(d.getIdDecision())
                .auteur(d.getAuteur())
                .dateDecision(d.getDateDecision())
                .ancienStatut(d.getAncienStatut())
                .nouveauStatut(d.getNouveauStatut())
                .commentaire(d.getCommentaire())
                .build();
    }

    // ==================== LECTURE (tous roles) ====================

    @Transactional(readOnly = true)
    public List<AnomalieResponseDTO> listerToutesAnomalies(Long centreId, String email) {
        verifierAccesCentre(centreId, email);

        List<Anomalie> anomalies;
        if (centreId != null) {
            anomalies = anomalieRepository.findByCentre(centreId);
        } else {
            anomalies = anomalieRepository.findAllByOrderByDateDetectionDesc();
        }
        return anomalies.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AnomalieResponseDTO> listerAnomaliesNonTraitees(Long centreId, String email) {
        verifierAccesCentre(centreId, email);

        List<Anomalie> anomalies;
        if (centreId != null) {
            anomalies = anomalieRepository.findByStatutAndCentre(
                    StatutAnomalie.NON_TRAITEE.name(), centreId);
        } else {
            anomalies = anomalieRepository.findByStatutOrderByDateDetectionDesc(StatutAnomalie.NON_TRAITEE);
        }
        return anomalies.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AnomalieGroupeDTO> getAnomaliesGroupes(Long centreId, StatutAnomalie statutFiltre, String email) {
        verifierAccesCentre(centreId, email);

        List<Anomalie> anomalies;
        if (statutFiltre != null) {
            anomalies = anomalieRepository.findByCentreAndStatutNative(centreId, statutFiltre.name());
        } else {
            anomalies = anomalieRepository.findByCentre(centreId);
        }

        if (anomalies.isEmpty()) {
            return List.of();
        }

        Map<String, List<Anomalie>> parFamille = anomalies.stream()
                .collect(Collectors.groupingBy(this::cleFamille));

        List<AnomalieGroupeDTO> result = new ArrayList<>();

        for (List<Anomalie> famille : parFamille.values()) {
            List<List<Anomalie>> rafales = decouperEnRafales(famille, GAP_SUCCESSIF_MINUTES);
            for (List<Anomalie> rafale : rafales) {
                result.add(construireGroupe(rafale, centreId));
            }
        }

        return result.stream()
                .sorted(Comparator.comparing(
                        AnomalieGroupeDTO::getDateDerniereDetection,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatsParStatut(Long centreId, String email) {
        verifierAccesCentre(centreId, email);

        List<Object[]> rows = anomalieRepository.countByStatutForCentreNative(centreId);
        Map<String, Long> stats = new HashMap<>();

        for (Object[] row : rows) {
            String statutStr = (String) row[0];
            Long count = ((Number) row[1]).longValue();

            if (statutStr == null) {
                statutStr = StatutAnomalie.NON_TRAITEE.name();
            }

            stats.merge(statutStr, count, Long::sum);
        }

        for (StatutAnomalie s : StatutAnomalie.values()) {
            stats.putIfAbsent(s.name(), 0L);
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public long compterParStatut(StatutAnomalie statut) {
        return anomalieRepository.countByStatut(statut);
    }

    // ==================== ECRITURE (Admin + Operateur) ====================

    @Transactional
    public AnomalieResponseDTO changerStatut(Long idAnomalie, StatutAnomalie nouveauStatut, String commentaire, String email) {
        verifierAccesAnomalie(idAnomalie, email);

        Anomalie anomalie = anomalieRepository.findById(idAnomalie)
                .orElseThrow(() -> new RuntimeException("Anomalie non trouvee : " + idAnomalie));

        anomalie.setStatut(nouveauStatut);
        anomalie.setDateStatut(LocalDateTime.now());
        if (commentaire != null && !commentaire.isBlank()) {
            anomalie.setCommentaireTraitement(commentaire);
        }

        return toResponseDTO(anomalieRepository.save(anomalie));
    }

    @Transactional
    public void prendreDecision(Long idAnomalie, AnomalieDecisionRequest request, String auteur) {
        verifierAccesAnomalie(idAnomalie, auteur);

        Anomalie anomalie = anomalieRepository.findById(idAnomalie)
                .orElseThrow(() -> new IllegalArgumentException("Anomalie introuvable: " + idAnomalie));

        StatutAnomalie ancien = anomalie.getStatut();
        StatutAnomalie nouveau = request.getNouveauStatut();

        Decision decision = Decision.builder()
                .anomalie(anomalie)
                .auteur(auteur)
                .ancienStatut(ancien)
                .nouveauStatut(nouveau)
                .commentaire(request.getCommentaire())
                .dateDecision(LocalDateTime.now())
                .build();

        anomalie.getDecisions().add(decision);
        anomalie.setStatut(nouveau);

        anomalieRepository.save(anomalie);
    }

    @Transactional
    public void traiterBatch(BatchDecisionRequest request, String auteur) {
        if (request.getIdsAnomalies() == null || request.getIdsAnomalies().isEmpty()) {
            throw new IllegalArgumentException("Aucune anomalie selectionnee");
        }

        // Verifier l'acces pour CHAQUE anomalie du batch
        for (Long id : request.getIdsAnomalies()) {
            verifierAccesAnomalie(id, auteur);
        }

        List<Anomalie> anomalies = anomalieRepository.findAllByIds(request.getIdsAnomalies());

        if (anomalies.size() != request.getIdsAnomalies().size()) {
            throw new IllegalArgumentException("Certaines anomalies sont introuvables");
        }

        StatutAnomalie nouveau = request.getNouveauStatut();
        String commentaire = request.getCommentaire();
        LocalDateTime now = LocalDateTime.now();

        for (Anomalie anomalie : anomalies) {
            StatutAnomalie ancien = anomalie.getStatut();

            Decision decision = Decision.builder()
                    .anomalie(anomalie)
                    .auteur(auteur)
                    .ancienStatut(ancien)
                    .nouveauStatut(nouveau)
                    .commentaire(commentaire)
                    .dateDecision(now)
                    .build();

            anomalie.getDecisions().add(decision);
            anomalie.setStatut(nouveau);
        }

        anomalieRepository.saveAll(anomalies);
    }

    // ==================== UTILITAIRES PRIVEES ====================

    private String cleFamille(Anomalie a) {
        Equipement eq = a.getMetrique() != null ? a.getMetrique().getEquipement() : null;
        return (eq != null ? eq.getIdEquipement() : 0) + "|"
                + a.getType().name() + "|"
                + (a.getNiveau() != null ? a.getNiveau() : "INCONNU");
    }

    private List<List<Anomalie>> decouperEnRafales(List<Anomalie> anomalies, long gapMinutes) {
        List<Anomalie> triees = anomalies.stream()
                .sorted(Comparator.comparing(
                    Anomalie::getDateDetection,
                    Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .toList();

        List<List<Anomalie>> rafales = new ArrayList<>();
        List<Anomalie> current = new ArrayList<>();

        for (Anomalie a : triees) {
            if (current.isEmpty()) {
                current.add(a);
            } else {
                Anomalie last = current.get(current.size() - 1);
                LocalDateTime lastDate = last.getDateDetection();
                LocalDateTime currDate = a.getDateDetection();

                if (lastDate != null && currDate != null) {
                    long diffMinutes = Duration.between(lastDate, currDate).toMinutes();
                    if (diffMinutes <= gapMinutes) {
                        current.add(a);
                    } else {
                        rafales.add(new ArrayList<>(current));
                        current.clear();
                        current.add(a);
                    }
                } else {
                    current.add(a);
                }
            }
        }

        if (!current.isEmpty()) {
            rafales.add(current);
        }

        return rafales;
    }

    private AnomalieGroupeDTO construireGroupe(List<Anomalie> rafale, Long centreId) {
        Anomalie first = rafale.get(0);
        Anomalie latest = rafale.stream()
                .max(Comparator.comparing(Anomalie::getDateDetection))
                .orElse(first);

        Equipement eqFirst = first.getMetrique() != null ? first.getMetrique().getEquipement() : null;

        double scoreMax = rafale.stream()
                .mapToDouble(a -> a.getScore())
                .max()
                .orElse(0.0);

        LocalDateTime dateDerniere = rafale.stream()
                .map(Anomalie::getDateDetection)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        LocalDateTime datePremiere = rafale.stream()
                .map(Anomalie::getDateDetection)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        Set<StatutAnomalie> statuts = rafale.stream()
                .map(Anomalie::getStatut)
                .collect(Collectors.toSet());
        String statutGroupe = statuts.size() == 1
                ? statuts.iterator().next().name()
                : "MIXTE";

        String idRafale = (eqFirst != null ? eqFirst.getIdEquipement() : 0) + "|"
                + first.getType().name() + "|"
                + (first.getNiveau() != null ? first.getNiveau() : "INCONNU") + "|"
                + (datePremiere != null ? datePremiere.toString() : "0");

        return AnomalieGroupeDTO.builder()
                .idGroupe(idRafale)
                .idEquipement(eqFirst != null ? eqFirst.getIdEquipement() : null)
                .nomEquipement(eqFirst != null ? eqFirst.getNom() : "Inconnu")
                .typeAnomalie(first.getType().name())
                .niveau(first.getNiveau())
                .scoreMax(scoreMax)
                .dateDerniereDetection(dateDerniere)
                .nombreOccurrences(rafale.size())
                .idsAnomalies(rafale.stream().map(Anomalie::getIdAnomalie).toList())
                .statut(statutGroupe)
                .decisions(latest.getDecisions().stream().map(this::toDecisionDTO).toList())
                .idCentre(centreId)
                .build();
    }
}