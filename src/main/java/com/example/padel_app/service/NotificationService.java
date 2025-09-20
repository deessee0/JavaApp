package com.example.padel_app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton NotificationService - Gestisce tutte le notifiche dell'app
 * Implementa il pattern Singleton utilizzando Spring @Service (singleton scope)
 */
@Service
@Slf4j
public class NotificationService {
    
    private final List<String> notifications = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public void sendMatchConfirmedNotification(String matchLocation, int playersCount) {
        String message = String.format("🎉 Partita CONFERMATA! Location: %s - Giocatori: %d/4 - %s", 
                                       matchLocation, playersCount, LocalDateTime.now().format(formatter));
        
        notifications.add(message);
        log.info("📧 Notifica Match Confermata: {}", message);
        
        // Simula invio notifiche ai giocatori
        log.info("📱 Invio notifiche push a {} giocatori", playersCount);
    }
    
    public void sendMatchFinishedNotification(String matchLocation, int playersCount) {
        String message = String.format("⚽ Partita TERMINATA! Location: %s - Lascia il tuo feedback! - %s", 
                                       matchLocation, LocalDateTime.now().format(formatter));
        
        notifications.add(message);
        log.info("📧 Notifica Match Terminata: {}", message);
        
        // Simula invio notifiche per feedback
        log.info("📝 Richiesta feedback inviata a {} giocatori", playersCount);
    }
    
    public void sendGeneralNotification(String message) {
        String timestampedMessage = String.format("%s - %s", message, LocalDateTime.now().format(formatter));
        notifications.add(timestampedMessage);
        log.info("📢 Notifica Generale: {}", timestampedMessage);
    }
    
    public List<String> getAllNotifications() {
        return new ArrayList<>(notifications);
    }
    
    public List<String> getLatestNotifications(int count) {
        int size = notifications.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(notifications.subList(fromIndex, size));
    }
    
    public void clearNotifications() {
        notifications.clear();
        log.info("🗑️ Notifiche cancellate");
    }
    
    public int getNotificationCount() {
        return notifications.size();
    }
}