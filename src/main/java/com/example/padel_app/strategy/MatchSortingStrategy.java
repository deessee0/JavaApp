package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;

import java.util.List;

/**
 * STRATEGY PATTERN - Interfaccia contratto per le strategie di ordinamento delle partite.
 * 
 * <h2>Cos'è lo Strategy Pattern?</h2>
 * Lo Strategy Pattern è un design pattern comportamentale che:
 * <ul>
 *   <li>Definisce una famiglia di algoritmi (strategie di ordinamento)</li>
 *   <li>Incapsula ogni algoritmo in una classe separata</li>
 *   <li>Rende gli algoritmi intercambiabili a runtime</li>
 *   <li>Permette al client di scegliere l'algoritmo senza conoscerne i dettagli</li>
 * </ul>
 * 
 * <h2>Problema risolto:</h2>
 * Senza Strategy Pattern, il codice sarebbe così:
 * <pre>
 * // ❌ APPROCCIO MONOLITICO (da evitare):
 * public List&lt;Match&gt; sortMatches(List&lt;Match&gt; matches, String sortType) {
 *     if (sortType.equals("data")) {
 *         return matches.stream()
 *             .sorted(Comparator.comparing(Match::getDateTime))
 *             .toList();
 *     } else if (sortType.equals("popolarita")) {
 *         return matches.stream()
 *             .sorted(Comparator.comparing(Match::getActiveRegistrationsCount).reversed())
 *             .toList();
 *     } else if (sortType.equals("livello")) {
 *         return matches.stream()
 *             .sorted(Comparator.comparing(m -> m.getRequiredLevel().ordinal()))
 *             .toList();
 *     }
 *     return matches;
 * }
 * 
 * // Problemi:
 * // 1. Viola Open/Closed Principle (modifico il codice per aggiungere strategie)
 * // 2. Difficile da testare (devo testare tutti i rami if-else insieme)
 * // 3. Non riusabile (logica di ordinamento legata a questo metodo)
 * </pre>
 * 
 * <h2>Soluzione con Strategy Pattern:</h2>
 * <pre>
 * // ✅ APPROCCIO STRATEGY (questo progetto):
 * 
 * 1. Definisco interfaccia comune (MatchSortingStrategy)
 * 2. Implemento strategie concrete (DateSortingStrategy, PopularitySortingStrategy, etc.)
 * 3. Il client sceglie la strategia a runtime
 * 
 * // Utilizzo:
 * Map&lt;String, MatchSortingStrategy&gt; strategies = ...; // Iniettate da Spring
 * MatchSortingStrategy strategy = strategies.get("dateSorting");
 * List&lt;Match&gt; sorted = strategy.sort(matches);
 * </pre>
 * 
 * <h2>Struttura del Pattern:</h2>
 * <pre>
 * ┌────────────────────────────┐
 * │  MatchSortingStrategy      │◄───────────┐
 * │  (Interface)               │            │
 * ├────────────────────────────┤            │
 * │ + sort(matches): List      │            │ implements
 * │ + getStrategyName(): String│            │
 * └────────────────────────────┘            │
 *              △                            │
 *              │ implements                 │
 *              │                            │
 *      ┌───────┴────────┬──────────────────┴─────────┐
 *      │                │                            │
 * ┌────────────┐  ┌────────────────┐  ┌──────────────────┐
 * │ DateSorting│  │ PopularitySorting│  │ LevelSorting     │
 * │ Strategy   │  │ Strategy         │  │ Strategy         │
 * └────────────┘  └──────────────────┘  └──────────────────┘
 *   Ordina per      Ordina per            Ordina per
 *   data            partecipanti          livello richiesto
 * </pre>
 * 
 * <h2>Perché un'interfaccia invece di classe astratta?</h2>
 * <ul>
 *   <li>Le strategie non condividono logica comune (ognuna ordina in modo diverso)</li>
 *   <li>Interfaccia = contratto puro, massima flessibilità</li>
 *   <li>Permette implementazioni completamente indipendenti</li>
 *   <li>Più semplice da mockare nei test</li>
 * </ul>
 * 
 * <h2>Ruolo dell'interfaccia (Strategy):</h2>
 * Questa interfaccia definisce il <strong>contratto</strong> che tutte le strategie concrete devono rispettare:
 * <ul>
 *   <li><code>sort(matches)</code> - Logica di ordinamento specifica</li>
 *   <li><code>getStrategyName()</code> - Nome user-friendly per la UI</li>
 * </ul>
 * 
 * <h2>Come Spring auto-inietta le strategie in una Map:</h2>
 * <pre>
 * // Nel Service:
 * &#64;Service
 * public class MatchService {
 *     
 *     // Spring trova tutti i @Component che implementano MatchSortingStrategy
 *     // e li inietta in una Map dove la chiave è il nome del bean
 *     private final Map&lt;String, MatchSortingStrategy&gt; sortingStrategies;
 *     
 *     public MatchService(Map&lt;String, MatchSortingStrategy&gt; strategies) {
 *         this.sortingStrategies = strategies;
 *         // Contenuto automatico della map:
 *         // {
 *         //   "dateSorting": DateSortingStrategy instance,
 *         //   "popularitySorting": PopularitySortingStrategy instance,
 *         //   "levelSorting": LevelSortingStrategy instance
 *         // }
 *     }
 *     
 *     public List&lt;Match&gt; getSortedMatches(String sortType) {
 *         MatchSortingStrategy strategy = sortingStrategies.get(sortType);
 *         return strategy.sort(getAllMatches());
 *     }
 * }
 * </pre>
 * 
 * <h2>Benefici dello Strategy Pattern:</h2>
 * <ul>
 *   <li><strong>Open/Closed Principle:</strong> Aperto all'estensione (nuove strategie), chiuso alla modifica</li>
 *   <li><strong>Single Responsibility:</strong> Ogni strategia ha una sola responsabilità</li>
 *   <li><strong>Testabilità:</strong> Testo ogni strategia indipendentemente</li>
 *   <li><strong>Riusabilità:</strong> Posso usare le strategie in contesti diversi</li>
 *   <li><strong>Runtime flexibility:</strong> Cambio algoritmo a runtime senza ricompilare</li>
 * </ul>
 * 
 * <h2>Esempio di aggiunta di nuova strategia (no modifica codice esistente):</h2>
 * <pre>
 * // Voglio ordinare per distanza geografica
 * &#64;Component("distanceSorting")
 * public class DistanceSortingStrategy implements MatchSortingStrategy {
 *     &#64;Override
 *     public List&lt;Match&gt; sort(List&lt;Match&gt; matches) {
 *         // Calcola distanza e ordina
 *     }
 *     
 *     &#64;Override
 *     public String getStrategyName() {
 *         return "Distanza";
 *     }
 * }
 * 
 * // Spring lo inietta automaticamente nella Map!
 * // Non devo modificare MatchService o altre strategie
 * </pre>
 * 
 * <h2>Quando usare Strategy Pattern:</h2>
 * <ul>
 *   <li>Hai multipli algoritmi per la stessa operazione</li>
 *   <li>Vuoi scegliere l'algoritmo a runtime</li>
 *   <li>Gli algoritmi possono cambiare indipendentemente</li>
 *   <li>Vuoi evitare lunghi blocchi if-else o switch</li>
 * </ul>
 * 
 * @see DateSortingStrategy Strategia per ordinamento per data
 * @see PopularitySortingStrategy Strategia per ordinamento per popolarità
 * @see LevelSortingStrategy Strategia per ordinamento per livello
 * @author Padel App Team
 */
public interface MatchSortingStrategy {
    
    /**
     * Ordina la lista di partite secondo l'algoritmo specifico della strategia.
     * 
     * <p>Ogni implementazione concrete definirà la propria logica:
     * <ul>
     *   <li>DateSortingStrategy: ordina per data crescente</li>
     *   <li>PopularitySortingStrategy: ordina per numero partecipanti decrescente</li>
     *   <li>LevelSortingStrategy: ordina per livello richiesto crescente</li>
     * </ul>
     * 
     * @param matches Lista di partite da ordinare (non modificata)
     * @return Nuova lista ordinata secondo la strategia
     */
    List<Match> sort(List<Match> matches);
    
    /**
     * Restituisce il nome user-friendly della strategia per la UI.
     * 
     * <p>Esempi: "Data", "Popolarità", "Livello"
     * 
     * @return Nome della strategia visualizzabile all'utente
     */
    String getStrategyName();
}
