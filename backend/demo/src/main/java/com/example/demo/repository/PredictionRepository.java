package com.example.demo.repository;

import com.example.demo.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByAnomalie_IdAnomalieOrderByDatePredictionDesc(Long idAnomalie);

}