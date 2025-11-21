package com.example.padel_app.service;

import com.example.padel_app.event.MatchConfirmedEvent;
import com.example.padel_app.event.MatchFinishedEvent;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.RegistrationStatus;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.RegistrationRepository;
import com.example.padel_app.repository.UserRepository;
import com.example.padel_app.strategy.MatchSortingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test Puro per MatchService.
 * 
 * OBIETTIVO:
 * Soddisfare il requisito "Unit tests are mandatory".
 * Questo test usa Mockito per isolare la classe da testare (MatchService)
 * da tutte le sue dipendenze (Repository, EventPublisher, ecc.).
 * 
 * DIFFERENZE CON INTEGRATION TEST:
 * - Nessun @SpringBootTest (non carica il contesto Spring)
 * - Nessun database reale (H2)
 * - Esecuzione istantanea
 * - Verifica solo la logica della classe, non l'integrazione
 */
@ExtendWith(MockitoExtension.class)
class MatchServiceUnitTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Map<String, MatchSortingStrategy> sortingStrategies;

    @InjectMocks
    private MatchService matchService;

    private Match testMatch;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setMatchesPlayed(0);

        testMatch = new Match();
        testMatch.setId(100L);
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setCreator(testUser);
    }

    @Test
    @DisplayName("saveMatch: deve chiamare il repository save")
    void testSaveMatch() {
        // GIVEN
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // WHEN
        Match result = matchService.saveMatch(testMatch);

        // THEN
        assertNotNull(result);
        verify(matchRepository, times(1)).save(testMatch);
    }

    @Test
    @DisplayName("checkAndConfirmMatch: deve confermare match e pubblicare evento se 4 giocatori")
    void testCheckAndConfirmMatch_With4Players() {
        // GIVEN
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(4);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Match result = matchService.checkAndConfirmMatch(testMatch);

        // THEN
        assertEquals(MatchStatus.CONFIRMED, result.getStatus());
        verify(matchRepository, times(1)).save(testMatch);
        verify(eventPublisher, times(1)).publishEvent(any(MatchConfirmedEvent.class));
    }

    @Test
    @DisplayName("checkAndConfirmMatch: NON deve confermare se meno di 4 giocatori")
    void testCheckAndConfirmMatch_WithLessThan4Players() {
        // GIVEN
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(3);

        // WHEN
        Match result = matchService.checkAndConfirmMatch(testMatch);

        // THEN
        assertEquals(MatchStatus.WAITING, result.getStatus());
        verify(matchRepository, never()).save(any(Match.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("finishMatch: deve aggiornare stato, contatori giocatori e pubblicare evento")
    void testFinishMatch_Success() {
        // GIVEN
        testMatch.setStatus(MatchStatus.CONFIRMED);
        
        // Mock finding the match
        when(matchRepository.findById(100L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock active registrations (players)
        User player2 = new User(); player2.setId(2L); player2.setMatchesPlayed(5);
        Registration reg = new Registration();
        reg.setUser(player2);
        reg.setStatus(RegistrationStatus.JOINED);
        
        when(registrationRepository.findByMatchAndStatus(testMatch, RegistrationStatus.JOINED))
            .thenReturn(Collections.singletonList(reg));

        // WHEN
        Match result = matchService.finishMatch(100L);

        // THEN
        assertEquals(MatchStatus.FINISHED, result.getStatus());
        
        // Verify creator stats updated
        assertEquals(1, testUser.getMatchesPlayed());
        // Verify player stats updated
        assertEquals(6, player2.getMatchesPlayed());
        
        verify(userRepository, times(1)).saveAll(anyList());
        verify(eventPublisher, times(1)).publishEvent(any(MatchFinishedEvent.class));
    }

    @Test
    @DisplayName("getMatchesOrderedBy: deve delegare alla strategia corretta")
    void testGetMatchesOrderedBy_DelegatesToStrategy() {
        // GIVEN
        String strategyName = "date";
        String strategyKey = "dateSorting";
        MatchSortingStrategy mockStrategy = mock(MatchSortingStrategy.class);
        
        when(sortingStrategies.get(strategyKey)).thenReturn(mockStrategy);
        when(mockStrategy.getStrategyName()).thenReturn("Date Sorting");
        when(matchRepository.findAllWithCreator()).thenReturn(Collections.singletonList(testMatch));
        when(mockStrategy.sort(anyList())).thenReturn(Collections.singletonList(testMatch));

        // WHEN
        List<Match> result = matchService.getMatchesOrderedBy(strategyName);

        // THEN
        assertNotNull(result);
        verify(sortingStrategies, times(1)).get(strategyKey);
        verify(mockStrategy, times(1)).sort(anyList());
    }
}
