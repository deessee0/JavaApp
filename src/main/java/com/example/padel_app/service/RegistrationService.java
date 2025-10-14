package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.RegistrationStatus;
import com.example.padel_app.repository.RegistrationRepository;
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
public class RegistrationService {
    
    private final RegistrationRepository registrationRepository;
    private final MatchService matchService;
    
    public List<Registration> getRegistrationsByUser(User user) {
        return registrationRepository.findByUser(user);
    }
    
    public List<Registration> getRegistrationsByMatch(Match match) {
        return registrationRepository.findByMatch(match);
    }
    
    public List<Registration> getActiveRegistrationsByMatch(Match match) {
        return registrationRepository.findByMatchAndStatus(match, RegistrationStatus.JOINED);
    }
    
    public boolean isUserRegisteredForMatch(User user, Match match) {
        return registrationRepository.existsByUserAndMatchAndStatus(user, match, RegistrationStatus.JOINED);
    }
    
    public boolean isUserRegistered(User user, Match match) {
        return registrationRepository.existsByUserAndMatchAndStatus(user, match, RegistrationStatus.JOINED);
    }
    
    public List<Registration> getActiveRegistrationsByUser(User user) {
        return registrationRepository.findByUserAndStatus(user, RegistrationStatus.JOINED);
    }
    
    // Business logic: Join match with constraints
    @Transactional
    public Registration joinMatch(User user, Match match) {
        // Check if already registered
        if (isUserRegisteredForMatch(user, match)) {
            throw new IllegalStateException("User already registered for this match");
        }
        
        // Check match capacity (max 4 players)
        int currentPlayers = registrationRepository.countActiveRegistrationsByMatch(match);
        if (currentPlayers >= 4) {
            throw new IllegalStateException("Match is full - maximum 4 players allowed");
        }
        
        // Create registration
        Registration registration = new Registration();
        registration.setUser(user);
        registration.setMatch(match);
        registration.setStatus(RegistrationStatus.JOINED);
        registration.setRegisteredAt(LocalDateTime.now());
        
        Registration savedRegistration = registrationRepository.save(registration);
        log.info("User {} joined match {} ({}/{} players)", 
                 user.getUsername(), match.getId(), currentPlayers + 1, 4);
        
        // Check if match should be auto-confirmed (4 players)
        matchService.checkAndConfirmMatch(match);
        
        return savedRegistration;
    }
    
    // Business logic: Leave match
    @Transactional
    public void leaveMatch(User user, Match match) {
        Optional<Registration> registrationOpt = registrationRepository.findByUserAndMatch(user, match);
        
        if (registrationOpt.isEmpty()) {
            throw new IllegalStateException("User is not registered for this match");
        }
        
        Registration registration = registrationOpt.get();
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new IllegalStateException("User already left this match");
        }
        
        // Update status to cancelled
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        
        log.info("User {} left match {} ({}/{} players remaining)", 
                 user.getUsername(), match.getId(), 
                 registrationRepository.countActiveRegistrationsByMatch(match), 4);
    }
    
    @Transactional
    public Registration saveRegistration(Registration registration) {
        return registrationRepository.save(registration);
    }
    
    @Transactional
    public void deleteRegistration(Long id) {
        registrationRepository.deleteById(id);
    }
}