package com.example.padel_app.config;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.*;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.RegistrationRepository;
import com.example.padel_app.repository.UserRepository;
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
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("🎾 Inizializzazione dati demo per App Padel...");
        
        // Crea utenti demo
        User mario = createUser("mario", "mario@padel.it", "Mario", "Rossi", Level.INTERMEDIO);
        User lucia = createUser("lucia", "lucia@padel.it", "Lucia", "Bianchi", Level.AVANZATO);
        User giuseppe = createUser("giuseppe", "giuseppe@padel.it", "Giuseppe", "Verdi", Level.PRINCIPIANTE);
        User anna = createUser("anna", "anna@padel.it", "Anna", "Neri", Level.PROFESSIONISTA);
        User francesco = createUser("francesco", "francesco@padel.it", "Francesco", "Bruno", Level.INTERMEDIO);
        User sara = createUser("sara", "sara@padel.it", "Sara", "Ferrari", Level.AVANZATO);
        
        // Crea partite demo
        Match partita1 = createMatch(
            "Centro Sportivo Milano", 
            "Partita serale", 
            Level.INTERMEDIO,
            MatchType.PROPOSTA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(2).withHour(19).withMinute(30),
            mario
        );
        
        Match partita2 = createMatch(
            "Padel Club Roma", 
            "Allenamento mattutino", 
            Level.AVANZATO,
            MatchType.FISSA,
            MatchStatus.CONFIRMED,
            LocalDateTime.now().plusDays(3).withHour(10).withMinute(0),
            lucia
        );
        
        Match partita3 = createMatch(
            "Sport Center Torino", 
            "Partita principianti", 
            Level.PRINCIPIANTE,
            MatchType.PROPOSTA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(1).withHour(16).withMinute(0),
            giuseppe
        );
        
        Match partita4 = createMatch(
            "Padel Arena Napoli", 
            "Torneo amichevole", 
            Level.PROFESSIONISTA,
            MatchType.FISSA,
            MatchStatus.FINISHED,
            LocalDateTime.now().minusDays(2).withHour(18).withMinute(0),
            anna
        );
        
        // Crea registrazioni demo
        createRegistration(mario, partita1);
        createRegistration(francesco, partita1);
        
        createRegistration(lucia, partita2);
        createRegistration(anna, partita2);
        createRegistration(sara, partita2);
        createRegistration(francesco, partita2);
        
        createRegistration(giuseppe, partita3);
        createRegistration(mario, partita3);
        
        createRegistration(anna, partita4);
        createRegistration(lucia, partita4);
        createRegistration(sara, partita4);
        createRegistration(francesco, partita4);
        
        // Aggiorna contatori partite giocate
        anna.setMatchesPlayed(5);
        lucia.setMatchesPlayed(3);
        sara.setMatchesPlayed(4);
        francesco.setMatchesPlayed(2);
        mario.setMatchesPlayed(1);
        giuseppe.setMatchesPlayed(0);
        
        userRepository.save(anna);
        userRepository.save(lucia);
        userRepository.save(sara);
        userRepository.save(francesco);
        userRepository.save(mario);
        userRepository.save(giuseppe);
        
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
}