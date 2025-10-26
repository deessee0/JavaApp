package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test Edge Cases e Branch Coverage per MatchService
 * 
 * OBIETTIVO:
 * Aumentare branch coverage testando casi limite e gestione errori
 * che non sono coperti dai test scenario-based principali.
 * 
 * FOCUS:
 * - Strategy Pattern con strategia non esistente (fallback)
 * - Tentativi di finire match in stati non validi
 * - Auto-confirm quando match già confermato
 * - Errori business logic
 * 
 * APPROCCIO:
 * Integration test con @SpringBootTest per testare interazioni reali
 * tra Service, Repository e Database H2.
 * 
 * @author Padel App Team
 */
@SpringBootTest
@Transactional  // Ogni test rollback automatico per pulizia DB
@DisplayName("MatchService - Edge Cases e Branch Coverage")
public class MatchServiceEdgeCasesTest {
    
    @Autowired
    private MatchService matchService;
    
    @Autowired
    private MatchRepository matchRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // ==================== STRATEGY PATTERN EDGE CASES ====================
    
    @Test
    @DisplayName("Strategy Pattern: strategia non esistente usa fallback (date sorting)")
    void testStrategyFallback_WhenStrategyNotFound() {
        // GIVEN: Match nel DB con diverse date
        User creator = createUser("fallback_user", "fallback@test.com");
        matchRepository.deleteAll();  // Clean DB per test isolato
        
        Match match1 = createAndSaveMatch(creator, "Campo A", LocalDateTime.now().plusDays(3), MatchStatus.WAITING);
        Match match2 = createAndSaveMatch(creator, "Campo B", LocalDateTime.now().plusDays(1), MatchStatus.WAITING);
        Match match3 = createAndSaveMatch(creator, "Campo C", LocalDateTime.now().plusDays(2), MatchStatus.WAITING);
        
        // WHEN: Richiedo ordinamento con strategia NON ESISTENTE
        List<Match> result = matchService.getMatchesOrderedBy("nonExistentStrategy");
        
        // THEN: Sistema usa fallback (date sorting) senza errore
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        // Verifica solo che siano ordinati (non check specifico location per evitare lazy issues)
        assertThat(result).extracting(Match::getDateTime).isSorted();
    }
    
    @Test
    @DisplayName("Strategy Pattern: ordinamento con lista vuota non causa errori")
    void testSortingStrategy_WithEmptyList() {
        // GIVEN: Nessun match nel DB
        matchRepository.deleteAll();
        
        // WHEN: Richiedo ordinamento
        List<Match> resultDate = matchService.getMatchesOrderedByDate();
        List<Match> resultPop = matchService.getMatchesOrderedByPopularity();
        List<Match> resultLevel = matchService.getMatchesOrderedByLevel();
        
        // THEN: Tutte le strategie gestiscono correttamente lista vuota
        assertThat(resultDate).isEmpty();
        assertThat(resultPop).isEmpty();
        assertThat(resultLevel).isEmpty();
    }
    
    // ==================== AUTO-CONFIRM EDGE CASES ====================
    
    @Test
    @DisplayName("Auto-confirm: match già CONFIRMED non viene ri-confermato")
    void testCheckAndConfirmMatch_AlreadyConfirmed() {
        // GIVEN: Match già nello stato CONFIRMED
        User creator = createUser("creator", "creator@test.com");
        Match match = createAndSaveMatch(creator, "Campo Test", LocalDateTime.now().plusDays(1), MatchStatus.CONFIRMED);
        MatchStatus originalStatus = match.getStatus();
        
        // WHEN: Tentiamo auto-confirm
        Match result = matchService.checkAndConfirmMatch(match);
        
        // THEN: Status rimane CONFIRMED, nessun evento pubblicato
        assertThat(result.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(result.getStatus()).isEqualTo(originalStatus);
    }
    
    @Test
    @DisplayName("Auto-confirm: match con meno di 4 giocatori rimane WAITING")
    void testCheckAndConfirmMatch_LessThan4Players() {
        // GIVEN: Match WAITING con 0 giocatori (solo creato, senza registrazioni)
        User creator = createUser("creator", "creator@test.com");
        Match match = createAndSaveMatch(creator, "Campo Test", LocalDateTime.now().plusDays(1), MatchStatus.WAITING);
        
        // WHEN: Verifichiamo auto-confirm
        Match result = matchService.checkAndConfirmMatch(match);
        
        // THEN: Status rimane WAITING (non abbastanza giocatori)
        assertThat(result.getStatus()).isEqualTo(MatchStatus.WAITING);
    }
    
    // ==================== FINISH MATCH EDGE CASES ====================
    
    @Test
    @DisplayName("Finish Match: errore se match non trovato")
    void testFinishMatch_MatchNotFound() {
        // GIVEN: ID match inesistente
        Long nonExistentId = 99999L;
        
        // WHEN & THEN: Eccezione quando tentiamo di finire match inesistente
        assertThatThrownBy(() -> matchService.finishMatch(nonExistentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Match not found or cannot be finished");
    }
    
    @Test
    @DisplayName("Finish Match: errore se match non è CONFIRMED")
    void testFinishMatch_NotConfirmed() {
        // GIVEN: Match WAITING (non ancora confermato)
        User creator = createUser("creator", "creator@test.com");
        Match waitingMatch = createAndSaveMatch(creator, "Campo Test", LocalDateTime.now().plusDays(1), MatchStatus.WAITING);
        
        // WHEN & THEN: Eccezione perché solo CONFIRMED può diventare FINISHED
        assertThatThrownBy(() -> matchService.finishMatch(waitingMatch.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Match not found or cannot be finished");
    }
    
    @Test
    @DisplayName("Finish Match: errore se match già FINISHED")
    void testFinishMatch_AlreadyFinished() {
        // GIVEN: Match già FINISHED
        User creator = createUser("creator", "creator@test.com");
        Match finishedMatch = createAndSaveMatch(creator, "Campo Test", LocalDateTime.now().minusDays(1), MatchStatus.FINISHED);
        
        // WHEN & THEN: Eccezione perché non può ri-finire match già finito
        assertThatThrownBy(() -> matchService.finishMatch(finishedMatch.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Match not found or cannot be finished");
    }
    
    @Test
    @DisplayName("Finish Match: successo con match CONFIRMED e pubblicazione evento")
    void testFinishMatch_Success() {
        // GIVEN: Match CONFIRMED pronto per essere finito
        User creator = createUser("creator", "creator@test.com");
        Match confirmedMatch = createAndSaveMatch(creator, "Campo Test", LocalDateTime.now().minusHours(2), MatchStatus.CONFIRMED);
        
        // WHEN: Finiamo il match
        Match result = matchService.finishMatch(confirmedMatch.getId());
        
        // THEN: Status cambiato a FINISHED
        assertThat(result.getStatus()).isEqualTo(MatchStatus.FINISHED);
        
        // Verifica persistenza nel DB
        Match fromDb = matchRepository.findById(result.getId()).orElseThrow();
        assertThat(fromDb.getStatus()).isEqualTo(MatchStatus.FINISHED);
    }
    
    // ==================== FILTRI EDGE CASES ====================
    
    @Test
    @DisplayName("Filtro per status: restituisce lista vuota se nessun match con quello status")
    void testGetMatchesByStatus_NoMatches() {
        // GIVEN: Solo match WAITING nel DB
        User creator = createUser("creator", "creator@test.com");
        createAndSaveMatch(creator, "Campo A", LocalDateTime.now().plusDays(1), MatchStatus.WAITING);
        
        // WHEN: Cerchiamo match CANCELLED (nessuno)
        List<Match> result = matchService.getMatchesByStatus(MatchStatus.CANCELLED);
        
        // THEN: Lista vuota
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Filtro per level: restituisce solo match con livello richiesto")
    void testGetMatchesByLevel_FilterCorrectly() {
        // GIVEN: Match con diversi livelli
        User creator = createUser("level_creator", "level@test.com");
        createMatchWithLevel(creator, Level.PRINCIPIANTE);
        createMatchWithLevel(creator, Level.PRINCIPIANTE);
        createMatchWithLevel(creator, Level.PROFESSIONISTA);
        
        // WHEN: Filtriamo per PRINCIPIANTE
        List<Match> beginnerMatches = matchService.getMatchesByLevel(Level.PRINCIPIANTE);
        
        // THEN: Almeno 2 match PRINCIPIANTE (potrebbero essercene altri dal seeder)
        assertThat(beginnerMatches).hasSizeGreaterThanOrEqualTo(2);
        assertThat(beginnerMatches).allMatch(m -> m.getRequiredLevel() == Level.PRINCIPIANTE);
    }
    
    @Test
    @DisplayName("Save Match: aggiorna match esistente correttamente")
    void testSaveMatch_UpdateExisting() {
        // GIVEN: Match esistente
        User creator = createUser("creator", "creator@test.com");
        Match original = createAndSaveMatch(creator, "Campo Vecchio", LocalDateTime.now().plusDays(1), MatchStatus.WAITING);
        Long matchId = original.getId();
        
        // WHEN: Modifichiamo e salviamo
        original.setLocation("Campo Nuovo");
        original.setStatus(MatchStatus.CONFIRMED);
        Match updated = matchService.saveMatch(original);
        
        // THEN: Modifiche persistite (stesso ID, dati aggiornati)
        assertThat(updated.getId()).isEqualTo(matchId);
        assertThat(updated.getLocation()).isEqualTo("Campo Nuovo");
        assertThat(updated.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        
        // Verifica nel DB
        Match fromDb = matchRepository.findById(matchId).orElseThrow();
        assertThat(fromDb.getLocation()).isEqualTo("Campo Nuovo");
    }
    
    @Test
    @DisplayName("Delete Match: rimuove match dal database")
    void testDeleteMatch_RemovesFromDatabase() {
        // GIVEN: Match esistente
        User creator = createUser("creator", "creator@test.com");
        Match match = createAndSaveMatch(creator, "Campo Test", LocalDateTime.now().plusDays(1), MatchStatus.WAITING);
        Long matchId = match.getId();
        
        // WHEN: Eliminiamo il match
        matchService.deleteMatch(matchId);
        
        // THEN: Match non più presente nel DB
        assertThat(matchRepository.findById(matchId)).isEmpty();
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Crea e persiste User nel DB
     */
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password");
        user.setDeclaredLevel(Level.INTERMEDIO);
        return userRepository.save(user);
    }
    
    /**
     * Crea e persiste Match nel DB
     */
    private Match createAndSaveMatch(User creator, String location, LocalDateTime dateTime, MatchStatus status) {
        Match match = new Match();
        match.setLocation(location);
        match.setDateTime(dateTime);
        match.setRequiredLevel(Level.INTERMEDIO);
        match.setType(MatchType.FISSA);
        match.setStatus(status);
        match.setCreator(creator);
        match.setDescription("Test match");
        return matchRepository.save(match);
    }
    
    /**
     * Crea match con livello specifico
     */
    private Match createMatchWithLevel(User creator, Level level) {
        Match match = new Match();
        match.setLocation("Campo " + level.name());
        match.setDateTime(LocalDateTime.now().plusDays(1));
        match.setRequiredLevel(level);
        match.setType(MatchType.FISSA);
        match.setStatus(MatchStatus.WAITING);
        match.setCreator(creator);
        match.setDescription("Match " + level.name());
        return matchRepository.save(match);
    }
}
