package com.example.demo.services;

import com.example.demo.dto.MetriqueResponseDTO;
import com.example.demo.dto.MetriqueSeriePointDTO;
import com.example.demo.dto.MetriqueStatutResponseDTO;
import com.example.demo.entity.Anomalie;
import com.example.demo.entity.Metrique;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.MetriqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetriqueService {

    private final MetriqueRepository metriqueRepository;
    private final EquipementRepository equipementRepository;
    private final MlStressDetectionService mlStressDetectionService;

    // ==================== MAPPING DTO ====================

    private MetriqueResponseDTO toResponseDTO(Metrique metrique) {
        return new MetriqueResponseDTO(
                metrique.getIdMetrique(),
                metrique.getCpu(),
                metrique.getRam(),
                metrique.getRamPct(),
                metrique.getUsedGb(),
                metrique.getAvailableGb(),
                metrique.getSwapPct(),
                metrique.getDisque(),
                metrique.getTemperature(),
                metrique.getReseau(),
                metrique.getDateCollecte(),
                metrique.getEquipement() != null ? metrique.getEquipement().getIdEquipement() : null,
                metrique.getEquipement() != null ? metrique.getEquipement().getNom() : null,
                metrique.getEquipement() != null ? metrique.getEquipement().getAdresseIP() : null,
                metrique.getSource() != null ? metrique.getSource().name() : null
        );
    }

    private MetriqueStatutResponseDTO toStatutResponseDTO(Metrique metrique) {
        List<MetriqueStatutResponseDTO.AnomalieInfoDTO> anomalies = metrique.getAnomalies() == null
                ? List.of()
                : metrique.getAnomalies().stream()
                        .sorted(Comparator.comparing(Anomalie::getDateDetection, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(a -> new MetriqueStatutResponseDTO.AnomalieInfoDTO(
                                a.getType() != null ? a.getType().name() : "INCONNU",
                                a.getNiveau() != null ? a.getNiveau() : "INCONNU",
                                a.getDescription(),
                                a.getDateDetection() != null ? a.getDateDetection().toString() : null
                        ))
                        .toList();

        List<String> typesAnomalie = anomalies.stream()
                .map(MetriqueStatutResponseDTO.AnomalieInfoDTO::getType)
                .distinct()
                .toList();

        String status = deriveGlobalStatus(anomalies);

        return new MetriqueStatutResponseDTO(
                toResponseDTO(metrique),
                status,
                typesAnomalie,
                anomalies
        );
    }

    private String deriveGlobalStatus(List<MetriqueStatutResponseDTO.AnomalieInfoDTO> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            return "NORMAL";
        }
        int maxRank = anomalies.stream()
                .map(MetriqueStatutResponseDTO.AnomalieInfoDTO::getNiveau)
                .map(this::severityRank)
                .max(Integer::compareTo)
                .orElse(0);

        return switch (maxRank) {
            case 3 -> "CRITIQUE";
            case 2 -> "HAUTE";
            case 1 -> "MINEUR";
            default -> "NORMAL";
        };
    }

    private int severityRank(String niveau) {
        if (niveau == null) return 0;
        String n = niveau.toUpperCase(Locale.ROOT);
        if ("CRITIQUE".equals(n)) return 3;
        if ("HAUTE".equals(n) || "MAJEUR".equals(n) || "STRESS".equals(n)) return 2;
        if ("MINEUR".equals(n) || "SUSPECT".equals(n)) return 1;
        return 0;
    }

    private void ensureEquipementExists(Long idEquipement) {
        equipementRepository.findById(idEquipement)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipement introuvable avec l'id : " + idEquipement));
    }

    // ==================== METHODES EXISTANTES ====================

    @Transactional(readOnly = true)
    public List<MetriqueResponseDTO> listerParEquipement(Long idEquipement) {
        ensureEquipementExists(idEquipement);
        return metriqueRepository.findByEquipement_IdEquipementOrderByDateCollecteDesc(idEquipement).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MetriqueStatutResponseDTO> listerParEquipementAvecStatut(Long idEquipement) {
        ensureEquipementExists(idEquipement);
        return metriqueRepository.findByEquipement_IdEquipementOrderByDateCollecteDesc(idEquipement).stream()
                .map(this::toStatutResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public MetriqueResponseDTO trouverDerniereParEquipement(Long idEquipement) {
        ensureEquipementExists(idEquipement);
        List<Metrique> metriques = metriqueRepository.findByEquipement_IdEquipementOrderByDateCollecteDesc(idEquipement);
        if (metriques.isEmpty()) {
            throw new IllegalArgumentException("Aucune metrique trouvee pour l'equipement : " + idEquipement);
        }
        return toResponseDTO(metriques.get(0));
    }

    @Transactional(readOnly = true)
    public MetriqueStatutResponseDTO trouverDerniereParEquipementAvecStatut(Long idEquipement) {
        ensureEquipementExists(idEquipement);
        List<Metrique> metriques = metriqueRepository.findByEquipement_IdEquipementOrderByDateCollecteDesc(idEquipement);
        if (metriques.isEmpty()) {
            throw new IllegalArgumentException("Aucune metrique trouvee pour l'equipement : " + idEquipement);
        }
        return toStatutResponseDTO(metriques.get(0));
    }

    @Transactional
    public Metrique saveMetrique(Metrique metrique) {
        if (metrique.getEquipement() == null || metrique.getEquipement().getIdEquipement() == null) {
            throw new IllegalArgumentException("L'equipement doit etre specifie pour la metrique.");
        }
        ensureEquipementExists(metrique.getEquipement().getIdEquipement());
        Metrique saved = metriqueRepository.save(metrique);
        mlStressDetectionService.detectStress(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MetriqueResponseDTO> trouverDernieresPourTous() {
        return equipementRepository.findAll().stream()
                .map(eq -> {
                    List<Metrique> metriques = metriqueRepository
                            .findByEquipement_IdEquipementOrderByDateCollecteDesc(eq.getIdEquipement());
                    return metriques.isEmpty() ? null : toResponseDTO(metriques.get(0));
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // ==================== NOUVELLES METHODES DASHBOARD ====================

    @Transactional(readOnly = true)
    public List<MetriqueResponseDTO> findAllLatest() {
        List<Metrique> metriques = metriqueRepository.findAllLatest();
        return garderDerniereParEquipement(metriques).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MetriqueResponseDTO> findByCentre(Long centreId) {
        List<Metrique> metriques = metriqueRepository.findLatestByCentre(centreId);
        return garderDerniereParEquipement(metriques).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MetriqueSeriePointDTO> getHistorique(Long centreId, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        List<Object[]> rows = (centreId != null)
                ? metriqueRepository.getHistoriqueByCentre(centreId, since)
                : metriqueRepository.getHistoriqueGlobal(since);

        List<MetriqueSeriePointDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            MetriqueSeriePointDTO dto = new MetriqueSeriePointDTO();
            dto.setDateCollecte(toLocalDateTime(row[0]));
            dto.setCpuMoyen(toDouble(row[1]));
            dto.setRamPctMoyen(toDouble(row[2]));
            dto.setReseauMoyen(toDouble(row[3]));
            dto.setDiskPctMoyen(toDouble(row[4]));
            dto.setNombreEquipements(((Number) row[5]).intValue());
            result.add(dto);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MetriqueSeriePointDTO> getHistoriqueAgrege(int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        List<Metrique> metriques = metriqueRepository.findByDateCollecteAfterOrderByDateCollecteAsc(since);

        Map<LocalDateTime, List<Metrique>> parCycle = metriques.stream()
                .collect(Collectors.groupingBy(
                        Metrique::getDateCollecte,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return parCycle.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Metrique> groupe = entry.getValue();
                    return new MetriqueSeriePointDTO(
                            entry.getKey(),
                            moyenne(groupe, m -> m.getCpu() != null ? m.getCpu().doubleValue() : 0.0),
                            moyenne(groupe, m -> m.getRamPct() != null ? m.getRamPct() : 0.0),
                            moyenne(groupe, m -> m.getReseau() != null ? m.getReseau().doubleValue() : 0.0),
                            moyenne(groupe, m -> m.getDisque() != null ? m.getDisque().doubleValue() : 0.0),
                            groupe.size()
                    );
                })
                .toList();
    }

    // ==================== UTILITAIRES ====================

    private List<Metrique> garderDerniereParEquipement(List<Metrique> metriques) {
        Map<Long, Metrique> map = new LinkedHashMap<>();
        for (Metrique m : metriques) {
            Long idEq = m.getEquipement().getIdEquipement();
            if (!map.containsKey(idEq) || m.getDateCollecte().isAfter(map.get(idEq).getDateCollecte())) {
                map.put(idEq, m);
            }
        }
        return new ArrayList<>(map.values());
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        if (value instanceof java.util.Date) return ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (value instanceof Instant) return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        throw new IllegalArgumentException("Type de date non supporte: " + value.getClass().getName());
    }

    private double moyenne(List<Metrique> metriques, java.util.function.Function<Metrique, Double> extractor) {
        return metriques.stream()
                .map(extractor)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}