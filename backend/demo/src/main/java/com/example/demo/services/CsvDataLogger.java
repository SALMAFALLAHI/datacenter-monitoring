package com.example.demo.services;

import com.example.demo.entity.Metrique;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CsvDataLogger {

    @Value("${ml.data.csv.path:}")
    private String csvPath;

    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] HEADERS = {
        "timestamp", "cpu", "ram_pct", "used_gb", "available_gb",
        "swap_pct", "disk_pct", "network_mb", "label"
    };

    public void log(Metrique metrique) {
        try {
            // Si pas configuré, utilise un chemin par defaut
            String pathStr = (csvPath != null && !csvPath.isBlank()) 
                ? csvPath 
                : "ml_data/metriques.csv";
            
            Path path = Paths.get(pathStr).toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());

            boolean fileExists = Files.exists(path);

            try (PrintWriter writer = new PrintWriter(new FileWriter(path.toFile(), true))) {
                if (!fileExists) {
                    writer.println(String.join(",", HEADERS));
                    log.info("Fichier CSV cree : {}", path);
                }

                String line = String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.6f,",
                    LocalDateTime.now().format(FORMATTER),
                    metrique.getCpu() != null ? metrique.getCpu() : 0.0,
                    metrique.getRamPct() != null ? metrique.getRamPct() : 0.0,
                    metrique.getUsedGb() != null ? metrique.getUsedGb() : 0.0,
                    metrique.getAvailableGb() != null ? metrique.getAvailableGb() : 0.0,
                    metrique.getSwapPct() != null ? metrique.getSwapPct() : 0.0,
                    metrique.getDisque() != null ? metrique.getDisque() : 0.0,
                    metrique.getReseau() != null ? metrique.getReseau() : 0.0
                );

                writer.println(line);
            }

        } catch (IOException e) {
            log.error("Erreur ecriture CSV : {}", e.getMessage());
        }
    }
}