package com.example.padel_app.observer;

import com.example.padel_app.event.MatchConfirmedEvent;
import com.example.padel_app.event.MatchFinishedEvent;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.UserRepository;
import com.example.padel_app.service.MatchService;
import com.example.padel_app.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Suite per verificare l'Observer Pattern implementato nell'applicazione.
 * 
 * <h2>Cosa testa questo file?</h2>
 * Questo test verifica che il pattern Observer funzioni correttamente:
 * <ul>
 *   <li>Eventi vengono pubblicati quando avvengono cambiamenti di stato</li>
 *   <li>Listener ricevono gli eventi corretti</li>
 *   <li>Il flusso Publisher → Event → Listener funziona end-to-end</li>
 * </ul>
 * 
 * <h2>Annotazioni chiave per testare eventi Spring:</h2>
 * <ul>
 *   <li><strong>@RecordApplicationEvents</strong>: Registra tutti gli eventi pubblicati durante il test</li>
 *   <li><strong>ApplicationEvents</strong>: Contiene la lista degli eventi catturati</li>
 *   <li><strong>@SpringBootTest</strong>: Carica il contesto completo inclusi Publisher e Listener</li>
 *   <li><strong>@Transactional</strong>: Rollback automatico del DB dopo ogni test</li>
 * </ul>
 * 
 * <h2>Perché @RecordApplicationEvents?</h2>
 * Spring normalmente propaga gli eventi ai listener in modo sincrono e poi li "dimentica".
 * @RecordApplicationEvents intercetta e salva tutti gli eventi per permetterci di:
 * <ul>
 *   <li>Verificare che un evento specifico sia stato pubblicato</li>
 *   <li>Controllare il numero di volte che è stato pubblicato</li>
 *   <li>Ispezionare i dati contenuti nell'evento</li>
 *   <li>Testare il comportamento del publisher senza dipendere dal listener</li>
 * </ul>
 * 
 * <h2>Scenario di test Observer Pattern:</h2>
 * <pre>
 * Test 1: Auto-conferma a 4 giocatori
 *   GIVEN: Una partita con 3 giocatori
 *   WHEN: Si unisce il 4° giocatore
 *   THEN: MatchConfirmedEvent viene pubblicato
 * 
 * Test 2: Finish match pubblica evento
 *   GIVEN: Una partita CONFIRMED
 *   WHEN: Il creatore marca la partita come FINISHED
 *   THEN: MatchFinishedEvent viene pubblicato
 * 
 * Test 3: Contenuto eventi
 *   GIVEN: Un evento pubblicato
 *   WHEN: Recupero l'evento da ApplicationEvents
 *   THEN: Contiene i dati corretti della partita
 * </pre>
 * 
 * @see com.example.padel_app.event.MatchConfirmedEvent
 * @see com.example.padel_app.event.MatchFinishedEvent
 * @see com.example.padel_app.listener.MatchEventListener
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents  // Cattura tutti gli eventi pubblicati durante i test
public class ObserverPatternTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents applicationEvents;  // Contiene gli eventi catturati

    private User creator;
    private User player1;
    private User player2;
    private User player3;
    private User player4;
    private Match testMatch;

    @BeforeEach
    void setUp() {
        // Crea utenti di test
        creator = createUser("creator", "creator@test.com", "Creator", "User", Level.INTERMEDIO);
        player1 = createUser("player1", "p1@test.com", "Player", "One", Level.INTERMEDIO);
        player2 = createUser("player2", "p2@test.com", "Player", "Two", Level.INTERMEDIO);
        player3 = createUser("player3", "p3@test.com", "Player", "Three", Level.INTERMEDIO);
        player4 = createUser("player4", "p4@test.com", "Player", "Four", Level.INTERMEDIO);

        // Crea partita di test
        testMatch = new Match();
        testMatch.setLocation("Test Court");
        testMatch.setDateTime(LocalDateTime.now().plusDays(1));
        testMatch.setRequiredLevel(Level.INTERMEDIO);
        testMatch.setType(MatchType.PROPOSTA);
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setCreator(creator);
        testMatch.setCreatedAt(LocalDateTime.now());
        testMatch = matchRepository.save(testMatch);
    }

    /**
     * Test: Verifica che MatchConfirmedEvent venga pubblicato quando 4 giocatori si uniscono.
     * 
     * <h3>Scenario:</h3>
     * <ol>
     *   <li>Partita iniziale in stato WAITING con 0 giocatori</li>
     *   <li>3 giocatori si uniscono → Nessun evento (ancora 3/4)</li>
     *   <li>4° giocatore si unisce → MatchConfirmedEvent pubblicato</li>
     * </ol>
     * 
     * <h3>Cosa verifica:</h3>
     * <ul>
     *   <li>L'evento viene pubblicato esattamente 1 volta</li>
     *   <li>L'evento è del tipo corretto (MatchConfirmedEvent)</li>
     *   <li>L'evento contiene i dati della partita corretta</li>
     *   <li>La partita cambia stato a CONFIRMED</li>
     * </ul>
     */
    @Test
    void testMatchConfirmedEventPublishedWhen4PlayersJoin() {
        // ARRANGE: Join 3 players (non ancora auto-conferma)
        registrationService.joinMatch(player1, testMatch);
        registrationService.joinMatch(player2, testMatch);
        registrationService.joinMatch(player3, testMatch);
        
        // Verifica: ancora nessun evento MatchConfirmedEvent
        long eventsBefore = applicationEvents.stream(MatchConfirmedEvent.class).count();
        assertEquals(0, eventsBefore, "Non dovrebbero esserci eventi prima del 4° giocatore");

        // ACT: Join 4th player → trigger auto-conferma
        registrationService.joinMatch(player4, testMatch);

        // ASSERT: Verifica che MatchConfirmedEvent sia stato pubblicato
        List<MatchConfirmedEvent> confirmedEvents = applicationEvents.stream(MatchConfirmedEvent.class)
                .toList();
        
        assertEquals(1, confirmedEvents.size(), 
            "Dovrebbe essere pubblicato esattamente 1 MatchConfirmedEvent");
        
        MatchConfirmedEvent event = confirmedEvents.get(0);
        assertNotNull(event.getMatch(), "L'evento deve contenere la partita");
        assertEquals(testMatch.getId(), event.getMatch().getId(), 
            "L'evento deve riferirsi alla partita corretta");
        
        // Verifica che lo stato sia cambiato
        Match updated = matchRepository.findById(testMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.CONFIRMED, updated.getStatus(),
            "La partita deve essere CONFIRMED dopo 4 giocatori");
    }

    /**
     * Test: Verifica che MatchFinishedEvent venga pubblicato quando una partita termina.
     * 
     * <h3>Scenario:</h3>
     * <ol>
     *   <li>Partita in stato CONFIRMED con 4 giocatori</li>
     *   <li>Il creatore marca la partita come terminata</li>
     *   <li>MatchFinishedEvent viene pubblicato</li>
     * </ol>
     * 
     * <h3>Cosa verifica:</h3>
     * <ul>
     *   <li>L'evento viene pubblicato esattamente 1 volta</li>
     *   <li>L'evento è del tipo corretto (MatchFinishedEvent)</li>
     *   <li>L'evento contiene i dati della partita corretta</li>
     *   <li>La partita cambia stato a FINISHED</li>
     * </ul>
     */
    @Test
    void testMatchFinishedEventPublishedWhenMatchFinished() {
        // ARRANGE: Prepara partita CONFIRMED con 4 giocatori
        registrationService.joinMatch(player1, testMatch);
        registrationService.joinMatch(player2, testMatch);
        registrationService.joinMatch(player3, testMatch);
        registrationService.joinMatch(player4, testMatch);
        
        Match confirmed = matchRepository.findById(testMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.CONFIRMED, confirmed.getStatus());

        // ACT: Finisci la partita
        matchService.finishMatch(confirmed.getId());

        // ASSERT: Verifica che MatchFinishedEvent sia stato pubblicato
        List<MatchFinishedEvent> finishedEvents = applicationEvents.stream(MatchFinishedEvent.class)
                .toList();
        
        assertEquals(1, finishedEvents.size(),
            "Dovrebbe essere pubblicato esattamente 1 MatchFinishedEvent");
        
        MatchFinishedEvent event = finishedEvents.get(0);
        assertNotNull(event.getMatch(), "L'evento deve contenere la partita");
        assertEquals(confirmed.getId(), event.getMatch().getId(),
            "L'evento deve riferirsi alla partita corretta");
        
        // Verifica che lo stato sia cambiato
        Match finished = matchRepository.findById(testMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.FINISHED, finished.getStatus(),
            "La partita deve essere FINISHED");
    }

    /**
     * Test: Verifica che nessun evento venga pubblicato se la partita non raggiunge 4 giocatori.
     * 
     * <h3>Scenario:</h3>
     * Solo 2 giocatori si uniscono (non abbastanza per auto-conferma)
     * 
     * <h3>Cosa verifica:</h3>
     * Nessun MatchConfirmedEvent pubblicato (partita rimane WAITING)
     */
    @Test
    void testNoEventPublishedIfLessThan4Players() {
        // ACT: Join solo 2 players
        registrationService.joinMatch(player1, testMatch);
        registrationService.joinMatch(player2, testMatch);

        // ASSERT: Nessun evento MatchConfirmedEvent
        long confirmedCount = applicationEvents.stream(MatchConfirmedEvent.class).count();
        assertEquals(0, confirmedCount,
            "Non dovrebbe esserci MatchConfirmedEvent con meno di 4 giocatori");
        
        // Verifica che la partita sia ancora WAITING
        Match stillWaiting = matchRepository.findById(testMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.WAITING, stillWaiting.getStatus());
    }

    /**
     * Test: Verifica che finishMatch possa essere chiamato su una partita CONFIRMED.
     * 
     * <h3>Scenario:</h3>
     * Una partita CONFIRMED può essere terminata senza restrizioni sull'utente
     * 
     * <h3>Nota implementativa:</h3>
     * L'implementazione attuale non verifica CHI chiama finishMatch.
     * La verifica "solo creatore" è gestita lato UI (mostra bottone solo a creatore).
     * Questo è un design choice: logica business delegata al controller.
     * 
     * <h3>Cosa verifica:</h3>
     * MatchFinishedEvent viene pubblicato quando si chiama finishMatch
     */
    @Test
    void testFinishMatchPublishesEvent() {
        // ARRANGE: Partita CONFIRMED
        registrationService.joinMatch(player1, testMatch);
        registrationService.joinMatch(player2, testMatch);
        registrationService.joinMatch(player3, testMatch);
        registrationService.joinMatch(player4, testMatch);

        // ACT: Finisci la partita
        matchService.finishMatch(testMatch.getId());

        // ASSERT: Evento pubblicato
        long finishedCount = applicationEvents.stream(MatchFinishedEvent.class).count();
        assertEquals(1, finishedCount,
            "MatchFinishedEvent deve essere pubblicato quando partita finisce");
        
        // Verifica che lo stato sia FINISHED
        Match finished = matchRepository.findById(testMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.FINISHED, finished.getStatus());
    }

    /**
     * Test: Verifica il contenuto dettagliato dell'evento MatchConfirmedEvent.
     * 
     * <h3>Cosa verifica:</h3>
     * L'evento contiene tutti i dati corretti della partita:
     * <ul>
     *   <li>ID partita</li>
     *   <li>Location</li>
     *   <li>Stato aggiornato</li>
     *   <li>Numero giocatori</li>
     * </ul>
     */
    @Test
    void testMatchConfirmedEventContainsCorrectData() {
        // ACT: Trigger evento
        registrationService.joinMatch(player1, testMatch);
        registrationService.joinMatch(player2, testMatch);
        registrationService.joinMatch(player3, testMatch);
        registrationService.joinMatch(player4, testMatch);

        // ASSERT: Verifica dati nell'evento
        MatchConfirmedEvent event = applicationEvents.stream(MatchConfirmedEvent.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("MatchConfirmedEvent non trovato"));
        
        Match matchInEvent = event.getMatch();
        assertEquals(testMatch.getId(), matchInEvent.getId());
        assertEquals("Test Court", matchInEvent.getLocation());
        assertEquals(MatchStatus.CONFIRMED, matchInEvent.getStatus());
        
        // Verifica numero giocatori dal database (lazy loading non funziona nell'evento)
        Match refreshed = matchRepository.findById(matchInEvent.getId()).orElseThrow();
        int activeCount = registrationService.getActiveRegistrationsCount(refreshed);
        assertEquals(4, activeCount, "Dovrebbero esserci 4 giocatori attivi");
    }

    // Helper method per creare utenti
    private User createUser(String username, String email, String firstName, String lastName, Level level) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword("password");
        user.setDeclaredLevel(level);
        user.setMatchesPlayed(0);
        return userRepository.save(user);
    }
}
