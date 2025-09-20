package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import com.example.padel_app.service.MatchService;
import com.example.padel_app.service.RegistrationService;
import com.example.padel_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    
    private final MatchService matchService;
    private final UserService userService;
    private final RegistrationService registrationService;
    
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
    
    // Create new match
    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody CreateMatchRequest request) {
        // For demo, use first user as creator
        Optional<User> creatorOpt = userService.getAllUsers().stream().findFirst();
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Match match = new Match();
        match.setLocation(request.getLocation());
        match.setDescription(request.getDescription());
        match.setRequiredLevel(Level.valueOf(request.getRequiredLevel()));
        match.setType(MatchType.PROPOSTA);
        match.setStatus(MatchStatus.WAITING);
        match.setDateTime(LocalDateTime.parse(request.getDateTime()));
        match.setCreator(creatorOpt.get());
        match.setCreatedAt(LocalDateTime.now());
        
        Match savedMatch = matchService.saveMatch(match);
        return ResponseEntity.ok(savedMatch);
    }
    
    // Join match
    @PostMapping("/{id}/join")
    public ResponseEntity<String> joinMatch(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            Optional<Match> matchOpt = matchService.getMatchById(id);
            if (matchOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // For demo, use first user if not specified
            User user;
            if (userId != null) {
                Optional<User> userOpt = userService.getUserById(userId);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("User not found");
                }
                user = userOpt.get();
            } else {
                user = userService.getAllUsers().stream().findFirst().orElse(null);
                if (user == null) {
                    return ResponseEntity.badRequest().body("No users available");
                }
            }
            
            registrationService.joinMatch(user, matchOpt.get());
            return ResponseEntity.ok("Successfully joined match!");
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Leave match
    @PostMapping("/{id}/leave")
    public ResponseEntity<String> leaveMatch(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            Optional<Match> matchOpt = matchService.getMatchById(id);
            if (matchOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // For demo, use first user if not specified
            User user;
            if (userId != null) {
                Optional<User> userOpt = userService.getUserById(userId);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("User not found");
                }
                user = userOpt.get();
            } else {
                user = userService.getAllUsers().stream().findFirst().orElse(null);
                if (user == null) {
                    return ResponseEntity.badRequest().body("No users available");
                }
            }
            
            registrationService.leaveMatch(user, matchOpt.get());
            return ResponseEntity.ok("Successfully left match!");
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Manual finish match for demo
    @PostMapping("/{id}/finish")
    public ResponseEntity<Match> finishMatch(@PathVariable Long id) {
        try {
            Match finishedMatch = matchService.finishMatch(id);
            return ResponseEntity.ok(finishedMatch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // DTO for creating matches
    public static class CreateMatchRequest {
        public String location;
        public String description;
        public String requiredLevel;
        public String dateTime;
        
        public String getLocation() { return location; }
        public String getDescription() { return description; }
        public String getRequiredLevel() { return requiredLevel; }
        public String getDateTime() { return dateTime; }
    }
}