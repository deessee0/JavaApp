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
import java.util.Optional;

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
    
    // Query con JOIN FETCH per evitare LazyInitializationException
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations")
    List<Match> findAllWithCreator();
    
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations WHERE m.status = ?1")
    List<Match> findByStatusWithCreator(MatchStatus status);
    
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations WHERE m.requiredLevel = ?1") 
    List<Match> findByRequiredLevelWithCreator(Level level);
    
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations ORDER BY m.dateTime ASC")
    List<Match> findAllOrderByDateWithCreator();
    
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations ORDER BY SIZE(m.registrations) DESC")
    List<Match> findAllOrderByPopularityWithCreator();
    
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations ORDER BY m.requiredLevel ASC")
    List<Match> findAllOrderByLevelWithCreator();
}