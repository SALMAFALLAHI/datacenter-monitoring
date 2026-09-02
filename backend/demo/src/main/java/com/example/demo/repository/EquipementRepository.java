package com.example.demo.repository;

import com.example.demo.entity.Equipement;
import com.example.demo.entity.TypeEquipement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipementRepository extends JpaRepository<Equipement, Long> {

    List<Equipement> findByCentreDeDonnees_IdCentre(Long idCentre);

    @Query("SELECT e FROM Equipement e WHERE e.Type = :type AND e.centreDeDonnees.idCentre = :idCentre")
    List<Equipement> findByTypeAndCentreDeDonnees_IdCentre(
            @Param("type") TypeEquipement type,
            @Param("idCentre") Long idCentre);

    boolean existsByAdresseIP(String adresseIP);

    Optional<Equipement> findByAdresseIP(String adresseIP);
}