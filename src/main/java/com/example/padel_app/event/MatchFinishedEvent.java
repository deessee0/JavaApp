package com.example.padel_app.event;

import com.example.padel_app.model.Match;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * OBSERVER PATTERN - Custom Event per notificare il termine di una partita.
 * 
 * <h2>Ruolo nell'Observer Pattern</h2>
 * Questo evento rappresenta la notifica "la partita è terminata" che viene propagata
 * dal Publisher (MatchService) a tutti gli Observer (Listeners) interessati.
 * 
 * <h2>Differenza tra MatchConfirmedEvent e MatchFinishedEvent</h2>
 * Separare gli eventi in base allo stato permette di:
 * <ul>
 *   <li><strong>Listener specifici:</strong> Un listener può reagire solo al termine, non alla conferma</li>
 *   <li><strong>Azioni diverse:</strong> Conferma → notifica giocatori; Fine → richiesta feedback</li>
 *   <li><strong>Single Responsibility:</strong> Ogni evento rappresenta un cambio di stato distinto</li>
 *   <li><strong>Estendibilità:</strong> Posso aggiungere MatchCancelledEvent senza toccare gli altri</li>
 * </ul>
 * 
 * <h2>Perché usare ApplicationEvent invece di un semplice metodo?</h2>
 * <table border="1">
 *   <tr>
 *     <th>Approccio</th>
 *     <th>Chiamata diretta</th>
 *     <th>ApplicationEvent</th>
 *   </tr>
 *   <tr>
 *     <td>Accoppiamento</td>
 *     <td>FORTE - Service dipende da NotificationService</td>
 *     <td>DEBOLE - Service non conosce i listener</td>
 *   </tr>
 *   <tr>
 *     <td>Estensione</td>
 *     <td>Modifico MatchService per ogni nuova azione</td>
 *     <td>Aggiungo nuovo listener senza toccare MatchService</td>
 *   </tr>
 *   <tr>
 *     <td>Testing</td>
 *     <td>Mock di tutti i servizi coinvolti</td>
 *     <td>Testo solo il publisher o solo il listener</td>
 *   </tr>
 *   <tr>
 *     <td>Asincrono</td>
 *     <td>Complesso - serve gestione thread manuale</td>
 *     <td>Semplice - @Async sul listener</td>
 *   </tr>
 * </table>
 * 
 * <h2>Esempio concreto - Flusso completo:</h2>
 * <pre>
 * 1. Utente marca partita come terminata
 *    ↓
 * 2. WebController chiama MatchService.finishMatch(matchId)
 *    ↓
 * 3. MatchService:
 *    - Aggiorna stato: match.setStatus(FINISHED)
 *    - Pubblica: publishEvent(new MatchFinishedEvent(this, match))
 *    - Ritorna immediatamente (non aspetta i listener)
 *    ↓
 * 4. Spring ApplicationContext propaga evento a tutti i listener
 *    ↓
 * 5. MatchEventListener.handleMatchFinished():
 *    - Invia notifiche tramite NotificationService
 *    - Registra log
 *    - Attiva richiesta feedback
 *    ↓
 * 6. (Possibili altri listener):
 *    - StatisticsListener aggiorna statistiche giocatori
 *    - EmailListener invia email di recap
 *    - AuditListener registra evento per auditing
 * </pre>
 * 
 * <h2>Vantaggi rispetto a codice accoppiato:</h2>
 * <pre>
 * // ❌ APPROCCIO ACCOPPIATO (da evitare):
 * public void finishMatch(Long matchId) {
 *     Match match = findMatch(matchId);
 *     match.setStatus(FINISHED);
 *     
 *     // Accoppiamento diretto - difficile da testare ed estendere
 *     notificationService.send(...);
 *     statisticsService.update(...);
 *     emailService.send(...);
 *     auditService.log(...);
 * }
 * 
 * // ✅ APPROCCIO OBSERVER (questo progetto):
 * public void finishMatch(Long matchId) {
 *     Match match = findMatch(matchId);
 *     match.setStatus(FINISHED);
 *     
 *     // Disaccoppiato - listener aggiunti senza modificare questo codice
 *     publishEvent(new MatchFinishedEvent(this, match));
 * }
 * </pre>
 * 
 * <h2>Pattern correlati:</h2>
 * Questo evento viene gestito da:
 * <ul>
 *   <li>{@link MatchEventListener} - Observer che reagisce all'evento</li>
 *   <li>{@link com.example.padel_app.service.NotificationService} - Singleton per inviare notifiche</li>
 * </ul>
 * 
 * @see MatchEventListener Listener che gestisce questo evento
 * @see MatchConfirmedEvent Evento simile per la conferma partita
 * @see org.springframework.context.ApplicationEvent Classe base di Spring
 * @author Padel App Team
 */
@Getter
public class MatchFinishedEvent extends ApplicationEvent {
    
    /**
     * Match che è stata terminata.
     * I listener useranno questi dati per eseguire le azioni appropriate
     * (es. inviare notifiche, richiedere feedback, aggiornare statistiche).
     */
    private final Match match;
    
    /**
     * Costruttore dell'evento.
     * 
     * @param source L'oggetto che ha pubblicato l'evento (tipicamente MatchService)
     * @param match La partita che è stata terminata
     */
    public MatchFinishedEvent(Object source, Match match) {
        super(source);
        this.match = match;
    }
}
