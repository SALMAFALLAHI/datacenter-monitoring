package com.example.demo.repository;

import com.example.demo.entity.Anomalie;
import com.example.demo.entity.StatutAnomalie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalieRepository extends JpaRepository<Anomalie, Long> {

    // ==================== METHODES EXISTANTES ====================

    List<Anomalie> findByMetrique_Equipement_IdEquipementOrderByMetrique_IdMetriqueAscDateDetectionDesc(Long idEquipement);

    List<Anomalie> findAllByOrderByDateDetectionDesc();

    List<Anomalie> findByStatutOrderByDateDetectionDesc(StatutAnomalie statut);

    List<Anomalie> findByStatutInOrderByDateDetectionDesc(List<StatutAnomalie> statuts);

    long countByStatut(StatutAnomalie statut);

    // ==================== REQUETES NATIVES (EXISTANTES) ====================

    @Query(value = """
        SELECT a.* FROM anomalie a
        JOIN metrique m ON a.id_metrique = m.id_metrique
        JOIN equipement e ON m.id_equipement = e.id_equipement
        WHERE e.id_centre = :centreId
        ORDER BY a.date_detection DESC
        """, nativeQuery = true)
    List<Anomalie> findByCentre(@Param("centreId") Long centreId);

    @Query(value = """
        SELECT a.* FROM anomalie a
        JOIN metrique m ON a.id_metrique = m.id_metrique
        JOIN equipement e ON m.id_equipement = e.id_equipement
        WHERE a.statut = :statut
        AND e.id_centre = :centreId
        ORDER BY a.date_detection DESC
        """, nativeQuery = true)
    List<Anomalie> findByStatutAndCentre(
            @Param("statut") String statut,
            @Param("centreId") Long centreId);

    // ==================== NOUVELLES METHODES (VUE GROUPEE) ====================

    /**
     * Recupere les anomalies d'un centre, filtree par statut.
     * Utilise du SQL natif car Equipement n'a pas de relation JPA vers Centre.
     */
    @Query(value = """
        SELECT a.* FROM anomalie a
        JOIN metrique m ON a.id_metrique = m.id_metrique
        JOIN equipement e ON m.id_equipement = e.id_equipement
        WHERE e.id_centre = :centreId
          AND a.statut = :statut
        ORDER BY a.date_detection DESC
        """, nativeQuery = true)
    List<Anomalie> findByCentreAndStatutNative(
            @Param("centreId") Long centreId,
            @Param("statut") String statut);

    /**
     * Recupere les anomalies par IDs (pour traitement par lot).
     * JPQL car pas besoin de filtrer par centre ici.
     */
    @Query("""
        SELECT a FROM Anomalie a
        JOIN FETCH a.metrique m
        JOIN FETCH m.equipement e
        LEFT JOIN FETCH a.decisions
        WHERE a.idAnomalie IN :ids
        """)
    List<Anomalie> findAllByIds(@Param("ids") List<Long> ids);

    /**
     * Compte les anomalies par statut pour un centre (pour KPIs).
     * SQL natif car Equipement n'a pas de relation JPA vers Centre.
     */
    @Query(value = """
        SELECT a.statut, COUNT(*) as cnt
        FROM anomalie a
        JOIN metrique m ON a.id_metrique = m.id_metrique
        JOIN equipement e ON m.id_equipement = e.id_equipement
        WHERE e.id_centre = :centreId
        GROUP BY a.statut
        """, nativeQuery = true)
    List<Object[]> countByStatutForCentreNative(@Param("centreId") Long centreId);




    @Query("""
    SELECT a.statut, COUNT(DISTINCT a.idAnomalie)
    FROM Anomalie a
    JOIN a.metrique m
    JOIN m.equipement e
    WHERE e.centreDeDonnees.idCentre = :centreId
    GROUP BY a.statut
    """)
List<Object[]> countByStatutForCentre(@Param("centreId") Long centreId);

    // ==================== METHODE LEGACY ====================

    List<Anomalie> findByMetrique_Equipement_IdEquipementOrderByDateDetectionDesc(Long idEquipement);
}