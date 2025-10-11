package com.example.padel_app.service;

import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.FeedbackRepository;
import com.example.padel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public Feedback createFeedback(User author, User targetUser, Match match, Level suggestedLevel, String comment) {
        // Check if feedback already exists
        Optional<Feedback> existing = feedbackRepository.findByAuthorAndTargetUserAndMatch(author, targetUser, match);
        if (existing.isPresent()) {
            throw new RuntimeException("Feedback already exists for this user and match");
        }
        
        Feedback feedback = new Feedback();
        feedback.setAuthor(author);
        feedback.setTargetUser(targetUser);
        feedback.setMatch(match);
        feedback.setSuggestedLevel(suggestedLevel);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        
        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback created by {} for {} on match {}", 
                 author.getUsername(), targetUser.getUsername(), match.getId());
        
        // Update perceived level
        updatePerceivedLevel(targetUser.getId());
        
        return saved;
    }
    
    @Transactional
    public void updatePerceivedLevel(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found, cannot update perceived level", userId);
            return;
        }
        
        User user = userOpt.get();
        List<Feedback> feedbacks = feedbackRepository.findByTargetUser(user);
        
        if (feedbacks.isEmpty()) {
            log.debug("No feedbacks for user {}, perceived level unchanged", userId);
            return;
        }
        
        // Calculate average level from feedbacks
        double avgLevel = feedbacks.stream()
            .mapToInt(f -> f.getSuggestedLevel().ordinal())
            .average()
            .orElse(0);
        
        int levelIndex = (int) Math.round(avgLevel);
        Level perceivedLevel = Level.values()[levelIndex];
        
        user.setPerceivedLevel(perceivedLevel);
        userRepository.save(user);
        
        log.info("Updated perceived level for user {} to {} (based on {} feedbacks)", 
                 user.getUsername(), perceivedLevel, feedbacks.size());
    }
    
    public List<Feedback> getFeedbacksByTargetUser(User user) {
        return feedbackRepository.findByTargetUser(user);
    }
    
    public List<Feedback> getFeedbacksByAuthor(User user) {
        return feedbackRepository.findByAuthor(user);
    }
    
    public List<Feedback> getFeedbacksByMatch(Match match) {
        return feedbackRepository.findByMatch(match);
    }
    
    public Optional<Feedback> getFeedback(User author, User targetUser, Match match) {
        return feedbackRepository.findByAuthorAndTargetUserAndMatch(author, targetUser, match);
    }
}
