package com.example.padel_app.repository;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    List<Match> findByStatus(MatchStatus status);
    
    List<Match> findByType(MatchType type);
    
    List<Match> findByRequiredLevel(Level requiredLevel);
    
    @Query("SELECT m FROM Match m ORDER BY m.dateTime ASC")
    List<Match> findAllOrderByDate();
    
    @Query("SELECT m FROM Match m ORDER BY SIZE(m.registrations) DESC")
    List<Match> findAllOrderByPopularity();
    
    @Query("SELECT m FROM Match m ORDER BY m.requiredLevel ASC")
    List<Match> findAllOrderByLevel();
    
    List<Match> findByDateTimeBefore(LocalDateTime dateTime);
    
    @Query("SELECT m FROM Match m WHERE m.status = :status AND m.dateTime > :dateTime")
    List<Match> findByStatusAndDateTimeAfter(MatchStatus status, LocalDateTime dateTime);
    
    @Query("SELECT m FROM Match m WHERE m.requiredLevel = :level AND m.status = :status")
    List<Match> findByRequiredLevelAndStatus(Level level, MatchStatus status);
}