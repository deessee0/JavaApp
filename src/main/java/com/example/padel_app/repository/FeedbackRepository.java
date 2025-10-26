package com.example.padel_app.repository;

import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FeedbackRepository - Interfaccia per accesso dati Feedback
 * 
 * CONCETTI AVANZATI JPA DIMOSTRATI:
 * 
 * 1. JOIN FETCH MULTIPLI
 *    - Feedback ha 3 relazioni @ManyToOne: author, targetUser, match
 *    - JOIN FETCH carica tutte e 3 le relazioni in UN'UNICA query
 *    - Evita N+1 queries problem (1 query feedback + N query per ogni relazione)
 *    
 * 2. Optional<T> vs List<T>
 *    - Optional: quando ci aspettiamo 0 o 1 risultato
 *    - List: quando ci aspettiamo 0 o N risultati
 *    - Optional forza gestione caso "not found" e previene NullPointerException
 *    
 * 3. EXISTS QUERY
 *    - Verifica esistenza senza caricare entità completa
 *    - Più efficiente di findBy().isPresent()
 *    - SQL: SELECT EXISTS(SELECT 1 FROM feedbacks WHERE ...)
 *    
 * 4. AGGREGATE FUNCTIONS
 *    - AVG(), COUNT(), SUM(), MIN(), MAX()
 *    - Operano su insiemi di righe e restituiscono valore singolo
 *    - Utili per calcoli statistici (rating medio, totali, ecc.)
 *    
 * 5. CASE WHEN - CONVERSIONE ENUM → NUMERO
 *    - DB memorizza enum come stringa ('PRINCIPIANTE', 'INTERMEDIO', ...)
 *    - Per calcolare media serve conversione a numero
 *    - CASE WHEN simile a switch in Java
 *    - Permette calcoli matematici su valori categorici
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    /**
     * Trova tutti i feedback scritti da un autore specifico
     * 
     * JOIN FETCH MULTIPLI:
     * Carica in un'unica query:
     * 1. Feedback principale
     * 2. f.author (User che ha scritto il feedback)
     * 3. f.targetUser (User che ha ricevuto il feedback)
     * 4. f.match (Match in cui è avvenuto il feedback)
     * 
     * SQL generato (semplificato):
     *   SELECT f.*, a.*, tu.*, m.*
     *   FROM feedbacks f
     *   JOIN users a ON f.author_id = a.id
     *   JOIN users tu ON f.target_user_id = tu.id
     *   JOIN matches m ON f.match_id = m.id
     *   WHERE a.id = ?
     * 
     * Senza JOIN FETCH:
     * 1 query feedbacks + N query author + N query targetUser + N query match
     * = N+1 queries problem!
     * 
     * Con JOIN FETCH:
     * 1 query totale = molto più efficiente
     * 
     * Uso:
     *   User alice = userRepository.findByUsername("alice").get();
     *   List<Feedback> aliceFeedbacks = feedbackRepository.findByAuthor(alice);
     *   // Tutti i feedback scritti da Alice, con dati completi già caricati
     */
    @Query("SELECT f FROM Feedback f JOIN FETCH f.author JOIN FETCH f.targetUser JOIN FETCH f.match WHERE f.author = :author")
    List<Feedback> findByAuthor(User author);
    
    /**
     * Trova tutti i feedback ricevuti da un utente specifico
     * 
     * JOIN FETCH MULTIPLI con filtro su targetUser:
     * Carica tutti i feedback dove targetUser = parametro
     * Include author (chi ha scritto), targetUser (chi ha ricevuto), match (dove)
     * 
     * SQL: SELECT f.*, a.*, tu.*, m.*
     *      FROM feedbacks f
     *      JOIN users a ON f.author_id = a.id
     *      JOIN users tu ON f.target_user_id = tu.id
     *      JOIN matches m ON f.match_id = m.id
     *      WHERE tu.id = ?
     * 
     * Uso tipico: pagina profilo utente, sezione "Feedback ricevuti"
     * 
     * Esempio:
     *   User bob = userRepository.findByUsername("bob").get();
     *   List<Feedback> bobReviews = feedbackRepository.findByTargetUser(bob);
     *   // Mostra tutti i giudizi che Bob ha ricevuto da altri giocatori
     */
    @Query("SELECT f FROM Feedback f JOIN FETCH f.author JOIN FETCH f.targetUser JOIN FETCH f.match WHERE f.targetUser = :targetUser")
    List<Feedback> findByTargetUser(User targetUser);
    
    /**
     * Trova feedback scritti da un autore in un match specifico
     * 
     * JOIN FETCH con 2 condizioni WHERE:
     * - f.author = :author (chi ha scritto)
     * - f.match = :match (in quale partita)
     * 
     * SQL: WHERE f.author_id = ? AND f.match_id = ?
     * 
     * Uso: dopo una partita, mostrare tutti i feedback che un giocatore ha lasciato
     * 
     * Esempio:
     *   List<Feedback> aliceFeedbacksInMatch5 = 
     *       feedbackRepository.findByAuthorAndMatch(alice, match5);
     *   // Tutti i feedback che Alice ha scritto nella partita #5
     */
    @Query("SELECT f FROM Feedback f JOIN FETCH f.author JOIN FETCH f.targetUser JOIN FETCH f.match WHERE f.author = :author AND f.match = :match")
    List<Feedback> findByAuthorAndMatch(User author, Match match);
    
    /**
     * Trova tutti i feedback relativi a un match
     * 
     * DERIVED QUERY METHOD (senza JOIN FETCH):
     * Spring genera: SELECT * FROM feedbacks WHERE match_id = ?
     * 
     * Nota: questo metodo NON usa JOIN FETCH
     * Se accedi a feedback.author.getUsername() → potenziale LazyInitializationException
     * 
     * SQL: SELECT * FROM feedbacks WHERE match_id = ?
     * 
     * Uso: quando serve solo contare feedback, non accedere alle relazioni
     */
    List<Feedback> findByMatch(Match match);
    
    /**
     * Trova UN feedback specifico (author → targetUser in uno specifico match)
     * 
     * DERIVED QUERY METHOD + Optional<T>:
     * 
     * Optional<Feedback> invece di Feedback:
     * - Può essere vuoto (nessun feedback trovato)
     * - Forza gestione esplicita caso "not found"
     * - Metodi utili: isPresent(), orElse(), orElseThrow()
     * 
     * Spring genera:
     * SELECT * FROM feedbacks 
     * WHERE author_id = ? AND target_user_id = ? AND match_id = ?
     * 
     * Uso: verificare se Alice ha già lasciato feedback a Bob nella partita #5
     * 
     * Esempio:
     *   Optional<Feedback> existing = feedbackRepository
     *       .findByAuthorAndTargetUserAndMatch(alice, bob, match5);
     *   if (existing.isPresent()) {
     *       throw new Exception("Hai già lasciato feedback a questo giocatore!");
     *   }
     */
    Optional<Feedback> findByAuthorAndTargetUserAndMatch(User author, User targetUser, Match match);
    
    /**
     * Verifica se esiste già un feedback (controllo duplicati)
     * 
     * EXISTS QUERY - check esistenza senza caricare dati:
     * 
     * Confronto:
     * - findByAuthorAndTargetUserAndMatch(): carica TUTTA l'entità Feedback
     * - existsByAuthorAndTargetUserAndMatch(): solo true/false
     * 
     * SQL generato:
     *   SELECT EXISTS(
     *       SELECT 1 FROM feedbacks 
     *       WHERE author_id = ? AND target_user_id = ? AND match_id = ?
     *   )
     * 
     * Più efficiente quando serve solo sapere "esiste?" senza accedere ai dati
     * 
     * Uso nel service:
     *   if (feedbackRepository.existsByAuthorAndTargetUserAndMatch(alice, bob, match5)) {
     *       return "Feedback già presente";
     *   }
     *   // Procedi con creazione nuovo feedback
     */
    boolean existsByAuthorAndTargetUserAndMatch(User author, User targetUser, Match match);
    
    /**
     * Trova feedback ricevuti da un utente ordinati per data (più recente prima)
     * 
     * @Query CON ORDER BY timestamp:
     * DESC (descending): dal più recente al più vecchio
     * 
     * SQL: SELECT * FROM feedbacks 
     *      WHERE target_user_id = ? 
     *      ORDER BY created_at DESC
     * 
     * Uso: timeline feedback utente (più recenti in alto)
     * 
     * Esempio:
     *   List<Feedback> recentFeedbacks = 
     *       feedbackRepository.findByTargetUserOrderByCreatedAtDesc(bob);
     *   // Primo elemento = feedback più recente ricevuto da Bob
     */
    @Query("SELECT f FROM Feedback f WHERE f.targetUser = :targetUser ORDER BY f.createdAt DESC")
    List<Feedback> findByTargetUserOrderByCreatedAtDesc(User targetUser);
    
    /**
     * Calcola livello medio percepito di un utente (aggregate function)
     * 
     * AGGREGATE FUNCTION - AVG() + CASE WHEN:
     * 
     * Problema: suggestedLevel è enum ('PRINCIPIANTE', 'INTERMEDIO', ...)
     * Non possiamo fare AVG('PRINCIPIANTE') → serve conversione a numero
     * 
     * Soluzione: CASE WHEN (come switch in Java)
     * WHEN f.suggestedLevel = 'PRINCIPIANTE' THEN 1
     * WHEN f.suggestedLevel = 'INTERMEDIO' THEN 2
     * WHEN f.suggestedLevel = 'AVANZATO' THEN 3
     * WHEN f.suggestedLevel = 'PROFESSIONISTA' THEN 4
     * 
     * Poi AVG() calcola media numerica:
     * - Feedback: [INTERMEDIO, AVANZATO, INTERMEDIO] → [2, 3, 2]
     * - AVG(2, 3, 2) = 2.33 → livello tra INTERMEDIO e AVANZATO
     * 
     * SQL generato (semplificato):
     *   SELECT AVG(
     *       CASE 
     *           WHEN suggested_level = 'PRINCIPIANTE' THEN 1
     *           WHEN suggested_level = 'INTERMEDIO' THEN 2
     *           WHEN suggested_level = 'AVANZATO' THEN 3
     *           WHEN suggested_level = 'PROFESSIONISTA' THEN 4
     *       END
     *   ) FROM feedbacks WHERE target_user_id = ?
     * 
     * Ritorna Double (può essere NULL se nessun feedback):
     * - null: utente senza feedback
     * - 1.0: tutti feedback PRINCIPIANTE
     * - 2.5: mix INTERMEDIO/AVANZATO
     * - 4.0: tutti feedback PROFESSIONISTA
     * 
     * Uso nel service per aggiornare perceivedLevel:
     *   Double avg = feedbackRepository.getAverageLevelForUser(bob);
     *   if (avg != null) {
     *       Level perceivedLevel = convertToLevel(avg);  // 2.5 → INTERMEDIO o AVANZATO
     *       bob.setPerceivedLevel(perceivedLevel);
     *   }
     */
    @Query("SELECT AVG(CASE " +
           "WHEN f.suggestedLevel = 'PRINCIPIANTE' THEN 1 " +
           "WHEN f.suggestedLevel = 'INTERMEDIO' THEN 2 " +
           "WHEN f.suggestedLevel = 'AVANZATO' THEN 3 " +
           "WHEN f.suggestedLevel = 'PROFESSIONISTA' THEN 4 " +
           "END) FROM Feedback f WHERE f.targetUser = :targetUser")
    Double getAverageLevelForUser(User targetUser);
}
