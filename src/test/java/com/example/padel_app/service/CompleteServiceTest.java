package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione completo per il service layer dell'applicazione Padel.
 * 
 * <h2>Scopo del file</h2>
 * Questa classe verifica il corretto funzionamento dell'intero service layer attraverso
 * test di integrazione che coinvolgono più componenti (service, repository, database).
 * 
 * <h2>@SpringBootTest vs Unit Test</h2>
 * <ul>
 *   <li><strong>@SpringBootTest</strong>: Carica l'intero contesto Spring, inclusi tutti i bean,
 *       repository, service e configurazioni. Permette di testare l'interazione reale tra componenti.</li>
 *   <li><strong>Unit Test</strong>: Testa singole classi in isolamento usando mock, senza Spring context.
 *       È più veloce ma non verifica l'integrazione reale.</li>
 * </ul>
 * 
 * <h2>@Transactional per rollback automatico</h2>
 * L'annotazione @Transactional fa sì che ogni test venga eseguito in una transazione che viene
 * automaticamente annullata (rollback) al termine del test. Questo garantisce che:
 * <ul>
 *   <li>Ogni test parte con un database pulito</li>
 *   <li>I test non si influenzano a vicenda</li>
 *   <li>Non è necessario pulire manualmente i dati dopo ogni test</li>
 * </ul>
 * 
 * <h2>Setup dati di test con @BeforeEach</h2>
 * Il metodo annotato con @BeforeEach viene eseguito prima di ogni test, creando un set
 * di dati di test consistente (5 utenti e 1 partita). Questo assicura che ogni test
 * parta dalle stesse condizioni iniziali.
 * 
 * @see org.springframework.boot.test.context.SpringBootTest
 * @see org.springframework.transaction.annotation.Transactional
 */
@SpringBootTest
@Transactional
public class CompleteServiceTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private User testUser4;
    private User testUser5;
    private Match testMatch;

    @BeforeEach
    void createTestData() {
        // Create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@test.com");
        testUser1.setFirstName("Test1");
        testUser1.setLastName("User1");
        testUser1.setPassword("password");
        testUser1.setDeclaredLevel(Level.INTERMEDIO);
        testUser1.setMatchesPlayed(0);
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@test.com");
        testUser2.setFirstName("Test2");
        testUser2.setLastName("User2");
        testUser2.setPassword("password");
        testUser2.setDeclaredLevel(Level.INTERMEDIO);
        testUser2.setMatchesPlayed(0);
        testUser2 = userRepository.save(testUser2);

        testUser3 = new User();
        testUser3.setUsername("testuser3");
        testUser3.setEmail("test3@test.com");
        testUser3.setFirstName("Test3");
        testUser3.setLastName("User3");
        testUser3.setPassword("password");
        testUser3.setDeclaredLevel(Level.AVANZATO);
        testUser3.setMatchesPlayed(0);
        testUser3 = userRepository.save(testUser3);

        testUser4 = new User();
        testUser4.setUsername("testuser4");
        testUser4.setEmail("test4@test.com");
        testUser4.setFirstName("Test4");
        testUser4.setLastName("User4");
        testUser4.setPassword("password");
        testUser4.setDeclaredLevel(Level.PRINCIPIANTE);
        testUser4.setMatchesPlayed(0);
        testUser4 = userRepository.save(testUser4);

        testUser5 = new User();
        testUser5.setUsername("testuser5");
        testUser5.setEmail("test5@test.com");
        testUser5.setFirstName("Test5");
        testUser5.setLastName("User5");
        testUser5.setPassword("password");
        testUser5.setDeclaredLevel(Level.PROFESSIONISTA);
        testUser5.setMatchesPlayed(0);
        testUser5 = userRepository.save(testUser5);

        // Create test match
        testMatch = new Match();
        testMatch.setLocation("Test Location");
        testMatch.setDateTime(LocalDateTime.now().plusDays(1));
        testMatch.setRequiredLevel(Level.INTERMEDIO);
        testMatch.setType(MatchType.PROPOSTA);
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setCreator(testUser1);
        testMatch.setCreatedAt(LocalDateTime.now());
        testMatch = matchRepository.save(testMatch);
    }

    /**
     * Testa la creazione di una nuova partita.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Un utente esistente nel sistema</li>
     *   <li><strong>WHEN</strong>: L'utente crea una nuova partita con tutti i dati necessari</li>
     *   <li><strong>THEN</strong>: La partita viene salvata nel database con ID generato e stato WAITING</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Una partita appena creata deve essere inizialmente in stato WAITING (in attesa di giocatori)
     * e deve avere tutti i campi obbligatori compilati (location, dateTime, level, creator).
     * 
     * <h3>Perché è importante</h3>
     * Questo test verifica la funzionalità base dell'applicazione: la creazione di nuove partite.
     * Se questo test fallisce, gli utenti non possono organizzare partite, rendendo l'app inutilizzabile.
     */
    @Test
    void testCreateMatch() {
        Match newMatch = new Match();
        newMatch.setLocation("New Match Location");
        newMatch.setDateTime(LocalDateTime.now().plusDays(2));
        newMatch.setRequiredLevel(Level.AVANZATO);
        newMatch.setType(MatchType.PROPOSTA);
        newMatch.setStatus(MatchStatus.WAITING);
        newMatch.setCreator(testUser1);
        newMatch.setCreatedAt(LocalDateTime.now());

        Match saved = matchRepository.save(newMatch);

        assertNotNull(saved.getId());
        assertEquals("New Match Location", saved.getLocation());
        assertEquals(MatchStatus.WAITING, saved.getStatus());
        assertEquals(testUser1, saved.getCreator());
    }

    /**
     * Testa l'iscrizione di un giocatore a una partita.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Una partita esistente in stato WAITING</li>
     *   <li><strong>WHEN</strong>: Un utente si iscrive alla partita</li>
     *   <li><strong>THEN</strong>: Viene creata una registrazione attiva collegata all'utente e alla partita</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Gli utenti devono potersi iscrivere alle partite disponibili. Ogni iscrizione crea un record
     * di tipo Registration che collega l'utente alla partita.
     * 
     * <h3>Perché è importante</h3>
     * Senza questa funzionalità, le partite rimarrebbero vuote. È fondamentale per permettere
     * la formazione di gruppi di giocatori.
     */
    @Test
    void testJoinMatch() {
        Registration reg = registrationService.joinMatch(testUser2, testMatch);

        assertNotNull(reg);
        assertEquals(testUser2, reg.getUser());
        assertEquals(testMatch, reg.getMatch());
        
        List<Registration> active = registrationService.getActiveRegistrationsByMatch(testMatch);
        assertTrue(active.size() >= 1);
    }

    /**
     * Testa la conferma automatica della partita quando si raggiungono 4 giocatori.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Una partita in stato WAITING con meno di 4 giocatori</li>
     *   <li><strong>WHEN</strong>: Si iscrivono progressivamente 4 giocatori</li>
     *   <li><strong>THEN</strong>: Lo stato della partita passa automaticamente a CONFIRMED al 4° giocatore</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Una partita di padel richiede esattamente 4 giocatori (2vs2). Quando viene raggiunto
     * questo numero, la partita deve essere automaticamente confermata e pronta per essere giocata.
     * 
     * <h3>Perché è importante</h3>
     * Questo test verifica una business logic critica: l'auto-conferma delle partite.
     * Garantisce che gli utenti vengano notificati automaticamente quando la partita è pronta,
     * senza bisogno di intervento manuale.
     */
    @Test
    void testAutoConfirmAt4Players() {
        // Join 4 players
        registrationService.joinMatch(testUser1, testMatch);
        assertEquals(MatchStatus.WAITING, matchRepository.findById(testMatch.getId()).get().getStatus());

        registrationService.joinMatch(testUser2, testMatch);
        assertEquals(MatchStatus.WAITING, matchRepository.findById(testMatch.getId()).get().getStatus());

        registrationService.joinMatch(testUser3, testMatch);
        assertEquals(MatchStatus.WAITING, matchRepository.findById(testMatch.getId()).get().getStatus());

        registrationService.joinMatch(testUser4, testMatch);
        
        // Match should be CONFIRMED after 4th player
        Match updated = matchRepository.findById(testMatch.getId()).get();
        assertEquals(MatchStatus.CONFIRMED, updated.getStatus());
    }

    /**
     * Testa il limite massimo di 4 giocatori per partita.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Una partita con già 4 giocatori iscritti</li>
     *   <li><strong>WHEN</strong>: Un 5° giocatore tenta di iscriversi</li>
     *   <li><strong>THEN</strong>: Il sistema lancia un'eccezione IllegalStateException</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Non possono esserci più di 4 giocatori in una partita di padel. Questa è una regola
     * ferrea dello sport che il sistema deve far rispettare.
     * 
     * <h3>Perché è importante</h3>
     * Questo test verifica che il sistema impedisca situazioni invalide. Senza questo controllo,
     * potrebbero verificarsi overbooking con più di 4 giocatori, creando confusione e conflitti.
     */
    @Test
    void testMaxPlayersLimit() {
        // Join 4 players
        registrationService.joinMatch(testUser1, testMatch);
        registrationService.joinMatch(testUser2, testMatch);
        registrationService.joinMatch(testUser3, testMatch);
        registrationService.joinMatch(testUser4, testMatch);

        // 5th player should fail
        assertThrows(IllegalStateException.class, () -> {
            registrationService.joinMatch(testUser5, testMatch);
        });
    }

    /**
     * Testa che uno stesso utente non possa iscriversi due volte alla stessa partita.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Un utente già iscritto a una partita</li>
     *   <li><strong>WHEN</strong>: Lo stesso utente tenta di iscriversi nuovamente</li>
     *   <li><strong>THEN</strong>: Il sistema lancia un'eccezione IllegalStateException</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Ogni utente può iscriversi solo una volta a ciascuna partita. Iscrizioni duplicate
     * non hanno senso logico e causerebbero inconsistenze nei dati.
     * 
     * <h3>Perché è importante</h3>
     * Protegge l'integrità dei dati e previene bug nell'interfaccia utente o errori di rete
     * che potrebbero causare iscrizioni duplicate accidentali.
     */
    @Test
    void testDuplicateRegistrationConstraint() {
        registrationService.joinMatch(testUser1, testMatch);

        // Same user cannot join twice
        assertThrows(IllegalStateException.class, () -> {
            registrationService.joinMatch(testUser1, testMatch);
        });
    }

    /**
     * Testa la cancellazione dell'iscrizione di un utente da una partita.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Un utente iscritto a una partita</li>
     *   <li><strong>WHEN</strong>: L'utente decide di cancellarsi dalla partita</li>
     *   <li><strong>THEN</strong>: La registrazione viene rimossa e il conteggio giocatori diminuisce</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Gli utenti devono poter annullare la loro partecipazione fino a quando la partita
     * non è iniziata, permettendo ad altri di prendere il loro posto.
     * 
     * <h3>Perché è importante</h3>
     * Garantisce flessibilità agli utenti che hanno imprevisti. Senza questa funzione,
     * un utente impossibilitato a partecipare bloccherebbe inutilmente un posto.
     */
    @Test
    void testLeaveMatch() {
        registrationService.joinMatch(testUser2, testMatch);
        
        List<Registration> before = registrationService.getActiveRegistrationsByMatch(testMatch);
        int countBefore = before.size();

        registrationService.leaveMatch(testUser2, testMatch);

        List<Registration> after = registrationService.getActiveRegistrationsByMatch(testMatch);
        assertTrue(after.size() < countBefore);
    }

    /**
     * Testa la creazione di un feedback da un giocatore a un altro.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Due utenti che hanno giocato insieme in una partita</li>
     *   <li><strong>WHEN</strong>: Un utente lascia un feedback sull'altro con livello suggerito e commento</li>
     *   <li><strong>THEN</strong>: Il feedback viene salvato con tutti i riferimenti corretti</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Dopo ogni partita, i giocatori possono valutarsi a vicenda per aiutare il sistema
     * a determinare il livello percepito di ciascun giocatore, migliorando il matchmaking.
     * 
     * <h3>Perché è importante</h3>
     * Il sistema di feedback è fondamentale per identificare il vero livello dei giocatori
     * e garantire partite equilibrate e divertenti per tutti.
     */
    @Test
    void testCreateFeedback() {
        Feedback feedback = feedbackService.createFeedback(
            testUser1, testUser2, testMatch, Level.AVANZATO, "Ottimo giocatore"
        );

        assertNotNull(feedback);
        assertEquals(testUser1, feedback.getAuthor());
        assertEquals(testUser2, feedback.getTargetUser());
        assertEquals(Level.AVANZATO, feedback.getSuggestedLevel());
    }

    /**
     * Testa il vincolo di un solo feedback per coppia utente-partita.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Un utente che ha già lasciato feedback su un altro dopo una partita</li>
     *   <li><strong>WHEN</strong>: Lo stesso utente tenta di lasciare un secondo feedback sulla stessa persona per la stessa partita</li>
     *   <li><strong>THEN</strong>: Il sistema lancia un'eccezione RuntimeException</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Ogni giocatore può lasciare un solo feedback per ciascun compagno/avversario in ogni partita.
     * Feedback multipli altererebbero ingiustamente il calcolo del livello percepito.
     * 
     * <h3>Perché è importante</h3>
     * Previene abusi del sistema di rating (es. voti multipli per favorire o penalizzare un giocatore)
     * e mantiene l'equità nella valutazione.
     */
    @Test
    void testOneFeedbackPerUserPerMatch() {
        feedbackService.createFeedback(testUser1, testUser2, testMatch, Level.INTERMEDIO, "Bravo");

        // Second feedback should fail
        assertThrows(RuntimeException.class, () -> {
            feedbackService.createFeedback(testUser1, testUser2, testMatch, Level.AVANZATO, "Altro");
        });
    }

    /**
     * Testa il calcolo automatico del livello percepito basato sui feedback ricevuti.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Un utente che ha ricevuto 3 feedback con livelli suggeriti diversi</li>
     *   <li><strong>WHEN</strong>: Viene invocato il metodo updatePerceivedLevel</li>
     *   <li><strong>THEN</strong>: Il livello percepito viene calcolato come media dei feedback (INTERMEDIO)</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Il livello percepito di un giocatore è la media aritmetica dei livelli suggeriti dai
     * compagni di gioco nei vari feedback ricevuti. Questo valore è più affidabile del
     * livello dichiarato autonomamente.
     * 
     * <h3>Perché è importante</h3>
     * Il livello percepito basato su feedback reali migliora la qualità del matchmaking,
     * evitando partite sbilanciate causate da autovalutazioni errate (troppo alte o troppo basse).
     */
    @Test
    void testPerceivedLevelUpdate() {
        // Create multiple feedbacks
        feedbackService.createFeedback(testUser1, testUser5, testMatch, Level.PRINCIPIANTE, "Test");
        feedbackService.createFeedback(testUser2, testUser5, testMatch, Level.INTERMEDIO, "Test");
        feedbackService.createFeedback(testUser3, testUser5, testMatch, Level.INTERMEDIO, "Test");

        // Update perceived level
        feedbackService.updatePerceivedLevel(testUser5.getId());

        User updated = userRepository.findById(testUser5.getId()).get();
        assertNotNull(updated.getPerceivedLevel());
        // Average of PRINCIPIANTE(0), INTERMEDIO(1), INTERMEDIO(1) = 0.67 ≈ 1 (INTERMEDIO)
        assertEquals(Level.INTERMEDIO, updated.getPerceivedLevel());
    }

    /**
     * Testa l'ordinamento delle partite per data (Strategy Pattern).
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Diverse partite con date diverse nel database</li>
     *   <li><strong>WHEN</strong>: Viene richiesto l'ordinamento per data</li>
     *   <li><strong>THEN</strong>: Le partite sono restituite in ordine cronologico ascendente</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * L'ordinamento per data mostra prima le partite più imminenti, aiutando gli utenti
     * a trovare partite che si svolgeranno a breve.
     * 
     * <h3>Perché è importante</h3>
     * Verifica che il Strategy Pattern funzioni correttamente per l'ordinamento temporale.
     * Gli utenti tipicamente vogliono vedere prima le partite più vicine nel tempo.
     */
    @Test
    void testStrategyDateSorting() {
        List<Match> sorted = matchService.getMatchesOrderedByDate();
        assertNotNull(sorted);
        
        // Verify sorted by date ascending
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getDateTime().compareTo(sorted.get(i + 1).getDateTime()) <= 0);
        }
    }

    /**
     * Testa l'ordinamento delle partite per popolarità (numero di iscritti).
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Partite con diversi numeri di giocatori iscritti</li>
     *   <li><strong>WHEN</strong>: Viene richiesto l'ordinamento per popolarità</li>
     *   <li><strong>THEN</strong>: Le partite sono ordinate in modo decrescente per numero di iscritti</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Le partite più popolari (con più iscritti) vengono mostrate per prime, indicando
     * partite quasi complete e quindi più probabili di essere confermate presto.
     * 
     * <h3>Perché è importante</h3>
     * Verifica il Strategy Pattern per la popolarità. Gli utenti preferiscono unirsi a
     * partite quasi piene perché hanno maggiori probabilità di essere giocate.
     */
    @Test
    void testStrategyPopularitySorting() {
        List<Match> sorted = matchService.getMatchesOrderedByPopularity();
        assertNotNull(sorted);
        
        // Verify sorted by registrations count descending
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getActiveRegistrationsCount() >= sorted.get(i + 1).getActiveRegistrationsCount());
        }
    }

    /**
     * Testa l'ordinamento delle partite per livello richiesto.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Partite con diversi livelli richiesti (PRINCIPIANTE, INTERMEDIO, AVANZATO)</li>
     *   <li><strong>WHEN</strong>: Viene richiesto l'ordinamento per livello</li>
     *   <li><strong>THEN</strong>: Le partite sono ordinate in modo ascendente (dal più facile al più difficile)</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * L'ordinamento per livello aiuta i giocatori a trovare rapidamente partite adatte
     * alle loro capacità, dalle più semplici alle più competitive.
     * 
     * <h3>Perché è importante</h3>
     * Verifica il Strategy Pattern per il livello. Un buon matchmaking basato sul livello
     * è essenziale per la soddisfazione degli utenti: partite troppo facili annoiano,
     * troppo difficili scoraggiano.
     */
    @Test
    void testStrategyLevelSorting() {
        List<Match> sorted = matchService.getMatchesOrderedByLevel();
        assertNotNull(sorted);
        
        // Verify sorted by level ascending
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getRequiredLevel().ordinal() <= sorted.get(i + 1).getRequiredLevel().ordinal());
        }
    }

    /**
     * Testa il filtraggio delle partite per stato.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Partite in vari stati (WAITING, CONFIRMED, FINISHED)</li>
     *   <li><strong>WHEN</strong>: Viene richiesto il filtro per uno stato specifico (es. WAITING)</li>
     *   <li><strong>THEN</strong>: Vengono restituite solo le partite in quello stato</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * Gli utenti devono poter filtrare le partite per stato per trovare facilmente:
     * partite aperte (WAITING), partite confermate (CONFIRMED) o storico (FINISHED).
     * 
     * <h3>Perché è importante</h3>
     * Verifica che il filtraggio funzioni correttamente. Senza filtri efficaci, l'utente
     * vedrebbe tutte le partite mescolate, rendendo difficile trovare quelle rilevanti.
     */
    @Test
    void testFilterByStatus() {
        List<Match> waiting = matchService.getMatchesByStatus(MatchStatus.WAITING);
        for (Match m : waiting) {
            assertEquals(MatchStatus.WAITING, m.getStatus());
        }
    }

    /**
     * Testa il filtraggio delle partite per livello richiesto.
     * 
     * <h3>Scenario (GIVEN-WHEN-THEN)</h3>
     * <ul>
     *   <li><strong>GIVEN</strong>: Partite con diversi livelli richiesti</li>
     *   <li><strong>WHEN</strong>: Viene richiesto il filtro per un livello specifico (es. INTERMEDIO)</li>
     *   <li><strong>THEN</strong>: Vengono restituite solo le partite di quel livello</li>
     * </ul>
     * 
     * <h3>Business Rule</h3>
     * I giocatori devono poter cercare partite adatte al proprio livello, escludendo
     * quelle troppo facili o troppo difficili.
     * 
     * <h3>Perché è importante</h3>
     * Il filtraggio per livello è fondamentale per l'esperienza utente. Permette ai giocatori
     * di trovare rapidamente partite competitive e bilanciate, evitando frustrazioni.
     */
    @Test
    void testFilterByLevel() {
        List<Match> intermediate = matchService.getMatchesByLevel(Level.INTERMEDIO);
        for (Match m : intermediate) {
            assertEquals(Level.INTERMEDIO, m.getRequiredLevel());
        }
    }
}
