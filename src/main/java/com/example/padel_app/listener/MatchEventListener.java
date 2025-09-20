package com.example.padel_app.listener;

import com.example.padel_app.event.MatchConfirmedEvent;
import com.example.padel_app.event.MatchFinishedEvent;
import com.example.padel_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern Implementation - Event Listeners per gestire eventi Match
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventListener {
    
    private final NotificationService notificationService;
    
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