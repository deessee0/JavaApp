package com.example.padel_app.service;

import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.FeedbackRepository;
import com.example.padel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FeedbackService - Service per gestione feedback tra giocatori
 * 
 * RESPONSABILITÀ BUSINESS LOGIC:
 * Questo service gestisce il sistema di valutazione tra giocatori dopo le partite.
 * Ogni giocatore può lasciare feedback agli altri partecipanti suggerendo il loro
 * livello tecnico percepito durante la partita.
 * 
 * FLUSSO PRINCIPALE:
 * 1. Partita completata → giocatori possono lasciare feedback
 * 2. Ogni giocatore valuta gli altri (1 feedback per coppia giocatore-match)
 * 3. Sistema calcola livello percepito in base a feedback ricevuti
 * 4. Livello percepito aiuta a creare partite bilanciate
 * 
 * CONCETTI SPRING DIMOSTRATI:
 * 
 * 1. @Service - Componente business logic
 *    - Marca classe come service layer Spring
 *    - Auto-scansionato da component scan
 *    - Iniettabile in controller e altri service
 * 
 * 2. @Transactional - Gestione transazioni database
 *    - readOnly=true (default classe): ottimizzazione per query read-only
 *    - @Transactional su metodi write: garantisce ACID properties
 *    - Se errore → rollback automatico (nessun dato parziale salvato)
 * 
 * 3. @RequiredArgsConstructor (Lombok)
 *    - Genera costruttore con parametri per campi final
 *    - Spring usa costruttore per dependency injection
 *    - Equivalente a: public FeedbackService(FeedbackRepository repo, UserRepository userRepo) {...}
 * 
 * 4. @Slf4j (Lombok)
 *    - Genera logger automaticamente: log.info(), log.warn(), log.debug()
 *    - Utile per debugging e monitoraggio applicazione
 * 
 * ALGORITMO CALCOLO PERCEIVED LEVEL:
 * Il livello percepito è calcolato come media matematica dei feedback ricevuti.
 * 
 * Conversione enum → numero tramite ordinal():
 * - PRINCIPIANTE (ordinal = 0) → valore 0
 * - INTERMEDIO (ordinal = 1) → valore 1
 * - AVANZATO (ordinal = 2) → valore 2
 * - PROFESSIONISTA (ordinal = 3) → valore 3
 * 
 * Nota: la documentazione parla di valori 1-4, ma il codice usa ordinal() che parte da 0.
 * Questo funziona comunque perché manteniamo la stessa scala per conversione e riconversione.
 * 
 * Esempio calcolo:
 * - Giocatore riceve feedback: [INTERMEDIO, AVANZATO, INTERMEDIO]
 * - Conversione ordinal: [1, 2, 1]
 * - Media: (1 + 2 + 1) / 3 = 1.33
 * - Arrotondamento: Math.round(1.33) = 1
 * - Level.values()[1] = INTERMEDIO
 * 
 * Altro esempio:
 * - Feedback ricevuti: [AVANZATO, PROFESSIONISTA, AVANZATO]
 * - Ordinal: [2, 3, 2]
 * - Media: (2 + 3 + 2) / 3 = 2.33
 * - Arrotondamento: Math.round(2.33) = 2
 * - Risultato: AVANZATO
 * 
 * Questo algoritmo permette di:
 * - Valutazione oggettiva basata su opinioni multiple
 * - Aggiustamento automatico livello con più partite
 * - Rilevare discrepanze tra declaredLevel (auto-dichiarato) e perceivedLevel (da altri)
 * 
 * VALIDAZIONE UNICITÀ:
 * Un giocatore può lasciare UN SOLO feedback per ogni coppia (targetUser, match).
 * Previene duplicati e manipolazione del sistema di rating.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    
    /**
     * Crea un nuovo feedback dopo una partita
     * 
     * BUSINESS LOGIC:
     * Permette a un giocatore (author) di valutare un altro giocatore (targetUser)
     * suggerendo il livello tecnico osservato durante una specifica partita (match).
     * 
     * VALIDAZIONE UNICITÀ:
     * Prima di creare il feedback, verifica che non esista già un feedback
     * con la stessa tripla (author, targetUser, match).
     * 
     * Esempio scenario:
     * - Match #5: Alice, Bob, Charlie, Diana giocano insieme
     * - Alice può lasciare feedback a: Bob, Charlie, Diana (max 3 feedback)
     * - Alice NON può lasciare 2 feedback a Bob per lo stesso match
     * 
     * TRANSACTIONAL:
     * @Transactional (senza readOnly) apre transazione write.
     * Se qualsiasi operazione fallisce → rollback completo:
     * - Feedback non salvato
     * - perceivedLevel non aggiornato
     * - Database rimane consistente
     * 
     * SIDE EFFECT IMPORTANTE:
     * Dopo salvataggio feedback, chiama updatePerceivedLevel() per ricalcolare
     * automaticamente il livello percepito del targetUser.
     * 
     * @param author Utente che scrive il feedback (chi valuta)
     * @param targetUser Utente che riceve il feedback (chi viene valutato)
     * @param match Partita in cui è avvenuta l'interazione
     * @param suggestedLevel Livello tecnico suggerito (osservazione durante partita)
     * @param comment Commento testuale opzionale (es: "Ottimo diritto, serve da migliorare")
     * @return Feedback salvato con ID generato
     * @throws RuntimeException se feedback già esistente per questa tripla
     * 
     * Esempio uso nel controller:
     * <pre>
     * User alice = userContext.getCurrentUser();
     * User bob = userService.getUserById(bobId).orElseThrow();
     * Match match = matchService.getMatchById(matchId).orElseThrow();
     * 
     * Feedback feedback = feedbackService.createFeedback(
     *     alice, bob, match, 
     *     Level.INTERMEDIO, 
     *     "Buon giocatore, movimento in campo da migliorare"
     * );
     * </pre>
     */
    @Transactional
    public Feedback createFeedback(User author, User targetUser, Match match, Level suggestedLevel, String comment) {
        // VALIDAZIONE: verifica che feedback non esista già
        Optional<Feedback> existing = feedbackRepository.findByAuthorAndTargetUserAndMatch(author, targetUser, match);
        if (existing.isPresent()) {
            throw new RuntimeException("Feedback already exists for this user and match");
        }
        
        // CREAZIONE ENTITÀ: costruisce oggetto Feedback con dati forniti
        Feedback feedback = new Feedback();
        feedback.setAuthor(author);
        feedback.setTargetUser(targetUser);
        feedback.setMatch(match);
        feedback.setSuggestedLevel(suggestedLevel);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        
        // PERSISTENZA: salva nel database (INSERT)
        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback created by {} for {} on match {}", 
                 author.getUsername(), targetUser.getUsername(), match.getId());
        
        // SIDE EFFECT: aggiorna perceived level del target user
        // Questo garantisce che il livello sia sempre aggiornato dopo ogni nuovo feedback
        updatePerceivedLevel(targetUser.getId());
        
        return saved;
    }
    
    /**
     * Aggiorna il livello percepito di un utente basato sui feedback ricevuti
     * 
     * ALGORITMO CALCOLO PERCEIVED LEVEL:
     * 
     * 1. Recupera tutti i feedback ricevuti dall'utente
     * 2. Converte ogni suggestedLevel in numero usando ordinal():
     *    - PRINCIPIANTE (ordinal=0) → valore 0
     *    - INTERMEDIO (ordinal=1) → valore 1
     *    - AVANZATO (ordinal=2) → valore 2
     *    - PROFESSIONISTA (ordinal=3) → valore 3
     * 3. Calcola media aritmetica
     * 4. Arrotonda al livello più vicino con Math.round()
     * 5. Converte indice → Level usando Level.values()[index]
     * 
     * Esempio concreto:
     * - Bob ha ricevuto 5 feedback: [INTERMEDIO, AVANZATO, INTERMEDIO, AVANZATO, PROFESSIONISTA]
     * - Conversione ordinal: [1, 2, 1, 2, 3]
     * - Media: (1+2+1+2+3)/5 = 1.8
     * - Arrotondamento: Math.round(1.8) = 2
     * - Level.values()[2] = AVANZATO
     * - Bob.perceivedLevel = AVANZATO
     * 
     * CASI PARTICOLARI:
     * - Nessun feedback: perceived level rimane invariato (non viene aggiornato)
     * - 1 solo feedback: perceived level = suggestedLevel di quel feedback
     * - Utente non trovato: log warning, nessuna modifica
     * 
     * TRANSACTIONAL:
     * Metodo write che modifica User.perceivedLevel.
     * In caso di errore durante salvataggio → rollback automatico.
     * 
     * INVOCAZIONE AUTOMATICA:
     * Chiamato automaticamente da createFeedback() dopo ogni nuovo feedback.
     * Garantisce che perceivedLevel sia sempre sincronizzato con feedback ricevuti.
     * 
     * @param userId ID dell'utente di cui aggiornare il perceived level
     * 
     * Esempio comportamento completo:
     * <pre>
     * // Stato iniziale Bob
     * bob.declaredLevel = INTERMEDIO (auto-dichiarato)
     * bob.perceivedLevel = null (nessun feedback ancora)
     * 
     * // Alice lascia feedback: AVANZATO
     * feedbackService.createFeedback(alice, bob, match1, AVANZATO, "Molto forte");
     * // → updatePerceivedLevel chiamato automaticamente
     * // → bob.perceivedLevel = AVANZATO (1 solo feedback)
     * 
     * // Charlie lascia feedback: INTERMEDIO
     * feedbackService.createFeedback(charlie, bob, match2, INTERMEDIO, "Buon livello");
     * // → bob.perceivedLevel = INTERMEDIO (media di AVANZATO e INTERMEDIO)
     * 
     * // Sistema ora sa: Bob si dichiara INTERMEDIO, altri lo vedono tra INTERMEDIO-AVANZATO
     * </pre>
     */
    @Transactional
    public void updatePerceivedLevel(Long userId) {
        // RECUPERO UTENTE: cerca user nel database
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found, cannot update perceived level", userId);
            return;
        }
        
        User user = userOpt.get();
        
        // RECUPERO FEEDBACK: tutti i feedback ricevuti da questo utente
        List<Feedback> feedbacks = feedbackRepository.findByTargetUser(user);
        
        // CASO BASE: nessun feedback ricevuto
        if (feedbacks.isEmpty()) {
            log.debug("No feedbacks for user {}, perceived level unchanged", userId);
            return;
        }
        
        // CALCOLO MEDIA: converti enum → int → calcola media
        // Stream API:
        // 1. feedbacks.stream(): crea stream da lista
        // 2. .mapToInt(f -> f.getSuggestedLevel().ordinal()): estrae ordinal di ogni level
        // 3. .average(): calcola media aritmetica
        // 4. .orElse(0): default 0 se stream vuoto (impossibile qui, ma safety check)
        double avgLevel = feedbacks.stream()
            .mapToInt(f -> f.getSuggestedLevel().ordinal())
            .average()
            .orElse(0);
        
        // ARROTONDAMENTO: converte media decimale in indice intero
        // Math.round(1.8) = 2, Math.round(2.5) = 3
        int levelIndex = (int) Math.round(avgLevel);
        
        // CONVERSIONE: indice → enum Level
        // Level.values() = [PRINCIPIANTE, INTERMEDIO, AVANZATO, PROFESSIONISTA]
        // levelIndex=0 → PRINCIPIANTE, levelIndex=2 → AVANZATO, etc.
        Level perceivedLevel = Level.values()[levelIndex];
        
        // AGGIORNAMENTO: salva nuovo perceived level
        user.setPerceivedLevel(perceivedLevel);
        userRepository.save(user);
        
        log.info("Updated perceived level for user {} to {} (based on {} feedbacks)", 
                 user.getUsername(), perceivedLevel, feedbacks.size());
    }
    
    /**
     * Recupera tutti i feedback ricevuti da un utente
     * 
     * BUSINESS LOGIC:
     * Usato per mostrare pagina profilo utente, sezione "Cosa dicono di me".
     * 
     * Esempio:
     * <pre>
     * List<Feedback> bobReviews = feedbackService.getFeedbacksByTargetUser(bob);
     * // Mostra tutti i commenti e valutazioni che Bob ha ricevuto
     * </pre>
     * 
     * @param user Utente di cui recuperare i feedback ricevuti
     * @return Lista feedback ricevuti (può essere vuota se nessuno ha ancora valutato)
     */
    public List<Feedback> getFeedbacksByTargetUser(User user) {
        return feedbackRepository.findByTargetUser(user);
    }
    
    /**
     * Recupera tutti i feedback scritti da un utente
     * 
     * BUSINESS LOGIC:
     * Usato per mostrare "Feedback che ho lasciato" nel profilo utente.
     * 
     * Esempio:
     * <pre>
     * List<Feedback> aliceFeedbacks = feedbackService.getFeedbacksByAuthor(alice);
     * // Mostra tutti i feedback che Alice ha scritto per altri giocatori
     * </pre>
     * 
     * @param user Utente autore dei feedback
     * @return Lista feedback scritti dall'utente
     */
    public List<Feedback> getFeedbacksByAuthor(User user) {
        return feedbackRepository.findByAuthor(user);
    }
    
    /**
     * Recupera feedback scritti da un autore in una specifica partita
     * 
     * BUSINESS LOGIC:
     * Dopo una partita, mostra quali feedback un giocatore ha già lasciato.
     * Utile per sapere a chi manca ancora lasciare feedback.
     * 
     * Esempio scenario:
     * - Match #10: Alice, Bob, Charlie, Diana
     * - Alice ha già lasciato feedback a Bob e Charlie
     * - getFeedbacksByAuthorAndMatch(alice, match10) → [feedback_bob, feedback_charlie]
     * - Sistema sa che Alice deve ancora valutare Diana
     * 
     * @param author Autore dei feedback
     * @param match Partita specifica
     * @return Lista feedback lasciati dall'autore in quella partita
     */
    public List<Feedback> getFeedbacksByAuthorAndMatch(User author, Match match) {
        return feedbackRepository.findByAuthorAndMatch(author, match);
    }
    
    /**
     * Recupera tutti i feedback relativi a una partita
     * 
     * BUSINESS LOGIC:
     * Statistiche partita: quanti feedback sono stati scambiati.
     * Utile per metriche e analytics.
     * 
     * @param match Partita di interesse
     * @return Lista tutti i feedback relativi a quella partita
     */
    public List<Feedback> getFeedbacksByMatch(Match match) {
        return feedbackRepository.findByMatch(match);
    }
    
    /**
     * Cerca feedback specifico tra author, targetUser e match
     * 
     * BUSINESS LOGIC:
     * Prima di mostrare form "Lascia feedback", verifica se già esiste.
     * Se esiste → mostra "Hai già lasciato feedback"
     * Se non esiste → mostra form compilabile
     * 
     * Esempio uso nel controller:
     * <pre>
     * Optional<Feedback> existing = feedbackService.getFeedback(alice, bob, match5);
     * if (existing.isPresent()) {
     *     model.addAttribute("message", "Hai già lasciato feedback a questo giocatore");
     *     model.addAttribute("feedback", existing.get());
     * } else {
     *     model.addAttribute("showForm", true);
     * }
     * </pre>
     * 
     * @param author Autore feedback
     * @param targetUser Destinatario feedback
     * @param match Partita specifica
     * @return Optional contenente feedback se esiste, empty altrimenti
     */
    public Optional<Feedback> getFeedback(User author, User targetUser, Match match) {
        return feedbackRepository.findByAuthorAndTargetUserAndMatch(author, targetUser, match);
    }
}
