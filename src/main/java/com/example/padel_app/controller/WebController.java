package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.MatchService;
import com.example.padel_app.service.RegistrationService;
import com.example.padel_app.service.UserService;
import com.example.padel_app.service.FeedbackService;
import com.example.padel_app.service.UserContext;
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
    private final UserContext userContext;
    
    @GetMapping("/")
    public String home(Model model) {
        User currentUser = userContext.getCurrentUser();
        
        // Partite disponibili (a cui Margherita NON è iscritta)
        List<Match> availableMatches = matchService.getAllMatches().stream()
            .filter(m -> m.getStatus() == MatchStatus.WAITING || m.getStatus() == MatchStatus.CONFIRMED)
            .filter(m -> !registrationService.isUserRegistered(currentUser, m))
            .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("availableMatches", availableMatches);
        
        return "index";
    }
    
    @GetMapping("/my-matches")
    public String myMatches(Model model) {
        User currentUser = userContext.getCurrentUser();
        
        // Partite a cui sono iscritta (future)
        List<Match> myRegisteredMatches = registrationService.getActiveRegistrationsByUser(currentUser).stream()
            .map(com.example.padel_app.model.Registration::getMatch)
            .filter(m -> m.getStatus() != MatchStatus.FINISHED)
            .sorted((m1, m2) -> m1.getDateTime().compareTo(m2.getDateTime()))
            .collect(java.util.stream.Collectors.toList());
        
        // Partite già giocate (passate)
        List<Match> myFinishedMatches = registrationService.getActiveRegistrationsByUser(currentUser).stream()
            .map(com.example.padel_app.model.Registration::getMatch)
            .filter(m -> m.getStatus() == MatchStatus.FINISHED)
            .sorted((m1, m2) -> m2.getDateTime().compareTo(m1.getDateTime()))
            .collect(java.util.stream.Collectors.toList());
        
        // Feedback ricevuti da Margherita
        List<com.example.padel_app.model.Feedback> feedbackReceived = 
            feedbackService.getFeedbacksByTargetUser(currentUser);
        
        // Feedback dati da Margherita
        List<com.example.padel_app.model.Feedback> feedbackGiven = 
            feedbackService.getFeedbacksByAuthor(currentUser);
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("registeredMatches", myRegisteredMatches);
        model.addAttribute("finishedMatches", myFinishedMatches);
        model.addAttribute("feedbackReceived", feedbackReceived);
        model.addAttribute("feedbackGiven", feedbackGiven);
        
        return "my-matches";
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
        return "create-match";
    }
    
    @PostMapping("/matches/create")
    @Transactional
    public String createMatch(CreateMatchRequest request, Model model) {
        try {
            User currentUser = userContext.getCurrentUser();
            
            Match match = new Match();
            match.setLocation(request.getLocation());
            match.setDescription(request.getDescription());
            match.setRequiredLevel(Level.valueOf(request.getRequiredLevel()));
            match.setType(com.example.padel_app.model.enums.MatchType.PROPOSTA);
            match.setStatus(MatchStatus.WAITING);
            match.setDateTime(java.time.LocalDateTime.parse(request.getDateTime()));
            match.setCreator(currentUser);
            match.setCreatedAt(java.time.LocalDateTime.now());
            
            Match savedMatch = matchService.saveMatch(match);
            
            // Auto-iscrive il creatore alla partita
            registrationService.joinMatch(currentUser, savedMatch);
            
            return "redirect:/my-matches";
            
        } catch (Exception e) {
            model.addAttribute("error", "Errore nella creazione della partita: " + e.getMessage());
            model.addAttribute("matchRequest", request);
            model.addAttribute("levels", Level.values());
            return "create-match";
        }
    }
    
    // Join match
    @PostMapping("/matches/{id}/join")
    public String joinMatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userContext.getCurrentUser();
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            
            registrationService.joinMatch(currentUser, match);
            redirectAttributes.addFlashAttribute("success", "Ti sei iscritta alla partita!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/";
    }
    
    // Leave match
    @PostMapping("/matches/{id}/leave")
    public String leaveMatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userContext.getCurrentUser();
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            
            registrationService.leaveMatch(currentUser, match);
            redirectAttributes.addFlashAttribute("success", "Ti sei disiscritto dalla partita!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/my-matches";
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
        User currentUser = userContext.getCurrentUser();
        Match match = matchService.getMatchById(id)
            .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
        
        if (match.getStatus() != MatchStatus.FINISHED) {
            model.addAttribute("error", "Puoi dare feedback solo a partite terminate");
            return "redirect:/my-matches";
        }
        
        // Get players from registrations (exclude current user)
        List<User> players = registrationService.getActiveRegistrationsByMatch(match).stream()
            .map(com.example.padel_app.model.Registration::getUser)
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("match", match);
        model.addAttribute("players", players);
        model.addAttribute("levels", Level.values());
        
        return "feedback";
    }
    
    // Submit feedback
    @PostMapping("/matches/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @RequestParam Long targetUserId,
                                 @RequestParam String suggestedLevel,
                                 @RequestParam(required = false) String comment,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userContext.getCurrentUser();
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            User targetUser = userService.getUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utente target non trovato"));
            
            feedbackService.createFeedback(currentUser, targetUser, match, 
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