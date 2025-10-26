package com.example.padel_app.event;

import com.example.padel_app.model.Match;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * OBSERVER PATTERN - Custom Event per notificare la conferma di una partita.
 * 
 * <h2>Cos'è l'Observer Pattern?</h2>
 * L'Observer Pattern è un design pattern comportamentale che definisce una dipendenza uno-a-molti
 * tra oggetti, in modo che quando un oggetto (Subject/Publisher) cambia stato, tutti i suoi
 * dipendenti (Observers/Listeners) vengano notificati e aggiornati automaticamente.
 * 
 * <h2>Perché estendere ApplicationEvent?</h2>
 * Spring Framework fornisce un sistema di eventi integrato basato su ApplicationEvent.
 * Estendendo ApplicationEvent otteniamo:
 * <ul>
 *   <li>Integrazione automatica con il contesto Spring</li>
 *   <li>Gestione asincrona degli eventi (se configurata)</li>
 *   <li>Disaccoppiamento tra Publisher e Listener</li>
 *   <li>Possibilità di avere multipli listener per lo stesso evento</li>
 * </ul>
 * 
 * <h2>Perché creare un evento custom?</h2>
 * Un evento custom permette di:
 * <ul>
 *   <li>Trasportare dati specifici del dominio (in questo caso, Match)</li>
 *   <li>Identificare univocamente il tipo di evento</li>
 *   <li>Applicare type-safety: solo listener specifici riceveranno questo evento</li>
 *   <li>Separare le responsabilità: l'evento rappresenta "cosa è successo", non "cosa fare"</li>
 * </ul>
 * 
 * <h2>Diagramma del flusso Observer Pattern in Spring:</h2>
 * <pre>
 * ┌─────────────────┐
 * │  MatchService   │  (Publisher/Subject)
 * │   confirmMatch()│─────┐
 * └─────────────────┘     │
 *                         │ 1. Pubblica evento
 *                         ▼
 *              ┌──────────────────────┐
 *              │ ApplicationContext   │ (Event Manager di Spring)
 *              │  publishEvent()      │
 *              └──────────────────────┘
 *                         │
 *                         │ 2. Notifica tutti i listener registrati
 *                         ▼
 *              ┌──────────────────────┐
 *              │ MatchEventListener   │ (Observer/Listener)
 *              │ @EventListener       │
 *              │ handleMatchConfirmed()│
 *              └──────────────────────┘
 *                         │
 *                         │ 3. Esegue azione
 *                         ▼
 *              ┌──────────────────────┐
 *              │ NotificationService  │
 *              │ sendNotification()   │
 *              └──────────────────────┘
 * </pre>
 * 
 * <h2>Esempio d'uso:</h2>
 * <pre>
 * // Nel Publisher (MatchService):
 * public void confirmMatch(Long matchId) {
 *     Match match = findMatch(matchId);
 *     match.setStatus(MatchStatus.CONFIRMED);
 *     
 *     // Pubblica evento - tutti i listener verranno notificati
 *     applicationEventPublisher.publishEvent(
 *         new MatchConfirmedEvent(this, match)
 *     );
 * }
 * 
 * // Nel Listener (MatchEventListener):
 * &#64;EventListener
 * public void handleMatchConfirmed(MatchConfirmedEvent event) {
 *     Match match = event.getMatch();
 *     // Reagisce all'evento inviando notifiche
 *     notificationService.sendMatchConfirmedNotification(match);
 * }
 * </pre>
 * 
 * <h2>Benefici del Pattern:</h2>
 * <ul>
 *   <li><strong>Disaccoppiamento:</strong> MatchService non conosce MatchEventListener</li>
 *   <li><strong>Open/Closed Principle:</strong> Posso aggiungere nuovi listener senza modificare il publisher</li>
 *   <li><strong>Single Responsibility:</strong> MatchService si occupa solo di confermare, il listener gestisce le notifiche</li>
 *   <li><strong>Scalabilità:</strong> Multipli listener possono reagire allo stesso evento (es. statistiche, email, log)</li>
 * </ul>
 * 
 * @see MatchEventListener Listener che gestisce questo evento
 * @see org.springframework.context.ApplicationEvent Classe base per eventi Spring
 * @see org.springframework.context.ApplicationEventPublisher Publisher di eventi
 * @author Padel App Team
 */
@Getter
public class MatchConfirmedEvent extends ApplicationEvent {
    
    /**
     * Match che è stata confermata.
     * Questo dato viene trasportato dall'evento ai listener.
     */
    private final Match match;
    
    /**
     * Costruttore dell'evento.
     * 
     * @param source L'oggetto che ha pubblicato l'evento (tipicamente MatchService)
     * @param match La partita che è stata confermata
     */
    public MatchConfirmedEvent(Object source, Match match) {
        super(source);
        this.match = match;
    }
}
