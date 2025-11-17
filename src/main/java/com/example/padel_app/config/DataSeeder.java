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

/**
 * DataSeeder - Componente per l'inizializzazione dei dati demo dell'applicazione.
 * 
 * <h2>Scopo del DataSeeder</h2>
 * <p>
 * Questa classe ha il compito di:
 * <ul>
 *   <li><b>Popolare il database</b> con dati di esempio all'avvio dell'applicazione</li>
 *   <li>Creare un <b>ambiente di test completo</b> per sviluppatori e tester</li>
 *   <li>Simulare scenari realistici per <b>demo e presentazioni</b></li>
 *   <li>Permettere di <b>testare tutte le funzionalità</b> senza dover inserire dati manualmente</li>
 * </ul>
 * 
 * <h3>Implementa CommandLineRunner</h3>
 * <p>
 * L'interfaccia <code>CommandLineRunner</code> di Spring Boot:
 * <ul>
 *   <li>Ha un metodo <code>run(String... args)</code> che viene <b>eseguito automaticamente</b> 
 *       dopo che il contesto Spring è stato inizializzato</li>
 *   <li>È perfetto per operazioni di <b>setup iniziale</b> come il seeding del database</li>
 *   <li>Viene eseguito <b>una sola volta</b> all'avvio dell'applicazione</li>
 *   <li>Ha accesso a tutti i <b>bean Spring</b> (repository, service, ecc.)</li>
 * </ul>
 * 
 * <h3>Alternative a CommandLineRunner</h3>
 * <p>
 * Altre opzioni per inizializzare dati in Spring Boot:
 * <ul>
 *   <li><b>@PostConstruct</b>: eseguito dopo la creazione del bean, ma prima che sia pronto
 *       <ul>
 *         <li>Pro: più semplice, eseguito prima</li>
 *         <li>Contro: non tutti i bean potrebbero essere pronti</li>
 *       </ul>
 *   </li>
 *   <li><b>ApplicationRunner</b>: simile a CommandLineRunner ma riceve ApplicationArguments invece di String[]
 *       <ul>
 *         <li>Pro: parsing degli argomenti più avanzato</li>
 *         <li>Contro: più complesso se non servono argomenti</li>
 *       </ul>
 *   </li>
 *   <li><b>@EventListener(ApplicationReadyEvent.class)</b>: eseguito quando l'app è completamente pronta
 *       <ul>
 *         <li>Pro: certezza che tutto sia inizializzato</li>
 *         <li>Contro: più verboso</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Ordine di Esecuzione con @Order</h3>
 * <p>
 * Se ci fossero più <code>CommandLineRunner</code>, si potrebbe usare <code>@Order(1)</code>
 * per controllare la sequenza di esecuzione:
 * <pre>
 * @Order(1) public class DatabaseSeeder implements CommandLineRunner { }
 * @Order(2) public class CacheWarmer implements CommandLineRunner { }
 * </pre>
 * <ul>
 *   <li>I numeri più <b>bassi</b> vengono eseguiti <b>prima</b></li>
 *   <li>Default: <code>Ordered.LOWEST_PRECEDENCE</code> (ultimo)</li>
 * </ul>
 * 
 * <h2>Dati Demo Creati</h2>
 * <p>
 * Questo seeder crea un dataset completo per testare l'applicazione:
 * <ul>
 *   <li><b>Margherita Biffi</b>: utente principale simulato come "loggato"
 *       <ul>
 *         <li>Con partite a cui è iscritta (future)</li>
 *         <li>Con partite già giocate (passate)</li>
 *         <li>Con feedback ricevuti e dati</li>
 *       </ul>
 *   </li>
 *   <li><b>Altri 6 utenti</b>: Mario, Lucia, Giuseppe, Anna, Francesco, Sara
 *       <ul>
 *         <li>Con diversi livelli (PRINCIPIANTE, INTERMEDIO, AVANZATO, PROFESSIONISTA)</li>
 *         <li>Iscritti a varie partite per creare scenari realistici</li>
 *       </ul>
 *   </li>
 *   <li><b>Partite disponibili</b>: partite a cui Margherita può iscriversi
 *       <ul>
 *         <li>Con diversi livelli richiesti</li>
 *         <li>Con numero variabile di giocatori già iscritti (da 0 a 3)</li>
 *         <li>In diverse date future</li>
 *       </ul>
 *   </li>
 *   <li><b>Partite in corso</b>: partite a cui Margherita è già iscritta
 *       <ul>
 *         <li>Partita PROPOSTA (creata da Margherita, 2 giocatori)</li>
 *         <li>Partita CONFIRMED (4 giocatori, pronta per giocare)</li>
 *       </ul>
 *   </li>
 *   <li><b>Partite terminate</b>: per testare il sistema di feedback
 *       <ul>
 *         <li>2 partite già giocate da Margherita</li>
 *         <li>Con feedback incrociati tra i giocatori</li>
 *         <li>Per calcolare il livello percepito</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * @author PadelApp Team
 * @version 1.0
 * @see org.springframework.boot.CommandLineRunner
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    
    /**
     * Repository iniettati per accedere al database.
     * 
     * <p>
     * Usiamo direttamente i Repository (non i Service) perché:
     * <ul>
     *   <li>È un'operazione di <b>setup iniziale</b>, non logica di business</li>
     *   <li>Vogliamo <b>controllo totale</b> sui dati inseriti (senza validazioni)</li>
     *   <li>Evitiamo di triggerare eventi e side-effects dei Service</li>
     * </ul>
     */
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final FeedbackRepository feedbackRepository;
    
    /**
     * Metodo principale di inizializzazione dati.
     * 
     * <p>
     * Viene eseguito automaticamente da Spring Boot dopo l'inizializzazione del contesto.
     * 
     * <h3>@Transactional - Atomicità del Seeding</h3>
     * <p>
     * L'annotazione @Transactional assicura che:
     * <ul>
     *   <li>Tutte le operazioni di inserimento siano una <b>singola transazione atomica</b></li>
     *   <li>In caso di errore, viene fatto <b>rollback completo</b> (niente dati parziali)</li>
     *   <li>Le relazioni tra entità (FK) siano <b>consistenti</b></li>
     * </ul>
     * 
     * <p>
     * <b>Perché è importante?</b>
     * <pre>
     * Senza @Transactional:
     * 1. Inserisci User OK
     * 2. Inserisci Match OK
     * 3. Inserisci Registration ERRORE → Database inconsistente!
     * 
     * Con @Transactional:
     * 1. Inserisci User OK
     * 2. Inserisci Match OK
     * 3. Inserisci Registration ERRORE → ROLLBACK automatico di tutto!
     * </pre>
     * 
     * @param args Argomenti da linea di comando (non usati in questo caso)
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info("🎾 Inizializzazione dati demo per App Padel...");
        
        // =====================================================================
        // STEP 1: Creazione utenti
        // =====================================================================
        
        // Margherita: utente principale per demo/test dell'applicazione
        // Credenziali di login: margherita.biffi@padel.it / password123
        User margherita = createUser("margherita", "margherita.biffi@padel.it", "Margherita", "Biffi", Level.INTERMEDIO);
        
        // Altri utenti demo con diversi livelli di abilità
        User mario = createUser("mario", "mario@padel.it", "Mario", "Rossi", Level.INTERMEDIO);
        User lucia = createUser("lucia", "lucia@padel.it", "Lucia", "Bianchi", Level.AVANZATO);
        User giuseppe = createUser("giuseppe", "giuseppe@padel.it", "Giuseppe", "Verdi", Level.PRINCIPIANTE);
        User anna = createUser("anna", "anna@padel.it", "Anna", "Neri", Level.PROFESSIONISTA);
        User francesco = createUser("francesco", "francesco@padel.it", "Francesco", "Bruno", Level.INTERMEDIO);
        User sara = createUser("sara", "sara@padel.it", "Sara", "Ferrari", Level.AVANZATO);
        
        // =====================================================================
        // STEP 2: Partite disponibili (Margherita NON iscritta)
        // =====================================================================
        // Queste partite appariranno nella home page come "disponibili"
        
        // Partita fissa intermedi - lunedì 18:00
        // Ha già 2 giocatori iscritti (Mario e Francesco)
        Match disponibile1 = createMatch(
            "Centro Sportivo Milano", 
            "Partita fissa serale intermedi", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(1).withHour(18).withMinute(0),
            null  // Nessun creatore (partita fissa organizzata dal centro)
        );
        createRegistration(mario, disponibile1);
        createRegistration(francesco, disponibile1);
        
        // Partita fissa avanzati - mercoledì 10:00
        // Ha solo 1 giocatore iscritto (Lucia)
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
        // Nessun giocatore iscritto (partita vuota)
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
        // Ha 1 giocatore professionista (Anna)
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
        // Permette di testare l'iscrizione come primo giocatore
        Match disponibile5 = createMatch(
            "Padel Club Monza", 
            "Partita pomeridiana", 
            Level.INTERMEDIO,
            MatchType.FISSA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(6).withHour(14).withMinute(0),
            null
        );
        
        // =====================================================================
        // STEP 3: Partite a cui MARGHERITA È ISCRITTA
        // =====================================================================
        // Queste partite appariranno in "Le mie partite"
        
        // Partita PROPOSTA da Margherita (WAITING - 2 giocatori)
        // Margherita ha creato questa partita e si è auto-iscritta
        // Sara si è unita successivamente
        Match iscritta1 = createMatch(
            "Tennis Club Bergamo", 
            "Partita intermedi domani sera", 
            Level.INTERMEDIO,
            MatchType.PROPOSTA,
            MatchStatus.WAITING,
            LocalDateTime.now().plusDays(1).withHour(19).withMinute(30),
            margherita  // Margherita è la creatrice
        );
        createRegistration(margherita, iscritta1);
        createRegistration(sara, iscritta1);
        
        // Partita CONFERMATA con Margherita (CONFIRMED - 4 giocatori)
        // Tutti i 4 slot sono occupati, la partita è confermata e pronta da giocare
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
        
        // =====================================================================
        // STEP 4: Partite TERMINATE (per testare il sistema di feedback)
        // =====================================================================
        
        // Partita giocata 1 settimana fa
        // Permette di testare il form di feedback e il calcolo del livello percepito
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
        
        // Feedback incrociati per la partita giocata 1
        // Questi feedback aggiornano il "livello percepito" degli utenti
        createFeedback(margherita, mario, giocata1, Level.INTERMEDIO, "Ottimo compagno, livello corretto");
        createFeedback(margherita, lucia, giocata1, Level.AVANZATO, "Molto brava, sopra il suo livello");
        createFeedback(mario, margherita, giocata1, Level.INTERMEDIO, "Buon gioco, livello confermato");
        createFeedback(lucia, margherita, giocata1, Level.INTERMEDIO, "Brava, livello corretto");
        
        // Partita giocata 3 giorni fa
        // Altra partita con feedback per avere dati statistici più ricchi
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
        
        // Feedback per la partita giocata 2
        // Nota: alcuni giocatori molto più forti/deboli per testare la variabilità del livello
        createFeedback(margherita, giuseppe, giocata2, Level.PRINCIPIANTE, "Ha bisogno di più pratica");
        createFeedback(margherita, anna, giocata2, Level.PROFESSIONISTA, "Eccellente giocatrice");
        createFeedback(anna, margherita, giocata2, Level.AVANZATO, "Ben giocato, sopra le aspettative");
        createFeedback(sara, margherita, giocata2, Level.INTERMEDIO, "Livello confermato");
        
        // =====================================================================
        // STEP 5: Aggiornamento statistiche utenti
        // =====================================================================
        
        // Aggiorna il numero di partite giocate per ogni utente
        // Questo dato è usato per statistiche e calcolo del livello percepito
        margherita.setMatchesPlayed(2);
        margherita.setPerceivedLevel(Level.INTERMEDIO); // Media dei feedback ricevuti
        
        mario.setMatchesPlayed(1);
        lucia.setMatchesPlayed(1);
        giuseppe.setMatchesPlayed(1);
        anna.setMatchesPlayed(1);
        francesco.setMatchesPlayed(0);  // Non ha ancora giocato partite terminate
        sara.setMatchesPlayed(1);
        
        // Salva gli aggiornamenti
        userRepository.save(margherita);
        userRepository.save(mario);
        userRepository.save(lucia);
        userRepository.save(giuseppe);
        userRepository.save(anna);
        userRepository.save(francesco);
        userRepository.save(sara);
        
        // Log finale con statistiche
        log.info("✅ Dati demo caricati: {} utenti, {} partite, {} registrazioni", 
                 userRepository.count(), matchRepository.count(), registrationRepository.count());
    }
    
    /**
     * Metodo helper per creare un utente.
     * 
     * <p>
     * Questo metodo incapsula la logica di creazione di un utente,
     * evitando duplicazione di codice e rendendo il seeding più leggibile.
     * 
     * @param username Username univoco
     * @param email Email dell'utente
     * @param firstName Nome
     * @param lastName Cognome
     * @param level Livello dichiarato (inizialmente uguale al percepito)
     * @return L'utente salvato nel database
     */
    private User createUser(String username, String email, String firstName, String lastName, Level level) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123"); // Password placeholder per demo (in produzione: hash bcrypt!)
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDeclaredLevel(level);
        user.setPerceivedLevel(level); // Inizialmente il livello percepito è uguale al dichiarato
        user.setMatchesPlayed(0);
        return userRepository.save(user);
    }
    
    /**
     * Metodo helper per creare una partita.
     * 
     * @param location Luogo della partita
     * @param description Descrizione breve
     * @param requiredLevel Livello richiesto per partecipare
     * @param type Tipo di partita (FISSA o PROPOSTA)
     * @param status Stato della partita (WAITING, CONFIRMED, FINISHED)
     * @param dateTime Data e ora della partita
     * @param creator Creatore della partita (null per partite fisse)
     * @return La partita salvata nel database
     */
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
    
    /**
     * Metodo helper per creare una registrazione (iscrizione di un utente a una partita).
     * 
     * <p>
     * Le registrazioni collegano utenti e partite in una relazione many-to-many.
     * Ogni registrazione ha uno stato (JOINED, CANCELLED) e una data di iscrizione.
     * 
     * @param user L'utente che si iscrive
     * @param match La partita a cui iscriversi
     * @return La registrazione salvata nel database
     */
    private Registration createRegistration(User user, Match match) {
        Registration registration = new Registration();
        registration.setUser(user);
        registration.setMatch(match);
        registration.setStatus(RegistrationStatus.JOINED);
        registration.setRegisteredAt(LocalDateTime.now());
        return registrationRepository.save(registration);
    }
    
    /**
     * Metodo helper per creare un feedback.
     * 
     * <p>
     * I feedback permettono ai giocatori di valutare il livello percepito
     * degli altri partecipanti dopo una partita terminata.
     * Questi feedback sono usati per calcolare il "livello percepito medio"
     * di un utente, che può differire dal "livello dichiarato".
     * 
     * <h3>Importanza dei Feedback</h3>
     * <ul>
     *   <li>Permettono di <b>calibrare</b> il livello reale di un giocatore</li>
     *   <li>Evitano che giocatori si <b>sopravvalutino o sottovalutino</b></li>
     *   <li>Migliorano il <b>matching</b> tra giocatori di livello simile</li>
     *   <li>Forniscono <b>statistiche</b> utili nel profilo utente</li>
     * </ul>
     * 
     * @param author L'utente che dà il feedback
     * @param targetUser L'utente che riceve il feedback
     * @param match La partita in cui hanno giocato insieme
     * @param suggestedLevel Il livello percepito suggerito
     * @param comment Commento testuale opzionale
     * @return Il feedback salvato nel database
     */
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
