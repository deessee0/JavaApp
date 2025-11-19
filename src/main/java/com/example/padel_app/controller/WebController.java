package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.MatchService;
import com.example.padel_app.service.RegistrationService;
import com.example.padel_app.service.UserService;
import com.example.padel_app.service.FeedbackService;
import com.example.padel_app.service.UserSessionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WebController - Il Controller principale dell'applicazione nel pattern MVC.
 * 
 * <h2>Ruolo del Controller nel Pattern MVC (Model-View-Controller)</h2>
 * <p>
 * Il Controller è il "C" di MVC e rappresenta il livello di controllo dell'applicazione.
 * È responsabile di:
 * <ul>
 *   <li><b>Ricevere le richieste HTTP</b> dall'utente (tramite browser)</li>
 *   <li><b>Coordinare la logica di business</b> chiamando i Service appropriati</li>
 *   <li><b>Preparare i dati</b> per la visualizzazione (Model)</li>
 *   <li><b>Selezionare la vista</b> (View) da mostrare all'utente</li>
 * </ul>
 * 
 * <h3>@Controller vs @RestController</h3>
 * <p>
 * Questa classe usa <b>@Controller</b> perché:
 * <ul>
 *   <li>Restituisce <b>nomi di template HTML</b> (es. "index", "my-matches")</li>
 *   <li>I metodi popolano il <b>Model</b> con dati che verranno renderizzati in pagine HTML tramite Thymeleaf</li>
 *   <li>Gestisce una <b>applicazione web tradizionale</b> con server-side rendering</li>
 * </ul>
 * 
 * <p>
 * Al contrario, <b>@RestController</b> si usa per API REST che:
 * <ul>
 *   <li>Restituiscono direttamente <b>dati JSON/XML</b> (non HTML)</li>
 *   <li>Sono pensate per essere consumate da <b>client JavaScript</b> (React, Angular, ecc.) o app mobile</li>
 *   <li>Equivale a @Controller + @ResponseBody su ogni metodo</li>
 * </ul>
 * 
 * <h3>Dependency Injection con @RequiredArgsConstructor</h3>
 * <p>
 * L'annotazione Lombok <b>@RequiredArgsConstructor</b> genera automaticamente un costruttore
 * che inietta tutte le dipendenze dichiarate come <code>final</code>.
 * Questo è il modo consigliato per fare Dependency Injection in Spring, perché:
 * <ul>
 *   <li>Le dipendenze sono <b>immutabili</b> (final)</li>
 *   <li>Il codice è più <b>testabile</b> (possiamo passare mock nel costruttore nei test)</li>
 *   <li>Evita l'uso di @Autowired sui campi (considerato meno sicuro)</li>
 * </ul>
 * 
 * @author PadelApp Team
 * @version 1.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {
    
    /**
     * Servizi iniettati tramite Dependency Injection (pattern IoC - Inversion of Control).
     * Ogni Service incapsula la logica di business per un dominio specifico.
     */
    private final MatchService matchService;
    private final UserService userService;
    private final RegistrationService registrationService;
    private final FeedbackService feedbackService;
    private final UserSessionService userSessionService;
    
    /**
     * Home page - Mostra le partite disponibili per l'utente corrente.
     * 
     * <h3>@GetMapping("/") - Gestione richieste GET</h3>
     * <p>
     * L'annotazione @GetMapping mappa le <b>richieste HTTP GET</b> all'URL "/" a questo metodo.
     * Le richieste GET sono usate per:
     * <ul>
     *   <li><b>Leggere/visualizzare</b> dati (idempotenti e sicure)</li>
     *   <li>Possono essere <b>bookmarked e condivise</b></li>
     *   <li>Vengono <b>cachate</b> dai browser</li>
     * </ul>
     * 
     * <h3>Il parametro Model</h3>
     * <p>
     * Il parametro <code>Model model</code> è fornito automaticamente da Spring.
     * È una <b>mappa chiave-valore</b> usata per passare dati dal Controller alla View:
     * <ul>
     *   <li>Il Controller aggiunge attributi con <code>model.addAttribute("nomeAttributo", valore)</code></li>
     *   <li>Il template Thymeleaf può accedere a questi dati con <code>${nomeAttributo}</code></li>
     * </ul>
     * 
     * <h3>Model Attributes e Thymeleaf Binding</h3>
     * <p>
     * Esempio di binding:
     * <pre>
     * model.addAttribute("currentUser", user);  // Nel Controller
     * ${currentUser.firstName}                   // Nel template Thymeleaf
     * </pre>
     * 
     * @param model Il Model di Spring per passare dati alla vista
     * @return Il nome del template Thymeleaf da renderizzare ("index.html")
     */
    @GetMapping("/")
    public String home(
            HttpSession session,
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "date") String sort,
            Model model) {
        // Verifica autenticazione
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // STEP 1: Ottieni tutte le partite (con filtro level opzionale)
        List<Match> allMatches;
        if (level != null && !level.isEmpty()) {
            try {
                Level levelEnum = Level.valueOf(level.toUpperCase());
                allMatches = matchService.getMatchesByLevel(levelEnum);
            } catch (IllegalArgumentException e) {
                // Level non valido, prendi tutte le partite
                log.warn("Invalid level filter: {}, falling back to all matches", level);
                allMatches = matchService.getAllMatches();
            }
        } else {
            allMatches = matchService.getAllMatches();
        }
        
        // STEP 2: Applica sempre l'ordinamento usando Strategy Pattern (anche se level era invalido)
        allMatches = matchService.sortMatches(allMatches, sort != null ? sort : "date");
        
        // STEP 3: Filtra solo partite disponibili (non già iscritto, stato WAITING/CONFIRMED)
        List<Match> availableMatches = allMatches.stream()
            .filter(m -> m.getStatus() == MatchStatus.WAITING || m.getStatus() == MatchStatus.CONFIRMED)
            .filter(m -> !registrationService.isUserRegistered(currentUser, m))
            .collect(java.util.stream.Collectors.toList());
        
        // STEP 4: Calcola player count per ogni partita
        java.util.Map<Long, Integer> playerCounts = new java.util.HashMap<>();
        for (Match match : availableMatches) {
            playerCounts.put(match.getId(), registrationService.getActiveRegistrationsCount(match));
        }
        
        // STEP 5: Passa dati al template
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("availableMatches", availableMatches);
        model.addAttribute("playerCounts", playerCounts);
        model.addAttribute("level", level);
        model.addAttribute("sort", sort);
        
        return "index";
    }
    
    /**
     * Pagina "Le mie partite" - Mostra partite a cui l'utente è iscritto e partite già giocate.
     * 
     * <h3>@Transactional(readOnly = true) - Gestione Lazy Loading</h3>
     * <p>
     * Questa annotazione è <b>fondamentale</b> quando si lavora con JPA/Hibernate e relazioni lazy:
     * <ul>
     *   <li><b>Apre una transazione</b> per tutta la durata del metodo</li>
     *   <li>Permette il <b>lazy loading</b> delle relazioni (es. Match.registrations) anche nel template</li>
     *   <li><b>readOnly = true</b> ottimizza le performance indicando che non ci saranno modifiche al database</li>
     *   <li>Evita l'errore <b>"LazyInitializationException"</b> quando Thymeleaf accede a relazioni lazy</li>
     * </ul>
     * 
     * <p>
     * <b>Problema senza @Transactional:</b>
     * <pre>
     * // Nel Service: la transazione si chiude quando ritorna
     * Match match = matchRepository.findById(id);
     * // Nel Controller/Template: transazione chiusa!
     * match.getRegistrations() // ❌ LazyInitializationException!
     * </pre>
     * 
     * <p>
     * <b>Soluzione con @Transactional:</b>
     * <pre>
     * // La transazione rimane aperta per tutto il metodo del Controller
     * // e anche durante il rendering del template
     * match.getRegistrations() // ✅ Funziona!
     * </pre>
     * 
     * @param model Il Model per passare dati alla vista
     * @return Il nome del template "my-matches.html"
     */
    @GetMapping("/my-matches")
    @Transactional(readOnly = true)
    public String myMatches(
            HttpSession session,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "date") String sort,
            Model model) {
        // Verifica autenticazione
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // STEP 1: Ottieni SOLO le partite con registration JOINED (esclude CANCELLED)
        // Fix Bug: usare getActiveRegistrationsByUser() per evitare duplicati UI con registrations CANCELLED
        List<Match> allMyMatches = registrationService.getActiveRegistrationsByUser(currentUser).stream()
            .map(com.example.padel_app.model.Registration::getMatch)
            .collect(java.util.stream.Collectors.toList());
        
        // STEP 2: Dividi tra registrate (non terminate) e finite
        List<Match> myRegisteredMatches = allMyMatches.stream()
            .filter(m -> m.getStatus() != MatchStatus.FINISHED)
            .collect(java.util.stream.Collectors.toList());
        
        List<Match> myFinishedMatches = allMyMatches.stream()
            .filter(m -> m.getStatus() == MatchStatus.FINISHED)
            .collect(java.util.stream.Collectors.toList());
        
        // STEP 3: Applica filtro per stato (opzionale) SOLO alla sezione pertinente
        if (status != null && !status.isEmpty()) {
            try {
                MatchStatus statusEnum = MatchStatus.valueOf(status.toUpperCase());
                
                // Se filtro per FINISHED: myFinishedMatches contiene già solo FINISHED, registered rimane intatta
                if (statusEnum == MatchStatus.FINISHED) {
                    // Non serve filtrare myFinishedMatches (già solo FINISHED dallo STEP 2)
                    // myRegisteredMatches rimane invariata → mostra partite attive
                }
                // Se filtro per WAITING/CONFIRMED: filtra SOLO registeredMatches, lascia finished intatta
                else {
                    myRegisteredMatches = myRegisteredMatches.stream()
                        .filter(m -> m.getStatus() == statusEnum)
                        .collect(java.util.stream.Collectors.toList());
                    // myFinishedMatches rimane invariata → mostra partite terminate
                }
            } catch (IllegalArgumentException e) {
                // Status non valido, ignora filtro
                log.warn("Invalid status filter: {}, showing all matches", status);
            }
        }
        
        // STEP 4: Applica ordinamento usando Strategy Pattern a entrambe le liste
        myRegisteredMatches = matchService.sortMatches(myRegisteredMatches, sort);
        myFinishedMatches = matchService.sortMatches(myFinishedMatches, sort);
        
        // STEP 5: Calcola contatori giocatori
        java.util.Map<Long, Integer> playerCounts = new java.util.HashMap<>();
        for (Match match : myRegisteredMatches) {
            playerCounts.put(match.getId(), registrationService.getActiveRegistrationsCount(match));
        }
        for (Match match : myFinishedMatches) {
            playerCounts.put(match.getId(), registrationService.getAllRegistrationsCount(match));
        }
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("registeredMatches", myRegisteredMatches);
        model.addAttribute("finishedMatches", myFinishedMatches);
        model.addAttribute("playerCounts", playerCounts);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        
        return "my-matches";
    }
    
    /**
     * Pagina elenco partite con filtri e ordinamento.
     * 
     * <h3>@RequestParam - Parametri della Query String</h3>
     * <p>
     * L'annotazione @RequestParam estrae i parametri dalla query string dell'URL:
     * <ul>
     *   <li>URL: <code>/matches?level=INTERMEDIO&sort=date</code></li>
     *   <li><code>@RequestParam String level</code> → estrae "INTERMEDIO"</li>
     *   <li><code>required = false</code> → il parametro è opzionale</li>
     *   <li><code>defaultValue = "date"</code> → valore di default se il parametro non è presente</li>
     * </ul>
     * 
     * @param level Livello di filtro opzionale (PRINCIPIANTE, INTERMEDIO, AVANZATO, PROFESSIONISTA)
     * @param sort Criterio di ordinamento (date, level, popularity)
     * @param model Il Model per passare dati alla vista
     * @return Il nome del template "matches.html"
     */
    @GetMapping("/matches")
    public String matches(
            HttpSession session,
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "date") String sort,
            Model model) {
        
        // Verifica autenticazione
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Match> matches;
        
        // STEP 1: Applica filtro per livello se fornito
        if (level != null && !level.isEmpty()) {
            try {
                Level levelEnum = Level.valueOf(level.toUpperCase());
                matches = matchService.getMatchesByLevel(levelEnum);
            } catch (IllegalArgumentException e) {
                // Level non valido, prendi tutte le partite
                log.warn("Invalid level filter: {}, falling back to all matches", level);
                matches = matchService.getAllMatches();
            }
        } else {
            matches = matchService.getAllMatches();
        }
        
        // STEP 2: Applica sempre ordinamento usando Strategy Pattern (anche se level era invalido)
        matches = matchService.sortMatches(matches, sort != null ? sort : "date");
        
        model.addAttribute("matches", matches);
        model.addAttribute("level", level);
        model.addAttribute("sort", sort);
        
        return "matches";
    }
    
    /**
     * Pagina elenco utenti.
     * 
     * @param model Il Model per passare dati alla vista
     * @return Il nome del template "users.html"
     */
    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        // Verifica autenticazione
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users";
    }
    
    /**
     * Form di creazione partita (visualizzazione del form vuoto).
     * 
     * <p>
     * Questo metodo gestisce la richiesta GET per mostrare il form.
     * Prepara:
     * <ul>
     *   <li>Un oggetto DTO vuoto per il data binding del form</li>
     *   <li>I valori possibili per le select (enum Level)</li>
     * </ul>
     * 
     * @param model Il Model per passare dati alla vista
     * @return Il nome del template "create-match.html"
     */
    @GetMapping("/matches/create")
    public String createMatchForm(HttpSession session, Model model) {
        // Verifica autenticazione
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("matchRequest", new CreateMatchRequest());
        model.addAttribute("levels", Level.values());
        return "create-match";
    }
    
    /**
     * Creazione di una nuova partita (submit del form).
     * 
     * <h3>@PostMapping - Gestione richieste POST</h3>
     * <p>
     * L'annotazione @PostMapping mappa le <b>richieste HTTP POST</b> a questo metodo.
     * Le richieste POST sono usate per:
     * <ul>
     *   <li><b>Creare o modificare</b> dati sul server</li>
     *   <li>Inviare <b>dati di form</b> (non visibili nell'URL)</li>
     *   <li>Operazioni <b>non idempotenti</b> (ripetere la richiesta può creare duplicati)</li>
     *   <li><b>Non vengono cachate</b> dai browser</li>
     * </ul>
     * 
     * <h3>@GetMapping vs @PostMapping</h3>
     * <table border="1">
     *   <tr>
     *     <th>Aspetto</th>
     *     <th>@GetMapping (GET)</th>
     *     <th>@PostMapping (POST)</th>
     *   </tr>
     *   <tr>
     *     <td>Scopo</td>
     *     <td>Leggere/visualizzare dati</td>
     *     <td>Creare/modificare dati</td>
     *   </tr>
     *   <tr>
     *     <td>Parametri</td>
     *     <td>Visibili nell'URL (?key=value)</td>
     *     <td>Nel body della richiesta (nascosti)</td>
     *   </tr>
     *   <tr>
     *     <td>Idempotenza</td>
     *     <td>✅ Idempotente (ripetibile senza effetti collaterali)</td>
     *     <td>❌ Non idempotente (può creare duplicati)</td>
     *   </tr>
     *   <tr>
     *     <td>Caching</td>
     *     <td>✅ Può essere cachata</td>
     *     <td>❌ Mai cachata</td>
     *   </tr>
     *   <tr>
     *     <td>Bookmark</td>
     *     <td>✅ Può essere salvata nei preferiti</td>
     *     <td>❌ Non può essere salvata</td>
     *   </tr>
     * </table>
     * 
     * <h3>@Transactional - Gestione Transazioni</h3>
     * <p>
     * Senza <code>readOnly = true</code>, questa transazione è in <b>modalità scrittura</b>:
     * <ul>
     *   <li>Garantisce <b>ACID</b> (Atomicity, Consistency, Isolation, Durability)</li>
     *   <li>Se c'è un'eccezione, <b>rollback automatico</b> di tutte le modifiche</li>
     *   <li>Tutte le operazioni nel metodo sono <b>una singola unità atomica</b></li>
     * </ul>
     * 
     * <h3>Pattern POST-Redirect-GET</h3>
     * <p>
     * Dopo un POST di successo, si fa un <b>redirect</b> (non un return diretto al template):
     * <ul>
     *   <li><code>return "redirect:/my-matches"</code> → Browser fa una nuova richiesta GET</li>
     *   <li>Evita il <b>problema del "double submit"</b> (ricaricando la pagina non si ricrea la partita)</li>
     *   <li>Migliora l'<b>esperienza utente</b> e la <b>navigabilità</b></li>
     * </ul>
     * 
     * @param request DTO con i dati del form (Spring fa automaticamente il binding)
     * @param model Il Model per eventuali errori da mostrare
     * @return Redirect alla pagina "my-matches" in caso di successo, o rimostra il form in caso di errore
     */
    @PostMapping("/matches/create")
    @Transactional
    public String createMatch(HttpSession session, CreateMatchRequest request, Model model) {
        try {
            User currentUser = userSessionService.getCurrentUser(session);
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            // Crea l'oggetto Match dai dati del form
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
            
            // Auto-iscrizione del creatore alla partita
            registrationService.joinMatch(currentUser, savedMatch);
            
            // Pattern POST-Redirect-GET: dopo un POST con successo, fai redirect
            return "redirect:/my-matches";
            
        } catch (Exception e) {
            // In caso di errore, rimostra il form con messaggio di errore
            model.addAttribute("error", "Errore nella creazione della partita: " + e.getMessage());
            model.addAttribute("matchRequest", request);
            model.addAttribute("levels", Level.values());
            return "create-match";
        }
    }
    
    /**
     * Iscrizione a una partita esistente.
     * 
     * <h3>@PathVariable - Variabili nel Path dell'URL</h3>
     * <p>
     * L'annotazione @PathVariable estrae variabili dal path dell'URL:
     * <ul>
     *   <li>Mapping: <code>@PostMapping("/matches/{id}/join")</code></li>
     *   <li>URL richiesta: <code>/matches/42/join</code></li>
     *   <li><code>@PathVariable Long id</code> → estrae 42</li>
     * </ul>
     * 
     * <h3>RedirectAttributes - Flash Messages</h3>
     * <p>
     * <code>RedirectAttributes</code> permette di passare messaggi temporanei tra un redirect:
     * <ul>
     *   <li><code>redirectAttributes.addFlashAttribute("success", "Messaggio")</code> 
     *       → Il messaggio sopravvive al redirect ma viene rimosso dopo essere stato letto una volta</li>
     *   <li>Utile per mostrare <b>messaggi di conferma</b> dopo operazioni POST</li>
     *   <li>I flash attributes sono memorizzati in <b>sessione temporaneamente</b></li>
     *   <li>Dopo il redirect e il rendering, vengono <b>automaticamente cancellati</b></li>
     * </ul>
     * 
     * <p>
     * <b>Flusso completo con Flash Messages:</b>
     * <pre>
     * 1. POST /matches/42/join
     * 2. Controller: redirectAttributes.addFlashAttribute("success", "Iscritto!")
     * 3. Redirect 302 → GET /
     * 4. Template Thymeleaf: ${success} → mostra "Iscritto!"
     * 5. Reload della pagina: ${success} → null (messaggio rimosso)
     * </pre>
     * 
     * @param id ID della partita a cui iscriversi (estratto dall'URL)
     * @param redirectAttributes Per passare messaggi flash tra redirect
     * @return Redirect alla home page
     */
    @PostMapping("/matches/{id}/join")
    public String joinMatch(HttpSession session, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userSessionService.getCurrentUser(session);
            if (currentUser == null) {
                return "redirect:/login";
            }
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            
            registrationService.joinMatch(currentUser, match);
            
            // Flash message di successo: sopravvive al redirect, poi viene rimosso
            redirectAttributes.addFlashAttribute("success", "Ti sei iscritta alla partita!");
            
        } catch (Exception e) {
            // Flash message di errore
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/";
    }
    
    /**
     * Disiscrizione da una partita esistente.
     * 
     * <p>
     * Questo metodo gestisce due scenari:
     * <ul>
     *   <li>Se l'utente è il <b>creatore</b> della partita → elimina l'intera partita</li>
     *   <li>Se l'utente è un <b>partecipante</b> → lo disiscrive dalla partita</li>
     * </ul>
     * 
     * @param id ID della partita da cui disiscriversi
     * @param redirectAttributes Per messaggi flash di conferma/errore
     * @return Redirect alla pagina "my-matches"
     */
    @PostMapping("/matches/{id}/leave")
    public String leaveMatch(HttpSession session, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userSessionService.getCurrentUser(session);
            if (currentUser == null) {
                return "redirect:/login";
            }
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            
            // Controlla se l'utente è il creatore della partita
            boolean isCreator = match.getCreator() != null && match.getCreator().getId().equals(currentUser.getId());
            
            registrationService.leaveMatch(currentUser, match);
            
            if (isCreator) {
                redirectAttributes.addFlashAttribute("success", "Partita eliminata con successo (eri il creatore).");
            } else {
                redirectAttributes.addFlashAttribute("success", "Ti sei disiscritto dalla partita!");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/my-matches";
    }
    
    /**
     * Termina una partita (cambia stato a FINISHED).
     * 
     * <p>
     * Solo dopo che una partita è terminata, i giocatori possono lasciare feedback
     * sul livello percepito degli altri partecipanti.
     * 
     * <p>
     * <b>Autorizzazione:</b> Solo il creatore della partita può terminarla.
     * 
     * @param id ID della partita da terminare
     * @param redirectAttributes Per messaggi flash
     * @return Redirect alla pagina "my-matches"
     */
    @PostMapping("/matches/{id}/finish")
    public String finishMatch(HttpSession session,
                             @PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            // Verifica autenticazione
            User currentUser = userSessionService.getCurrentUser(session);
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            // Verifica che l'utente sia il creatore della partita
            Match match = matchService.getMatchById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
            
            if (match.getCreator() == null || !match.getCreator().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Solo il creatore della partita può terminarla.");
                return "redirect:/my-matches";
            }
            
            matchService.finishMatch(id);
            redirectAttributes.addFlashAttribute("success", "Partita terminata! Puoi dare feedback ai giocatori.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/my-matches";
    }
    
    /**
     * Form di feedback per una partita terminata.
     * 
     * <p>
     * Mostra la lista dei giocatori a cui l'utente corrente può ancora dare feedback.
     * Esclude:
     * <ul>
     *   <li>L'utente corrente stesso (non può valutare se stesso)</li>
     *   <li>I giocatori già valutati in precedenza</li>
     * </ul>
     * 
     * <p>
     * Usa <code>@Transactional(readOnly = true)</code> perché nel template Thymeleaf
     * potrebbero essere accesse relazioni lazy (es. match.registrations, feedback.targetUser).
     * 
     * @param id ID della partita per cui dare feedback
     * @param model Il Model per passare dati alla vista
     * @return Il nome del template "feedback.html"
     */
    @GetMapping("/matches/{id}/feedback")
    @Transactional(readOnly = true)
    public String feedbackForm(HttpSession session, @PathVariable Long id, Model model) {
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        Match match = matchService.getMatchById(id)
            .orElseThrow(() -> new IllegalArgumentException("Partita non trovata"));
        
        // Verifica che la partita sia terminata
        if (match.getStatus() != MatchStatus.FINISHED) {
            model.addAttribute("error", "Puoi dare feedback solo a partite terminate");
            return "redirect:/my-matches";
        }
        
        // Recupera i feedback già dati dall'utente corrente per questa partita
        List<com.example.padel_app.model.Feedback> feedbacksGiven = 
            feedbackService.getFeedbacksByAuthorAndMatch(currentUser, match);
        
        // Crea un Set degli ID degli utenti già valutati
        java.util.Set<Long> alreadyRatedUserIds = feedbacksGiven.stream()
            .map(f -> f.getTargetUser().getId())
            .collect(java.util.stream.Collectors.toSet());
        
        // Filtra i giocatori disponibili per il feedback
        // BUG FIX: Include SOLO i partecipanti JOINED (esclude CANCELLED) per feedback
        // Mostra TUTTI gli altri giocatori che hanno effettivamente giocato, non solo il creator
        List<User> availablePlayers = registrationService.getActiveRegistrationsByMatch(match).stream()
            .map(com.example.padel_app.model.Registration::getUser)
            .filter(u -> !u.getId().equals(currentUser.getId()))  // Esclude se stesso
            .filter(u -> !alreadyRatedUserIds.contains(u.getId()))  // Esclude già valutati
            .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("match", match);
        model.addAttribute("players", availablePlayers);
        model.addAttribute("levels", Level.values());
        model.addAttribute("allRated", availablePlayers.isEmpty());
        
        return "feedback";
    }
    
    /**
     * Invio di un feedback per un giocatore.
     * 
     * <h3>@RequestParam con required = false</h3>
     * <p>
     * Il parametro <code>comment</code> è opzionale:
     * <ul>
     *   <li><code>@RequestParam(required = false) String comment</code></li>
     *   <li>Se non fornito nel form → <code>comment</code> sarà <code>null</code></li>
     *   <li>Il codice gestisce il null: <code>comment != null ? comment : ""</code></li>
     * </ul>
     * 
     * @param id ID della partita
     * @param targetUserId ID dell'utente da valutare
     * @param suggestedLevel Livello percepito suggerito
     * @param comment Commento opzionale
     * @param redirectAttributes Per messaggi flash
     * @return Redirect al form di feedback (per valutare altri giocatori)
     */
    @PostMapping("/matches/{id}/feedback")
    public String submitFeedback(HttpSession session,
                                 @PathVariable Long id,
                                 @RequestParam Long targetUserId,
                                 @RequestParam String suggestedLevel,
                                 @RequestParam(required = false) String comment,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userSessionService.getCurrentUser(session);
            if (currentUser == null) {
                return "redirect:/login";
            }
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
    
    /**
     * Pagina profilo utente - Mostra statistiche e feedback.
     * 
     * <p>
     * Questa pagina mostra:
     * <ul>
     *   <li>Feedback ricevuti dall'utente</li>
     *   <li>Feedback dati dall'utente</li>
     *   <li>Statistiche: media del livello percepito, distribuzione per livello</li>
     * </ul>
     * 
     * @param model Il Model per passare dati alla vista
     * @return Il nome del template "my-profile.html"
     */
    @GetMapping("/my-profile")
    public String myProfile(HttpSession session, Model model) {
        User currentUser = userSessionService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Recupera feedback ricevuti e dati
        List<com.example.padel_app.model.Feedback> feedbackReceived = 
            feedbackService.getFeedbacksByTargetUser(currentUser);
        List<com.example.padel_app.model.Feedback> feedbackGiven = 
            feedbackService.getFeedbacksByAuthor(currentUser);
        
        // Calcola la media del livello percepito dai feedback ricevuti
        Double averageLevel = feedbackReceived.isEmpty() ? null : 
            feedbackReceived.stream()
                .mapToInt(f -> {
                    switch(f.getSuggestedLevel()) {
                        case PRINCIPIANTE: return 1;
                        case INTERMEDIO: return 2;
                        case AVANZATO: return 3;
                        case PROFESSIONISTA: return 4;
                        default: return 0;
                    }
                })
                .average()
                .orElse(0.0);
        
        // Calcola la distribuzione dei feedback per ogni livello
        long countPrincipiante = feedbackReceived.stream()
            .filter(f -> f.getSuggestedLevel() == Level.PRINCIPIANTE).count();
        long countIntermedio = feedbackReceived.stream()
            .filter(f -> f.getSuggestedLevel() == Level.INTERMEDIO).count();
        long countAvanzato = feedbackReceived.stream()
            .filter(f -> f.getSuggestedLevel() == Level.AVANZATO).count();
        long countProfessionista = feedbackReceived.stream()
            .filter(f -> f.getSuggestedLevel() == Level.PROFESSIONISTA).count();
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("feedbackReceived", feedbackReceived);
        model.addAttribute("feedbackGiven", feedbackGiven);
        model.addAttribute("averageLevel", averageLevel);
        model.addAttribute("countPrincipiante", countPrincipiante);
        model.addAttribute("countIntermedio", countIntermedio);
        model.addAttribute("countAvanzato", countAvanzato);
        model.addAttribute("countProfessionista", countProfessionista);
        model.addAttribute("levels", Level.values());
        
        return "my-profile";
    }
    
    /**
     * Aggiornamento del livello dichiarato dall'utente.
     * 
     * @param declaredLevel Il nuovo livello dichiarato
     * @param redirectAttributes Per messaggi flash
     * @return Redirect alla pagina profilo
     */
    @PostMapping("/my-profile/update-level")
    public String updateDeclaredLevel(HttpSession session,
                                      @RequestParam String declaredLevel,
                                      RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userSessionService.getCurrentUser(session);
            if (currentUser == null) {
                return "redirect:/login";
            }
            userService.updateDeclaredLevel(currentUser, Level.valueOf(declaredLevel));
            redirectAttributes.addFlashAttribute("success", "Livello dichiarato aggiornato con successo!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Errore nell'aggiornamento: " + e.getMessage());
        }
        
        return "redirect:/my-profile";
    }
    
    /**
     * DTO (Data Transfer Object) per il form di creazione partita.
     * 
     * <h3>Cos'è un DTO?</h3>
     * <p>
     * Un DTO è un oggetto semplice usato per trasferire dati tra livelli dell'applicazione:
     * <ul>
     *   <li>Nel nostro caso: tra il <b>form HTML</b> e il <b>Controller</b></li>
     *   <li>Contiene solo <b>dati</b> (niente logica di business)</li>
     *   <li>Ha solo getter e setter</li>
     * </ul>
     * 
     * <h3>Perché non usare direttamente l'entità Match?</h3>
     * <ul>
     *   <li>Il form ha dati in <b>formato diverso</b> (es. dateTime come String, non LocalDateTime)</li>
     *   <li>Il form potrebbe avere solo <b>alcuni campi</b> dell'entità</li>
     *   <li>Separare DTO da entità aumenta la <b>sicurezza</b> (evita mass assignment vulnerabilities)</li>
     * </ul>
     * 
     * <h3>Spring Data Binding</h3>
     * <p>
     * Spring fa automaticamente il binding dei campi del form ai campi del DTO:
     * <pre>
     * &lt;input name="location" /&gt;      → request.setLocation(...)
     * &lt;input name="description" /&gt;   → request.setDescription(...)
     * &lt;select name="requiredLevel"&gt; → request.setRequiredLevel(...)
     * </pre>
     */
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
