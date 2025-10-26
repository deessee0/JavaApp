package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserService - Service per gestione utenti
 * 
 * RESPONSABILITÀ BUSINESS LOGIC:
 * Service layer che gestisce tutte le operazioni CRUD sugli utenti e le funzionalità
 * business correlate come gestione livelli, statistiche, e autenticazione.
 * 
 * PATTERN ARCHITETTURALE:
 * Segue il pattern Service Layer:
 * - Controller → chiama Service → chiama Repository → Database
 * - Service contiene business logic (es: validazioni, calcoli statistici)
 * - Repository contiene solo accesso dati (query)
 * - Separazione responsabilità: più facile testare e mantenere
 * 
 * CONCETTI SPRING DIMOSTRATI:
 * 
 * 1. @Service - Componente business logic
 *    - Marca classe come service layer Spring
 *    - Auto-scansionato e gestito da Spring container
 *    - Singleton per default (una sola istanza condivisa)
 * 
 * 2. @Transactional - Gestione transazioni
 *    - readOnly=true (default classe): ottimizzazione per query SELECT
 *    - @Transactional su metodi write: garantisce atomicità operazioni
 *    - Rollback automatico in caso di exception
 * 
 * 3. @RequiredArgsConstructor (Lombok)
 *    - Genera costruttore per dependency injection
 *    - Inietta UserRepository automaticamente
 * 
 * GESTIONE LIVELLI:
 * Il sistema gestisce DUE tipi di livello per ogni utente:
 * 
 * 1. declaredLevel (livello dichiarato)
 *    - Impostato dall'utente stesso durante registrazione
 *    - Rappresenta auto-valutazione personale
 *    - Modificabile dall'utente in qualsiasi momento
 *    - Es: "Mi considero un giocatore INTERMEDIO"
 * 
 * 2. perceivedLevel (livello percepito)
 *    - Calcolato automaticamente dai feedback ricevuti
 *    - Rappresenta valutazione oggettiva da altri giocatori
 *    - Aggiornato automaticamente da FeedbackService
 *    - Es: "Gli altri mi vedono come AVANZATO"
 * 
 * Utilità confronto livelli:
 * - Identificare giocatori che sottostimano/sovrastimano le proprie capacità
 * - Creare match bilanciati usando perceivedLevel (più affidabile)
 * - Suggerire all'utente di aggiornare declaredLevel se molto diverso
 * 
 * STATISTICHE UTENTE:
 * Il service fornisce metodi per calcolare e recuperare statistiche:
 * - matchesPlayed: contatore partite completate (incrementato automaticamente)
 * - Feedback ricevuti: tramite FeedbackService.getFeedbacksByTargetUser()
 * - Classifica giocatori attivi: getUsersOrderByMatchesPlayed()
 * 
 * OPERAZIONI CRUD:
 * Create: saveUser() - crea nuovo utente
 * Read: getUserById(), getUserByUsername(), getAllUsers()
 * Update: saveUser() con ID esistente, updateDeclaredLevel(), updatePerceivedLevel()
 * Delete: deleteUser()
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Recupera tutti gli utenti registrati
     * 
     * BUSINESS LOGIC:
     * Query semplice per listing utenti (es: pagina admin, lista giocatori).
     * 
     * READ-ONLY: nessuna modifica database, usa ottimizzazione readOnly.
     * 
     * Esempio uso:
     * <pre>
     * List<User> allUsers = userService.getAllUsers();
     * // Mostra tabella con tutti i giocatori registrati
     * </pre>
     * 
     * @return Lista di tutti gli utenti (può essere vuota se nessun utente registrato)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Trova utenti con uno specifico livello dichiarato
     * 
     * BUSINESS LOGIC:
     * Filtra utenti per livello auto-dichiarato.
     * Utile per:
     * - Mostrare giocatori dello stesso livello
     * - Suggerire avversari per partite bilanciate
     * - Statistiche distribuzione livelli
     * 
     * Esempio uso:
     * <pre>
     * List<User> intermediates = userService.getUsersByDeclaredLevel(Level.INTERMEDIO);
     * // Mostra tutti i giocatori che si dichiarano INTERMEDIO
     * </pre>
     * 
     * @param level Livello dichiarato da cercare
     * @return Lista utenti con quel declared level
     */
    public List<User> getUsersByDeclaredLevel(Level level) {
        return userRepository.findByDeclaredLevel(level);
    }
    
    /**
     * Trova utenti con uno specifico livello percepito
     * 
     * BUSINESS LOGIC:
     * Filtra utenti per livello calcolato da feedback.
     * Più affidabile di declaredLevel perché basato su valutazioni oggettive.
     * 
     * Utilità:
     * - Creare match bilanciati con giocatori valutati allo stesso livello
     * - Identificare talenti nascosti (perceivedLevel > declaredLevel)
     * - Suggerire upgrade/downgrade declaredLevel
     * 
     * Esempio uso:
     * <pre>
     * List<User> perceivedAdvanced = userService.getUsersByPerceivedLevel(Level.AVANZATO);
     * // Giocatori valutati come AVANZATO da altri, indipendentemente da auto-valutazione
     * </pre>
     * 
     * @param level Livello percepito da cercare
     * @return Lista utenti con quel perceived level
     */
    public List<User> getUsersByPerceivedLevel(Level level) {
        return userRepository.findByPerceivedLevel(level);
    }
    
    /**
     * Recupera utenti ordinati per numero partite giocate (decrescente)
     * 
     * BUSINESS LOGIC:
     * Classifica giocatori più attivi.
     * 
     * Utilità:
     * - Leaderboard "Giocatori più attivi"
     * - Identificare utenti veterani vs principianti
     * - Badge/achievement per milestone partite
     * 
     * Esempio uso:
     * <pre>
     * List<User> topPlayers = userService.getUsersOrderByMatchesPlayed();
     * // topPlayers.get(0) = giocatore con più partite
     * // Mostra classifica attività
     * </pre>
     * 
     * @return Lista utenti ordinata per matchesPlayed DESC (primo = più attivo)
     */
    public List<User> getUsersOrderByMatchesPlayed() {
        return userRepository.findAllOrderByMatchesPlayedDesc();
    }
    
    /**
     * Trova utente per ID
     * 
     * BUSINESS LOGIC:
     * Recupero utente tramite chiave primaria (lookup più veloce).
     * 
     * Optional<User>:
     * - Se ID esiste → Optional.of(user)
     * - Se ID non esiste → Optional.empty()
     * - Forza gestione caso "not found" senza NullPointerException
     * 
     * Esempio uso:
     * <pre>
     * User user = userService.getUserById(123L)
     *     .orElseThrow(() -> new NotFoundException("Utente non trovato"));
     * </pre>
     * 
     * @param id ID univoco utente
     * @return Optional contenente User se trovato, empty altrimenti
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Trova utente per username (usato per login)
     * 
     * BUSINESS LOGIC:
     * Metodo fondamentale per autenticazione.
     * Username è univoco (validato da existsByUsername prima di creare utente).
     * 
     * Flusso login tipico:
     * 1. Utente inserisce username e password
     * 2. getUserByUsername(username) → recupera User
     * 3. Verifica password (hash bcrypt)
     * 4. Se match → login success, crea sessione
     * 
     * Esempio uso:
     * <pre>
     * Optional<User> user = userService.getUserByUsername("alice");
     * if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
     *     // Login success
     *     session.setAttribute("currentUser", user.get());
     * }
     * </pre>
     * 
     * @param username Username univoco utente
     * @return Optional contenente User se username esiste, empty altrimenti
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Trova utente per email
     * 
     * BUSINESS LOGIC:
     * Usato per:
     * - Password reset (invia link a email)
     * - Verifica email durante registrazione
     * - Evitare email duplicate
     * 
     * Esempio uso:
     * <pre>
     * Optional<User> user = userService.getUserByEmail("alice@example.com");
     * if (user.isPresent()) {
     *     sendPasswordResetEmail(user.get());
     * }
     * </pre>
     * 
     * @param email Email utente
     * @return Optional contenente User se email esiste, empty altrimenti
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Salva un utente (CREATE o UPDATE)
     * 
     * BUSINESS LOGIC:
     * Metodo generico per persistenza utente.
     * - Se user.id == null → INSERT (nuovo utente)
     * - Se user.id != null → UPDATE (modifica utente esistente)
     * 
     * TRANSACTIONAL:
     * Operazione write, richiede transazione.
     * Se errore durante save → rollback automatico.
     * 
     * Esempio CREATE (registrazione):
     * <pre>
     * User newUser = new User();
     * newUser.setUsername("bob");
     * newUser.setEmail("bob@example.com");
     * newUser.setDeclaredLevel(Level.INTERMEDIO);
     * newUser.setMatchesPlayed(0);
     * User saved = userService.saveUser(newUser);
     * // saved.id ora contiene ID generato dal database
     * </pre>
     * 
     * Esempio UPDATE:
     * <pre>
     * User alice = userService.getUserByUsername("alice").orElseThrow();
     * alice.setEmail("alice.new@example.com");
     * userService.saveUser(alice);
     * // Email aggiornata nel database
     * </pre>
     * 
     * @param user Utente da salvare (nuovo o esistente)
     * @return User salvato con ID generato (se nuovo)
     */
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Elimina un utente per ID
     * 
     * BUSINESS LOGIC:
     * Rimozione permanente utente dal sistema.
     * 
     * ATTENZIONE - INTEGRITÀ REFERENZIALE:
     * Prima di eliminare utente, verificare che non ci siano riferimenti:
     * - Feedback scritti/ricevuti
     * - Registrazioni a match
     * - Match creati
     * 
     * Potrebbe causare errore constraint violation se ci sono FK.
     * Meglio implementare "soft delete" (flag deleted=true) in produzione.
     * 
     * TRANSACTIONAL:
     * Operazione write, usa transazione.
     * Se errore FK constraint → rollback, utente non eliminato.
     * 
     * Esempio uso:
     * <pre>
     * userService.deleteUser(123L);
     * // Utente con ID 123 rimosso dal database
     * </pre>
     * 
     * @param id ID utente da eliminare
     */
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    /**
     * Incrementa contatore partite giocate
     * 
     * BUSINESS LOGIC:
     * Chiamato automaticamente quando un utente completa una partita.
     * Traccia statistica partecipazione utente.
     * 
     * TRANSACTIONAL:
     * Operazione write che modifica user.matchesPlayed.
     * Atomicità garantita: o incremento riuscito o rollback.
     * 
     * INVOCAZIONE AUTOMATICA:
     * Tipicamente chiamato da MatchService quando match passa a COMPLETED.
     * 
     * Esempio uso:
     * <pre>
     * // Match completato, aggiorna statistiche tutti i partecipanti
     * List<Registration> participants = registrationService.getJoinedRegistrations(match);
     * for (Registration reg : participants) {
     *     userService.incrementMatchesPlayed(reg.getUser());
     * }
     * </pre>
     * 
     * @param user Utente di cui incrementare counter
     * @return User aggiornato con matchesPlayed++
     */
    @Transactional
    public User incrementMatchesPlayed(User user) {
        user.setMatchesPlayed(user.getMatchesPlayed() + 1);
        return userRepository.save(user);
    }
    
    /**
     * Aggiorna livello percepito manualmente
     * 
     * BUSINESS LOGIC:
     * Normalmente perceivedLevel è aggiornato automaticamente da FeedbackService.
     * Questo metodo serve per:
     * - Correzioni manuali admin
     * - Reset perceived level
     * - Testing
     * 
     * NOTA: in uso normale, FeedbackService.updatePerceivedLevel() gestisce
     * automaticamente questo campo. Usare con cautela.
     * 
     * TRANSACTIONAL:
     * Operazione write, modifica database.
     * 
     * @param user Utente da aggiornare
     * @param newLevel Nuovo livello percepito
     * @return User aggiornato
     */
    @Transactional
    public User updatePerceivedLevel(User user, Level newLevel) {
        user.setPerceivedLevel(newLevel);
        return userRepository.save(user);
    }
    
    /**
     * Aggiorna livello dichiarato
     * 
     * BUSINESS LOGIC:
     * Permette all'utente di modificare la propria auto-valutazione.
     * 
     * Scenario tipico:
     * - Utente inizia come PRINCIPIANTE
     * - Dopo 50 partite, migliora molto
     * - Utente aggiorna declaredLevel a INTERMEDIO
     * 
     * Sistema potrebbe suggerire cambio se perceivedLevel molto diverso:
     * "Ti dichiari PRINCIPIANTE ma gli altri ti vedono INTERMEDIO. Vuoi aggiornare?"
     * 
     * TRANSACTIONAL:
     * Operazione write, modifica database.
     * 
     * Esempio uso:
     * <pre>
     * User alice = userService.getUserByUsername("alice").orElseThrow();
     * userService.updateDeclaredLevel(alice, Level.AVANZATO);
     * // Alice ha aggiornato il suo livello auto-dichiarato
     * </pre>
     * 
     * @param user Utente da aggiornare
     * @param newLevel Nuovo livello dichiarato
     * @return User aggiornato
     */
    @Transactional
    public User updateDeclaredLevel(User user, Level newLevel) {
        user.setDeclaredLevel(newLevel);
        return userRepository.save(user);
    }
    
    /**
     * Verifica se username già esiste (validazione registrazione)
     * 
     * BUSINESS LOGIC:
     * Usato durante registrazione nuovo utente per evitare duplicati.
     * Username deve essere univoco nel sistema.
     * 
     * EFFICIENZA:
     * Usa EXISTS query invece di findByUsername().isPresent()
     * Più veloce perché non carica entità completa, solo check esistenza.
     * 
     * Esempio uso nel controller registrazione:
     * <pre>
     * if (userService.existsByUsername("alice")) {
     *     model.addAttribute("error", "Username già in uso, scegline un altro");
     *     return "register";
     * }
     * // Procedi con creazione utente
     * </pre>
     * 
     * @param username Username da verificare
     * @return true se username già esiste, false se disponibile
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Verifica se email già esiste (validazione registrazione)
     * 
     * BUSINESS LOGIC:
     * Previene registrazione multipla con stessa email.
     * Una persona = un account.
     * 
     * EFFICIENZA:
     * Usa EXISTS query, non carica entità completa.
     * 
     * Esempio uso:
     * <pre>
     * if (userService.existsByEmail("alice@example.com")) {
     *     model.addAttribute("error", "Email già registrata");
     *     return "register";
     * }
     * </pre>
     * 
     * @param email Email da verificare
     * @return true se email già registrata, false se disponibile
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
