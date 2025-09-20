package com.example.padel_app.repository;

import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByDeclaredLevel(Level declaredLevel);
    
    @Query("SELECT u FROM User u WHERE u.perceivedLevel = :level")
    List<User> findByPerceivedLevel(Level level);
    
    @Query("SELECT u FROM User u ORDER BY u.matchesPlayed DESC")
    List<User> findAllOrderByMatchesPlayedDesc();
}