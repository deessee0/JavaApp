package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.MatchService;
import com.example.padel_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    
    private final MatchService matchService;
    private final UserService userService;
    
    @GetMapping("/")
    public String home(Model model) {
        // Statistiche per la homepage
        List<Match> allMatches = matchService.getAllMatches();
        List<Match> waitingMatches = matchService.getMatchesByStatus(MatchStatus.WAITING);
        List<Match> confirmedMatches = matchService.getMatchesByStatus(MatchStatus.CONFIRMED);
        List<User> allUsers = userService.getAllUsers();
        
        model.addAttribute("totalMatches", allMatches.size());
        model.addAttribute("waitingMatches", waitingMatches.size());
        model.addAttribute("confirmedMatches", confirmedMatches.size());
        model.addAttribute("totalUsers", allUsers.size());
        
        return "index";
    }
    
    @GetMapping("/matches")
    public String matches(
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "date") String sort,
            Model model) {
        
        List<Match> matches;
        
        // Apply level filter if provided
        if (level != null && !level.isEmpty()) {
            try {
                Level levelEnum = Level.valueOf(level.toUpperCase());
                matches = matchService.getMatchesByLevel(levelEnum);
            } catch (IllegalArgumentException e) {
                matches = matchService.getAllMatches();
            }
        } else {
            // Apply sorting using Strategy pattern
            matches = matchService.getMatchesOrderedBy(sort);
        }
        
        model.addAttribute("matches", matches);
        model.addAttribute("level", level);
        model.addAttribute("sort", sort);
        
        return "matches";
    }
    
    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users";
    }
    
    @GetMapping("/matches/create")
    public String createMatchForm(Model model) {
        model.addAttribute("levels", Level.values());
        return "create-match";
    }
}