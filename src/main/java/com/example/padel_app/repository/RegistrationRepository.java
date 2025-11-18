package com.example.padel_app.repository;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RegistrationRepository - Interfaccia per accesso dati Registration
 * 
 * PATTERN: Repository per Association Class (Many-to-Many con attributi)
 * 
 * CONCETTI JPA DIMOSTRATI:
 * 1. Query su relazioni ManyToOne (findByUser, findByMatch)
 * 2. Query composite con AND (findByUserAndMatch, existsByUserAndMatchAndStatus)
 * 3. **JOIN FETCH per lazy relationships**: carica User eagerly per evitare LazyInitializationException
 * 4. **COUNT queries**: aggregate function per contare righe senza caricare entità
 * 5. **EXISTS queries**: verifica esistenza senza caricare entità (più efficiente)
 * 
 * BUSINESS LOGIC CRUCIALE:
 * - countActiveRegistrationsByMatch(): conta solo JOINED (determina se partita è piena)
 * - countAllRegistrationsByMatch(): conta TUTTI (anche CANCELLED, per storico completo)
 * - existsByUserAndMatchAndStatus(): previene iscrizioni duplicate
 */
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    
    /**
     * Trova tutte le iscrizioni di un utente
     * 
     * DERIVED QUERY METHOD:
     * SQL: SELECT * FROM registrations WHERE user_id = ?
     * 
     * Uso: mostrare storico partite di un utente (iscritte + disiscritte)
     */
    List<Registration> findByUser(User user);
    
    /**
     * Trova tutte le iscrizioni per una partita (con JOIN FETCH)
     * 
     * @Query CUSTOM con JOIN FETCH:
     * JPQL: SELECT r FROM Registration r JOIN FETCH r.user WHERE r.match = :match
     * 
     * JOIN FETCH: carica eagerly anche l'entità User associata
     * PERCHÉ? Evita LazyInitializationException quando accediamo a registration.getUser()
     * nel template Thymeleaf (dove la sessione Hibernate è chiusa)
     * 
     * SQL generato:
     *   SELECT r.*, u.* FROM registrations r
     *   INNER JOIN users u ON r.user_id = u.id
     *   WHERE r.match_id = ?
     * 
     * NOTA: Carica TUTTE le registrations (JOINED + CANCELLED) per storico completo
     * 
     * Uso:
     *   Match match = ...;
     *   List<Registration> allParticipants = registrationRepository.findByMatch(match);
     *   // Posso accedere a allParticipants.get(0).getUser().getFirstName() senza errori
     */
    @Query("SELECT r FROM Registration r JOIN FETCH r.user WHERE r.match = :match")
    List<Registration> findByMatch(Match match);
    
    /**
     * Trova tutte le iscrizioni con uno specifico status
     * 
     * SQL: SELECT * FROM registrations WHERE status = 'JOINED'
     * Utile per statistiche globali
     */
    List<Registration> findByStatus(RegistrationStatus status);
    
    /**
     * Trova iscrizione specifica per coppia (user, match)
     * 
     * DERIVED QUERY METHOD - composite:
     * SQL: SELECT * FROM registrations WHERE user_id = ? AND match_id = ?
     * 
     * Optional perché può non esistere (utente non iscritto a quella partita)
     * 
     * Uso: verificare se utente è iscritto prima di operazioni (leave, feedback)
     */
    Optional<Registration> findByUserAndMatch(User user, Match match);
    
    /**
     * Verifica se esiste iscrizione per coppia (user, match)
     * 
     * DERIVED QUERY METHOD - exists:
     * SQL: SELECT EXISTS(SELECT 1 FROM registrations WHERE user_id = ? AND match_id = ?)
     * 
     * Più efficiente di findByUserAndMatch().isPresent() perché non carica l'entità
     * 
     * Uso: validazione prima di iscriversi (previene duplicati)
     *   if (registrationRepository.existsByUserAndMatch(user, match)) {
     *       throw new Exception("Già iscritto!");
     *   }
     */
    boolean existsByUserAndMatch(User user, Match match);
    
    /**
     * Verifica se esiste iscrizione attiva (JOINED) per coppia (user, match)
     * 
     * DERIVED QUERY METHOD - exists con 3 condizioni:
     * SQL: SELECT EXISTS(SELECT 1 FROM registrations 
     *                     WHERE user_id = ? AND match_id = ? AND status = 'JOINED')
     * 
     * BUSINESS LOGIC CRITICA:
     * Usato per determinare se utente è "attivamente" iscritto.
     * Se status = CANCELLED, ritorna false (può ri-iscriversi)
     * 
     * Uso: filtrare partite disponibili (escludi quelle già iscritte JOINED)
     */
    boolean existsByUserAndMatchAndStatus(User user, Match match, RegistrationStatus status);
    
    /**
     * Trova iscrizione specifica per coppia (user, match) con status specifico
     * 
     * DERIVED QUERY METHOD - composite con 3 condizioni:
     * SQL: SELECT * FROM registrations WHERE user_id = ? AND match_id = ? AND status = ?
     * 
     * BUSINESS LOGIC:
     * Usato per trovare registration CANCELLED da riattivare durante ri-iscrizione.
     * Questo previene violazione del vincolo unique (user_id, match_id) permettendo
     * iscrizioni multiple dopo disiscrizione.
     * 
     * Optional perché può non esistere (prima iscrizione o mai cancellata)
     * 
     * Uso in RegistrationService.joinMatch():
     *   Optional<Registration> cancelled = 
     *       registrationRepository.findByUserAndMatchAndStatus(user, match, CANCELLED);
     *   if (cancelled.isPresent()) {
     *       // Riattiva esistente invece di crearne nuova
     *       cancelled.get().setStatus(JOINED);
     *   }
     */
    Optional<Registration> findByUserAndMatchAndStatus(User user, Match match, RegistrationStatus status);
    
    /**
     * Trova iscrizioni ATTIVE (JOINED) per una partita (con JOIN FETCH)
     * 
     * @Query CUSTOM con filtro status:
     * JOIN FETCH r.user: carica User eagerly per accesso sicuro in template
     * 
     * SQL:
     *   SELECT r.*, u.* FROM registrations r
     *   INNER JOIN users u ON r.user_id = u.id
     *   WHERE r.match_id = ? AND r.status = 'JOINED'
     * 
     * DIFFERENZA con findByMatch():
     * - findByMatch(): ritorna TUTTI (JOINED + CANCELLED) → per storico completo
     * - findByMatchAndStatus(..., JOINED): ritorna solo JOINED → per roster attuale
     * 
     * Uso: mostrare lista giocatori confermati per una partita
     */
    @Query("SELECT r FROM Registration r JOIN FETCH r.user WHERE r.match = :match AND r.status = :status")
    List<Registration> findByMatchAndStatus(Match match, RegistrationStatus status);
    
    /**
     * Conta iscrizioni ATTIVE (JOINED) per una partita
     * 
     * @Query AGGREGATE - COUNT:
     * SQL: SELECT COUNT(*) FROM registrations WHERE match_id = ? AND status = 'JOINED'
     * 
     * BUSINESS LOGIC CRUCIALE:
     * Determina se partita è piena (4 giocatori) o ha posti liberi.
     * Conta SOLO JOINED perché solo questi occupano posti reali.
     * 
     * PERFORMANCE:
     * COUNT query è molto efficiente: ritorna solo un numero, no entità caricate.
     * Molto meglio di match.getRegistrations().stream().filter(...).count()
     * 
     * Uso in MatchService:
     *   int activeCount = registrationRepository.countActiveRegistrationsByMatch(match);
     *   if (activeCount >= 4) {
     *       throw new Exception("Partita piena!");
     *   }
     */
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.match = :match AND r.status = 'JOINED'")
    int countActiveRegistrationsByMatch(Match match);
    
    /**
     * Conta TUTTE le iscrizioni per una partita (JOINED + CANCELLED)
     * 
     * @Query AGGREGATE - COUNT senza filtro status:
     * SQL: SELECT COUNT(*) FROM registrations WHERE match_id = ?
     * 
     * PERCHÉ SERVE?
     * Per partite FINISHED, vogliamo sapere quanti partecipanti totali ci sono stati,
     * anche se qualcuno si è disiscritto dopo la fine.
     * 
     * ESEMPIO:
     * Partita con Alice, Bob, Carlo, Diana (4 JOINED) → diventa FINISHED
     * Bob si disiscrivo dopo → status CANCELLED
     * - countActiveRegistrationsByMatch(): 3 (solo JOINED)
     * - countAllRegistrationsByMatch(): 4 (TUTTI i partecipanti reali)
     * 
     * Uso: mostrare contatori corretti per partite finite e form feedback
     */
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.match = :match")
    int countAllRegistrationsByMatch(Match match);
    
    /**
     * Trova iscrizioni di un utente con uno specifico status
     * 
     * DERIVED QUERY METHOD:
     * SQL: SELECT * FROM registrations WHERE user_id = ? AND status = 'JOINED'
     * 
     * Uso: filtrare partite iscritte vs partite da cui si è disiscritto
     *   List<Registration> activeMatches = 
     *       registrationRepository.findByUserAndStatus(user, RegistrationStatus.JOINED);
     */
    List<Registration> findByUserAndStatus(User user, RegistrationStatus status);
}
