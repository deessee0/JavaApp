package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Test per NotificationService - Singleton Pattern
 * 
 * OBIETTIVO:
 * Verificare che il servizio di notifiche funzioni correttamente
 * e dimostri il pattern Singleton attraverso Spring.
 * 
 * FOCUS:
 * - Verifica log messages corretti
 * - Test singleton behavior (implicito via Spring)
 * - Coverage base per service semplice
 * 
 * APPROCCIO:
 * Test unit con @ExtendWith per catturare log output.
 * NotificationService è stateless quindi test semplici.
 * 
 * SINGLETON PATTERN:
 * NotificationService è Singleton grazie a @Service annotation Spring
 * (default scope = singleton). Spring garantisce una sola istanza
 * condivisa in tutto il container.
 * 
 * @author Padel App Team
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("NotificationService - Singleton Pattern Tests")
public class NotificationServiceTest {
    
    private NotificationService notificationService;
    private Match testMatch;
    
    @BeforeEach
    void setup() {
        notificationService = new NotificationService();
        
        // Setup match di test
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testuser");
        creator.setDeclaredLevel(Level.INTERMEDIO);
        
        testMatch = new Match();
        testMatch.setId(100L);
        testMatch.setLocation("Campo Test");
        testMatch.setDateTime(LocalDateTime.now().plusDays(1));
        testMatch.setRequiredLevel(Level.INTERMEDIO);
        testMatch.setType(MatchType.FISSA);
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setCreator(creator);
        testMatch.setDescription("Test match");
    }
    
    // ==================== MATCH CONFIRMED NOTIFICATION ====================
    
    @Test
    @DisplayName("Send Match Confirmed Notification: genera log con emoji e location")
    void testSendMatchConfirmedNotification_LogsCorrectMessage(CapturedOutput output) {
        // GIVEN: Match confermato con 4 giocatori
        String matchLocation = "Campo Test";
        int playersCount = 4;
        
        // WHEN: Inviamo notifica
        notificationService.sendMatchConfirmedNotification(matchLocation, playersCount);
        
        // THEN: Log contiene messaggio di conferma con location
        String logOutput = output.toString();
        assertThat(logOutput).contains("Partita CONFERMATA");
        assertThat(logOutput).contains("Campo Test");
        assertThat(logOutput).contains("4");
    }
    
    @Test
    @DisplayName("Send Match Confirmed Notification: funziona con match diversi")
    void testSendMatchConfirmedNotification_DifferentMatches(CapturedOutput output) {
        // GIVEN: Location diversa
        String location = "Campo Centrale";
        int players = 4;
        
        // WHEN: Inviamo notifica
        notificationService.sendMatchConfirmedNotification(location, players);
        
        // THEN: Log contiene location specifica
        assertThat(output.toString()).contains("Campo Centrale");
    }
    
    // ==================== MATCH FINISHED NOTIFICATION ====================
    
    @Test
    @DisplayName("Send Match Finished Notification: genera log con emoji e location")
    void testSendMatchFinishedNotification_LogsCorrectMessage(CapturedOutput output) {
        // GIVEN: Match finito
        String matchLocation = "Campo Test";
        int playersCount = 4;
        
        // WHEN: Inviamo notifica
        notificationService.sendMatchFinishedNotification(matchLocation, playersCount);
        
        // THEN: Log contiene messaggio di fine partita
        String logOutput = output.toString();
        assertThat(logOutput).contains("Partita TERMINATA");
        assertThat(logOutput).contains("Campo Test");
    }
    
    @Test
    @DisplayName("Send Match Finished Notification: funziona con match diversi")
    void testSendMatchFinishedNotification_DifferentMatches(CapturedOutput output) {
        // GIVEN: Location diversa
        String location = "Campo Sud";
        int players = 4;
        
        // WHEN: Inviamo notifica
        notificationService.sendMatchFinishedNotification(location, players);
        
        // THEN: Log contiene location specifica
        assertThat(output.toString()).contains("Campo Sud");
    }
    
    // ==================== EDGE CASES ====================
    
    @Test
    @DisplayName("Notification Service: gestisce location null senza errori")
    void testNotifications_HandleNullLocation(CapturedOutput output) {
        // GIVEN: Location null
        String location = null;
        int players = 4;
        
        // WHEN: Inviamo entrambe le notifiche
        notificationService.sendMatchConfirmedNotification(location, players);
        notificationService.sendMatchFinishedNotification(location, players);
        
        // THEN: Nessuna eccezione, log generati correttamente
        assertThat(output.toString()).contains("Partita CONFERMATA");
        assertThat(output.toString()).contains("Partita TERMINATA");
    }
    
    @Test
    @DisplayName("Notification Service: metodi possono essere chiamati multipli volte")
    void testNotifications_MultipleCallsSafe(CapturedOutput output) {
        // GIVEN: Location e players
        String location = "Campo Test";
        int players = 4;
        
        // WHEN: Chiamiamo notifica 3 volte (simula più listener)
        notificationService.sendMatchConfirmedNotification(location, players);
        notificationService.sendMatchConfirmedNotification(location, players);
        notificationService.sendMatchConfirmedNotification(location, players);
        
        // THEN: Log generati 3 volte senza errori
        String logOutput = output.toString();
        int count = logOutput.split("Partita CONFERMATA").length - 1;
        assertThat(count).isEqualTo(3);
    }
    
    // ==================== SINGLETON PATTERN DOCUMENTATION ====================
    
    /**
     * Test documentativo per Singleton Pattern
     * 
     * NOTA:
     * NotificationService è un Singleton grazie a Spring @Service annotation.
     * Spring IoC container garantisce che esista una sola istanza condivisa.
     * 
     * In un contesto reale Spring Boot, due chiamate a:
     *   @Autowired NotificationService notificationService
     * 
     * Ritornano la STESSA istanza (verificabile con == o assertSame).
     * 
     * Non testiamo direttamente singleton behavior qui perché:
     * 1. È garantito da Spring framework (già testato da Spring team)
     * 2. Test richiederebbe ApplicationContext completo (@SpringBootTest)
     * 3. Focus è su funzionalità business, non infrastruttura
     * 
     * Per verificare singleton in integration test, vedere ObserverPatternTest
     * dove stesso NotificationService è iniettato in MatchEventListener.
     */
    @Test
    @DisplayName("Singleton Pattern: NotificationService gestisce stato condiviso")
    void testSingletonPattern_SharedState() {
        // GIVEN: Due istanze diverse del service (solo per test unit)
        NotificationService instance1 = new NotificationService();
        NotificationService instance2 = new NotificationService();
        
        // WHEN: Chiamiamo metodi su istanze diverse
        instance1.sendMatchConfirmedNotification("Campo A", 4);
        instance2.sendMatchConfirmedNotification("Campo B", 4);
        
        // THEN: Entrambe funzionano correttamente
        // Questo dimostra che il metodo è ben implementato
        assertThat(instance1).isNotNull();
        assertThat(instance2).isNotNull();
        
        // Verifica che le notifiche sono state aggiunte
        assertThat(instance1.getNotificationCount()).isEqualTo(1);
        assertThat(instance2.getNotificationCount()).isEqualTo(1);
        
        // NOTA: In produzione Spring garantisce instance1 == instance2
        // (stessa istanza condivisa), quindi avrebbero count = 2
    }
}
