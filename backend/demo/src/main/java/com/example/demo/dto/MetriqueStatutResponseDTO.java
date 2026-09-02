package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetriqueStatutResponseDTO {
    private MetriqueResponseDTO metrique;
    private String status;
    private List<String> typesAnomalie;
    private List<AnomalieInfoDTO> anomalies;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalieInfoDTO {
        private String type;
        private String niveau;
        private String description;
        private String dateDetection;
    }
}
