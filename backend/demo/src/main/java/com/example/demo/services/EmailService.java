package com.example.demo.services;

import com.example.demo.entity.Anomalie;
import com.example.demo.entity.AnomalieType;
import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import com.example.demo.repository.AdministrateurRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AdministrateurRepository administrateurRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.alert.email.subject:Nouvelair Supervision - Alerte Detecte}")
    private String alertSubject;

    public void envoyerAlerteStress(Anomalie anomalie, Metrique metrique, Equipement equipement,
                                    String emailUtilisateurConnecte) {
        try {
            String sujet = alertSubject + " [" + anomalie.getType().getLabel() + "] - " + equipement.getNom();
            String contenu = buildEmailContent(anomalie, metrique, equipement);

            if (emailUtilisateurConnecte != null && !emailUtilisateurConnecte.isBlank()) {
                sendEmail(emailUtilisateurConnecte, sujet, contenu);
                log.info("📧 Email d'alerte envoye a l'utilisateur connecte: {}", emailUtilisateurConnecte);
            }

            List<com.example.demo.entity.Administrateur> admins = administrateurRepository.findAll();
            for (com.example.demo.entity.Administrateur admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    sendEmail(admin.getEmail(), sujet, contenu);
                    log.info("📧 Email d'alerte envoye a l'admin: {}", admin.getEmail());
                }
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email d'alerte: {}", e.getMessage(), e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String buildEmailContent(Anomalie anomalie, Metrique metrique, Equipement equipement) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dateDetection = anomalie.getDateDetection() != null
                ? anomalie.getDateDetection().format(formatter)
                : "N/A";

        String couleurNiveau = switch (anomalie.getNiveau()) {
            case "CRITIQUE" -> "#dc2626";
            case "HAUTE" -> "#ea580c";
            case "SUSPECT" -> "#ca8a04";
            case "MAJEUR" -> "#ea580c";
            case "MINEUR" -> "#ca8a04";
            default -> "#6b7280";
        };

        // Icône selon le type
        String icone = switch (anomalie.getType()) {
            case RAM -> "🧠";
            case CPU -> "⚡";
            case DISK -> "💾";
            case NETWORK -> "🌐";
        };

        double ramTotale = metrique.getUsedGb() + metrique.getAvailableGb();

        // Section metrique specifique selon le type
        String sectionDetail = buildDetailSection(anomalie.getType(), metrique);

        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'><style>" +
            "body{font-family:Arial,sans-serif;background:#f3f4f6;margin:0;padding:20px;}" +
            ".container{max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1);}" +
            ".header{background:linear-gradient(135deg,#1e3a5f 0%,#3b82f6 100%);color:white;padding:24px;text-align:center;}" +
            ".header h1{margin:0;font-size:22px;}" +
            ".type-badge{display:inline-block;background:#0ea5e9;color:white;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold;margin-bottom:8px;}" +
            ".alert-badge{display:inline-block;background:" + couleurNiveau + ";color:white;padding:6px 16px;border-radius:20px;font-weight:bold;font-size:14px;margin-top:10px;}" +
            ".content{padding:24px;}" +
            ".metric-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:16px 0;}" +
            ".metric-card{background:#f8fafc;border-left:4px solid #3b82f6;padding:12px;border-radius:6px;}" +
            ".metric-label{font-size:12px;color:#64748b;text-transform:uppercase;}" +
            ".metric-value{font-size:18px;font-weight:bold;color:#1e293b;}" +
            ".warning{background:#fef3c7;border-left:4px solid #f59e0b;padding:12px;border-radius:6px;margin-top:16px;}" +
            ".detail-box{background:#e0f2fe;border-left:4px solid #0ea5e9;padding:12px;border-radius:6px;margin-top:16px;}" +
            ".footer{background:#f8fafc;padding:16px;text-align:center;font-size:12px;color:#94a3b8;}" +
            "</style></head><body>" +
            "<div class='container'>" +
            "<div class='header'>" +
            "<div class='type-badge'>" + icone + " " + anomalie.getType().getLabel().toUpperCase() + "</div>" +
            "<h1>🚨 Alerte Detectee</h1>" +
            "<div class='alert-badge'>NIVEAU: " + anomalie.getNiveau() + "</div></div>" +
            "<div class='content'>" +
            "<p><strong>Equipement:</strong> " + equipement.getNom() + "</p>" +
            "<p><strong>Adresse IP:</strong> " + equipement.getAdresseIP() + "</p>" +
            "<p><strong>Date de detection:</strong> " + dateDetection + "</p>" +
            "<p><strong>Score de confiance:</strong> " + String.format("%.2f", anomalie.getScore() * 100) + "%</p>" +

            sectionDetail +

            "<h3 style='color:#1e3a5f;border-bottom:2px solid #e2e8f0;padding-bottom:8px;'>📊 Vue globale des metriques</h3>" +
            "<div class='metric-grid'>" +
            "<div class='metric-card'><div class='metric-label'>CPU</div><div class='metric-value'>" + String.format("%.2f", metrique.getCpu()) + "%</div></div>" +
            "<div class='metric-card'><div class='metric-label'>RAM Utilisee</div><div class='metric-value'>" + String.format("%.2f", metrique.getRamPct()) + "%</div></div>" +
            "<div class='metric-card'><div class='metric-label'>RAM (GB)</div><div class='metric-value'>" + String.format("%.2f", metrique.getUsedGb()) + " / " + String.format("%.2f", ramTotale) + " GB</div></div>" +
            "<div class='metric-card'><div class='metric-label'>Disque</div><div class='metric-value'>" + String.format("%.2f", metrique.getDisque()) + "%</div></div>" +
            "<div class='metric-card'><div class='metric-label'>Swap</div><div class='metric-value'>" + String.format("%.2f", metrique.getSwapPct()) + "%</div></div>" +
            "<div class='metric-card'><div class='metric-label'>Reseau</div><div class='metric-value'>" + String.format("%.4f", metrique.getReseau()) + " MB/s</div></div>" +
            "</div>" +

            "<div class='warning'><strong>⚠️ Description:</strong> " + (anomalie.getDescription() != null ? anomalie.getDescription() : "Anomalie detectee") + "</div>" +

            "<div class='detail-box'><strong>🔮 Prochainement:</strong><br>Analyse de la cause probable et recommandations de correction seront ajoutees ici automatiquement.</div>" +
            "</div>" +
            "<div class='footer'><p>Nouvelair Supervision - Systeme de detection automatique</p><p>Cet email a ete genere automatiquement. Ne pas repondre.</p></div>" +
            "</div></body></html>";
    }

    private String buildDetailSection(AnomalieType type, Metrique metrique) {
        return switch (type) {
            case RAM -> "<div class='detail-box'><strong>🧠 Detail RAM:</strong><br>" +
                    "Utilisation: " + String.format("%.2f", metrique.getRamPct()) + "%<br>" +
                    "Utilisee: " + String.format("%.2f", metrique.getUsedGb()) + " GB<br>" +
                    "Disponible: " + String.format("%.2f", metrique.getAvailableGb()) + " GB<br>" +
                    "Swap: " + String.format("%.2f", metrique.getSwapPct()) + "%</div>";

            case CPU -> "<div class='detail-box'><strong>⚡ Detail CPU:</strong><br>" +
                    "Utilisation actuelle: " + String.format("%.2f", metrique.getCpu()) + "%<br>" +
                    "Alerte declenchee si surcharge soutenue > 85% sur 2.5 min</div>";

            case DISK -> "<div class='detail-box'><strong>💾 Detail Disque:</strong><br>" +
                    "Utilisation: " + String.format("%.2f", metrique.getDisque()) + "%<br>" +
                    "Alerte critique si > 95%, alerte haute si > 90%</div>";

            case NETWORK -> "<div class='detail-box'><strong>🌐 Detail Reseau:</strong><br>" +
                    "Traffic entrant: " + String.format("%.4f", metrique.getReseau()) + " MB/s<br>" +
                    "Alerte si traffic nul pendant 1.5 min consecutives</div>";
        };
    }
}