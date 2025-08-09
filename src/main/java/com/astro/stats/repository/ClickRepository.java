package com.astro.stats.repository;

import com.astro.stats.dto.DailyClickCount;
import com.astro.stats.dto.UrlClickCount;
import com.astro.stats.model.Click;
import com.astro.url.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    Optional<Click> findTopByUrlOrderByTimestampDesc(Url url);

    @Query("SELECT new com.astro.stats.dto.DailyClickCount(CAST(c.timestamp AS java.sql.Date), COUNT(c.id)) " +
            "FROM Click c WHERE c.url = :url AND c.timestamp >= :startDate " +
            "GROUP BY CAST(c.timestamp AS java.sql.Date) ORDER BY CAST(c.timestamp AS java.sql.Date) ASC")
    List<DailyClickCount> countClicksByDayForLast7Days(@Param("url") Url url, @Param("startDate") LocalDateTime startDate);

    /**
     * Nuevo método: Agrupa y cuenta todos los clics que ocurrieron después de una marca de tiempo específica.
     * Esto nos permite encontrar solo los "nuevos" clics que necesitan ser agregados al contador total.
     *
     * @param lastProcessedTimestamp La fecha y hora del último clic que fue procesado.
     * @return Una lista de DTOs, cada uno con el ID de la URL, el conteo de nuevos clics,
     *         y la marca de tiempo del clic más reciente en ese lote.
     */
    @Query("SELECT new com.astro.stats.dto.UrlClickCount(c.url.id, COUNT(c), MAX(c.timestamp)) " +
            "FROM Click c " +
            "WHERE c.timestamp > :lastProcessedTimestamp " +
            "GROUP BY c.url.id")
    List<UrlClickCount> getAggregatedClickCountsSince(@Param("lastProcessedTimestamp") LocalDateTime lastProcessedTimestamp);
}