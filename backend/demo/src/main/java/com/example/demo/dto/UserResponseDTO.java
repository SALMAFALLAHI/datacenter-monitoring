package com.example.demo.dto;

import com.example.demo.entity.DashboardModule;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserResponseDTO {
    private Long idAdmin;
    private String email;
    private String nom;
    private String prenom;
    private String fullName;
    private String role;
    private Set<DashboardModule> dashboardAccess;
    private boolean actif;
    private String createdByName;
    private List<Long> centreIds;
    private List<String> centreNoms;
}