package com.example.demo.entity;

public enum DashboardModule {
    ANOMALIES("Anomalies", "anomalies", "alert-triangle"),
    EQUIPEMENTS("Equipements", "equipements", "server"),
    METRIQUES("Métriques", "metriques", "activity"),
    RAPPORTS("Rapports", "rapports", "file-text"),
    CONFIGURATION("Configuration", "configuration", "settings"),
    UTILISATEURS("Utilisateurs", "utilisateurs", "users");

    private final String label;
    private final String route;
    private final String icon;

    DashboardModule(String label, String route, String icon) {
        this.label = label;
        this.route = route;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getRoute() { return route; }
    public String getIcon() { return icon; }
}