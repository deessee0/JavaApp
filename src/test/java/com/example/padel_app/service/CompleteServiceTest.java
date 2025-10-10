package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import com.example.padel_app.repository.MatchRepository;
import com.example.padel_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CompleteServiceTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private User testUser4;
    private User testUser5;
    private Match testMatch;

    @BeforeEach
    void createTestData() {
        // Create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@test.com");
        testUser1.setFirstName("Test1");
        testUser1.setLastName("User1");
        testUser1.setPassword("password");
        testUser1.setDeclaredLevel(Level.INTERMEDIO);
        testUser1.setMatchesPlayed(0);
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@test.com");
        testUser2.setFirstName("Test2");
        testUser2.setLastName("User2");
        testUser2.setPassword("password");
        testUser2.setDeclaredLevel(Level.INTERMEDIO);
        testUser2.setMatchesPlayed(0);
        testUser2 = userRepository.save(testUser2);

        testUser3 = new User();
        testUser3.setUsername("testuser3");
        testUser3.setEmail("test3@test.com");
        testUser3.setFirstName("Test3");
        testUser3.setLastName("User3");
        testUser3.setPassword("password");
        testUser3.setDeclaredLevel(Level.AVANZATO);
        testUser3.setMatchesPlayed(0);
        testUser3 = userRepository.save(testUser3);

        testUser4 = new User();
        testUser4.setUsername("testuser4");
        testUser4.setEmail("test4@test.com");
        testUser4.setFirstName("Test4");
        testUser4.setLastName("User4");
        testUser4.setPassword("password");
        testUser4.setDeclaredLevel(Level.PRINCIPIANTE);
        testUser4.setMatchesPlayed(0);
        testUser4 = userRepository.save(testUser4);

        testUser5 = new User();
        testUser5.setUsername("testuser5");
        testUser5.setEmail("test5@test.com");
        testUser5.setFirstName("Test5");
        testUser5.setLastName("User5");
        testUser5.setPassword("password");
        testUser5.setDeclaredLevel(Level.PROFESSIONISTA);
        testUser5.setMatchesPlayed(0);
        testUser5 = userRepository.save(testUser5);

        // Create test match
        testMatch = new Match();
        testMatch.setLocation("Test Location");
        testMatch.setDateTime(LocalDateTime.now().plusDays(1));
        testMatch.setRequiredLevel(Level.INTERMEDIO);
        testMatch.setType(MatchType.PROPOSTA);
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setCreator(testUser1);
        testMatch.setCreatedAt(LocalDateTime.now());
        testMatch = matchRepository.save(testMatch);
    }

    @Test
    void testCreateMatch() {
        Match newMatch = new Match();
        newMatch.setLocation("New Match Location");
        newMatch.setDateTime(LocalDateTime.now().plusDays(2));
        newMatch.setRequiredLevel(Level.AVANZATO);
        newMatch.setType(MatchType.PROPOSTA);
        newMatch.setStatus(MatchStatus.WAITING);
        newMatch.setCreator(testUser1);
        newMatch.setCreatedAt(LocalDateTime.now());

        Match saved = matchRepository.save(newMatch);

        assertNotNull(saved.getId());
        assertEquals("New Match Location", saved.getLocation());
        assertEquals(MatchStatus.WAITING, saved.getStatus());
        assertEquals(testUser1, saved.getCreator());
    }

    @Test
    void testJoinMatch() {
        Registration reg = registrationService.joinMatch(testUser2, testMatch);

        assertNotNull(reg);
        assertEquals(testUser2, reg.getUser());
        assertEquals(testMatch, reg.getMatch());
        
        List<Registration> active = registrationService.getActiveRegistrationsByMatch(testMatch);
        assertTrue(active.size() >= 1);
    }

    @Test
    void testAutoConfirmAt4Players() {
        // Join 4 players
        registrationService.joinMatch(testUser1, testMatch);
        assertEquals(MatchStatus.WAITING, matchRepository.findById(testMatch.getId()).get().getStatus());

        registrationService.joinMatch(testUser2, testMatch);
        assertEquals(MatchStatus.WAITING, matchRepository.findById(testMatch.getId()).get().getStatus());

        registrationService.joinMatch(testUser3, testMatch);
        assertEquals(MatchStatus.WAITING, matchRepository.findById(testMatch.getId()).get().getStatus());

        registrationService.joinMatch(testUser4, testMatch);
        
        // Match should be CONFIRMED after 4th player
        Match updated = matchRepository.findById(testMatch.getId()).get();
        assertEquals(MatchStatus.CONFIRMED, updated.getStatus());
    }

    @Test
    void testMaxPlayersLimit() {
        // Join 4 players
        registrationService.joinMatch(testUser1, testMatch);
        registrationService.joinMatch(testUser2, testMatch);
        registrationService.joinMatch(testUser3, testMatch);
        registrationService.joinMatch(testUser4, testMatch);

        // 5th player should fail
        assertThrows(IllegalStateException.class, () -> {
            registrationService.joinMatch(testUser5, testMatch);
        });
    }

    @Test
    void testDuplicateRegistrationConstraint() {
        registrationService.joinMatch(testUser1, testMatch);

        // Same user cannot join twice
        assertThrows(IllegalStateException.class, () -> {
            registrationService.joinMatch(testUser1, testMatch);
        });
    }

    @Test
    void testLeaveMatch() {
        registrationService.joinMatch(testUser2, testMatch);
        
        List<Registration> before = registrationService.getActiveRegistrationsByMatch(testMatch);
        int countBefore = before.size();

        registrationService.leaveMatch(testUser2, testMatch);

        List<Registration> after = registrationService.getActiveRegistrationsByMatch(testMatch);
        assertTrue(after.size() < countBefore);
    }

    @Test
    void testCreateFeedback() {
        Feedback feedback = feedbackService.createFeedback(
            testUser1, testUser2, testMatch, Level.AVANZATO, "Ottimo giocatore"
        );

        assertNotNull(feedback);
        assertEquals(testUser1, feedback.getAuthor());
        assertEquals(testUser2, feedback.getTargetUser());
        assertEquals(Level.AVANZATO, feedback.getSuggestedLevel());
    }

    @Test
    void testOneFeedbackPerUserPerMatch() {
        feedbackService.createFeedback(testUser1, testUser2, testMatch, Level.INTERMEDIO, "Bravo");

        // Second feedback should fail
        assertThrows(RuntimeException.class, () -> {
            feedbackService.createFeedback(testUser1, testUser2, testMatch, Level.AVANZATO, "Altro");
        });
    }

    @Test
    void testPerceivedLevelUpdate() {
        // Create multiple feedbacks
        feedbackService.createFeedback(testUser1, testUser5, testMatch, Level.PRINCIPIANTE, "Test");
        feedbackService.createFeedback(testUser2, testUser5, testMatch, Level.INTERMEDIO, "Test");
        feedbackService.createFeedback(testUser3, testUser5, testMatch, Level.INTERMEDIO, "Test");

        // Update perceived level
        feedbackService.updatePerceivedLevel(testUser5.getId());

        User updated = userRepository.findById(testUser5.getId()).get();
        assertNotNull(updated.getPerceivedLevel());
        // Average of PRINCIPIANTE(0), INTERMEDIO(1), INTERMEDIO(1) = 0.67 ≈ 1 (INTERMEDIO)
        assertEquals(Level.INTERMEDIO, updated.getPerceivedLevel());
    }

    @Test
    void testStrategyDateSorting() {
        List<Match> sorted = matchService.getMatchesOrderedByDate();
        assertNotNull(sorted);
        
        // Verify sorted by date ascending
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getDateTime().compareTo(sorted.get(i + 1).getDateTime()) <= 0);
        }
    }

    @Test
    void testStrategyPopularitySorting() {
        List<Match> sorted = matchService.getMatchesOrderedByPopularity();
        assertNotNull(sorted);
        
        // Verify sorted by registrations count descending
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getActiveRegistrationsCount() >= sorted.get(i + 1).getActiveRegistrationsCount());
        }
    }

    @Test
    void testStrategyLevelSorting() {
        List<Match> sorted = matchService.getMatchesOrderedByLevel();
        assertNotNull(sorted);
        
        // Verify sorted by level ascending
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getRequiredLevel().ordinal() <= sorted.get(i + 1).getRequiredLevel().ordinal());
        }
    }

    @Test
    void testFilterByStatus() {
        List<Match> waiting = matchService.getMatchesByStatus(MatchStatus.WAITING);
        for (Match m : waiting) {
            assertEquals(MatchStatus.WAITING, m.getStatus());
        }
    }

    @Test
    void testFilterByLevel() {
        List<Match> intermediate = matchService.getMatchesByLevel(Level.INTERMEDIO);
        for (Match m : intermediate) {
            assertEquals(Level.INTERMEDIO, m.getRequiredLevel());
        }
    }
}
