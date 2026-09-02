package com.example.demo.repository;

import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetriqueRepository extends JpaRepository<Metrique, Long> {

    List<Metrique> findByEquipement_IdEquipementOrderByDateCollecteDesc(Long idEquipement);
    List<Metrique> findTop5ByEquipementOrderByDateCollecteDesc(Equipement equipement);
    List<Metrique> findTop10ByEquipementOrderByDateCollecteDesc(Equipement equipement);
    List<Metrique> findTop3ByEquipementOrderByDateCollecteDesc(Equipement equipement);
    List<Metrique> findByDateCollecteAfterOrderByDateCollecteAsc(LocalDateTime since);

    // Dernière métrique par équipement (toutes dates)
    @Query(value = """
        SELECT m.* FROM metrique m
        INNER JOIN (
            SELECT id_equipement, MAX(date_collecte) as max_date
            FROM metrique
            GROUP BY id_equipement
        ) latest ON m.id_equipement = latest.id_equipement
        AND m.date_collecte = latest.max_date
        """, nativeQuery = true)
    List<Metrique> findAllLatest();

    // Dernière métrique par équipement filtrée par centre
    @Query(value = """
        SELECT m.* FROM metrique m
        INNER JOIN (
            SELECT met.id_equipement, MAX(met.date_collecte) as max_date
            FROM metrique met
            JOIN equipement eq ON met.id_equipement = eq.id_equipement
            WHERE eq.id_centre = :centreId
            GROUP BY met.id_equipement
        ) latest ON m.id_equipement = latest.id_equipement
        AND m.date_collecte = latest.max_date
        """, nativeQuery = true)
    List<Metrique> findLatestByCentre(@Param("centreId") Long centreId);

    // Historique global
    @Query(value = """
        SELECT 
            DATE_TRUNC('minute', m.date_collecte) as date_collecte,
            COALESCE(AVG(m.cpu), 0) as cpu_moyen,
            COALESCE(AVG(m.ram_pct), 0) as ram_pct_moyen,
            COALESCE(AVG(m.reseau), 0) as reseau_moyen,
            COALESCE(AVG(m.disque), 0) as disk_pct_moyen,
            COUNT(DISTINCT m.id_equipement) as nombre_equipements
        FROM metrique m
        WHERE m.date_collecte >= :since
        GROUP BY DATE_TRUNC('minute', m.date_collecte)
        ORDER BY date_collecte ASC
        """, nativeQuery = true)
    List<Object[]> getHistoriqueGlobal(@Param("since") LocalDateTime since);

    // Historique par centre
    @Query(value = """
        SELECT 
            DATE_TRUNC('minute', m.date_collecte) as date_collecte,
            COALESCE(AVG(m.cpu), 0) as cpu_moyen,
            COALESCE(AVG(m.ram_pct), 0) as ram_pct_moyen,
            COALESCE(AVG(m.reseau), 0) as reseau_moyen,
            COALESCE(AVG(m.disque), 0) as disk_pct_moyen,
            COUNT(DISTINCT m.id_equipement) as nombre_equipements
        FROM metrique m
        JOIN equipement eq ON m.id_equipement = eq.id_equipement
        WHERE m.date_collecte >= :since
        AND eq.id_centre = :centreId
        GROUP BY DATE_TRUNC('minute', m.date_collecte)
        ORDER BY date_collecte ASC
        """, nativeQuery = true)
    List<Object[]> getHistoriqueByCentre(@Param("centreId") Long centreId, @Param("since") LocalDateTime since);
}