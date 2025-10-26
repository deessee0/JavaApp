package com.example.padel_app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN - Service per la gestione centralizzata delle notifiche dell'applicazione.
 * 
 * <h2>Cos'è il Singleton Pattern?</h2>
 * Il Singleton è un design pattern creazionale che garantisce che una classe abbia
 * <strong>una sola istanza</strong> nell'intera applicazione e fornisce un punto di accesso globale a quella istanza.
 * 
 * <h2>Implementazione Singleton tramite Spring @Service</h2>
 * Spring implementa automaticamente il Singleton Pattern per tutti i bean annotati con @Service:
 * <pre>
 * ┌─────────────────────────────────────────────┐
 * │ Spring Container (Application Context)      │
 * ├─────────────────────────────────────────────┤
 * │                                             │
 * │  ┌────────────────────────────┐             │
 * │  │ NotificationService        │◄────────────┼─── Singleton instance
 * │  │ @Service                   │             │    (una sola istanza)
 * │  │ notifications: List        │             │
 * │  └────────────────────────────┘             │
 * │            △        △        △              │
 * │            │        │        │              │
 * │  ┌─────────┴──┐ ┌──┴─────┐ ┌┴──────────┐   │
 * │  │MatchService│ │Listener│ │Controller │   │
 * │  └────────────┘ └────────┘ └───────────┘   │
 * │   Tutti ricevono la STESSA istanza          │
 * └─────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>@Service e Singleton Scope in Spring</h2>
 * <ul>
 *   <li><strong>@Service</strong> è una specializzazione di @Component</li>
 *   <li>Lo <strong>scope default</strong> di tutti i bean Spring è <strong>singleton</strong></li>
 *   <li>Spring crea <strong>UNA SOLA istanza</strong> all'avvio dell'applicazione</li>
 *   <li>Tutte le classi che richiedono NotificationService ricevono la stessa istanza</li>
 *   <li>Lo <strong>stato</strong> (lista notifications) è condiviso tra tutti i componenti</li>
 * </ul>
 * 
 * <h2>Differenza tra Singleton manuale e Singleton Spring:</h2>
 * <table border="1">
 *   <tr>
 *     <th>Aspetto</th>
 *     <th>Singleton Classico (GOF)</th>
 *     <th>Singleton Spring (questo)</th>
 *   </tr>
 *   <tr>
 *     <td>Implementazione</td>
 *     <td>private static instance<br/>private constructor<br/>getInstance()</td>
 *     <td>@Service<br/>(Spring gestisce tutto)</td>
 *   </tr>
 *   <tr>
 *     <td>Creazione istanza</td>
 *     <td>Manuale (lazy o eager)</td>
 *     <td>Automatica (Spring container)</td>
 *   </tr>
 *   <tr>
 *     <td>Thread-safety</td>
 *     <td>Da gestire manualmente</td>
 *     <td>Garantita da Spring</td>
 *   </tr>
 *   <tr>
 *     <td>Testing</td>
 *     <td>Difficile (istanza globale)</td>
 *     <td>Facile (mock injection)</td>
 *   </tr>
 *   <tr>
 *     <td>Dependency Injection</td>
 *     <td>Non supportata</td>
 *     <td>Supportata nativamente</td>
 *   </tr>
 * </table>
 * 
 * <h2>Perché NotificationService è un Singleton?</h2>
 * Questo service è un perfetto candidato per Singleton perché:
 * <ul>
 *   <li><strong>Stato condiviso:</strong> La lista delle notifiche deve essere unica e condivisa</li>
 *   <li><strong>Risorsa centralizzata:</strong> Un solo punto di gestione per tutte le notifiche</li>
 *   <li><strong>Efficienza:</strong> Non serve creare istanze multiple dello stesso service</li>
 *   <li><strong>Coerenza:</strong> Tutti i componenti vedono le stesse notifiche</li>
 * </ul>
 * 
 * <h2>Flusso completo - Come viene usato il Singleton:</h2>
 * <pre>
 * 1. Spring Boot Startup
 *    ↓
 * 2. Spring crea UNA istanza di NotificationService
 *    NotificationService singleton = new NotificationService();
 *    ↓
 * 3. Spring inietta la STESSA istanza ovunque sia richiesta
 *    
 *    MatchEventListener listener = new MatchEventListener(singleton);
 *    MatchService service = new MatchService(..., singleton, ...);
 *    WebController controller = new WebController(...);
 *    ↓
 * 4. Durante il runtime, tutti usano la stessa istanza:
 *    
 *    T=10ms: listener.handleMatchConfirmed() 
 *            → singleton.sendMatchConfirmedNotification()
 *            → notifications.add("Partita confermata")
 *    
 *    T=50ms: controller.getNotifications()
 *            → singleton.getAllNotifications()
 *            → return notifications (stessa lista!)
 *    
 *    T=100ms: listener.handleMatchFinished()
 *             → singleton.sendMatchFinishedNotification()
 *             → notifications.add("Partita terminata")
 *    
 *    T=150ms: controller.getNotifications()
 *             → singleton.getAllNotifications()
 *             → return notifications (contiene entrambe!)
 * </pre>
 * 
 * <h2>Esempio pratico - Stato condiviso tra componenti:</h2>
 * <pre>
 * // Scenario: Due eventi consecutivi
 * 
 * // Evento 1: MatchEventListener usa il singleton
 * notificationService.sendMatchConfirmedNotification("Campo 1", 4);
 * // → notifications = ["🎉 Partita CONFERMATA! Campo 1 ..."]
 * 
 * // Evento 2: Stesso listener, STESSA istanza singleton
 * notificationService.sendMatchFinishedNotification("Campo 2", 4);
 * // → notifications = [
 * //     "🎉 Partita CONFERMATA! Campo 1 ...",
 * //     "⚽ Partita TERMINATA! Campo 2 ..."
 * //   ]
 * 
 * // WebController legge le notifiche, STESSA istanza singleton
 * List&lt;String&gt; all = notificationService.getAllNotifications();
 * // → all contiene ENTRAMBE le notifiche
 * // Questo funziona perché è la STESSA lista dello STESSO singleton
 * </pre>
 * 
 * <h2>Thread-Safety del Singleton Spring:</h2>
 * <ul>
 *   <li>Spring garantisce la creazione thread-safe del singleton</li>
 *   <li>ATTENZIONE: Lo stato interno (notifications list) NON è thread-safe per default</li>
 *   <li>Per applicazioni multi-thread reali, usare Collections.synchronizedList() o ConcurrentList</li>
 *   <li>In questa app (web sincrona) non è necessario perché ogni richiesta HTTP è gestita sequenzialmente</li>
 * </ul>
 * 
 * <h2>Come verificare che è un Singleton:</h2>
 * <pre>
 * &#64;Test
 * public void verifySingletonBehavior() {
 *     // Richiedi il bean due volte dal container
 *     NotificationService instance1 = context.getBean(NotificationService.class);
 *     NotificationService instance2 = context.getBean(NotificationService.class);
 *     
 *     // Stesso oggetto in memoria
 *     assertSame(instance1, instance2);
 *     
 *     // Stato condiviso
 *     instance1.sendGeneralNotification("Test");
 *     assertEquals(1, instance2.getNotificationCount()); // Vede la stessa lista
 * }
 * </pre>
 * 
 * <h2>Pattern correlati usati in questa app:</h2>
 * <ul>
 *   <li><strong>Singleton:</strong> NotificationService (questa classe)</li>
 *   <li><strong>Observer:</strong> MatchEventListener usa questo singleton per inviare notifiche</li>
 *   <li><strong>Strategy:</strong> Sorting strategies sono anche singleton Spring</li>
 *   <li><strong>Dependency Injection:</strong> Spring inietta il singleton ovunque necessario</li>
 * </ul>
 * 
 * <h2>Benefici del Singleton Pattern:</h2>
 * <ul>
 *   <li><strong>Stato condiviso:</strong> Tutte le notifiche in un unico posto</li>
 *   <li><strong>Efficienza memoria:</strong> Una sola istanza invece di molte</li>
 *   <li><strong>Punto di controllo centralizzato:</strong> Facile debuggare e monitorare</li>
 *   <li><strong>Coerenza:</strong> Tutti i componenti vedono lo stesso stato</li>
 * </ul>
 * 
 * <h2>Quando NON usare Singleton:</h2>
 * <ul>
 *   <li>Se ogni utente deve avere le sue notifiche separate → usare session scope</li>
 *   <li>Se lo stato deve essere isolato per richiesta → usare request scope</li>
 *   <li>Se serve creare nuove istanze ogni volta → usare prototype scope</li>
 * </ul>
 * 
 * @see MatchEventListener Observer che usa questo singleton per inviare notifiche
 * @see org.springframework.stereotype.Service Annotazione Spring per service layer
 * @author Padel App Team
 */
@Service
@Slf4j
public class NotificationService {
    
    /**
     * Lista delle notifiche condivisa da TUTTA l'applicazione.
     * 
     * <p>Essendo questo un Singleton, questa lista è una risorsa condivisa.
     * Tutti i componenti che usano NotificationService accedono alla stessa lista in memoria.
     * 
     * <p><strong>Nota Thread-Safety:</strong>
     * ArrayList non è thread-safe. Per applicazioni multi-thread concurrent,
     * considerare l'uso di Collections.synchronizedList() o CopyOnWriteArrayList.
     */
    private final List<String> notifications = new ArrayList<>();
    
    /**
     * Formatter per le date nelle notifiche.
     * Essendo final e immutabile, può essere condiviso in sicurezza.
     */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Invia notifica di conferma partita.
     * 
     * <p>Questo metodo viene chiamato dal {@link MatchEventListener} quando
     * viene pubblicato un {@link com.example.padel_app.event.MatchConfirmedEvent}.
     * 
     * <p><strong>Flusso Observer → Singleton:</strong>
     * <pre>
     * MatchService.confirmMatch()
     *   → publishEvent(MatchConfirmedEvent)
     *     → MatchEventListener.handleMatchConfirmed()
     *       → notificationService.sendMatchConfirmedNotification() ◄── Questo metodo
     * </pre>
     * 
     * @param matchLocation Luogo della partita confermata
     * @param playersCount Numero di giocatori iscritti
     */
    public void sendMatchConfirmedNotification(String matchLocation, int playersCount) {
        String message = String.format("🎉 Partita CONFERMATA! Location: %s - Giocatori: %d/4 - %s", 
                                       matchLocation, playersCount, LocalDateTime.now().format(formatter));
        
        notifications.add(message);
        log.info("📧 Notifica Match Confermata: {}", message);
        
        log.info("📱 Invio notifiche push a {} giocatori", playersCount);
    }
    
    /**
     * Invia notifica di termine partita.
     * 
     * <p>Questo metodo viene chiamato dal {@link MatchEventListener} quando
     * viene pubblicato un {@link com.example.padel_app.event.MatchFinishedEvent}.
     * 
     * @param matchLocation Luogo della partita terminata
     * @param playersCount Numero di giocatori che hanno partecipato
     */
    public void sendMatchFinishedNotification(String matchLocation, int playersCount) {
        String message = String.format("⚽ Partita TERMINATA! Location: %s - Lascia il tuo feedback! - %s", 
                                       matchLocation, LocalDateTime.now().format(formatter));
        
        notifications.add(message);
        log.info("📧 Notifica Match Terminata: {}", message);
        
        log.info("📝 Richiesta feedback inviata a {} giocatori", playersCount);
    }
    
    /**
     * Invia una notifica generica.
     * 
     * @param message Il messaggio della notifica
     */
    public void sendGeneralNotification(String message) {
        String timestampedMessage = String.format("%s - %s", message, LocalDateTime.now().format(formatter));
        notifications.add(timestampedMessage);
        log.info("📢 Notifica Generale: {}", timestampedMessage);
    }
    
    /**
     * Restituisce tutte le notifiche presenti.
     * 
     * <p><strong>Nota importante:</strong>
     * Restituisce una copia della lista per evitare modifiche esterne accidentali.
     * Questo è un buon pattern di sicurezza (defensive copy).
     * 
     * @return Nuova lista contenente tutte le notifiche
     */
    public List<String> getAllNotifications() {
        return new ArrayList<>(notifications);
    }
    
    /**
     * Restituisce le ultime N notifiche.
     * 
     * <p>Utile per mostrare solo le notifiche recenti nella UI.
     * 
     * @param count Numero di notifiche da restituire
     * @return Lista delle ultime 'count' notifiche
     */
    public List<String> getLatestNotifications(int count) {
        int size = notifications.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(notifications.subList(fromIndex, size));
    }
    
    /**
     * Cancella tutte le notifiche.
     * 
     * <p><strong>Attenzione:</strong>
     * Essendo un Singleton, questa operazione impatta tutta l'applicazione!
     */
    public void clearNotifications() {
        notifications.clear();
        log.info("🗑️ Notifiche cancellate");
    }
    
    /**
     * Restituisce il numero totale di notifiche.
     * 
     * @return Conteggio delle notifiche presenti
     */
    public int getNotificationCount() {
        return notifications.size();
    }
}
