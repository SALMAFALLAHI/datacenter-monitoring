package com.example.demo.services;

import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import com.example.demo.entity.TypeInfrastructure;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.MetriqueRepository;
import com.example.demo.security.SecurityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrometheusService {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusService.class);
    private final SecurityUtils securityUtils;

    @Value("${prometheus.url:http://172.30.198.232:9090}")
    private String prometheusUrl;

    private final RestTemplate restTemplate;
    private final MetriqueRepository metriqueRepository;
    private final EquipementRepository equipementRepository;
    private final CsvDataLogger csvDataLogger;

    // =====  Injection du service ML =====
    private final MlStressDetectionService mlStressDetectionService;
    private final AnomalyDetectionService anomalyDetectionService;
    // =============================================

    public PrometheusService(MetriqueRepository metriqueRepository, EquipementRepository equipementRepository, MlStressDetectionService mlStressDetectionService, AnomalyDetectionService anomalyDetectionService, SecurityUtils securityUtils) {
        this.restTemplate = new RestTemplate();
        this.metriqueRepository = metriqueRepository;
        this.equipementRepository = equipementRepository;
        this.mlStressDetectionService = mlStressDetectionService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.securityUtils = securityUtils;
        this.csvDataLogger = new CsvDataLogger();
    }

    @Scheduled(fixedRate = 30000)
    public void collecterEtSauvegarder() {
        logger.info("🔄 Debut collecte metriques depuis Prometheus: {}", prometheusUrl);

        LocalDateTime cycleTimestamp = LocalDateTime.now();

        List<Equipement> equipements = equipementRepository.findAll();

        for (Equipement equipement : equipements) {
            if (equipement.getCentreDeDonnees() != null 
                && equipement.getCentreDeDonnees().getType() == TypeInfrastructure.CLOUD) {
            logger.debug("⏩ Skip {} (géré par CloudMetricsScheduler)", equipement.getNom());
            continue;
        }
            try {
                String ip = equipement.getAdresseIP();
                logger.info("📡 Collecte pour {} (IP: {})", equipement.getNom(), ip);

                Float cpu = collecterCpu();
                Float ram = collecterRam();
                Double ramPct = collecterRamPct();
                Double usedGb = collecterUsedGb();
                Double availableGb = collecterAvailableGb();
                Double swapPct = collecterSwapPct();
                Float disque = collecterDisk();
                Float reseau = collecterNetwork();

                logger.info("📊 Valeurs brutes pour {}: cpu={}, ram={}, ramPct={}, usedGb={}, availableGb={}, swapPct={}, disque={}, reseau={}",
                    equipement.getNom(), cpu, ram, ramPct, usedGb, availableGb, swapPct, disque, reseau);

                Metrique metrique = new Metrique();
                metrique.setCpu(cpu);
                metrique.setRam(ram);
                metrique.setRamPct(ramPct);
                metrique.setUsedGb(usedGb);
                metrique.setAvailableGb(availableGb);
                metrique.setSwapPct(swapPct);
                metrique.setDisque(disque);
                metrique.setReseau(reseau);
                metrique.setDateCollecte(cycleTimestamp);
                metrique.setEquipement(equipement);

                Metrique saved = metriqueRepository.save(metrique);
                logger.info("✅ Metrique sauvegardee pour {} (RAM: {}%, USED: {}GB, AVAIL: {}GB, SWAP: {}%)",
               
                    equipement.getNom(), ramPct, usedGb, availableGb, swapPct);
                     csvDataLogger.log(saved);
                String emailUser = securityUtils.getEmailUtilisateurConnecte();
                // =====  Detection ML =====
                logger.info("🤖 Appel detection ML pour {}...", equipement.getNom());
                
                var mlResult = mlStressDetectionService.detectStress(saved, emailUser);
                            if (mlResult != null) {
                                    logger.info("🎯 Resultat ML: alertLevel={}, proba={}, isStress={}",
                        mlResult.getAlertLevel(), mlResult.getProba(), mlResult.getIsStress());}
                        // 2. Detection CPU/Disk/Network (regles metier)
                        logger.info("🤖 Detection CPU/Disk/Network (regles)...");
                        anomalyDetectionService.detecterTout(saved, emailUser);
                // =================================

            } catch (Exception e) {
                logger.error("❌ Erreur collecte pour {}: {}", equipement.getNom(), e.getMessage(), e);
            }
        }
    }

    private URI buildUri(String query) {
        URI uri = UriComponentsBuilder.fromUriString(prometheusUrl)
            .path("/api/v1/query")
            .queryParam("query", query)
            .build()
            .toUri();
        logger.info("🔍 URL envoyee: {}", uri);
        return uri;
    }

    private String queryPrometheus(String query) {
        URI uri = buildUri(query);
        String response = restTemplate.getForObject(uri, String.class);
        logger.info("📥 Reponse brute: {}", response);
        return response;
    }

    private Float collecterCpu() {
        try {
            String response = queryPrometheus("100 - (avg(irate(node_cpu_seconds_total{mode='idle'}[5m])) * 100)");
            return parseFloat(response, "cpu");
        } catch (Exception e) {
            logger.error("❌ Erreur CPU: {}", e.getMessage());
            return 0.0f;
        }
    }

    private Float collecterRam() {
        try {
            String response = queryPrometheus("100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))");
            return parseFloat(response, "ram");
        } catch (Exception e) {
            logger.error("❌ Erreur RAM: {}", e.getMessage());
            return 0.0f;
        }
    }

    private Double collecterRamPct() {
        try {
            String response = queryPrometheus("100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))");
            return parseDouble(response, "ramPct");
        } catch (Exception e) {
            logger.error("❌ Erreur RAM%: {}", e.getMessage());
            return 0.0d;
        }
    }

    private Double collecterUsedGb() {
        try {
            String response = queryPrometheus("(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / 1024 / 1024 / 1024");
            return parseDouble(response, "usedGb");
        } catch (Exception e) {
            logger.error("❌ Erreur UsedGB: {}", e.getMessage());
            return 0.0d;
        }
    }

    private Double collecterAvailableGb() {
        try {
            String response = queryPrometheus("node_memory_MemAvailable_bytes / 1024 / 1024 / 1024");
            return parseDouble(response, "availableGb");
        } catch (Exception e) {
            logger.error("❌ Erreur AvailGB: {}", e.getMessage());
            return 0.0d;
        }
    }

    private Double collecterSwapPct() {
        try {
            Double total = parseDouble(queryPrometheus("node_memory_SwapTotal_bytes"), "swapTotal");
            Double free = parseDouble(queryPrometheus("node_memory_SwapFree_bytes"), "swapFree");

            if (total == null || total <= 0.0d) {
                logger.info("⚠️ Swap total = 0 ou null");
                return 0.0d;
            }

            double usedPct = 100.0d * (1.0d - (free / total));
            logger.info("✅ swapPct calcule: {}%", usedPct);
            return Math.max(0.0d, usedPct);
        } catch (Exception e) {
            logger.error("❌ Erreur Swap%: {}", e.getMessage());
            return 0.0d;
        }
    }

    private Float collecterDisk() {
        try {
            String response = queryPrometheus("100 * (1 - (node_filesystem_avail_bytes{mountpoint='/'} / node_filesystem_size_bytes{mountpoint='/'}))");
            return parseFloat(response, "disk");
        } catch (Exception e) {
            logger.error("❌ Erreur Disk: {}", e.getMessage());
            return 0.0f;
        }
    }

    private Float collecterNetwork() {
        try {
            String response = queryPrometheus("sum(rate(node_network_receive_bytes_total{device='ens33'}[5m])) / 1024 / 1024");
            return parseFloat(response, "network");
        } catch (Exception e) {
            logger.error("❌ Erreur Network: {}", e.getMessage());
            return 0.0f;
        }
    }

    private Float parseFloat(String response, String type) {
        if (response == null) {
            logger.warn("⚠️ Response null pour {}", type);
            return 0.0f;
        }
        JSONObject json = new JSONObject(response);
        if (!"success".equals(json.getString("status"))) {
            logger.warn("⚠️ Query {} failed: {}", type, json.optString("error", "unknown"));
            return 0.0f;
        }
        JSONArray results = json.getJSONObject("data").getJSONArray("result");
        if (results.isEmpty()) {
            logger.warn("⚠️ Query {} retourne 0 resultats", type);
            return 0.0f;
        }
        JSONArray value = results.getJSONObject(0).getJSONArray("value");
        double val = value.getDouble(1);
        logger.info("✅ {} = {}", type, val);
        return (float) val;
    }

    private Double parseDouble(String response, String type) {
        if (response == null) {
            logger.warn("⚠️ Response null pour {}", type);
            return 0.0d;
        }
        JSONObject json = new JSONObject(response);
        if (!"success".equals(json.getString("status"))) {
            logger.warn("⚠️ Query {} failed: {}", type, json.optString("error", "unknown"));
            return 0.0d;
        }
        JSONArray results = json.getJSONObject("data").getJSONArray("result");
        if (results.isEmpty()) {
            logger.warn("⚠️ Query {} retourne 0 resultats", type);
            return 0.0d;
        }
        JSONArray value = results.getJSONObject(0).getJSONArray("value");
        double val = value.getDouble(1);
        logger.info("✅ {} = {}", type, val);
        return val;
    }
}