package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.MatchService;
import com.example.padel_app.service.RegistrationService;
import com.example.padel_app.service.UserService;
import com.example.padel_app.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    
    private final MatchService matchService;
    private final UserService userService;
    private final RegistrationService registrationService;
    private final FeedbackService feedbackService;
    
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
        model.addAttribute("matchRequest", new CreateMatchRequest());
        model.addAttribute("levels", Level.values());
        model.addAttribute("users", userService.getAllUsers());
        return "create-match";
    }
    
    @PostMapping("/matches/create")
    public String createMatch(CreateMatchRequest request, Model model) {
        try {
            // Usa il primo utente come creatore per semplicità
            User creator = userService.getAllUsers().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessun utente disponibile"));
            
            Match match = new Match();
            match.setLocation(request.getLocation());
            match.setDescription(request.getDescription());
            match.setRequiredLevel(Level.valueOf(request.getRequiredLevel()));
            match.setType(com.example.padel_app.model.enums.MatchType.PROPOSTA);
            match.setStatus(MatchStatus.WAITING);
            match.setDateTime(java.time.LocalDateTime.parse(request.getDateTime()));
            match.setCreator(creator);
            match.setCreatedAt(java.time.LocalDateTime.now());
            
            matchService.saveMatch(match);
            return "redirect:/matches";
            
        } catch (Exception e) {
            model.addAttribute("error", "Errore nella creazione della partita: " + e.getMessage());
            model.addAttribute("matchRequest", request);
            model.addAttribute("levels", Level.values());
            return "create-match";
        }
    }
    
    // Join match
    @PostMapping("/matches/{id}/join")
    public String joinMatch(@PathVariable Long id, 
                           @RequestParam Long userId,
                           RedirectAttributes redirectAttributes) {
        try {
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
            
            registrationService.joinMatch(user, match);
            redirectAttributes.addFlashAttribute("success", "Ti sei unito alla partita!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/matches";
    }
    
    // Leave match
    @PostMapping("/matches/{id}/leave")
    public String leaveMatch(@PathVariable Long id,
                            @RequestParam Long userId,
                            RedirectAttributes redirectAttributes) {
        try {
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
            
            registrationService.leaveMatch(user, match);
            redirectAttributes.addFlashAttribute("success", "Hai lasciato la partita!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/matches";
    }
    
    // Finish match
    @PostMapping("/matches/{id}/finish")
    public String finishMatch(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            matchService.finishMatch(id);
            redirectAttributes.addFlashAttribute("success", "Partita terminata! Puoi dare feedback ai giocatori.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/matches";
    }
    
    // Feedback page
    @GetMapping("/matches/{id}/feedback")
    public String feedbackForm(@PathVariable Long id, Model model) {
        Match match = matchService.getMatchById(id)
            .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
        
        if (match.getStatus() != MatchStatus.FINISHED) {
            model.addAttribute("error", "Puoi dare feedback solo a partite terminate");
            return "redirect:/matches";
        }
        
        // Get players from registrations
        List<User> players = registrationService.getActiveRegistrationsByMatch(match).stream()
            .map(com.example.padel_app.model.Registration::getUser)
            .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("match", match);
        model.addAttribute("players", players);
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("levels", Level.values());
        
        return "feedback";
    }
    
    // Submit feedback
    @PostMapping("/matches/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @RequestParam Long authorId,
                                 @RequestParam Long targetUserId,
                                 @RequestParam String suggestedLevel,
                                 @RequestParam(required = false) String comment,
                                 RedirectAttributes redirectAttributes) {
        try {
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            User author = userService.getUserById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Autore non trovato"));
            User targetUser = userService.getUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utente target non trovato"));
            
            feedbackService.createFeedback(author, targetUser, match, 
                Level.valueOf(suggestedLevel), comment != null ? comment : "");
            
            redirectAttributes.addFlashAttribute("success", 
                "Feedback inviato! Il livello percepito di " + targetUser.getFirstName() + " è stato aggiornato.");
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/matches/" + id + "/feedback";
    }
    
    // DTO per il form
    public static class CreateMatchRequest {
        private String location;
        private String description;
        private String requiredLevel;
        private String dateTime;
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getRequiredLevel() { return requiredLevel; }
        public void setRequiredLevel(String requiredLevel) { this.requiredLevel = requiredLevel; }
        
        public String getDateTime() { return dateTime; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    }
}