package com.example.padel_app.repository;

import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    @Query("SELECT f FROM Feedback f JOIN FETCH f.author JOIN FETCH f.targetUser JOIN FETCH f.match WHERE f.author = :author")
    List<Feedback> findByAuthor(User author);
    
    @Query("SELECT f FROM Feedback f JOIN FETCH f.author JOIN FETCH f.targetUser JOIN FETCH f.match WHERE f.targetUser = :targetUser")
    List<Feedback> findByTargetUser(User targetUser);
    
    @Query("SELECT f FROM Feedback f JOIN FETCH f.author JOIN FETCH f.targetUser JOIN FETCH f.match WHERE f.author = :author AND f.match = :match")
    List<Feedback> findByAuthorAndMatch(User author, Match match);
    
    List<Feedback> findByMatch(Match match);
    
    Optional<Feedback> findByAuthorAndTargetUserAndMatch(User author, User targetUser, Match match);
    
    boolean existsByAuthorAndTargetUserAndMatch(User author, User targetUser, Match match);
    
    @Query("SELECT f FROM Feedback f WHERE f.targetUser = :targetUser ORDER BY f.createdAt DESC")
    List<Feedback> findByTargetUserOrderByCreatedAtDesc(User targetUser);
    
    @Query("SELECT AVG(CASE " +
           "WHEN f.suggestedLevel = 'PRINCIPIANTE' THEN 1 " +
           "WHEN f.suggestedLevel = 'INTERMEDIO' THEN 2 " +
           "WHEN f.suggestedLevel = 'AVANZATO' THEN 3 " +
           "WHEN f.suggestedLevel = 'PROFESSIONISTA' THEN 4 " +
           "END) FROM Feedback f WHERE f.targetUser = :targetUser")
    Double getAverageLevelForUser(User targetUser);
}