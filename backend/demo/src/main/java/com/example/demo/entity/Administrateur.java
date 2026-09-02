package com.example.demo.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Administrateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAdmin;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = true)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Administrateur createdBy;

    @ElementCollection(targetClass = DashboardModule.class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "administrateur_dashboard_access",
        joinColumns = @JoinColumn(name = "administrateur_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "module")
    private Set<DashboardModule> dashboardAccess = new HashSet<>();

    // ===== PAS DE CASCADE =====
    @OneToMany(mappedBy = "administrateur")
    private List<CentreDeDonnees> centres = new ArrayList<>();

    public String getFullName() {
        if (prenom != null && !prenom.isBlank()) {
            return prenom + " " + nom;
        }
        return nom;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}