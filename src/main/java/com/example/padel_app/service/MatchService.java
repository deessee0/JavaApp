package com.example.padel_app.service;

import com.example.padel_app.event.MatchConfirmedEvent;
import com.example.padel_app.event.MatchFinishedEvent;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.strategy.MatchSortingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MatchService {
    
    private final MatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, MatchSortingStrategy> sortingStrategies;
    
    public List<Match> getAllMatches() {
        return matchRepository.findAllWithCreator();
    }
    
    public List<Match> getMatchesByStatus(MatchStatus status) {
        return matchRepository.findByStatusWithCreator(status);
    }
    
    public List<Match> getMatchesByLevel(Level level) {
        return matchRepository.findByRequiredLevelWithCreator(level);
    }
    
    // Strategy Pattern Implementation
    public List<Match> getMatchesOrderedBy(String strategy) {
        String strategyKey = strategy + "Sorting";
        MatchSortingStrategy sortingStrategy = sortingStrategies.get(strategyKey);
        
        if (sortingStrategy != null) {
            List<Match> allMatches = matchRepository.findAllWithCreator();
            log.debug("Using {} strategy to sort {} matches", sortingStrategy.getStrategyName(), allMatches.size());
            return sortingStrategy.sort(allMatches);
        }
        
        // Safe fallback - direct repository call to avoid recursion
        log.warn("Strategy {} not found, using date sorting as fallback", strategy);
        return matchRepository.findAllOrderByDateWithCreator();
    }
    
    public List<Match> getMatchesOrderedByDate() {
        return getMatchesOrderedBy("date");
    }
    
    public List<Match> getMatchesOrderedByPopularity() {
        return getMatchesOrderedBy("popularity");
    }
    
    public List<Match> getMatchesOrderedByLevel() {
        return getMatchesOrderedBy("level");
    }
    
    public Optional<Match> getMatchById(Long id) {
        return matchRepository.findById(id);
    }
    
    @Transactional
    public Match saveMatch(Match match) {
        return matchRepository.save(match);
    }
    
    @Transactional
    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }
    
    // Business logic for auto-confirming matches when 4 players join (Observer Pattern)
    @Transactional
    public Match checkAndConfirmMatch(Match match) {
        if (match.getActiveRegistrationsCount() >= 4 && match.getStatus() == MatchStatus.WAITING) {
            MatchStatus oldStatus = match.getStatus();
            match.setStatus(MatchStatus.CONFIRMED);
            Match savedMatch = matchRepository.save(match);
            
            // Publish Observer event
            log.info("🎯 Publishing MatchConfirmedEvent for match ID: {}", savedMatch.getId());
            eventPublisher.publishEvent(new MatchConfirmedEvent(this, savedMatch));
            
            return savedMatch;
        }
        return match;
    }
    
    // Business logic for marking matches as finished (Observer Pattern)
    @Transactional
    public void markExpiredMatchesAsFinished() {
        List<Match> expiredMatches = matchRepository.findByDateTimeBefore(LocalDateTime.now());
        for (Match match : expiredMatches) {
            if (match.getStatus() == MatchStatus.CONFIRMED) {
                MatchStatus oldStatus = match.getStatus();
                match.setStatus(MatchStatus.FINISHED);
                Match savedMatch = matchRepository.save(match);
                
                // Publish Observer event
                log.info("🏁 Publishing MatchFinishedEvent for match ID: {}", savedMatch.getId());
                eventPublisher.publishEvent(new MatchFinishedEvent(this, savedMatch));
            }
        }
    }
    
    // Manual finish match method for testing
    @Transactional
    public Match finishMatch(Long matchId) {
        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            if (match.getStatus() == MatchStatus.CONFIRMED) {
                match.setStatus(MatchStatus.FINISHED);
                Match savedMatch = matchRepository.save(match);
                
                // Publish Observer event
                log.info("🏁 Manual finish - Publishing MatchFinishedEvent for match ID: {}", savedMatch.getId());
                eventPublisher.publishEvent(new MatchFinishedEvent(this, savedMatch));
                
                return savedMatch;
            }
        }
        throw new IllegalArgumentException("Match not found or cannot be finished");
    }
}