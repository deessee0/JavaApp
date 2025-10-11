package com.example.padel_app.config;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.enums.*;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.RegistrationRepository;
import com.example.padel_app.repository.UserRepository;
import com.example.padel_app.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final FeedbackRepository feedbackRepository;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("🎾 Inizializzazione dati demo per App Padel...");
        
        // Crea utente principale (simulato come loggato)
        User margherita = createUser("margherita", "margherita.biffi@padel.it", "Margherita", "Biffi", Level.INTERMEDIO);
        
        // Crea altri utenti demo per partite
        User mario = createUser("mario", "mario@padel.it", "Mario", "Rossi", Level.INTERMEDIO);
        User lucia = createUser("lucia", "lucia@padel.it", "Lucia", "Bianchi", Level.AVANZATO);
        User giuseppe = createUser("giuseppe", "giuseppe@padel.it", "Giuseppe", "Verdi", Level.PRINCIPIANTE);
        User anna = createUser("anna", "anna@padel.it", "Anna", "Neri", Level.PROFESSIONISTA);
        User francesco = createUser("francesco", "francesco@padel.it", "Francesco", "Bruno", Level.INTERMEDIO);
        User sara = createUser("sara", "sara@padel.it", "Sara", "Ferrari", Level.AVANZATO);
        
        // === PARTITE DISPONIBILI (Margherita NON iscritta) ===
        
        // Partita fissa intermedio - lunedì 18:00
        Match disponibile1 = createMatch(
            "Centro Sportivo Milano", 
            "Partita fissa serale intermedi", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(1).withHour(18).withMinute(0),
            null
        );
        createRegistration(mario, disponibile1);
        createRegistration(francesco, disponibile1);
        
        // Partita fissa avanzato - mercoledì 10:00
        Match disponibile2 = createMatch(
            "Padel Club Roma", 
            "Allenamento mattutino avanzati", 
            Level.AVANZATO,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(3).withHour(10).withMinute(0),
            null
        );
        createRegistration(lucia, disponibile2);
        
        // Partita fissa principianti - giovedì 16:00
        Match disponibile3 = createMatch(
            "Sport Center Torino", 
            "Partita principianti", 
            Level.PRINCIPIANTE,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(4).withHour(16).withMinute(0),
            null
        );
        
        // Partita fissa professionisti - venerdì 20:00
        Match disponibile4 = createMatch(
            "Padel Arena Napoli", 
            "Torneo professionisti", 
            Level.PROFESSIONISTA,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(5).withHour(20).withMinute(0),
            null
        );
        createRegistration(anna, disponibile4);
        
        // Partita VUOTA - sabato 14:00
        Match disponibile5 = createMatch(
            "Padel Club Monza", 
            "Partita pomeridiana", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(6).withHour(14).withMinute(0),
            null
        );
        
        // === PARTITE A CUI MARGHERITA È ISCRITTA ===
        
        // Partita proposta da Margherita (WAITING - 2 giocatori)
        Match iscritta1 = createMatch(
            "Tennis Club Bergamo", 
            "Partita intermedi domani sera", 
            Level.INTERMEDIO,
            MatchType.PROPOSTA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(1).withHour(19).withMinute(30),
            margherita
        );
        createRegistration(margherita, iscritta1);
        createRegistration(sara, iscritta1);
        
        // Partita confermata con Margherita (CONFIRMED - 4 giocatori)
        Match iscritta2 = createMatch(
            "Padel Arena Milano", 
            "Partita confermata sabato", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.CONFIRMED,
            LocalDateTime.now().plusDays(6).withHour(10).withMinute(0),
            null
        );
        createRegistration(margherita, iscritta2);
        createRegistration(mario, iscritta2);
        createRegistration(francesco, iscritta2);
        createRegistration(sara, iscritta2);
        
        // === PARTITE CHE MARGHERITA HA GIÀ GIOCATO (FINISHED) ===
        
        // Partita giocata 1 settimana fa
        Match giocata1 = createMatch(
            "Centro Sportivo Milano", 
            "Partita intermedi", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.FINISHED,
            LocalDateTime.now().minusDays(7).withHour(18).withMinute(0),
            null
        );
        createRegistration(margherita, giocata1);
        createRegistration(mario, giocata1);
        createRegistration(lucia, giocata1);
        createRegistration(francesco, giocata1);
        
        // Feedback per partita giocata 1
        createFeedback(margherita, mario, giocata1, Level.INTERMEDIO, "Ottimo compagno, livello corretto");
        createFeedback(margherita, lucia, giocata1, Level.AVANZATO, "Molto brava, sopra il suo livello");
        createFeedback(mario, margherita, giocata1, Level.INTERMEDIO, "Buon gioco, livello confermato");
        createFeedback(lucia, margherita, giocata1, Level.INTERMEDIO, "Brava, livello corretto");
        
        // Partita giocata 3 giorni fa
        Match giocata2 = createMatch(
            "Padel Club Roma", 
            "Allenamento serale", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.FINISHED,
            LocalDateTime.now().minusDays(3).withHour(20).withMinute(0),
            null
        );
        createRegistration(margherita, giocata2);
        createRegistration(giuseppe, giocata2);
        createRegistration(anna, giocata2);
        createRegistration(sara, giocata2);
        
        // Feedback per partita giocata 2
        createFeedback(margherita, giuseppe, giocata2, Level.PRINCIPIANTE, "Ha bisogno di più pratica");
        createFeedback(margherita, anna, giocata2, Level.PROFESSIONISTA, "Eccellente giocatrice");
        createFeedback(anna, margherita, giocata2, Level.AVANZATO, "Ben giocato, sopra le aspettative");
        createFeedback(sara, margherita, giocata2, Level.INTERMEDIO, "Livello confermato");
        
        // Aggiorna contatori partite giocate
        margherita.setMatchesPlayed(2);
        margherita.setPerceivedLevel(Level.INTERMEDIO); // Media feedback ricevuti
        
        mario.setMatchesPlayed(1);
        lucia.setMatchesPlayed(1);
        giuseppe.setMatchesPlayed(1);
        anna.setMatchesPlayed(1);
        francesco.setMatchesPlayed(0);
        sara.setMatchesPlayed(1);
        
        userRepository.save(margherita);
        userRepository.save(mario);
        userRepository.save(lucia);
        userRepository.save(giuseppe);
        userRepository.save(anna);
        userRepository.save(francesco);
        userRepository.save(sara);
        
        log.info("✅ Dati demo caricati: {} utenti, {} partite, {} registrazioni", 
                 userRepository.count(), matchRepository.count(), registrationRepository.count());
    }
    
    private User createUser(String username, String email, String firstName, String lastName, Level level) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123"); // Placeholder password
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDeclaredLevel(level);
        user.setPerceivedLevel(level); // Inizialmente uguale al dichiarato
        user.setMatchesPlayed(0);
        return userRepository.save(user);
    }
    
    private Match createMatch(String location, String description, Level requiredLevel, 
                             MatchType type, MatchStatus status, LocalDateTime dateTime, User creator) {
        Match match = new Match();
        match.setLocation(location);
        match.setDescription(description);
        match.setRequiredLevel(requiredLevel);
        match.setType(type);
        match.setStatus(status);
        match.setDateTime(dateTime);
        match.setCreator(creator);
        match.setCreatedAt(LocalDateTime.now());
        return matchRepository.save(match);
    }
    
    private Registration createRegistration(User user, Match match) {
        Registration registration = new Registration();
        registration.setUser(user);
        registration.setMatch(match);
        registration.setStatus(RegistrationStatus.JOINED);
        registration.setRegisteredAt(LocalDateTime.now());
        return registrationRepository.save(registration);
    }
    
    private Feedback createFeedback(User author, User targetUser, Match match, Level suggestedLevel, String comment) {
        Feedback feedback = new Feedback();
        feedback.setAuthor(author);
        feedback.setTargetUser(targetUser);
        feedback.setMatch(match);
        feedback.setSuggestedLevel(suggestedLevel);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }
}