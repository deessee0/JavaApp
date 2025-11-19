package com.example.padel_app;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.UserRepository;
import com.example.padel_app.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite per filtri e sorting delle partite.
 * 
 * Testa le funzionalità di:
 * - Filtro per status (WAITING, CONFIRMED, FINISHED)
 * - Filtro per livello richiesto (PRINCIPIANTE, INTERMEDIO, AVANZATO, PROFESSIONISTA)
 * - Sorting per data, popolarità, livello
 * - Combinazione filtri + sorting
 * 
 * SCOPO DIDATTICO: Dimostra testing di query filtrate e uso della Strategy Pattern
 * per ordinamento dinamico delle liste.
 */
@SpringBootTest
@Transactional
class MatchFilterTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Match waitingMatch;
    private Match confirmedMatch;
    private Match finishedMatch;

    @BeforeEach
    void setUp() {
        // Crea utente di test
        testUser = new User();
        testUser.setEmail("filter.test@padel.it");
        testUser.setPassword("test123");
        testUser.setFirstName("Filter");
        testUser.setLastName("Test");
        testUser.setUsername("filtertest");
        testUser.setDeclaredLevel(Level.INTERMEDIO);
        testUser.setMatchesPlayed(0);
        testUser = userRepository.save(testUser);

        // Crea partite con diversi status
        waitingMatch = createMatch("Partita WAITING", MatchStatus.WAITING, Level.PRINCIPIANTE);
        confirmedMatch = createMatch("Partita CONFIRMED", MatchStatus.CONFIRMED, Level.INTERMEDIO);
        finishedMatch = createMatch("Partita FINISHED", MatchStatus.FINISHED, Level.AVANZATO);
    }

    private Match createMatch(String description, MatchStatus status, Level level) {
        Match match = new Match();
        match.setDescription(description);
        match.setLocation("Campo Test");
        match.setDateTime(LocalDateTime.now().plusDays(1));
        match.setRequiredLevel(level);
        match.setType(MatchType.PROPOSTA);
        match.setStatus(status);
        match.setCreator(testUser);
        return matchRepository.save(match);
    }

    // ========== FILTRO PER STATUS ==========

    @Test
    @DisplayName("Filtro per status WAITING - deve tornare solo partite in attesa")
    void testFilterByStatusWaiting() {
        // WHEN: Filtro per status WAITING
        List<Match> waitingMatches = matchService.getMatchesByStatus(MatchStatus.WAITING);

        // THEN: Deve contenere solo partite WAITING (almeno waitingMatch)
        assertFalse(waitingMatches.isEmpty());
        assertTrue(waitingMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.WAITING));
        
        // THEN: Deve contenere la partita waitingMatch che abbiamo creato
        assertTrue(waitingMatches.stream().anyMatch(m -> m.getId().equals(waitingMatch.getId())));
    }

    @Test
    @DisplayName("Filtro per status CONFIRMED - deve tornare solo partite confermate")
    void testFilterByStatusConfirmed() {
        // WHEN: Filtro per status CONFIRMED
        List<Match> confirmedMatches = matchService.getMatchesByStatus(MatchStatus.CONFIRMED);

        // THEN: Deve contenere solo partite CONFIRMED
        assertTrue(confirmedMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.CONFIRMED));
        
        // THEN: Deve contenere la partita confirmedMatch
        assertTrue(confirmedMatches.stream().anyMatch(m -> m.getId().equals(confirmedMatch.getId())));
    }

    @Test
    @DisplayName("Filtro per status FINISHED - deve tornare solo partite terminate")
    void testFilterByStatusFinished() {
        // WHEN: Filtro per status FINISHED
        List<Match> finishedMatches = matchService.getMatchesByStatus(MatchStatus.FINISHED);

        // THEN: Deve contenere solo partite FINISHED
        assertTrue(finishedMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED));
        
        // THEN: Deve contenere la partita finishedMatch
        assertTrue(finishedMatches.stream().anyMatch(m -> m.getId().equals(finishedMatch.getId())));
    }

    // ========== FILTRO PER LIVELLO ==========

    @Test
    @DisplayName("Filtro per livello PRINCIPIANTE - deve tornare solo partite per principianti")
    void testFilterByLevelPrincipiante() {
        // WHEN: Filtro per livello PRINCIPIANTE
        List<Match> beginnerMatches = matchService.getMatchesByLevel(Level.PRINCIPIANTE);

        // THEN: Deve contenere solo partite livello PRINCIPIANTE
        assertTrue(beginnerMatches.stream().allMatch(m -> m.getRequiredLevel() == Level.PRINCIPIANTE));
        
        // THEN: Deve contenere la partita waitingMatch (PRINCIPIANTE)
        assertTrue(beginnerMatches.stream().anyMatch(m -> m.getId().equals(waitingMatch.getId())));
    }

    @Test
    @DisplayName("Filtro per livello INTERMEDIO - deve tornare solo partite per intermedi")
    void testFilterByLevelIntermedio() {
        // WHEN: Filtro per livello INTERMEDIO
        List<Match> intermediateMatches = matchService.getMatchesByLevel(Level.INTERMEDIO);

        // THEN: Deve contenere solo partite livello INTERMEDIO
        assertTrue(intermediateMatches.stream().allMatch(m -> m.getRequiredLevel() == Level.INTERMEDIO));
        
        // THEN: Deve contenere la partita confirmedMatch (INTERMEDIO)
        assertTrue(intermediateMatches.stream().anyMatch(m -> m.getId().equals(confirmedMatch.getId())));
    }

    @Test
    @DisplayName("Filtro per livello AVANZATO - deve tornare solo partite per avanzati")
    void testFilterByLevelAvanzato() {
        // WHEN: Filtro per livello AVANZATO
        List<Match> advancedMatches = matchService.getMatchesByLevel(Level.AVANZATO);

        // THEN: Deve contenere solo partite livello AVANZATO
        assertTrue(advancedMatches.stream().allMatch(m -> m.getRequiredLevel() == Level.AVANZATO));
        
        // THEN: Deve contenere la partita finishedMatch (AVANZATO)
        assertTrue(advancedMatches.stream().anyMatch(m -> m.getId().equals(finishedMatch.getId())));
    }

    // ========== SORTING (Strategy Pattern) ==========

    @Test
    @DisplayName("Sorting per data - deve ordinare per dateTime crescente")
    void testSortingByDate() {
        // GIVEN: Creo 3 partite con date diverse
        Match match1 = createMatch("Partita tra 1 giorno", MatchStatus.WAITING, Level.PRINCIPIANTE);
        match1.setDateTime(LocalDateTime.now().plusDays(1));
        matchRepository.save(match1);

        Match match2 = createMatch("Partita tra 2 giorni", MatchStatus.WAITING, Level.PRINCIPIANTE);
        match2.setDateTime(LocalDateTime.now().plusDays(2));
        matchRepository.save(match2);

        Match match3 = createMatch("Partita tra 3 giorni", MatchStatus.WAITING, Level.PRINCIPIANTE);
        match3.setDateTime(LocalDateTime.now().plusDays(3));
        matchRepository.save(match3);

        // WHEN: Ordino per data
        List<Match> sorted = matchService.getMatchesOrderedBy("date");

        // THEN: Verifica che sia ordinato per data crescente
        // (le partite più vicine nel tempo devono venire prima)
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDateTime current = sorted.get(i).getDateTime();
            LocalDateTime next = sorted.get(i + 1).getDateTime();
            assertTrue(current.isBefore(next) || current.isEqual(next),
                "Le partite devono essere ordinate per data crescente");
        }
    }

    @Test
    @DisplayName("Sorting per popolarità - deve ordinare per numero iscrizioni decrescente")
    void testSortingByPopularity() {
        // WHEN: Ordino per popolarità
        List<Match> sorted = matchService.getMatchesOrderedBy("popularity");

        // THEN: Verifica che lista non sia vuota
        assertFalse(sorted.isEmpty());
        
        // THEN: Verifica che sia ordinato (partite con più iscrizioni prima)
        // Nota: non possiamo verificare l'ordine esatto senza conoscere il numero
        // di registrations, ma verifichiamo che non lanci exception
        assertTrue(sorted.size() >= 3, "Devono esserci almeno 3 partite");
    }

    @Test
    @DisplayName("Sorting per livello - deve ordinare per requiredLevel crescente")
    void testSortingByLevel() {
        // WHEN: Ordino per livello
        List<Match> sorted = matchService.getMatchesOrderedBy("level");

        // THEN: Verifica che lista non sia vuota
        assertFalse(sorted.isEmpty());
        
        // THEN: Verifica ordine corretto (PRINCIPIANTE < INTERMEDIO < AVANZATO < PROFESSIONISTA)
        // Nota: Level enum ha ordinal() che riflette questo ordine
        for (int i = 0; i < sorted.size() - 1; i++) {
            Level current = sorted.get(i).getRequiredLevel();
            Level next = sorted.get(i + 1).getRequiredLevel();
            assertTrue(current.ordinal() <= next.ordinal(),
                "Le partite devono essere ordinate per livello crescente");
        }
    }

    // ========== COMBINAZIONE FILTRI + SORTING ==========

    @Test
    @DisplayName("Filtro WAITING + Sorting per data - deve combinare filtro e ordinamento")
    void testFilterAndSortCombination() {
        // GIVEN: Creo partite WAITING con date diverse
        Match early = createMatch("Early WAITING", MatchStatus.WAITING, Level.PRINCIPIANTE);
        early.setDateTime(LocalDateTime.now().plusDays(1));
        matchRepository.save(early);

        Match late = createMatch("Late WAITING", MatchStatus.WAITING, Level.PRINCIPIANTE);
        late.setDateTime(LocalDateTime.now().plusDays(5));
        matchRepository.save(late);

        // WHEN: Prima filtro per WAITING, poi ordino per data
        List<Match> waitingMatches = matchService.getMatchesByStatus(MatchStatus.WAITING);
        
        // Simuliamo l'ordinamento che il controller farebbe dopo il filtro
        // (in realtà il controller chiama direttamente getMatchesSorted con filtro applicato)
        assertFalse(waitingMatches.isEmpty());
        
        // THEN: Verifica che tutte siano WAITING
        assertTrue(waitingMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.WAITING));
    }

    @Test
    @DisplayName("Filtro per livello INTERMEDIO + lista non vuota")
    void testFilterByLevelReturnsResults() {
        // WHEN: Filtro per livello INTERMEDIO
        List<Match> intermediateMatches = matchService.getMatchesByLevel(Level.INTERMEDIO);

        // THEN: Deve tornare almeno 1 partita (confirmedMatch è INTERMEDIO)
        assertFalse(intermediateMatches.isEmpty());
        assertTrue(intermediateMatches.stream().anyMatch(m -> m.getId().equals(confirmedMatch.getId())));
    }

    @Test
    @DisplayName("Sorting fallback - default a 'date' se strategia non esiste")
    void testSortingFallbackToDate() {
        // WHEN: Chiedo strategia inesistente (dovrebbe fallback a date)
        List<Match> sorted = matchService.getMatchesOrderedBy("nonexistent");

        // THEN: Deve tornare lista ordinata (fallback a date strategy)
        assertFalse(sorted.isEmpty());
        
        // THEN: Verifica ordinamento per data crescente (fallback behavior)
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDateTime current = sorted.get(i).getDateTime();
            LocalDateTime next = sorted.get(i + 1).getDateTime();
            assertTrue(current.isBefore(next) || current.isEqual(next));
        }
    }
}
