package com.example.demo.repository;

import com.example.demo.entity.CentreDeDonnees;
import com.example.demo.entity.TypeInfrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CentreDeDonneesRepository extends JpaRepository<CentreDeDonnees, Long> {
	List<CentreDeDonnees> findByAdministrateur_IdAdmin(Long idAdmin);
	List<CentreDeDonnees> findByType(TypeInfrastructure type);
	
}