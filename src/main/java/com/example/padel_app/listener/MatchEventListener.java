package com.example.padel_app.listener;

import com.example.padel_app.event.MatchConfirmedEvent;
import com.example.padel_app.event.MatchFinishedEvent;
import com.example.padel_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * OBSERVER PATTERN - Listener (Observer) che reagisce agli eventi delle partite.
 * 
 * <h2>Ruolo nell'Observer Pattern</h2>
 * Questa classe è l'<strong>Observer</strong> che:
 * <ul>
 *   <li>Si registra automaticamente per ricevere eventi (tramite @EventListener)</li>
 *   <li>Reagisce agli eventi pubblicati dal Subject (MatchService)</li>
 *   <li>Esegue azioni specifiche quando avviene un cambiamento di stato</li>
 *   <li>Non conosce chi ha pubblicato l'evento (disaccoppiamento)</li>
 * </ul>
 * 
 * <h2>Come funziona @EventListener in Spring?</h2>
 * L'annotazione @EventListener dice a Spring:
 * <pre>
 * "Quando viene pubblicato un evento di tipo MatchConfirmedEvent,
 *  chiama automaticamente questo metodo passandogli l'evento"
 * </pre>
 * 
 * Spring usa il <strong>tipo del parametro</strong> per capire quale evento ascoltare:
 * <ul>
 *   <li>handleMatchConfirmed(MatchConfirmedEvent event) → ascolta solo MatchConfirmedEvent</li>
 *   <li>handleMatchFinished(MatchFinishedEvent event) → ascolta solo MatchFinishedEvent</li>
 * </ul>
 * 
 * <h2>Registrazione automatica del Listener</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────┐
 * │ Spring Application Startup                      │
 * ├─────────────────────────────────────────────────┤
 * │ 1. Spring trova @Component sul listener         │
 * │ 2. Crea istanza di MatchEventListener           │
 * │ 3. Scansiona metodi con @EventListener          │
 * │ 4. Registra listener per ogni tipo di evento    │
 * │                                                  │
 * │ Registry interno di Spring:                     │
 * │  MatchConfirmedEvent → handleMatchConfirmed()   │
 * │  MatchFinishedEvent  → handleMatchFinished()    │
 * └─────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Flusso completo Publisher → Event → Listener:</h2>
 * <pre>
 * ┌──────────────────┐
 * │  MatchService    │ (Publisher)
 * │  confirmMatch()  │
 * └────────┬─────────┘
 *          │
 *          │ publishEvent(new MatchConfirmedEvent(...))
 *          ▼
 * ┌──────────────────────────┐
 * │ ApplicationEventPublisher │ (Spring Framework)
 * └────────┬─────────────────┘
 *          │
 *          │ Cerca tutti i listener per MatchConfirmedEvent
 *          ▼
 * ┌────────────────────────────┐
 * │  MatchEventListener        │ (Observer - questo file)
 * │  @EventListener            │
 * │  handleMatchConfirmed(...) │◄─── Metodo chiamato automaticamente
 * └────────┬───────────────────┘
 *          │
 *          │ Delega azione al Singleton
 *          ▼
 * ┌──────────────────────────┐
 * │ NotificationService      │ (Singleton)
 * │ sendNotification(...)    │
 * └──────────────────────────┘
 * </pre>
 * 
 * <h2>Collegamento con NotificationService (Singleton)</h2>
 * Il listener usa il {@link NotificationService} che è un <strong>Singleton</strong>:
 * <ul>
 *   <li>@Service in Spring crea una sola istanza dell'applicazione</li>
 *   <li>Tutti i listener condividono la stessa istanza</li>
 *   <li>Le notifiche sono centralizzate in un unico punto</li>
 *   <li>Lo stato (lista notifiche) è condiviso tra tutti gli eventi</li>
 * </ul>
 * 
 * <h2>Esempio pratico - Timeline di un evento:</h2>
 * <pre>
 * T=0ms   : User clicca "Conferma partita" nel browser
 * T=10ms  : WebController riceve richiesta HTTP POST
 * T=15ms  : WebController chiama MatchService.confirmMatch(123)
 * T=20ms  : MatchService aggiorna DB: UPDATE matches SET status='CONFIRMED'
 * T=25ms  : MatchService pubblica: publishEvent(new MatchConfirmedEvent(this, match))
 * T=26ms  : MatchService ritorna il controllo al controller
 * T=30ms  : Spring notifica MatchEventListener.handleMatchConfirmed()
 * T=35ms  : MatchEventListener chiama notificationService.send(...)
 * T=40ms  : NotificationService salva notifica in memoria e logga
 * T=50ms  : WebController restituisce risposta al browser
 * T=100ms : User vede la pagina aggiornata
 * </pre>
 * 
 * <h2>Vantaggi di questo approccio:</h2>
 * <ul>
 *   <li><strong>Disaccoppiamento:</strong> MatchService non sa dell'esistenza di questo listener</li>
 *   <li><strong>Testabilità:</strong> Posso testare MatchService senza questo listener</li>
 *   <li><strong>Estendibilità:</strong> Posso aggiungere EmailListener, StatisticsListener senza modificare MatchService</li>
 *   <li><strong>Single Responsibility:</strong> MatchService gestisce partite, questo listener gestisce notifiche</li>
 *   <li><strong>Riusabilità:</strong> Stesso evento può attivare più listener diversi</li>
 * </ul>
 * 
 * <h2>Possibili estensioni (seguono l'Open/Closed Principle):</h2>
 * <pre>
 * // Aggiungo nuovo listener SENZA modificare codice esistente
 * 
 * &#64;Component
 * public class EmailEventListener {
 *     &#64;EventListener
 *     public void handleMatchConfirmed(MatchConfirmedEvent event) {
 *         // Invia email di conferma ai partecipanti
 *     }
 * }
 * 
 * &#64;Component
 * public class StatisticsEventListener {
 *     &#64;EventListener
 *     public void handleMatchFinished(MatchFinishedEvent event) {
 *         // Aggiorna statistiche giocatori
 *     }
 * }
 * </pre>
 * 
 * @see MatchConfirmedEvent Evento quando una partita viene confermata
 * @see MatchFinishedEvent Evento quando una partita termina
 * @see NotificationService Singleton che gestisce le notifiche
 * @see org.springframework.context.event.EventListener Annotazione Spring per listener
 * @author Padel App Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventListener {
    
    /**
     * Singleton NotificationService iniettato tramite Constructor Injection.
     * Spring garantisce che tutti i listener ricevano la stessa istanza.
     */
    private final NotificationService notificationService;
    
    /**
     * Gestisce l'evento di conferma partita.
     * 
     * <p>Questo metodo viene chiamato automaticamente da Spring quando:
     * <ul>
     *   <li>Un componente pubblica un MatchConfirmedEvent</li>
     *   <li>Il tipo del parametro corrisponde esattamente</li>
     * </ul>
     * 
     * <p><strong>Nota:</strong> Il metodo può essere sincrono (default) o asincrono (@Async).
     * In modalità sincrona, il publisher aspetta che tutti i listener finiscano prima di continuare.
     * 
     * @param event L'evento contenente i dati della partita confermata
     */
    @EventListener
    public void handleMatchConfirmed(MatchConfirmedEvent event) {
        var match = event.getMatch();
        log.info("🎯 Observer - Match Confirmed: ID={}, Location={}", 
                 match.getId(), match.getLocation());
        
        // Invia notifica tramite Singleton NotificationService
        notificationService.sendMatchConfirmedNotification(
            match.getLocation(), 
            match.getActiveRegistrationsCount()
        );
        
        // Altri listener potrebbero fare altre azioni (es. aggiornare statistiche)
        log.info("✅ Match {} confermata - Notifiche inviate", match.getId());
    }
    
    /**
     * Gestisce l'evento di termine partita.
     * 
     * <p>Questo listener si attiva quando una partita termina e:
     * <ul>
     *   <li>Invia notifiche ai giocatori</li>
     *   <li>Attiva la richiesta di feedback</li>
     *   <li>Registra log per auditing</li>
     * </ul>
     * 
     * <p><strong>Separazione delle responsabilità:</strong>
     * Questo metodo si occupa solo di orchestrare le notifiche, mentre:
     * <ul>
     *   <li>MatchService gestisce la logica di business</li>
     *   <li>NotificationService gestisce l'invio effettivo</li>
     *   <li>Altri listener potrebbero gestire statistiche, email, etc.</li>
     * </ul>
     * 
     * @param event L'evento contenente i dati della partita terminata
     */
    @EventListener 
    public void handleMatchFinished(MatchFinishedEvent event) {
        var match = event.getMatch();
        log.info("🏁 Observer - Match Finished: ID={}, Location={}", 
                 match.getId(), match.getLocation());
        
        // Invia notifica tramite Singleton NotificationService
        notificationService.sendMatchFinishedNotification(
            match.getLocation(),
            match.getActiveRegistrationsCount()
        );
        
        // Trigger per richiedere feedback ai giocatori
        log.info("📝 Match {} terminata - Richiesta feedback attivata", match.getId());
    }
}
