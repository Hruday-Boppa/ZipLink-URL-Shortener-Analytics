package com.urlshortener.repository;

import java.time.Instant;
import java.util.Optional;

import com.urlshortener.domain.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findTopByOriginalUrlHashOrderByCreatedAtDesc(String originalUrlHash);

    boolean existsByShortCode(String shortCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UrlMapping u
           set u.clickCount = u.clickCount + 1,
               u.updatedAt = :updatedAt
         where u.shortCode = :shortCode
    """)
    int incrementClickCount(@Param("shortCode") String shortCode, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("delete from UrlMapping u where u.expiresAt is not null and u.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") Instant now);
}
