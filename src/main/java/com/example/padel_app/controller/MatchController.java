package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    
    private final MatchService matchService;
    
    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String level) {
        
        List<Match> matches;
        
        // Apply level filter if provided
        if (level != null) {
            try {
                Level levelEnum = Level.valueOf(level.toUpperCase());
                matches = matchService.getMatchesByLevel(levelEnum);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            // Apply sorting strategy (Strategy pattern implementation)
            switch (sort != null ? sort.toLowerCase() : "date") {
                case "popularity":
                    matches = matchService.getMatchesOrderedByPopularity();
                    break;
                case "level":
                    matches = matchService.getMatchesOrderedByLevel();
                    break;
                case "date":
                default:
                    matches = matchService.getMatchesOrderedByDate();
                    break;
            }
        }
        
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        Optional<Match> match = matchService.getMatchById(id);
        return match.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/waiting")
    public ResponseEntity<List<Match>> getWaitingMatches() {
        List<Match> waitingMatches = matchService.getMatchesByStatus(MatchStatus.WAITING);
        return ResponseEntity.ok(waitingMatches);
    }
    
    @GetMapping("/confirmed")
    public ResponseEntity<List<Match>> getConfirmedMatches() {
        List<Match> confirmedMatches = matchService.getMatchesByStatus(MatchStatus.CONFIRMED);
        return ResponseEntity.ok(confirmedMatches);
    }
}