package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HomeController {
    
    private final MatchService matchService;
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "🎾 Benvenuto nell'App Padel!");
        response.put("description", "Sistema di gestione partite padel - Progetto Università");
        response.put("status", "Sistema attivo e funzionante");
        response.put("endpoints", List.of(
            "/api/matches - Lista partite",
            "/api/matches/waiting - Partite in attesa",
            "/h2-console - Console database H2"
        ));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        List<Match> allMatches = matchService.getAllMatches();
        List<Match> waitingMatches = matchService.getMatchesByStatus(MatchStatus.WAITING);
        List<Match> confirmedMatches = matchService.getMatchesByStatus(MatchStatus.CONFIRMED);
        
        Map<String, Object> status = new HashMap<>();
        status.put("totalMatches", allMatches.size());
        status.put("waitingMatches", waitingMatches.size());
        status.put("confirmedMatches", confirmedMatches.size());
        status.put("databaseStatus", "H2 Database attivo");
        status.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(status);
    }
}