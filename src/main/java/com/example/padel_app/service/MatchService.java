package com.example.padel_app.service;

import com.example.padel_app.event.MatchConfirmedEvent;
import com.example.padel_app.event.MatchFinishedEvent;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.strategy.MatchSortingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MatchService - Service layer per gestione partite (business logic core)
 * 
 * RESPONSABILITÀ:
 * 1. CRUD operations su Match
 * 2. **STRATEGY PATTERN**: Ordinamento partite con strategie intercambiabili
 * 3. **OBSERVER PATTERN**: Pubblicazione eventi quando match cambia stato
 * 4. Business logic lifecycle: WAITING → CONFIRMED → FINISHED
 * 5. Auto-conferma quando 4 giocatori si iscrivono
 * 
 * SPRING ANNOTATIONS:
 * - @Service: Marca come service bean gestito da Spring IoC container
 * - @Transactional(readOnly = true): Default READ-ONLY per ottimizzazione
 *   * Query più veloci (Hibernate non prepara flush)
 *   * Metodi che modificano dati hanno @Transactional specifico
 * - @RequiredArgsConstructor: Lombok genera costruttore con final fields (Dependency Injection)
 * - @Slf4j: Lombok genera logger SLF4J per log applicativi
 * 
 * DESIGN PATTERNS IMPLEMENTATI:
 * - **Strategy Pattern**: Map<String, MatchSortingStrategy> per sorting dinamico
 * - **Observer Pattern**: ApplicationEventPublisher per notifiche eventi
 * - **Dependency Injection**: tutti i dependency sono final e iniettati via costruttore
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default read-only, override con @Transactional specifici
@Slf4j
public class MatchService {
    
    // ==================== DEPENDENCIES (Injected by Spring) ====================
    
    /**
     * Repository per accesso dati Match
     */
    private final MatchRepository matchRepository;
    
    /**
     * Repository per contare registrazioni (fix lazy loading issue)
     * Usato per conteggi efficienti senza caricare intere collection
     */
    private final com.example.padel_app.repository.RegistrationRepository registrationRepository;
    
    /**
     * Publisher per Observer Pattern
     * Pubblica eventi (MatchConfirmedEvent, MatchFinishedEvent) che vengono
     * ascoltati da MatchEventListener
     */
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Map di strategie di sorting (Strategy Pattern)
     * 
     * DEPENDENCY INJECTION AUTOMATICA:
     * Spring auto-popola questa Map con tutti i bean che implementano MatchSortingStrategy:
     * - Key: nome bean Spring (es. "dateSorting", "popularitySorting", "levelSorting")
     * - Value: istanza della strategy
     * 
     * Esempio contenuto Map:
     *   {
     *     "dateSorting": DateSortingStrategy instance,
     *     "popularitySorting": PopularitySortingStrategy instance,
     *     "levelSorting": LevelSortingStrategy instance
     *   }
     */
    private final Map<String, MatchSortingStrategy> sortingStrategies;
    
    // ==================== QUERY METHODS ====================
    
    /**
     * Ottieni tutte le partite (con creator eager-loaded)
     * 
     * Usa findAllWithCreator() per evitare LazyInitializationException
     * quando accediamo a match.getCreator() nei template Thymeleaf
     */
    public List<Match> getAllMatches() {
        return matchRepository.findAllWithCreator();
    }
    
    /**
     * Filtra partite per status (WAITING, CONFIRMED, FINISHED)
     * 
     * Uso: mostrare solo partite disponibili (WAITING), o partite da finire (CONFIRMED)
     */
    public List<Match> getMatchesByStatus(MatchStatus status) {
        return matchRepository.findByStatusWithCreator(status);
    }
    
    /**
     * Filtra partite per livello richiesto
     * 
     * Business logic: un PRINCIPIANTE non dovrebbe vedere partite per PROFESSIONISTI
     */
    public List<Match> getMatchesByLevel(Level level) {
        return matchRepository.findByRequiredLevelWithCreator(level);
    }
    
    // ==================== STRATEGY PATTERN IMPLEMENTATION ====================
    
    /**
     * Ordina partite usando una strategia dinamica (Strategy Pattern)
     * 
     * COME FUNZIONA:
     * 1. Riceve stringa "date", "popularity", o "level"
     * 2. Costruisce chiave strategia: strategy + "Sorting" (es. "dateSorting")
     * 3. Cerca nella Map la strategy corrispondente
     * 4. Delega sorting alla strategy trovata
     * 5. Fallback: ordina per data se strategy non trovata
     * 
     * VANTAGGI STRATEGY PATTERN:
     * - Aggiungere nuova strategia = solo creare classe @Component, zero modifiche qui
     * - Testabile: possiamo mockare le strategy
     * - Open/Closed Principle: aperto a estensioni, chiuso a modifiche
     * 
     * @param strategy nome strategia: "date", "popularity", "level"
     * @return lista partite ordinate secondo strategia
     */
    public List<Match> getMatchesOrderedBy(String strategy) {
        String strategyKey = strategy + "Sorting";
        MatchSortingStrategy sortingStrategy = sortingStrategies.get(strategyKey);
        
        if (sortingStrategy != null) {
            List<Match> allMatches = matchRepository.findAllWithCreator();
            log.debug("Using {} strategy to sort {} matches", sortingStrategy.getStrategyName(), allMatches.size());
            return sortingStrategy.sort(allMatches);
        }
        
        // Safe fallback - direct repository call to avoid recursion
        log.warn("Strategy {} not found, using date sorting as fallback", strategy);
        return matchRepository.findAllOrderByDateWithCreator();
    }
    
    /**
     * Convenienza: ordina per data (delega a Strategy Pattern)
     */
    public List<Match> getMatchesOrderedByDate() {
        return getMatchesOrderedBy("date");
    }
    
    /**
     * Convenienza: ordina per popolarità (delega a Strategy Pattern)
     */
    public List<Match> getMatchesOrderedByPopularity() {
        return getMatchesOrderedBy("popularity");
    }
    
    /**
     * Convenienza: ordina per livello (delega a Strategy Pattern)
     */
    public List<Match> getMatchesOrderedByLevel() {
        return getMatchesOrderedBy("level");
    }
    
    /**
     * Trova partita per ID
     * 
     * Optional perché potrebbe non esistere
     */
    public Optional<Match> getMatchById(Long id) {
        return matchRepository.findById(id);
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Salva partita (INSERT o UPDATE)
     * 
     * @Transactional: override default readOnly=true, abilita scrittura
     * Se id = null → INSERT
     * Se id esiste → UPDATE
     */
    @Transactional
    public Match saveMatch(Match match) {
        return matchRepository.save(match);
    }
    
    /**
     * Elimina partita per ID
     * 
     * Cascade delete: elimina anche tutte le Registration e Feedback associati
     * (definito in Match entity con cascade=ALL)
     */
    @Transactional
    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }
    
    // ==================== BUSINESS LOGIC - OBSERVER PATTERN ====================
    
    /**
     * Auto-conferma partita quando raggiunge 4 giocatori (Observer Pattern trigger)
     * 
     * BUSINESS RULE:
     * Quando 4° giocatore si iscrive:
     * 1. Match.status: WAITING → CONFIRMED
     * 2. Pubblica MatchConfirmedEvent
     * 3. MatchEventListener riceve evento e invia notifiche
     * 
     * PERCHÉ QUERY REPOSITORY invece di match.getActiveRegistrationsCount()?
     * - match.registrations potrebbe essere lazy e non caricata
     * - COUNT query diretta è più efficiente e sempre accurata
     * 
     * @param match partita da verificare
     * @return match aggiornato se confermato, altrimenti match originale
     */
    @Transactional
    public Match checkAndConfirmMatch(Match match) {
        // Use repository to get accurate count instead of entity collection
        int activeCount = registrationRepository.countActiveRegistrationsByMatch(match);
        
        if (activeCount >= 4 && match.getStatus() == MatchStatus.WAITING) {
            MatchStatus oldStatus = match.getStatus();
            match.setStatus(MatchStatus.CONFIRMED);
            Match savedMatch = matchRepository.save(match);
            
            // Publish Observer event
            log.info("🎯 Publishing MatchConfirmedEvent for match ID: {}", savedMatch.getId());
            eventPublisher.publishEvent(new MatchConfirmedEvent(this, savedMatch));
            
            return savedMatch;
        }
        return match;
    }
    
    /**
     * Marca partite scadute come FINISHED (cron job o scheduler)
     * 
     * BUSINESS LOGIC:
     * Se Match.dateTime < now() E status = CONFIRMED:
     * 1. Match.status: CONFIRMED → FINISHED
     * 2. Pubblica MatchFinishedEvent
     * 3. MatchEventListener invia notifiche "Lascia feedback!"
     * 
     * NOTA: Non implementato come @Scheduled in questo progetto,
     * ma potrebbe essere chiamato da un job schedulato
     */
    @Transactional
    public void markExpiredMatchesAsFinished() {
        List<Match> expiredMatches = matchRepository.findByDateTimeBefore(LocalDateTime.now());
        for (Match match : expiredMatches) {
            if (match.getStatus() == MatchStatus.CONFIRMED) {
                MatchStatus oldStatus = match.getStatus();
                match.setStatus(MatchStatus.FINISHED);
                Match savedMatch = matchRepository.save(match);
                
                // Publish Observer event
                log.info("🏁 Publishing MatchFinishedEvent for match ID: {}", savedMatch.getId());
                eventPublisher.publishEvent(new MatchFinishedEvent(this, savedMatch));
            }
        }
    }
    
    /**
     * Termina partita manualmente (per testing e demo)
     * 
     * Usato dal WebController quando utente clicca "Termina Partita"
     * 
     * @param matchId ID partita da terminare
     * @return match aggiornato
     * @throws IllegalArgumentException se match non trovato o non CONFIRMED
     */
    @Transactional
    public Match finishMatch(Long matchId) {
        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            if (match.getStatus() == MatchStatus.CONFIRMED) {
                match.setStatus(MatchStatus.FINISHED);
                Match savedMatch = matchRepository.save(match);
                
                // Publish Observer event
                log.info("🏁 Manual finish - Publishing MatchFinishedEvent for match ID: {}", savedMatch.getId());
                eventPublisher.publishEvent(new MatchFinishedEvent(this, savedMatch));
                
                return savedMatch;
            }
        }
        throw new IllegalArgumentException("Match not found or cannot be finished");
    }
}
