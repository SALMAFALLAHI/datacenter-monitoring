package com.example.demo.services;

import com.example.demo.dto.EquipementRequestDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.CentreDeDonneesRepository;
import com.example.demo.repository.EquipementRepository;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.*;
import java.util.*;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DecouverteReseauService {

    private static final Logger log = LoggerFactory.getLogger(DecouverteReseauService.class);
    private static final int TIMEOUT_MS = 2000;

    private final EquipementRepository equipementRepository;
    private final CentreDeDonneesRepository centreRepository;
    private final EquipementService equipementService;

    // ==================== CLASSES DE RÉSULTAT ====================

    @Getter @Setter @NoArgsConstructor
    public static class ResultatScan {
        private List<EquipementDetecte> detectes = new ArrayList<>();
        private List<EquipementAjoute> ajoutesAuto = new ArrayList<>();
        private List<String> ignores = new ArrayList<>();
        private int totalScanne;
        private int totalJoignables;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EquipementDetecte {
        private String adresseIP;
        private String nomPropose;
        private String typeDetecte;
        private String systemeDetecte;
        private boolean joignable;
        private List<Integer> portsOuverts;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EquipementAjoute {
        private Long idEquipement;
        private String adresseIP;
        private String nom;
        private TypeEquipement type;
        private SystemeExploitation systeme;
    }

    // ==================== SCAN RÉSEAU ====================

    /**
     * Scan une plage d'IP et retourne les équipements détectés
     * Sans les ajouter - juste pour information
     */
    public ResultatScan scannerPlage(String plageDebut, String plageFin) {
        ResultatScan resultat = new ResultatScan();

        try {
            long debut = ipToLong(plageDebut);
            long fin = ipToLong(plageFin);
            resultat.setTotalScanne((int) (fin - debut + 1));

            log.info("Démarrage scan : {} → {} ({} IPs)", plageDebut, plageFin, resultat.getTotalScanne());

            for (long ip = debut; ip <= fin; ip++) {
                String adresseIP = longToIp(ip);

                // Ignorer si déjà en base
                if (equipementRepository.existsByAdresseIP(adresseIP)) {
                    Equipement existant = equipementRepository.findByAdresseIP(adresseIP).orElse(null);
                    if (existant != null && existant.getCentreDeDonnees() != null) {
                        resultat.getIgnores().add(adresseIP + " (deja affecte au centre "
                                + existant.getCentreDeDonnees().getNom() + " - id="
                                + existant.getCentreDeDonnees().getIdCentre() + ")");
                    } else {
                        resultat.getIgnores().add(adresseIP + " (deja affecte)");
                    }
                    continue;
                }

                // On analyse l'IP puis on ne retient que les hôtes réellement joignables
                // (port connu ouvert ou ping confirmé).
                EquipementDetecte detecte = analyserIP(adresseIP);
                // Pour un scan de plage, on considère "réel" uniquement si au moins
                // un port d'intérêt est ouvert. Le ping seul est trop permissif.
                if (!detecte.getPortsOuverts().isEmpty()) {
                    resultat.setTotalJoignables(resultat.getTotalJoignables() + 1);
                    resultat.getDetectes().add(detecte);
                    log.info("Détecté : {} → type={}, systeme={}", 
                            adresseIP, detecte.getTypeDetecte(), detecte.getSystemeDetecte());
                } else {
                    resultat.getIgnores().add(adresseIP + " (aucun port d'interet ouvert)");
                }
            }

            log.info("Scan terminé : {} détectés, {} ignorés (déjà en base)", 
                    resultat.getDetectes().size(), resultat.getIgnores().size());

        } catch (Exception e) {
            log.error("Erreur scan : {}", e.getMessage());
        }

        return resultat;
    }

    /**
     * Scan ET ajoute automatiquement les équipements reconnus
     */
    @Transactional
    public ResultatScan scannerEtAjouter(String plageDebut, String plageFin, Long idCentre) {
        ResultatScan resultat = scannerPlage(plageDebut, plageFin);

        // Vérifier centre existe
        CentreDeDonnees centre = centreRepository.findById(idCentre)
                .orElseThrow(() -> new IllegalArgumentException("Centre introuvable : " + idCentre));

        for (EquipementDetecte detecte : resultat.getDetectes()) {
            try {
                // Créer DTO pour ajout
                EquipementRequestDTO dto = new EquipementRequestDTO();
                dto.setNom(detecte.getNomPropose());
                dto.setType(normaliserTypePourEnum(detecte.getTypeDetecte()));
                dto.setSysteme(normaliserSystemePourEnum(detecte.getSystemeDetecte()));
                dto.setAdresseIP(detecte.getAdresseIP());
                dto.setIdCentre(idCentre);

                // Ajouter via le service (qui vérifie cohérence réseau)
                var response = equipementService.ajouterEquipement(dto);

                resultat.getAjoutesAuto().add(new EquipementAjoute(
                        response.getIdEquipement(),
                        response.getAdresseIP(),
                        response.getNom(),
                        response.getType(),
                        response.getSysteme()
                ));

                log.info("Auto-ajouté : {} → id={}", detecte.getAdresseIP(), response.getIdEquipement());

            } catch (Exception e) {
                log.warn("Impossible d'ajouter {} : {}", detecte.getAdresseIP(), e.getMessage());
                resultat.getIgnores().add(detecte.getAdresseIP() + " (echec auto-ajout: " + e.getMessage() + ")");
            }
        }

        // On conserve detectes pour diagnostic: ce qui est trouvé n'est pas
        // forcément auto-ajoutable selon les règles métier.

        return resultat;
    }

    // ==================== ANALYSE D'UNE IP ====================

    private EquipementDetecte analyserIP(String adresseIP) {
        EquipementDetecte eq = new EquipementDetecte();
        eq.setAdresseIP(adresseIP);
        eq.setPortsOuverts(new ArrayList<>());

        // Tester les ports
        boolean p22 = testerPort(adresseIP, 22);
        boolean p3389 = testerPort(adresseIP, 3389);
        boolean p80 = testerPort(adresseIP, 80);
        boolean p443 = testerPort(adresseIP, 443);
        boolean p9100 = testerPort(adresseIP, 9100);
        boolean p161 = testerPort(adresseIP, 161);
        boolean p445 = testerPort(adresseIP, 445);
        boolean p3306 = testerPort(adresseIP, 3306);
        boolean p5432 = testerPort(adresseIP, 5432);

        if (p22) eq.getPortsOuverts().add(22);
        if (p3389) eq.getPortsOuverts().add(3389);
        if (p80) eq.getPortsOuverts().add(80);
        if (p443) eq.getPortsOuverts().add(443);
        if (p9100) eq.getPortsOuverts().add(9100);
        if (p161) eq.getPortsOuverts().add(161);
        if (p445) eq.getPortsOuverts().add(445);
        if (p3306) eq.getPortsOuverts().add(3306);
        if (p5432) eq.getPortsOuverts().add(5432);

        boolean pingOk = testerPing(adresseIP);

        // Détection système
        if (p3389) {
            eq.setSystemeDetecte("WINDOWS");
        } else if (p22) {
            eq.setSystemeDetecte("LINUX");
        } else {
            eq.setSystemeDetecte("INCONNU");
        }

        // Détection type
        boolean profilServeur = p80 || p443 || p22 || p3389 || p3306 || p5432;

        // 9100 seul est un bon indicateur d'imprimante, mais 9100 + ports serveur
        // correspond souvent à une VM/serveur avec service d'impression.
        if (p9100 && !profilServeur) {
            eq.setTypeDetecte("IMPRIMANTE");
            eq.setNomPropose("Imprimante-" + dernierOctet(adresseIP));
        } else if (p161 && !p80 && !p22 && !p3389) {
            eq.setTypeDetecte("ROUTEUR");
            eq.setNomPropose("Routeur-" + dernierOctet(adresseIP));
        } else if (profilServeur) {
            eq.setTypeDetecte("SERVEUR");
            eq.setNomPropose("Serveur-" + dernierOctet(adresseIP));
        } else if (p445) {
            eq.setTypeDetecte("WORKSTATION");
            eq.setNomPropose("Poste-" + dernierOctet(adresseIP));
        } else {
            eq.setTypeDetecte("INCONNU");
            eq.setNomPropose("Equipement-" + dernierOctet(adresseIP));
        }

        boolean joignableParPort = !eq.getPortsOuverts().isEmpty();
        eq.setJoignable(joignableParPort || pingOk);
        return eq;
    }

    // ==================== UTILITAIRES ====================

    private boolean testerPing(String adresseIP) {
        try {
            InetAddress inet = InetAddress.getByName(adresseIP);
            boolean javaPing = inet.isReachable(TIMEOUT_MS);
            if (javaPing) {
                return true;
            }

            // Fallback Windows pour limiter les faux resultats de isReachable.
            ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", "-w", String.valueOf(TIMEOUT_MS), adresseIP);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String normaliserTypePourEnum(String typeDetecte) {
        if (typeDetecte == null || typeDetecte.isBlank()) {
            return "AUTRE";
        }

        return switch (typeDetecte.toUpperCase()) {
            case "SERVEUR", "ROUTEUR", "SWITCH", "IMPRIMANTE", "AUTRE" -> typeDetecte.toUpperCase();
            case "WORKSTATION", "INCONNU" -> "AUTRE";
            default -> "AUTRE";
        };
    }

    private String normaliserSystemePourEnum(String systemeDetecte) {
        if (systemeDetecte == null || systemeDetecte.isBlank()) {
            return "AUTRE";
        }

        return switch (systemeDetecte.toUpperCase()) {
            case "WINDOWS", "LINUX", "MACOS", "AUTRE" -> systemeDetecte.toUpperCase();
            case "INCONNU" -> "AUTRE";
            default -> "AUTRE";
        };
    }

    private boolean testerPort(String adresseIP, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(adresseIP, port), TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String dernierOctet(String ip) {
        return ip.substring(ip.lastIndexOf('.') + 1);
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(octets[i]);
        }
        return result;
    }

    private String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }
}