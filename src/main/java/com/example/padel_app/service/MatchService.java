package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {
    
    private final MatchRepository matchRepository;
    
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }
    
    public List<Match> getMatchesByStatus(MatchStatus status) {
        return matchRepository.findByStatus(status);
    }
    
    public List<Match> getMatchesByLevel(Level level) {
        return matchRepository.findByRequiredLevel(level);
    }
    
    public List<Match> getMatchesOrderedByDate() {
        return matchRepository.findAllOrderByDate();
    }
    
    public List<Match> getMatchesOrderedByPopularity() {
        return matchRepository.findAllOrderByPopularity();
    }
    
    public List<Match> getMatchesOrderedByLevel() {
        return matchRepository.findAllOrderByLevel();
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
    
    // Business logic for auto-confirming matches when 4 players join
    @Transactional
    public Match checkAndConfirmMatch(Match match) {
        if (match.getActiveRegistrationsCount() >= 4 && match.getStatus() == MatchStatus.WAITING) {
            match.setStatus(MatchStatus.CONFIRMED);
            return matchRepository.save(match);
        }
        return match;
    }
    
    // Business logic for marking matches as finished
    @Transactional
    public void markExpiredMatchesAsFinished() {
        List<Match> expiredMatches = matchRepository.findByDateTimeBefore(LocalDateTime.now());
        for (Match match : expiredMatches) {
            if (match.getStatus() == MatchStatus.CONFIRMED) {
                match.setStatus(MatchStatus.FINISHED);
                matchRepository.save(match);
            }
        }
    }
}