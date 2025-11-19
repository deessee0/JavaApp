package com.example.padel_app.controller;

import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.UserRepository;
import com.example.padel_app.service.UserSessionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * CONTROLLER PER AUTENTICAZIONE UTENTI (Login, Register, Logout).
 * 
 * <h2>Panoramica Sistema Autenticazione</h2>
 * Questo controller implementa un sistema di autenticazione per scopi didattici:
 * <ul>
 *   <li><strong>✅ Password hashate con BCrypt</strong> (sicurezza implementata!)</li>
 *   <li><strong>Auto-upgrade password legacy</strong>: password in chiaro vengono hashate automaticamente al primo login</li>
 *   <li>Usa HTTP Session manuale tramite {@link UserSessionService} (session-based auth)</li>
 *   <li>No Spring Security framework (per semplicità didattica, ma BCrypt sì!)</li>
 * </ul>
 * 
 * <h2>Flusso di Autenticazione:</h2>
 * <pre>
 * 1. User visita /login → mostra form
 * 2. User invia email/password → POST /login
 * 3. Controller verifica credenziali nel database
 * 4. Se corrette → salva user in sessione HTTP
 * 5. Redirect a homepage (/) con user loggato
 * </pre>
 * 
 * <h2>Endpoints disponibili:</h2>
 * <table border="1">
 *   <tr><th>URL</th><th>Metodo</th><th>Descrizione</th></tr>
 *   <tr><td>/login</td><td>GET</td><td>Mostra form di login</td></tr>
 *   <tr><td>/login</td><td>POST</td><td>Autentica utente</td></tr>
 *   <tr><td>/register</td><td>GET</td><td>Mostra form registrazione</td></tr>
 *   <tr><td>/register</td><td>POST</td><td>Crea nuovo utente</td></tr>
 *   <tr><td>/logout</td><td>GET</td><td>Logout e distrugge sessione</td></tr>
 * </table>
 * 
 * <h2>Pattern utilizzati:</h2>
 * <ul>
 *   <li><strong>POST-Redirect-GET (PRG)</strong>: Dopo POST facciamo redirect per evitare 
 *       double-submit se user fa refresh</li>
 *   <li><strong>Flash Messages</strong>: Usiamo RedirectAttributes per messaggi 
 *       success/error tra redirect</li>
 * </ul>
 * 
 * <h2>✅ Sicurezza Implementata:</h2>
 * <ul>
 *   <li><strong>BCrypt password hashing</strong> ✅ IMPLEMENTATO - Le password sono hashate con BCrypt</li>
 *   <li><strong>Auto-upgrade legacy passwords</strong> - Password in chiaro vengono hashate al primo login</li>
 * </ul>
 * 
 * <h2>⚠️ Sicurezza Aggiuntiva per Produzione:</h2>
 * Per una produzione enterprise dovresti aggiungere:
 * <ol>
 *   <li>Implementare <strong>Spring Security</strong> (CSRF, XSS protection)</li>
 *   <li>Usare <strong>HTTPS</strong> per comunicazione sicura</li>
 *   <li>Implementare <strong>rate limiting</strong> contro brute force</li>
 *   <li>Validare input con <strong>Bean Validation</strong></li>
 *   <li>Aggiungere <strong>2FA</strong> (Two-Factor Authentication)</li>
 * </ol>
 * 
 * @see UserSessionService Servizio che gestisce la sessione HTTP
 * @see User Entità utente nel database
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserRepository userRepository;
    private final UserSessionService userSessionService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    /**
     * Mostra la pagina di login.
     * 
     * <h3>Flusso:</h3>
     * <pre>
     * 1. User visita /login
     * 2. Controller verifica se già loggato
     * 3. Se già loggato → redirect a homepage
     * 4. Altrimenti → mostra form login
     * </pre>
     * 
     * <h3>Query Parameters supportati:</h3>
     * <ul>
     *   <li><code>?error</code>: Mostra messaggio di errore (credenziali errate)</li>
     *   <li><code>?logout</code>: Mostra messaggio di logout riuscito</li>
     *   <li><code>?registered</code>: Mostra messaggio di registrazione completata</li>
     * </ul>
     * 
     * <h3>Esempio URL:</h3>
     * <pre>
     * /login               → Form pulito
     * /login?error         → "Email o password errati"
     * /login?logout        → "Logout effettuato con successo"
     * /login?registered    → "Registrazione completata! Ora puoi fare login"
     * </pre>
     * 
     * @param session Sessione HTTP corrente (iniettata da Spring)
     * @param error Parametro opzionale per mostrare errore
     * @param logout Parametro opzionale per mostrare messaggio logout
     * @param registered Parametro opzionale per mostrare messaggio registrazione
     * @param model Modello Thymeleaf per passare dati alla view
     * @return Nome template Thymeleaf "login" oppure redirect se già loggato
     */
    @GetMapping("/login")
    public String loginPage(
            HttpSession session,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String registered,
            Model model) {
        
        // STEP 1: Verifica se user già loggato
        if (userSessionService.isAuthenticated(session)) {
            log.debug("User già autenticato, redirect a homepage");
            return "redirect:/";
        }
        
        // STEP 2: Prepara messaggi di feedback per la view
        if (error != null) {
            model.addAttribute("error", "Email o password errati. Riprova.");
        }
        
        if (logout != null) {
            model.addAttribute("success", "Logout effettuato con successo.");
        }
        
        if (registered != null) {
            model.addAttribute("success", "Registrazione completata! Ora puoi fare login.");
        }
        
        // STEP 3: Mostra form login
        return "login";
    }
    
    /**
     * Gestisce il processo di login (autenticazione).
     * 
     * <h3>Flusso dettagliato:</h3>
     * <pre>
     * 1. User invia form con email + password
     * 2. Controller cerca user per email nel database
     * 3. Verifica password (confronto diretto - NO hash per semplicità)
     * 4. Se credenziali corrette:
     *    a. Salva user in sessione HTTP
     *    b. Redirect a homepage (/)
     * 5. Se credenziali errate:
     *    a. Redirect a /login?error
     *    b. Mostra messaggio errore
     * </pre>
     * 
     * <h3>Pattern POST-Redirect-GET:</h3>
     * Dopo POST facciamo sempre redirect (non return diretto alla view):
     * <ul>
     *   <li>✅ <strong>PRO</strong>: Evita double-submit se user fa F5</li>
     *   <li>✅ <strong>PRO</strong>: URL pulito nella barra browser</li>
     *   <li>✅ <strong>PRO</strong>: Back button del browser funziona meglio</li>
     * </ul>
     * 
     * <h3>Esempio utilizzo nel form HTML:</h3>
     * <pre>
     * &lt;form method="post" action="/login"&gt;
     *     &lt;input type="email" name="email" required /&gt;
     *     &lt;input type="password" name="password" required /&gt;
     *     &lt;button type="submit"&gt;Login&lt;/button&gt;
     * &lt;/form&gt;
     * </pre>
     * 
     * <h3>✅ Sicurezza BCrypt:</h3>
     * Questo metodo usa <code>passwordEncoder.matches()</code> per verificare password hashate.
     * Supporta anche password legacy in chiaro per retro-compatibilità (auto-upgrade).
     * 
     * @param email Email inserita dall'utente
     * @param password Password inserita dall'utente (in chiaro)
     * @param session Sessione HTTP per salvare user se login ok
     * @param redirectAttributes Attributi flash per messaggi tra redirect
     * @return Redirect a homepage (/) se ok, altrimenti redirect a /login?error
     */
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        // Normalizza email a lowercase per evitare problemi case-sensitivity
        String normalizedEmail = email.trim().toLowerCase();
        
        log.info("Tentativo di login per email: {}", normalizedEmail);
        
        // STEP 1: Cerca user per email nel database
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        
        if (userOpt.isEmpty()) {
            // Email non trovata
            log.warn("Login fallito: email {} non trovata", email);
            return "redirect:/login?error";
        }
        
        User user = userOpt.get();
        
        // STEP 2: Verifica password (supporta sia BCrypt che password in chiaro)
        // RETRO-COMPATIBILITÀ: dati seedati potrebbero avere password in chiaro
        boolean passwordMatches;
        String storedPassword = user.getPassword();
        
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // Password è hashata con BCrypt (formato: $2a$, $2b$, $2y$ sono varianti BCrypt)
            log.debug("Verifica password BCrypt per user {}", user.getUsername());
            passwordMatches = passwordEncoder.matches(password, storedPassword);
        } else {
            // Password in chiaro (legacy/seeding) - confronto diretto
            log.warn("ATTENZIONE: User {} ha password in chiaro (non sicuro!)", user.getUsername());
            passwordMatches = password.equals(storedPassword);
            
            // SECURITY IMPROVEMENT: Aggiorna password a BCrypt al prossimo login
            if (passwordMatches) {
                log.info("Aggiornamento automatico password a BCrypt per user {}", user.getUsername());
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }
        
        if (!passwordMatches) {
            // Password errata
            log.warn("Login fallito: password errata per user {}", user.getUsername());
            return "redirect:/login?error";
        }
        
        // STEP 3: ✅ Credenziali corrette! Salva user in sessione
        userSessionService.setCurrentUser(session, user);
        
        log.info("Login riuscito per user: {} ({})", user.getUsername(), user.getEmail());
        
        // STEP 4: Redirect a homepage
        redirectAttributes.addFlashAttribute("success", 
            "Benvenuto/a, " + user.getFirstName() + "!");
        
        return "redirect:/";
    }
    
    /**
     * Mostra la pagina di registrazione nuovo utente.
     * 
     * <h3>Cosa deve contenere il form:</h3>
     * <ul>
     *   <li>Username (univoco)</li>
     *   <li>Email (univoca)</li>
     *   <li>Password</li>
     *   <li>Nome (firstName)</li>
     *   <li>Cognome (lastName)</li>
     *   <li>Livello dichiarato (dropdown: PRINCIPIANTE, INTERMEDIO, AVANZATO, PROFESSIONISTA)</li>
     * </ul>
     * 
     * @param session Sessione HTTP corrente
     * @param model Modello per passare dati alla view (es. enum Level per dropdown)
     * @return Nome template "register" oppure redirect se già loggato
     */
    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        // Se user già loggato, redirect a homepage
        if (userSessionService.isAuthenticated(session)) {
            log.debug("User già autenticato, redirect a homepage");
            return "redirect:/";
        }
        
        // Passa enum Level alla view per popolare dropdown livello
        model.addAttribute("levels", Level.values());
        
        return "register";
    }
    
    /**
     * Gestisce la registrazione di un nuovo utente.
     * 
     * <h3>Flusso dettagliato:</h3>
     * <pre>
     * 1. User compila form registrazione
     * 2. Controller valida dati (email e username univoci)
     * 3. Crea nuovo User nel database
     * 4. Redirect a /login?registered per conferma
     * 5. User può ora fare login con le credenziali create
     * </pre>
     * 
     * <h3>Validazioni implementate:</h3>
     * <ul>
     *   <li>Email univoca (nessun altro user con stessa email)</li>
     *   <li>Username univoco (nessun altro user con stesso username)</li>
     * </ul>
     * 
     * <h3>Campi inizializzati:</h3>
     * <ul>
     *   <li><strong>declaredLevel</strong>: Livello dichiarato dall'utente</li>
     *   <li><strong>perceivedLevel</strong>: null (verrà calcolato dai feedback)</li>
     *   <li><strong>matchesPlayed</strong>: 0 (incrementato quando partecipa a partite)</li>
     * </ul>
     * 
     * <h3>✅ Sicurezza BCrypt:</h3>
     * La password viene automaticamente hashata con BCrypt prima del salvataggio.
     * 
     * <h3>Miglioramenti futuri:</h3>
     * <ul>
     *   <li>Validare formato email con regex</li>
     *   <li>Validare lunghezza password (min 8 caratteri)</li>
     *   <li>Validare complessità password (maiuscole, numeri, simboli)</li>
     *   <li>Sanitize input contro XSS</li>
     * </ul>
     * 
     * @param username Username scelto dall'utente (deve essere univoco)
     * @param email Email dell'utente (deve essere univoca)
     * @param password Password in chiaro (⚠️ in produzione hashare!)
     * @param firstName Nome
     * @param lastName Cognome
     * @param declaredLevel Livello padel dichiarato (PRINCIPIANTE, INTERMEDIO, etc)
     * @param redirectAttributes Attributi flash per messaggi
     * @return Redirect a /login?registered se ok, altrimenti redirect a /register?error
     */
    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam Level declaredLevel,
            RedirectAttributes redirectAttributes) {
        
        // Normalizza email a lowercase per consistenza
        String normalizedEmail = email.trim().toLowerCase();
        
        log.info("Tentativo registrazione nuovo user: {} ({})", username, normalizedEmail);
        
        // STEP 1: Verifica che email non sia già usata
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Registrazione fallita: email {} già esistente", email);
            redirectAttributes.addFlashAttribute("error", 
                "Email già registrata. Usa un'altra email o fai login.");
            return "redirect:/register";
        }
        
        // STEP 2: Verifica che username non sia già usato
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Registrazione fallita: username {} già esistente", username);
            redirectAttributes.addFlashAttribute("error", 
                "Username già in uso. Scegline un altro.");
            return "redirect:/register";
        }
        
        // STEP 3: Crea nuovo User
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(normalizedEmail);  // Salva email normalizzata
        
        // ✅ SECURITY: Hash password con BCrypt prima di salvare
        String hashedPassword = passwordEncoder.encode(password);
        newUser.setPassword(hashedPassword);
        log.debug("Password hashata con BCrypt per nuovo user {}", username);
        
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setDeclaredLevel(declaredLevel);
        newUser.setPerceivedLevel(null);  // Sarà calcolato dai feedback
        newUser.setMatchesPlayed(0);      // Inizia da 0
        
        // STEP 4: Salva nel database
        userRepository.save(newUser);
        
        log.info("Registrazione completata per user: {} ({})", username, email);
        
        // STEP 5: Redirect a login con messaggio successo
        return "redirect:/login?registered";
    }
    
    /**
     * Gestisce il logout dell'utente.
     * 
     * <h3>Flusso:</h3>
     * <pre>
     * 1. User clicca "Logout"
     * 2. Controller chiama userSessionService.clearSession()
     * 3. Sessione HTTP distrutta (session.invalidate())
     * 4. Redirect a /login?logout con messaggio conferma
     * </pre>
     * 
     * <h3>Sicurezza sessione:</h3>
     * Usiamo <code>session.invalidate()</code> invece di <code>removeAttribute()</code>
     * per garantire che TUTTI i dati in sessione vengano eliminati.
     * 
     * <h3>Link HTML per logout:</h3>
     * <pre>
     * &lt;a href="/logout"&gt;Logout&lt;/a&gt;
     * 
     * Oppure con Thymeleaf:
     * &lt;a th:href="@{/logout}"&gt;Logout&lt;/a&gt;
     * </pre>
     * 
     * @param session Sessione HTTP da invalidare
     * @return Redirect a /login?logout
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        log.info("Logout richiesto");
        
        // Invalida sessione e pulisce dati utente
        userSessionService.clearSession(session);
        
        // Redirect a login con messaggio logout
        return "redirect:/login?logout";
    }
}
