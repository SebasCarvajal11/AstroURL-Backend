package com.astro.url.repository;

import com.astro.url.model.Url;
import com.astro.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findBySlug(String slug);
    Page<Url> findByExpirationDateBefore(LocalDateTime dateTime, Pageable pageable);
    Page<Url> findByUser(User user, Pageable pageable);
    List<Url> findByUserAndExpirationDateAfter(User user, LocalDateTime dateTime);

    /**
     * Nuevo método: Actualiza el contador de clics y la fecha del último acceso para una URL específica.
     * Se usa @Modifying para indicar que es una consulta de actualización.
     *
     * @param urlId El ID de la URL a actualizar.
     * @param clickIncrement El número de nuevos clics a sumar al contador existente.
     * @param lastAccessedAt La nueva fecha del último acceso.
     */
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + :clickIncrement, u.lastAccessedAt = :lastAccessedAt WHERE u.id = :urlId")
    void incrementClickCountAndSetLastAccessed(@Param("urlId") Long urlId, @Param("clickIncrement") long clickIncrement, @Param("lastAccessedAt") LocalDateTime lastAccessedAt);
}