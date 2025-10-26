package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unit per il Strategy Pattern applicato all'ordinamento delle partite.
 * 
 * <h2>Scopo del file</h2>
 * Questa classe testa in modo isolato le diverse strategie di ordinamento implementate
 * per le partite di padel (DateSortingStrategy, PopularitySortingStrategy, LevelSortingStrategy).
 * Verifica che ogni strategia ordini correttamente i dati secondo il proprio criterio.
 * 
 * <h2>Differenza con Integration Test</h2>
 * <ul>
 *   <li><strong>Unit Test (questa classe)</strong>: NON carica il contesto Spring. Testa
 *       le strategie in completo isolamento creando manualmente gli oggetti necessari.
 *       È molto veloce e focalizzato solo sulla logica di ordinamento.</li>
 *   <li><strong>Integration Test</strong>: Usa @SpringBootTest, carica tutto il contesto,
 *       interagisce con database e service. Più lento ma testa l'integrazione completa.</li>
 * </ul>
 * 
 * <h2>Test isolato delle strategie di sorting</h2>
 * I test verificano il funzionamento del <strong>Strategy Pattern</strong>, un design pattern
 * comportamentale che permette di:
 * <ul>
 *   <li>Definire una famiglia di algoritmi di ordinamento</li>
 *   <li>Incapsulare ciascun algoritmo in una classe separata</li>
 *   <li>Rendere gli algoritmi intercambiabili a runtime</li>
 * </ul>
 * 
 * Ogni strategia implementa l'interfaccia <code>MatchSortingStrategy</code> e può essere
 * facilmente sostituita senza modificare il codice client.
 * 
 * <h2>Perché testare in isolamento</h2>
 * <ul>
 *   <li>Velocità: i test sono quasi istantanei senza Spring context</li>
 *   <li>Semplicità: focus esclusivo sulla logica di ordinamento</li>
 *   <li>Affidabilità: non dipendono da database o configurazioni esterne</li>
 *   <li>Debugging: più facile identificare problemi nella logica pura</li>
 * </ul>
 * 
 * @see com.example.padel_app.strategy.MatchSortingStrategy
 * @see com.example.padel_app.strategy.DateSortingStrategy
 * @see com.example.padel_app.strategy.PopularitySortingStrategy
 * @see com.example.padel_app.strategy.LevelSortingStrategy
 */
class StrategyPatternTest {

    private List<Match> matches;
    private Match match1;
    private Match match2;
    private Match match3;

    @BeforeEach
    void setUp() {
        match1 = new Match();
        match1.setId(1L);
        match1.setLocation("Location 1");
        match1.setDateTime(LocalDateTime.now().plusDays(3));
        match1.setRequiredLevel(Level.AVANZATO);
        match1.setRegistrations(createRegistrations(2)); // 2 players

        match2 = new Match();
        match2.setId(2L);
        match2.setLocation("Location 2");
        match2.setDateTime(LocalDateTime.now().plusDays(1));
        match2.setRequiredLevel(Level.PRINCIPIANTE);
        match2.setRegistrations(createRegistrations(4)); // 4 players

        match3 = new Match();
        match3.setId(3L);
        match3.setLocation("Location 3");
        match3.setDateTime(LocalDateTime.now().plusDays(2));
        match3.setRequiredLevel(Level.INTERMEDIO);
        match3.setRegistrations(createRegistrations(1)); // 1 player

        matches = new ArrayList<>(Arrays.asList(match1, match2, match3));
    }

    private List<Registration> createRegistrations(int count) {
        List<Registration> regs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            regs.add(new Registration());
        }
        return regs;
    }

    /**
     * Testa la strategia di ordinamento per data in ordine crescente.
     * 
     * <h3>Quale strategia viene testata</h3>
     * <strong>DateSortingStrategy</strong>: ordina le partite dalla più imminente alla più lontana.
     * 
     * <h3>Ordine atteso</h3>
     * <strong>Ascendente (crescente)</strong>: le partite con dateTime più vicino vengono prima.
     * Esempio: partita tra 1 giorno → partita tra 2 giorni → partita tra 3 giorni.
     * 
     * <h3>Come verificare che il Pattern Strategy funziona</h3>
     * <ol>
     *   <li>Creiamo 3 partite con date diverse (day+1, day+2, day+3)</li>
     *   <li>Istanziamo la strategia DateSortingStrategy</li>
     *   <li>Invochiamo il metodo sort() dell'interfaccia comune</li>
     *   <li>Verifichiamo che l'ordine sia: day+1, day+2, day+3</li>
     * </ol>
     * 
     * Il test dimostra che possiamo cambiare strategia senza modificare il codice client:
     * basta istanziare una diversa implementazione di MatchSortingStrategy.
     */
    @Test
    void testDateSorting_AscendingOrder() {
        MatchSortingStrategy strategy = new DateSortingStrategy();
        List<Match> sorted = strategy.sort(matches);

        assertEquals(3, sorted.size());
        assertEquals(match2.getId(), sorted.get(0).getId()); // Day 1
        assertEquals(match3.getId(), sorted.get(1).getId()); // Day 2
        assertEquals(match1.getId(), sorted.get(2).getId()); // Day 3
    }

    /**
     * Testa la strategia di ordinamento per popolarità in ordine decrescente.
     * 
     * <h3>Quale strategia viene testata</h3>
     * <strong>PopularitySortingStrategy</strong>: ordina le partite dalla più popolare (più giocatori iscritti)
     * alla meno popolare (meno giocatori).
     * 
     * <h3>Ordine atteso</h3>
     * <strong>Decrescente</strong>: partite con più registrazioni vengono prima.
     * Esempio: 4 giocatori → 2 giocatori → 1 giocatore.
     * 
     * <h3>Come verificare che il Pattern Strategy funziona</h3>
     * <ol>
     *   <li>Creiamo 3 partite con diverso numero di iscritti (4, 2, 1)</li>
     *   <li>Istanziamo PopularitySortingStrategy invece di DateSortingStrategy</li>
     *   <li>Usiamo lo stesso metodo sort() dell'interfaccia comune</li>
     *   <li>Verifichiamo l'ordine: 4 iscritti, 2 iscritti, 1 iscritto</li>
     * </ol>
     * 
     * Questo dimostra la <strong>intercambiabilità</strong> delle strategie: cambiando solo
     * l'istanza, otteniamo un ordinamento completamente diverso con la stessa interfaccia.
     */
    @Test
    void testPopularitySorting_DescendingOrder() {
        MatchSortingStrategy strategy = new PopularitySortingStrategy();
        List<Match> sorted = strategy.sort(matches);

        assertEquals(3, sorted.size());
        assertEquals(match2.getId(), sorted.get(0).getId()); // 4 players
        assertEquals(match1.getId(), sorted.get(1).getId()); // 2 players
        assertEquals(match3.getId(), sorted.get(2).getId()); // 1 player
    }

    /**
     * Testa la strategia di ordinamento per livello in ordine crescente.
     * 
     * <h3>Quale strategia viene testata</h3>
     * <strong>LevelSortingStrategy</strong>: ordina le partite dal livello più basso al più alto.
     * 
     * <h3>Ordine atteso</h3>
     * <strong>Ascendente</strong>: dal livello più facile al più difficile.
     * Esempio: PRINCIPIANTE → INTERMEDIO → AVANZATO → PROFESSIONISTA.
     * 
     * <h3>Come verificare che il Pattern Strategy funziona</h3>
     * <ol>
     *   <li>Creiamo 3 partite con livelli diversi (AVANZATO, PRINCIPIANTE, INTERMEDIO)</li>
     *   <li>Istanziamo LevelSortingStrategy</li>
     *   <li>Invochiamo sort() - ancora la stessa interfaccia!</li>
     *   <li>Verifichiamo l'ordine: PRINCIPIANTE, INTERMEDIO, AVANZATO</li>
     * </ol>
     * 
     * Nota: usiamo <code>ordinal()</code> degli enum perché i livelli sono ordinati
     * nella definizione dell'enum Level, e l'ordinal rappresenta la posizione.
     */
    @Test
    void testLevelSorting_AscendingOrder() {
        MatchSortingStrategy strategy = new LevelSortingStrategy();
        List<Match> sorted = strategy.sort(matches);

        assertEquals(3, sorted.size());
        assertEquals(Level.PRINCIPIANTE, sorted.get(0).getRequiredLevel());
        assertEquals(Level.INTERMEDIO, sorted.get(1).getRequiredLevel());
        assertEquals(Level.AVANZATO, sorted.get(2).getRequiredLevel());
    }

    /**
     * Testa l'intercambiabilità delle strategie - il cuore del Strategy Pattern.
     * 
     * <h3>Quale strategia viene testata</h3>
     * <strong>Tutte e tre</strong> le strategie (Date, Popularity, Level) vengono testate insieme
     * per dimostrare che sono perfettamente intercambiabili.
     * 
     * <h3>Concetto chiave del Pattern</h3>
     * Questo test dimostra il principio fondamentale del Strategy Pattern:
     * <ul>
     *   <li>Tutte le strategie implementano la stessa interfaccia</li>
     *   <li>Possono essere usate in modo polimorfico</li>
     *   <li>Il codice client non deve conoscere i dettagli implementativi</li>
     *   <li>Si può cambiare comportamento semplicemente cambiando l'istanza</li>
     * </ul>
     * 
     * <h3>Come verificare</h3>
     * <ol>
     *   <li>Creiamo una lista con tutte e tre le strategie</li>
     *   <li>Iteriamo sulla lista usando il tipo comune MatchSortingStrategy</li>
     *   <li>Chiamiamo sort() su ciascuna senza sapere quale implementazione è</li>
     *   <li>Verifichiamo che tutte producano risultati validi</li>
     * </ol>
     * 
     * Questo è un esempio perfetto di <strong>polimorfismo</strong> in azione!
     */
    @Test
    void testStrategyInterchangeability() {
        // Test that all strategies work with same input
        List<MatchSortingStrategy> strategies = Arrays.asList(
            new DateSortingStrategy(),
            new PopularitySortingStrategy(),
            new LevelSortingStrategy()
        );

        for (MatchSortingStrategy strategy : strategies) {
            List<Match> sorted = strategy.sort(new ArrayList<>(matches));
            assertNotNull(sorted);
            assertEquals(3, sorted.size());
        }
    }

    /**
     * Testa il comportamento con una lista vuota - caso edge (al limite).
     * 
     * <h3>Quale strategia viene testata</h3>
     * DateSortingStrategy (ma il test è valido per tutte le strategie).
     * 
     * <h3>Caso edge importante</h3>
     * Verifica che l'algoritmo di ordinamento gestisca correttamente il caso limite
     * di una lista vuota senza generare eccezioni.
     * 
     * <h3>Come verificare</h3>
     * <ol>
     *   <li>Creiamo una lista vuota di partite</li>
     *   <li>Invochiamo sort() sulla lista vuota</li>
     *   <li>Verifichiamo che il risultato sia non-null</li>
     *   <li>Verifichiamo che il risultato sia una lista vuota</li>
     * </ol>
     * 
     * <h3>Perché è importante</h3>
     * I casi edge come liste vuote sono comuni (es. nuovo utente senza partite create).
     * L'applicazione non deve crashare ma gestire elegantemente questi scenari.
     */
    @Test
    void testEmptyList() {
        List<Match> emptyList = new ArrayList<>();
        
        MatchSortingStrategy dateStrategy = new DateSortingStrategy();
        List<Match> sorted = dateStrategy.sort(emptyList);

        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    /**
     * Testa il comportamento con una singola partita - altro caso edge.
     * 
     * <h3>Quale strategia viene testata</h3>
     * PopularitySortingStrategy (ma il concetto si applica a tutte).
     * 
     * <h3>Caso edge importante</h3>
     * Una lista con un solo elemento è già "ordinata" per definizione.
     * Verifichiamo che l'algoritmo non alteri o duplichi l'elemento.
     * 
     * <h3>Come verificare</h3>
     * <ol>
     *   <li>Creiamo una lista con una sola partita</li>
     *   <li>Invochiamo sort()</li>
     *   <li>Verifichiamo che il risultato contenga esattamente 1 elemento</li>
     *   <li>Verifichiamo che sia la stessa partita (stesso ID)</li>
     * </ol>
     * 
     * <h3>Perché è importante</h3>
     * Liste con un solo elemento sono comuni nelle prime fasi dell'applicazione
     * o in viste filtrate. Il sistema deve gestirle correttamente senza effetti collaterali.
     */
    @Test
    void testSingleMatch() {
        List<Match> singleMatch = Arrays.asList(match1);
        
        MatchSortingStrategy strategy = new PopularitySortingStrategy();
        List<Match> sorted = strategy.sort(singleMatch);

        assertEquals(1, sorted.size());
        assertEquals(match1.getId(), sorted.get(0).getId());
    }
}
