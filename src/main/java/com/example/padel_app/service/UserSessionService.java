package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SERVIZIO PER GESTIONE SESSIONE UTENTE (HTTP SESSION).
 * 
 * <h2>Cos'è una HTTP Session?</h2>
 * La sessione HTTP è un meccanismo che permette al server di "ricordare" un utente
 * tra diverse richieste HTTP (che sono stateless per natura).
 * 
 * <h2>Come funziona:</h2>
 * <pre>
 * 1. User fa login → Server crea SESSION e salva user ID
 * 2. Server invia COOKIE al browser con Session ID
 * 3. Browser invia cookie in OGNI richiesta successiva
 * 4. Server ritrova la session usando il cookie e sa chi è l'utente
 * </pre>
 * 
 * <h2>Esempio pratico:</h2>
 * <pre>
 * // Login: salva user nella sessione
 * userSessionService.setCurrentUser(authenticatedUser);
 * 
 * // Richieste successive: recupera user dalla sessione
 * User currentUser = userSessionService.getCurrentUser();
 * if (currentUser != null) {
 *     // Utente autenticato!
 * }
 * 
 * // Logout: pulisce la sessione
 * userSessionService.clearSession();
 * </pre>
 * 
 * <h2>Alternativa Spring Security:</h2>
 * Normalmente si usa Spring Security per gestire autenticazione, ma per un progetto
 * universitario questo approccio con HTTP Session manuale è più semplice e didattico.
 * 
 * <h2>✅ Sicurezza Implementata:</h2>
 * <ul>
 *   <li><strong>✅ Password hashate con BCrypt</strong> - Implementato tramite {@link com.example.padel_app.config.SecurityConfig}</li>
 *   <li><strong>✅ Auto-upgrade legacy passwords</strong> - Password in chiaro convertite automaticamente a BCrypt al login</li>
 *   <li><strong>✅ Session management sicuro</strong> - Session invalidation completa al logout</li>
 * </ul>
 * 
 * <h2>⚠️ Sicurezza Aggiuntiva per Produzione Enterprise:</h2>
 * <ul>
 *   <li>Usare <strong>HTTPS</strong> obbligatorio</li>
 *   <li>Implementare <strong>CSRF protection</strong> con Spring Security</li>
 *   <li>Aggiungere <strong>rate limiting</strong> per prevenire brute force</li>
 *   <li>Usare <strong>Spring Security</strong> framework completo</li>
 * </ul>
 * 
 * @see HttpSession Oggetto Spring per gestire sessioni HTTP
 * @see AuthController Controller che usa questo servizio per login/logout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {
    
    /**
     * Chiave usata per salvare l'ID utente nella sessione HTTP.
     * 
     * <p>HttpSession funziona come una Map: (String key, Object value)
     * Usiamo questa costante per accedere sempre allo stesso attributo.
     */
    private static final String USER_ID_SESSION_KEY = "currentUserId";
    
    private final UserRepository userRepository;
    
    /**
     * Recupera l'utente attualmente loggato dalla sessione HTTP.
     * 
     * <h3>Flusso operativo:</h3>
     * <pre>
     * 1. Legge ID utente dalla sessione HTTP (getAttribute)
     * 2. Se presente → carica User dal database
     * 3. Se assente → ritorna null (utente non loggato)
     * </pre>
     * 
     * <h3>Esempio utilizzo nel Controller:</h3>
     * <pre>
     * &#64;GetMapping("/my-matches")
     * public String myMatches(HttpSession session, Model model) {
     *     User currentUser = userSessionService.getCurrentUser(session);
     *     
     *     if (currentUser == null) {
     *         // Redirect a login
     *         return "redirect:/login";
     *     }
     *     
     *     // Usa currentUser per filtrare partite
     *     List&lt;Match&gt; userMatches = matchService.getMatchesByUser(currentUser);
     *     model.addAttribute("matches", userMatches);
     *     return "my-matches";
     * }
     * </pre>
     * 
     * @param session La sessione HTTP corrente (iniettata automaticamente da Spring)
     * @return L'utente loggato, oppure null se nessuno è autenticato
     */
    public User getCurrentUser(HttpSession session) {
        // STEP 1: Leggi user ID dalla sessione
        Long userId = (Long) session.getAttribute(USER_ID_SESSION_KEY);
        
        if (userId == null) {
            log.debug("Nessun utente in sessione");
            return null;
        }
        
        // STEP 2: Carica user dal database
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            // ID in sessione ma user non esiste più nel DB → invalida sessione
            log.warn("User ID {} in sessione ma non trovato nel database. Clearing session.", userId);
            session.removeAttribute(USER_ID_SESSION_KEY);
            return null;
        }
        
        User user = userOpt.get();
        log.debug("Utente recuperato dalla sessione: {} ({})", user.getUsername(), user.getId());
        return user;
    }
    
    /**
     * Salva l'utente nella sessione HTTP dopo login riuscito.
     * 
     * <h3>Quando viene usato:</h3>
     * Subito dopo che l'utente ha fatto login con successo (username/password corretti).
     * 
     * <h3>Cosa succede internamente:</h3>
     * <pre>
     * - HttpSession.setAttribute("currentUserId", user.getId())
     * - Server genera un Session ID univoco
     * - Session ID inviato al browser tramite cookie JSESSIONID
     * - Cookie automaticamente inviato in tutte le richieste successive
     * </pre>
     * 
     * <h3>Esempio utilizzo nel AuthController:</h3>
     * <pre>
     * &#64;PostMapping("/login")
     * public String login(String email, String password, HttpSession session) {
     *     User user = userService.findByEmail(email);
     *     
     *     if (user != null && user.getPassword().equals(password)) {
     *         // ✅ Credenziali corrette → salva in sessione
     *         userSessionService.setCurrentUser(session, user);
     *         return "redirect:/";
     *     }
     *     
     *     // ❌ Credenziali errate
     *     return "redirect:/login?error";
     * }
     * </pre>
     * 
     * @param session La sessione HTTP corrente
     * @param user L'utente da salvare in sessione
     */
    public void setCurrentUser(HttpSession session, User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Salva solo l'ID (non l'intero oggetto) per evitare problemi di serializzazione
        session.setAttribute(USER_ID_SESSION_KEY, user.getId());
        
        log.info("Utente {} salvato nella sessione (Session ID: {})", 
                 user.getUsername(), session.getId());
    }
    
    /**
     * Pulisce la sessione rimuovendo l'utente loggato (logout).
     * 
     * <h3>Differenza tra removeAttribute e invalidate:</h3>
     * <ul>
     *   <li><strong>removeAttribute("key")</strong>: Rimuove solo l'attributo specifico, 
     *       mantiene la sessione attiva per altri dati</li>
     *   <li><strong>invalidate()</strong>: Distrugge completamente la sessione 
     *       (consigliato per logout sicuro)</li>
     * </ul>
     * 
     * Usiamo <code>invalidate()</code> per sicurezza: così siamo certi che 
     * NESSUN dato dell'utente rimanga in sessione.
     * 
     * <h3>Esempio utilizzo nel AuthController:</h3>
     * <pre>
     * &#64;GetMapping("/logout")
     * public String logout(HttpSession session) {
     *     userSessionService.clearSession(session);
     *     return "redirect:/login?logout";
     * }
     * </pre>
     * 
     * @param session La sessione HTTP da pulire
     */
    public void clearSession(HttpSession session) {
        String sessionId = session.getId();
        User currentUser = getCurrentUser(session);
        
        // Invalida completamente la sessione (più sicuro di removeAttribute)
        session.invalidate();
        
        if (currentUser != null) {
            log.info("Logout utente: {} (Session ID: {})", currentUser.getUsername(), sessionId);
        } else {
            log.debug("Session cleared (Session ID: {})", sessionId);
        }
    }
    
    /**
     * Verifica se un utente è autenticato (presente in sessione).
     * 
     * <h3>Uso pratico:</h3>
     * Metodo di utilità per verifiche rapide nei controller.
     * 
     * <pre>
     * // Controller:
     * if (!userSessionService.isAuthenticated(session)) {
     *     return "redirect:/login";
     * }
     * 
     * // Oppure in un Interceptor per proteggere tutte le route
     * </pre>
     * 
     * @param session La sessione HTTP corrente
     * @return true se un utente è loggato, false altrimenti
     */
    public boolean isAuthenticated(HttpSession session) {
        return getCurrentUser(session) != null;
    }
}
