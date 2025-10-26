package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * STRATEGY PATTERN - Implementazione concreta per ordinamento partite per popolarità.
 * 
 * <h2>Ruolo nel Strategy Pattern</h2>
 * Questa è una <strong>Concrete Strategy</strong> che implementa l'algoritmo
 * di ordinamento basato sul numero di giocatori iscritti (popolarità della partita).
 * 
 * <h2>@Component("popularitySorting") - Bean naming per Spring</h2>
 * Il nome del bean "popularitySorting" è usato come chiave nella Map di strategie:
 * <pre>
 * Map&lt;String, MatchSortingStrategy&gt; strategies = {
 *     "dateSorting"       → DateSortingStrategy,
 *     "popularitySorting" → PopularitySortingStrategy,  ◄── Questo bean
 *     "levelSorting"      → LevelSortingStrategy
 * }
 * </pre>
 * 
 * <h2>Come funziona l'auto-injection nella Map</h2>
 * Spring usa il <strong>Type</strong> e il <strong>Name</strong> del bean:
 * <pre>
 * // Spring vede:
 * &#64;Component("popularitySorting")
 * public class PopularitySortingStrategy implements MatchSortingStrategy
 *              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        ^^^^^^^^^^^^^^^^^^^^
 *              Nome bean (chiave Map)                Tipo (tipo Map value)
 * 
 * // Quando inietta:
 * public MatchService(Map&lt;String, MatchSortingStrategy&gt; strategies)
 *                          ^^^^^^  ^^^^^^^^^^^^^^^^^^^^
 *                          Key     Value type
 * 
 * // Spring automaticamente:
 * 1. Trova tutti i bean che implementano MatchSortingStrategy
 * 2. Crea entry nella Map: (bean name, bean instance)
 * 3. Inietta la Map completa
 * </pre>
 * 
 * <h2>Algoritmo di ordinamento:</h2>
 * Ordina le partite in base al numero di iscrizioni attive in ordine <strong>decrescente</strong>:
 * <ul>
 *   <li>Le partite con più giocatori iscritti appaiono per prime</li>
 *   <li>Utile per mostrare le partite più "calde" o richieste</li>
 *   <li>Usa getActiveRegistrationsCount() che conta solo iscrizioni con status JOINED</li>
 * </ul>
 * 
 * <h2>Esempio pratico di ordinamento:</h2>
 * <pre>
 * Input (partite non ordinate):
 *   Match A: Location "Campo 1", Iscritti: 1
 *   Match B: Location "Campo 2", Iscritti: 4  ← Piena
 *   Match C: Location "Campo 3", Iscritti: 2
 * 
 * Output (dopo sort() di questa strategia):
 *   Match B: Location "Campo 2", Iscritti: 4  ← Prima (più popolare)
 *   Match C: Location "Campo 3", Iscritti: 2  ← Seconda
 *   Match A: Location "Campo 1", Iscritti: 1  ← Ultima (meno popolare)
 * </pre>
 * 
 * <h2>Utilizzo dal Controller/Service:</h2>
 * <pre>
 * // Nel WebController:
 * &#64;GetMapping("/matches")
 * public String getMatches(&#64;RequestParam(defaultValue = "dateSorting") String sortBy) {
 *     // User sceglie "Popolarità" → sortBy = "popularitySorting"
 *     List&lt;Match&gt; matches = matchService.getSortedMatches(sortBy);
 *     // matches sono ordinate con questa strategia
 * }
 * 
 * // Nel MatchService:
 * public List&lt;Match&gt; getSortedMatches(String sortBy) {
 *     // Lookup dinamico della strategia
 *     MatchSortingStrategy strategy = sortingStrategies.get(sortBy);
 *     
 *     // Se sortBy = "popularitySorting", strategy è un'istanza di questa classe
 *     return strategy.sort(getAllMatches());
 * }
 * </pre>
 * 
 * <h2>Differenza tra le strategie:</h2>
 * <table border="1">
 *   <tr>
 *     <th>Strategia</th>
 *     <th>Ordina per</th>
 *     <th>Direzione</th>
 *     <th>Caso d'uso</th>
 *   </tr>
 *   <tr>
 *     <td>DateSortingStrategy</td>
 *     <td>Data/Ora</td>
 *     <td>Crescente (prima le più vicine)</td>
 *     <td>Vedere prossime partite</td>
 *   </tr>
 *   <tr>
 *     <td><strong>PopularitySortingStrategy</strong></td>
 *     <td><strong>Num. iscritti</strong></td>
 *     <td><strong>Decrescente (prima le più piene)</strong></td>
 *     <td><strong>Trovare partite quasi piene</strong></td>
 *   </tr>
 *   <tr>
 *     <td>LevelSortingStrategy</td>
 *     <td>Livello richiesto</td>
 *     <td>Crescente (principianti primi)</td>
 *     <td>Filtrare per abilità</td>
 *   </tr>
 * </table>
 * 
 * <h2>Vantaggi dell'isolamento della strategia:</h2>
 * <ul>
 *   <li><strong>Modificabile indipendentemente:</strong> Posso cambiare l'algoritmo senza impattare altre strategie</li>
 *   <li><strong>Testabile:</strong> Test unitario semplice senza dipendenze</li>
 *   <li><strong>Riusabile:</strong> Posso riusare questa logica in altri contesti</li>
 *   <li><strong>Open/Closed:</strong> Aggiungo nuove strategie senza modificare codice esistente</li>
 * </ul>
 * 
 * <h2>Esempio di test:</h2>
 * <pre>
 * &#64;Test
 * public void testPopularitySortingStrategy() {
 *     // Arrange
 *     PopularitySortingStrategy strategy = new PopularitySortingStrategy();
 *     List&lt;Match&gt; matches = List.of(
 *         matchWithRegistrations(1),  // Meno popolare
 *         matchWithRegistrations(4),  // Più popolare
 *         matchWithRegistrations(2)   // Media
 *     );
 *     
 *     // Act
 *     List&lt;Match&gt; sorted = strategy.sort(matches);
 *     
 *     // Assert
 *     assertEquals(4, sorted.get(0).getActiveRegistrationsCount()); // Prima
 *     assertEquals(2, sorted.get(1).getActiveRegistrationsCount()); // Seconda
 *     assertEquals(1, sorted.get(2).getActiveRegistrationsCount()); // Ultima
 * }
 * </pre>
 * 
 * <h2>Estendibilità - Esempio di nuova strategia:</h2>
 * <pre>
 * // Posso aggiungere nuova strategia senza modificare questa o MatchService
 * &#64;Component("availabilitySorting")
 * public class AvailabilitySortingStrategy implements MatchSortingStrategy {
 *     &#64;Override
 *     public List&lt;Match&gt; sort(List&lt;Match&gt; matches) {
 *         // Ordina per posti disponibili (4 - iscritti)
 *         return matches.stream()
 *             .sorted(Comparator.comparing(m -> 4 - m.getActiveRegistrationsCount()))
 *             .toList();
 *     }
 * }
 * // Spring lo inietta automaticamente nella Map!
 * </pre>
 * 
 * @see MatchSortingStrategy Interfaccia del Strategy Pattern
 * @see DateSortingStrategy Strategia per ordinamento per data
 * @see LevelSortingStrategy Strategia per ordinamento per livello
 * @author Padel App Team
 */
@Component("popularitySorting")
public class PopularitySortingStrategy implements MatchSortingStrategy {
    
    /**
     * Ordina le partite per numero di iscritti in ordine decrescente.
     * 
     * <p>Le partite con più giocatori iscritti vengono mostrate per prime.
     * Questo aiuta gli utenti a:
     * <ul>
     *   <li>Trovare partite quasi piene (che stanno per essere confermate)</li>
     *   <li>Vedere quali partite sono più richieste</li>
     *   <li>Unirsi a partite già popolari</li>
     * </ul>
     * 
     * <p><strong>Implementazione:</strong>
     * Usa Comparator.comparing() con .reversed() per ottenere ordine decrescente.
     * 
     * @param matches Lista di partite da ordinare (non viene modificata)
     * @return Nuova lista ordinata per popolarità decrescente
     */
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(Match::getActiveRegistrationsCount).reversed())
                .toList();
    }
    
    /**
     * Restituisce il nome user-friendly di questa strategia.
     * 
     * <p>Questo nome viene mostrato nella UI (dropdown, menu, radio buttons)
     * per permettere all'utente di scegliere come ordinare le partite.
     * 
     * @return "Popolarità" - nome visualizzato all'utente
     */
    @Override
    public String getStrategyName() {
        return "Popolarità";
    }
}
