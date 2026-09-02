package com.example.demo.services;

import com.example.demo.entity.Anomalie;
import com.example.demo.entity.AnomalieType;
import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import com.example.demo.entity.StatutAnomalie;
import com.example.demo.repository.AnomalieRepository;
import com.example.demo.repository.MetriqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final AnomalieRepository anomalieRepository;
    private final MetriqueRepository metriqueRepository;
    private final EmailService emailService;

    public void detecterTout(Metrique metrique, String emailUtilisateurConnecte) {
        if (metrique == null || metrique.getEquipement() == null) return;

        detecterCpu(metrique, emailUtilisateurConnecte);
        detecterDisk(metrique, emailUtilisateurConnecte);
        detecterNetwork(metrique, emailUtilisateurConnecte);
    }

    private void detecterCpu(Metrique metrique, String emailUser) {
        Float cpu = metrique.getCpu();
        if (cpu == null) return;

        List<Metrique> historique = metriqueRepository.findTop5ByEquipementOrderByDateCollecteDesc(metrique.getEquipement());
        double moyenne = historique.stream()
                .filter(m -> m.getCpu() != null)
                .mapToDouble(Metrique::getCpu)
                .average().orElse(0.0);

        if (moyenne > 85.0) {
            String niveau = moyenne > 95.0 ? "CRITIQUE" : "HAUTE";
            creerAnomalie(metrique, niveau, (float)(moyenne/100.0),
                "Surcharge CPU: moyenne " + String.format("%.1f", moyenne) + "% sur 2.5 min",
                AnomalieType.CPU, emailUser);
        }
    }

    private void detecterDisk(Metrique metrique, String emailUser) {
        Float disk = metrique.getDisque();
        if (disk == null) return;

        if (disk > 95.0f) {
            creerAnomalie(metrique, "CRITIQUE", 1.0f,
                "Disque quasi-sature: " + String.format("%.1f", disk) + "%", AnomalieType.DISK, emailUser);
        } else if (disk > 90.0f) {
            creerAnomalie(metrique, "HAUTE", 0.95f,
                "Disque tres charge: " + String.format("%.1f", disk) + "%", AnomalieType.DISK, emailUser);
        }
    }

    private void detecterNetwork(Metrique metrique, String emailUser) {
        Float reseau = metrique.getReseau();
        if (reseau == null) return;

        List<Metrique> hist = metriqueRepository.findTop3ByEquipementOrderByDateCollecteDesc(metrique.getEquipement());
        boolean toutZero = hist.stream().allMatch(m -> m.getReseau() != null && m.getReseau() == 0.0f);

        if (toutZero && hist.size() >= 3) {
            creerAnomalie(metrique, "CRITIQUE", 1.0f,
                "PANNE RESEAU: traffic nul pendant 1.5 min", AnomalieType.NETWORK, emailUser);
        }
    }

    private void creerAnomalie(Metrique metrique, String niveau, float score,
                                String desc, AnomalieType type, String emailUser) {
        Anomalie a = new Anomalie();
        a.setMetrique(metrique);
        a.setScore(score);
        a.setNiveau(niveau);
        a.setDescription(desc);
        a.setDateDetection(LocalDateTime.now());
        a.setType(type);

        Anomalie saved = anomalieRepository.save(a);
        log.info("🚨 Anomalie {} [{}] : {}", type, niveau, desc);
        emailService.envoyerAlerteStress(saved, metrique, metrique.getEquipement(), emailUser);
    }

    @Transactional(readOnly = true)
public Map<String, Long> getStatsParStatut(Long centreId, String email) {
   
    // Utilise la requête corrigée avec DISTINCT
    List<Object[]> rows = anomalieRepository.countByStatutForCentre(centreId);
    Map<String, Long> stats = new HashMap<>();

    for (Object[] row : rows) {
        String statutStr = (String) row[0];
        Long count = ((Number) row[1]).longValue();

        if (statutStr == null) {
            statutStr = StatutAnomalie.NON_TRAITEE.name();
        }

        stats.put(statutStr, count);
    }

    // Initialise tous les statuts à 0 s'ils n'existent pas
    for (StatutAnomalie s : StatutAnomalie.values()) {
        stats.putIfAbsent(s.name(), 0L);
    }

    return stats;
}
}