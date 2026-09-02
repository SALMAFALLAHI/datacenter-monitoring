package com.example.demo.repository;
import com.example.demo.entity.Alerte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alerte, Long> {
    
}
