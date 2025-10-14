package com.example.padel_app.repository;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    
    List<Registration> findByUser(User user);
    
    List<Registration> findByMatch(Match match);
    
    List<Registration> findByStatus(RegistrationStatus status);
    
    Optional<Registration> findByUserAndMatch(User user, Match match);
    
    boolean existsByUserAndMatch(User user, Match match);
    
    boolean existsByUserAndMatchAndStatus(User user, Match match, RegistrationStatus status);
    
    @Query("SELECT r FROM Registration r JOIN FETCH r.user WHERE r.match = :match AND r.status = :status")
    List<Registration> findByMatchAndStatus(Match match, RegistrationStatus status);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.match = :match AND r.status = 'JOINED'")
    int countActiveRegistrationsByMatch(Match match);
    
    List<Registration> findByUserAndStatus(User user, RegistrationStatus status);
}