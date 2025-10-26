package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * STRATEGY PATTERN - Implementazione concreta per ordinamento partite per livello richiesto.
 * 
 * <h2>Ruolo nel Strategy Pattern</h2>
 * Questa è una <strong>Concrete Strategy</strong> che implementa l'algoritmo
 * di ordinamento basato sul livello di abilità richiesto per la partita.
 * 
 * <h2>@Component("levelSorting") - Naming convention</h2>
 * Il nome del bean segue il pattern camelCase e descrive la funzionalità:
 * <pre>
 * &#64;Component("levelSorting")
 *           ^^^^^^^^^^^^
 *           - Inizia con minuscola (camelCase)
 *           - Suffisso "Sorting" identifica tutte le strategie di ordinamento
 *           - Prefisso "level" identifica il criterio specifico
 * 
 * Convenzioni di naming per le strategie:
 *   "dateSorting"       ← Per data
 *   "popularitySorting" ← Per popolarità
 *   "levelSorting"      ← Per livello (questa classe)
 *   "distanceSorting"   ← (Esempio futuro) Per distanza
 * </pre>
 * 
 * <h2>Auto-injection in Map - Dettaglio tecnico</h2>
 * Spring esegue questi passaggi durante il bootstrap:
 * <pre>
 * Fase 1: Component Scan
 *   → Trova tutte le classi con @Component/@Service/@Repository
 *   → Registra LevelSortingStrategy con nome "levelSorting"
 * 
 * Fase 2: Dependency Resolution
 *   → MatchService richiede Map&lt;String, MatchSortingStrategy&gt;
 *   → Spring cerca tutti i bean di tipo MatchSortingStrategy
 * 
 * Fase 3: Map Population
 *   → Crea Map vuota
 *   → Per ogni bean trovato:
 *        key   = nome bean (es. "levelSorting")
 *        value = istanza bean (es. LevelSortingStrategy instance)
 *   → Inietta Map nel constructor di MatchService
 * 
 * Risultato finale:
 *   Map&lt;String, MatchSortingStrategy&gt; strategies = {
 *       "dateSorting"       → DateSortingStrategy@1a2b,
 *       "popularitySorting" → PopularitySortingStrategy@3c4d,
 *       "levelSorting"      → LevelSortingStrategy@5e6f      ◄── Questa istanza
 *   }
 * </pre>
 * 
 * <h2>Algoritmo di ordinamento:</h2>
 * Ordina le partite per livello di abilità richiesto in ordine <strong>crescente</strong>:
 * <ul>
 *   <li>BEGINNER (principianti) appaiono per primi</li>
 *   <li>INTERMEDIATE (intermedi) in mezzo</li>
 *   <li>ADVANCED (avanzati) per ultimi</li>
 * </ul>
 * 
 * <h2>Come funziona ordinal() con gli Enum:</h2>
 * <pre>
 * // Enum Level (esempio):
 * public enum Level {
 *     BEGINNER,      // ordinal() = 0
 *     INTERMEDIATE,  // ordinal() = 1
 *     ADVANCED       // ordinal() = 2
 * }
 * 
 * // Ordinamento usa ordinal() per il confronto numerico:
 * Match A: requiredLevel = ADVANCED     → ordinal() = 2
 * Match B: requiredLevel = BEGINNER     → ordinal() = 0
 * Match C: requiredLevel = INTERMEDIATE → ordinal() = 1
 * 
 * Dopo sort():
 * Match B (BEGINNER = 0)     ← Prima
 * Match C (INTERMEDIATE = 1) ← Seconda
 * Match A (ADVANCED = 2)     ← Ultima
 * </pre>
 * 
 * <h2>Caso d'uso - Perché ordinare per livello?</h2>
 * <ul>
 *   <li><strong>Giocatori principianti:</strong> Vedono prima partite adatte a loro</li>
 *   <li><strong>Progressione:</strong> User vede prima il suo livello, poi livelli superiori</li>
 *   <li><strong>Accessibilità:</strong> Partite per tutti i livelli sono visibili</li>
 *   <li><strong>Filtro implicito:</strong> Non serve UI complessa per filtrare</li>
 * </ul>
 * 
 * <h2>Flusso completo - Esempio concreto:</h2>
 * <pre>
 * 1. User apre pagina "/matches"
 *    ↓
 * 2. User seleziona dropdown "Ordina per": "Livello"
 *    ↓
 * 3. JavaScript invia: GET /matches?sortBy=levelSorting
 *    ↓
 * 4. WebController riceve richiesta
 *    &#64;GetMapping("/matches")
 *    public String matches(&#64;RequestParam String sortBy, Model model) {
 *        List&lt;Match&gt; matches = matchService.getSortedMatches(sortBy);
 *        model.addAttribute("matches", matches);
 *        return "matches";
 *    }
 *    ↓
 * 5. MatchService seleziona strategia
 *    public List&lt;Match&gt; getSortedMatches(String sortBy) {
 *        MatchSortingStrategy strategy = sortingStrategies.get(sortBy);
 *        // strategy = LevelSortingStrategy instance
 *        return strategy.sort(getAllMatches());
 *    }
 *    ↓
 * 6. Questa classe esegue sort()
 *    matches.stream()
 *        .sorted(Comparator.comparing(m -> m.getRequiredLevel().ordinal()))
 *        .toList();
 *    ↓
 * 7. Partite ordinate per livello ritornano al controller
 *    ↓
 * 8. Thymeleaf renderizza lista ordinata
 *    ↓
 * 9. User vede partite ordinate da BEGINNER ad ADVANCED
 * </pre>
 * 
 * <h2>Confronto tra le tre strategie implementate:</h2>
 * <table border="1">
 *   <tr>
 *     <th>Strategia</th>
 *     <th>Bean Name</th>
 *     <th>Campo ordinato</th>
 *     <th>Tipo ordinamento</th>
 *   </tr>
 *   <tr>
 *     <td>DateSortingStrategy</td>
 *     <td>"dateSorting"</td>
 *     <td>match.dateTime</td>
 *     <td>Crescente (prima le più vicine)</td>
 *   </tr>
 *   <tr>
 *     <td>PopularitySortingStrategy</td>
 *     <td>"popularitySorting"</td>
 *     <td>match.activeRegistrationsCount</td>
 *     <td>Decrescente (prima le più piene)</td>
 *   </tr>
 *   <tr>
 *     <td><strong>LevelSortingStrategy</strong></td>
 *     <td><strong>"levelSorting"</strong></td>
 *     <td><strong>match.requiredLevel.ordinal()</strong></td>
 *     <td><strong>Crescente (prima principianti)</strong></td>
 *   </tr>
 * </table>
 * 
 * <h2>Benefici del Strategy Pattern evidenziati qui:</h2>
 * <ul>
 *   <li><strong>Single Responsibility:</strong> Questa classe ha una sola responsabilità: ordinare per livello</li>
 *   <li><strong>Open/Closed Principle:</strong> Aggiungere nuove strategie non modifica questa classe</li>
 *   <li><strong>Dependency Inversion:</strong> Dipende dall'interfaccia MatchSortingStrategy, non da implementazioni</li>
 *   <li><strong>Liskov Substitution:</strong> Posso sostituire questa strategia con qualsiasi altra senza rompere il codice</li>
 * </ul>
 * 
 * <h2>Esempio di test unitario:</h2>
 * <pre>
 * &#64;Test
 * public void testLevelSortingStrategy() {
 *     // Arrange
 *     LevelSortingStrategy strategy = new LevelSortingStrategy();
 *     List&lt;Match&gt; matches = List.of(
 *         createMatchWithLevel(Level.ADVANCED),
 *         createMatchWithLevel(Level.BEGINNER),
 *         createMatchWithLevel(Level.INTERMEDIATE)
 *     );
 *     
 *     // Act
 *     List&lt;Match&gt; sorted = strategy.sort(matches);
 *     
 *     // Assert - Ordine: BEGINNER, INTERMEDIATE, ADVANCED
 *     assertEquals(Level.BEGINNER, sorted.get(0).getRequiredLevel());
 *     assertEquals(Level.INTERMEDIATE, sorted.get(1).getRequiredLevel());
 *     assertEquals(Level.ADVANCED, sorted.get(2).getRequiredLevel());
 * }
 * </pre>
 * 
 * <h2>Aggiungere nuova strategia senza modificare esistenti:</h2>
 * <pre>
 * // Esempio: ordinamento per prezzo
 * &#64;Component("priceSorting")
 * public class PriceSortingStrategy implements MatchSortingStrategy {
 *     &#64;Override
 *     public List&lt;Match&gt; sort(List&lt;Match&gt; matches) {
 *         return matches.stream()
 *             .sorted(Comparator.comparing(Match::getPrice))
 *             .toList();
 *     }
 *     
 *     &#64;Override
 *     public String getStrategyName() {
 *         return "Prezzo";
 *     }
 * }
 * 
 * // Spring lo inietta automaticamente:
 * // strategies.get("priceSorting") → PriceSortingStrategy instance
 * // NESSUNA modifica necessaria a MatchService, Controller, o altre strategie!
 * </pre>
 * 
 * @see MatchSortingStrategy Interfaccia del Strategy Pattern
 * @see DateSortingStrategy Strategia per ordinamento per data
 * @see PopularitySortingStrategy Strategia per ordinamento per popolarità
 * @see com.example.padel_app.model.enums.Level Enum dei livelli di abilità
 * @author Padel App Team
 */
@Component("levelSorting")
public class LevelSortingStrategy implements MatchSortingStrategy {
    
    /**
     * Ordina le partite per livello di abilità richiesto in ordine crescente.
     * 
     * <p>L'ordinamento usa il metodo ordinal() dell'enum Level:
     * <ul>
     *   <li>BEGINNER (ordinal = 0) viene prima</li>
     *   <li>INTERMEDIATE (ordinal = 1) in mezzo</li>
     *   <li>ADVANCED (ordinal = 2) per ultimo</li>
     * </ul>
     * 
     * <p>Questo permette ai giocatori di vedere prima le partite adatte al loro livello,
     * facilitando la selezione e l'iscrizione.
     * 
     * <p><strong>Implementazione:</strong>
     * Usa method reference e ordinal() dell'enum per un confronto numerico efficiente.
     * 
     * @param matches Lista di partite da ordinare (non viene modificata)
     * @return Nuova lista ordinata per livello crescente
     */
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(match -> match.getRequiredLevel().ordinal()))
                .toList();
    }
    
    /**
     * Restituisce il nome user-friendly di questa strategia.
     * 
     * <p>Questo nome viene mostrato nella UI per permettere all'utente
     * di scegliere il criterio di ordinamento preferito.
     * 
     * @return "Livello" - nome visualizzato all'utente
     */
    @Override
    public String getStrategyName() {
        return "Livello";
    }
}
