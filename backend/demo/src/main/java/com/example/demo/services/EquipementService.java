package com.example.demo.services;

import com.example.demo.dto.EquipementRequestDTO;
import com.example.demo.dto.EquipementResponseDTO;
import com.example.demo.dto.ScanPlageRequestDTO;
import com.example.demo.dto.ScanPlageResultDTO;
import com.example.demo.entity.AnalyseReseau;
import com.example.demo.entity.CentreDeDonnees;
import com.example.demo.entity.Equipement;
import com.example.demo.entity.SystemeExploitation;
import com.example.demo.entity.TypeEquipement;
import com.example.demo.repository.CentreDeDonneesRepository;
import com.example.demo.repository.EquipementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class EquipementService {

    private static final Logger log = LoggerFactory.getLogger(EquipementService.class);

    private final EquipementRepository equipementRepository;
    private final CentreDeDonneesRepository centreDeDonneesRepository;

    // Timeout configurable (ms)
    private static final int TIMEOUT_MS = 3000;
    private static final int PORT_TIMEOUT_MS = 1500;

    // Ports étendus pour une meilleure détection
    private static final int[] PORTS_A_SCANNER = {
        22,     // SSH
        23,     // Telnet
        53,     // DNS
        80,     // HTTP
        443,    // HTTPS
        445,    // SMB
        1433,   // MSSQL
        3306,   // MySQL
        3389,   // RDP
        5432,   // PostgreSQL
        5900,   // VNC
        6379,   // Redis
        8080,   // HTTP Alt
        8443,   // HTTPS Alt
        9100,   // JetDirect
        9200,   // Elasticsearch
        27017,  // MongoDB
    };

    // ==================== MAPPER ====================

    private EquipementResponseDTO toResponseDTO(Equipement e) {
        EquipementResponseDTO dto = new EquipementResponseDTO();
        dto.setIdEquipement(e.getIdEquipement());
        dto.setNom(e.getNom());
        dto.setType(e.getType() != null ? e.getType() : null);
        dto.setSysteme(e.getSysteme() != null ? e.getSysteme() : null);
        dto.setEtat(e.getEtat());
        dto.setAdresseIP(e.getAdresseIP());
        dto.setDateAjout(e.getDateAjout());
        dto.setRamTotaleGb(e.getRamTotaleGb());

        if (e.getCentreDeDonnees() != null) {
            dto.setIdCentre(e.getCentreDeDonnees().getIdCentre());
            dto.setNomCentre(e.getCentreDeDonnees().getNom());
        }
        return dto;
    }

    // ==================== ANALYSE RÉSEAU ====================

    public AnalyseReseau analyserEquipement(String adresseIP) {
        AnalyseReseau analyse = new AnalyseReseau();
        analyse.setAdresseIP(adresseIP);

        // Scan des ports en parallèle pour plus de rapidité
        boolean[] portsOuverts = new boolean[PORTS_A_SCANNER.length];
        for (int i = 0; i < PORTS_A_SCANNER.length; i++) {
            portsOuverts[i] = testerPort(adresseIP, PORTS_A_SCANNER[i]);
        }

        boolean port22 = portsOuverts[0];
        boolean port23 = portsOuverts[1];
        boolean port53 = portsOuverts[2];
        boolean port80 = portsOuverts[3];
        boolean port443 = portsOuverts[4];
        boolean port445 = portsOuverts[5];
        boolean port1433 = portsOuverts[6];
        boolean port3306 = portsOuverts[7];
        boolean port3389 = portsOuverts[8];
        boolean port5432 = portsOuverts[9];
        boolean port5900 = portsOuverts[10];
        boolean port6379 = portsOuverts[11];
        boolean port8080 = portsOuverts[12];
        boolean port8443 = portsOuverts[13];
        boolean port9100 = portsOuverts[14];
        boolean port9200 = portsOuverts[15];
        boolean port27017 = portsOuverts[16];

        boolean anyWeb = port80 || port443 || port8080 || port8443;
        boolean anyDB = port3306 || port5432 || port1433 || port6379 || port9200 || port27017;
        boolean anyRemote = port22 || port3389 || port5900;
        boolean anyNetwork = port53 || port23;

        // Détection système d'exploitation
        if (port3389 || port445) {
            analyse.setSystemeDetecte("WINDOWS");
        } else if (port22) {
            analyse.setSystemeDetecte("LINUX");
        } else if (anyNetwork && !anyWeb && !anyRemote && !anyDB) {
            analyse.setSystemeDetecte("EMBARQUE");
        } else {
            analyse.setSystemeDetecte("INCONNU");
        }

        // Détection type d'équipement
        if (port9100 && !anyWeb && !anyRemote && !anyDB) {
            analyse.setTypeDetecte("IMPRIMANTE");
        } else if (port53 && !anyWeb && !anyRemote && !anyDB) {
            analyse.setTypeDetecte("SERVEUR_DNS");
        } else if (anyDB && !anyWeb) {
            analyse.setTypeDetecte("SERVEUR_BASE_DE_DONNEES");
        } else if (port6379 || port9200 || port27017) {
            analyse.setTypeDetecte("SERVEUR");
        } else if (anyWeb) {
            analyse.setTypeDetecte("SERVEUR_WEB");
        } else if (anyRemote) {
            analyse.setTypeDetecte("SERVEUR");
        } else if (port445 && !anyWeb && !anyDB) {
            analyse.setTypeDetecte("WORKSTATION");
        } else if (port23 && !anyWeb && !anyDB && !anyRemote) {
            analyse.setTypeDetecte("EQUIPEMENT_RESEAU");
        } else {
            analyse.setTypeDetecte("INCONNU");
        }

        // Joignabilité : au moins un port ouvert OU ping réussi
        boolean joignableParPort = false;
        for (boolean p : portsOuverts) {
            if (p) { joignableParPort = true; break; }
        }
        boolean joignableParPing = testerPing(adresseIP);
        analyse.setJoignable(joignableParPort || joignableParPing);

        log.info("Analyse réseau {} : système={}, type={}, joignable={}",
                adresseIP, analyse.getSystemeDetecte(), analyse.getTypeDetecte(), analyse.isJoignable());

        return analyse;
    }

    // ==================== SCAN DE PLAGE  ====================

    @Transactional(readOnly = true)
    public List<ScanPlageResultDTO> scannerPlage(ScanPlageRequestDTO request) {
        String ipDebut = request.getIpDebut();
        String ipFin = request.getIpFin();

        List<String> ips = genererPlageIP(ipDebut, ipFin);
        List<ScanPlageResultDTO> resultats = new ArrayList<>();

        // Thread pool pour paralléliser (max 50 threads)
        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<Future<ScanPlageResultDTO>> futures = new ArrayList<>();

        for (String ip : ips) {
            futures.add(executor.submit(() -> {
                AnalyseReseau analyse = analyserEquipement(ip);
                ScanPlageResultDTO r = new ScanPlageResultDTO();
                r.setAdresseIP(ip);
                r.setJoignable(analyse.isJoignable());
                r.setTypeDetecte(analyse.getTypeDetecte());
                r.setSystemeDetecte(analyse.getSystemeDetecte());
                return r;
            }));
        }

        for (Future<ScanPlageResultDTO> f : futures) {
            try {
                ScanPlageResultDTO r = f.get(10, TimeUnit.SECONDS);
                // NE garder que les machines réellement identifiées (pas juste ping)
                if (r.isJoignable() && !"INCONNU".equals(r.getTypeDetecte())) {
                    resultats.add(r);
                }
            } catch (Exception e) {
                log.warn("Erreur scan d'une IP : {}", e.getMessage());
            }
        }

        executor.shutdown();
        return resultats;
    }

    private List<String> genererPlageIP(String debut, String fin) {
        List<String> ips = new ArrayList<>();
        try {
            String[] d = debut.split("\\.");
            String[] f = fin.split("\\.");
            int start = (Integer.parseInt(d[0]) << 24) | (Integer.parseInt(d[1]) << 16)
                      | (Integer.parseInt(d[2]) << 8) | Integer.parseInt(d[3]);
            int end = (Integer.parseInt(f[0]) << 24) | (Integer.parseInt(f[1]) << 16)
                    | (Integer.parseInt(f[2]) << 8) | Integer.parseInt(f[3]);

            for (int i = start; i <= end && i - start < 256; i++) { // max 256 IPs
                ips.add(String.format("%d.%d.%d.%d",
                        (i >> 24) & 0xFF, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF));
            }
        } catch (Exception e) {
            log.error("Plage IP invalide : {} - {}", debut, fin);
        }
        return ips;
    }

    // ==================== DÉTERMINATION TYPE/SYSTÈME  ====================

    private String determinerType(String typeDeclare, AnalyseReseau analyse) {
        if (typeDeclare == null || typeDeclare.isBlank()) {
            if (!"INCONNU".equals(analyse.getTypeDetecte())) {
                log.info("Type auto-détecté : {}", analyse.getTypeDetecte());
                return analyse.getTypeDetecte();
            }
            // Au lieu de planter, on met une valeur par défaut et on loggue
            log.warn("Type indétectable pour {}, utilisation de SERVEUR par défaut", analyse.getAdresseIP());
            return "SERVEUR";
        }

        // Vérification cohérence seulement si la détection n'est pas INCONNU
        if (!"INCONNU".equals(analyse.getTypeDetecte())
                && !typeDeclare.equalsIgnoreCase(analyse.getTypeDetecte())) {
            log.warn("Incohérence type détecté vs déclaré : détecté={}, déclaré={}",
                    analyse.getTypeDetecte(), typeDeclare);
            // On autorise quand même mais on loggue (évite le blocage 403/400)
        }
        return typeDeclare.toUpperCase();
    }

    private String determinerSysteme(String systemeDeclare, AnalyseReseau analyse) {
        if (systemeDeclare == null || systemeDeclare.isBlank()) {
            if (!"INCONNU".equals(analyse.getSystemeDetecte())) {
                log.info("Système auto-détecté : {}", analyse.getSystemeDetecte());
                return analyse.getSystemeDetecte();
            }
            log.warn("Système indétectable pour {}, utilisation de LINUX par défaut", analyse.getAdresseIP());
            return "LINUX";
        }

        if (!"INCONNU".equals(analyse.getSystemeDetecte())
                && !systemeDeclare.equalsIgnoreCase(analyse.getSystemeDetecte())) {
            log.warn("Incohérence système détecté vs déclaré : détecté={}, déclaré={}",
                    analyse.getSystemeDetecte(), systemeDeclare);
        }
        return systemeDeclare.toUpperCase();
    }

    // ==================== CRUD : AJOUTER  ====================

    @Transactional
    public EquipementResponseDTO ajouterEquipement(EquipementRequestDTO dto) {
        CentreDeDonnees centre = centreDeDonneesRepository.findById(dto.getIdCentre())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Centre de données introuvable avec l'id : " + dto.getIdCentre()));

        // Vérifier IP unique
        if (equipementRepository.existsByAdresseIP(dto.getAdresseIP())) {
            Equipement existant = equipementRepository.findByAdresseIP(dto.getAdresseIP()).orElse(null);
            if (existant != null && existant.getCentreDeDonnees() != null) {
                throw new IllegalArgumentException(
                    "L'equipement " + dto.getAdresseIP() + " est deja affecte au centre "
                        + existant.getCentreDeDonnees().getNom() + " (id="
                        + existant.getCentreDeDonnees().getIdCentre() + ").");
            }
            throw new IllegalArgumentException(
                "Un equipement avec cette adresse IP existe deja : " + dto.getAdresseIP());
        }

        // Analyse réseau (informatif, non bloquante)
        AnalyseReseau analyse = analyserEquipement(dto.getAdresseIP());

        // Si injoignable, on loggue mais on autorise quand même (évite le blocage)
        if (!analyse.isJoignable()) {
            log.warn("Équipement {} injoignable lors de l'ajout, mais création autorisée.", dto.getAdresseIP());
        }

        String typeFinal = determinerType(dto.getType(), analyse);
        String systemeFinal = determinerSysteme(dto.getSysteme(), analyse);

        Equipement equipement = new Equipement();
        equipement.setNom(dto.getNom());
        equipement.setType(safeTypeEquipement(typeFinal));
        equipement.setSysteme(safeSystemeExploitation(systemeFinal));
        equipement.setAdresseIP(dto.getAdresseIP());
        equipement.setEtat(analyse.isJoignable() ? "actif" : "injoignable");
        equipement.setCentreDeDonnees(centre);
        equipement.setDateAjout(LocalDate.now());
        equipement.setRamTotaleGb(dto.getRamTotaleGb() != null ? dto.getRamTotaleGb() : 4.0f);

        Equipement sauvegarde = equipementRepository.save(equipement);
        log.info("Équipement ajouté : id={}, type={}, systeme={}, ip={}",
                sauvegarde.getIdEquipement(), typeFinal, systemeFinal, dto.getAdresseIP());

        return toResponseDTO(sauvegarde);
    }

    // ==================== CRUD : LISTER ====================

    @Transactional(readOnly = true)
    public List<EquipementResponseDTO> listerTous() {
        return equipementRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipementResponseDTO trouverParId(Long id) {
        Equipement e = equipementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Équipement introuvable avec l'id : " + id));
        return toResponseDTO(e);
    }

    @Transactional(readOnly = true)
    public List<EquipementResponseDTO> listerParCentre(Long idCentre) {
        return equipementRepository.findByCentreDeDonnees_IdCentre(idCentre).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ==================== CRUD : MODIFIER  ====================

    @Transactional
    public EquipementResponseDTO modifier(Long id, EquipementRequestDTO dto) {
        Equipement existant = equipementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Équipement introuvable avec l'id : " + id));

        if (dto.getNom() != null) {
            existant.setNom(dto.getNom());
        }

        if (dto.getType() != null && !dto.getType().isBlank()) {
            existant.setType(safeTypeEquipement(dto.getType().toUpperCase()));
        }

        if (dto.getSysteme() != null && !dto.getSysteme().isBlank()) {
            existant.setSysteme(safeSystemeExploitation(dto.getSysteme().toUpperCase()));
        }

        if (dto.getRamTotaleGb() != null) {
            existant.setRamTotaleGb(dto.getRamTotaleGb());
        }

        // Si l'IP change
        if (dto.getAdresseIP() != null && !dto.getAdresseIP().equals(existant.getAdresseIP())) {
            if (equipementRepository.existsByAdresseIP(dto.getAdresseIP())) {
                throw new IllegalArgumentException(
                        "Un équipement avec cette adresse IP existe déjà : " + dto.getAdresseIP());
            }

            existant.setAdresseIP(dto.getAdresseIP());
            AnalyseReseau analyse = analyserEquipement(dto.getAdresseIP());

            if (!analyse.isJoignable()) {
                existant.setEtat("injoignable");
            } else {
                if (dto.getType() == null || dto.getType().isBlank()) {
                    existant.setType(safeTypeEquipement(analyse.getTypeDetecte()));
                }
                if (dto.getSysteme() == null || dto.getSysteme().isBlank()) {
                    existant.setSysteme(safeSystemeExploitation(analyse.getSystemeDetecte()));
                }
                existant.setEtat("actif");
            }
        }

        Equipement sauvegarde = equipementRepository.save(existant);
        log.info("Équipement modifié : id={}", sauvegarde.getIdEquipement());
        return toResponseDTO(sauvegarde);
    }

    // ==================== CRUD : SUPPRIMER ====================

    @Transactional
    public void supprimer(Long id) {
        Equipement equipement = equipementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Équipement introuvable avec l'id : " + id));
        equipementRepository.delete(equipement);
        log.info("Équipement supprimé : id={}", id);
    }

    // ==================== UTILITAIRES RÉSEAU  ====================

    private boolean testerPing(String adresseIP) {
        // Essai 1 : Java InetAddress (nécessite root sur Linux)
        try {
            InetAddress adresse = InetAddress.getByName(adresseIP);
            if (adresse.isReachable(TIMEOUT_MS)) {
                return true;
            }
        } catch (Exception e) {
            log.debug("Ping Java échoué pour {} : {}", adresseIP, e.getMessage());
        }

        // Essai 2 : Commande système native (fonctionne sans root)
        return testerPingSysteme(adresseIP);
    }

    private boolean testerPingSysteme(String adresseIP) {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("ping", "-n", "1", "-w", "2000", adresseIP);
        } else {
            pb = new ProcessBuilder("ping", "-c", "1", "-W", "2", adresseIP);
        }
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase();
                    if (lower.contains("ttl=") || lower.contains("time=") || lower.contains("temps=")) {
                        return true;
                    }
                }
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            log.error("Erreur ping système pour {} : {}", adresseIP, e.getMessage());
            return false;
        }
    }

    private boolean testerPort(String adresseIP, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(adresseIP, port), PORT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ==================== SAFE ENUMS   ====================

    private TypeEquipement safeTypeEquipement(String value) {
        try {
            return TypeEquipement.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Type '{}' non reconnu, utilisation de SERVEUR", value);
            return TypeEquipement.SERVEUR;
        }
    }

    private SystemeExploitation safeSystemeExploitation(String value) {
        try {
            return SystemeExploitation.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Système '{}' non reconnu, utilisation de LINUX", value);
            return SystemeExploitation.LINUX;
        }
    }
}