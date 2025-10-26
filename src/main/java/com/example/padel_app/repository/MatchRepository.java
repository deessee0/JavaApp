package com.example.padel_app.repository;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MatchRepository - Interfaccia per accesso dati Match
 * 
 * CONCETTI AVANZATI JPA DIMOSTRATI IN QUESTO REPOSITORY:
 * 
 * 1. DERIVED QUERY METHODS CON ENUM
 *    - Spring Data genera automaticamente query da nomi metodi
 *    - Supporta enum come parametri (MatchStatus, MatchType, Level)
 *    - Convenzioni: findBy + NomeCampo + Condizione
 *    
 * 2. @Query CON ORDER BY
 *    - Ordinamento personalizzato dei risultati
 *    - SIZE(collection): conta elementi di una relazione @OneToMany
 *    - Utile per sorting strategy (data, popolarità, livello)
 *    
 * 3. JOIN FETCH - SOLUZIONE A LazyInitializationException
 *    Problema LAZY loading:
 *    - JPA carica relazioni @ManyToOne, @OneToMany solo quando accedute
 *    - Se la sessione Hibernate è chiusa → LazyInitializationException
 *    - Soluzione: JOIN FETCH carica relazioni in UN'UNICA query
 *    
 * 4. DISTINCT CON JOIN FETCH
 *    - JOIN può generare righe duplicate (1 match → N registrations)
 *    - DISTINCT elimina duplicati in memoria
 *    - Spring Data applica distinct sul risultato Java, non solo SQL
 *    
 * 5. QUERY COMPOSITE
 *    - Combinazione di più condizioni WHERE
 *    - Named parameters (:level, :status) per leggibilità
 *    - Utili per filtri complessi nell'interfaccia utente
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    /**
     * Trova tutti i match per status (PENDING, CONFIRMED, COMPLETED, CANCELLED)
     * 
     * DERIVED QUERY METHOD - enum parameter:
     * Spring genera: SELECT * FROM matches WHERE status = ?
     * 
     * Enum MatchStatus convertito automaticamente in stringa DB:
     * - MatchStatus.PENDING → 'PENDING'
     * - MatchStatus.CONFIRMED → 'CONFIRMED'
     * 
     * Uso:
     *   List<Match> pendingMatches = matchRepository.findByStatus(MatchStatus.PENDING);
     *   // Mostra tutti i match in attesa di conferma
     */
    List<Match> findByStatus(MatchStatus status);
    
    /**
     * Trova tutti i match per tipo (SINGOLO, DOPPIO)
     * 
     * DERIVED QUERY METHOD:
     * SQL generato: SELECT * FROM matches WHERE type = ?
     * 
     * Uso:
     *   List<Match> doppioMatches = matchRepository.findByType(MatchType.DOPPIO);
     *   // Filtra match doppio per mostrare solo partite 2vs2
     */
    List<Match> findByType(MatchType type);
    
    /**
     * Trova tutti i match per livello richiesto
     * 
     * DERIVED QUERY METHOD:
     * SQL: SELECT * FROM matches WHERE required_level = ?
     * 
     * Utile per matching algorithm: mostrare a un giocatore INTERMEDIO
     * solo match del suo livello o adiacenti
     * 
     * Uso:
     *   List<Match> intermediMatches = matchRepository.findByRequiredLevel(Level.INTERMEDIO);
     */
    List<Match> findByRequiredLevel(Level requiredLevel);
    
    /**
     * Trova tutti i match ordinati per data (cronologico)
     * 
     * @Query CON ORDER BY:
     * JPQL: SELECT m FROM Match m ORDER BY m.dateTime ASC
     * SQL generato: SELECT * FROM matches ORDER BY date_time ASC
     * 
     * ASC (ascending): dal più vecchio al più recente
     * Utile per "Prossimi match" in homepage
     * 
     * Uso:
     *   List<Match> upcomingMatches = matchRepository.findAllOrderByDate();
     *   // Prima partita = quella più vicina nel tempo
     */
    @Query("SELECT m FROM Match m ORDER BY m.dateTime ASC")
    List<Match> findAllOrderByDate();
    
    /**
     * Trova tutti i match ordinati per popolarità (iscritti)
     * 
     * @Query CON SIZE() FUNCTION:
     * SIZE(m.registrations): conta quante Registration ha questo Match
     * DESC: dal più popolare (più iscritti) al meno popolare
     * 
     * SQL generato (semplificato):
     *   SELECT m.*, COUNT(r.id) as reg_count
     *   FROM matches m LEFT JOIN registrations r ON m.id = r.match_id
     *   GROUP BY m.id
     *   ORDER BY reg_count DESC
     * 
     * Uso tipico: sezione "Match più popolari" / "Trending"
     */
    @Query("SELECT m FROM Match m ORDER BY SIZE(m.registrations) DESC")
    List<Match> findAllOrderByPopularity();
    
    /**
     * Trova tutti i match ordinati per livello richiesto
     * 
     * @Query CON ORDER BY su enum:
     * Ordina per valore enum (PRINCIPIANTE < INTERMEDIO < AVANZATO < PROFESSIONISTA)
     * 
     * SQL: SELECT * FROM matches ORDER BY required_level ASC
     * 
     * Uso: mostrare prima match per principianti, poi intermedi, ecc.
     */
    @Query("SELECT m FROM Match m ORDER BY m.requiredLevel ASC")
    List<Match> findAllOrderByLevel();
    
    /**
     * Trova match con data antecedente a una data specifica
     * 
     * DERIVED QUERY METHOD - Before keyword:
     * Spring genera: SELECT * FROM matches WHERE date_time < ?
     * 
     * Uso:
     *   List<Match> pastMatches = 
     *       matchRepository.findByDateTimeBefore(LocalDateTime.now());
     *   // Trova tutti i match già giocati (nel passato)
     */
    List<Match> findByDateTimeBefore(LocalDateTime dateTime);
    
    /**
     * Trova match per status con data futura (query composita)
     * 
     * @Query COMPOSITA - multiple WHERE conditions:
     * Combina due filtri:
     * 1. status = CONFIRMED
     * 2. dateTime > now (match futuri)
     * 
     * SQL: SELECT * FROM matches WHERE status = ? AND date_time > ?
     * 
     * Uso:
     *   List<Match> confirmedUpcoming = matchRepository
     *       .findByStatusAndDateTimeAfter(MatchStatus.CONFIRMED, LocalDateTime.now());
     *   // Match confermati che devono ancora essere giocati
     */
    @Query("SELECT m FROM Match m WHERE m.status = :status AND m.dateTime > :dateTime")
    List<Match> findByStatusAndDateTimeAfter(MatchStatus status, LocalDateTime dateTime);
    
    /**
     * Trova match per livello E status (query composita)
     * 
     * @Query CON NAMED PARAMETERS:
     * :level e :status → più leggibili di ?1, ?2
     * 
     * SQL: SELECT * FROM matches WHERE required_level = ? AND status = ?
     * 
     * Uso nel service layer:
     *   List<Match> availableIntermediate = matchRepository
     *       .findByRequiredLevelAndStatus(Level.INTERMEDIO, MatchStatus.PENDING);
     *   // Match INTERMEDIO disponibili per iscrizione
     */
    @Query("SELECT m FROM Match m WHERE m.requiredLevel = :level AND m.status = :status")
    List<Match> findByRequiredLevelAndStatus(Level level, MatchStatus status);
    
    /**
     * Trova tutti i match con creator e registrations EAGER-loaded
     * 
     * JOIN FETCH - EVITA LazyInitializationException:
     * 
     * Problema senza JOIN FETCH:
     * 1. Query carica solo Match (creator e registrations sono LAZY)
     * 2. Controller accede a match.getCreator().getUsername()
     * 3. Sessione Hibernate è chiusa → LazyInitializationException!
     * 
     * Soluzione JOIN FETCH:
     * - LEFT JOIN FETCH m.creator: carica User creatore in stessa query
     * - LEFT JOIN FETCH m.registrations: carica tutte Registration
     * - UN'UNICA query SQL con JOIN invece di N+1 query
     * 
     * DISTINCT:
     * - JOIN può duplicare Match (1 match → molte registrations)
     * - DISTINCT elimina duplicati in memoria
     * 
     * SQL generato (semplificato):
     *   SELECT m.*, u.*, r.*
     *   FROM matches m
     *   LEFT JOIN users u ON m.creator_id = u.id
     *   LEFT JOIN registrations r ON r.match_id = m.id
     * 
     * Uso: quando si deve mostrare lista match con nome creatore
     * senza causare LazyInitializationException
     */
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations")
    List<Match> findAllWithCreator();
    
    /**
     * Trova match per status con creator e registrations EAGER-loaded
     * 
     * JOIN FETCH + WHERE condition:
     * Combina caricamento eager con filtro
     * 
     * ?1 (positional parameter): primo parametro del metodo
     * 
     * SQL: SELECT m.*, u.*, r.* FROM matches m
     *      LEFT JOIN users u ON m.creator_id = u.id
     *      LEFT JOIN registrations r ON r.match_id = m.id
     *      WHERE m.status = ?
     * 
     * Uso:
     *   List<Match> confirmedWithData = 
     *       matchRepository.findByStatusWithCreator(MatchStatus.CONFIRMED);
     *   // Match confermati con creator e iscritti già caricati
     */
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations WHERE m.status = ?1")
    List<Match> findByStatusWithCreator(MatchStatus status);
    
    /**
     * Trova match per livello con creator e registrations EAGER-loaded
     * 
     * JOIN FETCH pattern per filtrare per livello
     * LEFT JOIN: include match anche se creator è NULL (dovrebbe mai succedere)
     * 
     * Uso:
     *   List<Match> advancedMatches = 
     *       matchRepository.findByRequiredLevelWithCreator(Level.AVANZATO);
     *   // Match avanzati con tutti i dati necessari per il rendering
     */
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations WHERE m.requiredLevel = ?1") 
    List<Match> findByRequiredLevelWithCreator(Level level);
    
    /**
     * Trova tutti i match ordinati per data con dati EAGER-loaded
     * 
     * JOIN FETCH + ORDER BY:
     * Combina caricamento eager con ordinamento
     * 
     * Pattern ideale per homepage/lista match:
     * - Carica tutto in una query
     * - Ordina cronologicamente
     * - Nessun LazyInitializationException nel template
     * 
     * Uso nel controller:
     *   List<Match> matches = matchRepository.findAllOrderByDateWithCreator();
     *   model.addAttribute("matches", matches);
     *   // Template può accedere a match.creator.username senza errori
     */
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations ORDER BY m.dateTime ASC")
    List<Match> findAllOrderByDateWithCreator();
    
    /**
     * Trova match ordinati per popolarità con dati EAGER-loaded
     * 
     * JOIN FETCH + SIZE() + ORDER BY:
     * Query complessa che combina:
     * - Caricamento eager (JOIN FETCH)
     * - Conteggio registrazioni (SIZE)
     * - Ordinamento per popolarità (ORDER BY DESC)
     * 
     * Uso per sezione "Match più popolari" con rendering completo
     */
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations ORDER BY SIZE(m.registrations) DESC")
    List<Match> findAllOrderByPopularityWithCreator();
    
    /**
     * Trova match ordinati per livello con dati EAGER-loaded
     * 
     * JOIN FETCH + ORDER BY enum:
     * Utile per mostrare match raggruppati per difficoltà
     * con tutti i dati necessari già caricati
     * 
     * Uso:
     *   List<Match> matchesByLevel = 
     *       matchRepository.findAllOrderByLevelWithCreator();
     *   // Prima PRINCIPIANTE, poi INTERMEDIO, poi AVANZATO, poi PROFESSIONISTA
     */
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.creator LEFT JOIN FETCH m.registrations ORDER BY m.requiredLevel ASC")
    List<Match> findAllOrderByLevelWithCreator();
}
