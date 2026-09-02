package com.example.demo.services;

import com.example.demo.entity.CentreDeDonnees;
import com.example.demo.entity.Equipement;
import com.example.demo.entity.Fournisseur;
import com.example.demo.entity.Metrique;
import com.example.demo.entity.TypeInfrastructure;
import com.example.demo.repository.CentreDeDonneesRepository;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.MetriqueRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CloudMetricsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CloudMetricsScheduler.class);

    private final CentreDeDonneesRepository centreRepository;
    private final EquipementRepository equipementRepository;
    private final MetriqueRepository metriqueRepository;
    private final AwsCloudWatchService awsService;
    private final AnomalyDetectionService anomalyDetectionService;

    /**
     * Collecte les métriques cloud toutes les 60 secondes.
     * Seuls les centres de type CLOUD avec fournisseur AWS sont traités (mock pour l'instant).
     */
    @Scheduled(fixedRate = 60_000)
    public void collectCloudMetrics() {
        logger.info("☁️ Début collecte métriques CLOUD");

        List<CentreDeDonnees> cloudCentres = centreRepository.findByType(TypeInfrastructure.CLOUD);

        if (cloudCentres.isEmpty()) {
            logger.debug("Aucun centre de données cloud trouvé.");
            return;
        }

        for (CentreDeDonnees centre : cloudCentres) {
            // Pour l'instant, seul AWS est supporté (mock)
            if (centre.getFournisseur() != Fournisseur.AWS) {
                logger.info("⏩ Fournisseur {} non supporté pour le centre {}", 
                    centre.getFournisseur(), centre.getNom());
                continue;
            }

            List<Equipement> equipements = equipementRepository
                    .findByCentreDeDonnees_IdCentre(centre.getIdCentre());

            for (Equipement eq : equipements) {
                if (eq.getCloudInstanceId() == null || eq.getCloudInstanceId().isBlank()) {
                    logger.warn("⚠️ Équipement cloud {} sans instance ID, ignoré.", eq.getNom());
                    continue;
                }

                try {
                    logger.info("☁️ Collecte CloudWatch pour {} (Instance: {})", 
                        eq.getNom(), eq.getCloudInstanceId());

                    List<Metrique> metrics = awsService.fetchMetrics(
                            eq, 
                            centre.getRegion(), 
                            2 // minutesBack
                    );

                    for (Metrique m : metrics) {
                        Metrique saved = metriqueRepository.save(m);
                        logger.info("✅ Métrique cloud sauvegardée pour {} (CPU: {}%)", 
                            eq.getNom(), saved.getCpu());

                        // Détection d'anomalies (même pipeline que le physique)
                        // Note : pas de contexte HTTP dans un @Scheduled, 
                        // donc pas d'utilisateur connecté. On passe "system".
                        anomalyDetectionService.detecterTout(saved, "system");
                    }

                } catch (Exception e) {
                    logger.error("❌ Erreur collecte cloud pour {}: {}", 
                        eq.getNom(), e.getMessage(), e);
                }
            }
        }

        logger.info("☁️ Fin collecte métriques CLOUD");
    }
}