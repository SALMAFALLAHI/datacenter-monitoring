package com.example.demo.dto;

import com.example.demo.entity.DashboardModule;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserAccessRequest {
    private Set<DashboardModule> dashboardAccess;
    private Boolean actif;
}