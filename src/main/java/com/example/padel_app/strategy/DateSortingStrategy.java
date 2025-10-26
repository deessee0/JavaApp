package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * STRATEGY PATTERN - Implementazione concreta per ordinamento partite per data.
 * 
 * <h2>Ruolo nel Strategy Pattern</h2>
 * Questa classe è una <strong>Concrete Strategy</strong> che implementa l'algoritmo
 * di ordinamento specifico per data/ora della partita.
 * 
 * <h2>@Component("dateSorting") - Naming della strategia</h2>
 * L'annotazione @Component registra questa classe come Spring Bean con nome specifico:
 * <pre>
 * &#64;Component("dateSorting")
 *           ^^^^^^^^^^^^^
 *           Nome del bean
 * </pre>
 * 
 * <strong>Perché è importante il nome del bean?</strong>
 * <ul>
 *   <li>Spring usa questo nome come chiave nella Map di strategie</li>
 *   <li>Il controller può selezionare la strategia usando questo nome</li>
 *   <li>Permette lookup dinamico a runtime: strategies.get("dateSorting")</li>
 * </ul>
 * 
 * <h2>Come Spring auto-inietta le strategie in una Map</h2>
 * <pre>
 * // In MatchService:
 * &#64;Service
 * public class MatchService {
 *     
 *     // Spring trova TUTTI i bean che implementano MatchSortingStrategy
 *     private final Map&lt;String, MatchSortingStrategy&gt; sortingStrategies;
 *     
 *     // Constructor Injection - Spring popola automaticamente la map
 *     public MatchService(Map&lt;String, MatchSortingStrategy&gt; strategies) {
 *         this.sortingStrategies = strategies;
 *         
 *         // Map risultante (auto-popolata da Spring):
 *         // {
 *         //   "dateSorting"       → istanza di DateSortingStrategy,
 *         //   "popularitySorting" → istanza di PopularitySortingStrategy,
 *         //   "levelSorting"      → istanza di LevelSortingStrategy
 *         // }
 *     }
 *     
 *     public List&lt;Match&gt; getSortedMatches(String sortBy) {
 *         // Selezione dinamica della strategia
 *         MatchSortingStrategy strategy = sortingStrategies.getOrDefault(
 *             sortBy, 
 *             sortingStrategies.get("dateSorting") // default
 *         );
 *         return strategy.sort(getAllMatches());
 *     }
 * }
 * </pre>
 * 
 * <h2>Meccanismo di auto-injection di Spring:</h2>
 * <pre>
 * 1. Spring Boot Startup
 *    ↓
 * 2. Component Scan trova @Component("dateSorting")
 *    ↓
 * 3. Crea istanza: DateSortingStrategy instance = new DateSortingStrategy()
 *    ↓
 * 4. Registra nel container con nome "dateSorting"
 *    ↓
 * 5. Quando trova Map&lt;String, MatchSortingStrategy&gt; nel constructor:
 *    a) Cerca tutti i bean di tipo MatchSortingStrategy
 *    b) Crea Map con nome bean come chiave
 *    c) Inietta la map completa
 * </pre>
 * 
 * <h2>Esempio d'uso completo - Dal click del user al risultato:</h2>
 * <pre>
 * 1. User seleziona "Ordina per Data" nella UI
 *    ↓
 * 2. Browser invia: GET /matches?sortBy=dateSorting
 *    ↓
 * 3. WebController riceve richiesta
 *    &#64;GetMapping("/matches")
 *    public String getMatches(&#64;RequestParam String sortBy, Model model) {
 *        List&lt;Match&gt; matches = matchService.getSortedMatches(sortBy);
 *        ...
 *    }
 *    ↓
 * 4. MatchService usa la strategia
 *    public List&lt;Match&gt; getSortedMatches(String sortBy) {
 *        MatchSortingStrategy strategy = sortingStrategies.get(sortBy);
 *        // strategy è DateSortingStrategy
 *        return strategy.sort(getAllMatches());
 *    }
 *    ↓
 * 5. DateSortingStrategy.sort() esegue ordinamento
 *    return matches.stream()
 *        .sorted(Comparator.comparing(Match::getDateTime))
 *        .toList();
 *    ↓
 * 6. Partite ordinate per data vengono restituite alla UI
 * </pre>
 * 
 * <h2>Algoritmo di ordinamento:</h2>
 * Questa strategia ordina le partite in ordine <strong>cronologico crescente</strong>:
 * <ul>
 *   <li>Le partite più vicine nel tempo appaiono per prime</li>
 *   <li>Utile per vedere le prossime partite disponibili</li>
 *   <li>Usa il campo dateTime della partita per il confronto</li>
 * </ul>
 * 
 * <h2>Benefici dell'approccio Strategy:</h2>
 * <ul>
 *   <li><strong>Isolamento:</strong> La logica di ordinamento è isolata in questa classe</li>
 *   <li><strong>Testabilità:</strong> Posso testare questo algoritmo indipendentemente</li>
 *   <li><strong>Riusabilità:</strong> Posso usare questa strategia in altri contesti</li>
 *   <li><strong>Manutenibilità:</strong> Modifiche a questo ordinamento non impattano altre strategie</li>
 * </ul>
 * 
 * <h2>Esempio di test unitario:</h2>
 * <pre>
 * &#64;Test
 * public void testDateSortingStrategy() {
 *     // Arrange
 *     DateSortingStrategy strategy = new DateSortingStrategy();
 *     List&lt;Match&gt; matches = List.of(
 *         createMatch("2025-10-30 10:00"),
 *         createMatch("2025-10-26 14:00"),  // Più vicina
 *         createMatch("2025-11-05 18:00")
 *     );
 *     
 *     // Act
 *     List&lt;Match&gt; sorted = strategy.sort(matches);
 *     
 *     // Assert
 *     assertEquals("2025-10-26 14:00", sorted.get(0).getDateTime());
 *     assertEquals("2025-10-30 10:00", sorted.get(1).getDateTime());
 *     assertEquals("2025-11-05 18:00", sorted.get(2).getDateTime());
 * }
 * </pre>
 * 
 * <h2>Pattern correlati utilizzati:</h2>
 * <ul>
 *   <li><strong>Strategy Pattern:</strong> Questa classe è una strategia concreta</li>
 *   <li><strong>Dependency Injection:</strong> Spring inietta automaticamente nella Map</li>
 *   <li><strong>Registry Pattern:</strong> La Map di strategie funge da registry</li>
 * </ul>
 * 
 * @see MatchSortingStrategy Interfaccia del Strategy Pattern
 * @see PopularitySortingStrategy Altra strategia per ordinamento per popolarità
 * @see LevelSortingStrategy Altra strategia per ordinamento per livello
 * @author Padel App Team
 */
@Component("dateSorting")
public class DateSortingStrategy implements MatchSortingStrategy {
    
    /**
     * Ordina le partite per data/ora crescente.
     * 
     * <p>Le partite più vicine nel tempo appaiono per prime nella lista.
     * Questo è utile per mostrare all'utente le prossime partite disponibili.
     * 
     * <p><strong>Implementazione:</strong>
     * Usa Stream API di Java 8+ con Comparator.comparing() per ordinare
     * in base al campo dateTime di ogni Match.
     * 
     * @param matches Lista di partite da ordinare (non viene modificata)
     * @return Nuova lista ordinata per data crescente
     */
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(Match::getDateTime))
                .toList();
    }
    
    /**
     * Restituisce il nome user-friendly di questa strategia.
     * 
     * <p>Questo nome viene mostrato nella UI per permettere all'utente
     * di scegliere il tipo di ordinamento preferito.
     * 
     * @return "Data" - nome visualizzato nella dropdown/menu della UI
     */
    @Override
    public String getStrategyName() {
        return "Data";
    }
}
