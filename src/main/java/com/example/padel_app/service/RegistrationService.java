package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.RegistrationStatus;
import com.example.padel_app.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RegistrationService - Gestisce iscrizioni utenti alle partite
 * 
 * RESPONSABILITÀ:
 * 1. Join Match: iscrizione con validazione vincoli business
 * 2. Leave Match: disiscrizione con logica speciale per creatore
 * 3. Query registrazioni: filtri per user, match, status
 * 4. Contatori: giocatori attivi vs totali
 * 
 * BUSINESS RULES IMPLEMENTATE:
 * - Max 4 giocatori per partita (vincolo hard)
 * - No iscrizioni duplicate (un utente può iscriversi 1 sola volta)
 * - Creatore che si disiscrivo → elimina partita intera
 * - Altri che si disiscrivono → status CANCELLED (partita rimane)
 * - Auto-conferma a 4 giocatori (delega a MatchService)
 * 
 * SPRING CONCEPTS:
 * - @Service: Bean gestito da Spring IoC
 * - @Transactional: Gestione transazioni database
 *   * readOnly=true: default per query (ottimizzazione)
 *   * override su joinMatch() e leaveMatch() per scrittura
 * - @RequiredArgsConstructor: Dependency injection via costruttore
 * - @Slf4j: Logger per operazioni business
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RegistrationService {
    
    // ==================== DEPENDENCIES ====================
    
    private final RegistrationRepository registrationRepository;
    private final MatchService matchService;  // Per auto-conferma e delete match
    
    // ==================== QUERY METHODS ====================
    
    /**
     * Trova tutte le iscrizioni di un utente (JOINED + CANCELLED)
     * 
     * Uso: storico completo partite di un utente
     */
    public List<Registration> getRegistrationsByUser(User user) {
        return registrationRepository.findByUser(user);
    }
    
    /**
     * Trova tutte le iscrizioni per una partita (con JOIN FETCH User)
     * 
     * NOTA: Ritorna TUTTE (JOINED + CANCELLED) per storico completo.
     * Utile per partite FINISHED dove vogliamo tutti i partecipanti.
     */
    public List<Registration> getRegistrationsByMatch(Match match) {
        return registrationRepository.findByMatch(match);
    }
    
    /**
     * Trova solo iscrizioni ATTIVE (status = JOINED) per una partita
     * 
     * Uso: roster attuale giocatori confermati
     * Differenza con getRegistrationsByMatch: filtra solo JOINED
     */
    public List<Registration> getActiveRegistrationsByMatch(Match match) {
        return registrationRepository.findByMatchAndStatus(match, RegistrationStatus.JOINED);
    }
    
    /**
     * Alias per getRegistrationsByMatch (retrocompatibilità)
     * Ritorna TUTTE le registrations (JOINED + CANCELLED)
     */
    public List<Registration> getAllRegistrationsByMatch(Match match) {
        return registrationRepository.findByMatch(match);
    }
    
    /**
     * Verifica se utente è iscritto ATTIVO a una partita
     * 
     * CHECK: user + match + status = JOINED
     * Ritorna false se:
     * - Utente non mai iscritto
     * - Utente era iscritto ma si è disiscritto (status = CANCELLED)
     * 
     * Uso: mostrare "Iscriviti" vs "Disiscrivi" in UI
     */
    public boolean isUserRegisteredForMatch(User user, Match match) {
        return registrationRepository.existsByUserAndMatchAndStatus(user, match, RegistrationStatus.JOINED);
    }
    
    /**
     * Alias per isUserRegisteredForMatch (retrocompatibilità)
     */
    public boolean isUserRegistered(User user, Match match) {
        return registrationRepository.existsByUserAndMatchAndStatus(user, match, RegistrationStatus.JOINED);
    }
    
    /**
     * Trova tutte le iscrizioni ATTIVE di un utente
     * 
     * Uso: "Le Mie Partite" → mostra solo partite a cui è iscritto ora
     */
    public List<Registration> getActiveRegistrationsByUser(User user) {
        return registrationRepository.findByUserAndStatus(user, RegistrationStatus.JOINED);
    }
    
    /**
     * Conta giocatori ATTIVI (JOINED) in una partita
     * 
     * COUNT query efficiente (no caricamento entità)
     * Uso: verificare se partita è piena (4/4) prima di join
     */
    public int getActiveRegistrationsCount(Match match) {
        return registrationRepository.countActiveRegistrationsByMatch(match);
    }
    
    /**
     * Conta TUTTI i partecipanti (JOINED + CANCELLED) in una partita
     * 
     * Uso: partite FINISHED → mostrare roster completo per feedback
     * Anche chi si disiscrivo dopo la fine deve apparire
     */
    public int getAllRegistrationsCount(Match match) {
        return registrationRepository.countAllRegistrationsByMatch(match);
    }
    
    // ==================== BUSINESS LOGIC ====================
    
    /**
     * Iscrive un utente a una partita con validazioni business
     * 
     * VALIDAZIONI:
     * 1. ❌ Utente già iscritto (JOINED) → eccezione
     * 2. ❌ Partita piena (4/4 giocatori) → eccezione
     * 3. ✅ Crea Registration con status = JOINED (o riattiva se CANCELLED)
     * 4. ✅ Trigger auto-conferma se raggiunge 4 giocatori
     * 
     * FLOW COMPLETO:
     * 1. Check duplicati (solo JOINED)
     * 2. Check capacità massima
     * 3. **RIUSA registration CANCELLED se esistente** (fix unique constraint)
     *    - Se esiste registration CANCELLED → riattiva (status = JOINED)
     *    - Se non esiste → crea nuova registration
     * 4. Log operazione
     * 5. **Chiama MatchService.checkAndConfirmMatch()**
     *    → Se 4° giocatore: WAITING → CONFIRMED + evento Observer
     * 
     * NOTA: Questo approccio risolve il problema del vincolo unique (user_id, match_id)
     * permettendo iscrizioni multiple dopo disiscrizione senza violare il DB.
     * 
     * @param user utente che si iscrive
     * @param match partita target
     * @return registration creata o riattivata
     * @throws IllegalStateException se già iscritto o partita piena
     */
    @Transactional  // Override readOnly: serve scrittura DB
    public Registration joinMatch(User user, Match match) {
        // Check if already registered with JOINED status
        if (isUserRegisteredForMatch(user, match)) {
            throw new IllegalStateException("User already registered for this match");
        }
        
        // Check match capacity (max 4 players)
        int currentPlayers = registrationRepository.countActiveRegistrationsByMatch(match);
        if (currentPlayers >= 4) {
            throw new IllegalStateException("Match is full - maximum 4 players allowed");
        }
        
        // Try to find existing CANCELLED registration to reuse
        Optional<Registration> existingCancelledReg = registrationRepository
            .findByUserAndMatchAndStatus(user, match, RegistrationStatus.CANCELLED);
        
        Registration registration;
        if (existingCancelledReg.isPresent()) {
            // Riusa registration esistente (fix unique constraint violation)
            registration = existingCancelledReg.get();
            registration.setStatus(RegistrationStatus.JOINED);
            registration.setRegisteredAt(LocalDateTime.now());  // Aggiorna timestamp
            log.info("User {} re-joined match {} (reactivating cancelled registration)", 
                     user.getUsername(), match.getId());
        } else {
            // Crea nuova registration (prima iscrizione)
            registration = new Registration();
            registration.setUser(user);
            registration.setMatch(match);
            registration.setStatus(RegistrationStatus.JOINED);
            registration.setRegisteredAt(LocalDateTime.now());
            log.info("User {} joined match {} for the first time", 
                     user.getUsername(), match.getId());
        }
        
        Registration savedRegistration = registrationRepository.save(registration);
        log.info("Match {} now has {}/{} players", match.getId(), currentPlayers + 1, 4);
        
        // Check if match should be auto-confirmed (4 players)
        // Delega a MatchService che pubblica evento Observer se necessario
        matchService.checkAndConfirmMatch(match);
        
        return savedRegistration;
    }
    
    /**
     * Disiscrizione da partita con logica speciale per creatore
     * 
     * BUSINESS RULES:
     * 1. Se creatore si disiscrivo → **elimina partita intera**
     *    Rationale: creatore organizza, se rinuncia la partita non ha senso
     * 2. Se giocatore normale → status CANCELLED (partita rimane)
     *    La partita torna disponibile (posto libero)
     * 
     * VALIDAZIONI:
     * - ❌ Utente non iscritto → eccezione
     * - ❌ Utente già disiscritto (CANCELLED) → eccezione
     * 
     * SIDE EFFECTS:
     * - Se creatore: cascade delete elimina anche registrations e feedbacks
     * - Se normale: partita da CONFIRMED può tornare WAITING (3/4 giocatori)
     * 
     * @param user utente che si disiscrivo
     * @param match partita da cui uscire
     * @throws IllegalStateException se non iscritto o già disiscritto
     */
    @Transactional  // Override readOnly: modifica o delete
    public void leaveMatch(User user, Match match) {
        Optional<Registration> registrationOpt = registrationRepository.findByUserAndMatch(user, match);
        
        if (registrationOpt.isEmpty()) {
            throw new IllegalStateException("User is not registered for this match");
        }
        
        Registration registration = registrationOpt.get();
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new IllegalStateException("User already left this match");
        }
        
        // Check if user is the creator - if so, delete the match entirely
        if (match.getCreator() != null && match.getCreator().getId().equals(user.getId())) {
            log.info("Creator {} leaving match {} - deleting entire match", user.getUsername(), match.getId());
            matchService.deleteMatch(match.getId());
            // Hibernate cascade delete rimuove automaticamente tutte le registrations
        } else {
            // Normal leave: just cancel the registration
            registration.setStatus(RegistrationStatus.CANCELLED);
            registrationRepository.save(registration);
            
            log.info("User {} left match {} ({}/{} players remaining)", 
                     user.getUsername(), match.getId(), 
                     registrationRepository.countActiveRegistrationsByMatch(match), 4);
        }
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Salva registration (generico, usato internamente)
     */
    @Transactional
    public Registration saveRegistration(Registration registration) {
        return registrationRepository.save(registration);
    }
    
    /**
     * Elimina registration per ID (generico, usato internamente)
     */
    @Transactional
    public void deleteRegistration(Long id) {
        registrationRepository.deleteById(id);
    }
}
